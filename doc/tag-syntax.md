= Syntax

Syntax inspiration:
- [LMNL](http://lmnl-markup.org/specs/archive/LMNL_syntax.xhtml)
- [TexMECS](http://xml.coverpages.org/MLCD-texmecs20010510.html)
- [FtanML](https://www.balisage.net/Proceedings/vol10/html/Kay01/BalisageVol10-Kay01.html)
- [JSON-LD](https://json-ld.org/)
- [RelaxNG Compact](http://relaxng.org/compact-tutorial-20030326.html)
- [Turtle (RDF compact syntax)](https://www.w3.org/TeamSubmission/turtle/)


Overlap: ingebakken in LMNL && MECS
De andere talen

LMNL heeft recursie in de range annotations: deze kunnen zelf ook weer tekst met ranges bevatten.

TexMECS heeft een compactere manier om discontinuity aan te geven dan LMNL

escaping - \ ( binnen strings)

datatype voor attributes

Turtle/Python - quotes gebruiken voor string content. Triple quotes geven aan dat
newlines behouden blijven.

[discontinued>bla<discontinued-][a>aaa<a][+discontinued><discontinued]

2 syntaxen: voor de TAG serialisatie, en voor de TAG schema's.


Aandachtspunten:  hiërarchie, overlap, discontinuïteit, whitespace handling.

TAG syntax: baseer op lmnl syntax, met de TexMECS manier om discontinuïteit aan te geven:
[sp}bla, {-sp][i}she said{i][+sp}bla bla bla.{sp]


Hiërarchie, overlap en whitespace significantie kan aangegeven worden inde TAG schema.

[Creole](http://www.princexml.com/howcome/2007/xtech/papers/output/0077-30/index.xhtml) als basis voor TAGschema.
Semantics in TAG schema?


Criteria:

TAG syntax:
- clear markers for beginning and end of markup range
- a way to indicate when a markup range is discontinued. (like in TexMECS)
- annotations inside markup open/close tags.
- annotations can contain marked-up tekst. (like in LMNL)
- text content in unicode
- markup/annotation tags have names
- markup tags should be distinct from annotation tags (unlike in LMNL)
- whitespace handling: no implicit line definition by interpreting newlines in text: newlines are for formatting of the syntax only. Use markup tags to explicitly define the lines.

TAG schema syntax:
- define hierarchies of markup tags (which markup contains how many of which other markup)
- define content type of annotations
- schema syntax may be hierarchical
- both a markup schema and a semantic schema: start with semantic schema, deduce markup schema
- markup schema should cover all the markup, so semantic schema should also cover all the markup
- with the semantic schema we should be able to convert the document to rdf data
