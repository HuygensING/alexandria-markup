import Data.Char
-- An algorithm for RELAX NG validation

-- James Clark (jjc@thaiopensource.com)

-- 2002-02-13

-- Table of contents

-- Basics
-- Optimizations
--   Computing nullable
--   Interning patterns
--   Avoiding exponential blowup
--   Memoization
-- Error handling
-- References
-- This document describes an algorithm for validating an XML document against a RELAX NG schema. This algorithm is based on the idea of what's called a derivative (sometimes called a residual). It is not the only possible algorithm for RELAX NG validation. This document does not describe any algorithms for transforming a RELAX NG schema into simplified form, nor for determining whether a RELAX NG schema is correct.

-- We use Haskell to describe the algorithm. Do not worry if you don't know Haskell; we use only a tiny subset which should be easily understandable.

-- Basics

-- First, we define the datatypes we will be using. URIs and local names are just strings.

type Uri = String
type LocalName = String

-- A ParamList represents a list of parameters; each parameter is a pair consisting of a local name and a value.
type ParamList = [(LocalName, String)]

-- A Context represents the context of an XML element. It consists of a base URI and a mapping from prefixes to namespace URIs.
type Prefix = String
type Context = (Uri, [(Prefix, Uri)])

-- A Datatype identifies a datatype by a datatype library name and a local name.
type Datatype = (Uri, LocalName)

-- A NameClass represents a name class.
data NameClass = AnyName
                 | AnyNameExcept NameClass
                 | Name Uri LocalName
                 | NsName Uri
                 | NsNameExcept Uri NameClass
                 | NameClassChoice NameClass NameClass

-- A Pattern represents a pattern after simplification.
data Pattern = Empty
               | NotAllowed
               | Text
               | Choice Pattern Pattern
               | Interleave Pattern Pattern
               | Group Pattern Pattern
               | OneOrMore Pattern
               | List Pattern
               | Data Datatype ParamList
               | DataExcept Datatype ParamList Pattern
               | Value Datatype String Context
               | Attribute NameClass Pattern
               | Element NameClass Pattern
               | After Pattern Pattern
-- The After pattern is used internally and will be explained later.

-- Note that there is an Element pattern rather than a Ref pattern. In the simplified XML representation of patterns, every ref element refers to an element pattern. In the internal representation of patterns, we can replace each reference to a ref pattern by a reference to the element pattern that the ref pattern references, resulting in a cyclic data structure. (Note that even though Haskell is purely functional it can handle cyclic data structures because of its laziness.)

-- In the instance, elements and attributes are labelled with QNames; a QName is a URI/local name pair.

data QName = QName Uri LocalName

-- An XML document is represented as a ChildNode. There are two kinds of child node:

-- a TextNode containing a string;
-- an ElementNode containing a name (of type QName), a Context, a set of attributes (represented as a list of AttributeNodes, each of which will be an AttributeNode), and a list of children (represented as a list of ChildNodes).
data ChildNode = ElementNode QName Context [AttributeNode] [ChildNode]
                 | TextNode String
-- An AttributeNode consists of a QName and a String.

data AttributeNode = AttributeNode QName String

-- Now we're ready to define our first function: contains tests whether a NameClass contains a particular QName.
contains :: NameClass -> QName -> Bool
contains AnyName _ = True
contains (AnyNameExcept nc) n = not (contains nc n)
contains (NsName ns1) (QName ns2 _) = (ns1 == ns2)
contains (NsNameExcept ns1 nc) (QName ns2 ln) =
  ns1 == ns2 && not (contains nc (QName ns2 ln))
contains (Name ns1 ln1) (QName ns2 ln2) = (ns1 == ns2) && (ln1 == ln2)
contains (NameClassChoice nc1 nc2) n = (contains nc1 n) || (contains nc2 n)
-- In Haskell, _ is an anonymous variable that matches any argument.

-- nullable tests whether a pattern matches the empty sequence.
nullable:: Pattern -> Bool
nullable (Group p1 p2) = nullable p1 && nullable p2
nullable (Interleave p1 p2) = nullable p1 && nullable p2
nullable (Choice p1 p2) = nullable p1 || nullable p2
nullable (OneOrMore p) = nullable p
nullable (Element _ _) = False
nullable (Attribute _ _) = False
nullable (List _) = False
nullable (Value _ _ _) = False
nullable (Data _ _) = False
nullable (DataExcept _ _ _) = False
nullable NotAllowed = False
nullable Empty = True
nullable Text = True
nullable (After _ _) = False
-- The key concept used by this validation technique is the concept of a derivative. The derivative of a pattern p with respect to a node x is a pattern for what's left of p after matching x; in other words, it is a pattern that matches any sequence that when appended to x will match p.

