name := "solr4-extras"

version := "1.0"

scalaVersion := "2.9.2"

resolvers ++= Seq(
  "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype Scala Tools" at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/repo"
)

libraryDependencies ++= Seq(
  "org.apache.solr" % "solr-core" % "4.0.0",
  "org.apache.solr" % "solr-solrj" % "4.0.0",
  "org.bouncycastle" % "bcprov-jdk16" % "1.45",
  "org.mongodb" %% "casbah" % "2.3.0",
  "com.novocode" % "junit-interface" % "0.8" % "test",
  "com.typesafe.akka" % "akka-actor" % "2.0",
  "play" % "play_2.9.1" % "2.0.4"
)
