package com.mycompany.solr4extras.corpus

import java.io.{PrintWriter, FileWriter, File}

import org.apache.lucene.index.{MultiFields, IndexReader, DocsEnum}
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.util.BytesRef

/**
 * Reads a Lucene4 index (new API) and writes out a
 * text file as (term, docID, frequency_of_term_in_doc).
 * @param indexDir the location of the Lucene index.
 * @param outputFile the output file name.
 * @param minDocFreq terms which are present in fewer
 *        documents than minDocFreq will be ignored. 
 * @param minTTF the minimum Total Term Frequency a 
 *        term must have to be considered for inclusion.
 * @param minTermFreq the minimum term frequency within
 *        a document so the term is included.
 */
class Lucene4TermFreq(indexDir: String) {

  def generate(outputFile: String, minDocs: Int,
      minTTF: Int, minTermFreq: Int): Unit = {
    val reader = IndexReader.open(
      new NIOFSDirectory(new File(indexDir), null))
    val writer = new PrintWriter(new FileWriter(outputFile), true)
    val terms = MultiFields.getTerms(reader, "body").iterator(null)
    var term: BytesRef = null
    var docs: DocsEnum = null
    do {
      term = terms.next
      if (term != null) {
        val docFreq = terms.docFreq
        val ttf = terms.totalTermFreq
        if (docFreq > minDocs && ttf > minTTF) {
          docs = terms.docs(null, docs)
          var docID: Int = -1
          do {
            docID = docs.nextDoc
            if (docID != DocIdSetIterator.NO_MORE_DOCS) {
              val termFreq = docs.freq
              if (termFreq > minTermFreq)
                writer.println("%s\t%d\t%d".format(
                  term.utf8ToString, docID, docs.freq))
            }
          } while (docID != DocIdSetIterator.NO_MORE_DOCS)
        }
      }
    } while (term != null)
    writer.flush
    writer.close
    reader.close
  }
}