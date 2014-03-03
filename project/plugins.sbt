logLevel := Level.Warn

addSbtPlugin("org.jetbrains" % "sbt-teamcity-logger" % "0.1.0-SNAPSHOT")

resolvers += Resolver.sbtPluginRepo("snapshots") // Needed for sbt-teamcity-logger