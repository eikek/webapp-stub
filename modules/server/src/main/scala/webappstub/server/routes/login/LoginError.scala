package webappstub.server.routes.login

import cats.data.Validated
import cats.syntax.all.*

import htmx4s.http4s.util.ValidationErrors

object LoginError:
  enum Key:
    case LoginName
    case Password
    case Invite
    case Default

type Errors = ValidationErrors[LoginError.Key, String]
type LoginValid[A] = Validated[Errors, A]
