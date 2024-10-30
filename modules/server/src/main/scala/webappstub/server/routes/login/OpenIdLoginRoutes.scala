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
import soidc.core.model.TokenResponse
import soidc.http4s.client.ByteEntityDecoder
import soidc.http4s.routes.AuthCodeFlow
import soidc.http4s.routes.AuthCodeFlow.Result.Success
import soidc.http4s.routes.GitHubFlow
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

  private def getGithubFlow(name: String): OptionT[F, GitHubFlow[F]] =
    OptionT.fromOption[F](
      backend.realms.githubAuth.filter(_._1 == name).map { case (_, auth) =>
        GitHubFlow[F](auth, SoidcLogger(logger))
      }
    )

  private val signupRealm = SignupRealm[F](config.backend.auth)

  private val postSignupAuthCookie = "webappstub_post_signup"

  def routes = AuthedRoutes.of[MaybeAuthenticated, F] {
    case ContextRequest(
          _,
          req @ GET -> Root / "openid-create-account" :? Username(name)
        ) =>
      // TODO check if account already exists for this external token, or error for internal token
      val settings = Settings.fromRequest(req)
      signupRealm.externalId(req).flatMap {
        case None => NotFound()
        case Some(_) =>
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
      }

    case ContextRequest(context, req @ POST -> Root / "openid-create-account") =>
      signupRealm.externalId(req).flatMap {
        case None =>
          logger.warn(s"token in request has no account key") >> Forbidden()
        case Some(id) =>
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
                    case SignupResult.Success(acc) =>
                      val authToken = req.cookies
                        .find(_.name == postSignupAuthCookie)
                        .traverse(c => signupRealm.decrypt(c.content))
                        .map(_.flatMap(AuthCookie.parse))

                      authToken.flatMap { t =>
                        Cookies.set(config)(req, t, None)(
                          Ok(Layout.clientRedirect(uri"/app/login")).map(
                            _.putHeaders(
                              HxLocation(HxLocation.Value.Path(uri"/app/login"))
                            )
                              .removeCookie(postSignupAuthCookie)
                              .removeCookie(signupRealm.cookieName)
                          )
                        )
                      }
                    case result =>
                      UnprocessableEntity(View.signupResult(settings, result))
                  }
              )
          yield resp
      }

    case ContextRequest(_, req @ GET -> ("openid" /: name /: _)) =>
      val baseUrl = ClientRequestInfo.getBaseUrl(config, req) / "app" / "login" / "openid"
      val forGithub = getGithubFlow(name)
        .semiflatMap { flow =>
          flow.run(req, baseUrl / name) {
            case Left(err) => Forbidden(View.loginFailed(err.toString()))
            case Right(GitHubFlow.Result.Success(user, resp)) =>
              val username = user.login.orEmpty
              val accountId = ExternalAccountId(Provider.github, user.id.toString())
              backend.login.loginExternal(accountId).flatMap {
                case LoginResult.AccountMissing =>
                  val uri = uri"/app/login/openid-create-account"
                    .withQueryParam(Username.key, username)
                  val resp = Ok(Layout.clientRedirect(uri))
                  signupRealm.createCookie(accountId, uri).flatMap { cookie =>
                    resp.map(_.addCookie(cookie))
                  }

                case LoginResult.InvalidAuth =>
                  Forbidden(View.loginFailed("Authentication failed"))
                case LoginResult.Success(token, rme) =>
                  val resp = Ok(Layout.clientRedirect(uri"/app/contacts"))
                  Cookies.set(config)(req, token, rme)(resp)
              }
          }
        }
      val forOpenid = openIdRealm(name)
        .semiflatMap { flow =>
          flow.run(req, baseUrl / name) {
            case Left(err) => Forbidden(View.loginFailed(err.toString()))
            case Right(AuthCodeFlow.Result.Success(token, idResp)) =>
              // must use client redirects, browser don't send the cookie
              backend.login.loginExternal(token).flatMap {
                case LoginResult.AccountMissing =>
                  val username = preferredName(idResp)
                  val uri = uri"/app/login/openid-create-account"
                    .withQueryParam(Username.key, username)
                  val signupCookie = token.accountKey
                    .collect { case AccountKey.External(id) => id }
                    .traverse(signupRealm.createCookie(_, uri))

                  signupRealm.encypt(token).flatMap { tokenStr =>
                    Ok(Layout.clientRedirect(uri))
                      .flatMap(r => signupCookie.map(_.map(r.addCookie).getOrElse(r)))
                      .map(
                        _.addCookie(
                          ResponseCookie(postSignupAuthCookie, tokenStr, path = Some("/"))
                        )
                      )
                  }
                case LoginResult.InvalidAuth =>
                  Forbidden(View.loginFailed("Authentication failed"))
                case LoginResult.Success(token, rme) =>
                  val resp = Ok(Layout.clientRedirect(uri"/app/contacts"))
                  Cookies.set(config)(req, token, rme)(resp)
              }
          }
        }

      (forGithub <+> forOpenid).getOrElseF(NotFound())
  }

  private def preferredName(resp: TokenResponse.Success) =
    resp.idTokenJWS
      .flatMap(_.toOption)
      .flatMap(_.decode[JoseHeader, SimpleClaims].toOption)
      .flatMap(
        _.claims.values
          .getAs[String](OidParameterNames.PreferredUsername)
          .toOption
          .flatten
      )
      .orEmpty
