package ke.co.sbsproperties.sbt

import sbt._
import scala.util.Try


object SBSPlugin extends Plugin {

  sealed trait BuildProfile {
    def expected: String

    def is = sys.props.get("sbs.build.profile").filter(e => e.equalsIgnoreCase(expected)).exists(_ => true)

    def getOption: Option[BuildProfile] = if (this.is) Some(this) else None
  }

  case object ReleaseProfile extends BuildProfile {
    override def expected = "release"
  }

  case object PreReleaseProfile extends BuildProfile {
    override def expected: String = "pre-release"
  }

  case object DevelopmentProfile extends BuildProfile {
    override def expected: String = "development"
  }

  val sbsBuildNumber = SettingKey[String]("sbsBuildNumber", "Build number computed from the CI environment. This setting should not be modified.")

  val sbsBuildVCSNumber = SettingKey[String]("sbsBuildVCSNumber", "VCS revision number computed from the CI environment. This setting should not be modified.")

  val sbsTeamcity = SettingKey[Boolean]("sbsTeamcity", "'true' if current build is running under TeamCity. This setting should not be modified.")

  val sbsImplementationVersion = SettingKey[String]("sbsImplementationVersion", "Implementation version computed from the current build. This setting should not be modified.")

  val sbsVersionMessage = TaskKey[Unit]("sbsVersionMessage", "Updates the current TeamCity build number with the project implementation version.")

  val sbsProfile = settingKey[Option[BuildProfile]]("'BuildProfile' computed from the current build. This setting should not be modified.")

  override lazy val projectSettings: Seq[Def.Setting[_]] = sbsPluginSettings

  private def sbsPluginSettings = Seq(
    sbsTeamcity := Impl.teamcity,
    sbsBuildNumber <<= sbsTeamcity(Impl.buildNumber),
    sbsBuildVCSNumber <<= sbsTeamcity(Impl.buildVcsNumber),
    sbsImplementationVersion <<= (Keys.version, sbsBuildNumber, sbsBuildVCSNumber)(
      (version, buildNumber, buildVCSNumber) => Impl.implementationVersion(version, buildNumber, buildVCSNumber)),
    sbsProfile := Seq(DevelopmentProfile, PreReleaseProfile, ReleaseProfile).find(_.is),
    sbsVersionMessage <<= (sbsTeamcity, sbsImplementationVersion).map(
      (teamcity, version) => Impl.sbsVersionMessageSetting(teamcity, version)),
    Keys.version <<= (Keys.version, sbsBuildNumber, sbsBuildVCSNumber, sbsProfile)((v, bn, vn, p) => Impl.version(v, bn, vn, p))
  )

  private object Impl {

    def teamcity: Boolean = !sys.env.get("TEAMCITY_VERSION").isEmpty

    /**
     * Helper method which builds a ''SemVer'' compliant project implementation version.
     *
     * @param version The current version of the project.
     * @param buildNumber The current build number of the project
     * @param buildVCSNumber The current version control revision number of the project.
     * @return Project build version formatted as
     *         "{PROJECT VERSION}+{BUILD NUMBER}.{SHORT VCS REVISION NUMBER}"
     */
    def implementationVersion(version: String, buildNumber: String, buildVCSNumber: String) =
      if (!version.endsWith(implementationMeta(buildNumber, buildVCSNumber)) && !version.contains("+"))
        s"$version+${implementationMeta(buildNumber, buildVCSNumber)}" else version

    def version(version: String, buildNumber: String, buildVCSNumber: String, profile: Option[BuildProfile]): String = profile match {
      case Some(ReleaseProfile) => version
      case Some(DevelopmentProfile) => version
      case _ => s"$version+${implementationMeta(buildNumber, buildVCSNumber)}"
    }

    def implementationMeta(buildNumber: String, buildVCSNumber: String) = {
      def vcsNo = buildVCSNumber.take(7)
      if (!buildNumber.startsWith("UNKNOWN")) s"$buildNumber.$vcsNo" else vcsNo
    }

    /**
     * Returns the project build number if build is running under a supported CI server
     *
     * @param teamcity Retrieves build number from Teamcity if true
     * @return Project build number
     */
    def buildNumber(teamcity: Boolean): String = sys.env.get("BUILD_NUMBER").getOrElse("UNKNOWN")

    /**
     * Returns the project build vcs revision number if build is running under a supported CI server
     *
     * @param teamcity Retrieves vcs revision number from Teamcity if true
     * @return Project vcs revision number
     */
    def buildVcsNumber(teamcity: Boolean) = (if (teamcity) sys.env.get("BUILD_VCS_NUMBER").get else Try[String](Process("git rev-parse HEAD").lines.head).getOrElse("UNKNOWN")).
      take(7)

    def sbsVersionMessageSetting(teamcity: Boolean, implementationVersion: String) =
      if (teamcity) println(s"##teamcity[buildNumber '$implementationVersion']")
  }

}
