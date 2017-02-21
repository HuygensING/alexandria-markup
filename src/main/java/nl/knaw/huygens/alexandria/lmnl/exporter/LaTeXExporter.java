package nl.knaw.huygens.alexandria.lmnl.exporter;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by bramb on 07/02/2017.
 */
public class LaTeXExporter {
  private static Logger LOG = LoggerFactory.getLogger(LaTeXExporter.class);

  public String toLaTeX(Document document) {
    StringBuilder latexBuilder = new StringBuilder();
    latexBuilder.append("\\documentclass{article}\n")
            .append("\\usepackage{tikz}\n")
            .append("\\usepackage{latexsym}\n")
            .append("\\usepackage[utf8x]{inputenc}\n")
            .append("\\usetikzlibrary{arrows,decorations.pathmorphing,backgrounds,positioning,fit,petri}\n")
            .append("\n").append("\\begin{document}\n")
            .append("  \\pagenumbering{gobble}% Remove page numbers (and reset to 1)\n")
            .append("  \\begin{tikzpicture}\n")
            .append("    [textnode/.style={rectangle,draw=black!50,thick},\n")
            .append("     textrange/.style={rectangle,draw=blue!50,thick},\n")
            .append("     document/.style={circle,draw=black!50,thick}]\n")
            .append("    \\node[document] (doc) {document};\n");
    Limen limen = document.value();
    appendLimen(latexBuilder, limen);
    latexBuilder.append("  \\end{tikzpicture}\n")
            .append("\\end{document}\n");
    return latexBuilder.toString();
  }

  private void appendLimen(StringBuilder latexBuilder, Limen limen) {
    if (limen != null) {
      Set<TextRange> openTextRanges = new LinkedHashSet<>();
      AtomicInteger textNodeCounter = new AtomicInteger(0);
      AtomicInteger textRangeCounter = new AtomicInteger(0);
      Map<TextRange,Integer> textRangeIndices = new HashMap<>();
      limen.getTextNodeIterator().forEachRemaining(tn -> {
        int i = textNodeCounter.getAndIncrement();
        List<TextRange> textRanges = limen.getTextRanges(tn);

        List<TextRange> toClose = new ArrayList<>();
        toClose.addAll(openTextRanges);
        toClose.removeAll(textRanges);
        Collections.reverse(toClose);

        List<TextRange> toOpen = new ArrayList<>();
        toOpen.addAll(textRanges);
        toOpen.removeAll(openTextRanges);

        openTextRanges.removeAll(toClose);
        openTextRanges.addAll(toOpen);

        String content = tn.getContent().replaceAll(" ", "\\\\textvisiblespace ").replaceAll("\n", "\\\\textbackslash n");
        String relPos = i == 0 ? "below=of doc" : ("right=of tn" + (i - 1));
        String nodeLine = "    \\node[textnode] (tn" + i + ") [" + relPos + "] {" + content + "};\n";
        latexBuilder.append(nodeLine);
        if (textNodeCounter.get() > 1) {
          String drawLine = "    \\draw [->] (tn" + (i - 1) + ") -- (tn" + i + ");\n";
          latexBuilder.append(drawLine);
        }else{
          String drawDocLine = "    \\draw [->] (doc) -- (tn" + i + ");\n";
          latexBuilder.append(drawDocLine);
        }
//        toClose.forEach(tr -> {
//          int textRangeIndex = textRangeIndices.get(tr);
//          String drawLine = "    \\draw [->] (tr" + textRangeIndex + ") -- (tn" + (i-1) + ");\n";
//          latexBuilder.append(drawLine);
//        });
//        toOpen.forEach(tr -> {
//          textRangeIndices.put(tr,textRangeCounter.get());
//          int textRangeIndex = textRangeCounter.getAndIncrement();
//          String relPos2 = textRangeIndex == 0 ? "below=of tn" + i : ("right=of tr" + (textRangeIndex - 1));
//          String textRangeLine = "    \\node[textrange] (tr" + textRangeIndex + ") [" + relPos2 + "] {" + tr.getTag() + "};\n";
//          latexBuilder.append(textRangeLine);
//          String drawLine = "    \\draw [->] (tr" + textRangeIndex + ") -- (tn" + i + ");\n";
//          latexBuilder.append(drawLine);
//        });
      });
//      openTextRanges.forEach(tr -> latexBuilder.append(toCloseTag(tr)));
    }
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
      annotationBuilder.append("{").append(annotation.getTag()).append("]");
    } else {
      annotationBuilder.append("]");
    }
    return annotationBuilder;
  }

}
