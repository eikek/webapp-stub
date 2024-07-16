package webappstub.server.routes.signup

import cats.data.Validated
import cats.syntax.all.*

import htmx4s.http4s.util.ValidationErrors

object SignupError:
  enum Key:
    case LoginName
    case Password
    case PasswordConfirm
    case Invite
    case Default
    case ServerSecret

type Errors = ValidationErrors[SignupError.Key, String]
type SignupValid[A] = Validated[Errors, A]
