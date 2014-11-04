package com.mycompany.solr4extras.neardups

import scala.math.abs
import scala.util.Random
import scala.util.hashing.MurmurHash3

import com.aliasi.tokenizer.RegExTokenizerFactory
import com.aliasi.tokenizer.TokenNGramTokenizerFactory

class ShingleProcessor(size: Int) {

  // everything in \\p{Punct} except "-", other exclusions may
  // be added as they are discovered
  val Puncts = """[!"#$%&\'()*+,./:;<=>?@[\\]^_`{|}~]""".r

  val factory = new TokenNGramTokenizerFactory(
    new RegExTokenizerFactory("\\S+"), size, size)

  def normalize(str: String): String = 
    Puncts.replaceAllIn(str.toLowerCase, " ")
      .replaceAll("\\s+", " ")
  
  def words(str: String): Array[String] = normalize(str).split(" ")
  
  def firstWord(str: String): String = words(str).head
  
  def numWords(str: String): Int = words(str).size
  
  def ngrams(str: String): Array[String] = {
	if (numWords(str) < size) Array(str)
	  else {
      val normStr = normalize(str)
      val tokenizer = factory.tokenizer(
        normStr.toCharArray(), 0, normStr.length())
      tokenizer.tokenize().toArray
	}
  }

  def hash(str: String): Int = MurmurHash3.stringHash(str, seed=42)
  
  def minHash(hashes: Array[Int]): Int = {
    hashes.toArray.sortWith(_ > _).head
  }
  
  def signatures(size: Int, str: String): Array[Int] = {
    Random.setSeed(42L)
    Array.fill(size)(Random.nextInt).map(mask => 
      minHash(ngrams(str).map(shingle => hash(shingle) ^ mask)))
  }
  
  def jaccard(set1: Set[String], set2: Set[String]): Float = 
    1.0F * set1.intersect(set2).size / set1.union(set2).size
}