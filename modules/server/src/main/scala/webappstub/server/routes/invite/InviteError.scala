package webappstub.server.routes.invite

import cats.data.Validated
import cats.syntax.all.*

import htmx4s.http4s.util.ValidationErrors

object InviteError:
  enum Key:
    case ServerSecret

type Errors = ValidationErrors[InviteError.Key, String]
type InviteValid[A] = Validated[Errors, A]
