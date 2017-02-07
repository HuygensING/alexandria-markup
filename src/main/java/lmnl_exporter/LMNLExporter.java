package lmnl_exporter;

import data_model.Annotation;
import data_model.Document;
import data_model.Limen;
import data_model.TextRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
//        LOG.info("LMNL={}", lmnlBuilder);
    return lmnlBuilder.toString();
  }

  private void appendLimen(StringBuilder lmnlBuilder, Limen limen) {
    if (limen != null) {
      Set<TextRange> openTextRanges = new LinkedHashSet<>();
      limen.getTextNodeIterator().forEachRemaining(tn -> {
        List<TextRange> textRanges = limen.getTextRanges(tn);
        List<TextRange> toOpen = new ArrayList<>();
        toOpen.addAll(textRanges);
        toOpen.removeAll(openTextRanges);
        List<TextRange> toClose = new ArrayList<>();
        toClose.addAll(openTextRanges);
        toClose.removeAll(textRanges);
        Collections.reverse(toClose);
        toClose.forEach(tr -> lmnlBuilder.append(toCloseTag(tr)));
        toOpen.forEach(tr -> lmnlBuilder.append(toOpenTag(tr)));
        openTextRanges.removeAll(toClose);
        openTextRanges.addAll(toOpen);
        lmnlBuilder.append(tn.getContent());
      });
      openTextRanges.forEach(tr -> lmnlBuilder.append(toCloseTag(tr)));
    }
  }

  private StringBuilder toCloseTag(TextRange tr) {
    return new StringBuilder("{").append(tr.getTag()).append("]");
  }

  private StringBuilder toOpenTag(TextRange tr) {
    StringBuilder tagBuilder = new StringBuilder("[").append(tr.getTag());
    tr.getAnnotations().forEach(a ->
        tagBuilder.append(" ").append(toLMNL(a))
    );
    return tagBuilder.append("}");
  }

  private StringBuilder toLMNL(Annotation annotation) {
    StringBuilder annotationBuilder = new StringBuilder("[").append(annotation.getTag());
    annotation.getAnnotations().forEach(a1 ->
        annotationBuilder.append(" ").append(toLMNL(a1))
    );
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
