package nl.knaw.huygens.alexandria.texmecs.importer;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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

import nl.knaw.huygens.alexandria.data_model.*;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSParser;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSParser.*;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSParserBaseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

class TexMECSListenerInMemory extends TexMECSParserBaseListener {

  Logger LOG = LoggerFactory.getLogger(getClass());

  private final Document document = new Document();
  private final Limen limen = document.value();
  private final Deque<Markup> openMarkup = new ArrayDeque<>();
  private final Deque<Markup> suspendedMarkup = new ArrayDeque<>();
  private boolean insideTagSet = false; // TODO: use this?
  private final HashMap<String, Markup> identifiedMarkups = new HashMap<>();
  private final HashMap<String, String> idsInUse = new HashMap<>();
  private final List<String> errors = new ArrayList<>();

  public TexMECSListenerInMemory() {
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
    if (markup != null) {
      suspendedMarkup.add(markup);
    }
  }

  @Override
  public void exitResumeTag(ResumeTagContext ctx) {
    Markup markup = removeFromSuspendedMarkup(ctx);
    if (markup != null) {
      openMarkup.add(markup);
    }
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
      String message = "idref '" + idref + "' not found: No <" + extendedTag.replace("=", "@") + "| tag found that this virtual element refers to.";
      errors.add(message);
    }

  }

  @Override
  public void exitDocument(TexMECSParser.DocumentContext ctx) {
    if (!openMarkup.isEmpty()) {
      String openMarkupString = openMarkup.stream()//
          .map(TexMECSListenerInMemory::startTag)//
          .collect(Collectors.joining(", "));
      String message = "Some markup was not closed: " + openMarkupString;
      errors.add(message);
    }
    if (!suspendedMarkup.isEmpty()) {
      String suspendedMarkupString = suspendedMarkup.stream()//
          .map(TexMECSListenerInMemory::suspendTag)//
          .collect(Collectors.joining(", "));
      String message = "Some suspended markup was not resumed: " + suspendedMarkupString;
      errors.add(message);
    }
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public List<String> getErrors() {
    return errors;
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
        String message = "id '" + id + "' was already used in markup <" + idsInUse.get(id) + "|.";
        errors.add(message);
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
      String message = "Closing tag |" + tag + "> found, which has no corresponding earlier opening tag.";
      errors.add(message);
    }
    return markup;
  }

  private Markup removeFromSuspendedMarkup(ResumeTagContext ctx) {
    String tag = ctx.gi().getText();
    Markup markup = removeFromMarkupStack(tag, suspendedMarkup);
    if (markup == null) {
      String message = "Resuming tag <+" + tag + "| found, which has no corresponding earlier suspending tag |-" + tag + ">.";
      errors.add(message);
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
