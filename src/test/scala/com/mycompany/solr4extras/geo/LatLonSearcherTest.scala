package com.mycompany.solr4extras.geo

import org.junit.Test
import org.junit.Assert

class LatLonSearcherTest {

  val dcLocation = Point(38.89, -77.04)

  val searcher = new LatLonSearcher()
  
  @Test
  def testFindWithinByGeofilt(): Unit = {
    val results = searcher.findWithin(
      "geofilt", dcLocation, 50, false, false)
    val neighborStates = results.map(_.state).toSet
    Assert.assertEquals(9, results.size)
    Assert.assertEquals(2, neighborStates.size)
    Assert.assertTrue(neighborStates.contains("MD") &&
      neighborStates.contains("VA"))
  }

  @Test
  def testFindWithinByBbox(): Unit = {
    val results = searcher.findWithin(
      "bbox", dcLocation, 50, false, false)
    val neighborStates = results.map(_.state).toSet
    Assert.assertEquals(10, results.size)
    Assert.assertEquals(2, neighborStates.size)
    Assert.assertTrue(neighborStates.contains("MD") &&
      neighborStates.contains("VA"))
  }

  @Test
  def testSortByDistance(): Unit = {
    val results = searcher.findWithin(
      "bbox", dcLocation, 50, true, true)
    Assert.assertTrue(results.head.dist < results.last.dist)
    val formattedResults = results
      .map(result => "%s, %s %s %s (%.2f km)"
      .format(result.street, result.city, result.state, 
              result.zip, result.dist))
      .foreach(Console.println(_))
  }
}
