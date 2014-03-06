package ke.co.sbsproperties.sbt

import sbt._


object Resolvers {

  val repositoryRoot = "https://dev.sbsproperties.co.ke/repo"
  val ivyRepositoryRoot = s"$repositoryRoot/ivy-"
  val mavenRepositoryRoot = s"$repositoryRoot/maven"

  def status(release: Boolean) = if (release) ("Release", "release") else ("Snapshot", "snapshot")

  def sbsMavenRepository(release: Boolean): MavenRepository =  s"SBS Maven ${status(release)._1} Repository" at
    s"$mavenRepositoryRoot-${status(release)._2}"

  def sbsIvyRepository(release: Boolean) = Resolver.url(s"SBS Ivy ${status(release)._1} Repository",
    url(s"$ivyRepositoryRoot-${status(release)._2}"))

  def publishToRepo(release: Boolean, mvn: Boolean): Resolver = if(mvn) sbsMavenRepository(release) else
    sbsIvyRepository(release)

  lazy val releaseResolvers = Seq(sbsIvyRepository(release = true), sbsMavenRepository(release = true))
  lazy val snapshotResolvers = Seq(sbsIvyRepository(release = false), sbsMavenRepository(release = false))
  lazy val allResolvers = releaseResolvers ++ snapshotResolvers

}
