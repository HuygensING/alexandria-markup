parser grammar TAGMLParser;

options { tokenVocab=TAGMLLexer; }

document
  :  namespaceDefinition* chunk* EOF
  ;

namespaceDefinition
  : NAMESPACE
  ;

chunk
  : startTag
  | endTag
  | milestone
  | textVariation
//  | comment
  | text
  ;

startTag
  : BEGIN_OPEN_MARKUP PREFIX? name SUFFIX? annotation* END_OPEN_MARKUP // possible recursion
  ;

endTag
  : BEGIN_CLOSE_MARKUP ( Optional | Suspend )? name SUFFIX? END_CLOSE_MARKUP
  ;

name
  : NameOpenMarkup
  | NameCloseMarkup
  ;

markupIdentifier
  : name
  ;

annotation
  : ANNOTATION_NAME EQ annotationValue
  ;

annotationValue
  : StringValue
  | booleanValue
  | NumberValue
  | mixedContentValue
  | listValue
  | objectValue
  ;

booleanValue
  : TRUE
  | FALSE
  ;

mixedContentValue
  : PIPE chunk* PIPE // recursion!
  ;

listValue
  : LIST_OPENER annotationValue (COMMA annotationValue)+ LIST_CLOSER // possible recursion
  ;

objectValue
  : OBJECT_OPENER annotation (annotation)+ OBJECT_CLOSER // recursion!
  ;

milestone
  : BEGIN_OPEN_MARKUP name annotation+ END_ANONYMOUS_MARKUP // possible recursion
  ;

textVariation
  : TextVariationStartTag variantText (TextVariationSeparator variantText)+ TextVariationEndTag
  ;

variantText
  : chunk+
  ;

text
  : TEXT
  ;

