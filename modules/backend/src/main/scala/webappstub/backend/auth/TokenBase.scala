package webappstub.backend.auth

import java.time.Instant

import scala.concurrent.duration.*

import cats.effect.Sync
import cats.effect.std.Random
import cats.syntax.all.*

import webappstub.common.SignUtil

import scodec.bits.ByteVector

final private[auth] case class TokenBase[A](
    v: A,
    created: Instant,
    salt: ByteVector,
    signature: ByteVector
)(using sd: SignedData[A]):
  val value: A = v

  def asString: String = {
    val b64 = sd.encode(v).toBase64
    val sig = signature.toBase64
    val millis = created.toEpochMilli()
    val s = salt.toBase64
    s"$millis-$b64-$s-$sig"
  }

  def sigValid(key: ByteVector): Boolean = {
    val newSig = TokenBase.sign(this, key)
    TokenBase.constTimeEq(signature, newSig)
  }
  def sigInvalid(key: ByteVector): Boolean = !sigValid(key)

  def isExpired[F[_]: Sync](validity: Duration): F[Boolean] = {
    val ends = created.plusMillis(validity.toMillis)
    Sync[F].delay(Instant.now).map(_.isAfter(ends))
  }
  def notExpired[F[_]: Sync](validity: Duration): F[Boolean] = isExpired(validity).map(!_)

  def validate[F[_]: Sync](key: ByteVector, validity: Duration): F[Boolean] =
    notExpired(validity).map(_ && sigValid(key))

private[auth] object TokenBase:
  def fromString[A](s: String)(using sd: SignedData[A]): Either[String, TokenBase[A]] =
    s.split("\\-", 4) match {
      case Array(ms, as, salt, sig) =>
        for {
          millis <- ms.toLongOption.toRight("Cannot read authenticator data")
          created = Instant.ofEpochMilli(millis)
          value <- ByteVector
            .fromBase64Descriptive(as)
            .flatMap(sd.decode)
          s <- ByteVector.fromBase64Descriptive(salt)
          signature <- ByteVector.fromBase64Descriptive(sig)
        } yield TokenBase(value, created, s, signature)

      case _ =>
        Left("Invalid authenticator")
    }

  def of[F[_]: Sync, A: SignedData](value: A, key: ByteVector): F[TokenBase[A]] =
    for {
      salt <- genSalt[F]
      created <- Sync[F].delay(Instant.now)
      token = TokenBase(value, created, salt, ByteVector.empty)
      sig = sign(token, key)
    } yield token.copy(signature = sig)

  private def sign[A](token: TokenBase[A], key: ByteVector)(using
      sd: SignedData[A]
  ): ByteVector = {
    val millis = ByteVector.fromLong(token.created.toEpochMilli())
    val value = sd.encode(token.value)
    val raw = millis ++ value ++ token.salt
    SignUtil.signBytes(raw, key)
  }

  private def constTimeEq(s1: ByteVector, s2: ByteVector): Boolean =
    s1.length == s2.length && s1
      .zipWith(s2)((b1, b2) => if (b1 == b2) 0 else 1)
      .foldLeft(true)((r, b) => r && b == 0)

  private def genSalt[F[_]: Sync]: F[ByteVector] =
    Random.scalaUtilRandom[F].flatMap(_.nextBytes(16)).map(ByteVector.view)
