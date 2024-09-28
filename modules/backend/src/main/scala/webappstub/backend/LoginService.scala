package webappstub.backend

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.*
import webappstub.common.model.*
import webappstub.store.AccountRepo

import org.http4s.Uri
import org.http4s.client.Client
import soidc.borer.given
import soidc.core.AuthorizationCodeFlow as ACF
import soidc.core.JwkGenerate
import soidc.core.JwtValidator
import soidc.core.LocalFlow
import soidc.http4s.client.Http4sClient
import soidc.jwt.{Uri as JwtUri, *}

trait LoginService[F[_]]:
  def internalRealm: WebappstubRealm[F]
  def openIdRealms(
      baseUri: Uri,
      resumeSegment: String = "resume"
  ): F[Map[String, OpenIdRealm[F]]]
  def loginInternal(up: UserPass): F[LoginResult]
  def loginRememberMe(token: RememberMeToken): F[LoginResult]
  def rememberMeValidator: TokenValidator[F]
  def autoLogin: F[LoginResult]
  def loginExternal(token: AuthToken): F[LoginResult]

object LoginService:

  def apply[F[_]: Sync](
      cfg: AuthConfig,
      repo: AccountRepo[F],
      client: Client[F]
  ): LoginService[F] =
    new LoginService[F] {
      private val tokenStore = AccountTokenStore[F](repo)
      private val logger = scribe.cats.effect[F]
      private val secret =
        cfg.internal.serverSecret.getOrElse(
          JwkGenerate.symmetric[SyncIO](16).unsafeRunSync()
        )

      private val localRealm =
        LocalFlow[F, JoseHeader, SimpleClaims](
          LocalFlow.Config(
            issuer = Provider.internal.uri,
            secretKey = secret,
            sessionValidTime = cfg.internal.sessionValid
          )
        )
      private val rmeRealm =
        LocalFlow[F, JoseHeader, SimpleClaims](
          LocalFlow.Config(
            issuer = Provider.internal.uri,
            secretKey = secret,
            sessionValidTime = cfg.internal.rememberMeValid
          )
        )

      def openIdRealms(
          baseUri: Uri,
          resumeSegment: String
      ): F[Map[String, OpenIdRealm[F]]] = cfg.openId.toList
        .traverse { case (name, c) =>
          val acfCfg = ACF.Config(
            c.clientId,
            c.clientSecret.some,
            JwtUri.unsafeFromString((baseUri / resumeSegment).renderString),
            c.providerUrl,
            secret,
            c.scope
          )
          ACF(acfCfg, Http4sClient(client), tokenStore, SoidcLogger(logger)).map(a =>
            name -> a
          )
        }
        .map(_.toMap)

      def internalRealm: WebappstubRealm[F] = localRealm

      def loginExternal(token: AuthToken): F[LoginResult] =
        ExternalAccountId.fromToken(token) match
          case None => LoginResult.InvalidAuth.pure[F]
          case Some(id) =>
            repo.findByExternalId(id).map {
              case Some(_) => LoginResult.Success(token, None)
              case None => LoginResult.AccountMissing
            }

      def loginInternal(up: UserPass): F[LoginResult] =
        for
          acc1 <- repo.findByLogin(up.login, Some(AccountState.Active))
          _ <- logger.debug(s"Found account: $acc1")
          result <- acc1 match
            case None => LoginResult.AccountMissing.pure[F]
            case Some(a) if !PasswordCrypt.check(up.password, a.password) =>
              LoginResult.InvalidAuth.pure[F]
            case Some(a) =>
              val rme =
                if (cfg.rememberMeEnabled && up.rememberMe)
                  makeRememberMeToken(a.id).map(_.some)
                else None.pure[F]
              (localRealm.makeToken(a.id), rme).mapN(LoginResult.Success(_, _))
        yield result

      def autoLogin: F[LoginResult] =
        for
          acc1 <- repo.findByLogin(LoginName.autoUser, Some(AccountState.Active))
          _ <- logger.debug(s"Found account: $acc1")
          result <- acc1 match
            case None => LoginResult.AccountMissing.pure[F]
            case Some(a) =>
              localRealm
                .makeToken(a.id)
                .map(LoginResult.Success(_, None))
        yield result

      def rememberMeValidator: JwtValidator[F, JoseHeader, SimpleClaims] =
        rmeRealm.validator

      def loginRememberMe(token: RememberMeToken): F[LoginResult] =
        if (!cfg.rememberMeEnabled) LoginResult.InvalidAuth.pure[F]
        else
          token.rememberMeKey match
            case None => LoginResult.InvalidAuth.pure[F]
            case Some(rkey) =>
              repo.findByRememberMe(rkey, cfg.internal.rememberMeValid).flatMap {
                case Some(account) =>
                  logger.debug(s"Found account $account for rememberme") >>
                    repo.incrementRememberMeUse(rkey) >>
                    localRealm
                      .makeToken(account.id)
                      .map(at => LoginResult.Success(at, Some(token)))

                case None =>
                  logger
                    .debug(s"No account found for remember me key: ${rkey}") >>
                    repo.deleteRememberMe(rkey).as(LoginResult.AccountMissing)
              }

      private def makeRememberMeToken(account: AccountId) =
        for
          rmeTok <- repo.createRememberMe(account)
          jwt <- rmeRealm.createToken(
            JoseHeader.jwt,
            SimpleClaims.empty.withSubject(
              StringOrUri(rmeTok.value)
            )
          )
        yield jwt
    }
