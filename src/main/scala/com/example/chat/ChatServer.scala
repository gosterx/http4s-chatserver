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
import domain.InputMessage

object ChatServer:

  def run[F[_]: Async: Network: Concurrent](queue: Queue[F, InputMessage], topic: Topic[F, OutputMessage]): F[Nothing] =
    EmberServerBuilder.default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpWebSocketApp(websocketBuilder =>
        Logger.httpApp[F](true, true)(ChatRoutes.chatRoutes[F](queue, topic, websocketBuilder).orNotFound)
      )
      .build.useForever
