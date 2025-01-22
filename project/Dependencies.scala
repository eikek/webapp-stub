import sbt._
import sbt.Keys.scalaVersion

object Dependencies {

  object V {
    val scala3 = "3.6.3"

    val borer = "1.15.0"
    val borerCompats = "0.1.0-SNAPSHOT"
    val bcrypt = "0.4"
    val catsEffect = "3.5.4"
    val catsParse = "1.0.0"
    val ciris = "3.7.0"
    val decline = "2.4.1"
    val http4s = "0.23.30"
    val monocle = "3.2.0"
    val munit = "1.1.0"
    val munitCatsEffect = "2.0.0"
    val munitScalaCheck = "1.0.0"
    val scodec2 = "2.2.2"
    val catsCore = "2.9.0"
    val fs2 = "3.11.0"
    val soidc = "0.1.0-SNAPSHOT"
    val scalaCheck = "1.17.0"
    val scribe = "3.16.0"
    val skunk = "1.1.0-M3"
    val fontawesome = "6.7.2"
    val htmx4s = "0.2.1"
    val htmx = "2.0.4"
    val htmxResponseTargets = "2.0.2"
    val http4sScalatags = "0.25.2"
    val flagIcons = "7.3.2"
  }

  val soidcJwt = Seq(
    "com.github.eikek" %% "soidc-jwt" % V.soidc,
    "com.github.eikek" %% "soidc-borer" % V.soidc
  )

  val soidcCore = Seq(
    "com.github.eikek" %% "soidc-core" % V.soidc
  )

  val soidcHttp4sClient = Seq(
    "com.github.eikek" %% "soidc-http4s-client" % V.soidc
  )

  val soidcHttp4sRoutes = Seq(
    "com.github.eikek" %% "soidc-http4s-routes" % V.soidc
  )

  val bcrypt = Seq(
    "org.mindrot" % "jbcrypt" % V.bcrypt
  )

  val htmx4s = Seq(
    "com.github.eikek" %% "htmx4s-constants" % V.htmx4s,
    "com.github.eikek" %% "htmx4s-scalatags" % V.htmx4s,
    "com.github.eikek" %% "htmx4s-http4s" % V.htmx4s,
    "org.http4s" %% "http4s-scalatags" % V.http4sScalatags
  )

  val skunk = Seq(
    "org.tpolecat" %% "skunk-core" % V.skunk
  )

  val monocle = Seq(
    "dev.optics" %% "monocle-core" % V.monocle,
    "dev.optics" %% "monocle-macro" % V.monocle
  )

  val scribe = Seq(
    "com.outr" %% "scribe" % V.scribe,
    "com.outr" %% "scribe-cats" % V.scribe
  )

  val scribeSlf4j = Seq(
    "com.outr" %% "scribe-slf4j" % V.scribe
  )

  val http4sCore = Seq("org.http4s" %% "http4s-core" % V.http4s)

  val http4sClient = Seq(
    "org.http4s" %% "http4s-client" % V.http4s,
    "org.http4s" %% "http4s-ember-client" % V.http4s
  )

  val http4sServer = Seq(
    "org.http4s" %% "http4s-ember-server" % V.http4s,
    "org.http4s" %% "http4s-dsl" % V.http4s
  )

  val ciris = Seq(
    "is.cir" %% "ciris" % V.ciris
  )

  val catsParse = Seq(
    "org.typelevel" %% "cats-parse" % V.catsParse
  )

  val decline = Seq(
    "com.monovore" %% "decline" % V.decline,
    "com.monovore" %% "decline-effect" % V.decline
  )

  val borer = Seq(
    "io.bullet" %% "borer-core" % V.borer,
    "io.bullet" %% "borer-derivation" % V.borer,
    "io.bullet" %% "borer-compat-cats" % V.borer
  )

  val borerScodec = Seq(
    "io.bullet" %% "borer-compat-scodec" % V.borer
  )

  val borerCompatsHttp4s = Seq(
    "com.github.eikek" %% "borer-compats-http4s" % V.borerCompats
  )

  val catsCore = Seq("org.typelevel" %% "cats-core" % V.catsCore)

  val catsEffect = Seq("org.typelevel" %% "cats-effect" % V.catsEffect)

  val fs2Core = Seq("co.fs2" %% "fs2-core" % V.fs2)
  val fs2Io = Seq("co.fs2" %% "fs2-io" % V.fs2)
  val fs2 = fs2Core ++ fs2Io

  val scalacheck = Seq("org.scalacheck" %% "scalacheck" % V.scalaCheck)

  val scodecCore = Seq("org.scodec" %% "scodec-core" % V.scodec2)

  val munit = Seq(
    "org.scalameta" %% "munit" % V.munit,
    "org.scalameta" %% "munit-scalacheck" % V.munitScalaCheck,
    "org.typelevel" %% "munit-cats-effect" % V.munitCatsEffect
  )

  val webjars = Seq(
    "org.webjars.npm" % "htmx.org" % V.htmx,
    "org.webjars.npm" % "htmx-ext-response-targets" % V.htmxResponseTargets,
    "org.webjars.npm" % "fortawesome__fontawesome-free" % V.fontawesome,
    "org.webjars.npm" % "flag-icons" % V.flagIcons
  ).map(
    // transitive deps are not needed, some fail to resolve
    _.excludeAll(
      ExclusionRule("org.webjars.npm")
    )
  )
}
