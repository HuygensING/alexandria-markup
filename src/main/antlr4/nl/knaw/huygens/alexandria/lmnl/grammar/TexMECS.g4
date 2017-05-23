// based on http://mlcd.blackmesatech.com/mlcd/2003/Papers/texmecs.html

grammar TexMECS;

BEGIN_OPEN_START_TAG
  : '<' -> pushMode(INSIDE_OPEN_START_TAG)
  ;


soleTag
  : '<' eid atts '>'
  ;

virtualElement // WFC: idref OK. The idref value in a virtual element must appear on some element in the document as the value of an id.
  : '<^' eid '^' idref atts '>'
  ;

startTag // WFC: endTag match
  : '<' eid atts '|'
  ;

endTag // WFC: startTag match
  : '|' gi '>'
  ;

startTagSet // WFC: endTagSet match
  : '<|' eid atts '||'
  ;

endTagSet /* WFC: startTagSet match */
  : '||' gi '|>'
  ;

suspendTag /* WFC: suspend-tag OK */
  : '|-' gi '>'
  ;

resumeTag /* WFC: resume-tag OK */
  : '<+' gi '|'
  ;

internalEntity /* CF: structured internal entities */
  : '<&' NAME '>'
  | '<&' NAME S '.' S NAME '>'
  ;

externalEntity /* CF: external entities */
  : '<<' URL '>>'
  ;

characterRef
  : '<#' D DIGITS '>'     # digitalCharacterRef
  | '<#' X HEXDIGITS '>'  # hexadecimalCharacterRef
  ;

cdataSection /* CF: CDATA sections */
  : '<#CDATA<' cdsecdata '>#CDATA>'
  ;

cdsecdata
  :
  | cdchars cdsecdata
  | cdataSection cdsecdata
  ;

cdchars
  : CHAR+ // - (CHAR* ('<#CDATA<' | '>#CDATA>') CHAR*)
  ;

comment
  : '<*' commcontent '*>'
  ;

commcontent
  : /* */
  | commcontent commentdata
  | commcontent comment
  ;

commentdata
  : CHAR+ // - (CHAR* ('<*' | '*>') CHAR*)
  ;

eid
  : gi ('@' id)?
  | '@' id
  ;

gi
  : NAME SUFFIX?
  | SUFFIX
  ;

id
  : NAME /* WFC: unique ID */
  ;

idref
  : NAME /* WFC: idref OK */
  ;

atts
  : avs* S?
  ;

avs
  : S NAME S? '=' S? quoted
  ;

datacharacter
  : CHAR+
  ;

NAME
  : Nameinit Namechar*
  ;

Nameinit
  : [a-zA-Z_]
  ;

Namechar
  : Nameinit
  | [0-9]
  | ':'
  | '.'
  | '-'
  ;

SUFFIX
  : '~' Namechar*
  ;

quoted
  : '"' dqstring '"'
  | '\'' sqstring '\''
  ;

dqstring
  : (DQSTRINGCHAR | internalEntity | characterRef)*
  ;

sqstring : (SQSTRINGCHAR | internalEntity | characterRef)*
  ;

DQSTRINGCHAR
  : ~["]
  ;

SQSTRINGCHAR
  : ~[']
  ;

S
  : [ \t\r\n\u000C]+ //(#x20 | #x9 | #xD | #xA | #x85 | #x2028 | #x2029)+
  ;

URL
  : ~[>]+ //(CHAR/* - '>'*/)+
  ;

CHAR
  : [a-zA-Z]
  | '\u2070'..'\u218F'
  | '\u2C00'..'\u2FEF'
  | '\u3001'..'\uD7FF'
  | '\uF900'..'\uFDCF'
  | '\uFDF0'..'\uFFFD'
  ;

DIGITS
  : [0-9]+
  ;

HEXDIGITS
  : [A-Fa-f0-9]+
  ;

//fragment
D : 'd' | 'D' ;

//fragment
X : 'x' | 'X' ;
