package com.example.chat

import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.websocket.WebSocketFrame
import fs2.{ Pipe, Stream }
import cats.effect.std.Queue
import org.http4s.server.websocket.WebSocketBuilder2

object ChatRoutes:
  def chatRoutes[F[_]: Concurrent](websocketBuilder: WebSocketBuilder2[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "ws" / name =>
        val queue = Queue.unbounded[F, WebSocketFrame]

        queue.flatMap { actualQueue =>
          def toClient: Stream[F, WebSocketFrame] = Stream.fromQueueUnterminated(actualQueue)

          def fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap(actualQueue.offer)

          websocketBuilder.build(toClient, fromClient)
        }
    }
