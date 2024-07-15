package webappstub.backend.signup

import webappstub.common.model.Password

enum SignupMode:
  case Closed
  case Open
  case WithInvite(serverKey: Password)

  def isInvite: Boolean = this match
    case Closed        => false
    case Open          => false
    case WithInvite(_) => true

object SignupMode:
  def fromString(s: String): Either[String, SignupMode] =
    s.toLowerCase() match
      case "closed" => Right(Closed)
      case "open"   => Right(Open)
      case invite if invite.startsWith("invite:") =>
        val key = s.drop(7)
        if (key.isEmpty()) Left(s"Empty invite server key: $s")
        else Right(WithInvite(Password(key)))
      case _ => Left(s"Invalid invite mode: $s")
