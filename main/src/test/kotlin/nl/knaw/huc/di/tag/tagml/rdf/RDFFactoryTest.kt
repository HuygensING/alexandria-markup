package nl.knaw.huc.di.tag.tagml.rdf

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

import nl.knaw.huc.di.tag.TAGBaseStoreTest
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter
import nl.knaw.huc.di.tag.tagml.importer2.TAG
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSetFormatter
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.*

class RDFFactoryTest : TAGBaseStoreTest() {
    @Test
    fun testBasic() {
        val tagml = "[line>The rain in Spain falls mainly on the plain.<line]"
        testRDFConversionContains(tagml,
                "")
        //        "tag:document0  a   tag:Document ;\n" +
//            "        tag:layer  tag:layer_ ;\n" +
//            "        tag:root   tag:markup2 .",
//        "tag:layer_  a           tag:LayerNode ;\n" +
//            "        tag:layer_name  \"\" .",
//        "tag:markup2  a           tag:MarkupNode ;\n" +
//            "        tag:elements     ( tag:text3 ) ;\n" +
//            "        tag:layer        tag:layer_ ;\n" +
//            "        tag:markup_name  \"line\" .",
//        "tag:text3  a         tag:TextNode ;\n" +
//            "        tag:content  \"The rain in Spain falls mainly on the plain.\" .");
    }

    @Test
    fun testLayers() {
        val tagml = "[line|+A>Cookie Monster likes cookies.<line|A]"
        testRDFConversionContains(tagml,
                "tag:content  \"Cookie Monster likes cookies.\"")
        //        "tag:document0  a   tag:Document ;\n" +
//            "        tag:layer  tag:layer_A ;\n" +
//            "        tag:root   tag:markup2 .",
//        "tag:layer_A  a          tag:LayerNode ;\n" +
//            "        tag:layer_name  \"A\" .",
//        "tag:markup2  a           tag:MarkupNode ;\n" +
//            "        tag:elements     ( tag:text3 ) ;\n" +
//            "        tag:layer        tag:layer_A ;\n" +
//            "        tag:markup_name  \"line\" .",
//        "tag:text3  a         tag:TextNode ;\n" +
//            "        tag:content  \"Cookie Monster likes cookies.\" .");
    }

    @Test
    fun testAnnotations() {
        val tagml = "[line month_1='November' month_2=11>In the eleventh month...<line]"
        testRDFConversionContains(tagml, "")
    }

    @Test
    fun testBooleanAnnotation() {
        val tagml = "[line rhyme=false>There once was a vicar in [place real=true>Slough<place]<line]"
        testRDFConversionContains(tagml, "tag:value            true",
                "tag:value            false")
    }

    @Test
    fun testListAnnotation() {
        val tagml = "[line>Donald and [group names=['Huey','Louie','Dewey']>his nephews<group] went for a ride.<line]"
        testRDFConversionContains(tagml, "tag:value  \"Huey\"",
                "tag:value  \"Louie\"",
                "tag:value  \"Dewey\"")
    }

    @Test
    fun testMapAnnotation() {
        val tagml = "[quote>Van de maan af gezien zijn we allen even groot.\n[by>[alias person={first_name='Eduard' middle_name='Douwes' last_name='Dekker'}>Multatuli<alias]<by]<quote]"
        testRDFConversionContains(tagml, "tag:content  \"Van de maan af gezien zijn we allen even groot.\\n\"")
    }

    @Test
    fun testRichTextAnnotation() {
        val tagml = "[text>Hello, my name is [gloss addition=[>[p>that’s [qualifier>Mrs.<qualifier] to you<p]<]>Doubtfire. How do you do?<gloss]<text]"
        testRDFConversionContains(tagml, "")
    }

    @Test
    fun testReferenceAnnotation() {
        val tagml = "[text meta={persons=[{:id=huyg0001 name='Constantijn Huygens'}]}>[title>De Zee-Straet<title] door [author pers->huyg0001>Constantijn Huygens<author] ....... <text]"
        testRDFConversionContains(tagml, "")
    }

