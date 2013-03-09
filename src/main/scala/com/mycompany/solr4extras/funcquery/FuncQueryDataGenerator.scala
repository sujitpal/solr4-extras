package com.mycompany.solr4extras.funcquery

import java.util.Random

import scala.collection.JavaConversions._

import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.common.SolrInputDocument

object FuncQueryDataGenerator extends App {
  
  generate()

  def generate(): Unit = {
    val solrServer = new HttpSolrServer("http://localhost:8983/solr/")
    solrServer.deleteByQuery("*:*")
    solrServer.commit()
    val randomGenerator = new Random()
    val titleWords = Array[String]("coffee", "cocoa", "sugar")
    for (i <- 0 until 1000) {
      val docs = (0 until 100).map(j => { 
        val ms = randomGenerator.nextFloat()
        val fs = randomGenerator.nextFloat()
        val mscore = Math.round(ms * 1000.0F)
        val fscore = Math.round(fs * 1000.0F)
        val word = titleWords(randomGenerator.nextInt(2))
        val title = word + ": M " + mscore + " F " + fscore
        println("adding title: " + title)
        val doc = new SolrInputDocument()
        doc.addField("id", ((i * 100) + j))
        doc.addField("mscore", mscore)
        doc.addField("fscore", fscore)
        doc.addField("title", title)
        doc
      })
      solrServer.add(docs)
      solrServer.commit()
    }
    solrServer.commit()
  }
}