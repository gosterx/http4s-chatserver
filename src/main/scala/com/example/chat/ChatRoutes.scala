package com.example.chat

import cats.effect.kernel.Concurrent
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.websocket.WebSocketFrame
import fs2.{ Pipe, Stream }
import cats.effect.std.Queue
import org.http4s.server.websocket.WebSocketBuilder2
import fs2.concurrent.Topic

object ChatRoutes:
  def chatRoutes[F[_]: Concurrent](
      queue: Queue[F, FromClient],
      topic: Topic[F, ToClient],
      websocketBuilder: WebSocketBuilder2[F]
  ): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "ws" / userName =>
        def toClient: Stream[F, WebSocketFrame] =
          topic.subscribe(1000).map(toClientMessage => WebSocketFrame.Text(toClientMessage.message))

        def fromClient: Pipe[F, WebSocketFrame, Unit] = wsfStream =>
          wsfStream.collect {
            case WebSocketFrame.Text(text, _) =>
              FromClient(userName, text)
          }.evalMap(queue.offer)

        websocketBuilder.build(toClient, fromClient)
    }
