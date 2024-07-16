package webappstub.server.routes.invite

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.SignupService
import webappstub.server.context.Context
import webappstub.server.routes.Layout
import webappstub.server.routes.invite.InviteError.Key

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.util.ValidationErrors
import org.http4s.HttpRoutes
import org.http4s.scalatags.*

final class InviteRoutes[F[_]: Async](signup: SignupService[F]) extends Htmx4sDsl[F]:

  def routes(ctx: Context.Authenticated) = HttpRoutes.of[F] {
    case req @ GET -> Root =>
      Ok(
        Layout("Create Invite", ctx.settings.theme, Layout.topNavBar(ctx).some)(
          View.view(ctx.settings, Model.CreateInvitePage())
        )
      )

    case req @ POST -> Root =>
      for
        in <- req.as[Model.CreateInviteForm]
        resp <- in.toModel.fold(
          errs =>
            BadRequest(
              View.view(ctx.settings, Model.CreateInvitePage(in, errs.some, None))
            ),
          secret =>
            signup.createInviteKey(secret).flatMap {
              case None =>
                val errs = ValidationErrors
                  .one(Key.ServerSecret, "Secret is not correct.")

                UnprocessableEntity(
                  View.view(
                    ctx.settings,
                    Model.CreateInvitePage(in, errs.some, None)
                  )
                )
              case key =>
                Ok(
                  View.view(ctx.settings, Model.CreateInvitePage(in, None, key))
                )
            }
        )
      yield resp
  }
