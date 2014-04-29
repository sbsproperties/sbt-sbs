package ke.co.sbsproperties.sbt

import sbt._
import Keys._
import aether.Aether
import scala.util.Try
import com.typesafe.sbt.SbtPgp
import sbtbuildinfo.{Plugin => BuildInfoPlugin}
import com.typesafe.sbt.pgp.PgpKeys


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
    pgpSettings ++ sbsPublishSettings ++ sbsBuildInfoSettings :+ sbsResolverSetting

  val sbsProjectSettings = Aether.aetherPublishSettings ++ sbsDefaultSettings ++ {
    import Aether._
    Seq(
      aetherArtifact <<=
        (coordinates, Keys.`package` in Compile, makePom in Compile, PgpKeys.signedArtifacts in Compile, sbsProfile, aetherArtifact) map {
          (coords: aether.MavenCoordinates, mainArtifact: File, pom: File, artifacts: Map[Artifact, File], profile, orig) =>
            def signed = aether.Aether.createArtifact(artifacts, pom, coords, mainArtifact)
            profile match {
              case ReleaseProfile | PreReleaseProfile => signed
              case _ => orig
            }
      }
    )
  }

  val sbsSbtPluginProjectSettings = sbsDefaultSettings ++ Seq(
    Keys.sbtPlugin := true,
    Keys.publish <<= SbtPgp.PgpKeys.publishSigned,
    Keys.publishLocal <<= SbtPgp.PgpKeys.publishLocalSigned
  )

  private def sbsBaseSettings = Seq(
    name ~= Impl.formalize,
    organization := "ke.co.sbsproperties",
    organizationName := "Said bin Seif Properties Ltd.",
    organizationHomepage := Some(url("http://www.sbsproperties.co.ke"))
  )

  private def sbsCompileSettings = Seq[Setting[_]](
    scalacOptions <<= (SBSPlugin.sbsProfile, scalacOptions) map ((p, o) => {
      val opts = Seq(Opts.compile.deprecation, "-feature")
      val devOpts = opts ++ Seq(Opts.compile.unchecked, Opts.compile.explaintypes)
      p match {
        case DevelopmentProfile => devOpts;
        case _ => opts
      }
    })
  )

  private def sbsPackageSettings: Seq[Setting[_]] = Seq(
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
      (base: File) => (base / "LICENSE") -> "META-INF/LICENSE.txt"
    },
    mappings in(Compile, packageSrc) <+= baseDirectory map {
      (base: File) => (base / "LICENSE") -> "META-INF/LICENSE.txt"
    },
    mappings in(Compile, packageBin) <+= baseDirectory map {
      (base: File) => (base / "NOTICE") -> "META-INF/NOTICE.txt"
    },
    mappings in(Compile, packageSrc) <+= baseDirectory map {
      (base: File) => (base / "NOTICE") -> "META-INF/NOTICE.txt"
    }
  )

  private def sbsPublishSettings: Seq[Setting[_]] = Seq(
    publishTo <<= (SBSPlugin.sbsProfile, publishMavenStyle, version, libraryDependencies) {
      (profile, mvn, ver, deps) =>
        def snapshotMatch(s: String) = s.contains("SNAPSHOT") || s.contains("snapshot")
        def isSnapshot = snapshotMatch(ver)
        def snapshotDeps = !deps.filter((dep) =>  snapshotMatch(dep.revision) || snapshotMatch(dep.name)).isEmpty
        def releaseProfile = profile match {
          case ReleaseProfile => true
          case _ => false
        }
        def release = !isSnapshot && !snapshotDeps && releaseProfile
        Some(SBSResolver.publishToRepo(release, mvn))
    },
    publishMavenStyle := !sbtPlugin.value
  )
  
  private def sbsResolverSetting: Setting[Seq[Resolver]] = resolvers ++= SBSResolver.releaseResolvers

  private def sbsSnapshotResolverSetting: Setting[Seq[Resolver]] = resolvers ++= SBSResolver.snapshotResolvers

  private def sbsBuildInfoSettings = {
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

  private def pgpSettings  ={
    import SbtPgp.PgpKeys._
    useGpg := true
  }

  def addBuildinfoKey(keys: SettingKey[Any]*): Setting[_] = {
    import BuildInfoPlugin._
    buildInfoKeys ++= keys.map(BuildInfoKey.setting)
  }
  
  def addSubOrganisation(s: String): Setting[String] = organization <<= organization(_ + s".$s")

  
  implicit class SBSProjectSyntax(p: Project) {

    def infoKeys(keys: SettingKey[Any]*) = p.settings(addBuildinfoKey(keys: _*))

    def subOrganisation(s: String) = p.settings(addSubOrganisation(s))

    def sbsSettings = p.settings(sbsProjectSettings: _*)

    def sbsSbtPluginSettings = p.settings(sbsSbtPluginProjectSettings: _*)

    def snapshotResolvers = p.settings(sbsSnapshotResolverSetting)
  }

  
  private object Impl {

    def teamcity: Boolean = !sys.env.get("TEAMCITY_VERSION").isEmpty

    def formalize(name: String): String = name.replaceFirst("sbs", "SBS").split("-").map(_.capitalize).mkString(" ")

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

    def buildNumber(teamcity: Boolean): String = sys.env.get("BUILD_NUMBER").getOrElse("UNKNOWN")

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
