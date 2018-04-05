/*
 * Grammar for the TAGML overlapping markup language format
 * @author: Ronald Haentjens Dekker
 * @author: Bram Buitendijk
 *
 */

lexer grammar TAGMLLexer;

//@lexer::header{
//import java.util.*;
//import java.util.concurrent.atomic.AtomicInteger;
//}
//
//@lexer::members{
//  int annotationDepth = 0; // 0 = before first annotation, 1 = inside annotation in markup open tag, 2 = inside annotation in markup open tag inside annotation in markup open tag, etc.
//  Map<Integer,AtomicInteger> openRangeInAnnotationTextCount = new HashMap<>();
//}

// In the default mode we are outside a Markup Range

COMMENT
  : '[! ' .*? ' !]' -> skip //channel(HIDDEN)
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

NAME
  : NameStartChar NameChar*
  ;

// ----------------- Everything INSIDE of a MARKUP OPENER ---------------------
mode INSIDE_MARKUP_OPENER;

COMMENT_IN_MARKUP_OPENER
  : '[!' .*? '!]' -> skip //channel(HIDDEN)
  ;

PREFIX
  : Optional
  | Resume
  ;

SUFFIX
  : TILDE ( NAME | DIGIT+ )
  ;

NameOpenMarkup
  : NAME
  ;

END_ANONYMOUS_MARKUP
  : RIGHT_SQUARE_BRACKET -> popMode
  ;

MO_WS
  : WS -> skip, pushMode(ANNOTATIONS)
  ;

END_OPEN_MARKUP
  :  TagOpenEndChar  -> popMode
  ;

// ----------------- Everything after the markup name -------------
mode ANNOTATIONS;

ANNOTATION_NAME
  : NAME
  ;

MARKUP_S
  :  WS  -> skip
  ;

EQ
  : '=' -> pushMode(ANNOTATION_VALUE)
  ;

A_END_OPEN_MARKUP
  :  TagOpenEndChar  -> popMode, popMode
  ;

A_END_ANONYMOUS_MARKUP
  : RIGHT_SQUARE_BRACKET -> popMode, popMode
  ;

// ----------------- Everything after the = in an annotation -------------
mode ANNOTATION_VALUE;

StringValue
  : ( '"' ~["]+ '"' | '\'' ~[']+ '\'' ) -> popMode
  ;

NumberValue
  : DIGIT+ ( '.' DIGIT+ )? -> popMode
  ;

TRUE
  : T R U E -> popMode
  ;

FALSE
  : F A L S E -> popMode
  ;

OPEN_MIXED_CONTENT
  : PIPE -> pushMode(INSIDE_MIXED_CONTENT)
  ;

OBJECT_OPENER
  : '{' -> pushMode(INSIDE_OBJECT)
  ;

OPEN_LIST
  : LEFT_SQUARE_BRACKET -> pushMode(INSIDE_LIST)
  ;

// ----------------- Everything INSIDE of | | -------------
mode INSIDE_MIXED_CONTENT;

MC_END
  : PIPE -> popMode, popMode, popMode // back to INSIDE_MARKUP_OPENER
  ;

// ----------------- Everything INSIDE of { } -------------
mode INSIDE_OBJECT;

O_ANNOTATION_NAME
  : NAME
  ;

O_MARKUP_S
  :  WS  -> skip
  ;

O_EQ
  : '=' -> pushMode(ANNOTATION_VALUE)
  ;

OBJECT_CLOSER
  : '}' -> popMode, popMode // back to INSIDE_MARKUP_OPENER
  ;

// ----------------- Everything INSIDE of [ ] -------------
mode INSIDE_LIST;

LIST_END
  : RIGHT_SQUARE_BRACKET -> popMode, popMode, popMode // back to INSIDE_MARKUP_OPENER
  ;

// ----------------- Everything INSIDE of a MARKUP CLOSER -------------
mode INSIDE_MARKUP_CLOSER;

CM_PREFIX
  : Optional
  | Suspend
  ;

NameCloseMarkup
  : NAME
  ;

CM_SUFFIX
  : TILDE ( NAME | DIGIT+ )
  ;

MARKUP_S2
  :   WS  -> skip
  ;

END_CLOSE_MARKUP
  :   RIGHT_SQUARE_BRACKET -> popMode // back to DEFAULT
  ;

// ----------------- Everything INSIDE of a TEXT VARIATION -------------
mode INSIDE_TEXT_VARIATION;

VARIANT_TEXT
  : ( ~[|<[] | '\\|' | '\\<' | '\\[' )+
  ;

TV_BEGIN_OPEN_MARKUP // [ moves into markup tag
  : LEFT_SQUARE_BRACKET  -> pushMode(INSIDE_MARKUP_OPENER)
  ;

TV_BEGIN_TEXT_VARIATION
  : TextVariationStartTag  -> pushMode(INSIDE_TEXT_VARIATION)
  ;

TV_BEGIN_CLOSE_MARKUP
  : TagCloseStartChar  -> pushMode(INSIDE_MARKUP_CLOSER)
  ;

END_TEXT_VARIATION
  : TextVariationEndTag -> popMode
  ;

// ----------------- lots of repeated stuff --------------------------

TagOpenEndChar
  : '>'
  ;

TagCloseStartChar
  : '<'
  ;

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

UNEXPECTED_CHAR // Throw unexpected token exception
  :  .
  ;
