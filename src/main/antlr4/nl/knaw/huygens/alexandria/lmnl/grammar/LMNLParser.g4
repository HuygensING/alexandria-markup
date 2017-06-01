parser grammar LMNLParser;

options { tokenVocab=LMNLLexer; }

document
  : limen EOF
  ;

limen
  : ( rangeOpenTag | text | rangeCloseTag )+
  ;

rangeOpenTag
  : BEGIN_OPEN_RANGE rangeName (annotation)* END_OPEN_RANGE
  ;

rangeCloseTag
  : BEGIN_CLOSE_RANGE rangeName (annotation)* END_CLOSE_RANGE
  ;

annotation
  : BEGIN_OPEN_ANNO annotationName (annotation)* END_OPEN_ANNO limen BEGIN_CLOSE_ANNO annotationName? END_CLOSE_ANNO # annotationWithLimen
  | BEGIN_OPEN_ANNO annotationName (annotation)* END_EMPTY_ANNO # emptyAnnotation
  | OPEN_ANNO_IN_ANNO_OPENER annotationName (annotation)* END_OPEN_ANNO limen BEGIN_CLOSE_ANNO annotationName? END_CLOSE_ANNO # nestedAnnotationWithLimen
  | OPEN_ANNO_IN_ANNO_OPENER annotationName (annotation)* END_EMPTY_ANNO # nestedEmptyAnnotation
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
