package webappstub.backend

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.*
import webappstub.common.model.*
import webappstub.store.AccountRepo

trait LoginService[F[_]]:
  def loginSession(sessionKey: String): F[LoginResult]
  def loginUserPass(up: UserPass): F[LoginResult]

object LoginService:

  def apply[F[_]: Sync](cfg: AuthConfig, repo: AccountRepo[F]): LoginService[F] =
    new LoginService[F] {
      private val logger = scribe.cats.effect[F]

      def loginSession(token: String): F[LoginResult] =
        AuthToken.fromString(token) match {
          case Right(at) =>
            if (at.sigInvalid(cfg.serverSecret)) LoginResult.InvalidAuth.pure[F]
            else if (at.isExpired(cfg.sessionValid)) LoginResult.InvalidTime.pure[F]
            else AuthToken.refresh(at, cfg.serverSecret).map(LoginResult.Success(_))
          case Left(_) =>
            LoginResult.InvalidAuth.pure[F]
        }

      def loginUserPass(up: UserPass): F[LoginResult] = cfg.authType match
        case AuthConfig.AuthenticationType.Internal =>
          loginInternal(up)

        case AuthConfig.AuthenticationType.Fixed =>
          loginFixed

      def loginInternal(up: UserPass): F[LoginResult] =
        for
          acc1 <- repo.findByLogin(up.login, Some(AccountState.Active))
          _ <- logger.debug(s"Found account: $acc1")
          result <- acc1 match
            case None => LoginResult.InvalidAuth.pure[F]
            case Some(a) if !PasswordCrypt.check(up.password, a.password) =>
              LoginResult.InvalidAuth.pure[F]
            case Some(a) =>
              AuthToken
                .user(a.id, cfg.serverSecret)
                .map(LoginResult.Success(_))
        yield result

      def loginFixed: F[LoginResult] =
        for
          acc1 <- repo.findByLogin(LoginName.autoUser, Some(AccountState.Active))
          _ <- logger.debug(s"Found account: $acc1")
          result <- acc1 match
            case None => LoginResult.InvalidAuth.pure[F]
            case Some(a) =>
              AuthToken
                .user(a.id, cfg.serverSecret)
                .map(LoginResult.Success(_))
        yield result
    }
