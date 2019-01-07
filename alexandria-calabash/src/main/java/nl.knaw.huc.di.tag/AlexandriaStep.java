package nl.knaw.huc.di.tag;

/*-
 * #%L
 * alexandria-calabash
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.XProcURIResolver;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import nl.knaw.huc.di.tag.tagml.TAGMLSyntaxError;
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huc.di.tag.tagml.xml.exporter.XMLExporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

@XMLCalabash(
    name = "tag:load",
    type = "{https://huygensing.github.io/TAG/TAGML/ns/tag}load")

public class AlexandriaStep extends DefaultStep {
  private static final QName _tagmlfile = new QName("", "tagmlfile");
  private static final String library_xpl = "https://huygensing.github.io/TAG/TAGML/calabash-steps.xpl";
  private static final String library_url = "/nl/knaw/huc/di/tag/library.xpl";
  private static final String STEP_NAME = "tag:load";
  private WritablePipe result = null;

  public AlexandriaStep(final XProcRuntime runtime, final XAtomicStep step) {
    super(runtime, step);
  }

  public void setOutput(String port, WritablePipe pipe) {
    result = pipe;
  }

  public void reset() {
    result.resetWriter();
  }

  public void run() throws SaxonApiException {
    super.run();
    URI tagmlURI = getOption(_tagmlfile).getBaseURI().resolve(getOption(_tagmlfile).getString());
    try {
      URL url = tagmlURI.toURL();
      URLConnection connection = url.openConnection();
      InputStream stream = connection.getInputStream();
      Path tmpPath = mkTmpDir();
      TAGStore store = new TAGStore(tmpPath.toString(), false);
      store.open();
      TAGDocument document = store.runInTransaction(() -> {
        try {
          return new TAGMLImporter(store).importTAGML(stream);
        } catch (TAGMLSyntaxError se) {
//          runtime.error(se);
          throw new XProcException(se.getMessage());
        }
      });
      String xml = store.runInTransaction(() -> new XMLExporter(store).asXML(document));
      store.close();
      rmTmpDir(tmpPath);

      StringReader reader = new StringReader(xml);
      InputSource inputSource = new InputSource(reader);
      XdmNode node = runtime.parse(inputSource);
      result.write(node);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void configureStep(XProcRuntime runtime) {
    XProcURIResolver resolver = runtime.getResolver();
    URIResolver uriResolver = resolver.getUnderlyingURIResolver();
    URIResolver myResolver = new StepResolver(uriResolver);
    resolver.setUnderlyingURIResolver(myResolver);
  }

  private static class StepResolver implements URIResolver {
    Logger logger = LoggerFactory.getLogger(AlexandriaStep.class);
    URIResolver nextResolver = null;

    public StepResolver(URIResolver next) {
      nextResolver = next;
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
      try {
        URI baseURI = new URI(base);
        URI xpl = baseURI.resolve(href);
        if (library_xpl.equals(xpl.toASCIIString())) {
          Class<AlexandriaStep> alexandriaStepClass = AlexandriaStep.class;
          URL url = alexandriaStepClass.getResource(library_url);
          logger.debug("Reading library.xpl for " + STEP_NAME + " from " + url);
          InputStream s = alexandriaStepClass.getResourceAsStream(library_url);
          if (s != null) {
            return new SAXSource(new InputSource(s));
          } else {
            logger.info("Failed to read " + library_url + " for " + STEP_NAME);
          }
        }
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }

      if (nextResolver != null) {
        return nextResolver.resolve(href, base);
      } else {
        return null;
      }
    }
  }

  private Path mkTmpDir() throws IOException {
    String tmpdirProperty = System.getProperty("java.io.tmpdir");
    Path tmpPath = Paths.get(tmpdirProperty, ".alexandria");
    if (tmpPath.toFile().exists()) {
      Files.walk(tmpPath)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .filter(File::isFile)
          .forEach(File::delete);

    } else {
      tmpPath = Files.createDirectory(tmpPath);
    }
    return tmpPath;
  }

  private void rmTmpDir(final Path tmpPath) throws IOException {
    Files.walk(tmpPath)
        .map(Path::toFile)
        .forEach(File::deleteOnExit);
  }
}
