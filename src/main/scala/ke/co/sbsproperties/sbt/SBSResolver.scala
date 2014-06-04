/**********************************************************************
 * See the NOTICE file distributed with this work for additional      *
 *   information regarding Copyright ownership.  The author/authors   *
 *   license this file to you under the terms of the Apache License,  *
 *   Version 2.0 (the "License"); you may not use this file except    *
 *   in compliance with the License.  You may obtain a copy of the    *
 *   License at:                                                      *
 *                                                                    *
 *       http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                                    *
 *   Unless required by applicable law or agreed to in writing,       *
 *   software distributed under the License is distributed on an      *
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY           *
 *   KIND, either express or implied.  See the License for the        *
 *   specific language governing permissions and limitations          *
 *   under the License.                                               *
 **********************************************************************/

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

  val sbsPublishTo = (release: Boolean, internal: Boolean,  mvn: Boolean) => {
    if(internal && mvn) sbsMavenRepository(release) else if (!internal) ossRepository(release) else sbsIvyRepository(release)
  }

  private def status(release: Boolean) = if (release) "release" else "snapshot"

  private def sbsMavenRepository(release: Boolean): MavenRepository =  s"SBS Maven ${status(release).capitalize} Repository" at
    s"$sbsMavenResolverRoot-${status(release)}"

  private def sbsIvyRepository(release: Boolean) = Resolver.url(s"SBS Ivy ${status(release).capitalize} Repository",
    url(s"$sbsIvyResolverRoot-${status(release)}"))(Resolver.ivyStylePatterns)

  private def ossRepository(release: Boolean) = if (!release) sonatypeOSSSnapshots else sonatypeOSSStaging
}
