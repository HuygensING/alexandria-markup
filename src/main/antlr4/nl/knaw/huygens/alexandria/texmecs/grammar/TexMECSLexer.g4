// based on http://mlcd.blackmesatech.com/mlcd/2003/Papers/texmecs.html

lexer grammar TexMECSLexer;

BEGIN_COMMENT
  : '<*' -> pushMode(INSIDE_COMMENT)
  ;

BEGIN_CDATA
  : '<#CDATA<' -> pushMode(INSIDE_CDATA)
  ;

BEGIN_VIRTUAL_ELEMENT
  : '<^' -> pushMode(INSIDE_VIRTUAL_ELEMENT)
  ;

BEGIN_RESUME_TAG
  : '<+' -> pushMode(INSIDE_START_TAG)
  ;

BEGIN_START_TAGSET
  : '<|' -> pushMode(INSIDE_START_TAG)
  ;

BEGIN_START_TAG
  : '<' -> pushMode(INSIDE_START_TAG)
  ;

TEXT
  : ~[<|\r\n]+
  ;

NL
  : [\r\n]+ -> skip
  ;

BEGIN_SUSPEND_TAG
  : '|-' -> pushMode(INSIDE_END_TAG)
  ;

BEGIN_END_TAGSET
  : '||' -> pushMode(INSIDE_END_TAG)
  ;

BEGIN_END_TAG
  : '|' -> pushMode(INSIDE_END_TAG)
  ;

//-----------------
mode INSIDE_START_TAG;

NAME_O
  : NAME
  ;

SUFFIX_O
  : SUFFIX
  ;

AtChar : '@';

WS
  :	[ \t\r\n] -> skip
  ;

EQUALS
  : '='
  ;

RESUME
  : '+'
  ;

STRING
  : '"' ~[<"]* '"'
  | '\'' ~[<']* '\''
  ;

END_START_TAGSET
  : '||' -> popMode
  ;

END_START_TAG
  : '|' -> popMode
  ;

END_SOLE_TAG
  : '>' -> popMode
  ;

//-----------------
mode INSIDE_END_TAG;

NAME_C
  : NAME
  ;

SUFFIX_C
  : SUFFIX
  ;

END_END_TAGSET
  : '|>' -> popMode
  ;

END_END_TAG
  : '>' -> popMode
  ;

//-----------------
mode INSIDE_COMMENT;

BEGIN_COMMENT_IN_COMMENT
  : '<*' -> pushMode(INSIDE_COMMENT)
  ;

END_COMMENT
  : '*>' -> popMode
  ;

COMMENT_TEXT
  : ~[*><]+
  ;

//-----------------
mode INSIDE_CDATA;

END_CDATA
  : '>#CDATA>' -> popMode
  ;

CDATA
  : ~[>]+
  ;

//-----------------
mode INSIDE_VIRTUAL_ELEMENT;

END_VIRTUAL_ELEMENT
  : '>' -> popMode
  ;

NAME_V
  : NAME
  ;

CARET
  : '^'
  ;

EQUALS_V
  : '='
  ;

STRING_V
  : '"' ~[<"]* '"'
  | '\'' ~[<']* '\''
  ;

//-----------------

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

S
  : [ \t\r\n\u000C]+ //(#x20 | #x9 | #xD | #xA | #x85 | #x2028 | #x2029)+
  ;

//URL
//  : ~[>]+ //(CHAR/* - '>'*/)+
//  ;
//

SUFFIX
  : '~' Namechar*
  ;

//DQSTRINGCHAR
//  : ~["]
//  ;
//
//SQSTRINGCHAR
//  : ~[']
//  ;

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

DoublePipeChar : '||';


DQuoteChar : '"';

SQuoteChar : '\'';

CommentOpen : '<*';

CommentClose : '*>';

CDataOpen : '<#CDATA<';

CDataClose : '>#CDATA>';

CharacterRefOpen : '<#';

ExternalEntityOpen : '<<';

ExternalEntityClose : '>>';

Dot : '.';

InternalEntityOpen : '<&';

ResumeOpen : '<+';

SuspendOpen : '|-';

StartTagSetOpen : '<|';

EndTagSetClose : '|>';

VirtualElementOpen : '<^';

Caret : '^';

fragment
D : 'd' | 'D' ;

fragment
X : 'x' | 'X' ;
