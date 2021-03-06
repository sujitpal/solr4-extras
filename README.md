solr4-extras
============

Random solr4 customizations (in Scala).

Secure - search "encrypted" data collections
--------------------------------------------

A custom Solr search component that allows searching against a collection that is split between MongoDB and Solr. Solr contains the searchable but unstored version of the documents, and MongoDB contains the encrypted version of the document (along with the user keys in another table). At search time, data is searched against Solr, then retrieved and decrypted from MongoDB and presented to the client.

More info on my [Blog Post](http://sujitpal.blogspot.com/2012/12/searching-encrypted-document-collection.html)

### Setup Instructions ###

1. Download and install MongoDB and Solr4.
2. Copy the contents of conf/secure to the Solr example/solr/collections1/conf directory.
3. git clone this project, then run "sbt" from the command line. This will populate your .ivy2 cache with the necessary JAR files for this project.
4. Make a directory example/solr/collections1/lib and copy the following JAR files from your .ivy cache to it. Here is the list I built by trial and error: (casbah-commons_2.9.2-2.3.0.jar,  mongo-java-driver-2.8.0.jar, casbah-core_2.9.2-2.3.0.jar, scala-library.jar, casbah-query_2.9.2-2.3.0.jar, scalaj-collection_2.9.1-1.2.jar, casbah-util_2.9.2-2.3.0.jar).
5. Run "sbt package" on the command line. This will create the solr4-extras JAR file. Copy this file to the lib directory above.
6. Start mongod.
7. Start Solr4 (java -jar start.jar)
8. Download and expand the Enron dataset and update conf/secure/secure.properties to point to it.
9. Run "sbt run" to run the indexer and populate the index and MongoDB tables.
10. Create index on the email collection:
    db.emails.ensureIndex({"message_id": 1})
11. You should now see results from queries to the custom /secure_select service. Example URL: http://localhost:8983/solr/collection1/secure_select?q=body:%22hedge%20fund%22&fq=from:kaye.ellis@enron.com&email=kaye.ellis@enron.com

FuncQuery - function queries to influence ranking using demographics
--------------------------------------------------------------------

SolrJ code to write random score values and a title to a Solr instance so these can be used in function queries. No front end code (although I guess I could have written a JUnit test to demonstrate the function queries in action), and no configuration changes. More info on my [Blog Post](http://sujitpal.blogspot.com/2013/03/solr-custom-ranking-with-function.html).

Payloads - a Solr4 port for concept maps as payloads
----------------------------------------------------

Payload implementation for modeling concepts and their scores as payload fields, with Similarity, QParser for Payloads. Needs following configuration:

### Setup Instructions ###

1. Build JAR using sbt package.
2. Copy JAR into lib, along with scala-compiler.jar and scala-library.jar.
3. Make following modifications to conf/schema.xml and conf/solrconfig.xml.

schema.xml:
    
	<field name="cscores" type="payloads" indexed="true" stored="true"/>
	<similarity
	  class="com.mycompany.solr4extras.payloads.MyCompanySimilarityWrapper"/>

solrconfig.xml:

	<queryParser name="payloadQueryParser"
	  class="com.mycompany.solr4extras.payloads.PayloadQParserPlugin"/>
	<requestHandler name="/cselect" class="solr.SearchHandler">
	  <lst name="defaults">
	    <str name="defType">payloadQueryParser</str>
	  </lst>
	</requestHandler>

More info on my [Blog Post](http://sujitpal.blogspot.com/2013/07/porting-payloads-to-solr4.html).

NER - Named Entity Extraction with LingPipe
--------------------------------------------

Using LingPipe to construct regex based and dictionary based Named Entity Extractors backed by Solr, used for preprocessing query.

The following fields need to be defined in schema.xml for the SolrMapDictionary object:

	<field name="nercat" type="string" indexed="true" stored="true" multiValued="true"/>
	<field name="nerval" type="text_general" indexed="true" stored="true"/>

More info on my [Blog Post](http://sujitpal.blogspot.com/2013/07/dictionary-backed-named-entity.html).

Concept Embedding - Mixed Concept + Text queries
------------------------------------------------

Code written against Solr4 to embed concept IDs like synonyms within text. Custom TokenFilter and Analyzer to support this work, plus configuration and JUnit tests. Configuration consists of the following field definitions and the following fieldType definition:

    <!-- for concept position -->
    <field name="itemtitle" type="text_en" indexed="true" stored="true"/>
    <field name="itemtitle_cp" type="text_cp" indexed="true" stored="true"/>

    <!-- text_cp field type definition -->
    <fieldType name="text_cp" class="solr.TextField">
      <analyzer type="index"
        class="com.mycompany.solr4extras.cpos.ConceptPositionAnalyzer"/>>
      <analyzer type="query">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.StopFilterFactory"
                ignoreCase="true"
                words="lang/stopwords_en.txt"
                enablePositionIncrements="true"/>
        <filter class="solr.LowerCaseFilterFactory"/>
        <filter class="solr.EnglishPossessiveFilterFactory"/>
        <filter class="solr.KeywordMarkerFilterFactory" 
                protected="protwords.txt"/>
        <filter class="solr.PorterStemFilterFactory"/>
      </analyzer>
    </fieldType>

More info on my [Blog Post](http://sujitpal.blogspot.com/2013/08/embedding-concepts-in-text-for-smarter.html).

Near-Duplicate Detection
------------------------

Uses Shingles and MinHashing to implement near-duplicate detection on the [Restaurant Dataset](http://www.cs.utexas.edu/users/ml/riddle/data.html). No customization of Solr required, everything is done in client. Following new fields need to be declared to use this application:

	<field name="content" type="text_general" indexed="false" stored="true" 
	  multiValued="false"/>
	<field name="md5_hash" type="string" indexed="true" stored="true"/>
	<field name="num_words" type="int" indexed="true" stored="true" />
	<field name="first_word" type="string" indexed="true" stored="true"/>
	<field name="content_ng" type="string" indexed="true" stored="true" 
	  multiValued="true"/>
	<field name="content_sg" type="string" indexed="true" stored="true" 
	  multiValued="true"/>

More info on my [Blog Post](http://sujitpal.blogspot.com/2014/11/near-duplicate-detection-using.html).
