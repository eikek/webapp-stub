package webappstub.server.routes.login

import scala.concurrent.duration.*

import cats.MonadThrow
import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.AuthConfig
import webappstub.common.model.*

import org.http4s.*
import soidc.borer.given
import soidc.core.*
import soidc.http4s.routes.GetToken
import soidc.http4s.routes.JwtAuthMiddleware
import soidc.http4s.routes.JwtCookie
import soidc.jwt.{Uri as _, *}

private[login] class SignupRealm[F[_]: Clock: MonadThrow](auth: AuthConfig):
  private val cea = ContentEncryptionAlgorithm.A256GCM
  private val encKey = JwkGenerate.symmetricEncrypt[SyncIO](cea).unsafeRunSync()

  private val validCookie = "webappstub_signup"
  private val atCookie = "webappstub_signup_at"
  private val rtCookie = "webappstub_signup_rt"

  final case class Data(
      accessToken: AuthToken,
      externalId: ExternalAccountId,
      refreshToken: Option[JWS]
  )

  def extract(req: Request[F]): OptionT[F, Data] =
    for
      atc <- OptionT.fromOption(
        req.cookies.find(_.name == atCookie).map(_.content)
      )
      rtc = req.cookies.find(_.name == rtCookie).map(_.content)
      atcPlain <- OptionT.liftF(decrypt(atc))
      rtcPlain <- OptionT.liftF(rtc.traverse(decrypt))

      eId <- OptionT.fromOption(atcPlain.accountKey.flatMap(_.fold(_ => None, _.some)))
    yield Data(atcPlain, eId, rtcPlain.map(_.jws))

  def createData(
      accessToken: AuthToken,
      refreshToken: Option[JWS]
  ): Option[Data] =
    accessToken.accountKey match
      case Some(AccountKey.External(id)) => Some(Data(accessToken, id, refreshToken))
      case _                             => None

  def addData(data: Option[Data], uri: Uri)(r: Response[F]): F[Response[F]] =
    data match
      case None => r.pure[F]
      case Some(d) =>
        for
          vt <- makeToken.map(t => JwtCookie.createDecoded(validCookie, t, uri))
          at <- encrypt(d.accessToken.jws)
          rt <- d.refreshToken.traverse(encrypt)
          atc = vt.copy(content = at, name = atCookie)
          rtc = rt.map(e => vt.copy(content = e, name = rtCookie))
          n = r.addCookie(vt).addCookie(atc)
          nn = rtc.map(n.addCookie).getOrElse(n)
        yield nn

  private def encrypt(token: JWS): F[String] =
    MonadThrow[F].fromEither {
      JWE
        .encryptJWS(
          JoseHeader.jwe(Algorithm.Encrypt.dir, cea),
          token,
          encKey
        )
        .map(_.compact)
    }

  private def decrypt(token: String): F[AuthToken] =
    MonadThrow[F].fromEither {
      JWE.decryptStringToJWS[JoseHeader, SimpleClaims](token, encKey)
    }

  private val delegate = LocalFlow[F, JoseHeader, SimpleClaims](
    LocalFlow.Config(
      issuer = Provider("webappstub:signup").uri,
      secretKey = auth.internal.serverSecret.getOrElse(
        JwkGenerate.symmetricSign[SyncIO]().unsafeRunSync()
      ),
      sessionValidTime = 5.minutes
    )
  )

  def secured = JwtAuthMiddleware
    .builder[F, JoseHeader, SimpleClaims]
    .withGeToken(GetToken.cookie(validCookie))
    .withValidator(delegate.validator)
    .secured

  private def makeToken: F[AuthToken] =
    delegate.createToken(JoseHeader.jwt, SimpleClaims.empty)

  def removeCookies(r: Response[F]) =
    r.removeCookie(validCookie)
      .removeCookie(atCookie)
      .removeCookie(rtCookie)
