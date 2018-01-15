// based on http://mlcd.blackmesatech.com/mlcd/2003/Papers/texmecs.html

parser grammar TexMECSParser;

options { tokenVocab=TexMECSLexer; }

document
  :  chunk* EOF
  ;

chunk
  : startTag
  | endTag
  | soleTag
  | suspendTag
  | resumeTag
  | startTagSet
  | endTagSet
  | startTagSet
  | endTagSet
  | virtualElement
//  | internalEntity
//  | externalEntity
//  | characterRef
  | cdataSection
  | comment
  | text
  ;

startTag // WFC: endTag match
  : BEGIN_START_TAG eid atts END_START_TAG
  ;

endTag // WFC: startTag match
  : BEGIN_END_TAG gi END_END_TAG
  ;

soleTag
  : BEGIN_START_TAG eid atts END_SOLE_TAG
  ;

suspendTag /* WFC: suspend-tag OK */
  : BEGIN_SUSPEND_TAG gi END_END_TAG
  ;

resumeTag /* WFC: resume-tag OK */
  : BEGIN_RESUME_TAG gi END_START_TAG
  ;

virtualElement // WFC: idref OK. The idref getDocumentId in a virtual element must appear on some element in the document as the getDocumentId of an id.
  : BEGIN_VIRTUAL_ELEMENT eid CARET idref atts END_VIRTUAL_ELEMENT
  ;

startTagSet // WFC: endTagSet match
  : BEGIN_START_TAGSET eid atts END_START_TAGSET
  ;

endTagSet /* WFC: startTagSet match */
  : BEGIN_END_TAGSET gi END_END_TAGSET
  ;

//internalEntity /* CF: structured internal entities */
//  : InternalEntityOpen NAME RightAngleBracket
//  | InternalEntityOpen NAME S Dot S NAME RightAngleBracket
//  ;

//externalEntity /* CF: external entities */
//  : ExternalEntityOpen URL ExternalEntityClose
//  ;

//characterRef
//  : CharacterRefOpen D DIGITS RightAngleBracket     # digitalCharacterRef
//  | CharacterRefOpen X HEXDIGITS RightAngleBracket  # hexadecimalCharacterRef
//  ;

cdataSection /* CF: CDATA sections */
  : BEGIN_CDATA cdsecdata END_CDATA
  ;

cdsecdata
  :
  | cdchars cdsecdata
  | cdataSection cdsecdata
  ;

cdchars
//  : CHAR+ // - (CHAR* ('<#CDATA<' | '>#CDATA>') CHAR*)
  : CDATA
  ;

comment
  : (BEGIN_COMMENT|BEGIN_COMMENT_IN_COMMENT) commcontent END_COMMENT
  ;

commcontent
  : /* */
  | commcontent commentdata
  | commcontent comment
  ;

commentdata
  : CHAR+ // - (CHAR* (CommentOpen | CommentClose) CHAR*)
  | COMMENT_TEXT
  ;

eid
  : gi (AtChar id)?
  | gi (EQUALS id)
  | AtChar id
  ;

gi
  : NAME_O SUFFIX_O?
  | NAME_C SUFFIX_C?
  | NAME_V
  | SUFFIX
  ;

id /* WFC: unique ID */
  : NAME_O
  | NAME_C
  | NAME_V
  ;

idref
  : NAME_V /* WFC: idref OK */
  ;

atts
  : avs*
  ;

avs
  : NAME_O EQUALS STRING
  ;

text
  : TEXT
  ;

//quoted
//  : DQuoteChar dqstring DQuoteChar
//  | SQuoteChar sqstring SQuoteChar
//  ;
//
//dqstring
//  : (DQSTRINGCHAR | internalEntity | characterRef)*
//  ;
//
//sqstring
//  : (SQSTRINGCHAR | internalEntity | characterRef)*
//  ;
