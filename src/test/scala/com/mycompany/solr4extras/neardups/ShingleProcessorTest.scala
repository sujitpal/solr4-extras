package com.mycompany.solr4extras.neardups

import org.junit.Test
import org.junit.Assert

class ShingleProcessorTest {
    
  val processor = new ShingleProcessor(3)

  @Test
  def testNormalize(): Unit = {
    val s = "21 Clear-Water  Cafe 168 W. Colorado Blvd. Los Angeles"
    val norms = processor.normalize(s)
    Console.println("normalized=" + norms)
    Assert.assertEquals("21 clear-water cafe 168 w colorado blvd los angeles", norms)
  }
  
  @Test
  def testFirstWord(): Unit = {
    val s = "Arnie Morton's of Chicago 435 S. La Cienega Blvd. Los Angeles"
    val fword = processor.firstWord(s)
    Console.println("first word=" + fword)
    Assert.assertEquals("arnie", fword)
  }
  
  @Test
  def testNumWords(): Unit = {
    val s = "Arnie Morton's of Chicago 435 S. La Cienega Blvd. Los Angeles"
    val numWords = processor.numWords(s)
    Console.println("number of words=" + numWords)
    Assert.assertEquals(12, numWords)
  }
  
  @Test
  def testNGrams(): Unit = {
    val s = "21 ClearWater Cafe 168 W. Colorado Blvd. Los Angeles"
    val ngrams = processor.ngrams(s)
    Console.println("ngrams=" + ngrams.toList)
    Assert.assertEquals(7, ngrams.size)
    Assert.assertEquals("21 clearwater cafe", ngrams.head)
  }
  
  @Test
  def testHashes(): Unit = {
    val s = "21 Clearwater Cafe 168 W. Colorado Blvd. Los Angeles"
    val hashes = processor.ngrams(s).map(shingle => processor.hash(shingle))
    Assert.assertEquals(7, hashes.size)
    Console.println("head(hashes)=" + hashes.head)
    Assert.assertEquals(420835737, hashes.head)
  }
  
  @Test
  def testMinHash(): Unit = {
    val s = "21 Clearwater Cafe 168 W. Colorado Blvd. Los Angeles"
    val hashes = processor.ngrams(s).map(shingle => processor.hash(shingle))
    val minhash = processor.minHash(hashes)
    Console.println("minhash=" + minhash)
    Assert.assertEquals(1829846867, minhash)
  }
  
  @Test
  def testSignatures(): Unit = {
    val s = "21 Clearwater Cafe 168 W. Colorado Blvd. Los Angeles"
    val signatures = processor.signatures(200, s)
    Assert.assertEquals(200, signatures.size)
    Console.println("head(signatures)=" + signatures.head)
    Assert.assertEquals(2047409978, signatures.head)
  }
  
  @Test
  def testSignatureOverlap(): Unit = {
    val s1 = "Art's Deli 12224 Ventura Blvd. Studio City"
    val s2 = "Art's Delicatessen 12224 Ventura Blvd. Studio City"
    val s3 = "Hotel Bel-Air 701 Stone Canyon Rd. Bel Air"
    Console.println("(s1,s1)\t(s1,s2)\t(s1,s3)")
    // base Jaccard similarity
    val simS1 = processor.jaccard(
      processor.words(s1).toSet, processor.words(s1).toSet)
    val simS2 = processor.jaccard(
      processor.words(s1).toSet, processor.words(s2).toSet)
    val simS3 = processor.jaccard(
      processor.words(s1).toSet, processor.words(s3).toSet)
    Console.println("%.3f\t%.3f\t%.3f\tJ(s,t)".format(simS1, simS2, simS3))
    // shingle similarity
    val shingleS1 = processor.jaccard(
      processor.ngrams(s1).toSet, processor.ngrams(s1).toSet)
    val shingleS2 = processor.jaccard(
      processor.ngrams(s1).toSet, processor.ngrams(s2).toSet)
    val shingleS3 = processor.jaccard(
      processor.ngrams(s1).toSet, processor.ngrams(s3).toSet)
    Console.println("%.3f\t%.3f\t%.3f\tJ(shingle(s), shingle(t))"
      .format(shingleS1, shingleS2, shingleS3))
    // hash of shingle similarity
    val hashS1 = processor.jaccard(
      processor.ngrams(s1).map(processor.hash(_).toString).toSet, 
      processor.ngrams(s1).map(processor.hash(_).toString).toSet)
    val hashS2 = processor.jaccard(
      processor.ngrams(s1).map(processor.hash(_).toString).toSet, 
      processor.ngrams(s2).map(processor.hash(_).toString).toSet)
    val hashS3 = processor.jaccard(
      processor.ngrams(s1).map(processor.hash(_).toString).toSet, 
      processor.ngrams(s3).map(processor.hash(_).toString).toSet)
    Console.println("%.3f\t%.3f\t%.3f\tJ(hash(shingle(s)), hash(shingle(t)))"
      .format(hashS1, hashS2, hashS3))
    // minhash signature similarity
    val mhashS1 = processor.jaccard(
      processor.signatures(20, s1).map(_.toString).toSet, 
      processor.signatures(20, s1).map(_.toString).toSet)
    val mhashS2 = processor.jaccard(
      processor.signatures(20, s1).map(_.toString).toSet, 
      processor.signatures(20, s2).map(_.toString).toSet)
    val mhashS3 = processor.jaccard(
      processor.signatures(20, s1).map(_.toString).toSet, 
      processor.signatures(20, s3).map(_.toString).toSet)
    Console.println("%.3f\t%.3f\t%.3f\tJ(minhash(shingle(s)), minhash(shingle(t)))"
      .format(mhashS1, mhashS2, mhashS3))
  }
}