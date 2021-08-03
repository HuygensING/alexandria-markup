package nl.knaw.huc.di.tag.tagml.rdf;

/*-
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class DotFactory {

  public static String fromModel(Model model) {
    StringBuilder dotBuilder =
        new StringBuilder("digraph KnowledgeBase{\n")
            .append("//graph [rankdir=LR]\n")
            .append("node [style=\"filled\";fillcolor=\"white\"]\n");
    StmtIterator stmtIterator = model.listStatements();
    AtomicInteger nodeCounter = new AtomicInteger();
    AtomicInteger edgeCounter = new AtomicInteger();
    Map<Resource, Integer> resource2nodenum = new HashMap<>();
    Map<String, String> nsPrefixMap = model.getNsPrefixMap();
    while (stmtIterator.hasNext()) {
      final Statement statement = stmtIterator.nextStatement();
      //      System.out.println(statement);
      Resource resource = statement.getSubject();
      int objectNum =
          processResource(dotBuilder, nodeCounter, resource2nodenum, nsPrefixMap, resource);

      RDFNode object = statement.getObject();
      int subjectNum = 0;
      if (object.isLiteral()) {
        subjectNum = nodeCounter.getAndIncrement();
        dotBuilder
            .append("node")
            .append(subjectNum)
            .append(" [shape=box;color=green;label=\"")
            .append(object.asLiteral().getString().replaceAll("\\s+", " "))
            .append("\"]\n");
      } else if (object.isResource()) {
        resource = object.asResource();
        subjectNum =
            processResource(dotBuilder, nodeCounter, resource2nodenum, nsPrefixMap, resource);
      }

      Property predicate = statement.getPredicate();
      int edgeNum = edgeCounter.getAndIncrement();
      String label = compactURI(predicate.getURI(), nsPrefixMap);
      dotBuilder
          .append("node")
          .append(objectNum)
          .append("->")
          .append("node")
          .append(subjectNum)
          .append(" [label=\"")
          .append(label)
          .append("\"]\n");
    }
    dotBuilder.append("}");
    return dotBuilder.toString();
  }

  private static int processResource(
      final StringBuilder dotBuilder,
      final AtomicInteger nodeCounter,
      final Map<Resource, Integer> resource2nodenum,
      final Map<String, String> nsPrefixMap,
      final Resource resource) {
    final int subjectNum;
    if (resource2nodenum.containsKey(resource)) {
      subjectNum = resource2nodenum.get(resource);
    } else {
      subjectNum = nodeCounter.getAndIncrement();
      String label = label(resource, nsPrefixMap);
      dotBuilder
          .append("node")
          .append(subjectNum)
          .append(" [label=\"")
          .append(label)
          .append("\"]\n");
      resource2nodenum.put(resource, subjectNum);
    }
    return subjectNum;
  }

  private static String label(final Resource resource, final Map<String, String> nsPrefixMap) {
    return resource.isAnon() ? "" : compactURI(resource.getURI(), nsPrefixMap);
  }

  private static String compactURI(final String uri, final Map<String, String> nsPrefixMap) {
    for (String ns : nsPrefixMap.keySet()) {
      String prefix = nsPrefixMap.get(ns);
      if (uri.startsWith(prefix)) {
        return uri.replace(prefix, ns + ":");
      }
    }
    return uri;
  }
}
