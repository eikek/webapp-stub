package webappstub.server.routes.invite

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.SignupService
import webappstub.server.context.*
import webappstub.server.routes.Layout
import webappstub.server.routes.invite.InviteError.Key

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.util.ValidationErrors
import org.http4s.*
import org.http4s.scalatags.*

final class InviteRoutes[F[_]: Async](signup: SignupService[F]) extends Htmx4sDsl[F]:

  def routes = AuthedRoutes.of[Context.Account, F] {
    case ContextRequest(ctx, req @ GET -> Root) =>
      val settings = Settings.fromRequest(req)
      Ok(
        Layout("Create Invite", settings.theme, Layout.topNavBar(ctx).some)(
          View.view(settings, Model.CreateInvitePage())
        )
      )

    case ContextRequest(_, req @ POST -> Root) =>
      val settings = Settings.fromRequest(req)
      for
        in <- req.as[Model.CreateInviteForm]
        resp <- in.toModel.fold(
          errs =>
            BadRequest(
              View.view(settings, Model.CreateInvitePage(in, errs.some, None))
            ),
          secret =>
            signup.createInviteKey(secret).flatMap {
              case None =>
                val errs = ValidationErrors
                  .one(Key.ServerSecret, "Secret is not correct.")

                UnprocessableEntity(
                  View.view(settings, Model.CreateInvitePage(in, errs.some, None))
                )
              case key =>
                Ok(View.view(settings, Model.CreateInvitePage(in, None, key)))
            }
        )
      yield resp
  }
