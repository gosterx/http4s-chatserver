package com.example.chat

import cats.effect.{ IO, IOApp }
import cats.effect.std.Queue
import fs2.concurrent.Topic

case class State(messageCount: Int)

case class FromClient(userName: String, message: String)
case class ToClient(message: String)

object Main extends IOApp.Simple:
  val run =
    for
      queue <- Queue.unbounded[IO, FromClient]
      topic <- Topic[IO, ToClient]
      _ <-
        fs2.Stream
          .fromQueueUnterminated(queue)
          .mapAccumulate(State(0)) {
            case (currentState, fromClient) =>
              (
                State(currentState.messageCount + 1),
                ToClient(s"(${currentState.messageCount + 1}): ${fromClient.userName} - ${fromClient.message}")
              )
          }
          .map(_._2)
          .through(topic.publish)
          .compile.drain.start
      _ <- ChatServer.run[IO](queue, topic)
    yield ()
