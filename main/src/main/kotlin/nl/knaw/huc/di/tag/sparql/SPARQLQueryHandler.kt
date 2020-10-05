package nl.knaw.huc.di.tag.sparql

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagml.rdf.RDFFactory
import nl.knaw.huygens.alexandria.storage.TAGDocument
import org.apache.jena.query.*
import org.apache.jena.rdf.model.Model
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets

class SPARQLQueryHandler(document: TAGDocument) {
    private val model: Model = document.store.runInTransaction<Model> { RDFFactory.fromDocument(document) }

    fun execute(sparqlQuery: String): SPARQLResult {
        val result = SPARQLResult(sparqlQuery)
        try {
            val query = QueryFactory.create(sparqlQuery)
            //    PrefixMapping prefixMapping = query.getPrefixMapping();
            val qe = QueryExecutionFactory.create(query, model)
            if (query.isSelectType) {
                val results = qe.execSelect()
                result.addValue(asTable(results, query))
            } else if (query.isAskType) {
                result.addValue(qe.execAsk())
            } else if (query.isConstructType) {
                result.addValue(asTurtle(qe.execConstruct()))
            } else if (query.isDescribeType) {
                result.addValue(asTurtle(qe.execDescribe()))
            }
        } catch (qpe: QueryParseException) {
            result.errors.add(qpe.message ?: "no message")
        }
        return result
    }

    private fun asTable(results: ResultSet, query: Query): String {
        val baos = ByteArrayOutputStream()
        ResultSetFormatter.out(baos, results, query)
        return asString(baos)
    }

    private fun asJson(results: ResultSet): String {
        val baos = ByteArrayOutputStream()
        ResultSetFormatter.outputAsJSON(baos, results)
        return asString(baos)
    }

    private fun asXML(results: ResultSet): String {
        val baos = ByteArrayOutputStream()
        ResultSetFormatter.outputAsXML(baos, results)
        return asString(baos)
    }

    private fun asTurtle(construct: Model): String {
        val baos = ByteArrayOutputStream()
        construct.write(baos, "TURTLE")
        return asString(baos)
    }

    private fun asString(baos: ByteArrayOutputStream): String {
        return try {
            baos.toString(StandardCharsets.UTF_8.name())
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException()
        }
    }

    init {
//    System.out.println("before RDF.list");
//    Resource list = RDFS.Container;
//    System.out.println("before fromDocument");
        //    System.out.println("after");
    }
}
