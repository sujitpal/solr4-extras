package com.mycompany.solr4extras.geo

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.common.SolrDocument
import scala.collection.JavaConversions._

class LatLonSearcher {

  val solr = new HttpSolrServer("http://localhost:8983/solr/collection1")
  
  def findWithinByGeofilt(p: Point, dkm: Double,
      sort: Boolean, nearestFirst: Boolean): List[LatLonDoc] = 
    findWithin("geofilt", p, dkm, sort, nearestFirst)
  
  def findWithinByBbox(p: Point, dkm: Double,
      sort: Boolean, nearestFirst: Boolean):List[LatLonDoc] =
    findWithin("bbox", p, dkm, sort, nearestFirst)
  
  def findWithin(method: String, p: Point, dkm: Double,
      sort: Boolean, nearestFirst: Boolean):
      List[LatLonDoc] = {
    val query = new SolrQuery()
    query.setQuery("*:*")
    query.setFields("*")
    query.setFilterQueries("{!%s}".format(method))
    query.set("pt", "%.2f,%.2f".format(p.x, p.y))
    query.set("d", dkm.toString)
    query.set("sfield", "latlon_p")
    if (sort) {
      query.set("sort", "geodist() %s"
        .format(if (nearestFirst) "asc" else "desc"))
      query.setFields("*,_dist_:geodist()")
    }
    val resp = solr.query(query)
    resp.getResults()
        .map(doc => getLatLonDocument(doc))
        .toList
  }

  def getLatLonDocument(sdoc: SolrDocument): LatLonDoc = {
    val latlon = sdoc.getFieldValue("latlon_p")
                     .asInstanceOf[String]
                     .split(",")
                     .map(_.toDouble)
    val dist = if (sdoc.getFieldValue("_dist_") != null) 
      sdoc.getFieldValue("_dist_").asInstanceOf[Double]
      else 0.0D 
    LatLonDoc(sdoc.getFieldValue("firstname_s").asInstanceOf[String],
      sdoc.getFieldValue("lastname_s").asInstanceOf[String],
      sdoc.getFieldValue("company_t").asInstanceOf[String],
      sdoc.getFieldValue("street_t").asInstanceOf[String],
      sdoc.getFieldValue("city_s").asInstanceOf[String],
      sdoc.getFieldValue("state_s").asInstanceOf[String],
      sdoc.getFieldValue("zip_s").asInstanceOf[String],
      Point(latlon(0), latlon(1)), dist)
  }
}

case class Point(x: Double, y: Double)
case class LatLonDoc(fname: String, lname: String, 
                     company: String, street: String, 
                     city: String, state: String, 
                     zip: String, location: Point,
                     dist: Double)
