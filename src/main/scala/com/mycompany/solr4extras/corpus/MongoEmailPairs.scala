package com.mycompany.solr4extras.corpus

import java.io.{PrintWriter, FileWriter, File}
import java.util.concurrent.atomic.AtomicInteger

import org.apache.commons.codec.binary.Hex
import org.apache.lucene.index.IndexReader
import org.apache.lucene.store.NIOFSDirectory

import com.mongodb.casbah.Imports.{wrapDBObj, wrapDBList, MongoDBObject, MongoConnection, BasicDBList}
import com.mycompany.solr4extras.secure.CryptUtils

class MongoEmailPairs(host: String, port: Int, db: String, 
    indexDir: String) {

  val conn = MongoConnection(host, port)
  val emails = conn(db)("emails")
  val users = conn(db)("users")
  val reader = IndexReader.open(
    new NIOFSDirectory(new File(indexDir), null))

  def generate(refFile: String, outputFile: String): Unit = {
    val counter = new AtomicInteger(0)
    val userKeys = users.find().map(user => 
      user.as[String]("email") -> 
      (Hex.decodeHex(user.as[String]("key").toCharArray), 
      Hex.decodeHex(user.as[String]("initvector").toCharArray),
      counter.incrementAndGet)).toMap
    // write out dictionary file for reference
    val refWriter = new PrintWriter(new FileWriter(new File(refFile)), true)
    userKeys.map(user =>
      refWriter.println("%s\t%d".format(user._1, user._2._3))
    )
    refWriter.flush
    refWriter.close
    // write out main file as required by PageRank
    val dataWriter = new PrintWriter(new FileWriter(new File(outputFile)), true)
    val numdocs = reader.numDocs
    var i = 0
    while (i < numdocs) {
      val doc = reader.document(i)
      val messageID = doc.get("message_id").asInstanceOf[String]
      val author = doc.get("from").asInstanceOf[String]
      val mongoQuery = MongoDBObject("message_id" -> messageID)
      val cur = emails.find(mongoQuery)
      emails.findOne(mongoQuery) match {
        case Some(email) => {
          try {
            val from = CryptUtils.decrypt(
              Hex.decodeHex(email.as[String]("from").toCharArray), 
              userKeys(author)._1, userKeys(author)._2)
            val fromId = userKeys(from)._3
            val targets = 
              (try {
                email.as[BasicDBList]("to").toList  
              } catch {
                case e: NoSuchElementException => List()
              }) ++
              (try {
                email.as[BasicDBList]("cc").toList
              } catch {
                case e: NoSuchElementException => List()
              }) ++
              (try {
                email.as[BasicDBList]("bcc").toList
              } catch {
                case e: NoSuchElementException => List()
              })
            targets.map(target => {
              val targetEmail = CryptUtils.decrypt(Hex.decodeHex(
                target.asInstanceOf[String].toCharArray), 
                userKeys(author)._1, userKeys(author)._2).trim
              val targetEmailId = userKeys(targetEmail)._3
              dataWriter.println("%d\t%d".format(fromId, targetEmailId))
            })
          } catch {
            // TODO: BadPaddingException, likely caused by, 
            // problems during population. Fix, but skip for now
            case e: Exception => println("error, skipping")
          }
        }
        case None => // skip
      }
      i = i + 1
    }
    dataWriter.flush
    dataWriter.close
    reader.close
    conn.close
  }
}