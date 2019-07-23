package nl.knaw.huygens.alexandria.lmnl.importer;

/*
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

import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class SnarkTest extends AlexandriaBaseStoreTest {
  private final Logger LOG = LoggerFactory.getLogger(SnarkTest.class);

  @Ignore
  @Test
  public void testSnark81Master() throws LMNLSyntaxError {
    String input = "[poem [title}The Hunting of the Snark (An Agony in 8 Fits){title] [voice}Narrator{voice]}\n" +
        "[text [title}The Hunting of the Snark (An Agony in 8 Fits){title]}\n" +
        "[canto [title}The Vanishing{title]}\n" +
        "[page}\n" +
        "[line}[phr [type}NP{]}[w [type}D{]}The{w] [w [type}N{]}Vanishing{w]{phr]\n" +
        "[page-nr}81{page-nr]{line]\n" +
        "...{page]\n" +
        "[page [n}80{n]} \n" +
        "[stanza}[p}\n" +
        "[sentence}\n" +
        "[l}[pl}In the next, that wild figure they saw{pl]{l]\n" +
        "{page]\n" +
        "[page [n}81{n]} \n" +
        "[line}[l}[pl}[s}[phr [type}NP{]}(As if stung{phr] [phr [type}PP{]}by a spasm){phr] [phr [type}VP{]}plunge into a chasm,{phr]{pl]{l]{line]\n" +
        "[line}[l}[pl}[phr [type}VP{]}While they waited [w [type}contr{]}and{w] listened in awe.{phr]{s]{pl]{l]{line]{sentence]\n" +
        "{p]{stanza]\n" +
        "[el}{el]\n" +
        "[el}{el]\n" +
        "[stanza}[p}\n" +
        "[sentence=a}[line}[l}[pl}[sentence=b [voice}Baker{voice]}[s}[phr [type}NP{]}\"It's a Snark!\"{phr]{sentence=b] [phr [type}VP{]}was the sound that first came{phr] {pl]{line]\n" +
        "[line}[pl}[phr [type}PP{]}to their ears,{phr]{pl]{l]{line]\n" +
        "[line}[l}[pl}[phr [type}VP{]}And seemed almost too [w [type}A{]}good{w] to be true.{phr]{s]{pl]{l]{line]{sentence=a]\n" +
        "[line}[l}[pl}[sentence=c}[s}[phr [type}VP{]}Then followed{phr] [phr [type}NP{]}a torrent of laughter and cheers:{phr]{pl]{l]{line]\n" +
        "[line}[l}[pl}[phr=a [type}NP{]}Then the ominous words [sentence=d [voice}Baker{voice]}[phr=b [type}VP{]}\"It's a Boo--\"{phr=b]{phr=a]{sentence=d]{l]{line]{pl]\n" +
        "{p]{stanza]\n" +
        "[el}{el]\n" +
        "[el}{el]\n" +
        "[stanza}[p}\n" +
        "[line}[l}[pl}[phr [type}NP{]}Then, silence.{phr]{s]{sentence=c] [sentence=e}[s}[phr [type}NP{]}Some fancied{phr] [phr [type}VP{]}they heard in the {pl]{line][line}[pl}air{phr]{pl]{l]{line]\n" +
        "[line}[l}[pl}[phr [type}NP{]}A weary and wandering sigh{phr]{pl]{l]{line]\n" +
        "[line}[l}[pl}[phr=a [type}VP{]}That sounded like [sentence=f [voice}Baker{voice]}[phr=b [type}N{]}\"--jum!\"{phr=b]{sentence=f] but the others de-{pl]{line][line}[pl}clare{pl]{phr=a]{l]{line]\n" +
        "[line}[l}[pl}[phr [type}NP{]}It was only a breeze that went by.{phr]{s]{pl]{l]{line]{sentence=e]\n" +
        "{p]{stanza]\n" +
        "{canto]\n" +
        "{page]\n" +
        "{text]\n" +
        "{poem]";
    printTokens(input);
    runInStoreTransaction(store -> {
      TAGDocument document = new LMNLImporter(store).importLMNL(input);
      assertThat(document).isNotNull();

      Set<String> linguisticMarkup = new HashSet<>(asList("text", "phr", "phr=a", "phr=b", "word", "s", "m", "c", "w"));
      TAGView viewL = new TAGView(store).setMarkupToInclude(linguisticMarkup);
      LMNLExporter exporterL = new LMNLExporter(store, viewL).useShorthand();
      String lmnlL = exporterL.toLMNL(document).replaceAll("\n\\s*\n", "\n").trim();
      LOG.info("linguistic view: lmnlL=\n{}", lmnlL);

      Set<String> materialMarkup = new HashSet<>(asList("page", "page-nr", "line", "p", "el"));
      TAGView viewM = new TAGView(store).setMarkupToInclude(materialMarkup);
      LMNLExporter exporterM = new LMNLExporter(store, viewM).useShorthand();
      String lmnlM = exporterM.toLMNL(document).replaceAll("\n\\s*\n", "\n").trim();
      LOG.info("material view: lmnlM=\n{}", lmnlM);

      Set<String> poeticMarkup = new HashSet<>(asList("poem", "canto", "stanza", "l"));
      TAGView viewP = new TAGView(store).setMarkupToInclude(poeticMarkup);
      LMNLExporter exporterP = new LMNLExporter(store, viewP).useShorthand();
      String lmnlP = exporterP.toLMNL(document).replaceAll("\n\\s*\n", "\n").trim();
      LOG.info("poetic view: lmnlP=\n{}", lmnlP);
    });
  }

  @Test
  public void testSnark81Linguistic() throws LMNLSyntaxError {
    String input = "[text [title}The Hunting of the Snark (An Agony in 8 Fits){title] } \n" +
        "[phr [type}NP{]}[w [type}D{]}The{w] [w [type}N{]}Vanishing{w]{phr]\n" +
        "[s}[phr [type}NP{]}(As if stung{phr] [phr [type}PP{]}by a spasm){phr] [phr [type}VP{]}plunge into a chasm,{phr]\n" +
        "[phr [type}VP{]}While they waited [w [type}contr{]}and{w] listened in awe.{phr]{s]\n" +
        "[s}[phr [type}NP{]}\"It's a Snark!\"{phr] [phr [type}VP{]}was the sound that first came{phr]\n" +
        "[phr [type}PP{]}to their ears,{phr]\n" +
        "[phr [type}VP{]}And seemed almost too [w [type}A{]}good{w] to be true.{phr]{s]\n" +
        "[s}[phr [type}VP{]}Then followed{phr] [phr [type}NP{]}a torrent of laughter and cheers:{phr]\n" +
        "[phr=a [type}NP{]}Then the ominous words [phr=b [type}VP{]}\"It's a Boo--\"{phr=b]{phr=a]\n" +
        "[phr [type}NP{]}Then, silence.{phr]{s] [s}[phr [type}NP{]}Some fancied{phr] [phr [type}VP{]}they heard in the air{phr]\n" +
        "[phr [type}NP{]}A weary and wandering sigh{phr]\n" +
        "[phr=a [type}VP{]}That sounded like [phr=b [type}N{]}\"--jum!\"{phr=b] but the others declare{phr=a]\n" +
        "[phr [type}NP{]}It was only a breeze that went by.{phr]{s]\n" +
        "{text]\n";
    printTokens(input);
    Document actual = new LMNLImporterInMemory().importLMNL(input);
    assertThat(actual).isNotNull();
  }

  @Test
  public void testSnark81Material() throws LMNLSyntaxError {
    String input = "[page} \n" +
        "[line}The Vanishing [page-nr}81{page-nr]{line] \n" +
        "[p}\n" +
        "[line}(As if stung by a spasm) plunge into a chasm,{line]\n" +
        "[line}While they waited and listened in awe.{line]\n" +
        "{p]\n" +
        "[el}{el]\n" +
        "[el}{el]\n" +
        "[p}\n" +
        "[line}\"It's a Snark!\" was the sound that first came{line]\n" +
        "[line}to their ears,{line]\n" +
        "[line}And seemed almost too good to be true.{line]\n" +
        "[line}Then followed a torrent of laughter and cheers:{line]\n" +
        "[line}Then the ominous words \"It's a Boo--\"{line]\n" +
        "{p]\n" +
        "[el}{el]\n" +
        "[el}{el]\n" +
        "[p}\n" +
        "[line}Then, silence. Some fancied they heard in the {line][line}air{line]\n" +
        "[line}A weary and wandering sigh{line]\n" +
        "[line}That sounded like \"--jum!\" but the others de-{line][line}clare{line]\n" +
        "[line}It was only a breeze that went by.{line]\n" +
        "{p]\n" +
        "{page]\n";
    printTokens(input);
    Document actual = new LMNLImporterInMemory().importLMNL(input);
    assertThat(actual).isNotNull();
  }

  @Test
  public void testSnark81Poetic() throws LMNLSyntaxError {
    String input = "[poem [title}The Hunting of the Snark (An Agony in 8 Fits){title] [voice}Narrator{voice] } \n" +
        "[canto [title}The Vanishing{title]}\n" +
        "[stanza} \n" +
        "[l}(As if stung by a spasm) plunge into a chasm,{l]\n" +
        "[l}While they waited and listened in awe.{l]\n" +
        "{stanza]\n" +
        "[stanza}\n" +
        "[l}\"It's a Snark!\" was the sound that first came to their ears,{l]\n" +
        "[l}And seemed almost too good to be true.{l]\n" +
        "[l}Then followed a torrent of laughter and cheers:{l]\n" +
        "[l}Then the ominous words \"It's a Boo--\"{l]\n" +
        "{stanza]\n" +
        "[stanza}\n" +
        "[l}Then, silence. Some fancied they heard in the air{l]\n" +
        "[l}A weary and wandering sigh{l]\n" +
        "[l}That sounded like \"--jum!\" but the others declare{l]\n" +
        "[l}It was only a breeze that went by.{l]\n" +
        "{stanza]\n" +
        "{canto]\n" +
        "{poem]\n";
    printTokens(input);
    Document actual = new LMNLImporterInMemory().importLMNL(input);
    assertThat(actual).isNotNull();
  }

}
