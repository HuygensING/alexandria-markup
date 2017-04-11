# LQL (LMNL query language)


## Questions/Requirements

- Should we avoid the use of 'range', use 'text set' or 'text node set' instead?
>  use _markup_ instead.

- What should we be able to query? query ~~ranges~~ markup, text ~~nodes~~, other?

- what should the query return? text, numbers, kwic?

- Do we treat all ~~ranges~~ markup as separate, or also as part of a known hierarchy?

- How should we identify/address ~~ranges/textnodesets~~ markup:
  - by name : `markup('excerpt')`
  - by name + id : `markup('excerpt','e-1')`
  - by name + index : `markup('excerpt')[0]`
  - with/without 'range()' :  `excerpt` , `excerpt[0]`, `excerpt(e-1)`

- How should we address annotations:
  - _markup_identifier_:_annotation_identifier_
  
- How to indicate the limen we want to query?

- Do we need to be able to query multiple limen at the same time?

-

----
[range relationships](http://lmnl-markup.org/specs/archive/Range_relationships.xhtml):
(introducing discontinuous ranges changes the types of relationship)

- congruent ranges = identical text sets (but: text offset matters)
- subsets
- superset
- union
- intersection
- complement


---

Relevant functions from [PostGIS](http://postgis.net/docs/manual-2.3/reference.html):

PostGIS is meant for querying over PostGIS Geometry types (2d/3d points, bounding boxes, etc.)
We may be able to use some of the functions of this language for our LQL.

(description changed to reflect the use in LQL)

#### Operators: (A & B are ranges)
* && — Returns TRUE if A overlaps B.
* &< — Returns TRUE if A overlaps or is to the left of B.
* &> — Returns TRUE if A overlaps or is to the right of B.
* << — Returns TRUE if A is strictly to the left of B.
* \>> — Returns TRUE if A is strictly to the right of B.
* = — Returns TRUE if A is congruent with B.
* @ — Returns TRUE if A is contained by / is a subset of B.
* ~ — Returns TRUE if A is superset of B.


#### Spatial Relationships and Measurements
* ST_Contains — Returns true iif B is completely within A
* ST_Touches — Returns TRUE if one of the outermost textnodes of A is directly adjacent to one of the outermost textnodes of B
* ST_Within — Returns true if A is completely within B
* ST_Disjoint — Returns TRUE iif there is no overlap between the ranges
* ST_Equals — Returns true iif the ranges are congruent
* ST_Length — Returns the total character length of all the textnodes in the range
* ST_Overlaps — Returns TRUE iif there is an overlap between A & B

#### Geometry Processing
* ST_Difference — Returns the textnodes in A that are not in B
* ST_Intersection — Returns the textnodes in A that are also in B
* ST_SymDifference — Returns the the textnodes in A that are not in B + the textnodes in B that are not in A
* ST_Union — Returns set union of A and B.

#### Dealing with discontinued ranges
* isDiscontinued - true iif not all textnodes in the range are consecutive
With discontinued ranges, you would want a function to get the textnodes in between the leftmost and rightmost textnodes of the range that are not part of the range.

----

#### sample queries from [Luminescent](https://www.balisage.net/Proceedings/vol13/print/Piez01/BalisageVol13-Piez01.html#LuminescentQueries)

with suggested LQL alternatives A & B

> return the number of the page (in the 1831 edition) on which Volney is mentioned. (Functions named with the lm prefix are defined by Luminescent.)
```
let $novel  := db:open('LMNL-library','Frankenstein.xlmnl')/*
return lm:ranges('page',$novel)[contains(lm:range-value(.),'Volney')]
  /lm:annotations('n',.)/lm:annotation-value(.)\\
```

> This returns 102. (If Volney were mentioned more than once, more than one page number would be returned.)

A `novel.ranges('page').filter(text().contains('Volney')).annotations('n').value()`

B `select annotationvalue('n') from markup where name='page' and text.contains('Volney');`

---

> Here is a query for distinct values of annotations indicating speakers (who annotations on said ranges):
```
let $novel  := db:open('LMNL-library','Frankenstein.xlmnl')/*
return distinct-values(
  lm:ranges('said',$novel)/
  lm:annotations('who',.)/lm:annotation-value(.) )
```
> 26 strings are returned, including the Creature.

A `novel.ranges('said').annotations('who').value().distinct()`

B `select distinct(annotationvalue('who')) from markup where name='said';`

---

> Here is a query that returns all the speeches attributed to the Creature (or substitute any character):
```
let $novel  := db:open('LMNL-library','Frankenstein.xlmnl')/*
let $who    := 'The creature'
return lm:ranges('said',$novel)[lm:annotations('who',.) = $who]
  /lm:range-value-ws-trim(.)
```
> 48 speeches are returned.

A `novel.ranges('said').filter(annotationvalue('who').eq($who)).text()`

B `select text from markup where name='said' and annotationvalue('who')=$who;`

---

> Count the ranges overlapping said ranges, excluding page ranges:
```
let $novel  := db:open('LMNL-library','Frankenstein.xlmnl')/*
return count(
  lm:ranges('said',$novel)/lm:overlapping-ranges(.)[not(lm:named('page',.))] )
```
> We get 4. (Another query shows they are all p ranges.)

A `novel.ranges('said').overlapping-ranges().filter(not().hasName('page')).count()`

B `select count(*) from markup where name!='page' and overlap in (select * from markup where name='said');`

---

Other example queries

> get the text of the first line

A `novel.range('l')[0].text()`

B `select text from markup('1')[0];`

---

Use regular expressions to find specific textnodes

B `select text from markup where name = 'phr' and text =~ ".\*[Mm]ark[uU]p.\*"

---
Given a certain text, return the markup containing (parts of) this text.


## Example text + queries

### text 1
````
[excerpt}[p}
Alice was beginning to get very tired of sitting by her sister on the bank,
and of having nothing to do: once or twice she had peeped into the book her sister
was reading, but it had no pictures or conversations in it, 
[q=a}and what is the use of a book,{q=a]
thought Alice
[q=a}without pictures or conversation?{q=a]
{p]{excerpt]
````

> Get the quoted text by Alice

`select m.text from markup m where m.name='q' and m.id='a'`

*returns:*
````
"and what is the use of a book,without pictures or conversation?"
````

---

### text 2
````
[excerpt [source [book}1 Kings{book] [chapter}12{chapter]]}
[verse}And he said unto them, [q}What counsel give ye that we may answer this people, who have spoken to me, saying, [q}Make the yoke which thy father did put upon us lighter?{q]{q]{verse]
[verse}And the young men that were grown up with him spake unto him, saying, [q}Thus shalt thou speak unto this people that spake unto thee, saying, [q=i}Thy father made our yoke heavy, but make thou it lighter unto us;{q=i] thus shalt thou say unto them, [q=j}My little finger shall be thicker than my father's loins.{verse]
[verse}And now whereas my father did lade you with a heavy yoke, I will add to your yoke: my father hath chastised you with whips, but I will chastise you with scorpions.{q=j]{q]{verse]
[verse}So Jeroboam and all the people came to Rehoboam the third day, as the king had appointed, saying, [q}Come to me again the third day.{q]{verse]
{excerpt]
````

> Get the chapter value

`select m.annotationvalue('source:chapter') from markup m where m.name='excerpt'`

*returns:* 12
 
 (source:chapter) indicates the chapter annotation nested within the source annotation
 
> Get the quoted texts that are inside of other quoted texts
 
`select m.text from markup m where m.name='q' and m in (select q from markup q where q.name='q')

*returns:* 
````
"Make the yoke which thy father did put upon us lighter?",
"Thy father made our yoke heavy, but make thou it lighter unto us;",
"My little finger shall be thicker than my father's loins.\nAnd now whereas my father did lade you with a heavy yoke, I will add to your yoke: my father hath chastised you with whips, but I will chastise you with scorpions."
````
   