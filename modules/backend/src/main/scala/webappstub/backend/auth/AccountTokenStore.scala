package webappstub.backend.auth

import cats.Monad
import cats.data.OptionT

import webappstub.common.model.{ExternalAccountId, Provider}
import webappstub.store.AccountRepo

import soidc.jwt.JWS

final class AccountTokenStore[F[_]: Monad](repo: AccountRepo[F])
    extends WebappstubTokenStore[F]:

  def getRefreshToken(jwt: AuthToken): F[Option[JWS]] =
    OptionT
      .fromOption[F](getKey(jwt))
      .flatMapF(repo.findByExternalId)
      .subflatMap(_.refreshToken)
      .value

  def setRefreshToken(jwt: AuthToken, refreshToken: JWS): F[Unit] =
    ???

  private def getKey(jwt: AuthToken) =
    for
      iss <- jwt.claims.issuer
      sub <- jwt.claims.subject
    yield ExternalAccountId(Provider(iss.value), sub.value)
