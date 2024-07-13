package webappstub.common.model

import io.bullet.borer.Encoder

final case class Password(value: String):
  def isEmpty: Boolean = value.isEmpty
  def nonEmpty: Boolean = !isEmpty
  override def toString: String = "***"

object Password:
  given Encoder[Password] = Encoder.forString.contramap(_ => "***")
