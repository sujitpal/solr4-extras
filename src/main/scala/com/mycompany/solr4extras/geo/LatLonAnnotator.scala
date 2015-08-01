package com.mycompany.solr4extras.geo

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.net.URLEncoder

import scala.io.Source

import org.codehaus.jackson.map.ObjectMapper

class LatLonAnnotator {

  val googleApiKey = "secret"
  val geocodeServer = "https://maps.googleapis.com/maps/api/geocode/json"

  val objectMapper = new ObjectMapper()
  
  def annotate(addr: String): (Double,Double) = {
    val params = Map(
      ("address", URLEncoder.encode(addr)),
      ("key", googleApiKey))
    val url = geocodeServer + "?" + 
      params.map(kv => kv._1 + "=" + kv._2)
            .mkString("&")
    val json = Source.fromURL(url).mkString
    val root = objectMapper.readTree(json)
    try {
      val location = root.path("results").get(0)
          .path("geometry").path("location")
      val lat = location.path("lat").getValueAsDouble
      val lon = location.path("lng").getValueAsDouble
      (lat, lon)
    } catch {
      case e: Exception => (0.0D, 0.0D)
    }
  }
  
  def batchAnnotate(infile: File, outfile: File): Unit = {
    val writer = new PrintWriter(new FileWriter(outfile), true)
    val lines = Source.fromFile(infile)
      .getLines()
      .filter(line => !line.startsWith("first_name"))
      .foreach(line => {
        val cols = line.split("\t")
        val fname = cols(0)
        val lname = cols(1)
        val company = cols(2)
        val address = cols(3)
        val city = cols(4)
        val state = cols(6)
        val zip = cols(7)
        val apiAddress = List(address, city, state)
          .mkString(", ") + " " + zip
        Console.println(apiAddress)
        val latlon = annotate(apiAddress)
        writer.println(List(fname, lname, company, address, city, state, zip, latlon._1, latlon._2)
          .mkString("\t"))
        Thread.sleep(1000) // sleep 1s between calls to Google API
      })
    writer.flush()
    writer.close()
  }
}