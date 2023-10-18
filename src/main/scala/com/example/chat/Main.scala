package com.example.chat

import cats.effect.{ IO, IOApp }
import cats.effect.std.Queue
import org.http4s.websocket.WebSocketFrame
import fs2.concurrent.Topic

object Main extends IOApp.Simple:
  val run =
    for
      queue <- Queue.unbounded[IO, WebSocketFrame]
      topic <- Topic[IO, WebSocketFrame]
      _     <- fs2.Stream.fromQueueUnterminated(queue).through(topic.publish).compile.drain.start
      _     <- ChatServer.run[IO](queue, topic)
    yield ()
