package webappstub.server.routes.signup

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.SignupService
import webappstub.backend.signup.SignupResult
import webappstub.server.Config
import webappstub.server.context.*
import webappstub.server.routes.{Layout, UiConfig}

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxLocation
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.scalatags.*

final class SignupRoutes[F[_]: Async](
    signup: SignupService[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]:

  val signupCfg = config.backend.signup

  def routes = AuthedRoutes.of[MaybeAuthenticated, F] {
    case ContextRequest(ctx, req @ GET -> Root) =>
      val settings = Settings.fromRequest(req)
      Ok(
        Layout("Signup", settings.theme)(
          View.view(uiCfg, settings, signupCfg.mode, Model.SignupForm(), None)
        )
      )

    case ContextRequest(ctx, req @ POST -> Root) =>
      val settings = Settings.fromRequest(req)
      for
        in <- req.as[Model.SignupForm]
        resp <- in.toModel.fold(
          errs => BadRequest(View.signupForm(in, settings, signupCfg.mode, errs.some)),
          sreq =>
            signup.signup(sreq).flatMap {
              case SignupResult.Success(_) =>
                NoContent()
                  .map(_.putHeaders(HxLocation(HxLocation.Value.Path(uri"/app/login"))))
              case result =>
                UnprocessableEntity(View.signupResult(settings, result))
            }
        )
      yield resp
  }
