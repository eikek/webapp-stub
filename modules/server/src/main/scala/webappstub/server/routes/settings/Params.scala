package webappstub.server.routes.settings

import org.http4s.dsl.impl.*

object Params:

  object MenuOpen extends FlagQueryParamMatcher("open")
