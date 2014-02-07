package com.mycompany.solr4extras.dictann

import com.redis.RedisClient
import scala.collection.mutable.ArrayBuffer

class AhoCorasickRedis {

  val redis = new RedisClient("localhost", 6379)
  
  def prepare(keywords: List[String]): Unit = {
    keywords.foreach(keyword => {
      var prevCh = '\0'
      keyword.foreach(ch => {
        redis.sadd(tkey(prevCh), ch)
        prevCh = ch
      })
      redis.sadd(rkey(prevCh), keyword)
    })
  }
  
  def search(phrase: String): List[String] = {
    val matches = ArrayBuffer[String]()
    var prevCh = '\0'
    phrase.foreach(ch => {
      prevCh = if (redis.sismember(tkey(prevCh), ch)) ch 
               else '\0'
      val cmatches = redis.smembers(rkey(prevCh)) match {
        case Some(results) => results.flatten
        case None => List() 
      }
      matches ++= cmatches
    })
    matches.toSet.toList
  }
  
  def tkey(ch: Char) = "trn:" + ch
  def rkey(ch: Char) = "res:" + ch
}