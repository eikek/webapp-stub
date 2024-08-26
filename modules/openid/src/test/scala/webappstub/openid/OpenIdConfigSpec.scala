package webappstub.openid

import io.bullet.borer.Json
import munit.FunSuite
import org.http4s.implicits.*

class OpenIdConfigSpec extends FunSuite with JwtResources:

  test("parse json"):
    val decoded = Json.decode(configEndpointData.getBytes()).to[OpenIdConfig].value
    assertEquals(
      decoded.authorizationEndpoint,
      uri"http://wasdev:8180/realms/MyRealm/protocol/openid-connect/auth"
    )
    assertEquals(
      decoded.issuer,
      uri"http://wasdev:8180/realms/MyRealm"
    )
    assertEquals(
      decoded.jwksUri,
      uri"http://wasdev:8180/realms/MyRealm/protocol/openid-connect/certs"
    )
    assert(decoded.authorizationSigningAlgSupported.contains("RS512"))
