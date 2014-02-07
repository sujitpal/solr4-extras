package com.mycompany.solr4extras.dictann

import org.junit.Test
import org.junit.Assert

class AhoCorasickRedisTest {

  @Test
  def testBuild(): Unit = {
    val aho = new AhoCorasickRedis()
    aho.prepare(List("Seahawks", "Broncos", "Super Bowl"))
    val matches = aho.search("The Seahawks defeated the Broncos at the Super Bowl.")
    Console.println("matches=" + matches)
    Assert.assertEquals(3, matches.size)
    Assert.assertTrue(matches.contains("Seahawks"))
    Assert.assertTrue(matches.contains("Broncos"))
    Assert.assertTrue(matches.contains("Super Bowl"))
  }

}