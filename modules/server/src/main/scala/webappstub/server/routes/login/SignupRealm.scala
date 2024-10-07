package webappstub.server.routes.login

import scala.concurrent.duration.*

import cats.MonadThrow
import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.AuthConfig
import webappstub.common.model.*

import org.http4s.*
import soidc.borer.given
import soidc.core.*
import soidc.http4s.routes.GetToken
import soidc.http4s.routes.JwtAuth
import soidc.http4s.routes.JwtCookie
import soidc.jwt.{Uri as _, *}

private[login] class SignupRealm[F[_]: Clock: MonadThrow](auth: AuthConfig):

  val cookieName: String = "webappstub_signup"
  private val delegate = LocalFlow[F, JoseHeader, SimpleClaims](
    LocalFlow.Config(
      issuer = Provider("webappstub:signup").uri,
      secretKey = auth.internal.serverSecret.getOrElse(
        JwkGenerate.symmetric[SyncIO]().unsafeRunSync()
      ),
      sessionValidTime = 5.minutes
    )
  )

  val jwtAuth = JwtAuth
    .builder[F, JoseHeader, SimpleClaims]
    .withGetToken(GetToken.cookie(cookieName))
    .withValidator(delegate.validator)
    .secured

  private val provider = ParameterName.of("external_provider")

  def makeToken(id: ExternalAccountId): F[AuthToken] =
    delegate.createToken(
      JoseHeader.jwt,
      SimpleClaims.empty
        .withSubject(StringOrUri(id.id))
        .withValue(provider, id.provider.value)
    )

  def createCookie(id: ExternalAccountId, uri: Uri) =
    makeToken(id).map(token => JwtCookie.createDecoded(cookieName, token, uri))

  def externalId(req: Request[F]): F[Option[ExternalAccountId]] =
    jwtAuth.run(req).map {
      case Left(_) => None
      case Right(token) =>
        for
          p <- token.claims.values.getAs[Provider](provider).toOption.flatten
          id <- token.claims.subject
        yield ExternalAccountId(p, id.value)
    }
