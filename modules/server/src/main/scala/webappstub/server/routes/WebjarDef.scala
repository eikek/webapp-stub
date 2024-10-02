package webappstub.server.routes

import htmx4s.http4s.WebjarRoute.Webjar

object WebjarDef:

  private def makeWebjar(uri: String, a: Webjars.Artifact, path: String = "") =
    Webjar(uri)(a.name, a.version, path)

  val webjars = Seq(
    // refers to our own js and css stuff, version is not needed
    Webjar("self")("webappstub-server", "", ""),
    makeWebjar("htmx", Webjars.htmxorg, "dist"),
    makeWebjar("htmx-rt", Webjars.htmxextresponsetargets),
    makeWebjar("fa", Webjars.fortawesome__fontawesomefree),
    makeWebjar("fi", Webjars.flagicons)
  )
