package com.mycompany.solr4extras.cpos

import java.io.StringReader

import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, KeywordAttribute, OffsetAttribute, PositionIncrementAttribute}
import org.junit.{Assert, Test}

class ConceptPositionTokenFilterTest {

  val TestStrings = Array[String](
    "5047841$radic0cystectomi with without 8095296$urethrectomi",
    "use mr 5047865$angiographi angiographi 9724059$ct0angiographi 8094588$stereotact0radiosurgeri intracranial avm",
    "factor influenc time 8239660$sentinel0node visual 2790981$breast0cancer patient 8100872$intradermal0injection radiotrac",
    "8129320$pretreat 9323323$vascular0endothelial0growth0factor 9204769$9323323$vegf 9160599$matrix0_metalloproteinase_9_ 9160599$mmp09 serum level patient with metastatic 8092190$non0small0cell0lung0canc nsclc"
  )
  
  @Test def testTokenFilter(): Unit = {
    val analyzer = new ConceptPositionAnalyzer()
    val expected = Map(
      ("breast0cancer" -> Token("breast0cancer", 172, 186, 1, false)),
      ("2790981" -> Token("2790981", 172, 186, 0, true)))
    TestStrings.foreach(testString => {
      Console.println("=== %s ===".format(testString))
      val tokenStream = analyzer.tokenStream("f", 
        new StringReader(testString))
      tokenStream.reset()
      while (tokenStream.incrementToken()) {
        val termAttr = tokenStream.getAttribute(classOf[CharTermAttribute])
        val offsetAttr = tokenStream.getAttribute(classOf[OffsetAttribute])
        val posAttr = tokenStream.getAttribute(classOf[PositionIncrementAttribute])
        val keyAttr = tokenStream.getAttribute(classOf[KeywordAttribute])
        val term = new String(termAttr.buffer(), 0, termAttr.length())
        if (expected.contains(term)) {
          val expectedToken = expected(term)
          Assert.assertEquals(expectedToken.strval, term)
          Assert.assertEquals(expectedToken.start, offsetAttr.startOffset())
          Assert.assertEquals(expectedToken.end, offsetAttr.endOffset())
          Assert.assertEquals(expectedToken.inc, posAttr.getPositionIncrement())
          Assert.assertEquals(expectedToken.isKeyword, keyAttr.isKeyword())
        }
        Console.println("  %s (@ %d, %d, %d) [keyword? %s]".format(
          term, offsetAttr.startOffset(), offsetAttr.endOffset(),
          posAttr.getPositionIncrement(), 
          if (keyAttr.isKeyword()) "TRUE" else "FALSE"))
      }
      tokenStream.end()
      tokenStream.close()
    })
  }

  case class Token(strval: String, start: Int, end: Int, 
    inc: Int, isKeyword: Boolean)

}