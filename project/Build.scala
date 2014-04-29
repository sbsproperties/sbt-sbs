import sbt._
import Keys._
import ScriptedPlugin._
import com.typesafe.sbt.SbtPgp._


object Build extends sbt.Build {
  import Dependencies._

  val sbsTeamcity = settingKey[Boolean]("'true' if current build is running under TeamCity. This setting should not be modified.")

  val buildVCSNumber = settingKey[String]("Current VCS revision.")

  lazy val sbsSbtBuild = Project("sbt-sbs", file("."))
    .settings(sbsSbtBuildSettings: _*)

  def sbsSbtBuildSettings = ScriptedPlugin.scriptedSettings ++ Seq[Setting[_]](
    name := "SBT Sbs",
    version := s"1.0-SNAPSHOT+${buildVCSNumber.value}",
    organization := "ke.co.sbsproperties",
    description := "SBT Plugin for quick configuration of an SBS project.",
    startYear := Some(2014),
    scmInfo := Some(ScmInfo(url("https://github.com/sbsproperties/sbt-sbs"),
      "scm:git:https://github.com/sbsproperties/sbt-sbs.git")),
    sbtPlugin := true,
    publishMavenStyle := false,
    sbsTeamcity := teamcity,
    publish <<= PgpKeys.publishSigned,
    publishTo <<= version {
      (v) => {
        def r = !v.contains("SNAPSHOT")
       Some(ivyRepo(r))
      }
    },
    buildVCSNumber <<= sbsTeamcity(tc => buildVCSNumberSetting(tc)),
    sbtBuildInfo, sbtPgp, sbtTeamcity, aetherDeploy,
    resolvers ++= Seq(Resolver.sbtPluginRepo("snapshots"), ivyRepo(release = true), ivyRepo(release = false)),
    scalacOptions in Compile += "-deprecation",
    scriptedRun <<= scriptedRun.dependsOn(publishLocal),
    scriptedLaunchOpts <+= version { "-Dproject.version=" + _ }
  )

  def teamcity: Boolean = if (sys.env.get("TEAMCITY_VERSION").isEmpty) false else true

  def ivyRepo(release: Boolean): URLRepository = {
    def root = "https://dev.sbsproperties.co.ke/repo"
    def status = if (release) "release" else "snapshot"
    Resolver.url(s"SBS Ivy $status Repo", url(s"$root/ivy-$status"))(Resolver.ivyStylePatterns)
  }

  def buildVCSNumberSetting(teamcity: Boolean) = (if (teamcity) sys.env.get("BUILD_VCS_NUMBER").get
    else Process("git rev-parse HEAD").lines.head).take(7)

  object Dependencies {
    val sbtBuildInfo = addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.1")
    val sbtPgp = addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")
    val sbtTeamcity = addSbtPlugin("org.jetbrains" % "sbt-teamcity-logger" % "0.1.0-SNAPSHOT")
    val aetherDeploy = addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.12-M1-sbs") //TODO revert to upstream binary when available
  }

}
