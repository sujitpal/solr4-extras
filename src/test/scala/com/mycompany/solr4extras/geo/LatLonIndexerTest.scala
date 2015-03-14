package com.mycompany.solr4extras.geo

import org.junit.Test

class LatLonIndexerTest {

  @Test
  def testBuildIndex(): Unit = {
    val indexer = new LatLonIndexer()
    indexer.buildIndex()
  }
}