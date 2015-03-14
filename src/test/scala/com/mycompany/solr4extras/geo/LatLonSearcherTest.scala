package com.mycompany.solr4extras.geo

import org.junit.Test
import org.junit.Assert

class LatLonSearcherTest {

  val washington = TestLatLon("1700 G St NW, Washington DC 20552", Point(38.89, -77.04))

  val searcher = new LatLonSearcher()
  
  @Test
  def testFindWithinByGeofilt(): Unit = {
    val results = searcher.findWithinByGeofilt(
      washington.loc, 50, false, false)
    val neighborStates = results.map(_.state).toSet
    Assert.assertEquals(9, results.size)
    Assert.assertEquals(2, neighborStates.size)
    Assert.assertTrue(neighborStates.contains("MD") &&
      neighborStates.contains("VA"))
  }

  @Test
  def testFindWithinByBbox(): Unit = {
    val results = searcher.findWithinByBbox(
      washington.loc, 50, false, false)
    val neighborStates = results.map(_.state).toSet
    Assert.assertEquals(10, results.size)
    Assert.assertEquals(2, neighborStates.size)
    Assert.assertTrue(neighborStates.contains("MD") &&
      neighborStates.contains("VA"))
  }

  @Test
  def testSortByDistance(): Unit = {
    val results = searcher.findWithinByBbox(
      washington.loc, 50, true, true)
    results.foreach(Console.println(_))
  }
}

case class TestLatLon(addr: String, loc: Point)