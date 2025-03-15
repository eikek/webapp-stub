package webappstub.common.model

import soidc.jwt.StringOrUri
import soidc.jwt.codec.FromJson

opaque type Provider = String

object Provider:
  val internal: Provider = "webappstub:internal"
  val github: Provider = "https://github.com"

  def apply(p: String): Provider = p

  given FromJson[Provider] =
    FromJson.strm(Right(_))

  extension (self: Provider)
    def value: String = self
    def uri: StringOrUri = StringOrUri(self)
