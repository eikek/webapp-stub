package webappstub.backend

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.*
import webappstub.common.model.*
import webappstub.store.AccountRepo

trait LoginService[F[_]]:
  def loginSession(session: SessionInfo): F[LoginResult]
  def loginSessionOnly(token: String): F[LoginResult]
  def loginRememberMe(token: String): F[LoginResult]
  def loginUserPass(up: UserPass): F[LoginResult]
  def autoLogin: F[LoginResult]

object LoginService:

  def apply[F[_]: Sync](cfg: AuthConfig, repo: AccountRepo[F]): LoginService[F] =
    new LoginService[F] {
      private val logger = scribe.cats.effect[F]

      def loginSession(session: SessionInfo): F[LoginResult] =
        logger.debug(s"Login with: $session").flatMap { _ =>
          session match
            case SessionInfo.SessionOnly(token) => loginSessionOnly(token)
            case SessionInfo.RememberMe(token)  => loginRememberMe(token)
            case SessionInfo.Session(st, rt) =>
              loginSessionOnly(st).flatMap {
                case r @ LoginResult.Success(_, _) => r.pure[F]
                case LoginResult.InvalidAuth       => loginRememberMe(rt)
              }
        }

      def loginSessionOnly(token: String): F[LoginResult] =
        AuthToken.fromString(token) match {
          case Right(at) =>
            at.validate(cfg.serverSecret, cfg.sessionValid).flatMap {
              case true =>
                AuthToken.refresh(at, cfg.serverSecret).map(LoginResult.Success(_, None))
              case false => LoginResult.InvalidAuth.pure[F]
            }
          case Left(_) =>
            LoginResult.InvalidAuth.pure[F]
        }

      def loginRememberMe(token: String): F[LoginResult] =
        if (!cfg.rememberMeEnabled) LoginResult.InvalidAuth.pure[F]
        else
          RememberMeToken.fromString(token) match {
            case Left(_) => LoginResult.InvalidAuth.pure[F]
            case Right(rt) =>
              if (rt.validate(cfg.serverSecret))
                repo.findByRememberMe(rt.value, cfg.rememberMeValid).flatMap {
                  case Some(account) =>
                    logger.debug(s"Found account $account for rememberme") >>
                      repo.incrementRememberMeUse(rt.value) >>
                      AuthToken
                        .of(account.id, cfg.serverSecret)
                        .map(at => LoginResult.Success(at, Some(rt)))

                  case None =>
                    logger
                      .debug(s"No account found for remember me token: ${rt.value}") >>
                      repo.deleteRememberMe(rt.value).as(LoginResult.InvalidAuth)
                }
              else LoginResult.InvalidAuth.pure[F]
          }

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
              val rememberMe =
                if (cfg.rememberMeEnabled && up.rememberMe)
                  repo
                    .createRememberMe(a.id)
                    .flatMap(rk => RememberMeToken.of(rk, cfg.serverSecret).map(Some(_)))
                else None.pure[F]

              rememberMe.flatMap { rme =>
                AuthToken
                  .of(a.id, cfg.serverSecret)
                  .map(LoginResult.Success(_, rme))
              }
        yield result

      def loginFixed: F[LoginResult] =
        for
          acc1 <- repo.findByLogin(LoginName.autoUser, Some(AccountState.Active))
          _ <- logger.debug(s"Found account: $acc1")
          result <- acc1 match
            case None => LoginResult.InvalidAuth.pure[F]
            case Some(a) =>
              AuthToken
                .of(a.id, cfg.serverSecret)
                .map(LoginResult.Success(_, None))
        yield result
    }
