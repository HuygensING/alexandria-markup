package nl.knaw.huc.di.tag.tagml.exporter;

import nl.knaw.huygens.alexandria.StreamUtil;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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
public class TAGMLExporter {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLExporter.class);

  static class DocumentTextIterator implements Iterator<String> {

    public DocumentTextIterator(final DocumentWrapper document) {
      document.getFirstTextNode();
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public String next() {
      return null;
    }

  }

  // TODO
  public Stream<String> stream(DocumentWrapper document) {
    final Iterator<String> stringIterator = new DocumentTextIterator(document);
    return StreamUtil.stream(stringIterator);
  }

  public List<String> method(DocumentWrapper document) {
    List<String> text = new ArrayList<>();
    Deque<TextNodeWrapper> nextNodes = new LinkedList<>();
    nextNodes.add(document.getFirstTextNode());
    Set<TextNodeWrapper> processedNodes = new HashSet<>();
    while (!nextNodes.isEmpty()) {
      final TextNodeWrapper nodeWrapper = nextNodes.pop();
      if (nodeWrapper != null) {
        LOG.info("text=<{}>", nodeWrapper.getText());
        if (!processedNodes.contains(nodeWrapper)) {
          TAGTextNode textNode = nodeWrapper.getTextNode();
          String content = nodeWrapper.getText();
          text.add(content);
          switch (textNode.getType()) {
            case plaintext:
              nextNodes.addFirst(nodeWrapper.getNextTextNode());
              break;
            case divergence:
              nodeWrapper.getNextTextNodes().forEach(nextNodes::add);
              break;
            case convergence:
              nextNodes.add(nodeWrapper.getNextTextNode());
              break;
          }
          processedNodes.add(nodeWrapper);
        }
      }
    }
    return text;
  }
}
