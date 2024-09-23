package webappstub.backend

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.*
import webappstub.common.model.*
import webappstub.store.AccountRepo

import soidc.borer.given
import soidc.core.JwkGenerate
import soidc.core.JwtValidator
import soidc.core.LocalFlow
import soidc.jwt.*

trait LoginService[F[_]]:
  def realm: WebappstubRealm[F]
  def loginUserPass(up: UserPass): F[LoginResult]
  def loginRememberMe(token: RememberMeToken): F[LoginResult]
  def rememberMeValidator: JwtValidator[F, JoseHeader, SimpleClaims]
  def autoLogin: F[LoginResult]

object LoginService:

  def apply[F[_]: Sync](cfg: AuthConfig, repo: AccountRepo[F]): LoginService[F] =
    new LoginService[F] {
      private val logger = scribe.cats.effect[F]
      private val secret =
        cfg.serverSecret.getOrElse(JwkGenerate.symmetric[SyncIO](16).unsafeRunSync())

      private val localRealm =
        LocalFlow[F, JoseHeader, SimpleClaims](
          LocalFlow.Config(
            issuer = StringOrUri("webappstub-app"),
            secretKey = secret,
            sessionValidTime = cfg.sessionValid
          )
        )
      private val rmeRealm =
        LocalFlow[F, JoseHeader, SimpleClaims](
          LocalFlow.Config(
            issuer = StringOrUri("webappstub-app"),
            secretKey = secret,
            sessionValidTime = cfg.rememberMeValid
          )
        )

      def realm: WebappstubRealm[F] =
        localRealm

      def loginUserPass(up: UserPass): F[LoginResult] = cfg.authType match
        case AuthConfig.AuthenticationType.Internal =>
          loginInternal(up)

        case AuthConfig.AuthenticationType.Fixed =>
          loginFixed

      def autoLogin: F[LoginResult] = loginFixed

      def loginInternal(up: UserPass): F[LoginResult] =
        for
          acc1 <- repo.findByLogin(up.login, Some(AccountState.Active))
          _ <- logger.debug(s"Found account: $acc1")
          result <- acc1 match
            case None => LoginResult.InvalidAuth.pure[F]
            case Some(a) if !PasswordCrypt.check(up.password, a.password) =>
              LoginResult.InvalidAuth.pure[F]
            case Some(a) =>
              val rme =
                if (cfg.rememberMeEnabled && up.rememberMe)
                  makeRememberMeToken(a.id).map(_.some)
                else None.pure[F]
              (localRealm.makeToken(a.id), rme).mapN(LoginResult.Success(_, _))
        yield result

      def loginFixed: F[LoginResult] =
        for
          acc1 <- repo.findByLogin(LoginName.autoUser, Some(AccountState.Active))
          _ <- logger.debug(s"Found account: $acc1")
          result <- acc1 match
            case None => LoginResult.InvalidAuth.pure[F]
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
              repo.findByRememberMe(rkey, cfg.rememberMeValid).flatMap {
                case Some(account) =>
                  logger.debug(s"Found account $account for rememberme") >>
                    repo.incrementRememberMeUse(rkey) >>
                    localRealm
                      .makeToken(account.id)
                      .map(at => LoginResult.Success(at, Some(token)))

                case None =>
                  logger
                    .debug(s"No account found for remember me key: ${rkey}") >>
                    repo.deleteRememberMe(rkey).as(LoginResult.InvalidAuth)
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
