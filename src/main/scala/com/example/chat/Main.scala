package com.example.chat

import cats.effect.{ IO, IOApp }
import cats.effect.std.Queue
import org.http4s.websocket.WebSocketFrame
import fs2.concurrent.Topic

case class State(messageCount: Int)

object Main extends IOApp.Simple:
  val run =
    for
      queue <- Queue.unbounded[IO, WebSocketFrame]
      topic <- Topic[IO, WebSocketFrame]
      _ <-
        fs2.Stream
          .fromQueueUnterminated(queue)
          .mapAccumulate(State(0)) {
            case (currentState, nextWsMessage) =>
              nextWsMessage match
                case WebSocketFrame.Text(text, _) =>
                  (
                    State(currentState.messageCount + 1),
                    WebSocketFrame.Text(s"(${currentState.messageCount + 1}): $text")
                  )
                case _ => (currentState, nextWsMessage)
          }
          .map(_._2)
          .through(topic.publish)
          .compile.drain.start
      _ <- ChatServer.run[IO](queue, topic)
    yield ()
