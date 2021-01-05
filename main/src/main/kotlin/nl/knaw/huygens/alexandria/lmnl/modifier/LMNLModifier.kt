package nl.knaw.huygens.alexandria.lmnl.modifier

/*
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

import nl.knaw.huygens.alexandria.data_model.Limen
import nl.knaw.huygens.alexandria.data_model.Markup
import nl.knaw.huygens.alexandria.data_model.TextNode
import org.slf4j.LoggerFactory
import kotlin.math.min

internal class LMNLModifier(private val limen: Limen) {
    private val log = LoggerFactory.getLogger(LMNLModifier::class.java)

    fun addMarkup(newMarkup: Markup, position: Position) {
        // logTextNodes(limen.textNodeList);
        // logMarkups(limen.markupList);
        val cursor = TextNodeCursor(limen)
        val startOffset = position.offset
        val endOffset = position.offset + position.length
        // get to starting TextNode
        // logTextNodes(limen.textNodeList);
        val findEndingTextNode = handleStartingTextNode(newMarkup, cursor, startOffset)
        // logMarkups(limen.markupList);
        // logTextNodes(limen.textNodeList);
        if (findEndingTextNode) {
            handleEndingTextNode(newMarkup, cursor, endOffset)
        }
        // logTextNodes(limen.textNodeList);
        // limen.markupList.add(newMarkup);
        // logMarkups(limen.markupList);
    }

    private fun handleStartingTextNode(newMarkup: Markup, cursor: TextNodeCursor, startOffset: Int): Boolean {
        var findStartingTextNode = true
        var findEndingTextNode = true
        while (findStartingTextNode) {
            val currentText = cursor.currentText
            val currentTextNodeLength = cursor.currentTextLength
            val offsetAtEndOfCurrentTextNode: Int = cursor.offsetAtEndOfCurrentTextNode
            if (startOffset < offsetAtEndOfCurrentTextNode) {
                // newMarkup starts in this TextNode
                val tailLength = min(offsetAtEndOfCurrentTextNode, offsetAtEndOfCurrentTextNode - startOffset)
                val headLength = currentTextNodeLength - tailLength
                if (headLength == 0) {
                    // newMarkup exactly covers current TextNode
                    newMarkup.addTextNode(cursor.currentTextNode)
                    limen.associateTextWithRange(cursor.currentTextNode, newMarkup)
                    findEndingTextNode = false
                } else {
                    if (tailLength > 0) {
                        // detach tail
                        val headText = currentText.substring(0, headLength)
                        val tailText = currentText.substring(headLength)
                        cursor.currentTextNode.content = headText
                        val newTailNode = TextNode(tailText)
                        val nextTextNode = cursor.currentTextNode.nextTextNode
                        newTailNode.previousTextNode = cursor.currentTextNode
                        newTailNode.nextTextNode = nextTextNode
                        if (nextTextNode != null) {
                            nextTextNode.previousTextNode = newTailNode
                        }
                        cursor.currentTextNode.nextTextNode = newTailNode
                        limen.getMarkups(cursor.currentTextNode).forEach { tr: Markup ->
                            tr.addTextNode(newTailNode)
                            limen.associateTextWithRange(newTailNode, tr)
                        }
                        newMarkup.addTextNode(newTailNode)
                        limen.associateTextWithRange(newTailNode, newMarkup)
                        limen.textNodeList.add(cursor.textNodeIndex + 1, newTailNode)
                    } else {
                        // newMarkup.addTextNode(cursor.getCurrentTextNode());
                        // limen.associateTextNodeWithMarkupForLayer(cursor.getCurrentTextNode(), newMarkup);
                        throw RuntimeException("tail=empty!")
                    }
                }
                findStartingTextNode = false
            } else {
                findStartingTextNode = cursor.canAdvance()
            }
            cursor.advance()
        }
        return findEndingTextNode
    }

    private fun handleEndingTextNode(newMarkup: Markup, cursor: TextNodeCursor, endOffset: Int) {
        var findEndingTextNode = true
        while (findEndingTextNode) {
            val offsetAtEndOfCurrentTextNode: Int = cursor.offsetAtEndOfCurrentTextNode
            if (offsetAtEndOfCurrentTextNode < endOffset) {
                // this is not the TextNode where newMarkup ends, but it is part of newMarkup
                limen.associateTextWithRange(cursor.currentTextNode, newMarkup)
                findEndingTextNode = cursor.canAdvance()
                cursor.advance()
            } else {
                // this is the TextNode where newMarkup ends
                val tailLength = offsetAtEndOfCurrentTextNode - endOffset
                val headLength = cursor.currentTextLength - tailLength
                if (tailLength > 0) {
                    if (headLength > 0) {
                        // detach tail
                        val headText = cursor.currentText.substring(0, headLength)
                        val tailText = cursor.currentText.substring(headLength)
                        cursor.currentTextNode.content = headText
                        val newTailNode = TextNode(tailText)
                        val nextTextNode = cursor.currentTextNode.nextTextNode
                        newTailNode.nextTextNode = nextTextNode
                        newTailNode.previousTextNode = cursor.currentTextNode
                        cursor.currentTextNode.nextTextNode = newTailNode
                        limen.getMarkups(cursor.currentTextNode)
                                .stream()
                                .filter { tr: Markup -> newMarkup != tr }
                                .forEach { tr: Markup ->
                                    limen.associateTextWithRange(newTailNode, tr)
                                    tr.addTextNode(newTailNode)
                                }
                        limen.textNodeList.add(cursor.textNodeIndex + 1, newTailNode)
                    } else {
                        // limen.associateTextNodeWithMarkupForLayer(cursor.getCurrentTextNode(), newMarkup);
                        // newMarkup.addTextNode(cursor.getCurrentTextNode());
                        throw RuntimeException("head=empty!")
                    }
                }
                findEndingTextNode = false
            }
        }
    }

    fun addMarkup(newMarkup: Markup, positions: Collection<Position>) {
        if (!newMarkup.hasId()) {
            throw RuntimeException("Markup " + newMarkup.tag + " should have an id.")
        }
        positions.forEach { position: Position ->
            log.debug("position={}", position)
            logTextNodes(limen.textNodeList)
            logMarkups(limen.markupList)
            addMarkup(newMarkup, position)
        }
        logTextNodes(limen.textNodeList)
        logMarkups(limen.markupList)
        // LMNLImporter.joinDiscontinuedRanges(limen);
    }

    private fun logTextNodes(list: List<TextNode>) {
        val textnodes = StringBuilder()
        list.forEach { tn: TextNode ->
            if (tn.previousTextNode != null) {
                textnodes.append("\"").append(tn.previousTextNode.content).append("\" -> ")
            }
            textnodes.append("[").append(tn.content).append("]")
            if (tn.nextTextNode != null) {
                textnodes.append(" -> \"").append(tn.nextTextNode.content).append("\"")
            }
            textnodes.append("\n")
        }
        log.debug("\nTextNodes:\n{}", textnodes)
    }

    private fun logMarkups(list: List<Markup>) {
        val markups = StringBuilder()
        list.forEach { tr: Markup ->
            markups.append("[").append(tr.tag).append("}\n")
            tr.textNodes.forEach { tn: TextNode -> markups.append("  \"").append(tn.content).append("\"\n") }
        }
        log.debug("\nMarkups:\n{}", markups)
    }

    internal class TextNodeCursor(limen: Limen) {
        var currentTextNode: TextNode
            private set
        var textNodeIndex = 0
            private set
        var offset = 0
            private set

        fun advance() {
            offset += currentTextLength
            currentTextNode = currentTextNode.nextTextNode
            textNodeIndex++
        }

        val currentText: String
            get() = currentTextNode.content

        val currentTextLength: Int
            get() = currentText.length

        fun canAdvance(): Boolean {
            return currentTextNode.nextTextNode != null
        }

        internal val offsetAtEndOfCurrentTextNode: Int
            get() = offset + currentTextLength

        init {
            currentTextNode = limen.textNodeList[0]
        }
    }

}
