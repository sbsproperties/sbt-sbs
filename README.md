sbt-sbs
=======

Simple plug-in for simple configuration of a Said bin Seif Properties [sbt][1] project.


Build Status:  ![Build Status Icon][2]

### Supported [sbt][1] versions:
-  0.13.5-M2


## Resolver Coordinates:

Release Resolver:
`Resolver.url("SBS Ivy Releases", url("https://dev.sbsproperties.co.ke/repo/ivy-release"))(Resolver.ivyStylePatterns)`


Snapshot Resolver:
`Resolver.url("SBS Ivy Snapshots", url("https://dev.sbsproperties.co.ke/repo/ivy-snapshot"))(Resolver.ivyStylePatterns)`


## Artefact Coordinates:
`addSbtPlugin("ke.co.sbsproperties" % "sbt-sbs" % "${VERSION}")`

*Note:*  Until sbt-aether version >= 0.12 is released in binary form, it is also necessary to add it as a a source
dependency to any dependent project with a `dependsOn` directive on:
`uri("https://github.com/arktekk/sbt-aether-deploy.git")`



[1]:  http://scala-sbt.org
[2]: https://dev.sbsproperties.co.ke/app/rest/builds/buildType(id:SBT_Sbs_Default)/statusIcon