-- If we can compute derivatives, then we can determine whether a pattern matches a node: a pattern matches a node if the derivative of the pattern with respect to the node is nullable.

-- It is desirable to be able to compute the derivative of a node in a streaming fashion, making a single pass over the tree. In order to do this, we break down an element into a sequence of components:

-- a start-tag open containing a QName
-- a sequence of zero or more attributes
-- a start-tag close
-- a sequence of zero or more children
-- an end-tag
-- We compute the derivative of a pattern with respect to an element by computing its derivative with respect to each component in turn.

-- We can now explain why we need the After pattern. A pattern After x y is a pattern that matches x followed by an end-tag followed by y. We need the After pattern in order to be able to express the derivative of a pattern with respect to a start-tag open.

-- The central function is childNode which computes the derivative of a pattern with respect to a ChildNode and a Context:
childDeriv :: Context -> Pattern -> ChildNode -> Pattern
childDeriv cx p (TextNode s) = textDeriv cx p s
childDeriv _ p (ElementNode qn cx atts children) =
  let p1 = startTagOpenDeriv p qn
      p2 = attsDeriv cx p1 atts
      p3 = startTagCloseDeriv p2
      p4 = childrenDeriv cx p3 children
  in endTagDeriv p4
-- textDeriv computes the derivative of a pattern with respect to a text node.

textDeriv :: Context -> Pattern -> String -> Pattern
-- Choice is easy:
textDeriv cx (Choice p1 p2) s =
  choice (textDeriv cx p1 s) (textDeriv cx p2 s)

-- Interleave is almost as easy (one of the main advantages of this validation technique is the ease with which it handles interleave):
textDeriv cx (Interleave p1 p2) s =
  choice (interleave (textDeriv cx p1 s) p2)
         (interleave p1 (textDeriv cx p2 s))

-- For Group, the derivative depends on whether the first operand is nullable.
textDeriv cx (Group p1 p2) s =
  let p = group (textDeriv cx p1 s) p2
  in if nullable p1 then choice p (textDeriv cx p2 s) else p

-- For After, we recursively apply textDeriv to the first argument.
textDeriv cx (After p1 p2) s = after (textDeriv cx p1 s) p2

-- For OneOrMore we partially expand the OneOrMore into a Group.
textDeriv cx (OneOrMore p) s =
  group (textDeriv cx p s) (choice (OneOrMore p) Empty)

-- A text pattern matches zero or more text nodes. Thus the derivative of Text with respect to a text node is Text, not Empty.
textDeriv cx Text _ = Text

-- The derivative of a value, data or list pattern with respect to a text node is Empty if the pattern matches and NotAllowed if it does not.

-- To determine whether a value or data pattern matches, we rely respectively on the datatypeEqual and datatypeAllows functions which implement the semantics of a datatype library.

textDeriv cx1 (Value dt value cx2) s =
  if datatypeEqual dt value cx2 s cx1 then Empty else NotAllowed
textDeriv cx (Data dt params) s =
  if datatypeAllows dt params s cx then Empty else NotAllowed
textDeriv cx (DataExcept dt params p) s =
  if datatypeAllows dt params s cx && not (nullable (textDeriv cx p s)) then
    Empty
  else
    NotAllowed

-- To determine whether a pattern List p matches a text node, the value of the text node is split into a sequence of whitespace-delimited tokens, and the resulting sequence is matched against p:
textDeriv cx (List p) s =
  if nullable (listDeriv cx p (words s)) then Empty else NotAllowed

-- In any other case, the pattern does not match the node.
textDeriv _ _ _ = NotAllowed

-- To compute the derivative of a pattern with respect to a list of strings, simply compute the derivative with respect to each member of the list in turn.
listDeriv :: Context -> Pattern -> [String] -> Pattern
listDeriv _ p [] = p
listDeriv cx p (h:t) = listDeriv cx (textDeriv cx p h) t
-- In Haskell, [] refers to the empty list.

-- When constructing Choice, Group, Interleave and After patterns while computing derivatives, we recognize the obvious algebraic identities for NotAllowed and Empty:
choice :: Pattern -> Pattern -> Pattern
choice p NotAllowed = p
choice NotAllowed p = p
choice p1 p2 = Choice p1 p2

