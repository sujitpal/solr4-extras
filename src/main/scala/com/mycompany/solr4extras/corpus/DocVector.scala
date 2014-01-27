package com.mycompany.solr4extras.corpus

import java.io.{PrintWriter, FileWriter, File}

import scala.collection.mutable.ListBuffer
import scala.io.Source

import org.apache.mahout.math.{VectorWritable, SequentialAccessSparseVector}

import com.twitter.scalding.{Tsv, TextLine, Job, Args}

import cascading.pipe.joiner.LeftJoin

/**
 * Reads input of the form (term freq) of most frequent terms,
 * and builds a dictionary file. Using this file, creates a 
 * collection of docID to sparse doc vector mappings of the 
 * form (docID, {termID:freq,...}).
 */
class DocVector(args: Args) extends Job(args) {

  val input = Tsv(args("input"), ('term, 'docID, 'freq))
  val termcounts = TextLine(args("termcounts"))
  
  val dictOutput = Tsv(args("dictionary"))
  val output = Tsv(args("docvector"))
  
  // (term freq) => (term num)
  val dictionary = termcounts.read.
    project('num, 'line).
    map('line -> 'word) { line: String => line.split('\t')(0) }.
    project('word, 'num)

  // input: (term, docID, freq)
  // join with dictionary ond write document as (docId, docvector) 
  input.read.
    joinWithSmaller('term -> 'word, dictionary, joiner = new LeftJoin).
    filter('word) { word: String => (!(word == null || word.isEmpty)) }.
    project('docID, 'num, 'freq).
    map(('docID, 'num, 'freq) -> ('docId, 'pvec)) { 
      doc: (String, Int, Int) => {
        val pvec = new SequentialAccessSparseVector(
          args("vocabsize").toInt)
        pvec.set(doc._2, doc._3)
      (doc._1, new VectorWritable(pvec))
    }}.
    groupBy('docId) { 
      group => group.reduce('pvec -> 'vec) {
        (left: VectorWritable, right: VectorWritable) => 
          new VectorWritable(left.get.plus(right.get).normalize)
    }}.
    write(output)
    
    // save the dictionary as (term, idx)    
    dictionary.write(dictOutput)
}

/**
 * Converts the Document Vector file to an ARFF file for 
 * consumption by Weka.
 */
class DocVectorToArff {
  
  def generate(input: String, output: String, 
      numDimensions: Int): Unit = {
    val writer = new PrintWriter(new FileWriter(new File(output)), true)
    // header
    writer.println("@relation docvector\n")
    (1 to numDimensions).map(n => 
      writer.println("@attribute vec" + n + " numeric"))
    writer.println("\n@data\n")
    // body
    Source.fromFile(new File(input)).getLines.foreach(line => { 
      writer.println(line.split('\t')(1).replaceAll(":", " "))
    })
    writer.flush
    writer.close
  }
}

/**
 * Reads output from Weka Explorer SimpleKMeans run (slightly
 * modified to remove header information) to produce a list
 * of top N words from each cluster.
 */
class WekaClusterDumper {
  
  def dump(input: String, dictionary: String, 
      output: String, topN: Int): Unit = {
    
    // build up map of terms from dictionary
    val dict = Source.fromFile(new File(dictionary)).getLines.
      map(line => { 
        val cols = line.split("\t")
        cols(1).toInt -> cols(0)
    }).toMap
    // build up elements list from weka output
    var clusterScores = new Array[ListBuffer[(Int,Double)]](5)
    Source.fromFile(new File(input)).getLines.
      foreach(line => {
        val cols = line.split("\\s+")
        val idx = cols(0).toInt - 1
        val scores = cols.slice(2, 7) 
        (0 to 4).foreach(i => 
          if (scores(i).toDouble > 0.0D) {
            if (clusterScores(i) == null)
              clusterScores(i) = new ListBuffer[(Int,Double)]
            clusterScores(i) += ((idx, scores(i).toDouble))
        })
    })
    // sort each clusterScore by score descending and get the
    // corresponding words from the dictionary by idx
    val writer = new PrintWriter(new FileWriter(new File(output)), true)
    var i = 0
    clusterScores.foreach(clusterScore => {
      writer.println("Cluster #" + i)
      clusterScore.toList.sortWith(_._2 > _._2).
        slice(0, topN).map(tuple => {
          val word = dict(tuple._1)
          writer.println("  " + word + " (" + tuple._2 + ")")
        })
      i = i + 1
    })
    writer.flush
    writer.close
  }
}