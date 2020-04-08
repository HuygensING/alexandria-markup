package nl.knaw.huygens.alexandria.texmecs.importer;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSParser;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSParser.*;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSParserBaseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

class TexMECSListener extends TexMECSParserBaseListener {

  Logger LOG = LoggerFactory.getLogger(getClass());

  private final TAGDocument document;
  private final Deque<TAGMarkup> openMarkup = new ArrayDeque<>();
  private final Deque<TAGMarkup> suspendedMarkup = new ArrayDeque<>();
  private boolean insideTagSet = false; // TODO: use this?
  private final HashMap<String, TAGMarkup> identifiedMarkups = new HashMap<>();
  private final HashMap<String, String> idsInUse = new HashMap<>();
  private final List<String> errors = new ArrayList<>();
  private final TAGStore store;

  public TexMECSListener(TAGStore store) {
    this.store = store;
    document = store.createDocument();
  }

  public TAGDocument getDocument() {
    return document;
  }

  @Override
  public void exitStartTag(StartTagContext ctx) {
    TAGMarkup markup = addMarkup(ctx.eid(), ctx.atts());
    openMarkup.add(markup);
  }

  @Override
  public void exitStartTagSet(StartTagSetContext ctx) {
    TAGMarkup markup = addMarkup(ctx.eid(), ctx.atts());
    openMarkup.add(markup);
    insideTagSet = true;
  }

  @Override
  public void exitText(TextContext ctx) {
    TAGTextNode tn = store.createTextNode(ctx.getText());
    document.addTextNode(tn, null);
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
    TAGTextNode tn = store.createTextNode("");
    document.addTextNode(tn, null);
    openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    TAGMarkup markup = addMarkup(ctx.eid(), ctx.atts());
    linkTextToMarkup(tn, markup);
  }

  private void linkTextToMarkup(TAGTextNode tn, TAGMarkup markup) {
    markup.getLayers()
        .forEach(layerName -> document.associateTextNodeWithMarkupForLayer(tn, markup, layerName));
  }

  @Override
  public void exitSuspendTag(SuspendTagContext ctx) {
    TAGMarkup markup = removeFromOpenMarkup(ctx.gi());
    if (markup != null) {
      suspendedMarkup.add(markup);
    }
  }

  @Override
  public void exitResumeTag(ResumeTagContext ctx) {
    TAGMarkup markup = removeFromSuspendedMarkup(ctx);
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
      TAGMarkup ref = identifiedMarkups.get(extendedTag);
      TAGMarkup markup = addMarkup(ref.getTag(), ctx.atts());
      ref.getTextNodeStream().forEach(tn -> {
        TAGTextNode copy = store.createTextNode(tn.getText());
        document.addTextNode(copy, null);
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
          .map(TexMECSListener::startTag)//
          .collect(Collectors.joining(", "));
      String message = "Some markup was not closed: " + openMarkupString;
      errors.add(message);
    }
    if (!suspendedMarkup.isEmpty()) {
      String suspendedMarkupString = suspendedMarkup.stream()//
          .map(TexMECSListener::suspendTag)//
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

  private TAGMarkup addMarkup(EidContext eid, AttsContext atts) {
    String extendedTag = eid.getText();
    return addMarkup(extendedTag, atts);
  }

  private TAGMarkup addMarkup(String extendedTag, AttsContext atts) {
    TAGMarkup markup = store.createMarkup(document, extendedTag);
    addAttributes(atts, markup);
    document.addMarkup(markup);
    if (markup.hasMarkupId()) {
      identifiedMarkups.put(extendedTag, markup);
      String id = markup.getMarkupId();
      if (idsInUse.containsKey(id)) {
        String message = "id '" + id + "' was already used in markup <" + idsInUse.get(id) + "|.";
        errors.add(message);
      }
      idsInUse.put(id, extendedTag);
    }
    return markup;
  }

  private void addAttributes(AttsContext attsContext, TAGMarkup markup) {
    attsContext.avs().forEach(avs -> {
      String attrName = avs.NAME_O().getText();
      String quotedAttrValue = avs.STRING().getText();
      String attrValue = quotedAttrValue.substring(1, quotedAttrValue.length() - 1); // remove single||double quotes
//      TAGAnnotation annotation = store.createStringAnnotation(attrName, attrValue);
//      markup.addAnnotation(annotation);
    });
  }

  private TAGMarkup removeFromOpenMarkup(GiContext gi) {
    String tag = gi.getText();
    TAGMarkup markup = removeFromMarkupStack(tag, openMarkup);
    if (markup == null) {
      String message = "Closing tag |" + tag + "> found, which has no corresponding earlier opening tag.";
      errors.add(message);
    }
    return markup;
  }

  private TAGMarkup removeFromSuspendedMarkup(ResumeTagContext ctx) {
    String tag = ctx.gi().getText();
    TAGMarkup markup = removeFromMarkupStack(tag, suspendedMarkup);
    if (markup == null) {
      String message = "Resuming tag <+" + tag + "| found, which has no corresponding earlier suspending tag |-" + tag + ">.";
      errors.add(message);
    }
    return markup;
  }

  private TAGMarkup removeFromMarkupStack(String tag, Deque<TAGMarkup> markupStack) {
    Iterator<TAGMarkup> descendingIterator = markupStack.descendingIterator();
    TAGMarkup markup = null;
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

  private static String suspendTag(TAGMarkup m) {
    return "|-" + m.getTag() + ">";
  }

  private static String startTag(TAGMarkup m) {
    return "<" + m.getExtendedTag() + "|";
  }

}
