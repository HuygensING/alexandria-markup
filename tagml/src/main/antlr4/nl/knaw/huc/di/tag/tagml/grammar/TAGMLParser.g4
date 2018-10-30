parser grammar TAGMLParser;

options { tokenVocab=TAGMLLexer; }

document
  :  ( namespaceDefinition text* )* chunk+ EOF
  ;

namespaceDefinition
  : DEFAULT_NamespaceOpener IN_NamespaceIdentifier IN_NamespaceURI IN_NamespaceCloser
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
  | IRT_BeginOpenMarkup
  ;

endOpenMarkup
  : IMO_EndOpenMarkup
  | A_EndOpenMarkup
  ;

markupName
  : prefix? name suffix? layerInfo?
  ;

prefix
  : IMO_Prefix
  | IMC_Prefix
  ;

layerInfo
  : divider layerName ( comma layerName )*
  ;

comma
  : IMO_Comma
  | IMC_Comma
  ;

divider
  : IMO_Divider
  | IMC_Divider
  ;

layerName
  : name? IMO_Prefix? name
  ;

name
  : IMO_Name
  | IMC_Name
  ;

suffix
  : IMO_Suffix
  | IMC_Suffix
  ;

endTag
  : beginCloseMarkup markupName IMC_EndCloseMarkup
  ;

beginCloseMarkup
  : DEFAULT_BeginCloseMarkup
  | ITV_BeginCloseMarkup
  | IRT_BeginCloseMarkup
  ;

milestoneTag
  : beginOpenMarkup name layerInfo? annotation* endMilestoneTag // possible recursion
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
  | richTextValue
  | listValue
  | objectValue
  ;

idValue
  : AV_IdValue
  ;

ref
  : A_Ref
  | IO_Ref
  ;

refValue
  : RV_RefValue
  ;

booleanValue
  : AV_TRUE
  | AV_FALSE
  ;

richTextValue
  : AV_RichTextOpener chunk* IRT_RichTextCloser // recursion!
  ;

listValue
  : AV_ListOpener annotationValue ( IL_WS annotationValue )* IL_ListCloser // possible recursion
  ;

objectValue
  : AV_ObjectOpener annotation annotation* IO_ObjectCloser // recursion!
  ;

textVariation
  : beginTextVariation variantText ( textVariationSeparator variantText )+ ITV_EndTextVariation
  ;

beginTextVariation
  : DEFAULT_BeginTextVariation
  | ITV_BeginTextVariation
  | IRT_BeginTextVariation
  ;

textVariationSeparator
  : TextVariationSeparator
  ;

variantText
  : ( chunk | ITV_Text )+
  ;

text
  : DEFAULT_Text
  | ITV_Text
  | IRT_Text
  ;
