package nl.knaw.huc.di.tag.tagml.importer;

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
import static java.util.regex.Pattern.quote;
import static nl.knaw.huc.di.tag.tagml.TAGML.*;

public class ExtendedMarkupName {
  private String tagName;
  private boolean optional = false;
  private boolean resume = false;
  private boolean suspend = false;
  private String id;

  public static ExtendedMarkupName of(String text) {
    ExtendedMarkupName extendedMarkupName = new ExtendedMarkupName();
    if (text.startsWith(OPTIONAL_PREFIX)) {
      text = text.replaceFirst(quote(OPTIONAL_PREFIX), "");
      extendedMarkupName.setOptional(true);
    }
    if (text.startsWith(SUSPEND_PREFIX)) {
      text = text.replaceFirst(quote(SUSPEND_PREFIX), "");
      extendedMarkupName.setSuspend(true);
    }
    if (text.startsWith(RESUME_PREFIX)) {
      text = text.replaceFirst(quote(RESUME_PREFIX), "");
      extendedMarkupName.setResume(true);
    }
    if (text.contains("~")) {
      String[] parts = text.split("~");
      text = parts[0];
      String id = parts[1];
      extendedMarkupName.setId(id);
    }
    extendedMarkupName.setTagName(text);
    return extendedMarkupName;
  }

  public String getTagName() {
    return tagName;
  }

  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  public boolean isOptional() {
    return optional;
  }

  public void setResume(boolean resume) {
    this.resume = resume;
  }

  public boolean isResume() {
    return resume;
  }

  public void setSuspend(boolean suspend) {
    this.suspend = suspend;
  }

  public boolean isSuspend() {
    return suspend;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public String getExtendedMarkupName() {
    return id != null ? tagName + "=" + id : tagName;
  }
}