group :: Pattern -> Pattern -> Pattern
group p NotAllowed = NotAllowed
group NotAllowed p = NotAllowed
group p Empty = p
group Empty p = p
group p1 p2 = Group p1 p2

interleave :: Pattern -> Pattern -> Pattern
interleave p NotAllowed = NotAllowed
interleave NotAllowed p = NotAllowed
interleave p Empty = p
interleave Empty p = p
interleave p1 p2 = Interleave p1 p2

after :: Pattern -> Pattern -> Pattern
after p NotAllowed = NotAllowed
after NotAllowed p = NotAllowed
after p1 p2 = After p1 p2

-- The datatypeAllows and datatypeEqual functions represent the semantics of datatype libraries. Here, we specify only the semantics of the builtin datatype library.
datatypeAllows :: Datatype -> ParamList -> String -> Context -> Bool
datatypeAllows ("", "string") [] _ _ = True
datatypeAllows ("", "token") [] _ _ = True

datatypeEqual :: Datatype -> String -> Context -> String -> Context -> Bool
datatypeEqual ("", "string") s1 _ s2 _ = (s1 == s2)
datatypeEqual ("", "token") s1 _ s2 _ = (normalizeWhitespace s1) == (normalizeWhitespace s2)

normalizeWhitespace :: String -> String
normalizeWhitespace s = unwords (words s)

-- Perhaps the trickiest part of the algorithm is in computing the derivative with respect to a start-tag open. For this, we need a helper function; applyAfter takes a function and applies it to the second operand of each After pattern.
applyAfter :: (Pattern -> Pattern) -> Pattern -> Pattern
applyAfter f (After p1 p2) = after p1 (f p2)
applyAfter f (Choice p1 p2) = choice (applyAfter f p1) (applyAfter f p2)
applyAfter f NotAllowed = NotAllowed
-- We rely here on the fact that After patterns are restricted in where they can occur. Specifically, an After pattern cannot be the descendant of any pattern other than a Choice pattern or another After pattern; also the first operand of an After pattern can neither be an After pattern nor contain any After pattern descendants.

startTagOpenDeriv :: Pattern -> QName -> Pattern

-- The derivative of a Choice pattern is as usual.
startTagOpenDeriv (Choice p1 p2) qn = choice (startTagOpenDeriv p1 qn) (startTagOpenDeriv p2 qn)
-- To represent the derivative of a Element pattern, we introduce an After pattern.

startTagOpenDeriv (Element nc p) qn = if contains nc qn then after p Empty else NotAllowed
-- For Interleave, OneOrMore Group or After we compute the derivative in a similar way to textDeriv but with an important twist. The twist is that instead of applying interleave, group and after directly to the result of recursively applying startTagOpenDeriv, we instead use applyAfter to push the interleave, group or after down into the second operand of After. Note that the following definitions ensure that the invariants on where After patterns can occur are maintained.

-- We make use of the standard Haskell function flip which flips the order of the arguments of a function of two arguments. Thus, flip applied to a function of two arguments f and an argument x returns a function of one argument g such that g(y) = f(y, x).
startTagOpenDeriv (Interleave p1 p2) qn =
  choice (applyAfter (flip interleave p2) (startTagOpenDeriv p1 qn))
         (applyAfter (interleave p1) (startTagOpenDeriv p2 qn))
startTagOpenDeriv (OneOrMore p) qn =
  applyAfter (flip group (choice (OneOrMore p) Empty))
             (startTagOpenDeriv p qn)
startTagOpenDeriv (Group p1 p2) qn =
  let x = applyAfter (flip group p2) (startTagOpenDeriv p1 qn)
  in if nullable p1 then
       choice x (startTagOpenDeriv p2 qn)
     else
       x

startTagOpenDeriv (After p1 p2) qn =
  applyAfter (flip after p2) (startTagOpenDeriv p1 qn)

-- In any other case, the derivative is NotAllowed.
startTagOpenDeriv _ qn = NotAllowed

-- To compute the derivative of a pattern with respect to a sequence of attributes, simply compute the derivative with respect to each attribute in turn.
attsDeriv :: Context -> Pattern -> [AttributeNode] -> Pattern
attsDeriv cx p [] = p
attsDeriv cx p ((AttributeNode qn s):t) =
  attsDeriv cx (attDeriv cx p (AttributeNode qn s)) t

