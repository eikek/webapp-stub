package webappstub.server.routes.login
import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.Backend
import webappstub.backend.auth.*
import webappstub.backend.signup.SignupResult
import webappstub.common.model.*
import webappstub.server.Config
import webappstub.server.common.*
import webappstub.server.context.*
import webappstub.server.routes.{Layout, UiConfig}

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxLocation
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.scalatags.*
import soidc.borer.given
import soidc.core.OidParameterNames
import soidc.http4s.client.ByteEntityDecoder
import soidc.http4s.routes.AuthCodeFlow
import soidc.http4s.routes.AuthCodeFlow.Result.Success
import soidc.jwt.JoseHeader
import soidc.jwt.SimpleClaims

final class OpenIdLoginRoutes[F[_]: Async](
    backend: Backend[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]
    with ByteEntityDecoder:
  private val logger = scribe.cats.effect[F]

  object Username extends OptionalQueryParamDecoderMatcher[String]("preferredName") {
    val key = "preferredName"
  }

  private def openIdRealm(
      name: String
  ): OptionT[F, AuthCodeFlow[F, JoseHeader, SimpleClaims]] =
    OptionT
      .fromOption[F](backend.realms.openIdRealms.get(name))
      .semiflatMap(AuthCodeFlow(_, SoidcLogger(logger)))

  def routes = AuthedRoutes.of[MaybeAuthenticated, F] {
    case ContextRequest(
          context,
          req @ GET -> Root / "openid-create-account" :? Username(name)
        ) =>
      // TODO check if account already exists for this external token, or error for internal token
      val settings = Settings.fromRequest(req)
      if (!context.isAuthenticated) Forbidden()
      else
        Ok(
          Layout("Signup", settings.theme)(
            View.signupView(
              uiCfg,
              settings,
              config.backend.signup.mode,
              Model.SignupForm(user = name.orEmpty),
              None
            )
          )
        )

    case ContextRequest(context, req @ POST -> Root / "openid-create-account") =>
      context.getToken.flatMap(_.accountKey) match
        case None =>
          logger.warn(s"token in request has no account key") >> Forbidden()
        case Some(AccountKey.Internal(_)) =>
          logger.warn(s"token in request is an internal token") >> UnprocessableEntity()
        case Some(AccountKey.External(id)) =>
          for
            data <- req.as[Model.SignupForm]
            settings = Settings.fromRequest(req)
            resp <- data
              .toModel(id)
              .fold(
                errs =>
                  BadRequest(
                    View.signupForm(data, settings, config.backend.signup.mode, errs.some)
                  ),
                sreq =>
                  backend.signup.signup(sreq).flatMap {
                    case SignupResult.Success(_) =>
                      NoContent()
                        .map(
                          _.putHeaders(HxLocation(HxLocation.Value.Path(uri"/app/login")))
                        )
                    case result =>
                      UnprocessableEntity(View.signupResult(settings, result))
                  }
              )
          yield resp

    case ContextRequest(context, req @ GET -> ("openid" /: name /: _)) =>
      val baseUrl = ClientRequestInfo.getBaseUrl(config, req) / "app" / "login" / "openid"
      openIdRealm(name)
        .semiflatMap { flow =>
          flow.run(req, baseUrl / name) {
            case Left(err) => Forbidden(View.loginFailed(err.toString()))
            case Right(AuthCodeFlow.Result.Success(token, idResp)) =>
              val username = idResp.idTokenJWS
                .flatMap(_.toOption)
                .flatMap(_.decode[JoseHeader, SimpleClaims].toOption)
                .flatMap(
                  _.claims.values
                    .getAs[String](OidParameterNames.PreferredUsername)
                    .toOption
                    .flatten
                )
                .orEmpty

              // must use client redirects, browser don't send the cookie
              backend.login.loginExternal(token).flatMap {
                case LoginResult.AccountMissing =>
                  val uri = uri"/app/login/openid-create-account"
                    .withQueryParam(Username.key, username)
                  val resp = Ok(Layout.clientRedirect(uri))
                  Cookies.set(config)(req, token, None)(resp)

                case LoginResult.InvalidAuth =>
                  Forbidden(View.loginFailed("Authentication failed"))
                case LoginResult.Success(token, rme) =>
                  val resp = Ok(Layout.clientRedirect(uri"/app/contacts"))
                  Cookies.set(config)(req, token, rme)(resp)
              }
          }
        }
        .getOrElseF(NotFound())
  }
