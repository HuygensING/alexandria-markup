package nl.knaw.huygens.alexandria.freemarker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

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
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    } catch (TemplateException e) {
      throw new RuntimeException(e);
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
