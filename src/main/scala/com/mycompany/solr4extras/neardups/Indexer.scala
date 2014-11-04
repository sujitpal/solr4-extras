package com.mycompany.solr4extras.neardups

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import scala.io.Source

import org.apache.commons.codec.digest.DigestUtils
import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.common.SolrInputDocument

class Indexer(ngramSize: Int, sigSize: Int) {

  val SolrUrl = "http://localhost:8983/solr/collection1"
  val InputFile = new File("/home/sujit/Downloads/restaurant/data/addresses.txt")

  def buildIndex(): Unit = {
    val solr = new HttpSolrServer(SolrUrl)
    val processor = new ShingleProcessor(ngramSize)
    Source.fromFile(InputFile).getLines
      .foreach(line => {
        val splitAt = line.indexOf(' ')
        val id = line.substring(0, splitAt).toInt
        val data = processor.normalize(line.substring(splitAt + 1))
        if (id % 100 == 0) {
          Console.println("Processing record: %s".format(id))
          solr.commit()
        }
        val doc = new SolrInputDocument()
        doc.addField("id", id.toString)
        doc.addField("content", data)
        // to quickly check for exact match
        doc.addField("md5_hash", DigestUtils.md5Hex(data))
        // to limit scope of search
        doc.addField("num_words", processor.numWords(data))
        doc.addField("first_word", processor.firstWord(data))
        // compute shingles
        processor.ngrams(data).foreach(ngram => 
          doc.addField("content_ng", ngram))
        // compute the minhash signature matrix
        processor.signatures(sigSize, data).foreach(sig => 
          doc.addField("content_sg", sig.toString))
        solr.add(doc)
      })
      Console.println("Cleaning up... complete")
      solr.commit()
  }
}
