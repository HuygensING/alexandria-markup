package nl.knaw.huc.di.tag.tagml.rdf;

/*-
 * #%L
 * alexandria-markup-core
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
import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huc.di.tag.tagml.importer2.TAG;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RDFFactory {

  public static Model fromDocument(TAGDocument document) {
    AtomicLong resourceCounter = new AtomicLong();
    Model model = ModelFactory.createDefaultModel()
        .setNsPrefix("rdf", RDF.getURI())
//        .setNsPrefix("rdfs", RDFS.getURI())
//        .setNsPrefix("dc", DC.getURI())
        .setNsPrefix("tag", TAG.getURI());

    String documentURI = resourceURI("document", resourceCounter.getAndIncrement());
    Resource documentResource = model.createResource(documentURI).addProperty(RDF.type, TAG.Document);
    final TextGraph textGraph = document.getDTO().textGraph;

    Map<Long, String> markupURI = new HashMap<>();
    document.getMarkupStream().forEach(markup -> {
      Resource markupResource = createMarkupResource(model, markup);
      markupURI.put(markup.getDbId(), markupResource.getURI());
      List<Resource> annotations = markup.getAnnotationStream()
          .map(ai -> toAnnotationResource(model, ai))
          .collect(Collectors.toList());
      if (!annotations.isEmpty()) {
        RDFList list = model.createList(annotations.iterator());
        markupResource.addProperty(TAG.hasAnnotations, list);
      }
    });

    AtomicReference<Resource> lastTextResource = new AtomicReference<>();
    textGraph.getTextNodeIdStream()
        .map(document.store::getTextNode)
        .forEach(textNode -> {
          Resource textResource = createTextResource(model, textNode.getText(), textNode.getDbId());
          textGraph.getMarkupIdStreamForTextNodeId(textNode.getDbId())
              .forEach(markupId -> {
                Resource markupResource = model.getResource(markupURI.get(markupId));
                model.add(markupResource, TAG.marksUp, textResource);
              });
          if (lastTextResource.get() == null) {
            model.add(documentResource, TAG.firstTextNode, textResource);
          } else {
            model.add(lastTextResource.get(), TAG.next, textResource);
          }
          lastTextResource.set(textResource);
        });

    return model;
  }

  private static Resource createMarkupResource(final Model model, final TAGMarkup markup) {
    String textURI = resourceURI("markup", markup.getDbId());
    Resource resource = model.createResource(textURI)
        .addProperty(RDF.type, TAG.MarkupElement)
        .addProperty(TAG.name, markup.getTag());
    return resource;
  }

  public static Resource createTextResource(final Model model, final String text, final Long dbid) {
    String textURI = resourceURI("text", dbid);
    Resource textResource = model.createResource(textURI)
        .addProperty(RDF.type, TAG.TextNode);
    Literal content = model.createLiteral(text);
    textResource.addProperty(RDF.value, content);
    return textResource;
  }

  private static String resourceURI(final String type, final Long resourceId) {
    return URI.create(TAG.getURI() + type + resourceId).toASCIIString();
  }

  private static Resource toAnnotationResource(Model model, AnnotationInfo annotationInfo) {
    String annotationURI = resourceURI("annotation", annotationInfo.getNodeId());
    Resource resource = model.createResource(annotationURI)
        .addProperty(RDF.type, TAG.Annotation)
        .addProperty(TAG.name, annotationInfo.getName())
        ;
    return resource;
  }

}
