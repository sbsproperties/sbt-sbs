package ke.co.sbsproperties.sbt

import sbt._


object Resolvers {

  val host = "https://dev.sbsproperties.co.ke/repo"

  def publishToRepo(release: Boolean): URLRepository = if (!release) sbsIvySnapshot else sbsIvyRelease

  def sbsIvyRepo(release: Boolean): URLRepository = Resolver.url(repoName(release), repoUrl(release))(Resolver.ivyStylePatterns)

  val sbsIvyRelease = sbsIvyRepo(release = false)

  val sbsIvySnapshot = sbsIvyRepo(release = true)

  lazy val releaseResolvers = Seq(sbsIvyRelease)
  lazy val snapshotResolvers = Seq(sbsIvySnapshot)
  lazy val allResolvers = releaseResolvers ++ snapshotResolvers

  private def repoName(release: Boolean): String = s"SBS Properties Ivy ${if (release) "Snapshot" else "Release"} Repository"

  private def repoUrl(release: Boolean): URL = sbt.url(s"https://dev.sbsproperties.co.ke/repo/${if (release) "ivy-snapshot" else "ivy-release"}")

}