-- Computing the derivative with respect to an attribute done in a similar to computing the derivative with respect to a text node. The main difference is in the handling of Group, which has to deal with the fact that the order of attributes is not significant. Computing the derivative of a Group pattern with respect to an attribute node works the same as computing the derivative of an Interleave pattern.
attDeriv :: Context -> Pattern -> AttributeNode -> Pattern
attDeriv cx (After p1 p2) att =
  after (attDeriv cx p1 att) p2
attDeriv cx (Choice p1 p2) att =
  choice (attDeriv cx p1 att) (attDeriv cx p2 att)
attDeriv cx (Group p1 p2) att =
  choice (group (attDeriv cx p1 att) p2)
         (group p1 (attDeriv cx p2 att))
attDeriv cx (Interleave p1 p2) att =
  choice (interleave (attDeriv cx p1 att) p2)
         (interleave p1 (attDeriv cx p2 att))
attDeriv cx (OneOrMore p) att =
  group (attDeriv cx p att) (choice (OneOrMore p) Empty)
attDeriv cx (Attribute nc p) (AttributeNode qn s) =
  if contains nc qn && valueMatch cx p s then Empty else NotAllowed
attDeriv _ _ _ = NotAllowed

-- valueMatch is used for matching attribute values. It has to implement the RELAX NG rules on whitespace: see (weak match 2) in the RELAX NG spec.
valueMatch :: Context -> Pattern -> String -> Bool
valueMatch cx p s =
  (nullable p && whitespace s) || nullable (textDeriv cx p s)

-- When we see a start-tag close, we know that there cannot be any further attributes. Therefore we can replace each Attribute pattern by NotAllowed.
startTagCloseDeriv :: Pattern -> Pattern
startTagCloseDeriv (After p1 p2) =
  after (startTagCloseDeriv p1) p2
startTagCloseDeriv (Choice p1 p2) =
  choice (startTagCloseDeriv p1) (startTagCloseDeriv p2)
startTagCloseDeriv (Group p1 p2) =
  group (startTagCloseDeriv p1) (startTagCloseDeriv p2)
startTagCloseDeriv (Interleave p1 p2) =
  interleave (startTagCloseDeriv p1) (startTagCloseDeriv p2)
startTagCloseDeriv (OneOrMore p) =
  oneOrMore (startTagCloseDeriv p)
startTagCloseDeriv (Attribute _ _) = NotAllowed
startTagCloseDeriv p = p

-- When constructing a OneOrMore, we need to treat an operand of NotAllowed specially:
oneOrMore :: Pattern -> Pattern
oneOrMore NotAllowed = NotAllowed
oneOrMore p = OneOrMore p

-- Computing the derivative of a pattern with respect to a list of children involves computing the derivative with respect to each pattern in turn, except that whitespace requires special treatment.
childrenDeriv :: Context -> Pattern -> [ChildNode] -> Pattern

-- The case where the list of children is empty is treated as if there were a text node whose value were the empty string. See rule (weak match 3) in the RELAX NG spec.
childrenDeriv cx p [] = childrenDeriv cx p [(TextNode "")]

-- In the case where the list of children consists of a single text node and the value of the text node consists only of whitespace, the list of children matches if the list matches either with or without stripping the text node. Note the similarity with valueMatch.
childrenDeriv cx p [(TextNode s)] =
  let p1 = childDeriv cx p (TextNode s)
  in if whitespace s then choice p p1 else p1

-- Otherwise, there must be one or more elements amongst the children, in which case any whitespace-only text nodes are stripped before the derivative is computed.
childrenDeriv cx p children = stripChildrenDeriv cx p children

stripChildrenDeriv :: Context -> Pattern -> [ChildNode] -> Pattern
stripChildrenDeriv _ p [] = p
stripChildrenDeriv cx p (h:t) = 
  stripChildrenDeriv cx (if strip h then p else (childDeriv cx p h)) t

strip :: ChildNode -> Bool
strip (TextNode s) = whitespace s
strip _ = False

-- whitespace tests whether a string is contains only whitespace.
whitespace :: String -> Bool
whitespace s = all isSpace s

-- Computing the derivative of a pattern with respect to an end-tag is obvious. Note that we rely here on the invariants about where After patterns can occur.
endTagDeriv :: Pattern -> Pattern
endTagDeriv (Choice p1 p2) = choice (endTagDeriv p1) (endTagDeriv p2)
endTagDeriv (After p1 p2) = if nullable p1 then p2 else NotAllowed
endTagDeriv _ = NotAllowed

-- Optimizations

