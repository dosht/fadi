package com.fadi

import cats.effect.Sync
import cats.implicits._
import com.fadi.Evaluations.URL
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl

object FadiRoutes {

  def evaluationRoutes[F[_] : Sync](E: Evaluations[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    object EvaluationQueryParamMatcher extends OptionalMultiQueryParamDecoderMatcher[URL]("url")

    HttpRoutes.of[F] {
      case GET -> Root / "evaluation" :? EvaluationQueryParamMatcher(validatedUrls) =>
        validatedUrls.fold(
          _ => BadRequest(Map("error" -> "Unable to parse urls")),
          urls => for {
            resultOrError <- E.evaluate(urls).attempt
            resp <- resultOrError
              .fold(
                err => BadRequest(Map("error" -> err.getMessage)),
                result => Ok(result))
          } yield resp)
    }
  }
}