package nl.knaw.huc.di.tag.tagml.importer;

import nl.knaw.huygens.alexandria.ErrorListener;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TAGKnowledgeModelBuilderTest {

//  @Test
  public void test() {
    ErrorListener errorListener = new ErrorListener();
    TAGKnowledgeModelBuilder builder = new TAGKnowledgeModelBuilder(errorListener);
    TAGMLImporter tagmlImporter = new TAGMLImporter();
    tagmlImporter.importTAGML(builder, "[x>bla<x]");
    String turtle = builder.asTurtle();
    assertThat(turtle).isEqualTo("some ttl string");
  }

}