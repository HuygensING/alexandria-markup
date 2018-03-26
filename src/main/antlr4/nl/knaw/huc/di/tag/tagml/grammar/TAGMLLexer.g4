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
  : ']'  {
    openRangeInAnnotationTextCount.computeIfAbsent(annotationDepth, k -> new AtomicInteger(0));
    openRangeInAnnotationTextCount.get(annotationDepth).decrementAndGet();
    popMode();
  }
  ;

END_OPEN_MARKUP
  :   TagOpenEndChar  -> popMode
  ;


Name_Open_Range
  :   NameStartChar NameChar* ('=' NameChar+)?
  ;

MARKUP_S
  :   WS  -> skip
  ;

// ----------------- Everything INSIDE of a MARKUP CLOSER -------------
mode INSIDE_MARKUP_CLOSER;

END_CLOSE_MARKUP
  :   TagCloseEndChar  -> popMode
  ;

Name_Close_Range
  :   NameStartChar NameChar* ('=' NameChar+)?
  ;

MARKUP_S2
  :   WS  -> skip
  ;

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
