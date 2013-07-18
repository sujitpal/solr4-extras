package com.mycompany.solr4extras.payloads

import org.apache.lucene.analysis.payloads.PayloadHelper
import org.apache.lucene.index.FieldInvertState
import org.apache.lucene.search.similarities.DefaultSimilarity
import org.apache.lucene.util.BytesRef

class PayloadSimilarity extends DefaultSimilarity {

  override def coord(overlap: Int, maxOverlap: Int) = 1.0F
  
  override def queryNorm(sumOfSquaredWeights: Float) = 1.0F
  
  override def lengthNorm(state: FieldInvertState) = state.getBoost()
  
  override def tf(freq: Float) = 1.0F
  
  override def sloppyFreq(distance: Int) = 1.0F
  
  override def scorePayload(doc: Int, start: Int, end: Int, 
      payload: BytesRef): Float = {
    if (payload == null) 1.0F
    else PayloadHelper.decodeFloat(payload.bytes, payload.offset)
  }
  
  override def idf(docFreq: Long, numDocs: Long) = 1.0F

  override def decodeNormValue(b: Byte) = 1.0F
}