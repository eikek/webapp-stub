package webappstub.backend

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.*
import webappstub.common.model.*
import webappstub.store.AccountRepo

import soidc.jwt.*

trait LoginService[F[_]]:
  def loginInternal(up: UserPass): F[LoginResult]
  def loginRememberMe(token: RememberMeToken): F[LoginResult]
  def autoLogin: F[LoginResult]
  def loginExternal(token: AuthToken): F[LoginResult]

object LoginService:

  def apply[F[_]: Sync](
      cfg: AuthConfig,
      repo: AccountRepo[F],
      realms: ConfiguredRealms[F]
  ): LoginService[F] =
    new LoginService[F] {
      private val logger = scribe.cats.effect[F]

      def loginExternal(token: AuthToken): F[LoginResult] =
        ExternalAccountId.fromToken(token) match
          case None => LoginResult.InvalidAuth.pure[F]
          case Some(id) =>
            repo.findByExternalId(id).map {
              case Some(_) => LoginResult.Success(token, None)
              case None    => LoginResult.AccountMissing
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
              (realms.localRealm.makeToken(a.id), rme).mapN(LoginResult.Success(_, _))
        yield result

      def autoLogin: F[LoginResult] =
        for
          acc1 <- repo.findByLogin(LoginName.autoUser, Some(AccountState.Active))
          _ <- logger.debug(s"Found account: $acc1")
          result <- acc1 match
            case None => LoginResult.AccountMissing.pure[F]
            case Some(a) =>
              realms.localRealm
                .makeToken(a.id)
                .map(LoginResult.Success(_, None))
        yield result

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
                    realms.localRealm
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
          jwt <- realms.rememberMeRealm.createToken(
            JoseHeader.jwt,
            SimpleClaims.empty.withSubject(
              StringOrUri(rmeTok.value)
            )
          )
        yield jwt
    }
