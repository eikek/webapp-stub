package webappstub.backend.auth

import cats.Invariant
import cats.syntax.all.*

import webappstub.common.model.*

import scodec.bits.ByteVector

trait SignedData[A]:
  def encode(value: A): ByteVector
  def decode(bv: ByteVector): Either[String, A]

object SignedData:

  def create[A](
      enc: A => ByteVector,
      dec: ByteVector => Either[String, A]
  ): SignedData[A] =
    new SignedData[A] {
      def encode(value: A): ByteVector = enc(value)
      def decode(bv: ByteVector): Either[String, A] = dec(bv)
    }

  given Invariant[SignedData] =
    new Invariant[SignedData] {
      def imap[A, B](fa: SignedData[A])(f: A => B)(g: B => A): SignedData[B] =
        create(b => fa.encode(g(b)), bv => fa.decode(bv).map(f))
    }

  extension [A](self: SignedData[A])
    def eimap[B](f: A => Either[String, B])(g: B => A): SignedData[B] =
      create(b => self.encode(g(b)), bv => self.decode(bv).flatMap(f))

  given SignedData[ByteVector] =
    create(identity, Right(_))

  given (using sd: SignedData[ByteVector]): SignedData[String] =
    sd.eimap(bv => bv.decodeUtf8.leftMap(_.getMessage))(s => ByteVector.view(s.getBytes))

  given (using sd: SignedData[String]): SignedData[AccountId] =
    sd.eimap(AccountId.fromString)(_.value.toString)

  given (using sd: SignedData[String]): SignedData[RememberMeKey] =
    sd.eimap(RememberMeKey.fromString)(_.value)
