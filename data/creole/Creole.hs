module Creole where
-- because we're re-using all as a function name
-- import qualified Prelude.all as Prelude.all

-- Basics

type Uri = String
type LocalName = String
type Id = String

-- A Context represents the context of an XML element. It consists of a base URI and a mapping from prefixes to namespace URIs.
type Prefix = String
type Context = (Uri, [(Prefix, Uri)])

-- Qualified names are represented by the QName datatype, which consists of a URI and a local name:
data QName = QName Uri LocalName

-- We need to define the different kinds of name classes that are allowed (these are the same as those allowed in Relax NG):
data NameClass = AnyName
                 | AnyNameExcept NameClass
                 | Name Uri LocalName
                 | NsName Uri
                 | NsNameExcept Uri NameClass
                 | NameClassChoice NameClass NameClass

-- We also need to define the different kinds of patterns that are supported by Creole. This is an different set from those allowed in Relax NG: ignoring those to do with annotations/attributes, atoms and datatypes, Concur, Partition, ConcurOneOrMore, Range, EndRange and All are new, and Element is no longer present.
data Pattern = Empty
               | NotAllowed
               | Text
               | Choice Pattern Pattern
               | Interleave Pattern Pattern
               | Group Pattern Pattern
               | Concur Pattern Pattern
               | Partition Pattern
               | OneOrMore Pattern
               | ConcurOneOrMore Pattern
               | Range NameClass Pattern
               | EndRange QName Id
               | After Pattern Pattern
               | All Pattern Pattern
-- The patterns EndRange, After and All are constructs that are used in the algorithm but don't correspond to patterns you can write in a Creole grammar.

-- The EndRange pattern matches the end tag of a range with the given qualified name and ID. The ID is used for matching self-overlapping ranges.

-- The After pattern is used when creating partitions; in the algorithm for Relax NG it guarantees that interleaved elements can't appear within each other. It's a bit like Group, in that the first sub-pattern of After must be completed before the second can be matched, but it's handled differently when patterns are composed, so that the After pattern "bubbles up" through groups.

-- The All pattern means that both sub-patterns must be matched by the events1.

-- For validating against Creole, a document is considered to be a sequence of events; the only ones we're going to look at here are start tag events, end tag events and text events. Note that, unlike in normal XML, start and end tags can have IDs, which allows us to match the tags of self-overlapping ranges.
data Event = StartTagEvent QName Id
             | EndTagEvent QName Id
             | TextEvent String Context

-- Utilities

-- The following utility functions are used in the algorithm.

-- The most important utility function is nullable, which tests whether a given pattern can match an empty sequence of events. Nullable is defined as follows for the various kinds of patterns:
nullable:: Pattern -> Bool
nullable Empty = True
nullable NotAllowed = False
nullable Text = True
nullable (Choice p1 p2) = nullable p1 || nullable p2
nullable (Interleave p1 p2) = nullable p1 && nullable p2
nullable (Group p1 p2) = nullable p1 && nullable p2
nullable (Concur p1 p2) = nullable p1 && nullable p2
nullable (Partition p) = nullable p
nullable (OneOrMore p) = nullable p
nullable (ConcurOneOrMore p) = nullable p
nullable (Range _ _) = False
nullable (EndRange _ _) = False
nullable (After _ _) = False
nullable (All p1 p2) = nullable p1 && nullable p2

-- The second utility function is allowsText, which returns true if the pattern can match text. This is important because whitespace-only text events are ignored if text isn't allowed by a pattern.
allowsText:: Pattern -> Bool
allowsText (Choice p1 p2) = allowsText p1 || allowsText p2
allowsText (Group p1 p2) = 
  if nullable p1 then (allowsText p1 || allowsText p2) 
                 else allowsText p1
allowsText (Interleave p1 p2) = 
  allowsText p1 || allowsText p2
allowsText (Concur p1 p2) = allowsText p1 && allowsText p2
allowsText (Partition p) = allowsText p
allowsText (OneOrMore p) = allowsText p
allowsText (ConcurOneOrMore p) = allowsText p
allowsText (After p1 p2) = 
  if nullable p1 then (allowsText p1 || allowsText p2) 
                 else allowsText p1
allowsText (All p1 p2) = allowsText p1 && allowsText p2
allowsText Text = True
allowsText _ = False

-- Finally, like Relax NG, Creole needs a method of testing whether a given qualified name matches a given name class:
contains :: NameClass -> QName -> Bool
contains AnyName _ = True
contains (AnyNameExcept nc) n = not (contains nc n)
contains (NsName ns1) (QName ns2 _) = (ns1 == ns2)
contains (NsNameExcept ns1 nc) (QName ns2 ln) =
  ns1 == ns2 && not (contains nc (QName ns2 ln))