-- Computing nullable

-- The nullability of a pattern can be determined straightforwardly as the pattern is being constructed. Instead of computing nullable repeatedly, it should be computed once when the pattern is constructed and stored as a field in the pattern.

-- Interning patterns

-- Additional optimizations become possible if it is possible to efficiently determine whether two patterns are equal. We don't want to have to completely walk the structure of both patterns to determine equality. To make efficient comparison possible, we intern patterns in a hash table. Two interned patterns are equal if and only if they are the same object (i.e. == in Java terms). (This is similar to the way that Strings are interned to make Symbols which can be compared for equality using ==.) To make interning possible, there are two notions of identity defined on patterns each with a corresponding hash function:

-- interned identity is simply object identity (i.e. == or Object.equals in Java); for a hash function, we can use Object.hash in Java or the address of the object in C/C++
-- uninterned identity uses the type of the pattern, the interned identity of subpatterns, and the identity of any other parts of the pattern; similarly, the uninterned hash function calls the interned hash function on subpatterns
-- To intern patterns, we maintain a set of patterns implemented as a hash table. The hash table used uninterned identity and the corresponding uninterned hash function. When a new pattern is constructed, any subpatterns must first be interned. The pattern is interned by looking it up in the hash table. If it is found, we throw the new pattern away and instead return the existing entry in the hash table. If it is not found, we store the pattern in the hash table before returning it. (This is basically hash-consing.)

-- Avoiding exponential blowup

-- In order to avoid exponential blowup with some patterns, it is essential for the choice function to eliminate redundant choices. Define the choice-leaves of a pattern to be the concatenation of the choice-leaves of its operands if the the pattern is a Choice pattern and the empty-list otherwise. Eliminating redundant choices means ensuring that the list of choice-leaves of the constructed pattern contains no duplicates. One way to do this is to for choice to walk the choice-leaves of one operand building a hash-table of the set of choice-leaves of that operand; then walk the other operand using this hash-table to eliminate any choice-leaf that has occurred in the other operand.

-- Memoization

-- Memoization is an optimization technique that can be applied to any pure function that has no side-effects and whose return value depends only on the value of its arguments. The basic idea is to remember function calls. A table is maintained that maps lists of arguments values to previously computed return values for those arguments. When a function is called with a particular list of arguments, that list of arguments is looked up in the table. If an entry is found, then the previously computed value is returned immediately. Otherwise, the value is computed as usual and then stored in the table for future use.

-- The functions startTagOpenDeriv, startTagCloseDeriv and endTagDeriv defined above can be memoized efficiently.

