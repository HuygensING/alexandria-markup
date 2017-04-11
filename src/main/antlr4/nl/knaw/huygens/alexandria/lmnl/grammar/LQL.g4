grammar LQL;

lql_script
  : statement* EOF
  ;

statement
  : select_statement
  ;

select_statement
  : SELECT identifier DOT part FROM source where_stmt
  ;

where_stmt
  : WHERE
  ;

identifier
  :
  ;

part
  : TEXT
  ;

source
  : MARKUP
  ;

SELECT
  : S E L E C T
  ;

FROM
  : F R O M
  ;

TEXT
  : T E X T
  ;

MARKUP
  : M A R K U P
  ;

WHERE
  : W H E R E
  ;


UNEXPECTED_CHAR
  : .
  ;

MULTILINE_COMMENT
  : '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN)
  ;

SPACES
  : [ \u000B\t\r\n] -> channel(HIDDEN)
  ;

fragment DOT : '.';

fragment DIGIT : [0-9];

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];