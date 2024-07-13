import scala.sys.process._
import com.github.sbt.git.SbtGit.GitKeys._

val writeVersion = taskKey[Unit]("Write version into a file for CI to pick up")

val scalafixSettings = Seq(
  semanticdbEnabled := true, // enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision
)

val sharedSettings = Seq(
  organization := "com.github.eikek",
  scalaVersion := Dependencies.V.scala3,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-language:higherKinds",
    "-feature",
    "-Werror", // fail when there are warnings
    "-unchecked",
    "-Wunused:imports",
    "-Wunused:locals",
    "-Wunused:explicits",
    "-Wvalue-discard"
  ),
  Compile / console / scalacOptions := Seq(),
  Test / console / scalacOptions := Seq()
) ++ scalafixSettings

val testSettingsMUnit = Seq(
  libraryDependencies ++= Dependencies.munit.map(_ % Test),
  testFrameworks += TestFrameworks.MUnit
)

val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    gitHeadCommit,
    gitHeadCommitDate,
    gitUncommittedChanges,
    gitDescribedVersion
  ),
  buildInfoOptions += BuildInfoOption.ToJson,
  buildInfoOptions += BuildInfoOption.BuildTime
)

val common = project
  .in(file("modules/common"))
  .disablePlugins(RevolverPlugin)
  .settings(sharedSettings)
  .settings(testSettingsMUnit)
  .settings(
    name := "webappstub-common",
    libraryDependencies ++=
      Dependencies.fs2 ++ Dependencies.borer
  )

val store = project
  .in(file("modules/store"))
  .disablePlugins(RevolverPlugin)
  .settings(sharedSettings)
  .settings(testSettingsMUnit)
  .settings(
    name := "webappstub-store",
    libraryDependencies ++=
      Dependencies.skunk ++
        Dependencies.scribe ++
        Dependencies.fs2
  )
  .dependsOn(common)

val backend = project
  .in(file("modules/backend"))
  .disablePlugins(RevolverPlugin)
  .settings(sharedSettings)
  .settings(testSettingsMUnit)
  .settings(
    name := "webappstub-backend",
    libraryDependencies ++= Dependencies.fs2 ++ Dependencies.bcrypt
  )
  .dependsOn(common, store % "compile->compile;test->test")

val server = project
  .in(file("modules/server"))
  .enablePlugins(
    RevolverPlugin,
    TailwindCssPlugin,
    JsPlugin,
    BuildInfoPlugin,
    JavaServerAppPackaging,
    SystemdPlugin,
    ClasspathJarPlugin
  )
  .settings(sharedSettings)
  .settings(testSettingsMUnit)
  .settings(buildInfoSettings)
  .settings(
    name := "webappstub-server",
    libraryDependencies ++=
      Dependencies.http4sServer ++
        Dependencies.htmx4s ++
        Dependencies.ciris ++
        Dependencies.scribe ++
        Dependencies.scribeSlf4j ++
        Dependencies.webjars,
    buildInfoPackage := "webappstub.server",
    reStart / javaOptions ++=
      Seq(
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005",
        "-Xmx512M"
      ),
    Compile / sourceGenerators += Def.task {
      createWebjarSource(Dependencies.webjars, (Compile / sourceManaged).value)
    }.taskValue,
    watchSources += Watched.WatchSource(
      (Compile / sourceDirectory).value / "js",
      FileFilter.globFilter("*.js"),
      HiddenFileFilter
    ),
    watchSources += Watched.WatchSource(
      (Compile / sourceDirectory).value / "css",
      FileFilter.globFilter("*.css"),
      HiddenFileFilter
    ),
    Compile / resourceGenerators += Def.task {
      val logger = streams.value.log
      copyWebjarResources(
        Seq((Compile / sourceDirectory).value / "webjar"),
        (Compile / resourceManaged).value,
        name.value,
        version.value,
        logger
      )
    }.taskValue
  )
  .dependsOn(common, backend)

val root = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(
    name := "webappstub-root",
    writeVersion := {
      val out = (LocalRootProject / target).value / "version.txt"
      val versionStr = version.value
      IO.write(out, versionStr)
    }
  )
  .aggregate(
    common,
    store,
    backend,
    server
  )

def copyWebjarResources(
    src: Seq[File],
    base: File,
    artifact: String,
    version: String,
    logger: Logger
): Seq[File] = {
  val targetDir = base / "META-INF" / "resources" / "webjars" / artifact
  src.flatMap { dir =>
    if (dir.isDirectory) {
      val files = (dir ** "*").filter(_.isFile).get.pair(Path.relativeTo(dir))
      files.map { case (f, name) =>
        val target = targetDir / name
        logger.info(s"Copy $f -> $target")
        IO.createDirectories(Seq(target.getParentFile))
        IO.copy(Seq(f -> target))
        target
      }
    } else {
      val target = targetDir / dir.name
      logger.info(s"Copy $dir -> $target")
      IO.createDirectories(Seq(target.getParentFile))
      IO.copy(Seq(dir -> target))
      Seq(target)
    }
  }
}

def createWebjarSource(wj: Seq[ModuleID], out: File): Seq[File] = {
  val target = out / "Webjars.scala"
  val invalidChars = "-.".toSet
  val fields = wj
    .map(m =>
      s"""  val ${m.name.toLowerCase
          .filterNot(invalidChars.contains)} = Artifact("${m.name}", "${m.revision}")"""
    )
    .mkString("\n\n")
  val content = s"""package webappstub.server.routes
                   |object Webjars {
                   |  case class Artifact(name: String, version: String)
                   |
                   |$fields
                   |}
                   |""".stripMargin

  IO.write(target, content)
  Seq(target)
}

addCommandAlias("ci", "Test/compile; lint; test")
addCommandAlias(
  "lint",
  "scalafmtSbtCheck; scalafmtCheckAll; restapi/openapiLint; Compile/scalafix --check; Test/scalafix --check"
)
addCommandAlias("fix", "Compile/scalafix; Test/scalafix; scalafmtSbt; scalafmtAll")
addCommandAlias("make-pkg", "server/Universal/packageBin")
