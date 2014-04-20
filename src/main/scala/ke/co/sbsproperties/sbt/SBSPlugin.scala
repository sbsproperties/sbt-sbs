package ke.co.sbsproperties.sbt

import sbt._
import Keys._
import aether.Aether
import scala.util.Try
import sbtbuildinfo.{Plugin => BuildInfoPlugin}


object SBSPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  override lazy val projectSettings: Seq[Def.Setting[_]] = sbsPluginSettings

  sealed trait BuildProfile

  case object ReleaseProfile extends BuildProfile

  case object PreReleaseProfile extends BuildProfile

  case object DevelopmentProfile extends BuildProfile

  val sbsBuildNumber =
    settingKey[String]("Build number computed from the CI environment. This setting should not be modified.")

  val sbsBuildVCSNumber =
    settingKey[String]("VCS revision number computed from the CI environment. This setting should not be modified.")

  val sbsTeamcity =
    settingKey[Boolean]("'true' if current build is running under TeamCity. This setting should not be modified.")

  val sbsImplementationVersion =
    settingKey[String]("Implementation version computed from the current build. This setting should not be modified.")

  val sbsVersionMessage =
    taskKey[Unit]("Updates the current TeamCity build number with the project implementation version.")

  val sbsProfile =
    settingKey[BuildProfile]("'BuildProfile' used to determine build specific settings.")

  private def sbsPluginSettings = Seq[Setting[_]](
    sbsTeamcity := Impl.teamcity,
    sbsBuildNumber <<= sbsTeamcity(Impl.buildNumber),
    sbsBuildVCSNumber <<= (Keys.baseDirectory, sbsTeamcity)((d, t) => Impl.buildVcsNumber(t, d)),
    sbsImplementationVersion <<= (Keys.version, sbsBuildNumber, sbsBuildVCSNumber)(
      (version, buildNumber, buildVCSNumber) => Impl.implementationVersion(version, buildNumber, buildVCSNumber)),
    sbsProfile <<= sbsProfile ?? DevelopmentProfile,
    sbsVersionMessage <<= (sbsTeamcity, sbsImplementationVersion).map(
      (teamcity, version) => Impl.sbsVersionMessageSetting(teamcity, version)),
    Keys.version <<= (Keys.version, Keys.publishMavenStyle, sbsBuildNumber, sbsBuildVCSNumber, sbsProfile)(
      (v, m, bn, vn, p) => Impl.version(v, m, bn, vn, p))
  )

  val sbsDefaultSettings: Seq[Setting[_]] = sbsBaseSettings ++ sbsCompileSettings ++ sbsPackageSettings ++
    sbsPublishSettings

  val sbsProjectSettings = Aether.aetherPublishSettings ++ sbsDefaultSettings

  val sbsSbtPluginProjectSettings = sbsDefaultSettings :+ (Keys.sbtPlugin := true)

  def sbsBaseSettings = Seq(
    name ~= Impl.formalName,
    organization := "ke.co.sbsproperties",
    organizationName := "Said bin Seif Properties Ltd.",
    organizationHomepage := Some(url("http://www.sbsproperties.co.ke"))
  )

  def sbsCompileSettings = Seq[Setting[_]](
    scalacOptions <<= (SBSPlugin.sbsProfile, scalacOptions) map ((p, o) => {
      val opts = Seq(Opts.compile.deprecation, "-feature")
      val devOpts = opts ++ Seq(Opts.compile.unchecked, Opts.compile.explaintypes)
      p match {
        case DevelopmentProfile => devOpts;
        case _ => opts
      }
    })
  )

  def sbsPackageSettings: Seq[Setting[_]] = Seq(
    packageOptions <<= (sbsImplementationVersion, version, packageOptions) map {
      (iv, sv, o) =>
        o :+ Package.ManifestAttributes(
          "Built-By" -> System.getProperty("user.name", "unknown"),
          "Build-Jdk" -> System.getProperty("java.version", "unknown"),
          "Built-Time" -> java.util.Calendar.getInstance.getTimeInMillis.toString,
          "Implementation-Version" -> iv,
          "Specification-Version" -> sv)
    },
    mappings in(Compile, packageBin) <+= baseDirectory map {
      (base: File) => (base / "LICENSE") -> "META-INF/LICENSE"
    },
    mappings in(Compile, packageSrc) <+= baseDirectory map {
      (base: File) => (base / "LICENSE") -> "META-INF/LICENSE"
    },
    mappings in(Compile, packageBin) <+= baseDirectory map {
      (base: File) => (base / "NOTICE") -> "META-INF/NOTICE"
    },
    mappings in(Compile, packageSrc) <+= baseDirectory map {
      (base: File) => (base / "NOTICE") -> "META-INF/NOTICE"
    }
  )

  def sbsPublishSettings: Seq[Setting[_]] = Seq(
    publishTo <<= (SBSPlugin.sbsProfile, publishMavenStyle, version) {
      (profile, mvn, v) =>
        def release = if (v.contains("SNAPSHOT")) false
        else profile match {
          case ReleaseProfile => true
          case _ => false
        }
        Some(SBSResolver.publishToRepo(release, mvn))
    },
    publishMavenStyle := !sbtPlugin.value
  )

  
  def sbsBuildInfoSettings = {
    import BuildInfoPlugin._
    def defaultInfoKeys = Seq[BuildInfoKey](
      BuildInfoKey.setting(sbsImplementationVersion),
      BuildInfoKey.setting(scalaVersion),
      BuildInfoKey.setting(sbsProfile))
    buildInfoSettings ++ Seq(
      sourceGenerators in Compile <+= buildInfo,
      buildInfoKeys := defaultInfoKeys,
      buildInfoPackage := s"${normalizedName.value.replace("-", ".")}.build"
    )
  }

  def addBuildinfoKeys(keys: SettingKey[Any]*): Setting[_] = {
    import BuildInfoPlugin._
    buildInfoKeys ++= keys.map(BuildInfoKey.setting)
  }
  
  def addSubOrganisation(s: String): Setting[String] = organization <<= organization(_ + s".$s")

  
  implicit class SBSProjectSyntax(p: Project) {
    
    def additionalInfoKeys(keys: SettingKey[Any]*) = p.settings(addBuildinfoKeys(keys: _*))

    def withSubOrganization(s: String) = p.settings(addSubOrganisation(s))

    def withSbsProjectSettings = p.settings(sbsProjectSettings: _*)

    def withSbsSbtPluginSettings = p.settings(sbsSbtPluginProjectSettings: _*)
  }

  
  private object Impl {

    def teamcity: Boolean = !sys.env.get("TEAMCITY_VERSION").isEmpty

    def formalName(name: String): String = name.replaceFirst("sbs", "SBS").split("-").map(_.capitalize).mkString(" ")

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
        s"$version+${implementationMeta(buildNumber, buildVCSNumber)}"
      else version

    def version(version: String, mvn: Boolean, buildNumber: String, buildVCSNumber: String, profile: BuildProfile) =
      (profile, mvn) match {
        case (ReleaseProfile, false) => version
        case (DevelopmentProfile, false) => version
        case (_, true) => version
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
     * Returns the project build git revision number
     *
     * @param teamcity Retrieves vcs revision number from Teamcity if true
     * @return Project vcs revision number
     */
    def buildVcsNumber(teamcity: Boolean, baseDir: File): String = {
      def isGitRepo(dir: File): Boolean = if (dir.listFiles().map(_.getName).contains(".git")) true
      else {
        val parent = dir.getParentFile
        if (parent == null) false else isGitRepo(parent)
      }

      def vcsNo = (teamcity, isGitRepo(baseDir)) match {
        case (true, _) => System.getenv("BUILD_VCS_NUMBER")
        case (false, true) => Try(Process("git rev-parse HEAD").lines.head).getOrElse("UNKNOWN")
        case _ => "UNKNOWN"
      }
      vcsNo.take(7)
    }

    def sbsVersionMessageSetting(teamcity: Boolean, implementationVersion: String) =
      if (teamcity) println(s"##teamcity[buildNumber '$implementationVersion']")
  }

}
