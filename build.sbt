name := "SBT Lift"

licenses := Seq(("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt")))

startYear := Some(2014)

version := "1.0-SNAPSHOT"

organization := "ke.co.sbsproperties"

description := "SBT Plugin for quick configuration of an SBS project."

startYear := Some(2014)

scmInfo := Some(ScmInfo(url("https://github.com/sbsproperties/sbt-sbs"),
  "scm:git:https://github.com/sbsproperties/sbt-sbs.git"))

homepage := Some(url("https://github.com/sbsproperties/sbt-sbs"))

sbtPlugin := true

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := (_ => false)

pomExtra <<= pomExtra(_ ++ developers)

scalacOptions in Compile += Opts.compile.deprecation

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.12.1")

// Impl
def developers = {
  val developers = Map(
    "arashi01" -> "Ali Salim Rashid"
  )
  <developers>
    {developers map { m =>
    <developer>
      <id>{m._1}</id>
      <name>{m._2}</name>
      <url>http://github.com/{m._1}</url>
    </developer>
  }}
  </developers>
}