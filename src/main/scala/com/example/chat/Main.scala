package com.example.chat

import cats.effect.{ IO, IOApp }
import cats.effect.std.Queue
import fs2.concurrent.Topic
import cats.effect.kernel.Ref
import com.example.chat.domain.OutputMessage
import domain.InputMessage

case class State(messageCount: Int)

case class FromClient(userName: String, message: String)
case class ToClient(message: String)

object Main extends IOApp.Simple:
  val run =
    for
      queue <- Queue.unbounded[IO, InputMessage]
      topic <- Topic[IO, OutputMessage]
      ref   <- Ref[IO].of(ChatState(Map.empty))
      _ <-
        fs2.Stream
          .fromQueueUnterminated(queue)
          .evalMap(msg => ref.modify(_.process(msg)))
          .flatMap(fs2.Stream.emits)
          .through(topic.publish)
          .compile.drain.start
      _ <- ChatServer.run[IO](queue, topic)
    yield ()
