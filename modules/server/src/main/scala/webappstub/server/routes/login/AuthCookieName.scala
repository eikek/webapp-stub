package webappstub.server.routes.login

opaque type AuthCookieName = "webappstub_auth"

object AuthCookieName:
  val value: AuthCookieName = "webappstub_auth"

  extension (self: AuthCookieName)
    def asString: String = self
