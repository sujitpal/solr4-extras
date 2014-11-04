package com.mycompany.solr4extras.neardups

import java.io.File

import org.junit.Assert
import org.junit.Test

class SearcherTest {

  val searcher = new Searcher(3, 20)
  
  @Test
  def testWordEditDistance(): Unit = {
    val words1 = "lipstick on pig".split("\\s+")
    val words2 = "lipstick on a pig".split("\\s+")
    val words3 = "lipstick on the pig".split("\\s+")
    Assert.assertEquals(0, searcher.editDistance(words1, words1)) // I
    Assert.assertEquals(1, searcher.editDistance(words1, words2)) // A
    Assert.assertEquals(1, searcher.editDistance(words2, words1)) // D
    Assert.assertEquals(1, searcher.editDistance(words2, words3)) // S
  }

  @Test
  def testFindNearDupsByShingles(): Unit = {
    val docId = 3
    val docs = searcher.findNearDupsByShingles(docId, 2)
    Console.println("Near dups (shingles) for docID: %d".format(docId))
    docs.foreach(doc => Console.println("%s '%s' (%d) %.3f"
      .format(doc.id, doc.content, doc.editDist, doc.score)))
  }
  
  @Test
  def testFindNearDupsByMinhash(): Unit = {
    val docId = 3
    val docs = searcher.findNearDupsByMinhash(docId, 2)
    Console.println("Near dups (minhash) for docID: %d".format(docId))
    docs.foreach(doc => Console.println("%s '%s' (%d) %.3f"
      .format(doc.id, doc.content, doc.editDist, doc.score)))
  }
  
  @Test
  def testRunReports(): Unit = {
    Console.println("==== SHINGLES ====")
    searcher.reportNearDups(new File("/tmp/near_dups_shingles.csv"), 
      1, 864, 2, true)
    Console.println("==== MINHASH ====")
    searcher.reportNearDups(new File("/tmp/near_dups_minhash.csv"), 
      1, 864, 2, false)
  }
}