package nl.knaw.huc.di.tag.sparql;

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

import nl.knaw.huc.di.tag.tagml.rdf.RDFFactory;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class SPARQLQueryHandler {

  private final Model model;

  public SPARQLQueryHandler(final TAGDocument document) {
    System.out.println("document=" + document.getDbId());
//    model = document.store.runInTransaction(() -> RDFFactory.fromDocument(document));
    model = RDFFactory.fromDocument(document);
    System.out.println("done!");
  }

  public SPARQLResult execute(final String sparqlQuery) {
    final SPARQLResult result = new SPARQLResult(sparqlQuery);

    try {
      Query query = QueryFactory.create(sparqlQuery);
//    PrefixMapping prefixMapping = query.getPrefixMapping();
      QueryExecution qe = QueryExecutionFactory.create(query, model);
      if (query.isSelectType()) {
        ResultSet results = qe.execSelect();
        result.addValue(asTable(results, query));

      } else if (query.isAskType()) {
        result.addValue(qe.execAsk());

      } else if (query.isConstructType()) {
        result.addValue(asTurtle(qe.execConstruct()));

      } else if (query.isDescribeType()) {
        result.addValue(asTurtle(qe.execDescribe()));
      }
    } catch (QueryParseException qpe) {
      result.getErrors().add(qpe.getMessage());
    }
    return result;
  }

  private String asTable(final ResultSet results, final Query query) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ResultSetFormatter.out(baos, results, query);
    return asString(baos);
  }

  private String asJson(final ResultSet results) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ResultSetFormatter.outputAsJSON(baos, results);
    return asString(baos);
  }

  private String asXML(final ResultSet results) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ResultSetFormatter.outputAsXML(baos, results);
    return asString(baos);
  }

  private String asTurtle(final Model construct) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    construct.write(baos, "TURTLE");
    return asString(baos);
  }

  private String asString(final ByteArrayOutputStream baos) {
    try {
      return baos.toString(StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException();
    }
  }
}
