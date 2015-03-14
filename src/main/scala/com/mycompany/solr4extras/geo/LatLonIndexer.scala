package com.mycompany.solr4extras.geo

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import scala.io.Source

import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.common.SolrInputDocument

class LatLonIndexer {

  val solrUrl = "http://localhost:8983/solr/collection1"
  val infile = new File("src/main/resources/us-cities-annotated.csv")
  
  def buildIndex(): Unit = {
    val solr = new HttpSolrServer(solrUrl)
    val ctr = new AtomicInteger(0)
    Source.fromFile(infile).getLines()
      .foreach(line => {
        val doc = new SolrInputDocument()
        val cols = line.split("\t")
        doc.addField("id", ctr.addAndGet(1))
        doc.addField("firstname_t", cols(0))
        doc.addField("lastName_t", cols(1))
        doc.addField("company_t", cols(2))
        doc.addField("street_t", cols(3))
        doc.addField("city_t", cols(4))
        doc.addField("state_t", cols(5))
        doc.addField("zip_t", cols(6))
        doc.addField("latlon_p", 
          List(cols(7), cols(8)).mkString(","))
        solr.add(doc)
    })
    solr.commit()
    solr.shutdown()
  }
}