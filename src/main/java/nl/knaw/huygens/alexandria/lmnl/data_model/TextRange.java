package nl.knaw.huygens.alexandria.lmnl.data_model;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
public class TextRange {
  private final Limen owner;
  private final String tag;
  private String id = ""; // LMNL, should be unique
  private String suffix = ""; // TexMECS, doesn't need to be unique
  private final List<Annotation> annotations;
  private Set<TextRange> parents = new HashSet<>();
  public final List<TextNode> textNodes;

  public TextRange(Limen owner, String tag) {
    this.owner = owner;
    if (tag == null) {
      this.tag = "";

    } else if (tag.contains("~")) {
      String[] parts = tag.split("~");
      this.tag = parts[0];
      this.suffix = parts[1];

    } else if (tag.contains("=")) {
      String[] parts = tag.split("=");
      this.tag = parts[0];
      this.id = parts[1];

    } else if (tag.contains("@")) {
      String[] parts = tag.split("@");
      this.tag = parts[0];
      this.id = parts[1];

    } else {
      this.tag = tag;
    }
    this.annotations = new ArrayList<>();
    this.textNodes = new ArrayList<>();
  }

  public TextRange addTextNode(TextNode node) {
    this.textNodes.add(node);
    this.owner.associateTextWithRange(node, this);
    return this;
  }

  public TextRange addAnnotation(Annotation annotation) {
    this.annotations.add(annotation);
    return this;
  }

  public String getTag() {
    return tag;
  }

  public String getExtendedTag() {
    if (StringUtils.isNotEmpty(suffix)) {
      return tag + "~" + suffix;
    }
    if (StringUtils.isNotEmpty(id)) {
      return tag + "=" + id;
    }
    return tag;
  }

  public String getId() {
    return id;
  }

  public String getSuffix() {
    return suffix;
  }

  public TextRange setFirstAndLastTextNode(TextNode firstTextNode, TextNode lastTextNode) {
    this.textNodes.clear();
    addTextNode(firstTextNode);
    if (firstTextNode != lastTextNode) {
      TextNode next = firstTextNode.getNextTextNode();
      while (next != lastTextNode) {
        addTextNode(next);
        next = next.getNextTextNode();
      }
      addTextNode(next);
    }
    return this;
  }

  public TextRange setOnlyTextNode(TextNode textNode) {
    this.textNodes.clear();
    addTextNode(textNode);
    return this;
  }

  public List<Annotation> getAnnotations() {
    return annotations;
  }

  public boolean isAnonymous() {
    return textNodes.size() == 1 && "".equals(textNodes.get(0).getContent());
  }

  public boolean hasId() {
    return StringUtils.isNotEmpty(id);
  }

  public boolean hasSuffix() {
    return StringUtils.isNotEmpty(suffix);
  }

  public void joinWith(TextRange textRange) {
    this.textNodes.addAll(textRange.textNodes);
    textRange.textNodes.forEach(tn -> {
      owner.disAssociateTextWithRange(tn, textRange);
      owner.associateTextWithRange(tn, this);
    });
    this.annotations.addAll(textRange.getAnnotations());
  }

  public boolean isContinuous() {
    boolean isContinuous = true;
    TextNode textNode = textNodes.get(0);
    TextNode expectedNext = textNode.getNextTextNode();
    for (int i = 1; i < textNodes.size(); i++) {
      textNode = textNodes.get(i);
      if (!textNode.equals(expectedNext)) {
        isContinuous = false;
        break;
      }
      expectedNext = textNode.getNextTextNode();
    }
    return isContinuous;
  }

  public void addParent(TextRange parent) {
    parents.add(parent);
  }

  public void removeParent(TextRange parent) {
    parents.remove(parent);
  }

  public Set<TextRange> getParents() {
    return parents;
  }

}
