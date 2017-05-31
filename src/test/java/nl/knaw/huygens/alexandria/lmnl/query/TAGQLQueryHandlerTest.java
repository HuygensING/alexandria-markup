package nl.knaw.huygens.alexandria.lmnl.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;

public class TAGQLQueryHandlerTest {
  Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testTAGQLQuery1() {
    String lmnl = "[excerpt}[p}\n"//
        + "Alice was beginning to get very tired of sitting by her sister on the bank,\n"//
        + "and of having nothing to do: once or twice she had peeped into the book her sister\n"//
        + "was reading, but it had no pictures or conversations in it, \n"//
        + "[q=a}and what is the use of a book,{q=a]\n"//
        + "thought Alice\n"//
        + "[q=a}without pictures or conversation?{q=a]\n"//
        + "{p]{excerpt]";
    Document alice = new LMNLImporter().importLMNL(lmnl);

    TAGQLQueryHandler h = new TAGQLQueryHandler(alice);
    // String statement = "select m.text from markup m where m.name='q' and m.id='a'";
    // String statement = "select m.text from markup m where m.name='q=a'";
    String statement = "select text from markup where name='q=a'";
    TAGQLResult result = h.execute(statement);
    LOG.info("result={}", result);
    assertThat(result).isNotNull();
    List<String> expected = new ArrayList<>();
    expected.add("and what is the use of a book,without pictures or conversation?");
    assertThat(result.getValues()).containsExactlyElementsOf(expected);
  }

  @Test
  public void testTAGQLQuery2() {
    String lmnl = "[text}\n"//
        + "[l}line 1{l]\n"//
        + "[l}line 2{l]\n"//
        + "[l}line 3{l]\n"//
        + "{text]";
    Document alice = new LMNLImporter().importLMNL(lmnl);

    TAGQLQueryHandler h = new TAGQLQueryHandler(alice);

    String statement1 = "select text from markup('l')[0]";
    TAGQLResult result1 = h.execute(statement1);
    LOG.info("result1.values={}", result1.getValues());
    assertThat(result1).isNotNull();
    List<String> expected1 = new ArrayList<>();
    expected1.add("line 1");
    assertThat(result1.getValues()).containsExactlyElementsOf(expected1);

    String statement2 = "select text from markup('l')[2]";
    TAGQLResult result2 = h.execute(statement2);
    LOG.info("result2.values={}", result2.getValues());
    assertThat(result2).isNotNull();
    List<String> expected2 = new ArrayList<>();
    expected2.add("line 3");
    assertThat(result2.getValues()).containsExactlyElementsOf(expected2);

    String statement3 = "select text from markup('l')";
    TAGQLResult result3 = h.execute(statement3);
    LOG.info("result3.values={}", result3.getValues());
    assertThat(result3).isNotNull();
    List<String> expected3 = new ArrayList<>();
    expected3.add("line 1");
    expected3.add("line 2");
    expected3.add("line 3");
    assertThat(result3.getValues()).containsExactlyElementsOf(expected3);
  }

  @Test
  public void testTAGQLQuery3() {
    String lmnl = "[excerpt [source [book}1 Kings{book] [chapter}12{chapter]]}\n"
        + "[verse}And he said unto them, [q}What counsel give ye that we may answer this people, who have spoken to me, saying, [q}Make the yoke which thy father did put upon us lighter?{q]{q]{verse]\n"
        + "[verse}And the young men that were grown up with him spake unto him, saying, [q}Thus shalt thou speak unto this people that spake unto thee, saying, [q=i}Thy father made our yoke heavy, but make thou it lighter unto us;{q=i] thus shalt thou say unto them, [q=j}My little finger shall be thicker than my father's loins.{verse]\n"
        + "[verse}And now whereas my father did lade you with a heavy yoke, I will add to your yoke: my father hath chastised you with whips, but I will chastise you with scorpions.{q=j]{q]{verse]\n"
        + "[verse}So Jeroboam and all the people came to Rehoboam the third day, as the king had appointed, saying, [q}Come to me again the third day.{q]{verse]\n" + "{excerpt]";
    Document kings = new LMNLImporter().importLMNL(lmnl);

    TAGQLQueryHandler h = new TAGQLQueryHandler(kings);

    String statement1 = "select annotationText('source:chapter') from markup where name='excerpt'";
    TAGQLResult result1 = h.execute(statement1);
    LOG.info("result1={}", result1);
    assertThat(result1).isNotNull();
    List<List<String>> expected1 = new ArrayList<>();
    List<String> expectedEntry = new ArrayList<>();
    expectedEntry.add("12");
    expected1.add(expectedEntry);
    assertThat(result1.getValues()).containsExactlyElementsOf(expected1);

    // String statement2 = "select m.text from markup m where m.name='q' and m in (select q from markup q where q.name='q')";
    // TAGQLResult result2 = h.execute(statement2);
    // LOG.info("result2={}", result2);
    // assertThat(result2).isNotNull();
    // List<String> expected2 = new ArrayList<>();
    // expected2.add("Make the yoke which thy father did put upon us lighter?");
    // expected2.add("Thy father made our yoke heavy, but make thou it lighter unto us;");
    // expected2.add(
    // "My little finger shall be thicker than my father's loins.\\nAnd now whereas my father did lade you with a heavy yoke, I will add to your yoke: my father hath chastised you with whips, but I
    // will chastise you with scorpions.");
    // assertThat(result2.getValues()).containsExactlyElementsOf(expected2);

  }

  @Test
  public void testLuminescentQuery1() throws IOException {
    String lmnl = FileUtils.readFileToString(new File("data/lmnl/frankenstein.lmnl"), "UTF-8");
    Document frankenstein = new LMNLImporter().importLMNL(lmnl);

    TAGQLQueryHandler h = new TAGQLQueryHandler(frankenstein);

    String statement1 = "select annotationText('n') from markup where name='page' and text contains 'Volney'";
    TAGQLResult result1 = h.execute(statement1);
    LOG.info("result1={}", result1);
    assertThat(result1).isNotNull();
    List<List<String>> expected1 = new ArrayList<>();
    List<String> expectedEntry = new ArrayList<>();
    expectedEntry.add("102");
    expected1.add(expectedEntry);
    assertThat(result1.getValues()).containsExactlyElementsOf(expected1);
  }

  // @Test
  public void testLuminescentQuery2() throws IOException {
    String lmnl = FileUtils.readFileToString(new File("data/lmnl/frankenstein.lmnl"), "UTF-8");
    Document frankenstein = new LMNLImporter().importLMNL(lmnl);

    TAGQLQueryHandler h = new TAGQLQueryHandler(frankenstein);

    String statement1 = "select distinct(annotationText('who')) from markup where name='said'";
    TAGQLResult result1 = h.execute(statement1);
    LOG.info("result1={}", result1);
    assertThat(result1).isNotNull();
    List<List<String>> expected1 = new ArrayList<>();
    List<String> expectedEntry = new ArrayList<>();
    expectedEntry.add("Creature");
    expected1.add(expectedEntry);
    assertThat(result1.getValues()).containsExactlyElementsOf(expected1);
  }
}
