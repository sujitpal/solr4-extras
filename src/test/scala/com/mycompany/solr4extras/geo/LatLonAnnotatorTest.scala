package com.mycompany.solr4extras.geo

import org.junit.Test
import org.apache.commons.io.FileUtils
import java.io.File
import org.junit.Assert

class LatLonAnnotatorTest {

  @Test
  def testAnnotate(): Unit = {
    val annotator = new LatLonAnnotator()
    val latlon = annotator.annotate(
      "1600 Amphitheatre Parkway, Mountain View, CA")
    Assert.assertEquals(37.4224764, latlon._1, 0.01)
    Assert.assertEquals(-122.0842499, latlon._2, 0.01)
  }

  @Test
  def testBatchAnnotate(): Unit = {
    val annotator = new LatLonAnnotator()
    annotator.batchAnnotate(
      new File("src/main/resources/us-500-copy.csv"),
      new File("/tmp/output.csv"))
  }
}