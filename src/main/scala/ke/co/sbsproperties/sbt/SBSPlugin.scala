/**********************************************************************
 * See the NOTICE file distributed with this work for additional      *
 *   information regarding Copyright ownership.  The author/authors   *
 *   license this file to you under the terms of the Apache License,  *
 *   Version 2.0 (the "License"); you may not use this file except    *
 *   in compliance with the License.  You may obtain a copy of the    *
 *   License at:                                                      *
 *                                                                    *
 *       http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                                    *
 *   Unless required by applicable law or agreed to in writing,       *
 *   software distributed under the License is distributed on an      *
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY           *
 *   KIND, either express or implied.  See the License for the        *
 *   specific language governing permissions and limitations          *
 *   under the License.                                               *
 **********************************************************************/

package ke.co.sbsproperties.sbt

import sbt._
import Keys._
import aether.Aether
import scala.util.Try
import com.typesafe.sbt.SbtPgp
import sbtbuildinfo.{Plugin => BuildInfoPlugin}
import com.typesafe.sbt.pgp.PgpKeys
import sbt.plugins.JvmPlugin


object SBSPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  override lazy val projectSettings: Seq[Def.Setting[_]] = sbsPluginSettings

  object Import extends Import with SBSResolver

  sealed trait Import {
    sealed trait BuildProfile

    case object ReleaseProfile extends BuildProfile

    case object MilestoneProfile extends BuildProfile

    case object IntegrationProfile extends BuildProfile

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

    val sbsRelease = settingKey[Boolean]("'true' if current build is a stable release. This setting should not be modified.")

    val sbsOss = settingKey[Boolean]("If true, configures project for open source publication and publishing.")

    def sbsProjectSettings: Seq[Setting[_]] = SBSPlugin.sbsProjectSettings
    
    def sbsPluginProjectSettings: Seq[Setting[_]] = SBSPlugin.sbsSbtPluginProjectSettings

    def publishInternal(internal: Boolean): Setting[Option[Resolver]] = SBSPlugin.publishInternal(internal)

    def publishInternal(internal: SettingKey[Boolean]): Setting[Option[Resolver]] = SBSPlugin.publishInternal(internal, invert = false)

    def publishInternal(internal: SettingKey[Boolean], invert: Boolean): Setting[Option[Resolver]] = SBSPlugin.publishInternal(internal, invert)

    def infoKeys(keys: SettingKey[Any]*): Setting[_] = SBSPlugin.addBuildInfoKey(keys: _*)

    def subOrganisation(s: String): Setting[String] = SBSPlugin.addSubOrganisation(s)

    implicit class SBSProjectSyntax(p: Project) {

      def infoKeys(keys: SettingKey[Any]*) = p.settings(Import.infoKeys(keys: _*))

      def subOrganisation(s: String) = p.settings(Import.subOrganisation(s))

      def sbsSettings = p.settings(sbsProjectSettings: _*)

      def sbsSbtPluginSettings = p.settings(sbsSbtPluginProjectSettings: _*)

      def snapshotResolvers = p.settings(sbsSnapshotResolverSetting)
    }
  }

  val autoImport = Import

  import autoImport._

  private def sbsPluginSettings = Seq[Setting[_]](
    sbsTeamcity := Impl.teamcity,
    sbsBuildNumber <<= sbsTeamcity(Impl.buildNumber),
    sbsBuildVCSNumber <<= (Keys.baseDirectory, sbsTeamcity)((d, t) => Impl.buildVcsNumber(t, d)),
    sbsImplementationVersion <<= (sbsProfile, sbsTeamcity, Keys.version, sbsBuildNumber, sbsBuildVCSNumber)(
      (profile, tc, version, buildNumber, buildVCSNumber) =>
        Impl.implementationVersion(profile, tc, version, buildNumber, buildVCSNumber)),
    sbsProfile <<= sbsProfile ?? DevelopmentProfile,
    sbsRelease := {
      def `release/milestone` = sbsProfile.value match {
        case ReleaseProfile | MilestoneProfile => true
        case _ => false
      }
      !isSnapshot.value && `release/milestone`
    },
    sbsVersionMessage <<= (sbsTeamcity, sbsImplementationVersion).map(
      (teamcity, version) => Impl.sbsVersionMessageSetting(teamcity, version))
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
              case ReleaseProfile | IntegrationProfile => signed
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
    Keys.version := Impl.version(sbsProfile.value, sbsTeamcity.value, version.value, publishMavenStyle.value,
      sbsBuildNumber.value, sbsBuildVCSNumber.value),
    organization := "ke.co.sbsproperties",
    organizationName := "Said bin Seif Properties Ltd.",
    organizationHomepage := Some(url("http://www.sbsproperties.co.ke")),
    sbsOss <<= sbsOss ?? false
  )

  private def sbsCompileSettings = Seq[Setting[_]](
    scalacOptions in (Compile, compile) := {
      val opts = scalacOptions.value ++ Seq(Opts.compile.deprecation, "-feature")
      val profile = sbsProfile.value
      val prodOpts = opts :+ "-optimise"
      val devOpts = opts ++ Seq(Opts.compile.unchecked, "–Xlint")

      (sbsTeamcity.value, profile) match {
        case (true, p) if p != DevelopmentProfile => prodOpts
        case (true, _) => opts
        case (false, p) if p == DevelopmentProfile => devOpts
        case _ => prodOpts
      }
    },
    scalacOptions in (Compile, doc)  ++= Seq(
      "-doc-root-content", s"${(scalaSource in (Compile, compile)).value.getPath}/rootdoc.txt",
      s"-doc-title", name.value,
      s"-doc-version", version.value,
      "-implicits",
      s"-doc-external-doc:${scalaInstance.value.libraryJar}#http://www.scala-lang.org/api/${scalaVersion.value}/",
      "-diagrams"
    )
  )

  private def sbsPackageSettings: Seq[Setting[_]] = Seq(
    packageOptions <<= (sbsImplementationVersion, version, packageOptions) map {
      (iv, sv, o) =>
        o :+ Package.ManifestAttributes(
          "Built-By" -> System.getProperty("java.version", "unknown"),
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

  private def sbsPublishSettings: Seq[Setting[_]] = Seq(
    pomIncludeRepository := (if (sbsOss.value) _ => false else pomIncludeRepository.value),
    publishMavenStyle := (if (sbsOss.value) true else sbtPlugin.value),
    isSnapshot := {
      def snapshotMatch(s: String) = s.contains("SNAPSHOT") || s.contains("snapshot")
      val isSnapshot = snapshotMatch(version.value)
      val snapshotDeps = libraryDependencies.value.filter((dep) => snapshotMatch(dep.revision) || snapshotMatch(dep.name))
      val containsSnapshotDeps = snapshotDeps.isEmpty
      if (!isSnapshot && containsSnapshotDeps) true else isSnapshot
    },
    publishInternal(sbsOss, invert = true)
  )

  private def sbsResolverSetting: Setting[Seq[Resolver]] = resolvers ++= sbsReleaseResolvers

  private def sbsSnapshotResolverSetting: Setting[Seq[Resolver]] = resolvers ++= sbsSnapshotResolvers

  private def sbsBuildInfoSettings = {
    import BuildInfoPlugin._
    def defaultInfoKeys = Seq[BuildInfoKey](
      BuildInfoKey.setting(sbsImplementationVersion),
      BuildInfoKey.setting(scalaVersion),
      BuildInfoKey.setting(sbsProfile))
    buildInfoSettings ++ Seq(
      sourceGenerators in Compile <+= buildInfo,
      buildInfoKeys := defaultInfoKeys,
      buildInfoPackage := s"${organization.value}.build",
      buildInfoObject := s"${normalizedName.value.split("-").map(_.capitalize).mkString("")}BuildInfo",
      sources in(Compile, doc) <<= (sources in(Compile, doc), buildInfoObject) map { (s, o) =>
        s.filter(!_.name.contains(o))
      }
    )
  }

  private def pgpSettings  ={
    import SbtPgp.PgpKeys._
    useGpg := true
  }

  def addBuildInfoKey(keys: SettingKey[Any]*): Setting[_] = {
    import BuildInfoPlugin._
    buildInfoKeys ++= keys.map(BuildInfoKey.setting)
  }
  
  def addSubOrganisation(s: String): Setting[String] = organization <<= organization(_ + s".$s")

  def publishInternal(internal: Boolean): Setting[Option[Resolver]] =
    publishTo := Some(sbsPublishTo(sbsRelease.value, internal, publishMavenStyle.value))
  
  def publishInternal(internal: SettingKey[Boolean]): Setting[Option[Resolver]] = publishInternal(internal, invert = false)
  
  def publishInternal(internal: SettingKey[Boolean], invert: Boolean): Setting[Option[Resolver]] = publishTo := {
    val i = {
      val in = internal.value
      if (invert) !in else in
    }
   Some(sbsPublishTo(sbsRelease.value, i, publishMavenStyle.value))
  }
  
  private object Impl {

    // Check if we are building on TeamCity
    def teamcity: Boolean = !sys.env.get("TEAMCITY_VERSION").isEmpty

    // Used to formalize project name for projects declared with the syntax 'val fooProject = project ...'
    def formalize(name: String): String = name.replaceFirst("sbs", "SBS")
      .split("-|(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")
      .map(_.capitalize).mkString(" ")

    // Append relevent build implementation information to the version/revision
    def implementationVersion(profile: BuildProfile, teamcity: Boolean, version: String, buildNumber: String, buildVCSNumber: String) =
      if (!version.endsWith(implementationMeta(profile, teamcity, buildNumber, buildVCSNumber)) && !version.contains("+"))
        s"$version+${implementationMeta(profile, teamcity, buildNumber, buildVCSNumber)}"
      else version

    def version(profile: BuildProfile, teamcity: Boolean, version: String, mvn: Boolean, buildNumber: String, buildVCSNumber: String) =
      (profile, mvn) match {
        case (_, true) => version
        case (ReleaseProfile | MilestoneProfile, false) => version
        case (DevelopmentProfile, false) => version
        case _ => s"$version+${implementationMeta(profile, teamcity, buildNumber, buildVCSNumber)}"
      }

    def implementationMeta(profile: BuildProfile, teamcity: Boolean, buildNumber: String, buildVCSNumber: String) = {
      def vcsNo = buildVCSNumber.take(7)
      def published: Boolean = profile match {case ReleaseProfile | MilestoneProfile | IntegrationProfile => true; case _ => false}
      def build = if (!teamcity) "" else if (published) s"b$buildNumber." else s"dev-b$buildNumber."

      s"$build$vcsNo"
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
