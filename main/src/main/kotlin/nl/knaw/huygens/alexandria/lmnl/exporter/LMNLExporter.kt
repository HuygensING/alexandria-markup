package nl.knaw.huygens.alexandria.lmnl.exporter

/*
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

/**
 * Created by bramb on 07/02/2017.
 */

import com.google.common.base.Preconditions
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter
import nl.knaw.huygens.alexandria.storage.*
import nl.knaw.huygens.alexandria.view.TAGView
import nl.knaw.huygens.alexandria.view.TAGViewFactory
import org.slf4j.LoggerFactory
import java.util.*

class LMNLExporter {
    private var useShorthand = false
    private val store: TAGStore
    private val view: TAGView

    constructor(store: TAGStore, view: TAGView) {
        Preconditions.checkNotNull(store)
        this.store = store
        this.view = view
    }

    constructor(store: TAGStore) {
        Preconditions.checkNotNull(store)
        this.store = store
        view = TAGViewFactory(store).defaultView
    }

    fun useShorthand(): LMNLExporter {
        useShorthand = true
        return this
    }

    fun toLMNL(document: TAGDocument?): String {
        val lmnlBuilder = StringBuilder()
        store.runInTransaction { appendLimen(lmnlBuilder, document) }
        // LOG.info("LMNL={}", lmnlBuilder);
        return lmnlBuilder.toString()
    }

    private fun appendLimen(lmnlBuilder: StringBuilder, document: TAGDocument?) {
        if (document != null) {
            val openMarkupIds: Deque<Long?> = ArrayDeque()
            val openTags: MutableMap<Long?, StringBuilder> = HashMap()
            val closeTags: MutableMap<Long?, StringBuilder> = HashMap()
            document.textNodeStream.forEach { tn: TAGTextNode ->
                val markupIds: MutableSet<Long> = HashSet()
                document.getMarkupStreamForTextNode(tn).forEach { mw: TAGMarkup ->
                    val id = mw.dbId
                    markupIds.add(id)
                    openTags.computeIfAbsent(id) { k: Long? -> toOpenTag(mw) }
                    closeTags.computeIfAbsent(id) { k: Long? -> toCloseTag(mw) }
                }
                val relevantMarkupIds: Set<Long?> = view.filterRelevantMarkup(markupIds)
                val toClose: MutableList<Long?> = ArrayList(openMarkupIds)
                toClose.removeAll(relevantMarkupIds)
                toClose.reverse()
                toClose.forEach { markupId: Long? -> lmnlBuilder.append(closeTags[markupId]) }
                val toOpen: MutableList<Long?> = ArrayList(relevantMarkupIds)
                toOpen.removeAll(openMarkupIds)
                toOpen.forEach { markupId: Long? -> lmnlBuilder.append(openTags[markupId]) }
                openMarkupIds.removeAll(toClose)
                openMarkupIds.addAll(toOpen)
                lmnlBuilder.append(tn.text)
            }
            openMarkupIds.descendingIterator()
                    .forEachRemaining { markupId: Long? -> lmnlBuilder.append(closeTags[markupId]) }
        }
    }

    private fun toCloseTag(markup: TAGMarkup): StringBuilder {
        return if (markup.isAnonymous) StringBuilder() else StringBuilder("{").append(markup.extendedTag).append("]")
    }

    private fun toOpenTag(markup: TAGMarkup): StringBuilder {
        return StringBuilder("TODO")
        //    StringBuilder tagBuilder = new StringBuilder("[").append(markup.getExtendedTag());
//    markup.getAnnotationStream().forEach(a -> tagBuilder.append(" ").append(toLMNL(a)));
//    return markup.isAnonymous()
//        ? tagBuilder.append("]")
//        : tagBuilder.append("}");
    }

    fun toLMNL(annotation: TAGAnnotation): StringBuilder {
        //    annotation.getAnnotationStream()
//        .forEach(a1 -> annotationBuilder.append(" ").append(toLMNL(a1)));
//    TAGDocument document = annotation.getDocument();
//    if (document.hasTextNodes()) {
//      annotationBuilder.append("}");
//      appendLimen(annotationBuilder, document);
//      if (useShorthand) {
//        annotationBuilder.append("{]");
//      } else {
//        annotationBuilder.append("{").append(annotation.getKey()).append("]");
//      }
//    } else {
//      annotationBuilder.append("]");
//    }
        return StringBuilder("[").append(annotation.key)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(LMNLExporter::class.java)
    }
}
