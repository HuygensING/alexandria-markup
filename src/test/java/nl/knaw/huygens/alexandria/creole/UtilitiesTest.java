package nl.knaw.huygens.alexandria.creole;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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

import org.junit.Test;

import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import static nl.knaw.huygens.alexandria.creole.Basics.id;
import static nl.knaw.huygens.alexandria.creole.Constructors.*;


public class UtilitiesTest extends CreoleTest {

  @Test
  public void testEmptyIsNullable() {
    Pattern p = Patterns.EMPTY;
    assertThat(p).isNullable();
  }

  @Test
  public void testNotAllowedIsNotNullable() {
    Pattern p = Patterns.NOT_ALLOWED;
    assertThat(p).isNotNullable();
  }

  @Test
  public void testTextIsNullable() {
    Pattern p = Patterns.TEXT;
    assertThat(p).isNullable();
  }

  @Test
  public void testChoiceNullability1() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p2 = NULLABLE_PATTERN;
    Pattern p = new Patterns.Choice(p1, p2);
    assertThat(p).isNullable();
  }

  @Test
  public void testChoiceNullability2() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p2 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.Choice(p1, p2);
    assertThat(p).isNullable();
  }

  @Test
  public void testChoiceNullability3() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p2 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.Choice(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testChoiceNullability4() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p2 = NULLABLE_PATTERN;
    Pattern p = new Patterns.Choice(p1, p2);
    assertThat(p).isNullable();
  }

  @Test
  public void testInterleaveNullability1() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p2 = NULLABLE_PATTERN;
    Pattern p = new Patterns.Interleave(p1, p2);
    assertThat(p).isNullable();
  }

  @Test
  public void testInterleaveNullability2() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p2 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.Interleave(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testInterleaveNullability3() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p2 = NULLABLE_PATTERN;
    Pattern p = new Patterns.Interleave(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testInterleaveNullability4() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p2 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.Interleave(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testConcurNullability1() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p2 = NULLABLE_PATTERN;
    Pattern p = new Patterns.Concur(p1, p2);
    assertThat(p).isNullable();
  }

  @Test
  public void testConcurNullability2() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p2 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.Concur(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testConcurNullability3() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p2 = NULLABLE_PATTERN;
    Pattern p = new Patterns.Concur(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testConcurNullability4() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p2 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.Concur(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testPartitionNullability1() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p = new Patterns.Partition(p1);
    assertThat(p).isNullable();
  }

  @Test
  public void testPartitionNullability2() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.Partition(p1);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testOneOrMoreNullability1() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p = new Patterns.OneOrMore(p1);
    assertThat(p).isNullable();
  }

  @Test
  public void testOneOrMoreNullability2() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.OneOrMore(p1);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testConcurOneOrMoreNullability1() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p = new Patterns.ConcurOneOrMore(p1);
    assertThat(p).isNullable();
  }

  @Test
  public void testConcurOneOrMoreNullability2() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.ConcurOneOrMore(p1);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testRangeIsNotNullable() {
    Pattern p1 = NULLABLE_PATTERN;
    NameClass nameClass = NameClasses.ANY_NAME;
    Pattern p = new Patterns.Range(nameClass, p1);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testEndRangeIsNotNullable() {
    Basics.QName qName = Basics.qName("uri", "localName");
    Pattern p = new Patterns.EndRange(qName, id("id"));
    assertThat(p).isNotNullable();
  }

  @Test
  public void testAfterNullability() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p2 = NULLABLE_PATTERN;
    Pattern p = new Patterns.After(p1, p2);
    assertThat(p).isNullable();
  }

  @Test
  public void testAllNullability1() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p2 = NULLABLE_PATTERN;
    Pattern p = new Patterns.All(p1, p2);
    assertThat(p).isNullable();
  }

  @Test
  public void testAllNullability2() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p2 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.All(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testAllNullability3() {
    Pattern p1 = NOT_NULLABLE_PATTERN;
    Pattern p2 = NULLABLE_PATTERN;
    Pattern p = new Patterns.All(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testAllNullability4() {
    Pattern p1 = NULLABLE_PATTERN;
    Pattern p2 = NOT_NULLABLE_PATTERN;
    Pattern p = new Patterns.All(p1, p2);
    assertThat(p).isNotNullable();
  }

  @Test
  public void testPatternTreeVisualisationForEmpty() {
    String emptyVisualisation = Utilities.patternTreeToDepth(Patterns.EMPTY, 1);
    assertThat(emptyVisualisation).isEqualTo("Empty()");
  }

  @Test
  public void testPatternTreeVisualisationForNotAllowed() {
    String emptyVisualisation = Utilities.patternTreeToDepth(Patterns.NOT_ALLOWED, 1);
    assertThat(emptyVisualisation).isEqualTo("NotAllowed()");
  }

  @Test
  public void testPatternTreeVisualisationForText() {
    String emptyVisualisation = Utilities.patternTreeToDepth(Patterns.TEXT, 1);
    assertThat(emptyVisualisation).isEqualTo("Text()");
  }

  @Test
  public void testPatternTreeVisualisationForChoice() {
    Pattern choice = choice(text(), empty());
    String emptyVisualisation = Utilities.patternTreeToDepth(choice, 15);
    assertThat(emptyVisualisation).isEqualTo("Choice(\n" +
        "| Text(),\n" +
        "| Empty()\n" +
        ")");
  }

  @Test
  public void testPatternTreeVisualisationForElement() {
    Pattern element = element("book", text());
    String visualisation = Utilities.patternTreeToDepth(element, 10);
    assertThat(visualisation).isEqualTo("Partition(\n" +//
        "| Range(\"book\",\n" +//
        "| | Text()\n" +//
        "| )\n" +//
        ")");
  }
}
