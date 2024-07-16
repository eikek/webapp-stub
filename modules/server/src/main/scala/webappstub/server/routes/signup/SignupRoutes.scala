package webappstub.server.routes.signup

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.SignupService
import webappstub.backend.signup.SignupResult
import webappstub.server.Config
import webappstub.server.context.Context
import webappstub.server.routes.{Layout, UiConfig}

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.util.ValidationErrors
import org.http4s.HttpRoutes
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.http4s.scalatags.*

import SignupError.Key

final class SignupRoutes[F[_]: Async](
    signup: SignupService[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]:

  val signupCfg = config.backend.signup
  println(s"$signup")

  def routes(ctx: Context.OptionalAuth) = HttpRoutes.of[F] {
    case req @ GET -> Root =>
      Ok(
        Layout("Signup", ctx.settings.theme)(
          View.view(uiCfg, ctx.settings, signupCfg.mode, None)
        )
      )

    case req @ GET -> Root / "create-invite" =>
      Ok(
        Layout("Create Invite", ctx.settings.theme)(
          View.createInvite(ctx.settings, Model.CreateInvitePage())
        )
      )

    case req @ POST -> Root =>
      for
        in <- req.as[Model.SignupForm]
        resp <- in.toModel.fold(
          errs =>
            BadRequest(View.signupForm(in, ctx.settings, signupCfg.mode, errs.some)),
          sreq =>
            signup.signup(sreq).flatMap {
              case SignupResult.Success(_) =>
                SeeOther(Location(uri"/app/login"))
              case result =>
                UnprocessableEntity(View.signupResult(ctx.settings, result))
            }
        )
      yield resp

    case req @ POST -> Root / "create-invite" =>
      for
        in <- req.as[Model.CreateInviteForm]
        resp <- in.toModel.fold(
          errs =>
            BadRequest(
              View.createInvite(ctx.settings, Model.CreateInvitePage(in, errs.some, None))
            ),
          secret =>
            signup.createInviteKey(secret).flatMap {
              case None =>
                val errs = ValidationErrors
                    .one(Key.ServerSecret, "Invitation keys cannot be generated")

                UnprocessableEntity(
                  View.createInvite(
                    ctx.settings,
                    Model.CreateInvitePage(in, errs.some, None)
                  )
                )
              case key =>
                Ok(
                  View.createInvite(ctx.settings, Model.CreateInvitePage(in, None, key))
                )
            }
        )
      yield resp
  }
