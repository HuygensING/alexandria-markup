package nl.knaw.huc.di.tag.tagml;

import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkupPathTest extends AlexandriaBaseStoreTest {
  @Test
  public void test1() {
    String tagml = "[a>bla [b>bla<b] bla<a]";
    runInStoreTransaction(store -> {
      TAGDocument tagDocument = parse(tagml, store);

      final List<TAGMarkup> markups = getTagMarkups(tagDocument);
      assertThat(markups).hasSize(2);

      final TAGMarkup a = markups.get(0);
      assertThat(a.getTag()).isEqualTo("a");

      final TAGMarkup b = markups.get(1);
      assertThat(b.getTag()).isEqualTo("b");

      MarkupPath pathA = new MarkupPath(a, tagDocument,store);
      assertThat(pathA.getPath()).isEqualTo("a");

      MarkupPath pathB = new MarkupPath(b, tagDocument, store);
      assertThat(pathB.getPath()).isEqualTo("a/b[1]");
    });
  }

  private List<TAGMarkup> getTagMarkups(final TAGDocument tagDocument) {
    return tagDocument.getMarkupStream().collect(Collectors.toList());
  }

  private TAGDocument parse(final String tagml, final TAGStore store) {
    TAGMLImporter importer = new TAGMLImporter(store);
    return importer.importTAGML(tagml);
  }

}