contains (Name ns1 ln1) (QName ns2 ln2) = 
  (ns1 == ns2) && (ln1 == ln2)
contains (NameClassChoice nc1 nc2) n = 
  (contains nc1 n) || (contains nc2 n)

-- Constructors

-- When we create a derivative, we often need to create a new pattern. These constructors take into account special handling of NotAllowed, Empty and After patterns.
choice :: Pattern -> Pattern -> Pattern
choice p NotAllowed = p
choice NotAllowed p = p
choice Empty Empty = Empty
choice p1 p2 = Choice p1 p2

group :: Pattern -> Pattern -> Pattern
group p NotAllowed = NotAllowed
group NotAllowed p = NotAllowed
group p Empty = p
group Empty p = p
group (After p1 p2) p3 = after p1 (group p2 p3)
group p1 (After p2 p3) = after p2 (group p1 p3)
group p1 p2 = Group p1 p2

interleave :: Pattern -> Pattern -> Pattern
interleave p NotAllowed = NotAllowed
interleave NotAllowed p = NotAllowed
interleave p Empty = p
interleave Empty p = p
interleave (After p1 p2) p3 = after p1 (interleave p2 p3)
interleave p1 (After p2 p3) = after p2 (interleave p1 p3)
interleave p1 p2 = Interleave p1 p2

concur :: Pattern -> Pattern -> Pattern
concur p NotAllowed = NotAllowed
concur NotAllowed p = NotAllowed
concur p Text = p
concur Text p = p
concur (After p1 p2) (After p3 p4) = 
  after (all2 p1 p3) (concur p2 p4)
concur (After p1 p2) p3 = after p1 (concur p2 p3)
concur p1 (After p2 p3) = after p2 (concur p1 p3)
concur p1 p2 = Concur p1 p2

partition :: Pattern -> Pattern
partition NotAllowed = NotAllowed
partition Empty = Empty
partition p = Partition p

oneOrMore :: Pattern -> Pattern
oneOrMore NotAllowed = NotAllowed
oneOrMore Empty = Empty
oneOrMore p = OneOrMore p

concurOneOrMore :: Pattern -> Pattern
concurOneOrMore NotAllowed = NotAllowed
concurOneOrMore Empty = Empty
concurOneOrMore p = ConcurOneOrMore p

after :: Pattern -> Pattern -> Pattern
after p NotAllowed = NotAllowed
after NotAllowed p = NotAllowed
after Empty p = p
after (After p1 p2) p3 = after p1 (after p2 p3)
after p1 p2 = After p1 p2

all2 :: Pattern -> Pattern -> Pattern
all2 p NotAllowed = NotAllowed
all2 NotAllowed p = NotAllowed
all2 p Empty = if nullable p then Empty else NotAllowed
all2 Empty p = if nullable p then Empty else NotAllowed
all2 (After p1 p2) (After p3 p4) =
  after (all2 p1 p3) (all2 p2 p4)
all2 p1 p2 = All p1 p2

-- Derivatives

-- Finally, we can look at the calculation of derivatives. A document is a sequence of events. The derivative for a sequence of events against a pattern is the derivative of the remaining events against the derivative of the first event.
eventsDeriv :: Pattern -> [Event] -> Pattern
eventsDeriv p [] = p
eventsDeriv p (h:t) = eventsDeriv (eventDeriv p h) t

-- The derivative for an event depends on the kind of event, and we use different functions for each kind. Whitespace-only text nodes can be ignored if the pattern doesn't allow text.
eventDeriv :: Pattern -> Event -> Pattern
eventDeriv p (TextEvent s cx) = 
  if (whitespace s && not allowsText p) 
  then p 
  else (textDeriv cx p s)
eventDeriv p (StartTagEvent qn id) = startTagDeriv p qn id
eventDeriv p (EndTagEvent qn id) = endTagDeriv p qn id

-- Text Derivatives

-- textDeriv computes the derivative of a pattern with respect to a text event.

textDeriv :: Context -> Pattern -> String -> Pattern

-- For Choice, Group, Interleave and the other standard Relax NG patterns, the derivative is just the same as in Relax NG:
textDeriv cx (Choice p1 p2) s =
  choice (textDeriv cx p1 s) (textDeriv cx p2 s)
textDeriv cx (Interleave p1 p2) s =
  choice (interleave (textDeriv cx p1 s) p2)
         (interleave p1 (textDeriv cx p2 s))
textDeriv cx (Group p1 p2) s =
  let p = group (textDeriv cx p1 s) p2
  in if nullable p1 then choice p (textDeriv cx p2 s) 
                    else p
textDeriv cx (After p1 p2) s = 
  after (textDeriv cx p1 s) p2
textDeriv cx (OneOrMore p) s =
  group (textDeriv cx p s) (choice (OneOrMore p) Empty)
