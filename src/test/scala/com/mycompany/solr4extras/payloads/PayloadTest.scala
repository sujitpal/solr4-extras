package com.mycompany.solr4extras.payloads

import scala.collection.JavaConversions.mapAsJavaMap

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer
import org.apache.solr.common.params.{CommonParams, MapSolrParams}
import org.junit.{After, Assert, Before, Test}

class PayloadTest {
  
  val solr = new ConcurrentUpdateSolrServer(
    "http://localhost:8983/solr/", 10, 1)

  @Before def setup(): Unit = {
//    solr.deleteByQuery("*:*")
//    solr.commit()
//    val randomizer = new Random(42)
//    val concepts = ('A' to 'Z').map(_.toString)
//    val ndocs = 10000
//    for (i <- 0 until ndocs) {
//      val nconcepts = randomizer.nextInt(10)
//      val alreadySeen = Set[String]()
//      val cscores = ArrayBuffer[(String,Float)]()
//      for (j <- 0 until nconcepts) {
//        val concept = concepts(randomizer.nextInt(26))
//        if (! alreadySeen.contains(concept)) {
//          cscores += ((concept, randomizer.nextInt(100)))
//          alreadySeen += concept
//        }
//      }
//      val title = "{" + 
//        cscores.sortWith((a, b) => a._1 < b._1).
//          map(cs => cs._1 + ":" + cs._2).
//          mkString(", ") + 
//          "}"
//      val payloads = cscores.map(cs => cs._1 + "|" + cs._2).
//        mkString(" ")
//      Console.println(payloads)
//      val doc = new SolrInputDocument()
//      doc.addField("id", i)
//      doc.addField("title", title)
//      doc.addField("cscores", payloads)
//      solr.add(doc)
//    }
//    solr.commit()
  }
  
  @After def teardown(): Unit = solr.shutdown()

  @Test def testAllDocsQuery(): Unit = {
    val params = new MapSolrParams(Map(
      (CommonParams.Q -> "*:*")))   
    val rsp = solr.query(params)
    Assert.assertEquals(10000, rsp.getResults().getNumFound()) 
  }
  
  @Test def testSingleConceptQuery(): Unit = {
    runQuery("cscores:A")
  }
  
  @Test def testAndConceptQuery(): Unit = {
    runQuery("cscores:A AND cscores:B")
  }
  
  @Test def testOrConceptQuery(): Unit = {
    runQuery("cscores:A OR cscores:B")
  }

  @Test def testBoostedOrConceptQuery(): Unit = {
    runQuery("cscores:A^10.0 OR cscores:B")
  }

  @Test def testBoostedAndConceptQuery(): Unit = {
    runQuery("cscores:A^10.0 AND cscores:B")
  }

  def runQuery(q: String): Unit = {
    val params = new MapSolrParams(Map(
        CommonParams.QT -> "/cselect",
        CommonParams.Q -> q,
        CommonParams.FL -> "*,score"))
    val rsp = solr.query(params)
    val dociter = rsp.getResults().iterator()
    Console.println("==== Query %s ====".format(q))
    while (dociter.hasNext()) {
      val doc = dociter.next()
      Console.println("%f: (%s) %s".format(
        doc.getFieldValue("score"), 
        doc.getFieldValue("id"), 
        doc.getFieldValue("title")))
    }
  }
}