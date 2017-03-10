package nl.knaw.huygens.alexandria.lmnl.exporter;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
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
        Set<TextRange> textRanges = limen.getTextRanges(tn);

        List<TextRange> toClose = new ArrayList<>();
        toClose.addAll(openTextRanges);
        toClose.removeAll(textRanges);
        Collections.reverse(toClose);
        toClose.forEach(tr -> lmnlBuilder.append(toCloseTag(tr)));

        List<TextRange> toOpen = new ArrayList<>();
        toOpen.addAll(textRanges);
        toOpen.removeAll(openTextRanges);
        toOpen.forEach(tr -> lmnlBuilder.append(toOpenTag(tr)));

        openTextRanges.removeAll(toClose);
        openTextRanges.addAll(toOpen);
        lmnlBuilder.append(tn.getContent());
      });
      openTextRanges.forEach(tr -> lmnlBuilder.append(toCloseTag(tr)));
    }
  }

  private StringBuilder toCloseTag(TextRange textRange) {
    return textRange.isAnonymous()
            ? new StringBuilder()
            : new StringBuilder("{").append(textRange.getTag()).append("]");
  }

  private StringBuilder toOpenTag(TextRange textRange) {
    StringBuilder tagBuilder = new StringBuilder("[").append(textRange.getTag());
    textRange.getAnnotations().forEach(a ->
            tagBuilder.append(" ").append(toLMNL(a))
    );
    return textRange.isAnonymous()
            ? tagBuilder.append("]")
            : tagBuilder.append("}");
  }

  public StringBuilder toLMNL(Annotation annotation) {
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
