package webappstub.server.routes.signup

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.SignupService
import webappstub.backend.signup.SignupResult
import webappstub.server.Config
import webappstub.server.context.Context
import webappstub.server.routes.{Layout, UiConfig}

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxLocation
import org.http4s.HttpRoutes
import org.http4s.implicits.*
import org.http4s.scalatags.*

final class SignupRoutes[F[_]: Async](
    signup: SignupService[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]:

  val signupCfg = config.backend.signup

  def routes(ctx: Context.OptionalAuth) = HttpRoutes.of[F] {
    case req @ GET -> Root =>
      Ok(
        Layout("Signup", ctx.settings.theme)(
          View.view(uiCfg, ctx.settings, signupCfg.mode, None)
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
                NoContent()
                  .map(_.putHeaders(HxLocation(HxLocation.Value.Path(uri"/app/login"))))
              case result =>
                UnprocessableEntity(View.signupResult(ctx.settings, result))
            }
        )
      yield resp
  }
