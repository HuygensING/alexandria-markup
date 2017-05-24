parser grammar LMNLParser;

options { tokenVocab=LMNLLexer; }

document
  : limen EOF
  ;

limen
  : ( rangeOpenTag | text | rangeCloseTag )+
  ;

rangeOpenTag
  : TagOpenStartChar rangeName (annotation)* TagOpenEndChar
  ;

rangeCloseTag
  : TagCloseStartChar rangeName (annotation)* TagCloseEndChar
  ;

annotation
  : TagOpenStartChar annotationName (annotation)* TagOpenStartChar limen TagCloseStartChar annotationName? TagCloseEndChar # annotationWithLimen
  | TagOpenStartChar annotationName (annotation)* TagCloseEndChar # anonymousAnnotation
  ;

text
  : TEXT+
  ;

rangeName
  : Name_Open_Range
  | Name_Close_Range
  ;

annotationName
  : Name_Open_Annotation
  | Name_Close_Annotation
  ;
