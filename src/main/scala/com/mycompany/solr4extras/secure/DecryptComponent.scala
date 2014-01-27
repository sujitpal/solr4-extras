package com.mycompany.solr4extras.secure

import java.io.{FileInputStream, File}
import java.util.Properties

import scala.collection.JavaConversions.seqAsJavaList

import org.apache.solr.common.{SolrDocumentList, SolrDocument}
import org.apache.solr.core.SolrCore
import org.apache.solr.handler.component.{SearchComponent, ResponseBuilder}
import org.apache.solr.response.ResultContext
import org.apache.solr.util.plugin.SolrCoreAware

class DecryptComponent extends SearchComponent with SolrCoreAware {

  var mongoDao: MongoDao = null
  var defaultFieldList: List[String] = null
  
  def getDescription(): String = 
    "Decrypts record with AES key for user identified by email"

  def getSource(): String = "DecryptComponent.scala"

  override def inform(core: SolrCore): Unit = {
    val props = new Properties()
    props.load(new FileInputStream(new File(
      core.getResourceLoader.getConfigDir, "secure.properties")))
    val host = props.get("mongo.host").asInstanceOf[String]
    val port = Integer.valueOf(props.get("mongo.port").asInstanceOf[String])
    val db = props.get("mongo.db").asInstanceOf[String]
    mongoDao = new MongoDao(host, port, db)
    defaultFieldList = props.get("default.fl").asInstanceOf[String].
      split(",").toList
  }
  
  override def prepare(rb: ResponseBuilder): Unit = { /* NOOP */ }

  override def process(rb: ResponseBuilder): Unit = {
    val params = rb.req.getParams
    val dfl = if (params.get("fl").isEmpty || params.get("fl") == "*") 
      defaultFieldList
      else rb.req.getParams.get("fl").split(",").toList
    val email = rb.req.getParams.get("email")
    if (! email.isEmpty) {
      // get docIds returned by previous component
      val nl = rb.rsp.getValues
      val ictx = nl.get("response").asInstanceOf[ResultContext]
      var docids = List[Integer]()
      val dociter = ictx.docs.iterator
      while (dociter.hasNext) docids = dociter.nextDoc :: docids
      // extract message_ids from the index and populate list
      val searcher = rb.req.getSearcher
      val mfl = new java.util.HashSet[String](List("message_id"))
      val messageIds = docids.reverse.map(docid => 
        searcher.doc(docid, mfl).get("message_id"))
      // populate a SolrDocumentList from index
      val solrdoclist = new SolrDocumentList
      solrdoclist.setMaxScore(ictx.docs.maxScore)
      solrdoclist.setNumFound(ictx.docs.matches)
      solrdoclist.setStart(ictx.docs.offset)
      val docs = mongoDao.getDecryptedDocs(email, dfl, messageIds).
        map(fieldmap => {
          val doc = new SolrDocument()
          fieldmap.keys.toList.map(fn => fieldmap(fn) match {
              case value: String =>
                doc.addField(fn, value.asInstanceOf[String])
              case value: List[_] => 
                value.asInstanceOf[List[String]].map(v =>
                  doc.addField(fn, v))
          })
          doc
      })
      solrdoclist.addAll(docs)
      // swap the response with the generated one
      rb.rsp.getValues().remove("response")
      rb.rsp.add("response", solrdoclist)
    }
  }
}
