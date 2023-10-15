package com.fadi

import cats.Semigroup
import cats.effect.Concurrent
import cats.implicits._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.Method._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{QueryParamDecoder, _}

trait Evaluations[F[_]] {
  def evaluate(url: List[Evaluations.URL]): F[Evaluations.Result]

  def parseCSV(doc: String): F[List[Evaluations.Speech]]

  def evaluatePartition(speech: Evaluations.Speech): F[Map[Evaluations.Speaker, Evaluations.Counter]]

  def toResult(counter: Map[Evaluations.Speaker, Evaluations.Counter]): Evaluations.Result
}

object Evaluations {
  final case class URL(url: Uri) extends AnyVal

  final case class Speech(speaker: String, topic: String, date: String, count: Integer)

  final case class Counter(speeches: Int, securityTopics: Int, words: Int)

  final case class Speaker(value: String) extends AnyVal

  final case class Result(
    mostSpeeches: Option[String],
    mostSecurity: Option[String],
    leastWordy: Option[String]
  )

  object URL {
    implicit val urlQueryParamDecoder: QueryParamDecoder[URL] = QueryParamDecoder[String]
      .emap(Uri.fromString).map(URL.apply)
  }

  object Result {
    implicit val ResultEncoder: Encoder[Result] = deriveEncoder[Result]
  }

  object Counter {
    implicit val partialResultSemiGroup: Semigroup[Counter] = (c1, c2) =>
      Counter(c1.speeches + c2.speeches, c1.securityTopics + c2.securityTopics, c1.words + c2.words)
  }

  def impl[F[_] : Concurrent](C: Client[F]): Evaluations[F] = new Evaluations[F] {
    val dsl = new Http4sClientDsl[F] {}

    import dsl._

    override def evaluate(urls: List[URL]): F[Result] = {
      val docsF: F[List[String]] = urls.traverse(url =>
        C.expect[String](GET(url.url))
          )
      val speechesF: F[List[Speech]] = docsF.flatMap(docs => docs.map(parseCSV).sequence.map(_.flatten))
      val partitionsF: F[List[Map[Speaker, Counter]]] = speechesF.flatMap(speeches => speeches.traverse(evaluatePartition))
      val resultF = partitionsF.map(partition => partition.reduce(_ |+| _)).map(toResult)
      resultF
    }

    override def parseCSV(doc: String): F[List[Speech]] = {
      val lines = doc.split("\n")
      Concurrent[F].catchNonFatal(
        lines.tail.map { line =>
          val columns = line.split(",")
          if (columns.size < 4) {
            throw new Exception("Failed to parse CSV")
          }
          Speech(columns(0).strip(), columns(1).strip(), columns(2).strip(), columns(3).strip().toInt)
        }.toList
      )
    }

    override def evaluatePartition(speech: Speech): F[Map[Speaker, Counter]] = Concurrent[F].pure(
      Map(Speaker(speech.speaker) ->
        Counter(if (speech.date.startsWith("2013")) 1 else 0, if (speech.topic == "Innere Sicherheit") 1 else 0, speech.count)))

    override def toResult(counter: Map[Speaker, Counter]): Result = {
      val speeches = counter.toList
        .filter { case (_, Counter(speeches, _, _)) => speeches > 0 }
        .sortBy { case (_, Counter(speeches, _, _)) => -speeches }
      val mostSpeeches: Option[Speaker] = speeches.headOption match {
        case None => None
        case Some(speaker -> Counter(n1, _, _)) =>
          speeches.tail.headOption match {
            case Some(_ -> Counter(n2, _, _)) if n1 == n2 => None
            case _ => Some(speaker)
          }
      }

      val security = counter.toList
        .filter { case (_, Counter(_, securityTopics, _)) => securityTopics > 0 }
        .sortBy { case (_, Counter(_, securityTopics, _)) => -securityTopics }
      val mostSecurity: Option[Speaker] = security.headOption match {
        case None => None
        case Some(speaker -> Counter(_, n1, _)) =>
          security.tail.headOption match {
            case Some(_ -> Counter(_, n2, _)) if n1 == n2 => None
            case _ => Some(speaker)
          }
      }

      val words = counter.toList
        .filter { case (_, Counter(_, _, words)) => words > 0 }
        .sortBy { case (_, Counter(_, _, words)) => words }
      val leastWords: Option[Speaker] = words.headOption match {
        case None => None
        case Some(speaker -> Counter(_, _, n1)) =>
          words.tail.headOption match {
            case Some(_ -> Counter(_, _, n2)) if n1 == n2 => None
            case _ => Some(speaker)
          }
      }
      Result(mostSpeeches.map(_.value), mostSecurity.map(_.value), leastWords.map(_.value))
    }
  }
}
