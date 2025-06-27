package webappstub.common.model

import cats.data.{Validated, ValidatedNel}

final case class Name private (first: String, last: String):
  val fullName = if (last.isEmpty) first else s"$first $last"
  def contains(s: String): Boolean =
    fullName.toLowerCase.contains(s)

object Name:
  given Ordering[Name] =
    Ordering.fromLessThan((a, b) => a.fullName < b.fullName)

  def unsafe(first: String, last: String): Name =
    create(first, last).fold(err => sys.error(err.toList.mkString), identity)

  def create(first: String, last: String): ValidatedNel[String, Name] =
    val fname = Validated.condNel(first.nonEmpty, first, "First Name must not be empty")
    fname.map(Name(_, last.trim))
