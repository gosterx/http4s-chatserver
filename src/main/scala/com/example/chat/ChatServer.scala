package com.example.chat

import cats.effect.Async
import cats.effect.kernel.Concurrent
import com.comcast.ip4s.*
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger
import cats.effect.std.Queue
import fs2.concurrent.Topic
import com.example.chat.domain.OutputMessage
import com.example.chat.domain.OutputMessage.*
import domain.InputMessage
import cats.effect.IOApp
import cats.effect.IO
import cats.effect.kernel.Ref
import scala.concurrent.duration.*
import fs2.Stream

object ChatServer extends IOApp.Simple:

  override def run: IO[Unit] =
    for
      queue <- Queue.unbounded[IO, InputMessage]
      topic <- Topic[IO, OutputMessage]
      ref   <- Ref[IO].of(ChatState(Map.empty))
      _ <- {
        val serverStream = Stream.eval(server[IO](queue, topic))

        val keepAliveStream = Stream.awakeEvery[IO](30.seconds).map(_ => KeepAlive).through(topic.publish)

        val inputMessageProcessingStream = Stream
          .fromQueueUnterminated(queue)
          .evalMap(msg => ref.modify(_.process(msg)))
          .flatMap(Stream.emits)
          .through(topic.publish)

        Stream(serverStream, keepAliveStream, inputMessageProcessingStream).parJoinUnbounded.compile.drain
      }
    yield ()

  def server[F[_]: Async: Network: Concurrent](
      queue: Queue[F, InputMessage],
      topic: Topic[F, OutputMessage]
  ): F[Nothing] =
    EmberServerBuilder.default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpWebSocketApp(websocketBuilder =>
        Logger.httpApp[F](true, true)(ChatRoutes.chatRoutes[F](queue, topic, websocketBuilder).orNotFound)
      )
      .build.useForever
