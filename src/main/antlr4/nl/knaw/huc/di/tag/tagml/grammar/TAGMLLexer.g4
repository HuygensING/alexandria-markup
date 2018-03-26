/*
 * Grammar for the TAGML overlapping markup language format
 * @author: Ronald Haentjens Dekker
 * @author: Bram Buitendijk
 *
 */

lexer grammar TAGMLLexer;

@lexer::header{
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
}

@lexer::members{
  int annotationDepth = 0; // 0 = before first annotation, 1 = inside annotation in rangeopener/closer, 2 = inside annotation in rangeopener/closer inside annotation in rangeopener/closer, etc.
  Map<Integer,AtomicInteger> openRangeInAnnotationTextCount = new HashMap<>();
}

// In the default mode we are outside a Range

NAMESPACE
  : '[!ns ' NamespaceIdentifier WS NamespaceURI ']'
  ;

COMMENT
  : '[!' .*? '!]' -> skip //channel(HIDDEN)
  ;

BEGIN_OPEN_MARKUP // [ moves into markup tag
  : TagOpenStartChar  -> pushMode(INSIDE_MARKUP_OPENER)
  ;

BEGIN_CLOSE_MARKUP
  : TagCloseStartChar  -> pushMode(INSIDE_MARKUP_CLOSER)
  ;

TEXT  // match any 16 bit char other than { (start close tag) and [ (start open tag)
  : ~[<\\[]+
  ;

//NL
//  : [\r\n]+ -> skip
//  ;

// ----------------- Everything INSIDE of a MARKUP OPENER ---------------------
mode INSIDE_MARKUP_OPENER;

COMMENT_IN_MARKUP_OPENER
  : '[!--' .*? '--]' -> skip // channel(HIDDEN)
  ;

END_ANONYMOUS_MARKUP
  :   ']'  {
    openRangeInAnnotationTextCount.computeIfAbsent(annotationDepth, k -> new AtomicInteger(0));
    openRangeInAnnotationTextCount.get(annotationDepth).decrementAndGet();
    popMode();
  }
  ;

END_OPEN_MARKUP
  :   '}'  -> popMode
  ;

BEGIN_OPEN_ANNO
  :   '['  -> pushMode(INSIDE_ANNOTATION_OPENER)
  ;

Name_Open_Range
  :   NameStartChar NameChar* ('=' NameChar+)?
  ;

MARKUP_S
  :   WS  -> skip
  ;

// ----------------- Everything INSIDE of a MARKUP CLOSER -------------
mode INSIDE_MARKUP_CLOSER;

COMMENT_IN_MARKUP_CLOSER
  : '[!--' .*? '--]' -> skip //channel(HIDDEN)
  ;

END_CLOSE_MARKUP
  :   TagCloseEndChar  -> popMode
  ;

BEGIN_OPEN_ANNO_IN_MARKUP_CLOSER
  :   '['  -> pushMode(INSIDE_ANNOTATION_OPENER)
  ;

Name_Close_Range
  :   NameStartChar NameChar* ('=' NameChar+)?
  ;

MARKUP_S2
  :   WS  -> skip
  ;

// ------------------ Everything INSIDE of a ANNOTATION OPENER -----------
// NOTE: Annotation openers are close to range openers, but not the same!
// NOTE: We can have anonymous annotations!
mode INSIDE_ANNOTATION_OPENER;

END_EMPTY_ANNO
  :   ']'  -> popMode
  ;

END_OPEN_ANNO
  :   '}'  { annotationDepth++; popMode(); pushMode(INSIDE_ANNOTATION_TEXT); }
  ;

OPEN_ANNO_IN_ANNO_OPENER
  :   '['  -> pushMode(INSIDE_ANNOTATION_OPENER)
  ;

Name_Open_Annotation
  :   NameStartChar NameChar*
  ;

ANNO_OPENER_WS
  :   WS  -> skip
  ;

// ----------------- Everything INSIDE of a ANNOTATION CLOSER -------------
// NOTE: Annotation closers are exact copy of range closers
mode INSIDE_ANNOTATION_CLOSER;

Name_Close_Annotation
  :   NameStartChar NameChar*
  ;

OPEN_ANNO_IN_ANNO_CLOSER
  :   '['  -> pushMode(INSIDE_ANNOTATION_OPENER)
  ;

END_CLOSE_ANNO
  :   ']'  -> popMode
  ;

ANNO_CLOSER_WS
  :   WS  -> skip
  ;

// ------------------ Inside ANNOTATION TEXT --------------------------------------
// NOTE:c Annotation text is simi
mode INSIDE_ANNOTATION_TEXT;

ATOM_IN_ANNOTATION_TEXT
  : '{{' .*? '}}' -> skip
  ;

BEGIN_ANNO_OPEN_MARKUP
  : '[' {
    openRangeInAnnotationTextCount.computeIfAbsent(annotationDepth, k -> new AtomicInteger(0));
    openRangeInAnnotationTextCount.get(annotationDepth).incrementAndGet();
    pushMode(INSIDE_MARKUP_OPENER);
  }
  ;

BEGIN_ANNO_CLOSE_MARKUP
  : '{'  {
    openRangeInAnnotationTextCount.computeIfAbsent(annotationDepth, k -> new AtomicInteger(0));
    if (openRangeInAnnotationTextCount.get(annotationDepth).get() == 0) {
      setType(BEGIN_CLOSE_ANNO);
      popMode();
      annotationDepth--;
      pushMode(INSIDE_ANNOTATION_CLOSER);
    } else {
      openRangeInAnnotationTextCount.get(annotationDepth).decrementAndGet();
      pushMode(INSIDE_MARKUP_CLOSER);
    }
  }
  ;

BEGIN_CLOSE_ANNO
  : '{'  //-> popMode, pushMode(INSIDE_ANNOTATION_CLOSER) // never actually reached, just for defining BEGIN_CLOSE_ANNO (?)
  ;

ANNO_TEXT  // match any 16 bit char other than { (start close tag) and [ (start open tag)
  : ~[{\\[]+ ;

// ----------------- lots of repeated stuff --------------------------

TagOpenStartChar
  : '['
  ;

TagOpenEndChar
  : '>'
  ;

TagCloseStartChar
  : '<'
  ;

TagCloseEndChar
  : ']'
  ;

TextVariationStartTag
  : '|>'
  ;

TextVariationEndTag
  : '<|'
  ;

Optional
  : '?'
  ;

Resume
  : '+'
  ;

Suspend
  : '-'
  ;

NamespaceIdentifier
  : NameChar+
  ;

NamespaceURI // TODO!
  : NameChar+
  ;

fragment
NameChar
  : NameStartChar
  | '-' | '_' | '.' | DIGIT
  | '\u00B7'
  | '\u0300'..'\u036F'
  | '\u203F'..'\u2040'
  ;

fragment
NameStartChar
  : [:a-zA-Z]
  | '\u2070'..'\u218F'
  | '\u2C00'..'\u2FEF'
  | '\u3001'..'\uD7FF'
  | '\uF900'..'\uFDCF'
  | '\uFDF0'..'\uFFFD'
  ;

fragment
DIGIT
  : [0-9]
  ;

fragment
WS
  : [ \t\r\n]
  ;

//UNEXPECTED_CHAR // Throw unexpected token exception
//  :  .
//  ;
