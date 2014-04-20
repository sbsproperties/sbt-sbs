package ke.co.sbsproperties.sbt

import sbt._


object SBSResolver {

  val root = "https://dev.sbsproperties.co.ke/repo"
  val ivyRsolverRoot = s"$root/ivy"
  val mavenResolverRoot = s"$root/maven"

  val sbsMavenReleases = sbsMavenRepository(release = true)
  val sbsMavenSnapshots = sbsMavenRepository(release = false)
  val sbsIvyReleases = sbsIvyRepository(release = true)
  val sbsIvySnapshots = sbsIvyRepository(release = false)


  val releaseResolvers = Seq(sbsMavenReleases, sbsIvyReleases)
  val snapshotResolvers = Seq(sbsMavenSnapshots, sbsIvySnapshots)
  val allResolvers = releaseResolvers ++ snapshotResolvers

 val publishToRepo = (release: Boolean, mvn: Boolean) => {
    if(mvn) sbsMavenRepository(release) else sbsIvyRepository(release)
  }

  private def status(release: Boolean) = if (release) ("Release", "release") else ("Snapshot", "snapshot")

  private def sbsMavenRepository(release: Boolean): MavenRepository =  s"SBS Maven ${status(release)._1} Repository" at
    s"$mavenResolverRoot-${status(release)._2}"

  private def sbsIvyRepository(release: Boolean) = Resolver.url(s"SBS Ivy ${status(release)._1} Repository",
    url(s"$ivyRsolverRoot-${status(release)._2}"))(Resolver.ivyStylePatterns)

}
