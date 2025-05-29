package webappstub.backend.auth

import scribe.Scribe
import soidc.core.Logger

final class SoidcLogger[F[_]](scribe: Scribe[F]) extends Logger[F]:
  def debug(message: String): F[Unit] = scribe.warn(s"OpenId: $message")
