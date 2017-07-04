package nl.knaw.huygens.alexandria.freemarker;

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


import freemarker.template.*;

import java.io.*;

public class FreeMarker {
  private static final Version VERSION = Configuration.VERSION_2_3_25;
  private static final Configuration FREEMARKER = new Configuration(VERSION);

  static {
    FREEMARKER.setObjectWrapper(new DefaultObjectWrapper(VERSION));
  }

  public static String templateToString(String fmTemplate, Object fmRootMap, Class<?> clazz) {
    StringWriter out = new StringWriter();
    return processTemplate(fmTemplate, fmRootMap, clazz, out);
  }

  private static String processTemplate(String fmTemplate, Object fmRootMap, Class<?> clazz, Writer out) {
    try {
      FREEMARKER.setClassForTemplateLoading(clazz, "");
      Template template = FREEMARKER.getTemplate(fmTemplate);
      template.setOutputEncoding("UTF-8");
      template.process(fmRootMap, out);
      return out.toString();
    } catch (IOException | TemplateException e1) {
      throw new RuntimeException(e1);
    }
  }

  public static String templateToFile(String fmTemplate, File file, Object fmRootMap, Class<?> clazz) {
    try {
      FileWriter out = new FileWriter(file);
      return processTemplate(fmTemplate, fmRootMap, clazz, out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
