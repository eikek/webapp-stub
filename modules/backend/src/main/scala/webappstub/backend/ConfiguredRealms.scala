package webappstub.backend

import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.auth.*
import webappstub.common.model.AuthToken
import webappstub.common.model.Provider
import webappstub.store.AccountRepo

import org.http4s.Uri
import org.http4s.client.Client
import soidc.borer.given
import soidc.core.model.LogoutRequest
import soidc.core.{AuthorizationCodeFlow as ACF, *}
import soidc.http4s.client.Http4sClient
import soidc.jwt.{Uri as JwtUri, *}

trait ConfiguredRealms[F[_]] extends WebappstubRealm[F]:
  def localRealm: InternalRealm[F]
  def rememberMeRealm: InternalRealm[F]
  def openIdRealms: Map[String, OpenIdRealm[F]]
  def githubAuth: Option[(String, GitHubOAuth[F])]
  def endSessionUri(jws: AuthToken, req: LogoutRequest): F[Option[Uri]]

object ConfiguredRealms:
  val githubUri: JwtUri = JwtUri.unsafeFromString("https://github.com")

  def apply[F[_]: Sync](
      cfg: AuthConfig,
      repo: AccountRepo[F],
      client: Client[F]
  ): F[ConfiguredRealms[F]] = {
    val logger = scribe.cats.effect[F]
    val secret =
      cfg.internal.serverSecret.getOrElse(
        JwkGenerate.symmetricSign[SyncIO](16).unsafeRunSync()
      )

    val openIdRealms = cfg.openId.toList
      .filter(_._2.providerUrl != githubUri)
      .traverse { case (name, c) =>
        val acfCfg = ACF.Config(
          c.clientId,
          c.clientSecret.some,
          c.providerUrl,
          secret,
          c.scope
        )
        ACF(acfCfg, Http4sClient(client), AccountTokenStore[F](repo), SoidcLogger(logger))
          .map(a => name -> a)
      }
      .map(_.toMap)

    openIdRealms.map { oidr =>
      new ConfiguredRealms[F] {
        val localRealm =
          LocalFlow[F, JoseHeader, SimpleClaims](
            LocalFlow.Config(
              issuer = Provider.internal.uri,
              secretKey = secret,
              sessionValidTime = cfg.internal.sessionValid
            )
          )
        val rememberMeRealm =
          LocalFlow[F, JoseHeader, SimpleClaims](
            LocalFlow.Config(
              issuer = Provider.internal.uri,
              secretKey = secret,
              sessionValidTime = cfg.internal.rememberMeValid
            )
          )

        val openIdRealms: Map[String, OpenIdRealm[F]] = oidr

        val githubAuth: Option[(String, GitHubOAuth[F])] = cfg.openId
          .find(_._2.providerUrl == githubUri)
          .map { case (name, oid) =>
            name -> GitHubOAuth(
              GitHubOAuth.Config(oid.clientId, secret, oid.clientSecret.some, oid.scope),
              Http4sClient(client),
              SoidcLogger(logger)
            )
          }

        val allRealms = localRealm +: openIdRealms.values.toSeq
        val combinedRealm = allRealms.combineAll

        def endSessionUri(jws: AuthToken, req: LogoutRequest): F[Option[Uri]] =
          OptionT
            .fromOption[F](oidr.values.find(_.isIssuer(jws)))
            .flatMapF(_.logoutUrl(req))
            .map(u => Uri.unsafeFromString(u.value))
            .value

        def isIssuer(jws: JWSDecoded[JoseHeader, SimpleClaims])(using
            StandardClaims[SimpleClaims]
        ): Boolean = combinedRealm.isIssuer(jws)

        def jwtRefresh: JwtRefresh[F, JoseHeader, SimpleClaims] = combinedRealm.jwtRefresh
        def validator: JwtValidator[F, JoseHeader, SimpleClaims] = combinedRealm.validator
      }
    }
  }
