package webappstub.server.routes.contacts

import webappstub.common.model

import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.http4s.dsl.impl.QueryParamDecoderMatcher

object Params:
  given QueryParamDecoder[model.ContactId] =
    QueryParamDecoder[Long].map(model.ContactId(_))

  object Query extends OptionalQueryParamDecoderMatcher[String]("q")

  object Page extends OptionalQueryParamDecoderMatcher[Int]("page")

  object Email extends QueryParamDecoderMatcher[String]("email")

  object IdOpt extends OptionalQueryParamDecoderMatcher[model.ContactId]("id")

  object ContactId {
    def unapply(s: String): Option[model.ContactId] =
      s.toLongOption.map(model.ContactId(_))
  }
