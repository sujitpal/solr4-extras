package com.mycompany.solr4extras.corpus

import com.twitter.scalding.Args

object CorpusAnalyzer extends App {

  //////////// document clustering ///////////
  
  (new Lucene4TermFreq("/Users/sujit/Downloads/apache-solr-4.0.0/example/solr/collection1/data/index")).
    generate("data/input/corpus_freqs.txt", 50, 1000, 10)

  (new FreqDist(Args(List(
    "--local", "", 
    "--input", "data/input/corpus_freqs.txt", 
    "--output", "data/output/freq_dist.txt",
    "--stopwords", "/Users/sujit/Downloads/apache-solr-4.0.0/example/solr/collection1/conf/stopwords.txt")))).
    run

  //////// mail rank ////////
  
  (new MongoEmailPairs("localhost", 27017, "solr4secure", 
    "/Users/sujit/Downloads/apache-solr-4.0.0/example/solr/collection1/data/index")).
    generate("data/input/email_refs.txt",
    "data/input/email_pairs.txt")
  
  (new MailRank(Args(List(
    "--local", "",
    "--input", "data/input/email_pairs.txt",
    "--output", "data/output/mailrank.txt",
    "--iterations", "10")))).
    run

  (new MailRankPostProcessor(Args(List(
    "--local", "",
    "--input", "data/output/mailrank.txt",
    "--reference", "data/input/email_refs.txt",
    "--output", "data/output/mailrank_final.txt")))).run

  ////////////// clustering terms ///////////////
    
  (new DocVector(Args(List(
    "--local", "",
    "--input", "data/input/corpus_freqs.txt",
    "--termcounts", "data/input/freq_words.txt",
    "--vocabsize", "1326", // cat freq_words | cut -f1 | sort | uniq | wc
    "--dictionary", "data/output/dictionary.txt",
    "--docvector", "data/output/docvector.txt")))).
    run

  (new DocVectorToArff()).generate(
    "/Users/sujit/Projects/solr4-extras/data/output/docvector.txt", 
    "data/output/docvector.arff", 1326)

  (new WekaClusterDumper()).dump(
    "/Users/sujit/Projects/solr4-extras/data/output/weka_cluster_output.txt",
    "/Users/sujit/Projects/solr4-extras/data/output/dictionary.txt",
    "data/output/cluster.dump",10)
    
}
