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
  : beginOpenMarkup markupName annotation* END_OPEN_MARKUP // possible recursion
  ;

beginOpenMarkup
  : BEGIN_OPEN_MARKUP
  | TV_BEGIN_OPEN_MARKUP
  ;

markupName
  : PREFIX? name SUFFIX?
  | CM_PREFIX? name CM_SUFFIX?
  ;

endTag
  : beginCloseMarkup markupName END_CLOSE_MARKUP
  ;

beginCloseMarkup
  : BEGIN_CLOSE_MARKUP
  | TV_BEGIN_CLOSE_MARKUP
  ;

name
  : NameOpenMarkup
  | NameCloseMarkup
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
  : beginTextVariation variantText (TextVariationSeparator variantText)+ END_TEXT_VARIATION
  ;

beginTextVariation
  : BEGIN_TEXT_VARIATION
  | TV_BEGIN_TEXT_VARIATION
  ;

variantText
  : ( chunk | VARIANT_TEXT )+
  ;

text
  : TEXT
  | VARIANT_TEXT
  ;
