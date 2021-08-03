package nl.knaw.huygens.alexandria.data_model;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 *
 * <p>A text node has textual content associated with it (as a UNICODE string). A text node can also
 * have multiple ranges on it... (see the Limen)
 */
public class TextNode {
  private String content = "";
  private TextNode previousTextNode = null; // for the leftmost (first) TextNode, this is null;
  private TextNode nextTextNode = null; // for the rightmost (last) TextNode, this is null;

  public TextNode(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public TextNode setPreviousTextNode(TextNode previousTextNode) {
    this.previousTextNode = previousTextNode;
    if (previousTextNode != null && previousTextNode.getNextTextNode() == null) {
      previousTextNode.setNextTextNode(this);
    }
    return this;
  }

  public TextNode getPreviousTextNode() {
    return previousTextNode;
  }

  public TextNode setNextTextNode(TextNode nextTextNode) {
    this.nextTextNode = nextTextNode;
    if (nextTextNode != null && nextTextNode.getPreviousTextNode() == null) {
      nextTextNode.setPreviousTextNode(this);
    }
    return this;
  }

  public TextNode getNextTextNode() {
    return nextTextNode;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "\"" + content.replace("\n", "\\n") + "\"";
  }
}
