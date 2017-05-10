package nl.knaw.huygens.alexandria.lmnl.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;

public class LQLQueryHandlerTest {
  Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testLQLQuery1() {
    String lmnl = "[excerpt}[p}\n"//
        + "Alice was beginning to get very tired of sitting by her sister on the bank,\n"//
        + "and of having nothing to do: once or twice she had peeped into the book her sister\n"//
        + "was reading, but it had no pictures or conversations in it, \n"//
        + "[q=a}and what is the use of a book,{q=a]\n"//
        + "thought Alice\n"//
        + "[q=a}without pictures or conversation?{q=a]\n"//
        + "{p]{excerpt]";
    Document alice = new LMNLImporter().importLMNL(lmnl);

    LQLQueryHandler h = new LQLQueryHandler(alice);
    // String statement = "select m.text from markup m where m.name='q' and m.id='a'";
    // String statement = "select m.text from markup m where m.name='q=a'";
    String statement = "select text from markup where name='q=a'";
    LQLResult result = h.execute(statement);
    LOG.info("result={}", result);
    assertThat(result).isNotNull();
    List<String> expected = new ArrayList<>();
    expected.add("and what is the use of a book,without pictures or conversation?");
    assertThat(result.getValues()).containsExactlyElementsOf(expected);
  }

  @Test
  public void testLQLQuery2() {
    String lmnl = "[text}\n"//
        + "[l}line 1{l]\n"//
        + "[l}line 2{l]\n"//
        + "[l}line 3{l]\n"//
        + "{text]";
    Document alice = new LMNLImporter().importLMNL(lmnl);

    LQLQueryHandler h = new LQLQueryHandler(alice);

    String statement1 = "select text from markup('l')[0]";
    LQLResult result1 = h.execute(statement1);
    LOG.info("result1.values={}", result1.getValues());
    assertThat(result1).isNotNull();
    List<String> expected1 = new ArrayList<>();
    expected1.add("line 1");
    assertThat(result1.getValues()).containsExactlyElementsOf(expected1);

    String statement2 = "select text from markup('l')[2]";
    LQLResult result2 = h.execute(statement2);
    LOG.info("result2.values={}", result2.getValues());
    assertThat(result2).isNotNull();
    List<String> expected2 = new ArrayList<>();
    expected2.add("line 3");
    assertThat(result2.getValues()).containsExactlyElementsOf(expected2);

    String statement3 = "select text from markup('l')";
    LQLResult result3 = h.execute(statement3);
    LOG.info("result3.values={}", result3.getValues());
    assertThat(result3).isNotNull();
    List<String> expected3 = new ArrayList<>();
    expected3.add("line 1");
    expected3.add("line 2");
    expected3.add("line 3");
    assertThat(result3.getValues()).containsExactlyElementsOf(expected3);
  }

}
