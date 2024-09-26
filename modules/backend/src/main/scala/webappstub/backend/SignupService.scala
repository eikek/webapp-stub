package webappstub.backend

import cats.Applicative
import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.PasswordCrypt
import webappstub.backend.signup.*
import webappstub.common.model.*
import webappstub.store.AccountRepo

trait SignupService[F[_]]:
  def signup(req: SignupRequest): F[SignupResult]
  def createInviteKey(serverPassword: Password): F[Option[InviteKey]]

object SignupService:
  def apply[F[_]: Sync](cfg: SignupConfig, repo: AccountRepo[F]): SignupService[F] =
    cfg.mode match
      case SignupMode.Closed                => closed[F]
      case SignupMode.Open                  => open[F](repo)
      case SignupMode.WithInvite(serverKey) => withInvite(repo, serverKey)

  private def closed[F[_]: Applicative]: SignupService[F] =
    new SignupService[F]:
      def signup(req: SignupRequest): F[SignupResult] = SignupResult.SignupClosed.pure[F]
      def createInviteKey(serverPassword: Password): F[Option[InviteKey]] = None.pure[F]

  private def open[F[_]: Sync](repo: AccountRepo[F]): SignupService[F] =
    new SignupService[F]:
      def signup(req: SignupRequest): F[SignupResult] = createAccount(repo, req)
      def createInviteKey(serverPassword: Password): F[Option[InviteKey]] = None.pure[F]

  private def withInvite[F[_]: Sync](
      repo: AccountRepo[F],
      serverKey: Password
  ): SignupService[F] =
    new SignupService[F]:
      def signup(req: SignupRequest): F[SignupResult] =
        req.inviteKey match
          case None => SignupResult.InvalidKey.pure[F]
          case Some(key) =>
            repo
              .withInvite(key) {
                case None    => Left(SignupResult.InvalidKey).pure[F]
                case Some(_) => createAccount(repo, req).map(_.toEither)
              }
              .map(_.fold(identity, identity))

      def createInviteKey(serverPassword: Password): F[Option[InviteKey]] =
        if (serverKey != serverPassword) None.pure[F]
        else repo.createInviteKey.map(_.some)

  private def createAccount[F[_]: Sync](
      repo: AccountRepo[F],
      req: SignupRequest
  ): F[SignupResult] =
    repo
      .insert(NewAccount.internalActive(req.login, PasswordCrypt.crypt(req.password)))
      .map(_.fold(SignupResult.LoginExists)(SignupResult.Success(_)))
