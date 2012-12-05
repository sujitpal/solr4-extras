scalaVersion := "2.9.2"

resolvers ++= Seq(
  "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  "org.apache.solr" % "solr-core" % "4.0.0"
)
