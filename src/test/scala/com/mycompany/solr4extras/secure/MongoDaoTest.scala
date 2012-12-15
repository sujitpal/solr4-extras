package com.mycompany.solr4extras.secure

import org.apache.commons.codec.binary.Hex
import org.junit.Assert._
import org.junit.Test

case class Foo(a: Int, b: Int)

class MongoDaoTest {

  @Test def testSortingByExternalList() = {
    val lref = List(2, 1, 3)
    val ldata = List(Foo(1, 1), Foo(2, 1), Foo(3, 1))
    println(lref.indexOf(3))
    println(ldata.sortWith((x, y) => lref.indexOf(x.a) < lref.indexOf(y.b)))
  }

  @Test def testGetKeys() = {
    val mongoDao = new MongoDao("localhost", 27017, "solr4secure")
    val (key, iv) = mongoDao.getKeys("phillip.allen@enron.com")
    assertEquals(Hex.encodeHexString(key), "65c4bf74e2059b8a68bf0e0277c171fa")
    assertEquals(Hex.encodeHexString(iv), "48b380405e253eeb966554c9c2264940")
  }
}