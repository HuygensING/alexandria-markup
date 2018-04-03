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
  : BEGIN_OPEN_MARKUP ( Optional | Resume )? name suffix? annotation* END_OPEN_MARKUP // possible recursion
  ;

endTag
  : BEGIN_CLOSE_MARKUP ( Optional | Suspend )? name suffix? END_CLOSE_MARKUP
  ;

name
  : NameOpenMarkup
  | NameCloseMarkup
  ;

suffix
  : TILDE markupIdentifier
  ;

markupIdentifier
  : name
  ;

annotation
  : name EQ annotationValue
  ;

annotationValue
  : stringValue
  | booleanValue
  | numberValue
  | mixedContentValue
  | listValue
  | objectValue
  ;

stringValue
  : SINGLE_QUOTE TEXT SINGLE_QUOTE
  | DOUBLE_QUOTE TEXT DOUBLE_QUOTE
  ;

booleanValue
  : TRUE
  | FALSE
  ;

numberValue
  : DIGIT+ ( DOT DIGIT+ )?
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
  : LEFT_SQUARE_BRACKET name annotation+ RIGHT_SQUARE_BRACKET // possible recursion
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

