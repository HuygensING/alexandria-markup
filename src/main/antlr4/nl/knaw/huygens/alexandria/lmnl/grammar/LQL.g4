/*
 * Grammar for the querying a LMNL model
 *
 * parts borrowed from https://github.com/antlr/grammars-v4/blob/master/sqlite/SQLite.g4
 */

grammar LQL;

parse
  : sql_stmt_list* EOF
  ;


sql_stmt_list
  : ';'* sql_stmt ( ';'+ sql_stmt )* ';'*
  ;

sql_stmt
  : TODO
  ;
