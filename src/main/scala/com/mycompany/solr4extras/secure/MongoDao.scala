package com.mycompany.solr4extras.secure

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asScalaSet

import org.apache.commons.codec.binary.Hex

import com.mongodb.casbah.Imports._
import com.mycompany.solr4extras.secure.CryptUtils._

class MongoDao(host: String, port: Int, db: String) {

  val conn = MongoConnection(host, port)
  val users = conn(db)("users")
  val emails = conn(db)("emails")
  
  /**
   * Called from the indexing subsystem. The index document, 
   * represented as a Map of name-value pairs, is sent to this
   * method to be encrypted and persisted to a MongoDB collection.
   * @param doc the index document to be saved.
   */
  def saveEncryptedDoc(doc: Map[String,Any]): Unit = {
    val email = doc.get("from") match {
      case Some(x) => {
        val keypair = getKeys(x.asInstanceOf[String])
        val builder = MongoDBObject.newBuilder
        // save the message_id unencrypted since we will
        // need to look up using this
        builder += "message_id" -> doc("message_id")
        doc.keySet.filter(fn => (! fn.equals("message_id"))).
          map(fn => doc(fn) match {
          case value: String => {
            val dbval = Hex.encodeHexString(encrypt(
              value.asInstanceOf[String].getBytes, 
              keypair._1, keypair._2))
            builder += fn -> dbval
          }
          case value: Array[String] => { 
            val dbval = value.asInstanceOf[Array[String]].map(x => 
              Hex.encodeHexString(encrypt(
              x.getBytes, keypair._1, keypair._2)))
            builder += fn -> dbval
          }
        })
        emails.save(builder.result)
      }
      case None => 
        throw new Exception("Invalid Email, no sender, skip")
    } 
  }
  
  /**
   * Implements a pass-through cache. If the email can be found
   * in the cache, then it is returned from there. If not, the
   * MongoDB database is checked. If found, its returned from 
   * there, else it is created and stored in the database and map.
   * @param email the email address of the user.
   * @return pair of (key, initvector) for the user.
   */
  def getKeys(email: String): (Array[Byte], Array[Byte]) = {
    this.synchronized {
      val query = MongoDBObject("email" -> email)
      users.findOne(query) match {
        case Some(x) => {
          val keys = (Hex.decodeHex(x.as[String]("key").toCharArray), 
            Hex.decodeHex(x.as[String]("initvector").toCharArray))
          keys
        }
        case None => {
          val keys = CryptUtils.keys
          users.save(MongoDBObject(
            "email" -> email,
            "key" -> Hex.encodeHexString(keys._1),
            "initvector" -> Hex.encodeHexString(keys._2)
          ))
          keys
        }
      }
    }
  }
  
  /**
   * Called from the Solr DecryptComponent with list of docIds.
   * Retrieves the document corresponding to each id in the list
   * from MongoDB and returns it as a List of Maps, where each
   * document is represented as a Map of name and decrypted value 
   * pairs.
   * @param email the email address of the user, used to retrieve 
   *              the encryption key and init vector for the user.
   * @param fields the list of field names to return.
   * @param ids the list of docIds to return.
   * @return a List of Map[String,Any] documents.              
   */
  def getDecryptedDocs(email: String, fields: List[String], 
      ids: List[String]): List[Map[String,Any]] = {
    val (key, iv) = getKeys(email)
    val fl = MongoDBObject(fields.map(x => x -> 1))
    val cursor = emails.find("message_id" $in ids, fl)
    cursor.map(doc => getDecryptedDoc(doc, key, iv)).toList.
      sortWith((x, y) => 
        ids.indexOf(x("message_id")) < ids.indexOf(y("message_id")))
  }
  
  /**
   * Returns a document returned from MongoDB (as a DBObject)
   * decrypts it with the key and init vector, and returns the
   * decrypted object as a Map of name-value pairs.
   * @param doc the DBObject representing a single document.
   * @param key the byte array representing the AES key.
   * @param iv the init vector created at key creation.
   * @return a Map[String,Any] of name-value pairs, where values
   *         are decrypted.
   */
  def getDecryptedDoc(doc: DBObject, 
      key: Array[Byte], iv: Array[Byte]): Map[String,Any] = {
    val fieldnames = doc.keySet.toList.filter(fn => 
      (! "message_id".equals(fn)))
    val fieldvalues = fieldnames.map(fn => doc(fn) match {
      case value: String =>
        decrypt(Hex.decodeHex(value.asInstanceOf[String].toCharArray), 
          key, iv)
      case value: BasicDBList =>
        value.asInstanceOf[BasicDBList].toList.
          map(v => decrypt(Hex.decodeHex(v.asInstanceOf[String].toCharArray), 
          key, iv))
      case _ =>
        doc(fn).toString
    })
    Map("message_id" -> doc("message_id")) ++ 
      fieldnames.zip(fieldvalues)
  }
}