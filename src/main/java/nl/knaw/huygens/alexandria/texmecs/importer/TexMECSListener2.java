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

import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser.*;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParserBaseListener;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class TexMECSListener2 extends TexMECSParserBaseListener {

  Logger LOG = LoggerFactory.getLogger(getClass());

  private DocumentWrapper document;
  private Deque<MarkupWrapper> openMarkup = new ArrayDeque<>();
  private Deque<MarkupWrapper> suspendedMarkup = new ArrayDeque<>();
  private boolean insideTagSet = false; // TODO: use this?
  private HashMap<String, MarkupWrapper> identifiedMarkups = new HashMap<>();
  private HashMap<String, String> idsInUse = new HashMap<>();
  private List<String> errors = new ArrayList<>();
  private TAGStore store;

  public TexMECSListener2(TAGStore store) {
    this.store = store;
    document = store.createDocumentWrapper();
  }

  public DocumentWrapper getDocument() {
    return document;
  }

  @Override
  public void exitStartTag(StartTagContext ctx) {
    MarkupWrapper markup = addMarkup(ctx.eid(), ctx.atts());
    openMarkup.add(markup);
  }

  @Override
  public void exitStartTagSet(StartTagSetContext ctx) {
    MarkupWrapper markup = addMarkup(ctx.eid(), ctx.atts());
    openMarkup.add(markup);
    insideTagSet = true;
  }


  @Override
  public void exitText(TextContext ctx) {
    TextNodeWrapper tn = store.createTextNodeWrapper(ctx.getText());
    document.addTextNode(tn);
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
    TextNodeWrapper tn = store.createTextNodeWrapper("");
    document.addTextNode(tn);
    openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    MarkupWrapper markup = addMarkup(ctx.eid(), ctx.atts());
    linkTextToMarkup(tn, markup);
  }

  private void linkTextToMarkup(TextNodeWrapper tn, MarkupWrapper markup) {
    document.associateTextNodeWithMarkup(tn, markup);
    markup.addTextNode(tn);
  }

  @Override
  public void exitSuspendTag(SuspendTagContext ctx) {
    MarkupWrapper markup = removeFromOpenMarkup(ctx.gi());
    if (markup != null) {
      suspendedMarkup.add(markup);
    }
  }

  @Override
  public void exitResumeTag(ResumeTagContext ctx) {
    MarkupWrapper markup = removeFromSuspendedMarkup(ctx);
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
      MarkupWrapper ref = identifiedMarkups.get(extendedTag);
      MarkupWrapper markup = addMarkup(ref.getTag(), ctx.atts());
      ref.getTextNodeStream().forEach(tn -> {
        TextNodeWrapper copy = store.createTextNodeWrapper(tn.getText());
        document.addTextNode(copy);
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
          .map(TexMECSListener2::startTag)//
          .collect(Collectors.joining(", "));
      String message = "Some markup was not closed: " + openMarkupString;
      errors.add(message);
    }
    if (!suspendedMarkup.isEmpty()) {
      String suspendedMarkupString = suspendedMarkup.stream()//
          .map(TexMECSListener2::suspendTag)//
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

  private MarkupWrapper addMarkup(EidContext eid, AttsContext atts) {
    String extendedTag = eid.getText();
    return addMarkup(extendedTag, atts);
  }

  private MarkupWrapper addMarkup(String extendedTag, AttsContext atts) {
    MarkupWrapper markup = store.createMarkupWrapper(document, extendedTag);
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

  private void addAttributes(AttsContext attsContext, MarkupWrapper markup) {
    attsContext.avs().forEach(avs -> {
      String attrName = avs.NAME_O().getText();
      String quotedAttrValue = avs.STRING().getText();
      String attrValue = quotedAttrValue.substring(1, quotedAttrValue.length() - 1); // remove single||double quotes
      AnnotationWrapper annotation = store.createAnnotationWrapper(attrName, attrValue);
      markup.addAnnotation(annotation);
    });
  }

  private MarkupWrapper removeFromOpenMarkup(GiContext gi) {
    String tag = gi.getText();
    MarkupWrapper markup = removeFromMarkupStack(tag, openMarkup);
    if (markup == null) {
      String message = "Closing tag |" + tag + "> found, which has no corresponding earlier opening tag.";
      errors.add(message);
    }
    return markup;
  }

  private MarkupWrapper removeFromSuspendedMarkup(ResumeTagContext ctx) {
    String tag = ctx.gi().getText();
    MarkupWrapper markup = removeFromMarkupStack(tag, suspendedMarkup);
    if (markup == null) {
      String message = "Resuming tag <+" + tag + "| found, which has no corresponding earlier suspending tag |-" + tag + ">.";
      errors.add(message);
    }
    return markup;
  }

  private MarkupWrapper removeFromMarkupStack(String tag, Deque<MarkupWrapper> markupStack) {
    Iterator<MarkupWrapper> descendingIterator = markupStack.descendingIterator();
    MarkupWrapper markup = null;
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

  private static String suspendTag(MarkupWrapper m) {
    return "|-" + m.getTag() + ">";
  }

  private static String startTag(MarkupWrapper m) {
    return "<" + m.getExtendedTag() + "|";
  }

}
