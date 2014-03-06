import sbt._
import Keys._

object Build extends sbt.Build {
  import Dependencies._

  val sbsTeamcity = settingKey[Boolean]("'true' if current build is running under TeamCity. This setting should not be modified.")

  val buildVCSNumber = settingKey[String]("Current VCS revision.")

  lazy val sbsSbtBuild = Project("sbt-sbs", file("."), settings = sbsSbtBuildSettings)

  def sbsSbtBuildSettings = Project.defaultSettings ++ Seq(
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
    publishTo <<= version {
      (v) => {
        def r = !v.contains("SNAPSHOT")
       Some(ivyRepo(r))
      }
    },
    buildVCSNumber <<= sbsTeamcity(tc => buildVCSNumberSetting(tc)),
    sbtIdea,
    sbtTeamcity,
    aetherDeploy,
    resolvers ++= Seq(Resolver.sbtPluginRepo("snapshots"), ivyRepo(release = false))
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
    def sbtIdea = addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")
    def sbtTeamcity = addSbtPlugin("org.jetbrains" % "sbt-teamcity-logger" % "0.1.0-SNAPSHOT")
    def aetherDeploy = addSbtPlugin("ke.co.sbsproperties.sbt" % "aether-deploy" % "0.12-SNAPSHOT") //TODO use repackaged aether-deploy until upstream bug fixed
  }

}
