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

  // @Test
  public void testLQLQuery1() {
    String lmnl = "[excerpt}[p}\n"//
        + "Alice was beginning to get very tired of sitting by her sister on the bank,\n"//
        + "and of having nothing to do: once or twice she had peeped into the book her sister\n" + "was reading, but it had no pictures or conversations in it, \n"//
        + "[q=a}and what is the use of a book,{q=a]\n"//
        + "thought Alice\n"//
        + "[q=a}without pictures or conversation?{q=a]\n"//
        + "{p]{excerpt]";
    Document alice = new LMNLImporter().importLMNL(lmnl);

    LQLQueryHandler h = new LQLQueryHandler(alice);
    String statement = "select m.text from markup m where m.name='q' and m.id='a'";
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
    String statement = "select text from markup('l')[0]";
    LQLResult result = h.execute(statement);
    LOG.info("result.values={}", result.getValues());
    assertThat(result).isNotNull();
    List<String> expected = new ArrayList<>();
    expected.add("line 1");
    assertThat(result.getValues()).containsExactlyElementsOf(expected);
  }

}
