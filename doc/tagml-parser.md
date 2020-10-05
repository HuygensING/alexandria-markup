Omdat TAGML overlapping hierarchies heeft, en dus (mogelijk) niet-hierarchisch is, is het niet mogelijk om de grammar van TAGML helemaal te beschrijven in ANTLR4
De AST die de parser genereert is een AST van TAGML-0, een versie die alle elementen van TAGML herkent, maar geen well-formedness kan garanderen.
Dit moet dan in de post-processing gebeuren.

TAGML is well-formed als:
- het geldige TAGML-0 is.
- iedere open tag een corresponderende opvolgende close tag heeft.
- iedere suspended tag een corresponderende opvolgende resume tag heeft.
- tussen suspend tag en corresponderende resume tag meer dan allen whitespace staat
- ieder geopende tekstdivergentie '<|' minstens één corresponderende opvolgende divider '|' en een corresponderende opvolgende sluiter '|>' heeft.
- de markup in de branches van textdivergentie self-contained zijn: markup die geopend is vóór de textdivergentie mag niet gesloten worden in één of meerdere van de textvariatie branches, en alle markup die geopend wordt in een branch moet binnen die branch gesloten worden. Dit geldt ook voor suspend/resume.
- de root markup tag geen milestone tag is.
- bij een list annotatie de elementen allemaal van hetzelfde type zijn, gescheiden door een komma.
- er geen tekst of markup meer voorkomt na het sluiten van de root markup.
- alle markup binnen een layer hierarchisch is, en dus niet overlapt
- een nieuwe layer eerst toegevoegd is (+) voordat het gebruikt wordt.
- een namespace gedefinieerd is voordat het gebruikt wordt.
- de layerid mag alleen weggelaten worden van de close tag als het niet ambigu is bij welke open tag het hoort.
- er geen non-whitespace text vóór de open tag van de root markup voorkomt.
- de root markup niet gesuspend wordt

ANTLR4 genereert een lexer (tokenizer): TAGMLLexer en een parser: TAGMLParser.
Met de lexer maak je een CommonTokenStream, waarmee je de TAGMLParser initialiseert.
Deze genereert dan een ParseTree, dit is de AST. Deze ParseTree kun je aflopen met een Listener of een Visitor. In de huidige code wordt de Listener TAGMLListener gebruikt. Deze extends de door ANTLR gegenereerde TAGMLParserBaseListener en implementeert de voorgedefinieerde enter- en exit calls op de tokens zoals gedefinieerd door de parser grammar. Deze calls krijgen een tokencontext mee van de parser.
De Listener houdt per layer bij welke markup open staat, en welke markup suspended is.
Aan het einde van het TAGML document wordt hiermee gecontroleerd of er nog markup open of suspended is.
De Listener krijgt bij het initialiseren een TAGStore mee waarin de MCT opgebouwd en opgeslagen wordt, en een ErrorListener die de foutmeldingen opslaat.

In enterStartTag() wordt aan de hand van de meegegeven StartTagContext een TAGMarkup aangemaakt, en in de huidige State wordt deze markup op de openMarkup stack gezet van de relevante layer. De TAGMarkup wordt in de MCT gekoppeld aan de laatst geopende markup uit dezelfde layer(s)

In exitEndTag() wordt gecontroleerd of deze endtag verwacht is volgens de well-formedness regels.

In exitText() wordt een TAGTextNode aangemaakt met de text uit de TextContext, en deze TAGTextNode wordt in de MCT verbonden met de laatst geopende markup per layer.
