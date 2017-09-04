package nl.knaw.huygens.alexandria.lmnl.data_model;

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

