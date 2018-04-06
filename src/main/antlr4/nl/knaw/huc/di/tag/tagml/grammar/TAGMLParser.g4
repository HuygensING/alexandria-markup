parser grammar TAGMLParser;

options { tokenVocab=TAGMLLexer; }

document
  :  namespaceDefinition* chunk+ EOF
  ;

namespaceDefinition
  : DEFAULT_Namespace
  ;

chunk
  : startTag
  | endTag
  | milestoneTag
  | textVariation
  | text
  ;

startTag
  : beginOpenMarkup markupName annotation* endOpenMarkup // possible recursion
  ;

beginOpenMarkup
  : DEFAULT_BeginOpenMarkup
  | ITV_BeginOpenMarkup
  | IMX_BeginOpenMarkup
  ;

endOpenMarkup
  : IMO_EndOpenMarkup
  | A_EndOpenMarkup
  ;

markupName
  : IMO_Prefix? name IMO_Suffix?
  | IMC_Prefix? name IMC_Suffix?
  ;

endTag
  : beginCloseMarkup markupName IMC_EndCloseMarkup
  ;

beginCloseMarkup
  : DEFAULT_BeginCloseMarkup
  | ITV_BeginCloseMarkup
  | IMX_BeginCloseMarkup
  ;

name
  : IMO_NameOpenMarkup
  | IMC_NameCloseMarkup
  ;

milestoneTag
  : beginOpenMarkup name annotation+ endMilestoneTag // possible recursion
  ;

endMilestoneTag
  : IMO_EndMilestoneMarkup
  | A_EndMilestoneMarkup
  ;

annotation
  : annotationName eq annotationValue # BasicAnnotation
  | annotationName ref refValue       # RefAnnotation
  | idAnnotation eq idValue           # IdentifyingAnnotation
  ;

annotationName
  : A_AnnotationName
  | IO_AnnotationName
  ;

idAnnotation
  : A_IdAnnotation
  | IO_IdAnnotation
  ;

eq
  : A_EQ
  | IO_EQ
  ;

annotationValue
  : AV_StringValue
  | booleanValue
  | AV_NumberValue
  | mixedContentValue
  | listValue
  | objectValue
  ;

idValue
  : AV_IdValue
  ;

ref
  : A_Ref
  ;

refValue
  : RV_RefValue
  ;

booleanValue
  : AV_TRUE
  | AV_FALSE
  ;

mixedContentValue
  : AV_MixedContentOpener chunk* IMX_MixedContentCloser // recursion!
  ;

listValue
  : AV_ListOpener annotationValue ( IL_COMMA annotationValue )* IL_ListCloser // possible recursion
  ;

objectValue
  : AV_ObjectOpener annotation annotation* IO_ObjectCloser // recursion!
  ;

textVariation
  : beginTextVariation variantText ( TextVariationSeparator variantText )+ ITV_EndTextVariation
  ;

beginTextVariation
  : DEFAULT_BeginTextVariation
  | ITV_BeginTextVariation
  | IMX_BeginTextVariation
  ;

variantText
  : ( chunk | ITV_Text )+
  ;

text
  : DEFAULT_Text
  | ITV_Text
  | IMX_Text
  ;
