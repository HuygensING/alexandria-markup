package nl.knaw.huygens.alexandria.lmnl.data_model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
// Annotations can be on ranges or annotations
public class Annotation {
  private String tag;
  private final Limen limen;
  private final List<Annotation> annotations = new ArrayList<>();

  public Annotation(String tag) {
    this.tag = tag;
    this.limen = new Limen();
  }

  public Annotation(String tag, String content) {
    this(tag);
    limen.setOnlyTextNode(new TextNode(content));
  }

  public Limen value() {
    return limen;
  }

  public List<Annotation> annotations() {
    return annotations;
  }

  public Annotation addAnnotation(Annotation annotation) {
    annotations.add(annotation);
    return this;
  }

  @Override
  public String toString() {
    return "[" + tag + "}";
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getTag() {
    return tag;
  }

  public List<Annotation> getAnnotations() {
    return annotations;
  }
}

