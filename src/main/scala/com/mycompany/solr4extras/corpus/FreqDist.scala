package com.mycompany.solr4extras.corpus

import com.twitter.scalding.{Tsv, Job, Args}

import cascading.pipe.joiner.LeftJoin

/**
 * Reads input of the form (term docID freq), removes stopword
 * terms based on a stop word list, sums up the term frequency
 * across docs and outputs the term frequency counts sorted by
 * count descending as (term count).
 * NOTE: this can also be done directly from Lucene using 
 * totalTermFreq.
 */
class FreqDist(args: Args) extends Job(args) {

  val stopwords = Tsv(args("stopwords"), ('stopword)).read
  val input = Tsv(args("input"), ('term, 'docID, 'freq))
  val output = Tsv(args("output"))
  input.read.
    joinWithSmaller('term -> 'stopword, stopwords, joiner = new LeftJoin).
    filter('stopword) { stopword: String => (stopword == null || stopword.isEmpty) }.
    groupBy('term) { _.sum('freq) }.
    groupAll { _.sortBy('freq).reverse }.
    write(output)
}