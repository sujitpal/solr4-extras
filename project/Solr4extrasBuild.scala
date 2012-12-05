import sbt._
import sbt.Keys._

object Solr4extrasBuild extends Build {

  lazy val solr4extras = Project(
    id = "solr4-extras",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "solr4-extras",
      organization := "com.mycompany",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.2"
      // add other settings here
    )
  )
}
