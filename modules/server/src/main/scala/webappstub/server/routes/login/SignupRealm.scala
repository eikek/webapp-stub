package webappstub.server.routes.login

import scala.concurrent.duration.*

import cats.MonadThrow
import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.AuthConfig
import webappstub.common.model.*

import org.http4s.*
import scodec.bits.ByteVector
import soidc.borer.given
import soidc.core.*
import soidc.http4s.routes.GetToken
import soidc.http4s.routes.JwtAuthMiddleware
import soidc.http4s.routes.JwtCookie
import soidc.jwt.{Uri as _, *}

private[login] class SignupRealm[F[_]: Clock: MonadThrow](auth: AuthConfig):
  private val cea = ContentEncryptionAlgorithm.A256GCM
  private val encKey = SyncIO(cea.generateKey).unsafeRunSync()

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

      atcJws <- OptionT.liftF(
        MonadThrow[F].fromEither(
          JWS
            .fromString(atcPlain)
            .left
            .map(JwtError.DecodeError(_))
            .flatMap(_.decode[JoseHeader, SimpleClaims])
        )
      )
      eId <- OptionT.fromOption(atcJws.accountKey.flatMap(_.fold(_ => None, _.some)))
      rtcJws = rtcPlain.map(JWS.fromString).flatMap(_.toOption)
    yield Data(atcJws, eId, rtcJws)

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
          at <- encrypt(d.accessToken.compact)
          rt <- d.refreshToken.traverse(e => encrypt(e.compact))
          atc = vt.copy(content = at, name = atCookie)
          rtc = rt.map(e => vt.copy(content = e, name = rtCookie))
          n = r.addCookie(vt).addCookie(atc)
          nn = rtc.map(n.addCookie).getOrElse(n)
        yield nn

  private def encrypt(token: String): F[String] =
    MonadThrow[F].fromEither {
      JWE
        .encryptSymmetric(encKey, cea, ByteVector.view(token.getBytes))
        .map(_.compact)
    }

  private def decrypt(token: String): F[String] =
    MonadThrow[F].fromEither {
      JWE
        .fromString(token)
        .left
        .map(JwtError.DecodeError(_))
        .flatMap(_.decryptSymmetric[JoseHeader](encKey))
        .map(_.decodeUtf8Lenient)
    }

  private val delegate = LocalFlow[F, JoseHeader, SimpleClaims](
    LocalFlow.Config(
      issuer = Provider("webappstub:signup").uri,
      secretKey = auth.internal.serverSecret.getOrElse(
        JwkGenerate.symmetric[SyncIO]().unsafeRunSync()
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
