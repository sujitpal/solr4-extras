package com.mycompany.solr4extras.ner

import scala.collection.JavaConversions.{asJavaIterator, asScalaIterator, asScalaSet, mapAsJavaMap}
import scala.collection.mutable.ArrayBuffer
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer
import org.apache.solr.common.params.{CommonParams, MapSolrParams}
import com.aliasi.dict.{DictionaryEntry, ExactDictionaryChunker, MapDictionary}
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.common.SolrInputDocument
import com.aliasi.chunk.RegExChunker
import com.aliasi.chunk.Chunk
import com.aliasi.chunk.Chunker
import collection.JavaConversions._

/**
 * Pass in a list of Chunkers during construction, and then
 * call the chunk method with the text to be chunked. Returns
 * a set of Chunk objects of various types in the text.
 */
class Chunkers(val chunkers: List[Chunker]) {

  def chunk(text: String): Set[Chunk] = chunkers.
    map(chunker => chunker.chunk(text).chunkSet.toList).
    flatten.
    toSet[Chunk]

  def mkString(text: String, chunk: Chunk): String = {
    val pair = mkPair(text, chunk)
    pair._1 + "/" + pair._2
  }
  
  def mkPair(text: String, chunk: Chunk): (String,String) = 
    (text.substring(chunk.start(), chunk.end()), 
      chunk.`type`())
}

/**
 * Custom MapDictionary backed by a Solr index. This is 
 * used by our Dictionary based NER (ExactMatchDictionaryChunker)
 * for large dictionaries of entity names. Dictionary entries
 * are stored as (category, value) pairs in Solr fields
 * (nercat, nerval).
 */
class SolrMapDictionary(
    val solr: SolrServer, val nrows: Int, val category: String) 
    extends MapDictionary[String] {

  override def addEntry(entry: DictionaryEntry[String]) = {} 
  
  override def iterator(): 
      java.util.Iterator[DictionaryEntry[String]] = {
    phraseEntryIt("*:*")
  }
  
  override def phraseEntryIt(phrase: String): 
      java.util.Iterator[DictionaryEntry[String]] = {
    val params = new MapSolrParams(Map(
      CommonParams.Q -> phrase,
      CommonParams.FQ -> ("nercat:" + category),
      CommonParams.FL -> "nerval",
      CommonParams.START -> "0", 
      CommonParams.ROWS -> String.valueOf(nrows)))
    val rsp = solr.query(params)
    rsp.getResults().iterator().
      toList.
      map(doc =>  new DictionaryEntry[String](
        doc.getFieldValue("nerval").asInstanceOf[String], 
        category, 1.0D)).
      iterator
  }
}
