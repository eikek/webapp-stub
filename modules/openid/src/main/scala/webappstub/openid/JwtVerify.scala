package webappstub.openid

import cats.Applicative
import cats.effect.*

import org.http4s.client.Client

trait JwtVerify[F[_]]:
  def verify(token: String): F[Either[JwtError, WebappJwtToken]]

object JwtVerify:
  def apply[F[_]: Async](client: Client[F], config: JwtVerifyConfig): F[JwtVerify[F]] =
    val clock = Clock[F]
    DefaultJwtVerify[F](client, clock, config)

  def fixed[F[_]: Applicative](result: JwtError | WebappJwtToken): JwtVerify[F] =
    new JwtVerify[F] {
      def verify(token: String): F[Either[JwtError, WebappJwtToken]] =
        Applicative[F].pure(result match
          case a: JwtError       => Left(a)
          case b: WebappJwtToken => Right(b)
        )
    }
