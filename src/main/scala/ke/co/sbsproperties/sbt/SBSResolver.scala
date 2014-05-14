package ke.co.sbsproperties.sbt

import sbt._
import Resolver.SonatypeRepositoryRoot


trait SBSResolver {

  val sbsRepositoryRoot = "https://dev.sbsproperties.co.ke/repo"
  val sbsIvyRsolverRoot = s"$sbsRepositoryRoot/ivy"
  val sbsMavenResolverRoot = s"$sbsRepositoryRoot/maven"

  val sbsMavenReleases: MavenRepository = sbsMavenRepository(release = true)
  val sbsMavenSnapshots: MavenRepository = sbsMavenRepository(release = false)
  val sbsIvyReleases: URLRepository = sbsIvyRepository(release = true)
  val sbsIvySnapshots: URLRepository = sbsIvyRepository(release = false)

  val sbsReleaseResolvers: Seq[Resolver] = Seq(sbsMavenReleases, sbsIvyReleases)
  val sbsSnapshotResolvers: Seq[Resolver] = Seq(sbsMavenSnapshots, sbsIvySnapshots)
  val sbsResolvers: Seq[Resolver] = sbsReleaseResolvers ++ sbsSnapshotResolvers

  val sonatypeOSSStaging: MavenRepository = s"Sonatype OSS Staging Repository" at
    s"$SonatypeRepositoryRoot/content/repositories/snapshots"

  val sonatypeOSSSnapshots: MavenRepository = s"Sonatype OSS Snapshot Repository" at
    s"$SonatypeRepositoryRoot/service/local/staging/deploy/maven2"


  val sbsPublishTo = (release: Boolean, oss: Boolean,  mvn: Boolean) => {
    if(!oss && mvn) sbsMavenRepository(release) else if (oss) ossRepository(release) else sbsIvyRepository(release)
  }

  private def status(release: Boolean) = if (release) ("Release", "release") else ("Snapshot", "snapshot")

  private def sbsMavenRepository(release: Boolean): MavenRepository =  s"SBS Maven ${status(release)._1} Repository" at
    s"$sbsMavenResolverRoot-${status(release)._2}"

  private def sbsIvyRepository(release: Boolean) = Resolver.url(s"SBS Ivy ${status(release)._1} Repository",
    url(s"$sbsIvyRsolverRoot-${status(release)._2}"))(Resolver.ivyStylePatterns)

  private def ossRepository(release: Boolean) = if (!release) sonatypeOSSSnapshots else sonatypeOSSStaging
}
