package nl.knaw.huygens.alexandria.lmnl.exporter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.Markup;

/**
 * Created by bramb on 07/02/2017.
 */
public class LMNLExporter {
  private static Logger LOG = LoggerFactory.getLogger(LMNLExporter.class);
  boolean useShorthand = false;

  public LMNLExporter useShorthand() {
    useShorthand = true;
    return this;
  }

  public String toLMNL(Document document) {
    StringBuilder lmnlBuilder = new StringBuilder();
    Limen limen = document.value();
    appendLimen(lmnlBuilder, limen);
    // LOG.info("LMNL={}", lmnlBuilder);
    return lmnlBuilder.toString();
  }

  private void appendLimen(StringBuilder lmnlBuilder, Limen limen) {
    if (limen != null) {
      Deque<Markup> openMarkups = new ArrayDeque<>();
      limen.getTextNodeIterator().forEachRemaining(tn -> {
        Set<Markup> markups = limen.getMarkups(tn);

        List<Markup> toClose = new ArrayList<>();
        toClose.addAll(openMarkups);
        toClose.removeAll(markups);
        Collections.reverse(toClose);
        toClose.forEach(tr -> lmnlBuilder.append(toCloseTag(tr)));

        List<Markup> toOpen = new ArrayList<>();
        toOpen.addAll(markups);
        toOpen.removeAll(openMarkups);
        toOpen.forEach(tr -> lmnlBuilder.append(toOpenTag(tr)));

        openMarkups.removeAll(toClose);
        openMarkups.addAll(toOpen);
        lmnlBuilder.append(tn.getContent());
      });
      openMarkups.descendingIterator()//
          .forEachRemaining(tr -> lmnlBuilder.append(toCloseTag(tr)));
    }
  }

  private StringBuilder toCloseTag(Markup markup) {
    return markup.isAnonymous()//
        ? new StringBuilder()//
        : new StringBuilder("{").append(markup.getExtendedTag()).append("]");
  }

  private StringBuilder toOpenTag(Markup markup) {
    StringBuilder tagBuilder = new StringBuilder("[").append(markup.getExtendedTag());
    markup.getAnnotations().forEach(a -> tagBuilder.append(" ").append(toLMNL(a)));
    return markup.isAnonymous()//
        ? tagBuilder.append("]")//
        : tagBuilder.append("}");
  }

  public StringBuilder toLMNL(Annotation annotation) {
    StringBuilder annotationBuilder = new StringBuilder("[").append(annotation.getTag());
    annotation.getAnnotations().forEach(a1 -> annotationBuilder.append(" ").append(toLMNL(a1)));
    Limen limen = annotation.value();
    if (limen.hasTextNodes()) {
      annotationBuilder.append("}");
      appendLimen(annotationBuilder, limen);
      if (useShorthand) {
        annotationBuilder.append("{]");
      } else {
        annotationBuilder.append("{").append(annotation.getTag()).append("]");
      }
    } else {
      annotationBuilder.append("]");
    }
    return annotationBuilder;
  }

}
