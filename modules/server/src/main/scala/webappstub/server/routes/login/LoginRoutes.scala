package webappstub.server.routes.login

import cats.effect.*
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.backend.{LoginService, SignupService}
import webappstub.backend.auth.*
import webappstub.server.Config
import webappstub.server.common.*
import webappstub.server.context.*
import webappstub.server.routes.{Layout, UiConfig}

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxLocation
import htmx4s.http4s.headers.HxRedirect
import org.http4s.*
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.http4s.scalatags.*
import soidc.http4s.client.ByteEntityDecoder
import soidc.http4s.routes.AuthCodeFlow
import soidc.http4s.routes.AuthCodeFlow.Result.Success
import soidc.http4s.routes.JwtCookie
import soidc.jwt.JoseHeader
import soidc.jwt.SimpleClaims
import soidc.borer.given
import soidc.core.OidParameterNames
import webappstub.backend.signup.SignupResult

final class LoginRoutes[F[_]: Async](
    login: LoginService[F],
    signup: SignupService[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]
    with ByteEntityDecoder:
  private val logger = scribe.cats.effect[F]
  private val api = LoginApi[F](login)
  private val cookieName = "webappstub_auth"
  private val rememberMeCookie = api.rememberMeCookie

  object Username extends OptionalQueryParamDecoderMatcher[String]("preferredName") {
    val key = "preferredName"
  }

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
                  signup.signup(sreq).flatMap {
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
      api
        .openIdRealm(baseUrl / name, logger, name)
        .semiflatMap { flow =>
          flow.run(req) {
            case Left(err) => Forbidden(View.loginFailed(err.toString()))
            case Right(AuthCodeFlow.Result.Success(token, idResp)) =>
              // check if account exists, if not redirect to signup
              // if yes, redirect to contact page
              val username = idResp.idToken
                .flatMap(_.decode[JoseHeader, SimpleClaims].toOption)
                .flatMap(
                  _.claims.values
                    .getAs[String](OidParameterNames.PreferredUsername)
                    .toOption
                    .flatten
                )
                .orEmpty

              // must use client redirects, browser don't send the cookie
              login.loginExternal(token).flatMap {
                case LoginResult.AccountMissing =>
                  val uri = uri"/app/login/openid-create-account"
                    .withQueryParam(Username.key, username)
                  val resp = Ok(Layout.clientRedirect(uri))
                  attachCookies(req, token, None, resp)

                case LoginResult.InvalidAuth =>
                  Forbidden(View.loginFailed("Authentication failed"))
                case LoginResult.Success(token, rme) =>
                  val resp = Ok(Layout.clientRedirect(uri"/app/contacts"))
                  attachCookies(req, token, rme, resp)
              }
          }
        }
        .getOrElseF(NotFound())

    case ContextRequest(ctx, req @ GET -> Root) =>
      val settings = Settings.fromRequest(req)
      if (ctx.isAuthenticated) Found(Location(uri"/app/contacts"))
      else
        api.findRememberMe(req).flatMap {
          case None => Ok(Layout("Login", settings.theme)(View.view(uiCfg, settings)))
          case Some(rkey) =>
            api.doRememberMeLogin(rkey).flatMap {
              case LoginResult.Success(token, _) =>
                val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
                val cookie = JwtCookie
                  .create(cookieName, token.jws, baseUrl)
                  .copy(maxAge = token.claims.expirationTime.map(_.toSeconds))
                Found(Location(uri"/app/contacts")).map(_.addCookie(cookie))
              case _ =>
                Ok(Layout("Login", settings.theme)(View.view(uiCfg, settings)))
            }
        }

    case ContextRequest(ctx, req @ POST -> Root) =>
      for
        in <- req.as[Model.UserPasswordForm]
        result <- api.doLogin(in)
        resp <- result.fold(
          errs => BadRequest(View.loginFailed(errs.mkString(", "))),
          _.fold(
            Forbidden(View.loginFailed("Authentication failed")),
            (token, rme) => {
              val resp = NoContent().map(
                _.putHeaders(HxLocation(HxLocation.Value.Path(uri"/app/contacts")))
              )
              attachCookies(req, token, rme, resp)
            }
          )
        )
      yield resp

    case ContextRequest(ctx, req @ DELETE -> Root) =>
      NoContent()
        .map(
          _.addCookie(
            JwtCookie.remove(cookieName, ClientRequestInfo.getBaseUrl(config, req))
          )
            .addCookie(
              JwtCookie
                .remove(rememberMeCookie, ClientRequestInfo.getBaseUrl(config, req))
            )
            .putHeaders(HxRedirect(uri"/app/login"))
        )
  }

  def attachCookies(
      req: Request[F],
      token: AuthToken,
      rme: Option[RememberMeToken],
      resp: F[Response[F]]
  ) =
    val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
    val cookie = JwtCookie
      .create(cookieName, token.jws, baseUrl)
      .copy(maxAge = token.claims.expirationTime.map(_.toSeconds))
    val rmeCookie = rme
      .map(t =>
        JwtCookie
          .create(rememberMeCookie, t.jws, baseUrl)
          .copy(maxAge = t.claims.expirationTime.map(_.toSeconds))
      )
      .getOrElse(JwtCookie.remove(rememberMeCookie, baseUrl))
    resp.map(_.addCookie(cookie).addCookie(rmeCookie))
