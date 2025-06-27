package webappstub.server.common

import org.http4s.QueryParamDecoder

trait ParamDecoders:

  given QueryParamDecoder[Boolean] =
    QueryParamDecoder.stringQueryParamDecoder.map(_.toLowerCase()).map {
      case "true" => true
      case "on"   => true
      case _      => false
    }
