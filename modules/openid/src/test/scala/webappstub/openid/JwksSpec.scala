package webappstub.openid

import munit.FunSuite

class JwksSpec extends FunSuite with JwtResources:
  test("parse jwks json"):
    assertEquals(jwks.keys.size, 2)
    assertEquals(
      jwks.keys.head.keyId,
      KeyId("2Jsu6JN8JoAW3gfPF32Gsdxo6Am1yGj82wwCK1qvijs")
    )
    assertEquals(
      jwks.keys(1).keyId,
      KeyId("0DHVaoi07_a0k_oAZux5nhjvcA_aiS5MkfXx72WF460")
    )

  test("get public key"):
    jwks.keys.foreach {
      case k: JsonWebKey.Rsa =>
        assert(k.toPublicKey.isRight)
      case k: JsonWebKey.Ec =>
        assert(k.toPublicKey.isRight)
      case k: JsonWebKey.Okp =>
        assert(k.toPublicKey.isLeft)
    }

  test("validation rsa"):
    val header = JwtBorer.readHeader(jwToken).fold(throw _, identity)
    val pubKey = jwks.findPublicKey(header).fold(throw _, identity)
    val result = JwtBorer(fixedClock).decodeAll(jwToken, pubKey)
    assert(result.isSuccess)
    assertEquals(
      result.get._2.issuer,
      Some("http://wasdev:8180/realms/MyRealm")
    )

  test("validation rsa in IO"):
    import cats.effect.IO
    import cats.effect.unsafe.implicits.*

    val clock = TestClock.fixedAt(jwTokenValidTime)
    val result = jwks.validate[IO](clock)(jwToken).unsafeRunSync()
    assert(result.isRight)
    assertEquals(
      result.fold(throw _, identity).issuer,
      Some("http://wasdev:8180/realms/MyRealm")
    )
