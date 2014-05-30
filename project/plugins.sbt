logLevel := Level.Warn

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

libraryDependencies <+= sbtVersion(sv => "org.scala-sbt" % "scripted-plugin" % sv)

resolvers += Resolver.sbtPluginRepo("snapshots") // Needed for sbt-teamcity-logger