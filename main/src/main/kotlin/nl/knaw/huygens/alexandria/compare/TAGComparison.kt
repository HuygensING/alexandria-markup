package nl.knaw.huygens.alexandria.compare

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

import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.view.TAGView
import prioritised_xml_collation.*
import java.util.*
import java.util.stream.Collectors

class TAGComparison(originalDocument: TAGDocument?, tagView: TAGView?, otherDocument: TAGDocument?) {
    private val diffLines: MutableList<String> = ArrayList()
    fun getDiffLines(): List<String> {
        return diffLines
    }

    fun hasDifferences(): Boolean {
        return diffLines.isNotEmpty()
    }

    private fun handleOmission(segment: Segment) {
        for (l: String in asLines(segment.tokensWa)) {
            diffLines.add("-$l")
        }
    }

    private fun handleAddition(segment: Segment) {
        for (l: String in asLines(segment.tokensWb)) {
            diffLines.add("+$l")
        }
    }

    private fun handleReplacement(segment: Segment) {
        handleOmission(segment)
        handleAddition(segment)
    }

    private fun handleAligned(segment: Segment) {
        val lines = asLines(segment.tokensWa)
        diffLines.add(" " + lines[0])
        if (lines.size > 2) {
            diffLines.add(" ...")
        }
        if (lines.size > 1) {
            val last = lines.size - 1
            diffLines.add(" " + lines[last])
        }
    }

    private fun asLines(tagTokens: List<TAGToken>): List<String> {
        return listOf(*tagTokens.stream()
                .map { tagToken: TAGToken -> tokenContent(tagToken) }
                .collect(Collectors.joining(""))
                .split("\n".toRegex()).toTypedArray())
    }

    private fun tokenContent(tagToken: TAGToken): String {
        return (tagToken as? MarkupCloseToken)?.toString()?.replace("/", "") ?: tagToken.toString()
    }

    init {
        val originalTokens = Tokenizer(originalDocument, tagView).tagTokens
        val editedTokens = Tokenizer(otherDocument, tagView).tagTokens
        val segmenter: SegmenterInterface = AlignedNonAlignedSegmenter()
        val segments = TypeAndContentAligner().alignTokens(originalTokens, editedTokens, segmenter)
        if (segments.size > 1) {
            for (segment in segments) {
                when (segment.type) {
                    Segment.Type.aligned -> handleAligned(segment)
                    Segment.Type.addition -> handleAddition(segment)
                    Segment.Type.omission -> handleOmission(segment)
                    Segment.Type.replacement -> handleReplacement(segment)
                    else -> throw RuntimeException("unexpected type:" + segment.type)
                }
            }
        }
    }
}
