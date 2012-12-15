solr4-extras
============

Random solr4 customizations

Secure - search "encrypted" data collections
--------------------------------------------

A custom Solr search component that allows searching against a collection that is split between MongoDB and Solr. Solr contains the searchable but unstored version of the documents, and MongoDB contains the encrypted version of the document (along with the user keys in another table). At search time, data is searched against Solr, then retrieved and decrypted from MongoDB and presented to the client.

More info on my [Blog Post](http://sujitpal.blogspot.com/2012/12/searching-encrypted-document-collection.html)

### Setup Instructions ###

1. Download and install MongoDB and Solr4.
2. Copy the contents of conf/secure to the Solr example/solr/collections1/conf directory.
3. git clone this project, then run "sbt" from the command line. This will populate your .ivy2 cache with the necessary JAR files for this project.
4. Make a directory example/solr/collections1/lib and copy the following JAR files from your .ivy cache to it. :
    casbah-commons_2.9.2-2.3.0.jar  mongo-java-driver-2.8.0.jar
    casbah-core_2.9.2-2.3.0.jar     scala-library.jar
    casbah-query_2.9.2-2.3.0.jar    scalaj-collection_2.9.1-1.2.jar
    casbah-util_2.9.2-2.3.0.jar
5. Run "sbt package" on the command line. This will create the solr4-extras JAR file. Copy this file to the lib directory above.
6. Start mongod.
7. Start Solr4 (java -jar start.jar)
8. Download and expand the Enron dataset and update conf/secure/secure.properties to point to it.
9. Run "sbt run" to run the indexer and populate the index and MongoDB tables.
10. You should now see results from queries to the custom /secure_select service. Example URL:
    http://localhost:8983/solr/collection1/secure_select\
        ?q=body:%22hedge%20fund%22\
        &fq=from:kaye.ellis@enron.com\
        &email=kaye.ellis@enron.com

