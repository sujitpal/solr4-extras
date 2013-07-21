package com.mycompany.solr4extras.ner

import scala.Array.canBuildFrom

import org.apache.solr.client.solrj.impl.{ConcurrentUpdateSolrServer, HttpSolrServer}
import org.junit.{After, Assert, Before, Test}

import com.aliasi.chunk.RegExChunker
import com.aliasi.dict.{DictionaryEntry, ExactDictionaryChunker, MapDictionary}
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory

class ChunkersTest {

  val solrUrl = "http://localhost:8983/solr/"
  val solrWriter = new ConcurrentUpdateSolrServer(solrUrl, 10, 1)
  val solrReader = new HttpSolrServer(solrUrl)
  
  val texts = Array[String]( 
    "Cardiologist Berkeley 94701 Dr Chen",
    "Herpetologist 94015-1234",
    "Cost of care $100.00",
    "San Francisco points of interest",
    "Liberty Island, New York"
  )
  val cities = Array[String]("Berkeley", "San Francisco", "New York")
  val specialists = Array[String]("cardiologist", "herpetologist")
  
  @Before def setup(): Unit = {
//    solrWriter.deleteByQuery("*:*")
//    var i = 0
//    cities.foreach(city => {
//      val doc = new SolrInputDocument()
//      doc.addField("id", String.valueOf(i))
//      doc.addField("nercat", "city")
//      doc.addField("nerval", city)
//      solrWriter.add(doc)
//      i = i + 1
//    })
//    solrWriter.commit()
  }

  @After def teardown(): Unit = {
    solrWriter.shutdown()
    solrReader.shutdown()
  }

  @Test def testRegexChunking(): Unit = {
    val zipCodeChunker = new RegExChunker(
      "\\d{5}-\\d{4}|\\d{5}", "zipcode", 1.0D)
    val chunkers = new Chunkers(List(zipCodeChunker))
    val expected = Array[Int](1, 1, 0, 0, 0)
    val actuals = texts.map(text => chunkers.chunk(text).size)
    Assert.assertArrayEquals(expected, actuals)
  }
  
  @Test def testInMemoryDictChunking(): Unit = {
    val dict = new MapDictionary[String]()
    specialists.foreach(specialist => 
      dict.addEntry(
      new DictionaryEntry[String](specialist, "specialist", 1.0D)))
    val specialistChunker = new ExactDictionaryChunker(
      dict, IndoEuropeanTokenizerFactory.INSTANCE, false, false)   
    val chunkers = new Chunkers(List(specialistChunker))
    val expected = Array[Int](1, 1, 0, 0, 0)
    val actuals = texts.map(text => chunkers.chunk(text).size)
    Assert.assertArrayEquals(expected, actuals)
  }
  
  @Test def testSolrDictChunking(): Unit = {
    val dict = new SolrMapDictionary(solrReader, 10, "city")
    val cityChunker = new ExactDictionaryChunker(
      dict, IndoEuropeanTokenizerFactory.INSTANCE, false, false)
    val chunkers = new Chunkers(List(cityChunker))
    val expected = Array[Int](1, 0, 0, 1, 1)
    val actuals = texts.map(text => chunkers.chunk(text).size)
    Assert.assertArrayEquals(expected, actuals)
  }
}