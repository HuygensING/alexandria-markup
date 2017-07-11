package nl.knaw.huygens.alexandria.texmecs.importer;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.Markup;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;
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
  private Deque<Markup> openMarkup = new ArrayDeque<>();
  private Deque<Markup> suspendedMarkup = new ArrayDeque<>();
  private boolean insideTagSet = false; // TODO: use this?
  private HashMap<String, Markup> identifiedMarkups = new HashMap<>();
  private HashMap<String, String> idsInUse = new HashMap<>();

  public TexMECSListener() {
  }

  public Document getDocument() {
    return document;
  }

  @Override
  public void exitStartTagSet(StartTagSetContext ctx) {
    Markup markup = addMarkup(ctx.eid(), ctx.atts());
    openMarkup.add(markup);
    insideTagSet = true;
  }

  @Override
  public void exitStartTag(StartTagContext ctx) {
    Markup markup = addMarkup(ctx.eid(), ctx.atts());
    openMarkup.add(markup);
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
    Markup markup = addMarkup(ctx.eid(), ctx.atts());

    linkTextToMarkup(tn, markup);
  }

  private void linkTextToMarkup(TextNode tn, Markup markup) {
    limen.associateTextWithRange(tn, markup);
    markup.addTextNode(tn);
  }

  @Override
  public void exitSuspendTag(SuspendTagContext ctx) {
    Markup markup = removeFromOpenMarkup(ctx.gi());
    suspendedMarkup.add(markup);
  }

  @Override
  public void exitResumeTag(ResumeTagContext ctx) {
    Markup markup = removeFromSuspendedMarkup(ctx);
    openMarkup.add(markup);
  }

  @Override
  public void exitVirtualElement(VirtualElementContext ctx) {
    String idref = ctx.idref().getText();
    String gi = ctx.eid().gi().getText();
    String extendedTag = gi + "=" + idref;
    if (identifiedMarkups.containsKey(extendedTag)) {
      Markup ref = identifiedMarkups.get(extendedTag);
      Markup markup = addMarkup(ref.getTag(), ctx.atts());
      ref.textNodes.forEach(tn -> {
        TextNode copy = new TextNode(tn.getContent());
        limen.addTextNode(copy);
        openMarkup.forEach(m -> linkTextToMarkup(copy, m));
        linkTextToMarkup(copy, markup);
      });

    } else {
      throw new TexMECSSyntaxError("idref '" + idref + "' not found: No <" + extendedTag.replace("=", "@") + "| tag found that this virtual element refers to.");
    }

  }

  @Override
  public void exitDocument(TexMECSParser.DocumentContext ctx) {
    if (!openMarkup.isEmpty()) {
      String openMarkupString = openMarkup.stream().map(TexMECSListener::startTag).collect(Collectors.joining(", "));
      throw new TexMECSSyntaxError("Some markup was not closed: " + openMarkupString);
    }
    if (!suspendedMarkup.isEmpty()) {
      String suspendedMarkupString = suspendedMarkup.stream().map(TexMECSListener::suspendTag).collect(Collectors.joining(", "));
      throw new TexMECSSyntaxError("Some suspended markup was not resumed: " + suspendedMarkupString);
    }
  }

  private Markup addMarkup(EidContext eid, AttsContext atts) {
    String extendedTag = eid.getText();
    return addMarkup(extendedTag, atts);
  }

  private Markup addMarkup(String extendedTag, AttsContext atts) {
    Markup markup = new Markup(limen, extendedTag);
    addAttributes(atts, markup);
    limen.addMarkup(markup);
    if (markup.hasId()) {
      identifiedMarkups.put(extendedTag, markup);
      String id = markup.getId();
      if (idsInUse.containsKey(id)) {
        throw new TexMECSSyntaxError("id '" + id + "' was aleady used in markup <" + idsInUse.get(id) + "|.");
      }
      idsInUse.put(id, extendedTag);
    }
    return markup;
  }

  private void addAttributes(AttsContext attsContext, Markup markup) {
    attsContext.avs().forEach(avs -> {
      String attrName = avs.NAME_O().getText();
      String quotedAttrValue = avs.STRING().getText();
      String attrValue = quotedAttrValue.substring(1, quotedAttrValue.length() - 1); // remove single||double quotes
      Annotation annotation = new Annotation(attrName, attrValue);
      markup.addAnnotation(annotation);
    });
  }

  private Markup removeFromOpenMarkup(GiContext gi) {
    String tag = gi.getText();
    Markup markup = removeFromMarkupStack(tag, openMarkup);
    if (markup == null) {
      throw new TexMECSSyntaxError("Closing tag |" + tag + "> found, which has no corresponding earlier opening tag.");
    }
    return markup;
  }

  private Markup removeFromSuspendedMarkup(ResumeTagContext ctx) {
    String tag = ctx.gi().getText();
    Markup markup = removeFromMarkupStack(tag, suspendedMarkup);
    if (markup == null) {
      throw new TexMECSSyntaxError("Resuming tag <+" + tag + "| found, which has no corresponding earlier suspending tag |-" + tag + ">.");
    }
    return markup;
  }

  private Markup removeFromMarkupStack(String tag, Deque<Markup> markupStack) {
    Iterator<Markup> descendingIterator = markupStack.descendingIterator();
    Markup markup = null;
    while (descendingIterator.hasNext()) {
      markup = descendingIterator.next();
      if (markup.getTag().equals(tag)) {
        break;
      }
    }
    if (markup != null) {
      markupStack.remove(markup);
    }
    return markup;
  }

  private static String suspendTag(Markup m) {
    return "|-" + m.getTag() + ">";
  }

  private static String startTag(Markup m) {
    return "<" + m.getExtendedTag() + "|";
  }

}
