package ke.co.sbsproperties.sbt

import sbt._
import sbt.Keys._
import ke.co.sbsproperties.sbt.SBSPlugin.{ReleaseProfile, sbsImplementationVersion}

trait SBSProject {

  def subProject(base: String, prefix: String = "sbs-")(project: String): Project =
    apply(project, prefix, base = file(base) / project.stripPrefix(prefix))

  def rootProject(project: String, prefix: String = "sbs-"): Project = apply(project, prefix, file("."))

  def apply(project: String, prefix: String, base: File): Project =
    Project(id = if (project.startsWith(prefix)) project else prefix + project, base = base).
      settings(defaultProjectSettings: _*)

  lazy val defaultProjectSettings: Seq[Setting[_]] = projectBaseSettings ++ organisationSettings ++ compileSettings ++
    packageSettings ++ publishSettings

  def projectBaseSettings = Seq(
    name ~= formalName
  )

  def organisationSettings: Seq[Setting[_]] = Seq(
    organization := "ke.co.sbsproperties",
    organizationName := "Said bin Seif Properties Ltd.",
    organizationHomepage := Some(url("http://www.sbsproperties.co.ke"))
  )

  def compileSettings = Seq[Setting[_]](
    scalacOptions ++= DefaultOptions.scalac ++ Seq("-encoding", "utf8") :+ Opts.compile.deprecation :+ Opts.compile.unchecked)

  def packageSettings: Seq[Setting[_]] = Seq(
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
      (base: File) => (base / "LICENSE") -> "META-INF/LICENSE"},
    mappings in(Compile, packageSrc) <+= baseDirectory map {
      (base: File) => (base / "LICENSE") -> "META-INF/LICENSE"},
    mappings in(Compile, packageBin) <+= baseDirectory map {
      (base: File) => (base / "NOTICE") -> "META-INF/NOTICE"},
    mappings in(Compile, packageSrc) <+= baseDirectory map {
      (base: File) => (base / "NOTICE") -> "META-INF/NOTICE"}
  )

  def publishSettings: Seq[Setting[_]] = Seq(
    publishTo <<= SBSPlugin.sbsProfile {
      case Some(ReleaseProfile) => Some(Resolvers.publishToRepo(release = true))
      case _ => Some(Resolvers.publishToRepo(release = false))
    },
    publishMavenStyle := false
  )

  def formalName(name: String): String = name.replaceFirst("sbs", "SBS").split("-").map(_.capitalize).mkString(" ")

  def subOrganization(s: String): Def.Setting[String] = organization <<= organization(_ ++ s".$s")
}

object SBSProject extends SBSProject