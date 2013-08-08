package com.mycompany.solr4extras.cpos

import scala.io.Source
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.XML

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer
import org.apache.solr.common.SolrInputDocument
import org.junit.{After, Before, Test}

class DataLoader {

  val solrUrl = "http://localhost:8983/solr/"
  val solr = new ConcurrentUpdateSolrServer(solrUrl, 10, 1)

  @Before def setup(): Unit = {
    solr.deleteByQuery("*:*")  
  }
  
  @After def teardown(): Unit = {
    solr.shutdown()
  }

  @Test def loadData(): Unit = {
    var i = 0
    val xml = XML.loadString(Source.fromFile(
      "data/cpos/cpdata.xml").mkString)
    val docs = xml \\ "response" \\ "result" \\ "doc"
    docs.foreach(doc => {
      val itemtitle = (doc \\ "str" \\ "_").
        filter(node => node.attribute("name").
        exists(name => name.text == "itemtitle")).
        map(node => node.text).
        mkString
      val itemtitle_cp = (doc \\ "str" \\ "_").
        filter(node => node.attribute("name").
        exists(name => name.text == "itemtitle_cp")).
        map(node => node.text).
        mkString
      Console.println(itemtitle)
      Console.println(itemtitle_cp)
      Console.println("--")
      val solrDoc = new SolrInputDocument()
      solrDoc.addField("id", "cpos-" + i)
      solrDoc.addField("itemtitle", itemtitle)
      solrDoc.addField("itemtitle_cp", itemtitle_cp)
      solr.add(solrDoc)
      i = i + 1
      if (i % 100 == 0) {
        Console.println("%d records processed".format(i))
        solr.commit()
      }
    })
    solr.commit()
  }
}