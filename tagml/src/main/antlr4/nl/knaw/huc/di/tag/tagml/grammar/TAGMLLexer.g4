/*
 * Grammar for the TAGML overlapping markup language format
 * @author: Ronald Haentjens Dekker
 * @author: Bram Buitendijk
 *
 */

lexer grammar TAGMLLexer;

// default mode

DEFAULT_NamespaceOpener
  : '[!ns ' -> pushMode(INSIDE_NAMESPACE)
  ;

DEFAULT_Comment
  : Comment -> skip
  ;

DEFAULT_BeginOpenMarkup // [ moves into markup tag
  : LEFT_SQUARE_BRACKET  -> pushMode(INSIDE_MARKUP_OPENER)
  ;

DEFAULT_BeginTextVariation
  : TextVariationStartTag  -> pushMode(INSIDE_TEXT_VARIATION)
  ;

DEFAULT_BeginCloseMarkup
  : TagCloseStartChar  -> pushMode(INSIDE_MARKUP_CLOSER)
  ;

DEFAULT_Text  // match any 16 bit char other than { (start close tag) and [ (start open tag)
  : ( ~[[<\\] | REGULAR_TEXT_ESCAPE_CHARACTER )+
  ;

NAME
  : NameStartChar NameChar*
  ;

// ----------------- Everything INSIDE of a NAMESPACE ---------------------
mode INSIDE_NAMESPACE;

IN_NamespaceIdentifier
  : NameChar+
  ;

IN_WS
  : WS -> skip
  ;

IN_NamespaceURI
  : ('http://' | 'https://') ( NameChar | '/' | '.' )+
  ;

IN_NamespaceCloser
  : ']' -> popMode
  ;

// ----------------- Everything INSIDE of a MARKUP OPENER ---------------------
mode INSIDE_MARKUP_OPENER;

IMO_Comment
  : Comment -> skip
  ;

IMO_Prefix
  : Optional
  | Resume
  ;

IMO_Comma
  : COMMA
  ;

IMO_Divider
  : PIPE
  ;

IMO_Name
  : NAME
  ;

IMO_Suffix
  : TILDE ( NAME | DIGIT+ )
  ;

IMO_WS
  : WS -> skip, pushMode(ANNOTATIONS)
  ;

IMO_EndMilestoneMarkup
  : RIGHT_SQUARE_BRACKET -> popMode
  ;

IMO_EndOpenMarkup
  :  TagOpenEndChar  -> popMode
  ;

// ----------------- Everything after the markup name -------------
mode ANNOTATIONS;

A_Ref
  : '->' -> pushMode(INSIDE_REF_VALUE)
  ;

A_IdAnnotation
  : ':id'
  ;

A_AnnotationName
  : NAME
  ;

A_WS
  :  WS  -> skip
  ;

A_EQ
  : '=' -> pushMode(ANNOTATION_VALUE)
  ;

A_EndOpenMarkup
  :  TagOpenEndChar -> popMode, popMode
  ;

A_EndMilestoneMarkup
  : RIGHT_SQUARE_BRACKET -> popMode, popMode
  ;

// ----------------- Everything after the = in an annotation -------------
mode ANNOTATION_VALUE;

AV_WS
  :  WS  -> skip
  ;

AV_StringValue
  : ( '"' ( ~["] | SINGLE_QUOTED_TEXT_ESCAPE_CHARACTER )+ '"' | '\'' ( ~['] | DOUBLE_QUOTED_TEXT_ESCAPE_CHARACTER )+ '\'' ) -> popMode
  ;

AV_NumberValue
  : DIGIT+ ( '.' DIGIT+ )? -> popMode
  ;

AV_TRUE
  : T R U E -> popMode
  ;

AV_FALSE
  : F A L S E -> popMode
  ;

AV_IdValue
  : NAME -> popMode
  ;

AV_MixedContentOpener
  : '[>' -> pushMode(INSIDE_MIXED_CONTENT)
  ;

AV_ObjectOpener
  : '{' -> pushMode(INSIDE_OBJECT)
  ;

AV_ListOpener
  : LEFT_SQUARE_BRACKET -> pushMode(INSIDE_LIST), pushMode(ANNOTATION_VALUE)
  ;

// ----------------- Everything after the -> in an annotation -------------
mode INSIDE_REF_VALUE;

RV_RefValue
  : NAME -> popMode
  ;

// ----------------- Everything INSIDE of [><] -------------
mode INSIDE_MIXED_CONTENT;

IMX_Comment
  : Comment -> skip
  ;

IMX_BeginOpenMarkup // [ moves into markup tag
  : LEFT_SQUARE_BRACKET  -> pushMode(INSIDE_MARKUP_OPENER)
  ;

IMX_BeginTextVariation
  : TextVariationStartTag  -> pushMode(INSIDE_TEXT_VARIATION)
  ;

IMX_BeginCloseMarkup
  : TagCloseStartChar  -> pushMode(INSIDE_MARKUP_CLOSER)
  ;

IMX_Text
  : ( ~[[<\\] | REGULAR_TEXT_ESCAPE_CHARACTER )+
  ;

IMX_MixedContentCloser
  : '<]' -> popMode, popMode // back to INSIDE_MARKUP_OPENER
  ;

// ----------------- Everything INSIDE of { } -------------
mode INSIDE_OBJECT;

IO_WS
  :  WS  -> skip
  ;

IO_IdAnnotation
  : ':id'
  ;

IO_AnnotationName
  : NAME
  ;

IO_EQ
  : '=' -> pushMode(ANNOTATION_VALUE)
  ;

IO_ObjectCloser
  : '}' -> popMode, popMode // back to INSIDE_MARKUP_OPENER
  ;

// ----------------- Everything INSIDE of [ ] -------------
mode INSIDE_LIST;

IL_WS
  :  WS  -> skip
  ;

IL_COMMA
  : COMMA -> pushMode(ANNOTATION_VALUE)
  ;

IL_ListCloser
  : RIGHT_SQUARE_BRACKET -> popMode, popMode // back to INSIDE_MARKUP_OPENER
  ;

// ----------------- Everything INSIDE of a MARKUP CLOSER -------------
mode INSIDE_MARKUP_CLOSER;

IMC_Prefix
  : Optional
  | Suspend
  ;

IMC_Name
  : NAME
  ;

IMC_Divider
  : PIPE
  ;

IMC_Comma
  : COMMA
  ;

IMC_Suffix
  : TILDE ( NAME | DIGIT+ )
  ;

IMC_WS
  : WS  -> skip
  ;

IMC_EndCloseMarkup
  : RIGHT_SQUARE_BRACKET -> popMode // back to DEFAULT
  ;

// ----------------- Everything INSIDE of a TEXT VARIATION -------------
mode INSIDE_TEXT_VARIATION;

ITV_Comment
  : Comment -> skip
  ;

ITV_Text
  : ( ~[<[|] | TEXT_VARIATION_ESCAPE_CHARACTER )+
  ;

ITV_BeginOpenMarkup // [ moves into markup tag
  : LEFT_SQUARE_BRACKET  -> pushMode(INSIDE_MARKUP_OPENER)
  ;

ITV_BeginTextVariation
  : TextVariationStartTag  -> pushMode(INSIDE_TEXT_VARIATION)
  ;

ITV_BeginCloseMarkup
  : TagCloseStartChar  -> pushMode(INSIDE_MARKUP_CLOSER)
  ;

ITV_EndTextVariation
  : TextVariationEndTag -> popMode
  ;

// ----------------- lots of repeated stuff --------------------------

Comment
  : '[! ' .*? ' !]'
  ;

TagOpenEndChar
  : '>'
  ;

TagCloseStartChar
  : '<'
  ;

TextVariationStartTag
  : '<|'
  ;

TextVariationEndTag
  : '|>'
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

REGULAR_TEXT_ESCAPE_CHARACTER
  : '\\<'
  | '\\['
  | ESCAPE_CHARACTER
  ;

TEXT_VARIATION_ESCAPE_CHARACTER
  : REGULAR_TEXT_ESCAPE_CHARACTER
  | '\\|'
  ;

COMMENT_ESCAPE_CHARACTER
  : ESCAPE_CHARACTER
  | '\\!'
  ;

SINGLE_QUOTED_TEXT_ESCAPE_CHARACTER
  : ESCAPE_CHARACTER
  | '\\\''
  ;

DOUBLE_QUOTED_TEXT_ESCAPE_CHARACTER
  : ESCAPE_CHARACTER
  | '\\"'
  ;

fragment ESCAPE_CHARACTER  : '\\\\';

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
  | '_' | DIGIT
//  | '.'
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

WS
  : [ \t\r\n]+
  ;

//UNEXPECTED_CHAR // Throw unexpected token exception
//  :  .
//  ;
