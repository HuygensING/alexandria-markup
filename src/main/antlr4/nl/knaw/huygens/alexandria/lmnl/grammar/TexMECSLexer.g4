// based on http://mlcd.blackmesatech.com/mlcd/2003/Papers/texmecs.html

lexer grammar TexMECSLexer;

BEGIN_RESUME_TAG
  : '<+' -> pushMode(INSIDE_OPEN_TAG)
  ;

BEGIN_OPEN_TAG
  : '<' -> pushMode(INSIDE_OPEN_TAG)
  ;

TEXT
  : ~[<|]+
  ;

BEGIN_SUSPEND_TAG
  : '|-' -> pushMode(INSIDE_CLOSE_TAG)
  ;

BEGIN_CLOSE_TAG
  : '|' -> pushMode(INSIDE_CLOSE_TAG)
  ;

//-----------------
mode INSIDE_OPEN_TAG;

NAME_O
  : NAME
  ;

SUFFIX_O
  : SUFFIX
  ;

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

END_OPEN_TAG
  : '|' -> popMode
  ;

END_SOLE_TAG
  : '>' -> popMode
  ;

//-----------------
mode INSIDE_CLOSE_TAG;

NAME_C
  : NAME
  ;

SUFFIX_C
  : SUFFIX
  ;

END_CLOSE_TAG
  : '>' -> popMode
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

AtChar : '@';

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
