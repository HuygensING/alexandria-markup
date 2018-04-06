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
  | milestoneTag
  | textVariation
//  | comment
  | text
  ;

startTag
  : beginOpenMarkup markupName annotation* endOpenMarkup // possible recursion
  ;

beginOpenMarkup
  : BEGIN_OPEN_MARKUP
  | TV_BEGIN_OPEN_MARKUP
  ;

endOpenMarkup
  : END_OPEN_MARKUP
  | A_END_OPEN_MARKUP
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
  : annotationName eq annotationValue # BasicAnnotation
  | annotationName ref refValue       # RefAnnotation
  | idAnnotation eq idValue           # IdentifyingAnnotation
  ;

annotationName
  : ANNOTATION_NAME
  | O_ANNOTATION_NAME
  ;

idAnnotation
  : ID_ANNOTATION
  | O_ID_ANNOTATION
  ;

eq
  : EQ
  | O_EQ
  ;

annotationValue
  : StringValue
  | booleanValue
  | NumberValue
  | mixedContentValue
  | listValue
  | objectValue
  ;

idValue
  : ID_VALUE
  ;

ref
  : REF
  ;

refValue
  : REF_VALUE
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
  : OBJECT_OPENER annotation (annotation)* OBJECT_CLOSER // recursion!
  ;

milestoneTag
  : BEGIN_OPEN_MARKUP name annotation+ endMilestoneTag // possible recursion
  ;

endMilestoneTag
  : END_ANONYMOUS_MARKUP
  | A_END_ANONYMOUS_MARKUP
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
