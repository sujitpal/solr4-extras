package com.mycompany.solr4extras.payloads

import org.apache.lucene.search.similarities.PerFieldSimilarityWrapper
import org.apache.lucene.search.similarities.Similarity
import org.apache.lucene.search.similarities.DefaultSimilarity

class MyCompanySimilarityWrapper extends PerFieldSimilarityWrapper {

  override def get(fieldName: String): Similarity = fieldName match {
    case "payloads"|"cscores" => new PayloadSimilarity()
    case _ => new DefaultSimilarity()
  }
}