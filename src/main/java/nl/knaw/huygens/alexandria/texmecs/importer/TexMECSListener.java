package nl.knaw.huygens.alexandria.texmecs.importer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.AttsContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.EidContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.EndTagContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.SoleTagContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.StartTagContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.TextContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParserBaseListener;

public class TexMECSListener extends TexMECSParserBaseListener {

  Document document = new Document();
  Limen limen = document.value();
  Deque<TextRange> openMarkup = new ArrayDeque<>();

  public TexMECSListener() {

  }

  public Document getDocument() {
    return document;
  }

  @Override
  public void exitStartTag(StartTagContext ctx) {
    TextRange textRange = addTextRange(ctx.eid(), ctx.atts());
    openMarkup.add(textRange);
    super.exitStartTag(ctx);
  }

  @Override
  public void exitText(TextContext ctx) {
    TextNode tn = new TextNode(ctx.getText());
    limen.addTextNode(tn);
    openMarkup.forEach(m -> limen.associateTextWithRange(tn, m));
    super.exitText(ctx);
  }

  @Override
  public void exitEndTag(EndTagContext ctx) {
    String tag = ctx.gi().NAME_C().getText();
    Iterator<TextRange> descendingIterator = openMarkup.descendingIterator();
    TextRange textRange = null;
    while (descendingIterator.hasNext()) {
      textRange = descendingIterator.next();
      if (textRange.getTag().equals(tag)) {
        break;
      }
    }
    openMarkup.remove(textRange);
    super.exitEndTag(ctx);
  }

  @Override
  public void exitSoleTag(SoleTagContext ctx) {
    TextNode tn = new TextNode("");
    limen.addTextNode(tn);
    openMarkup.forEach(m -> limen.associateTextWithRange(tn, m));

    TextRange textRange = addTextRange(ctx.eid(), ctx.atts());
    limen.associateTextWithRange(tn, textRange);

    super.exitSoleTag(ctx);
  }

  private TextRange addTextRange(EidContext eid, AttsContext atts) {
    String tag = eid.gi().NAME_O().getText();
    TextRange textRange = new TextRange(limen, tag);
    addAttributes(atts, textRange);
    limen.addTextRange(textRange);
    return textRange;
  }

  private void addAttributes(AttsContext attsContext, TextRange textRange) {
    attsContext.avs().forEach(avs -> {
      String attrName = avs.NAME_O().getText();
      String quotedAttrValue = avs.STRING().getText();
      String attrValue = quotedAttrValue.substring(1, quotedAttrValue.length() - 1); // remove single||double quotes
      Annotation annotation = new Annotation(attrName, attrValue);
      textRange.addAnnotation(annotation);
    });
  }

}