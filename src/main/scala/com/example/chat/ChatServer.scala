package com.example.chat

import cats.effect.Async
import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.io.net.Network
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger

object ChatServer:

  def run[F[_]: Async: Network: Concurrent]: F[Nothing] =
    EmberServerBuilder.default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpWebSocketApp(websocketBuilder =>
        Logger.httpApp[F](true, true)(ChatRoutes.chatRoutes[F](websocketBuilder).orNotFound)
      )
      .build.useForever
