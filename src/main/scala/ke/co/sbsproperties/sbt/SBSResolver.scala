package ke.co.sbsproperties.sbt

import sbt._
import Resolver.SonatypeRepositoryRoot


trait SBSResolver {

  val sbsRepositoryRoot = "https://dev.sbsproperties.co.ke/repo"
  val sbsIvyResolverRoot = s"$sbsRepositoryRoot/ivy"
  val sbsMavenResolverRoot = s"$sbsRepositoryRoot/maven"
  val sonatypeResolverRoot = "https://oss.sonatype.org"

  val sbsMavenReleases: MavenRepository = sbsMavenRepository(release = true)
  val sbsMavenSnapshots: MavenRepository = sbsMavenRepository(release = false)
  val sbsIvyReleases: URLRepository = sbsIvyRepository(release = true)
  val sbsIvySnapshots: URLRepository = sbsIvyRepository(release = false)

  val sbsReleaseResolvers: Seq[Resolver] = Seq(sbsMavenReleases, sbsIvyReleases)
  val sbsSnapshotResolvers: Seq[Resolver] = Seq(sbsMavenSnapshots, sbsIvySnapshots)
  val sbsResolvers: Seq[Resolver] = sbsReleaseResolvers ++ sbsSnapshotResolvers

  val sonatypeOSSStaging: MavenRepository = s"Sonatype OSS Staging Repository" at
    s"$sonatypeResolverRoot/service/local/staging/deploy/maven2"

  val sonatypeOSSSnapshots: MavenRepository = s"Sonatype OSS Snapshot Repository" at
    s"$sonatypeResolverRoot/content/repositories/snapshots"

  val sbsPublishTo = (release: Boolean, oss: Boolean,  mvn: Boolean) => {
    if(!oss && mvn) sbsMavenRepository(release) else if (oss) ossRepository(release) else sbsIvyRepository(release)
  }

  private def status(release: Boolean) = if (release) "release" else "snapshot"

  private def sbsMavenRepository(release: Boolean): MavenRepository =  s"SBS Maven ${status(release).capitalize} Repository" at
    s"$sbsMavenResolverRoot-${status(release)}"

  private def sbsIvyRepository(release: Boolean) = Resolver.url(s"SBS Ivy ${status(release).capitalize} Repository",
    url(s"$sbsIvyResolverRoot-${status(release)}"))(Resolver.ivyStylePatterns)

  private def ossRepository(release: Boolean) = if (!release) sonatypeOSSSnapshots else sonatypeOSSStaging
}