-- Memoizing textDeriv is suboptimal because although the textDeriv takes the string value of the text node and the context as arguments, in many cases the result does not depends on these arguments. Instead we can distinguish two different cases for the content of an element. One case is that the content contains no elements (i.e. it's empty or consists of just a string). In this case, we can first simplify pattern using a textOnlyDeriv that replaces each Element pattern by NotAllowed. This can be efficiently memoized.

textOnlyDeriv :: Pattern -> Pattern
textOnlyDeriv (After p1 p2) =
  after (textOnlyDeriv p1) p2
textOnlyDeriv (Choice p1 p2) =
  choice (textOnlyDeriv p1) (textOnlyDeriv p2)
textOnlyDeriv (Group p1 p2) =
  group (textOnlyDeriv p1) (textOnlyDeriv p2)
textOnlyDeriv (Interleave p1 p2) =
  interleave (textOnlyDeriv p1) (textOnlyDeriv p2)
textOnlyDeriv (OneOrMore p) =
  oneOrMore (textOnlyDeriv p)
textOnlyDeriv (Element _ _) = NotAllowed
textOnlyDeriv p = p
-- In this case, textOnlyDeriv will always be followed by endTagDeriv, so we can fold the functionality of endTagDeriv into textOnlyDeriv.

-- In the other case, the content of the element contains one or more child elements. In this case, any text nodes can match only Text patterns (because of the restrictions in section 7.2 of the RELAX NG specification). The derivative of a Text pattern with respect to a text node does not depend on either the value of the text node or the context. We therefore introduce a mixedTextDeriv function, which can be efficiently memoized, for use in this case.

mixedTextDeriv :: Pattern -> Pattern
mixedTextDeriv (Choice p1 p2) =
  choice (mixedTextDeriv p1) (mixedTextDeriv p2)
mixedTextDeriv (Interleave p1 p2) =
  choice (interleave (mixedTextDeriv p1) p2)
         (interleave p1 (mixedTextDeriv p2))
mixedTextDeriv (After p1 p2) = after (mixedTextDeriv p1) p2
mixedTextDeriv (Group p1 p2) =
  let p = group (mixedTextDeriv p1) p2
  in if nullable p1 then choice p (mixedTextDeriv p2) else p
mixedTextDeriv (OneOrMore p) =
  group (mixedTextDeriv p) (choice (OneOrMore p) Empty)
mixedTextDeriv Text = Text
mixedTextDeriv _ = NotAllowed

-- Another important special case of textDeriv that can be memoized efficiently is when we can determine statically that a pattern is consistent with some datatype. More precisely, we can define a pattern p to be consistent with a datatype d if and only if for any two strings s1 s2, and any two contexts c1 c2, if datatypeEqual d s1 c1 s2 c2, then textDeriv c1 p s1 is the same as textDeriv c2 p s2. In this case, we can combine the string and context arguments into a single argument representing the value of the datatype that the string represents in the context; this can be much more efficiently memoized than the general case.

-- The attDeriv function can be memoized more efficiently by splitting it into two function. The first function is a startAttributeDeriv function that works like startTagOpenDeriv and depends just on the QName of the attribute. The second stage works in the same way to the case when the children of an element contain a single string.

-- Error handling

-- So far, the algorithms presented do nothing more than compute whether or not the node is valid with respect to the pattern. However, a user will not appreciate a tool that simply reports that the document is invalid, without giving any indication of where the problem occurs or what the problem is.

-- The most important thing is to detect invalidity as soon as possible. If an implementation can do this, then it can tell the user where the problem occurs and it can protect the application from seeing invalid data. If we consider the XML document to be a sequence of SAX-like events, then detecting the error as soon as possible, means that the implementation must detect when an initial sequence s of events is such that there is no valid sequence of events that starts with s.

-- This is straightforward with the algorithm above. Detecting the error as soon as possible is equivalent to detecting when the current pattern becomes NotAllowed. Note that this relies on the choice, interleave, group and after functions recognizing the algebraic identities involving NotAllowed. The current pattern immediately before it becomes NotAllowed describes what was expected and can be used to diagnose the error.

-- It some scenarios it may be sufficient to produce a single error message for an invalid document, and to cease validation as soon as it is determined that the document is invalid. In other scenarios, it may desirable to attempt to recover from the error and continute validation so as to find subsequent errors in the document. Jing recovers from validation errors as follows:

-- If startTagOpenDeriv causes an error, then Jing first tries to recover on the assumption that some required elements have been omitted. In effect, it transforms the pattern by making the first operand of each Group optional and then retries startTagOpenDeriv. If this still causes an error, then the purposes of validating following siblings, it ignores the element. For the purpose of validating the element itself, it searches the whole schema for element patterns with a name class that contains the name of the start-tag open. If it finds one or more such element patterns, then it uses a choice of the content of all element patterns that have a name-class that contains the name of the start-tag open with maximum specificity. A name-class that contains the name by virtue of a name element is considered more specific than one that contains the name by virtue of a nsName or anyName element; similarly, a name-class that contains the name by virtue of a nsName element is considered more specific than one that contains the name by virtue of a anyName element. If there is no such element pattern, then it validates only any maximal subtrees rooted in an element for which the schema does contain an element pattern. Anything outside the maximal subtrees is ignored.
-- If startAttributeDeriv causes an error, then it recovers by ignoring the attribute.
-- If startTagCloseDeriv causes an error, it recovers by replacing all attribute patterns by empty.
-- If textDeriv (used only for an attribute value or for an element that contains no child elements) causes an error, then it recovers by replacing the first operands of all top-level After patterns (i.e. After patterns not inside another After pattern) by empty.
-- If mixedTextDeriv causes an error, it recovers by ignoring the text node.
-- If endTagDeriv causes an error, it recovers by using a choice of the second operands of all top-level After patterns.
-- References

-- Dongwon Lee, Murali Mani, Makoto Murata. Reasoning about XML Schema Languages using Formal Language Theory. 2000. See http://citeseer.nj.nec.com/lee00reasoning.html.

-- Janusz A. Brzozowski. Derivatives of Regular Expressions. Journal of the ACM, Volume 11, Issue 4, 1964.

-- Mark Hopkins. Regular Expression Package. Posted to comp.compilers, 1994. Available from ftp://iecc.com/pub/file/regex.tar.gz.