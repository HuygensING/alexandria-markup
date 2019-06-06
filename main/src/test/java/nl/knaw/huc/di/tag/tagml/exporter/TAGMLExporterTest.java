package nl.knaw.huc.di.tag.tagml.exporter;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import nl.knaw.huc.di.tag.TAGBaseStoreTest;
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huc.di.tag.tagml.importer.TAGModelBuilderImpl;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGDocumentDAO;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class TAGMLExporterTest extends TAGBaseStoreTest {

  @Test
  public void testRD207() {
    String tagML = "[tagml>" +
        "[layerdef|+A>" +
        "[p>" +
        "[l>A line in the [m|A>sand<m|A].<l]" +
        "<p]" +
        "<layerdef|A]" +
        "<tagml]";
    assertTAGMLOutIsIn(tagML);
  }

  @Test
  public void testNonLinearityWith3Branches() {
    String tagML = "[tagml>[layerdef|+A,+B>[l|A>Et voilà que de la <|sombre|jolie|miserable|> [x|B>surface<x|B] d'un étang s'élève un cygne<l|A]<layerdef|A,B]<tagml]";
    assertTAGMLOutIsIn(tagML);
  }

  @Ignore
  @Test
  public void testBrulez() {
    String tagmlIn = "[doc source=\"typescript-tg_lhhs_107\">\n" +
        "\t[defineLayers|+sem,+mat,+gen>\n" +
        "\t[page|mat>\n" +
        "\t\t[titleSection>\n" +
        "\t\t\t[l|mat>[u>[title|sem>LA LANTERNE DE PROJECTION \"A L A D I N\"<title]<u]<l]\n" +
        "\t\t\t[l|mat>par<l]\n" +
        "\t\t\t[l|mat>[u>[name|sem>Raymond Brulez<name]<u]<l]\n" +
        "\t\t<titleSection]\n" +
        "\t\t[bodySection>\n" +
        "\t\t\t[p>\n" +
        "\t\t\t\t[l|mat>Une belle main de femme, élégante et fine malgr<|[del|gen>á<del]|[add|gen>é<add]|> l'agrandissement<l]\n" +
        "\t\t\t\t[l|mat>du close-up, manipule du browning-bi[del|gen rend=\"slash_crossed\" type=\"typo\">l<del]jou. Les doigts bien manucurés<l]\n" +
        "\t\t\t\t[l|mat>caressent, irrésolus, les arabesques de la crosse d'ivoire. C'est<l]\n" +
        "\t\t\t\t[l|mat>l'image des dernie[subst|gen rend=\"add-del_crossed\" type=\"typo\">[del|gen>u<del][add|gen type=\"typo\">r<add]<subst]s soubresauts [del|gen rend=\"x_crossed\" type=\"ling\">pshy<del]psychologiques de l'heroïne avant<l]\n" +
        "\t\t\t\t[l|mat>sont acte de désespoir : sublime rébellion de la virginale [persName|sem>G[subst|gen rend=\"add-del_crossed\" type=\"typo\">[del|gen>e<del][add|gen>r<add]<subst]eta Brenn<persName]<l]\n" +
        "\t\t\t\t[l|mat>refusant de se laisser vendre par des parents indignes à un banquier<l]\n" +
        "\t\t\t\t[l|mat>riche, mais [del|gen rend=\"x_crossed\" type=\"?\">fi<del] difforme et débauché. L'index se courbe sur la cédille<l]\n" +
        "\t\t\t\t[l|mat>métallique de la détente... tire...<l] \n" +
        "\t\t\t<p]\n" +
        "\n" +
        "\t\t\t[p>\n" +
        "\t\t\t\t[l|mat>Et voilà que de la sombre surface d'un étang s'élève un cygne<l]\n" +
        "\t\t\t\t[l|mat>majestueux en un ébouissant battement d'ailes : ce qui n'est pas<l]\n" +
        "\t\t\t\t[l|mat>l'image allégorique de cette âme se dégageant des liens terrestre[subst|gen rend=\"add-del_crossed\" type=\"typo\">[del|gen>l<del][add|gen type=\"typo\">s<add]<subst]<l]\n" +
        "\t\t\t\t[l|mat>vers de calmes empyrées, mais simplement la signature du film et la<l]\n" +
        "\t\t\t\t[l|mat>marque commerciale de la \"[orgName|sem index=false>Swann Vitascope Distributing Co. Ltd.<orgName]\"<l]\n" +
        "\t\t\t<p]\n" +
        "\n" +
        "\t\t\t[p>\n" +
        "\t\t\t\t[l|mat>[quote>Comment trouvez-vous cette nouve[subst|gen rend=\"add-del_crossed\" type=\"typo\">[del|gen>e<del][add|gen type=\"typo\">lle<add]<subst] [del|gen rend=\"slash_crossed\" type=\"typo\">e<del]invention, ce film en re[wordDivision|mat>-<wordDivision]<l]\n" +
        "\t\t\t\t[l|mat>lief ?<quote] demandait [persName|sem index=true>Van Truggel<persName], le directeur du cinéma \"[placeName|sem index=true>Apollon<placeName]\" à<l]\n" +
        "\t\t\t\t[l|mat>l'opérateur qui enlevait les bobines.<l]\n" +
        "\t\t\t<p]\n" +
        "\t\t<bodySection]\n" +
        "\t<page]\n" +
        "\t<defineLayers]\n" +
        "<doc]\n";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testSimpleExampleWithMarkupAndTwoLayers() {
    String tagmlIn = "[x|+P,+T>[a|T>Donald [b|P>likes<a|T] Vladimir<b|P]<x|P,T]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testEscapedCharacters() {
    String tagmlIn = "[line>\\[, \\< and \\\\ need to be escaped.<line]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testMarkedUpText() {
    String tagmlIn = "[line>The rain in Spain falls mainly on the plain.<line]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testLinearAnnotatedText() {
    String tagmlIn = "[a>I've got a [b>bad<b] feeling about [c>this<c].<a]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testNonLinearAnnotatedText() {
    String tagmlIn = "[a>I've got a <|[sic>very [b>bad<b]<sic]|[corr>exceptionally good<corr]|> feeling about [c>this<c].<a]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testNestedNonLinearity() {
    String tagmlIn = "[l>This is <|" +
        "[del>great stuff!<del]" +
        "|" +
        "[add>questionable <|[del>text<del]|[add>code<add]|><add]" +
        "|><l]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testDoublyNestedNonLinearity() {
    String tagmlIn = "[l>This is <|" +
        "[del>great stuff!<del]" +
        "|" +
        "[add>questionable <|" +
        "[del>text<del]" +
        "|" +
        "[add>but readable <|" +
        "[del>cdoe<del]|[add>code<add]" +
        "|><add]" +
        "|><add]" +
        "|><l]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testOptionalTags() {
    String tagmlIn = "[l>This [?w>word<?w] is optional.<l]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testDiscontinuity() {
    String tagmlIn = "[x>[q>and what is the use of a book,<-q] thought Alice[+q>without pictures or conversation?<q]<x]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Ignore
  @Test
  public void testCombiningOverlapAndNonLinearity1() {
    String tagmlIn = "[text>It is a truth universally acknowledged that every " +
        "<|[add>young [b>woman<b]<add]" +
        "|[b>[del>rich<del]|>" +
        " man <b] is in need of a maid.<text]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testCombiningOverlapAndNonLinearity2() {
    String tagmlIn = "[text>It is a truth universally acknowledged that every " +
        "<|[add>young [b>woman<b]<add]" +
        "|[b>[del>rich<del]<b]|>" +
        " [b>man<b] is in need of a maid.<text]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Ignore
  @Test
  public void testCombiningDiscontinuityAndNonLinearity1() {
    String tagML = "[x>[q>and what is the use of a " +
        "<|[del>book,<del]" +
        "|<-q][add>thought Alice<add][+q>|>" +
        "without pictures or conversation?<q]<x]";
    assertTAGMLOutIsIn(tagML);
  }

  @Test
  public void testNonLinearityAndLayers1() {
    String tagML = "[tagml>[layerdef|+A,+B>[l|A>Et voilà que de la <|sombre|jolie|> [x|B>surface<x|B] d'un étang s'élève un cygne<l|A]<layerdef|A,B]<tagml]";
    assertTAGMLOutIsIn(tagML);
  }

  @Test
  public void testNonLinearityAndLayers2() {
    String tagML = "[a>[tagml|+A,+B>[l|A>Et voilà que de la <|[w|A>sombre<w|A]|[w|B>jolie<w|B]|> [x|B>surface<x|B] d'un étang s'élève un cygne<l|A]<tagml|A,B]<a]";
    assertTAGMLOutIsIn(tagML);
  }

  @Test
  public void testSingleQuotedStringAnnotation() {
    String tagML = "[tagml author='me'>test<tagml]";
    assertTAGMLOutIsIn(tagML);
  }

  @Test
  public void testDoubleQuotedStringAnnotation() {
    String tagML = "[tagml author=\"you\">test<tagml]";
    String out = parseAndExport(tagML);
    String expected = "[tagml author='you'>test<tagml]";
    assertThat(out).isEqualTo(expected);
  }

  @Test
  public void testStringAnnotationWithEscapedQuotes() {
    String tagML = "[tagml author=\"John \\\"Nickname\\\" O'Neill\">test<tagml]";
    String out = parseAndExport(tagML);
    String expected = "[tagml author='John \"Nickname\" O\\'Neill'>test<tagml]";
    assertThat(out).isEqualTo(expected);
  }

  @Test
  public void testStringListAnnotation() {
    String tagML = "[tagml author=['me','you']>test<tagml]";
    assertTAGMLOutIsIn(tagML);
  }

  @Test
  public void testNumberListAnnotation() {
    String tagML = "[tagml odd=[1,3,5]>test<tagml]";
    assertTAGMLOutIsIn(tagML);
  }

  @Test
  public void testBooleanListAnnotation() {
    String tagML = "[tagml b=[true,false]>test<tagml]";
    assertTAGMLOutIsIn(tagML);
  }

  @Test
  public void testNestedAnnotation() {
    String tagML = "[tagml author={first='John' last='Doe' alias=[{first='Jack' last='White'},{first='Ano' last='Nymous'}]}>test<tagml]";
    assertTAGMLOutIsIn(tagML);
  }

  @Ignore
  @Test
  public void balisageSlide4() {
    String tagML =
        "[ex|+B,+M>[q|B who=\"Brandy\">You can't destroy this love I found<-q]\n" +
            "[q|M who=\"Monica\">Your silly games I won't allow<-q]\n" +
            "[+q|B>The boy is mine without a doubt<q]\n" +
            "[+q|M>You might as well throw in the towel<q]<ex]";
    assertTAGMLOutIsIn(tagML.replaceAll("\\n", ""));
  }

  @Test
  public void balisageSelfOverlap1() {
    String tagML = "[phrase>[phrase>Oscar the Grouch is<phrase] a trash can-dwelling creature.<phrase]";
    assertTAGMLOutIsIn(tagML.replaceAll("\\n", ""));
  }

  @Test
  public void balisageSelfOverlap1a() {
    String tagML = "[phrase>Oscar the Grouch [phrase>is<phrase] a trash can-dwelling creature.<phrase]";
    assertTAGMLOutIsIn(tagML.replaceAll("\\n", ""));
  }

  @Test
  public void balisageSelfOverlap2() {
    String tagML = "[tagml>[phrase|+P1>[phrase|+P2>Rosita is<phrase|P1] a bilingual monster.<phrase|P2]<tagml]";
    assertTAGMLOutIsIn(tagML.replaceAll("\\n", ""));
  }

  @Test
  public void balisageSelfOverlap2a() {
    String tagML = "[tagml>[phrase|+P1>Rosita [phrase|+P2>is<phrase|P1] a bilingual monster.<phrase|P2]<tagml]";
    assertTAGMLOutIsIn(tagML.replaceAll("\\n", ""));
  }

  // --- private methods ---

  private void assertTAGMLOutIsIn(final String tagmlIn) {
    String tagmlOut = parseAndExport(tagmlIn);
    System.out.println(tagmlOut);
    assertThat(tagmlOut).isEqualTo(tagmlIn);
  }

  private String parseAndExport(final String tagmlIn) {
    AtomicReference<String> tagmlOut = new AtomicReference<>();
    runInStore(store -> {
      TAGDocumentDAO document = store.runInTransaction(
          () -> new TAGMLImporter().importTAGML(new TAGModelBuilderImpl(store, new ErrorListener()), tagmlIn)
      );
      String tagml = store.runInTransaction(() -> {
        logDocumentGraph(document, tagmlIn);
        return new TAGMLExporter(store).asTAGML(document);
      });
      tagmlOut.set(tagml);
    });
    return tagmlOut.get();
  }

}
