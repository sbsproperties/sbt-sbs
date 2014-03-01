package ke.co.sbsproperties.sbt

import sbt._
import Keys._

object Dependencies {

  val addConfig = Seq(libraryDependencies += "com.typesafe" % "config" % "1.2.0")

  val addScalaLogging = Seq(libraryDependencies <++= scalaVersion {
    sv => Seq(
      "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
      "org.scala-lang" % "scala-reflect" % sv)
  }) //FIXME Specifying scala-reflect version as sbt pulls in older version transitively

  val addScalaz = Seq(libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.5")

  // Test Libraries
  val addScalatest = Seq(libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test")

  lazy val commonBOM = addConfig ++ addScalaLogging ++ addScalaz

  lazy val testCommonBOM = addScalatest

  lazy val defaultBOM = commonBOM ++ testCommonBOM
}