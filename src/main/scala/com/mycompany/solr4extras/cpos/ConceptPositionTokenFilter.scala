package com.mycompany.solr4extras.cpos

import java.io.Reader
import java.util.Stack
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

import org.apache.lucene.analysis.{TokenFilter, TokenStream}
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, KeywordAttribute, OffsetAttribute, PositionIncrementAttribute}
import org.apache.lucene.util.Version

class ConceptPositionTokenFilter(input: TokenStream) 
    extends TokenFilter(input) {

  val TokenPattern = Pattern.compile("(\\d+\\$)+([\\S]+)");
  val AnnotationSeparator = '$';

  val termAttr = addAttribute(classOf[CharTermAttribute])
  val keyAttr = addAttribute(classOf[KeywordAttribute])
  val posAttr = addAttribute(classOf[PositionIncrementAttribute])
  val offsetAttr = addAttribute(classOf[OffsetAttribute])
  val annotations = new Stack[(String,Int,Int)]()
  val offset = new AtomicInteger()

  override def incrementToken(): Boolean = {
    if (annotations.isEmpty) {
      if (input.incrementToken()) {
        val term = new String(termAttr.buffer(), 0, termAttr.length())
        val matcher = TokenPattern.matcher(term)
        if (matcher.matches()) {
          val subtokens = term.split(AnnotationSeparator)
          val str = subtokens(subtokens.size - 1)
          clearAttributes()
          termAttr.copyBuffer(str.toCharArray(), 0, str.length())
          val startOffset = offset.get()
          val endOffset = offset.addAndGet(str.length() + 1)
          offsetAttr.setOffset(startOffset, endOffset)
          val range = 0 until subtokens.length - 1
          range.foreach(i => {
            annotations.push((subtokens(i), startOffset, endOffset))
          })
        } else {
          clearAttributes()
          termAttr.copyBuffer(term.toCharArray(), 0, term.length())
          val startOffset = offset.get()
          val endOffset = offset.addAndGet(term.length() + 1)
          offsetAttr.setOffset(startOffset, endOffset)
        }
        true
      } else 
        false
    } else {
      val (conceptId, startOffset, endOffset) = annotations.pop()
      clearAttributes()
      termAttr.copyBuffer(conceptId.toCharArray(), 0, conceptId.length())
      posAttr.setPositionIncrement(0)
      offsetAttr.setOffset(startOffset, endOffset)
      keyAttr.setKeyword(true)
      true
    }
  }
}

class ConceptPositionAnalyzer extends Analyzer {

  override def createComponents(fieldname: String, reader: Reader): 
      TokenStreamComponents = {
    val source = new WhitespaceTokenizer(Version.LUCENE_43, reader)
    val filter = new ConceptPositionTokenFilter(source)
    new TokenStreamComponents(source, filter)
  }
}