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
import soidc.jwt.JWSDecoded
import soidc.jwt.JoseHeader
import soidc.jwt.SimpleClaims
import soidc.jwt.StringOrUri

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

  def signupRoutes = AuthedRoutes.of[Authenticated, F] {
    case ContextRequest(
          _,
          req @ GET -> Root / "openid-create-account" :? Username(name)
        ) =>
      // TODO check if account already exists for this external token, or error for internal token
      val settings = Settings.fromRequest(req)
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

    case ContextRequest(_, req @ POST -> Root / "openid-create-account") =>
      signupRealm.extract(req).value.flatMap {
        case None =>
          logger.warn(s"token in request has no account key") >> Forbidden()
        case Some(tokens) =>
          for
            data <- req.as[Model.SignupForm]
            settings = Settings.fromRequest(req)
            resp <- data
              .toModel(tokens.externalId, tokens.refreshToken)
              .fold(
                errs =>
                  BadRequest(
                    View.signupForm(data, settings, config.backend.signup.mode, errs.some)
                  ),
                sreq =>
                  backend.signup.signup(sreq).flatMap {
                    case SignupResult.Success(acc) =>
                      val authToken =
                        if (tokens.accessToken.jws.signature.isDefined)
                          tokens.accessToken.pure[F]
                        else backend.realms.localRealm.makeToken(acc.id)

                      authToken.flatMap { t =>
                        Cookies.set(config)(req, t, None)(
                          Ok(Layout.clientRedirect(uri"/app/login"))
                            .map(
                              _.putHeaders(
                                HxLocation(HxLocation.Value.Path(uri"/app/login"))
                              )
                            )
                            .map(signupRealm.removeCookies)
                        )
                      }

                    case result =>
                      UnprocessableEntity(View.signupResult(settings, result))
                  }
              )
          yield resp
      }
  }

  def signup: HttpRoutes[F] = signupRealm.secured(signupRoutes)

  def signin = HttpRoutes.of[F] { case req @ GET -> ("openid" /: name /: _) =>
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
                val token: AuthToken = JWSDecoded.createUnsigned(
                  JoseHeader.jwt,
                  SimpleClaims.empty
                    .withIssuer(Provider.github.uri)
                    .withSubject(StringOrUri(user.id.toString))
                )
                val signupData = signupRealm.createData(token, None)
                Ok(Layout.clientRedirect(uri))
                  .flatMap(signupRealm.addData(signupData, uri))

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
                val signupData = signupRealm
                  .createData(token, idResp.refreshTokenJWS.flatMap(_.toOption))

                Ok(Layout.clientRedirect(uri))
                  .flatMap(signupRealm.addData(signupData, uri))

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

  def routes = signin <+> signup

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
