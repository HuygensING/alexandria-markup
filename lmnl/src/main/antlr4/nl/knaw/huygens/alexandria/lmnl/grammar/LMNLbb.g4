grammar LMNLbb;

document
  : limen EOF
  ;

limen
  : ( rangeOpenTag | text | rangeCloseTag )+
  ;

rangeOpenTag
  : '[' rangeName (annotation)* '}'
  ;

rangeCloseTag
  : '{' rangeName (annotation)* ']'
  ;

annotation
  : '[' annotationName (annotation)* '[' limen '{' annotationName? ']' # annotationWithLimen
  | '[' annotationName (annotation)* ']' # anonymousAnnotation
  ;

text
  : TEXT+
  ;

rangeName
  : Name_Range
  ;

annotationName
  : Name_Annotation
  ;

Name_Range
  : NameStartChar NameChar* ('=' NameChar+)?
  ;

Name_Annotation
  :   NameStartChar NameChar*
  ;

TEXT  // match any 16 bit char other than { (start close tag) and [ (start open tag)
//  : ~[{}\\[\]]+
  : [a-zA-Z ]+
  ;

WS
  : [ \t\r\n] -> skip
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
  : [a-zA-Z]
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
