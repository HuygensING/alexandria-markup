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
  : LEFT_SQUARE_BRACKET  -> pushMode(INSIDE_MARKUP_OPENER)
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

//Nameinit
//  : [a-zA-Z_]
//  ;
//
//Namechar
//  : Nameinit
//  | [0-9]
//  ;

NAME
  : NameStartChar NameChar*
  ;
//NL
//  : [\r\n]+ -> skip
//  ;

// ----------------- Everything INSIDE of a MARKUP OPENER ---------------------
mode INSIDE_MARKUP_OPENER;

COMMENT_IN_MARKUP_OPENER
  : '[!' .*? '!]' -> skip //channel(HIDDEN)
  ;

END_ANONYMOUS_MARKUP
  : RIGHT_SQUARE_BRACKET  {
    openRangeInAnnotationTextCount.computeIfAbsent(annotationDepth, k -> new AtomicInteger(0));
    openRangeInAnnotationTextCount.get(annotationDepth).decrementAndGet();
    popMode();
  }
  ;

TRUE
  : T R U E
  ;

FALSE
  : F A L S E
  ;

//SINGLE_QUOTE
//  : '\''
//  ;
//
//DOUBLE_QUOTE
//  : '"'
//  ;

NameOpenMarkup
  :  ( Optional | Resume )? NAME SUFFIX? -> pushMode(ANNOTATIONS)
  ;

SUFFIX
  : TILDE NAME
  ;


// ----------------- Everything INSIDE of a MARKUP CLOSER -------------
mode ANNOTATIONS;

MARKUP_S
  :  WS  -> skip
  ;

ANNOTATION_NAME
  : NAME
  ;

EQ
  : '='
  ;

//Annotation
//  : AnnotationIdentifier '=' AnnotationValue
//  ;
//
//AnnotationIdentifier
//  : NAME
//  ;
//
//AnnotationValue
//  : StringValue
//  | BooleanValue
//  | NumberValue
//  | MixedContentValue
//  | ListValue
//  | ObjectValue
//  ;
//
StringValue
  : '"' ~["]+ '"'
  | '\'' ~[']+ '\''
  ;
//
//BooleanValue
//  : TRUE
//  | FALSE
//  ;
//
NumberValue
  : DIGIT+ ( '.' DIGIT+ )?
  ;
//
//MixedContentValue
//  : '|' ~[|] '|'
//  ;
//
//ListValue
//  : '[' AnnotationValue (',' AnnotationValue)+ ']'
//  ;
//
//ObjectValue
//  : '{' Annotation (' ' Annotation)+ '}'
//  ;

OPEN_MIXED_CONTENT
  : PIPE -> pushMode(INSIDE_MIXED_CONTENT)
  ;

OPEN_OBJECT
  : '{' -> pushMode(INSIDE_OBJECT)
  ;

OPEN_LIST
  : LEFT_SQUARE_BRACKET -> pushMode(INSIDE_LIST)
  ;

END_OPEN_MARKUP
  :  TagOpenEndChar  -> popMode, popMode
  ;

// ----------------- Everything INSIDE of a MARKUP CLOSER -------------
mode INSIDE_MARKUP_CLOSER;

END_CLOSE_MARKUP
  :   RIGHT_SQUARE_BRACKET  -> popMode
  ;

NameCloseMarkup
  :   ( Optional | Suspend )? NAME SUFFIX?
  ;

MARKUP_S2
  :   WS  -> skip
  ;

// ----------------- Everything INSIDE of a TEXT VARIATION -------------
mode INSIDE_TEXT_VARIATION;

TV_BEGIN_OPEN_MARKUP // [ moves into markup tag
  : LEFT_SQUARE_BRACKET  -> pushMode(INSIDE_MARKUP_OPENER)
  ;

TV_BEGIN_CLOSE_MARKUP
  : TagCloseStartChar  -> pushMode(INSIDE_MARKUP_CLOSER)
  ;

END_TEXT_VARIATION
  : TextVariationEndTag -> popMode
  ;

VARIANT_TEXT
  : ( ~[|<[] | '\\|' | '\\<' | '\\[' )+
  ;

// ----------------- Everything INSIDE of | | -------------
mode INSIDE_MIXED_CONTENT;

MC_END
  : PIPE -> popMode
  ;

// ----------------- Everything INSIDE of { } -------------
mode INSIDE_OBJECT;

OBJECT_END
  : '}' -> popMode
  ;

// ----------------- Everything INSIDE of [ ] -------------
mode INSIDE_LIST;

LIST_END
  : RIGHT_SQUARE_BRACKET -> popMode
  ;

// ----------------- lots of repeated stuff --------------------------



//TagOpenStartChar
//  : LEFT_SQUARE_BRACKET
//  ;

TagOpenEndChar
  : '>'
  ;

TagCloseStartChar
  : '<'
  ;

//TagCloseEndChar
//  : RIGHT_SQUARE_BRACKET
//  ;

TextVariationStartTag
  : '|>'
  ;

TextVariationEndTag
  : '<|'
  ;

TextVariationSeparator
  : PIPE
  ;

PIPE
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

TILDE
  : '~'
  ;

LIST_OPENER
  : LEFT_SQUARE_BRACKET
  ;

LIST_CLOSER
  : RIGHT_SQUARE_BRACKET
  ;

OBJECT_OPENER
  : '{'
  ;

OBJECT_CLOSER
  : '}'
  ;

NamespaceIdentifier
  : NameChar+
  ;

NamespaceURI
  : ('http://' | 'https://') ( NameChar | '/' )+
  ;

DOT
  : '.'
  ;

COMMA
  : ','
  ;

LEFT_SQUARE_BRACKET
  : '['
  ;

RIGHT_SQUARE_BRACKET
  : ']'
  ;


DIGIT
  : [0-9]
  ;


fragment A : [Aa];
fragment B : [Bb];
fragment C : [Cc];
fragment D : [Dd];
fragment E : [Ee];
fragment F : [Ff];
fragment G : [Gg];
fragment H : [Hh];
fragment I : [Ii];
fragment J : [Jj];
fragment K : [Kk];
fragment L : [Ll];
fragment M : [Mm];
fragment N : [Nn];
fragment O : [Oo];
fragment P : [Pp];
fragment Q : [Qq];
fragment R : [Rr];
fragment S : [Ss];
fragment T : [Tt];
fragment U : [Uu];
fragment V : [Vv];
fragment W : [Ww];
fragment X : [Xx];
fragment Y : [Yy];
fragment Z : [Zz];

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
WS
  : [ \t\r\n]+
  ;

//UNEXPECTED_CHAR // Throw unexpected token exception
//  :  .
//  ;
