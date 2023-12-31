package com.example.chat

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
import org.http4s.StaticFile
import cats.effect.kernel.Async
import cats.effect.kernel.Ref
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType

object ChatRoutes:
  def chatRoutes[F[_]: Async](
      chatState: Ref[F, ChatState],
      queue: Queue[F, InputMessage],
      topic: Topic[F, OutputMessage],
      websocketBuilder: WebSocketBuilder2[F]
  ): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    HttpRoutes.of[F] {
      case req @ GET -> Root =>
        StaticFile
          .fromPath[F](fs2.io.file.Path("static/index.html"))
          .getOrElseF(NotFound())

      case req @ GET -> Root / "chat.js" =>
        StaticFile
          .fromPath[F](fs2.io.file.Path("static/chat.js"))
          .getOrElseF(NotFound())

      case GET -> Root / "metrics" =>
        val responseStream = Stream.eval(chatState.get).map { state =>
          s"""
             |<html>
             |  <title>Chat Server State</title>
             |  <body>
             |    <div>Users: ${state.usersRoom.keySet.size}</div>
             |    <div>Rooms: ${state.usersRoom.values.toSet.size}</div>
             |  </body>
             |</html>
              """.stripMargin
        }

        Ok(responseStream, `Content-Type`(MediaType.text.html))

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