    @Test
    fun testOverlap() {
        val tagml = "[line>[a|+A>Cookie Monster [b|+B>likes<a|A] cookies.<b|B]<line]"
        testRDFConversionContains(tagml, "")
    }

    @Test
    fun testHierarchical() {
        val tagml = "[line>[a>Cookie Monster [b>likes<b] cookies.<a]<line]"
        testRDFConversionContains(tagml, "")
    }

    @Test
    fun testDiscontinuity() {
        val tagml = "[ex>[q>and what is the use of a book,<-q] thought Alice[+q>without pictures or conversation?<q]<ex]"
        testRDFConversionContains(tagml, "")
    }

    @Test
    fun testNonLinearity() {
        val tagml = "[q>To be, or <|to be not|not to be|>.<q]"
        testRDFConversionContains(tagml, "")
    }

    @Test
    fun loadOntology() {
        val model = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM)
        model.read(TAG.NS)
        println(DotFactory.fromModel(model))
    }

    private fun testRDFConversionContains(tagmlBody: String, vararg turtleStatements: String) {
        val tagml = addTAGMLHeader(tagmlBody)
        runInStore { store: TAGStore ->
            val document = store.runInTransaction<TAGDocument> { TAGMLImporter(store).importTAGML(tagml.trim { it <= ' ' }) }
            val model = store.runInTransaction<Model> { RDFFactory.fromDocument(document) }

//      String queryString = "prefix tag: <" + TAG.getURI() + "> "
//          + "select ?s ?o "
//          + "where { ?s <" + TAG.markupName + "> ?o . }";
//      System.out.println(queryString);
//      List<Map<String, String>> resultList = getResults(model, queryString);
//      System.out.println(resultList);
            val dot = DotFactory.fromModel(model)
            println("\n------------TTL------------------------------------------------------------------------------------\n")
            model.write(System.out, "TURTLE")
            println("\n------------TTL------------------------------------------------------------------------------------\n")
            println("\n------------8<------------------------------------------------------------------------------------\n")
            println(dot)
            println("\n------------8<------------------------------------------------------------------------------------\n")
            val baos: OutputStream = ByteArrayOutputStream()
            model.write(baos, "TURTLE")
            val ttl = baos.toString()
            assertThat(ttl).contains(*turtleStatements)
        }
    }

    private fun getResults(model: Model, queryString: String): List<Map<String, String>> {
        val query = QueryFactory.create(queryString)
        val prefixMapping = query.prefixMapping
        val qe = QueryExecutionFactory.create(query, model)
        //    if (query.isSelectType()){
        val results = qe.execSelect()
        //    boolean b = qe.execAsk();
//    Model model1 = qe.execConstruct();
//    Model model2 = qe.execDescribe();
        println("----------------------------------------------------------------")
        ResultSetFormatter.out(System.out, results, query)
        println("----------------------------------------------------------------")
        ResultSetFormatter.outputAsJSON(QueryExecutionFactory.create(query, model).execSelect())
        println("----------------------------------------------------------------")
        ResultSetFormatter.outputAsCSV(QueryExecutionFactory.create(query, model).execSelect())
        println("----------------------------------------------------------------")
        ResultSetFormatter.outputAsTSV(QueryExecutionFactory.create(query, model).execSelect())
        println("----------------------------------------------------------------")
        ResultSetFormatter.outputAsXML(QueryExecutionFactory.create(query, model).execSelect())
        println("----------------------------------------------------------------")
        val resultList: MutableList<Map<String, String>> = ArrayList()
        while (results.hasNext()) {
            val querySolution = results.next()
            val result: MutableMap<String, String> = HashMap()
            querySolution.varNames().forEachRemaining { varName: String ->
                val node = querySolution[varName]
                val `val` = if (node.isResource) "<" + prefixMapping.shortForm(node.asResource().uri) + ">" else node.toString()
                result[varName] = `val`
            }
            resultList.add(result)
        }
        qe.close()
        return resultList
    }
}
