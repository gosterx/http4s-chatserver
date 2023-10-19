package com.example.chat

import cats.effect.kernel.Concurrent
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.websocket.WebSocketFrame
import fs2.{ Pipe, Stream }
import cats.effect.std.Queue
import org.http4s.server.websocket.WebSocketBuilder2
import fs2.concurrent.Topic
import com.example.chat.domain.domain.UserName
import com.example.chat.domain.InputMessage.EnterRoom
import com.example.chat.domain.InputMessage.Disconnect
import com.example.chat.domain.OutputMessage
import domain.InputMessage

object ChatRoutes:
  def chatRoutes[F[_]: Concurrent](
      queue: Queue[F, InputMessage],
      topic: Topic[F, OutputMessage],
      websocketBuilder: WebSocketBuilder2[F]
  ): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "ws" / UserName(userName) =>
        def fromClient: Pipe[F, WebSocketFrame, Unit] = (wsfStream: fs2.Stream[F, WebSocketFrame]) =>
          val initialStream: Stream[F, InputMessage] = Stream.emit(EnterRoom(userName))

          val parsedWebSocketInput: Stream[F, InputMessage] =
            wsfStream.collect {
              case WebSocketFrame.Text(text, _) => domain.InputMessage.parse(userName, text)
              case _: WebSocketFrame.Close      => Disconnect(userName)
            }

          (initialStream ++ parsedWebSocketInput).evalMap(queue.offer)

        def toClient: Stream[F, WebSocketFrame] =
          topic.subscribe(1000).filter(_.directedTo(userName)).map(outputMessage =>
            WebSocketFrame.Text(outputMessage.textToSend)
          )

        websocketBuilder.build(toClient, fromClient)
    }
