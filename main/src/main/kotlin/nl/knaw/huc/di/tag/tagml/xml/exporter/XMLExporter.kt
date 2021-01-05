package nl.knaw.huc.di.tag.tagml.xml.exporter

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

import nl.knaw.huc.di.tag.TAGExporter
import nl.knaw.huc.di.tag.TAGTraverser
import nl.knaw.huc.di.tag.tagml.TAGML
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.view.TAGView
import org.slf4j.LoggerFactory

class XMLExporter : TAGExporter {
    constructor(store: TAGStore) : super(store)
    constructor(store: TAGStore, view: TAGView) : super(store, view)

    fun asXML(document: TAGDocument, leadingLayer: String = TAGML.DEFAULT_LAYER): String {
        val xmlBuilder = XMLBuilder(leadingLayer)
        TAGTraverser(store, view, document).accept(xmlBuilder)
        return xmlBuilder.result
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(XMLExporter::class.java)
    }
}
