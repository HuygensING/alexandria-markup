package nl.knaw.huygens.alexandria.data_model;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import java.util.Set;

import org.junit.jupiter.api.Test;

import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporterInMemory;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLSyntaxError;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeRangeIndexInMemoryTest {

  @Test
  public void testRangesFromNodes() throws LMNLSyntaxError {
    String lmnl =
        "[excerpt\n"
            + "  [source [date}1915{][title}The Housekeeper{]]\n"
            + "  [author\n"
            + "    [name}Robert Frost{]\n"
            + "    [dates}1874-1963{]] }\n"
            + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"
            + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"
            + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n"
            + "{excerpt]";
    NodeRangeIndexInMemory index = index(lmnl);
    // textnode 1= "He manages to keep the upper hand"
    Set<Integer> rangeIndices = index.getRanges(1); // indices of ranges that contain textnode 1
    assertThat(rangeIndices).containsExactly(1, 2); // excerpt,s,l
  }

  @Test
  public void testNodesFromRanges() throws LMNLSyntaxError {
    String lmnl =
        "[excerpt\n"
            + "  [source [date}1915{][title}The Housekeeper{]]\n"
            + "  [author\n"
            + "    [name}Robert Frost{]\n"
            + "    [dates}1874-1963{]] }\n"
            + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"
            + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"
            + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n"
            + "{excerpt]";
    NodeRangeIndexInMemory index = index(lmnl);
    Set<Integer> textNodeIndices =
        index.getTextNodes(0); // indices of textnodes contained in range 0: excerpt
    assertThat(textNodeIndices).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test
  public void testIndexWithAlice() throws LMNLSyntaxError {
    String lmnl =
        "[excerpt}[p}\n"
            + "Alice was beginning to get very tired of sitting by her sister on the bank,\n"
            + "and of having nothing to do: once or twice she had peeped into the book her sister\n"
            + "was reading, but it had no pictures or conversations in it, \n"
            + "[q [n}a{]}and what is the use of a book,{q]\n"
            + "thought Alice\n"
            + "[q [n}a{]}without pictures or conversation?{q]\n"
            + "{p]{excerpt]";
    NodeRangeIndexInMemory index = index(lmnl);
    Set<Integer> textNodeIndices =
        index.getTextNodes(2); // indices of textnodes contained in range 2: q=a
    assertThat(textNodeIndices).containsExactly(1, 3);

    Set<Integer> rangeIndices = index.getRanges(1); // indices of ranges that contain textnode 1
    assertThat(rangeIndices).containsExactly(0, 1, 2); // excerpt,p,q=a
  }

  private NodeRangeIndexInMemory index(String lmnl) throws LMNLSyntaxError {
    LMNLImporterInMemory importer = new LMNLImporterInMemory();
    Document document = importer.importLMNL(lmnl);
    return new NodeRangeIndexInMemory(document.value());
  }
}
