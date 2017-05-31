package nl.knaw.huygens.alexandria.texmecs.importer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.AttsContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.EidContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.EndTagContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.EndTagSetContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.GiContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.ResumeTagContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.SoleTagContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.StartTagContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.StartTagSetContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.SuspendTagContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.TextContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.VirtualElementContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParserBaseListener;

public class TexMECSListener extends TexMECSParserBaseListener {

  Logger LOG = LoggerFactory.getLogger(getClass());

  private Document document = new Document();
  private Limen limen = document.value();
  private Deque<TextRange> openMarkup = new ArrayDeque<>();
  private Deque<TextRange> suspendedMarkup = new ArrayDeque<>();
  private boolean insideTagSet = false; // TODO: use this?
  private HashMap<String, TextRange> identifiedTextRanges = new HashMap<>();

  public TexMECSListener() {
  }

  public Document getDocument() {
    return document;
  }

  @Override
  public void exitStartTagSet(StartTagSetContext ctx) {
    TextRange textRange = addTextRange(ctx.eid(), ctx.atts());
    openMarkup.add(textRange);
    insideTagSet = true;
  }

  @Override
  public void exitStartTag(StartTagContext ctx) {
    TextRange textRange = addTextRange(ctx.eid(), ctx.atts());
    openMarkup.add(textRange);
  }

  @Override
  public void exitText(TextContext ctx) {
    TextNode tn = new TextNode(ctx.getText());
    limen.addTextNode(tn);
    openMarkup.forEach(m -> linkTextToMarkup(tn, m));
  }

  @Override
  public void exitEndTag(EndTagContext ctx) {
    removeFromOpenMarkup(ctx.gi());
  }

  @Override
  public void exitEndTagSet(EndTagSetContext ctx) {
    insideTagSet = false;
    removeFromOpenMarkup(ctx.gi());
  }

  @Override
  public void exitSoleTag(SoleTagContext ctx) {
    TextNode tn = new TextNode("");
    limen.addTextNode(tn);

    openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    TextRange textRange = addTextRange(ctx.eid(), ctx.atts());
    linkTextToMarkup(tn, textRange);
  }

  private void linkTextToMarkup(TextNode tn, TextRange textRange) {
    limen.associateTextWithRange(tn, textRange);
    textRange.addTextNode(tn);
  }

  @Override
  public void exitSuspendTag(SuspendTagContext ctx) {
    TextRange textRange = removeFromOpenMarkup(ctx.gi());
    suspendedMarkup.add(textRange);
  }

  @Override
  public void exitResumeTag(ResumeTagContext ctx) {
    TextRange textRange = removeFromSuspendedMarkup(ctx);
    openMarkup.add(textRange);
  }

  @Override
  public void exitVirtualElement(VirtualElementContext ctx) {
    String extendedTag = ctx.eid().gi().getText() + "=" + ctx.idref().getText();
    if (identifiedTextRanges.containsKey(extendedTag)) {
      TextRange ref = identifiedTextRanges.get(extendedTag);
      TextRange textRange = addTextRange(ref.getTag(), ctx.atts());
      ref.textNodes.forEach(tn -> {
        TextNode copy = new TextNode(tn.getContent());
        limen.addTextNode(copy);
        openMarkup.forEach(m -> linkTextToMarkup(copy, m));
        linkTextToMarkup(copy, textRange);
      });
    }
  }

  private TextRange addTextRange(EidContext eid, AttsContext atts) {
    String extendedTag = eid.getText();
    return addTextRange(extendedTag, atts);
  }

  private TextRange addTextRange(String extendedTag, AttsContext atts) {
    TextRange textRange = new TextRange(limen, extendedTag);
    addAttributes(atts, textRange);
    limen.addTextRange(textRange);
    if (textRange.hasId()) {
      identifiedTextRanges.put(extendedTag, textRange);
    }
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

  private TextRange removeFromOpenMarkup(GiContext gi) {
    String tag = gi.getText();
    TextRange textRange = removeFromTextRangeStack(tag, openMarkup);
    return textRange;
  }

  private TextRange removeFromSuspendedMarkup(ResumeTagContext ctx) {
    String tag = ctx.gi().getText();
    TextRange textRange = removeFromTextRangeStack(tag, suspendedMarkup);
    return textRange;
  }

  private TextRange removeFromTextRangeStack(String extendedTag, Deque<TextRange> textRangeStack) {
    Iterator<TextRange> descendingIterator = textRangeStack.descendingIterator();
    TextRange textRange = null;
    while (descendingIterator.hasNext()) {
      textRange = descendingIterator.next();
      if (textRange.getExtendedTag().equals(extendedTag)) {
        break;
      }
    }
    textRangeStack.remove(textRange);
    return textRange;
  }

}
