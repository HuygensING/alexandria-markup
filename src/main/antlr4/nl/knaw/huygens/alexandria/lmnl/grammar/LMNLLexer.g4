/*
 * Grammar for the LMNL overlapping markup language format
 * @author: Ronald Haentjens Dekker
 * @author: Bram Buitendijk
 *
 * NOTE: Implementation of Name, DIGIT and NameChar are taken from ANTLR4 XML grammar
 */

lexer grammar LMNLLexer;

// In the default mode we are outside a Range

COMMENT
  : '[!--' .*? '--]' -> skip //channel(HIDDEN)
  ;

BEGIN_OPEN_RANGE // [ moves into range
  : '['  -> pushMode(INSIDE_RANGE_OPENER)
  ;

BEGIN_CLOSE_RANGE
  : '{'  -> pushMode(INSIDE_RANGE_CLOSER)
  ;

TEXT  // match any 16 bit char other than { (start close tag) and [ (start open tag)
  : ~[{\\[]+
  ;

// ----------------- Everything INSIDE of a RANGE OPENER ---------------------
mode INSIDE_RANGE_OPENER;

COMMENT_IN_RANGE_OPENER
  : '[!--' .*? '--]' -> skip // channel(HIDDEN)
  ;

END_ANONYMOUS_RANGE
  :   ']'  -> popMode
  ;

END_OPEN_RANGE
  :   '}'  -> popMode
  ;

BEGIN_OPEN_ANNO
  :   '['  -> pushMode(INSIDE_ANNOTATION_OPENER)
  ;

Name_Open_Range
  :   NameStartChar NameChar* ('=' NameChar+)?
  ;

RANGE_S
  :   WS  -> skip
  ;

// ----------------- Everything INSIDE of a RANGE CLOSER -------------
mode INSIDE_RANGE_CLOSER;

COMMENT_IN_RANGE_CLOSER
  : '[!--' .*? '--]' -> skip //channel(HIDDEN)
  ;

END_CLOSE_RANGE
  :   ']'  -> popMode
  ;

BEGIN_OPEN_ANNO_IN_RANGE_CLOSER
  :   '['  -> pushMode(INSIDE_ANNOTATION_OPENER)
  ;

Name_Close_Range
  :   NameStartChar NameChar* ('=' NameChar+)?
  ;

RANGE_S2
  :   WS  -> skip
  ;

// ------------------ Everything INSIDE of a ANNOTATION OPENER -----------
// NOTE: Annotation openers are close to range openers, but not the same!
// NOTE: We can have anonymous annotations!
mode INSIDE_ANNOTATION_OPENER;

END_ANONYMOUS_ANNO
  :   ']'  -> popMode
  ;

END_OPEN_ANNO
  :   '}'  -> popMode, pushMode(INSIDE_ANNOTATION_TEXT)
  ;

OPEN_ANNO_IN_ANNO
  :   '['  -> pushMode(INSIDE_ANNOTATION_OPENER)
  ;

Name_Open_Annotation
  :   NameStartChar NameChar*
  ;

ANNO_S
  :   WS  -> skip
  ;

// ----------------- Everything INSIDE of a ANNOTATION CLOSER -------------
// NOTE: Annotation closers are exact copy of range closers
mode INSIDE_ANNOTATION_CLOSER;

END_CLOSE_ANNO
  :   ']'  -> popMode
  ;

Name_Close_Annotation
  :   NameStartChar NameChar*
  ;

// ------------------ Inside ANNOTATION TEXT --------------------------------------
// NOTE:c Annotation text is simi
mode INSIDE_ANNOTATION_TEXT;

//BEGIN_ANNO_OPEN_RANGE
//  : '['  -> pushMode(INSIDE_RANGE_OPENER)
//  ;

BEGIN_CLOSE_ANNO
  : '{'  -> popMode, pushMode(INSIDE_ANNOTATION_CLOSER)
  ;

ANNO_TEXT  // match any 16 bit char other than { (start close tag) and [ (start open tag)
  : ~[{\\[]+ ;

// ----------------- lots of repeated stuff --------------------------

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
  :  [0-9]
  ;

fragment
WS
  :  [ \t\r\n]
  ;

//UNEXPECTED_CHAR // Throw unexpected token exception
//  :  .
//  ;
