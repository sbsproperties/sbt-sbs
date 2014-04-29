logLevel := Level.Warn

addSbtPlugin("org.jetbrains" % "sbt-teamcity-logger" % "0.1.0-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

libraryDependencies <+= sbtVersion(sv => "org.scala-sbt" % "scripted-plugin" % sv)

resolvers += Resolver.sbtPluginRepo("snapshots") // Needed for sbt-teamcity-logger