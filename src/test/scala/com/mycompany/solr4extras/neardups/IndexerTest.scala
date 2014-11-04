package com.mycompany.solr4extras.neardups

import org.junit.Test

class IndexerTest {

  @Test
  def testBuildIndex(): Unit = {
    val indexer = new Indexer(3, 20)
    indexer.buildIndex()
  }
}