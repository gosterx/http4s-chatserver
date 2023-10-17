package com.example.chat

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  val run = ChatServer.run[IO]
