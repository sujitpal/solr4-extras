package com.mycompany.solr4extras.secure

import org.apache.commons.codec.binary.Hex
import org.junit.Assert.assertEquals
import org.junit.Test

class CryptUtilsTest {
  
  @Test def testSymmetricEncryptDecrypt() = {
    val keys = CryptUtils.keys
    val daten = Array(
        "Sam I am, I am Sam, I love Green Eggs and Ham",
        """
        Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal.
        Now we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war. We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this.
        But, in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion -- that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom -- and that government of the people, by the people, for the people, shall not perish from the earth.
        """)
    daten.foreach(data => {
      val encoded = CryptUtils.encrypt(data.getBytes, keys._1, keys._2)
      val hexEncoded = Hex.encodeHexString(encoded)
      val decoded = CryptUtils.decrypt(Hex.decodeHex(
          hexEncoded.toCharArray), keys._1, keys._2)
      println("decoded=[" +  decoded + "]")
      assertEquals(decoded, data)
    })
  }
  
  @Test def decryptFromMongoString() = {
    val body = "200d2e5e516630617de7551d77f085c8cf4536b597098d4e35b6d85b54c627e1"
    val keys = (
      Hex.decodeHex("0d427e2942dad65dca885b4e09a83997".toCharArray),
      Hex.decodeHex("31f56401324ae6f83612f6a2a26fcace".toCharArray))
    val decoded = CryptUtils.decrypt(Hex.decodeHex(body.toCharArray), keys._1, keys._2)
    println("decoded=[" + decoded + "]")
    assertEquals("\nHere is our forecast", decoded)
  }
}