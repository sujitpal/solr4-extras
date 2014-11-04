package com.mycompany.solr4extras.neardups

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

import scala.collection.JavaConversions._
import scala.math.min

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrRequest.METHOD
import org.apache.solr.client.solrj.impl.HttpSolrServer

class Searcher(ngramSize: Int, sigSize: Int) {

  val SolrUrl = "http://localhost:8983/solr/collection1"

  val processor = new ShingleProcessor(ngramSize)
  val solr = new HttpSolrServer(SolrUrl)

  def getById(id: Int): Document = {
    val idquery = new SolrQuery()
    idquery.setQuery("id:" + id)
    idquery.setRows(1)
    idquery.setFields("id", "content", "md5_hash")
    val idresp = solr.query(idquery, METHOD.POST)
    val iddoc = idresp.getResults().head
    Document(iddoc.getFieldValue("id").asInstanceOf[String],
      iddoc.getFieldValue("content").asInstanceOf[String],
      iddoc.getFieldValue("md5_hash").asInstanceOf[String],
      0, 0.0F)
  }
  
  def findNearDupsByShingles(docId: Int, maxDist: Int): List[Document] = {
    val doc = getById(docId)
    val queryString = processor.ngrams(doc.content)
      .map(ngram => "content_ng:\"" + ngram + "\"")
      .mkString(" OR ")
    nearDupSearch(docId, maxDist, queryString)
  }
  
  def findNearDupsByMinhash(docId: Int, maxDist: Int): List[Document] = {
    val doc = getById(docId)
    val queryString = processor.signatures(sigSize, doc.content)
      .map(sig => "content_sg:\"" + sig + "\"")
      .mkString(" OR ")
    nearDupSearch(docId, maxDist, queryString)
  }
  
  def nearDupSearch(docId: Int, maxDist: Int, q: String): List[Document] = {
    val sourceDoc = getById(docId)
    val query = new SolrQuery()
    // build shingle query
    query.setQuery(q)
    query.setFilterQueries("first_word:\"" + 
      processor.firstWord(sourceDoc.content) + "\"")
    query.setRows(10)
    query.setFields("id", "content", "md5_hash", "score")
    val resp = solr.query(query, METHOD.POST)
    var prevEditDist = 0
    resp.getResults().map(ndoc => {
      val hash = ndoc.getFieldValue("md5_hash").asInstanceOf[String]
      val content = ndoc.getFieldValue("content").asInstanceOf[String]
      val editDist = if (hash == sourceDoc.md5hash) 0 
        else if (prevEditDist <= maxDist)
          editDistance(processor.words(sourceDoc.content), 
                       processor.words(content))
        else Int.MaxValue
      prevEditDist = editDist
      new Document(ndoc.getFieldValue("id").asInstanceOf[String], 
        content, hash, editDist,
        ndoc.getFieldValue("score").asInstanceOf[Float])
    })
    .filter(doc => doc.id.toInt > sourceDoc.id.toInt)
    .filter(doc => doc.editDist <= maxDist)
    .toList
  }
  
  def reportNearDups(outfile: File, startDocId: Int, 
      endDocId: Int, editDist: Int, useShingles: Boolean): Unit = {
    val writer = new PrintWriter(new FileWriter(outfile), true)
    (startDocId until endDocId).foreach(docId => {
      val sdoc = getById(docId)
      val neighborDocs = if (useShingles) 
        findNearDupsByShingles(docId, editDist)
        else findNearDupsByMinhash(docId, editDist)
      if (neighborDocs.size > 0) {
    	Console.println("%s: %s".format(docId, sdoc.content))
        neighborDocs.foreach(ndoc => {
          writer.println("%s\t%s\t%d"
            .format(docId, ndoc.id, ndoc.editDist))
          Console.println("%s: %s (%d)"
            .format(ndoc.id, ndoc.content, ndoc.editDist))
        })
        Console.println("==")
      }
    })
    writer.flush()
    writer.close()
  }
  
  // adapted from rosettacode.org/wiki/Levenshtein_distance#Scala
  def editDistance(words1: Array[String], words2: Array[String]): Int = {
    val len1 = words1.length
    val len2 = words2.length
    val distances = Array.ofDim[Int](len1+1, len2+1)
    for (i <- 0 to len1;
         j <- 0 to len2) {
      if (j == 0) distances(i)(j) = i
      else if (i == 0) distances(i)(j) = j
      else distances(i)(j) = 0
    }
    for (i <- 1 to len1;
         j <- 1 to len2) {
      distances(i)(j) = if (words1(i-1).equals(words2(j-1))) 
          distances(i-1)(j-1)
        else minimum(
          distances(i-1)(j) + 1,  // deletion
          distances(i)(j-1) + 1,  // insertion
          distances(i-1)(j-1) + 1 // substitution
        )
    }
    distances(len1)(len2)
  }

  def minimum(i1: Int, i2: Int, i3: Int): Int = min(min(i1, i2), i3)
}

case class Document(id: String, 
                    content: String, 
                    md5hash: String, 
                    editDist: Int, 
                    score: Float)
