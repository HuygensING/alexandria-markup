# LQL (LMNL query language)

## Questions/Requirements

- What should we be able to query? query ranges, textnodes, other?

- what should the query return? text, numbers, kwic?

- Do we treat all ranges as separate, or also as part of a known hierarchy?

- Should we avoid the use of 'range', use 'text set' or 'text node set' instead?

- How should we identifying/addressing ranges/textnodesets:
  - by name : `range('excerpt')`
  - by name + id : `range('excerpt','e-1')`
  - by name + index : `range('excerpt')[0]`
  - with/without 'range()' :  `excerpt` , `excerpt[0]`, `excerpt(e-1)`

- How should we address annotations:
  - _range_identifier_:_annotation_idenifier_

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

---

TODO: regular expressions
Use regular expressions to find specific textnodes

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

B `select annotationvalue('n') from ranges where name='page' and text.contains('Volney');`

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

B `select distinct(annotationvalue('who')) from ranges where name='said';`

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

B `select text from ranges where name='said' and annotationvalue('who')=$who;`

---

> Count the ranges overlapping said ranges, excluding page ranges:
```
let $novel  := db:open('LMNL-library','Frankenstein.xlmnl')/*
return count(
  lm:ranges('said',$novel)/lm:overlapping-ranges(.)[not(lm:named('page',.))] )
```
> We get 4. (Another query shows they are all p ranges.)

A `novel.ranges('said').overlapping-ranges().filter(not().hasName('page')).count()`

B `select count(*) from ranges where name!='page' and overlap in (select * from ranges where name='said');`

---

Other example queries

> get the text of the first line

```
novel.range('l')[0].text()
```

```
select text from range('1')[0];

```
