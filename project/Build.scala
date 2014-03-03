import sbt._
import Keys._

object Build extends sbt.Build {

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
    credentials <<= (sbsTeamcity, Keys.credentials) map ((teamcity, credentials) =>
      if (teamcity) Seq(credentialsFromEnv("SBS_PUBLISH_REALM", "SBS_PUBLISH_USER", "SBS_PUBLISH_KEY", "dev.sbsproperties.co.ke")) else credentials),
    publishTo <<= version {
      (v) =>
        val root = "https://dev.sbsproperties.co.ke/repo/"
        val (name, url) = if (v.contains("-SNAPSHOT")) ("SBS Ivy Snapshots", root + "ivy-snapshot") else ("SBS Ivy Releases", root + "ivy-release")
        Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
    },
    buildVCSNumber <<= sbsTeamcity(tc => buildVCSNumberSetting(tc)),
    Dependencies.sbtIdea
  )

  def teamcity: Boolean = if (sys.env.get("TEAMCITY_VERSION").isEmpty) false else true

  def credentialsFromEnv(realmEnv: String, userEnv: String, keyEnv: String, host: String): Credentials = {
    def e(key: String) = sys.env.get(key).getOrElse("UNDEFINED")

    if (e(keyEnv).startsWith("UNDEF")) {
      println(s"##teamcity[message text='Required publishing authentication key not defined at env.$keyEnv. Credentials likely invalid.' status='WARNING']")
      Credentials(e(realmEnv), host, e(userEnv), e(keyEnv))
    }
    Credentials(e(realmEnv), host, e(userEnv), e(keyEnv))
  }

  def buildVCSNumberSetting(teamcity: Boolean) = (if (teamcity) sys.env.get("BUILD_VCS_NUMBER").get
    else Process("git rev-parse HEAD").lines.head).take(7)

  object Dependencies {
    def sbtIdea = addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")
  }

}
