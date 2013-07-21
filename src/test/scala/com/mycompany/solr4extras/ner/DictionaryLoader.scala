package com.mycompany.solr4extras.ner

import java.sql.DriverManager
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer

class DictionaryLoader {

  val solrUrl = "http://localhost:8983/solr/"
  val mysqlUrl = "jdbc:mysql://localhost:3306/aetnadsdb"
  val mysqlUser = "root"
  val mysqlPass = "orange"
    
  Class.forName("com.mysql.jdbc.Driver").newInstance
  val mysql = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass)
  val solr = new ConcurrentUpdateSolrServer(solrUrl, 10, 1)
  
  // city: aetna_ds_address.city
  // state: list of states (long and short form)
  // street: aetna_ds_address
  // filter by: country = 'USA'
  // name: given+additonal+family from aetna_ds_individual_provider_name
  // hospital: aetna_ds_organization_provider
  
}