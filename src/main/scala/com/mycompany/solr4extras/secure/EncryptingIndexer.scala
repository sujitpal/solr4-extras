package com.mycompany.solr4extras.secure

import java.io.File
import java.text.SimpleDateFormat

import scala.Array.canBuildFrom
import scala.collection.immutable.Stream.consWrapper
import scala.collection.immutable.HashMap
import scala.io.Source

import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.common.SolrInputDocument

import akka.actor.actorRef2Scala
import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import akka.routing.RoundRobinRouter

object EncryptingIndexer extends App {

  val props = properties(new File("conf/secure/secure.properties"))
  val system = ActorSystem("Solr4ExtrasSecure")
  val reaper = system.actorOf(Props[IndexReaper], name="reaper")
  val master = system.actorOf(Props(new IndexMaster(props, reaper)), 
    name="master")
  master ! StartMsg
  
  ///////////////// actors and messages //////////////////
  sealed trait AbstractMsg
  case class StartMsg extends AbstractMsg
  case class IndexMsg(file: File) extends AbstractMsg
  case class IndexedMsg(status: Int) extends AbstractMsg
  case class StopMsg extends AbstractMsg
  
  /**
   * The master actor starts up the workers and the reaper,
   * then populates the input queue for the IndexWorker actors.
   * It also handles counters to track the progress of the job
   * and once the work is done sends a message to the Reaper
   * to shut everything down.
   */
  class IndexMaster(props: Map[String,String], reaper: ActorRef)
      extends Actor {

    val mongoDao = new MongoDao(props("mongo.host"),
        props("mongo.port").toInt,
        props("mongo.db"))
    val solrServer = new HttpSolrServer(props("solr.server"))
    val numWorkers = props("num.workers").toInt
    val router = context.actorOf(
      Props(new IndexWorker(mongoDao, solrServer)).
      withRouter(RoundRobinRouter(numWorkers)))
    
    var nreqs = 0
    var nsuccs = 0
    var nfails = 0
    
    override def receive = {
      case StartMsg => {
        val files = walk(new File(props("data.root.dir"))).
          filter(x => x.isFile)
        for (file <- files) {
          println("adding " + file + " to worker queue")
          nreqs = nreqs + 1
          router ! IndexMsg(file)
        }
      }
      case IndexedMsg(status) => {
        if (status == 0) nsuccs = nsuccs + 1 else nfails = nfails + 1
        val processed = nsuccs + nfails
        if (processed % 100 == 0) {
          solrServer.commit
          println("Processed %d/%d (success=%d, failures=%d)".
            format(processed, nreqs, nsuccs, nfails))
        }
        if (nreqs == processed) {
          solrServer.commit
          println("Processed %d/%d (success=%d, failures=%d)".
            format(processed, nreqs, nsuccs, nfails))
          reaper ! StopMsg
          context.stop(self)
        }
      }
    }
  }
  
  /**
   * These actors do the work of parsing the input file, encrypting
   * the content and writing the encrypted data to MongoDB and the
   * unstored data to Solr.
   */
  class IndexWorker(mongoDao: MongoDao, solrServer: HttpSolrServer) 
      extends Actor {
    
    override def receive = {
      case IndexMsg(file) => {
        val doc = parse(Source.fromFile(file))
        try {
          mongoDao.saveEncryptedDoc(doc)
          addToSolr(doc, solrServer)
          sender ! IndexedMsg(0)
        } catch {
          case e: Exception => {
            e.printStackTrace
            sender ! IndexedMsg(-1)
          }
        }
      }
    }
  }
  
  /**
   * The Reaper shuts down the system once everything is done.
   */
  class IndexReaper extends Actor {
    override def receive = {
      case StopMsg => {
        println("Shutting down Indexer")
        context.system.shutdown
      }
    }
  }
  
  ///////////////// global functions /////////////////////
  
  /**
   * Add the document, represented as a Map[String,Any] name-value
   * pairs to the Solr index. Note that the schema sets all these
   * values to tokenized+unstored, so all we have in the index is
   * the inverted index for these fields.
   * @param doc the Map[String,Any] set of field key-value pairs.
   * @param server a reference to the Solr server.
   */
  def addToSolr(doc: Map[String,Any], server: HttpSolrServer): Unit = {
    val solrdoc = new SolrInputDocument()
    doc.keys.map(key => doc(key) match {
      case value: String => 
        solrdoc.addField(normalize(key), value.asInstanceOf[String])
      case value: Array[String] => 
        value.asInstanceOf[Array[String]].
          map(v => solrdoc.addField(normalize(key), v)) 
    })
    server.add(solrdoc)
  }

  /**
   * Normalize keys so they can be used without escaping in
   * Solr and MongoDB.
   * @param key the un-normalized string.
   * @return the normalized key (lowercased and space and 
   *         hyphen replaced by underscore).
   */
  def normalize(key: String): String = 
    key.toLowerCase.replaceAll("[ -]", "_")
    
  /**
   * Parse the email file into a set of name value pairs.
   * @param source the Source object representing the file.
   * @return a Map of name value pairs.
   */
  def parse(source: Source): Map[String,Any] = {
    parse0(source.getLines(), HashMap[String,Any](), false).
      filter(x => x._2 != null)
  }
  
  private def parse0(lines: Iterator[String], 
      map: Map[String,Any], startBody: Boolean): 
      Map[String,Any] = {
    if (lines.isEmpty) map
    else {
      val head = lines.next()
      if (head.trim.length == 0) parse0(lines, map, true)
      else if (startBody) {
        val body = map.getOrElse("body", "") + "\n" + head
        parse0(lines, map + ("body" -> body), startBody)
      } else {
        val split = head.indexOf(':')
        if (split > 0) {
          val kv = (head.substring(0, split), head.substring(split + 1))
          val key = kv._1.map(c => if (c == '-') '_' else c).trim.toLowerCase
          val value = kv._1 match {
            case "Date" => 
              formatDate(kv._2.trim)
            case "Cc" | "Bcc" | "To" => 
              kv._2.split("""\s*,\s*""")
            case "Message-ID" | "From" | "Subject" | "body" => 
              kv._2.trim
            case _ => null
          }
          parse0(lines, map + (key -> value), startBody)
        } else parse0(lines, map, startBody)
      }
    }
  }
  
  def formatDate(date: String): String = {
    lazy val parser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss")
    lazy val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    formatter.format(parser.parse(date.substring(0, date.lastIndexOf('-') - 1)))
  }

  def properties(conf: File): Map[String,String] = {
    Map() ++ Source.fromFile(conf).getLines().toList.
      filter(line => (! (line.isEmpty || line.startsWith("#")))).
      map(line => (line.split("=")(0) -> line.split("=")(1)))
  }

  def walk(root: File): Stream[File] = {
    if (root.isDirectory)
      root #:: root.listFiles.toStream.flatMap(walk(_))
    else root #:: Stream.empty
  }
}