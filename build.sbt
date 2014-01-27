import AssemblyKeys._

assemblySettings

name := "solr4-extras"

version := "1.0"

scalaVersion := "2.10.2"

EclipseKeys.withSource := true

resolvers ++= Seq(
  "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Maven Restlet" at "http://maven.restlet.org",
  "Sonatype Scala Tools" at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/repo",
  "Maven.org" at "http://repo2.maven.org/maven2",
  "Conjars.org" at "http://conjars.org/repo"
)

libraryDependencies ++= Seq(
  "org.apache.solr" % "solr-core" % "4.3.0",
  "org.apache.solr" % "solr-solrj" % "4.3.0",
  "org.bouncycastle" % "bcprov-jdk16" % "1.45",
  "org.mongodb" %% "casbah" % "2.5.0",
  "com.novocode" % "junit-interface" % "0.8" % "test",
  "com.typesafe.akka" % "akka-actor_2.10" % "2.2.3",
  "play" % "play_2.10" % "2.1.5",
  "com.twitter" % "scalding-core_2.10" % "0.8.11",
  "org.apache.mahout" % "mahout-core" % "0.7",
  "org.apache.hadoop" % "hadoop-core" % "0.20.2",
  "org.jboss.netty" % "netty" % "3.2.9.Final",
  "mysql" % "mysql-connector-java" % "5.1.12",
  "net.debasishg" % "redisclient_2.10" % "2.11",
  "org.drools" % "drools-core" % "6.0.1.Final"
)

// assembly settings
test in assembly := {}

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
    case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
    case PathList("org", "apache", "jasper", xs @ _*) => MergeStrategy.first
    case PathList("org", "apache", "lucene", xs @ _*) => MergeStrategy.first
    case PathList("org", "slf4j", xs @ _*) => MergeStrategy.last
    case PathList("org", "apache", "commons", xs @ _*) => MergeStrategy.first
    case PathList("org", "tartarus", xs @ _*) => MergeStrategy.first
    case PathList("project.clj") => MergeStrategy.discard
    case PathList("META-INF", "spring.tooling") => MergeStrategy.concat
    case PathList("overview.html") => MergeStrategy.discard
    case x => old(x)
  }
}