textDeriv cx Text _ = Text

-- For Concur, text is only allowed if it is allowed by both of the sub-patterns: we create a new Concur whose sub-patterns are the derivatives of the original sub-patterns.
textDeriv cx (Concur p1 p2) s =
  concur (textDeriv cx p1 s)
         (textDeriv cx p2 s)

-- For ConcurOneOrMore, we partially expand the ConcurOneOrMore into a Concur. This mirrors the derivative for OneOrMore, except that a new Concur pattern is constructed rather than a Group, and the second sub-pattern is a choice between a ConcurOneOrMore and Text.
textDeriv cx (ConcurOneOrMore p) s =
  concur (textDeriv cx p s) 
         (choice (ConcurOneOrMore p) Text)

-- For Partition, we create an After pattern that contains the derivative.
textDeriv cx (Partition p) s =
  after (textDeriv cx p s) Empty

-- No other patterns can match a text event; the default is specified as
textDeriv _ _ _ = NotAllowed

-- Start-tag Derivatives

-- Start tags are handled in a very generic way by all the patterns, except the Range pattern, whose derivative is a group of the content pattern for the range followed by an EndRange pattern for the range. Note that the EndRange pattern is created with the same qualified name and ID as the matched range.

startTagDeriv :: Pattern -> QName -> Id -> Pattern
startTagDeriv (Range nc p) qn id =
  if contains nc qn then group p (EndRange qn id)
                    else NotAllowed
startTagDeriv (Choice p1 p2) qn id =
  choice (startTagDeriv p1 qn id)
         (startTagDeriv p2 qn id)
startTagDeriv (Group p1 p2) qn id =
  let d = group (startTagDeriv p1 qn id) p2
  in if nullable p1 then choice d (startTagDeriv p2 qn id) 
                    else d
startTagDeriv (Interleave p1 p2) qn id =
  choice (interleave (startTagDeriv p1 qn id) p2)
         (interleave p1 (startTagDeriv p2 qn id))
startTagDeriv (Concur p1 p2) qn id =
  let d1 = startTagDeriv p1 qn id
      d2 = startTagDeriv p2 qn id
  in choice (choice (concur d1 p2) (concur p1 d2))
            (concur d1 d2)
startTagDeriv (Partition p) qn id =
  after (startTagDeriv p qn id) Empty
startTagDeriv (OneOrMore p) qn id =
  group (startTagDeriv p qn id)
        (choice (OneOrMore p) Empty)              
startTagDeriv (ConcurOneOrMore p) qn id =
  concur (startTagDeriv p qn id)
         (choice (ConcurOneOrMore p) anyContent)              
startTagDeriv (After p1 p2) qn id =
  after (startTagDeriv p1 qn id) p2
startTagDeriv _ _ _ = NotAllowed

-- End Tags

-- End tags are matched by EndRange patterns. An id is used to support self-overlap: when an EndTagEvent matches an EndRange pattern, the names have to match and so do the ids.
endTagDeriv :: Pattern -> QName -> Id -> Pattern
endTagDeriv (EndRange (QName ns1 ln1) id1) 
            (QName ns2 ln2) id2 =
  if id1 == id2 || (id1 == "" && id2 == "" && ns1 == ns2 && ln1 == ln2)
  then Empty
  else NotAllowed
endTagDeriv (Choice p1 p2) qn id =
  choice (endTagDeriv p1 qn id)
         (endTagDeriv p2 qn id)
endTagDeriv (Group p1 p2) qn id =
  let p = group (endTagDeriv p1 qn id) p2
  in if nullable p1 then choice p (endTagDeriv p2 qn id) 
                    else p
endTagDeriv (Interleave p1 p2) qn id =
  choice (interleave (endTagDeriv p1 qn id) p2)
         (interleave p1 (endTagDeriv p2 qn id))
endTagDeriv (Concur p1 p2) qn id =
  let d1 = endTagDeriv p1 qn id
      d2 = endTagDeriv p2 qn id
  in choice (choice (concur d1 p2) (concur p1 d2))
            (concur d1 d2)
endTagDeriv (Partition p) qn id =
  after (endTagDeriv p qn id) Empty
endTagDeriv (OneOrMore p) qn id =
  group (endTagDeriv p qn id)
        (choice (OneOrMore p) Empty)
endTagDeriv (ConcurOneOrMore p) qn id =
  concur (endTagDeriv p qn id)
         (choice (ConcurOneOrMore p) anyContent)
endTagDeriv (After p1 p2) qn id =
  after (endTagDeriv p1 qn id) p2
endTagDeriv _ _ _ = NotAllowed

-- whitespace tests whether a string is contains only whitespace.
whitespace :: String -> Bool
whitespace s = all isSpace s


