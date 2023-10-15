package com.fadi

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  val run: IO[Nothing] = FadiServer.run[IO]
}

