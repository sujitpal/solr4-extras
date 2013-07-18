package com.mycompany.solr4extras.payloads

import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.flexible.core.QueryParserHelper
import org.apache.lucene.queryparser.flexible.core.nodes.{BoostQueryNode, FieldQueryNode, QueryNode}
import org.apache.lucene.queryparser.flexible.standard.builders.{BoostQueryNodeBuilder, StandardQueryBuilder, StandardQueryTreeBuilder}
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser
import org.apache.lucene.queryparser.flexible.standard.processors.StandardQueryNodeProcessorPipeline
import org.apache.lucene.search.Query
import org.apache.lucene.search.payloads.{AveragePayloadFunction, PayloadTermQuery}
import org.apache.solr.common.params.SolrParams
import org.apache.solr.common.util.NamedList
import org.apache.solr.request.SolrQueryRequest
import org.apache.solr.search.{QParser, QParserPlugin}

class PayloadQParserPlugin extends QParserPlugin {

  override def init(args: NamedList[_]): Unit = {}
  
  override def createParser(qstr: String, localParams: SolrParams, 
      params: SolrParams, req: SolrQueryRequest): QParser =  
    new PayloadQParser(qstr, localParams, params, req)
}

class PayloadQParser(qstr: String, localParams: SolrParams,
    params: SolrParams, req: SolrQueryRequest) 
    extends QParser(qstr, localParams, params, req) {
  
  req.getSearcher().setSimilarity(new PayloadSimilarity())
  
  override def parse(): Query = {
    val parser = new PayloadQueryParser()
    parser.parse(qstr, null).asInstanceOf[Query]
  }
}

class PayloadQueryParser extends QueryParserHelper(
    new StandardQueryConfigHandler(), 
    new StandardSyntaxParser(), 
    new StandardQueryNodeProcessorPipeline(null), 
    new PayloadQueryTreeBuilder()) {
}

class PayloadQueryTreeBuilder() extends StandardQueryTreeBuilder {
  
  setBuilder(classOf[FieldQueryNode], new PayloadQueryNodeBuilder())
  setBuilder(classOf[BoostQueryNode], new BoostQueryNodeBuilder())
}

class PayloadQueryNodeBuilder extends StandardQueryBuilder {
  
  override def build(queryNode: QueryNode): PayloadTermQuery = {
    val node = queryNode.asInstanceOf[FieldQueryNode]
    val fieldName = node.getFieldAsString()
    val payloadQuery = new PayloadTermQuery(
      new Term(fieldName, node.getTextAsString()), 
      new AveragePayloadFunction(), true)
    payloadQuery
  }
}
