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
  int annotationDepth = 0; // 0 = before first annotation, 1 = inside annotation in markup open tag, 2 = inside annotation in markup open tag inside annotation in markup open tag, etc.
  Map<Integer,AtomicInteger> openRangeInAnnotationTextCount = new HashMap<>();
}

// In the default mode we are outside a Markup Range

COMMENT
  : '[! ' .*? ' !]' //-> skip //channel(HIDDEN)
  ;

NAMESPACE
  : '[!ns ' NamespaceIdentifier WS NamespaceURI ']'
  ;

BEGIN_OPEN_MARKUP // [ moves into markup tag
  : TagOpenStartChar  -> pushMode(INSIDE_MARKUP_OPENER)
  ;

BEGIN_TEXT_VARIATION
  : TextVariationStartTag  -> pushMode(INSIDE_TEXT_VARIATION)
  ;

BEGIN_CLOSE_MARKUP
  : TagCloseStartChar  -> pushMode(INSIDE_MARKUP_CLOSER)
  ;

TEXT  // match any 16 bit char other than { (start close tag) and [ (start open tag)
  : ~[<\\[|]+
  ;

Nameinit
  : [a-zA-Z_]
  ;

Namechar
  : Nameinit
  | [0-9]
  ;

//NL
//  : [\r\n]+ -> skip
//  ;

// ----------------- Everything INSIDE of a MARKUP OPENER ---------------------
mode INSIDE_MARKUP_OPENER;

COMMENT_IN_MARKUP_OPENER
  : '[!' .*? '!]' -> skip //channel(HIDDEN)
  ;

//END_ANONYMOUS_MARKUP
//  : ']'  {
//    openRangeInAnnotationTextCount.computeIfAbsent(annotationDepth, k -> new AtomicInteger(0));
//    openRangeInAnnotationTextCount.get(annotationDepth).decrementAndGet();
//    popMode();
//  }
//  ;

NameOpenMarkup
  :   ( Optional | Resume )? NameStartChar NameChar* SUFFIX?
  ;

SUFFIX
  : '~' NameChar*
  ;

MARKUP_S
  :   WS  -> skip
  ;


Annotation
  : AnnotationIdentifier '=' AnnotationValue
  ;

AnnotationIdentifier
  : NameStartChar NameChar*
  ;

AnnotationValue
  : StringValue
  | BooleanValue
  | NumberValue
  | MixedContentValue
  | ListValue
  | ObjectValue
  ;

StringValue
  : '"' ~["]+ '"'
  | '\'' ~[']+ '\''
  ;

BooleanValue
  : 'true'
  | 'false'
  ;

NumberValue
  : DIGIT+
  ;

MixedContentValue
  : '|' ~[|] '|'
  ;

ListValue
  : '[' AnnotationValue (',' AnnotationValue)+ ']'
  ;

ObjectValue
  : '{' Annotation (' ' Annotation)+ '}'
  ;

END_OPEN_MARKUP
  :  TagOpenEndChar  -> popMode
  ;

// ----------------- Everything INSIDE of a MARKUP CLOSER -------------
mode INSIDE_MARKUP_CLOSER;

END_CLOSE_MARKUP
  :   TagCloseEndChar  -> popMode
  ;

NameCloseMarkup
  :   ( Optional | Resume )? NameStartChar NameChar* SUFFIX?
  ;

MARKUP_S2
  :   WS  -> skip
  ;

// ----------------- Everything INSIDE of a MARKUP CLOSER -------------
mode INSIDE_TEXT_VARIATION;

TEXT_VARIATION
  : ~[|<]+
  ;

END_TEXT_VARIATION
  : TextVariationEndTag -> popMode
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

TextVariationSeparator
  : '|'
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
  : ('http://' | 'https://') ( NameChar | '/' )+
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
  : [ \t\r\n]+
  ;

//UNEXPECTED_CHAR // Throw unexpected token exception
//  :  .
//  ;
