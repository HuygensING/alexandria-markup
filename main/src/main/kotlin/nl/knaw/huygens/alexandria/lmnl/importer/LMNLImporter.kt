package nl.knaw.huygens.alexandria.lmnl.importer

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

import nl.knaw.huygens.alexandria.ErrorListener
import nl.knaw.huygens.alexandria.lmnl.grammar.LMNLLexer
import nl.knaw.huygens.alexandria.storage.TAGAnnotation
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.storage.dto.*
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Token
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException
import java.util.*
import java.util.stream.Collectors

class LMNLImporter(tagStore: TAGStore?) {
  @Throws(LMNLSyntaxError::class)
  fun importLMNL(input: String?): TAGDocument {
    val antlrInputStream: CharStream = CharStreams.fromString(input)
    return importLMNL(antlrInputStream)
  }

  @Throws(LMNLSyntaxError::class)
  fun importLMNL(input: InputStream?): TAGDocument {
    return try {
      val antlrInputStream = CharStreams.fromStream(input)
      importLMNL(antlrInputStream)
    } catch (e: IOException) {
      e.printStackTrace()
      throw UncheckedIOException(e)
    }
  }

  @Throws(LMNLSyntaxError::class)
  private fun importLMNL(antlrInputStream: CharStream): TAGDocument {
    val lexer = LMNLLexer(antlrInputStream)
    val errorListener = ErrorListener()
    lexer.addErrorListener(errorListener)
    val context = ImporterContext(lexer)
    val document = tagStore!!.createDocument()
    val dto = document.dto
    update(dto)
    context.pushDocumentContext(dto)
    handleDefaultMode(context)
    joinDiscontinuedRanges(document)
    context.popDocumentContext()
    var errorMsg = ""
    if (context.hasErrors()) {
      val errors = java.lang.String.join("\n", context.getErrors())
      errorMsg = "Parsing errors:\n$errors"
    }
    if (errorListener.hasErrors()) {
      errorMsg += """
        
        
        Tokenizing errors:
        ${errorListener.prefixedErrorMessagesAsString}
        """.trimIndent()
    }
    if (errorMsg.isNotEmpty()) {
      throw LMNLSyntaxError(errorMsg)
    }
    update(dto)
    return document
  }

  private fun handleDefaultMode(context: ImporterContext) {
    val methodName = "defaultMode"
    var token: Token
    do {
      token = context.nextToken()
      if (token.type != Token.EOF) {
        val ruleName = context.ruleName
        val modeName = context.modeName
        log(methodName, ruleName, modeName, token, context)
        when (token.type) {
          LMNLLexer.BEGIN_OPEN_RANGE -> handleOpenRange(context)
          LMNLLexer.BEGIN_CLOSE_RANGE -> handleCloseRange(context)
          LMNLLexer.TEXT -> {
            val textNode = TAGTextNodeDTO(token.text)
            update(textNode)
            context.addTextNode(textNode)
          }
          else -> handleUnexpectedToken(methodName, token, ruleName, modeName)
        }
      }
    } while (token.type != Token.EOF)
  }

  private fun handleOpenRange(context: ImporterContext) {
    val methodName = "handleOpenRange"
    var goOn = true
    while (goOn) {
      val token = context.nextToken()
      val ruleName = context.ruleName
      val modeName = context.modeName
      log(methodName, ruleName, modeName, token, context)
      when (token.type) {
        LMNLLexer.Name_Open_Range -> {
          val markup = context.newMarkup(token.text)
          context.openMarkup(markup)
        }
        LMNLLexer.BEGIN_OPEN_ANNO -> handleAnnotation(context)
        LMNLLexer.END_OPEN_RANGE -> {
          context.popOpenMarkup()
          goOn = false
        }
        LMNLLexer.END_ANONYMOUS_RANGE -> {
          val textNode = TAGTextNodeDTO("")
          update(textNode)
          context.addTextNode(textNode)
          context.closeMarkup()
          goOn = false
        }
        else -> handleUnexpectedToken(methodName, token, ruleName, modeName)
      }
      goOn = goOn && token.type != Token.EOF
    }
  }

  private fun handleAnnotation(context: ImporterContext) {
    val methodName = "handleAnnotation"
    //    TAGAnnotationDTO annotation = tagStore.createAnnotationDTO("");
    //    context.openAnnotation(annotation);
    //    boolean goOn = true;
    //    while (goOn) {
    //      Token token = context.nextToken();
    //      String ruleName = context.getRuleName();
    //      String modeName = context.getModeName();
    //      log(methodName, ruleName, modeName, token, context);
    //      switch (token.getType()) {
    //        case LMNLLexer.Name_Open_Annotation:
    //          annotation.setKey(token.getText());
    //          break;
    //        case LMNLLexer.OPEN_ANNO_IN_ANNO_OPENER:
    //        case LMNLLexer.OPEN_ANNO_IN_ANNO_CLOSER:
    //          handleAnnotation(context);
    //          break;
    //        case LMNLLexer.END_OPEN_ANNO:
    //          context.pushDocumentContext(context.currentAnnotationDocument());
    //          break;

    //        case LMNLLexer.ANNO_TEXT:
    //          TAGTextNodeDTO textNode = new TAGTextNodeDTO(token.getText());
    //          update(textNode);
    //          context.addTextNode(textNode);
    //          break;

    //        case LMNLLexer.BEGIN_ANNO_OPEN_RANGE:
    //          handleOpenRange(context);
    //          break;

    //        case LMNLLexer.BEGIN_ANNO_CLOSE_RANGE:
    //          handleCloseRange(context);
    //          break;

    //        case LMNLLexer.BEGIN_CLOSE_ANNO:
    //        case LMNLLexer.Name_Close_Annotation:
    //          break;
    //        case LMNLLexer.END_CLOSE_ANNO:
    //          context.popDocumentContext();
    //        case LMNLLexer.END_EMPTY_ANNO:
    //          context.closeAnnotation();
    //          goOn = false;
    //          break;

    //        // case LMNLLexer.TagOpenStartChar:
    //        // case LMNLLexer.TagOpenEndChar:
    //        // case LMNLLexer.TagCloseStartChar:
    //        // case LMNLLexer.TagCloseEndChar:
    //        // break;

    //        default:
    //          handleUnexpectedToken(methodName, token, ruleName, modeName);
    //          break;
    //      }
    //      goOn = goOn && token.getType() != Token.EOF;
    //    }
  }

  private fun handleCloseRange(context: ImporterContext) {
    val methodName = "handleCloseRange"
    var goOn = true
    while (goOn) {
      val token = context.nextToken()
      val ruleName = context.ruleName
      val modeName = context.modeName
      log(methodName, ruleName, modeName, token, context)
      when (token.type) {
        LMNLLexer.Name_Close_Range -> {
          val rangeName = token.text
          context.pushOpenMarkup(rangeName)
        }
        LMNLLexer.BEGIN_OPEN_ANNO_IN_RANGE_CLOSER -> handleAnnotation(context)
        LMNLLexer.END_CLOSE_RANGE -> {
          context.closeMarkup()
          goOn = false
        }
        else -> handleUnexpectedToken(methodName, token, ruleName, modeName)
      }
      goOn = goOn && token.type != Token.EOF
    }
  }

  private fun handleUnexpectedToken(
      methodName: String, token: Token, ruleName: String, modeName: String) {
    val message = (methodName
        + ": unexpected rule/token: token="
        + token
        + ", ruleName="
        + ruleName
        + ", mode="
        + modeName)
    LOG.error(message)
    throw LMNLSyntaxError(message)
  }

  //  private static void joinDiscontinuedRanges(Document document) {
  //    joinDiscontinuedRanges(document.getDocumentId());
  //  }
  private fun log(
      mode: String, ruleName: String, modeName: String, token: Token, context: ImporterContext) {
    // LOG.info("{}:\tlevel:{}, <{}> :\t{} ->\t{}",
    // mode, context.limenContextStack.size(),
    // token.getText().replace("\n", "\\n"),
    // ruleName, modeName);
  }

  internal class DocumentContext(val document: TAGDocumentDTO, private val importerContext: ImporterContext) {
    val openMarkupDeque: Deque<TAGMarkupDTO> = ArrayDeque()
    private val openMarkupStack = Stack<TAGMarkupDTO>()
    private val annotationStack = Stack<TAGAnnotationDTO>()
    fun openMarkup(markup: TAGMarkupDTO) {
      openMarkupDeque.push(markup)
      openMarkupStack.push(markup)
      document.markupIds.add(markup.dbId)
    }

    fun pushOpenMarkup(rangeName: String) {
      // LOG.info("currentDocumentContext().openMarkupDeque={}",
      // openMarkupDeque.stream().map(Markup::getKey).collect(Collectors.toList()));
      val findFirst = openMarkupDeque.stream()
          .map { dto: TAGMarkupDTO? -> TAGMarkup(tagStore, dto) }
          .filter { m: TAGMarkup -> m.extendedTag == rangeName }
          .findFirst()
      if (findFirst.isPresent) {
        val markup = findFirst.get()
        if (!document.markupHasTextNodes(markup)) {
          // every markup should have at least one textNode
          val emptyTextNode = TAGTextNodeDTO("")
          update(emptyTextNode)
          addTextNode(emptyTextNode)
          closeMarkup()
        }
        openMarkupStack.push(markup.dto)
      } else {
        importerContext.getErrors().add(
            "Closing tag {$rangeName] found without corresponding open tag.")
      }
    }

    fun popOpenMarkup() {
      openMarkupStack.pop()
    }

    fun closeMarkup() {
      if (!openMarkupStack.isEmpty()) {
        val markup = openMarkupStack.pop()
        update(markup)
        openMarkupDeque.remove(markup)
      }
    }

    fun addTextNode(textNode: TAGTextNodeDTO) {
      openMarkupDeque
          .descendingIterator()
          .forEachRemaining { m: TAGMarkupDTO? ->
            //            m.addTextNode(textNode);
            document.associateTextWithMarkupForLayer(textNode, m, "")
          }
      if (!document.hasTextNodes()) {
        document.firstTextNodeId = textNode.dbId
      }
      document.addTextNode(textNode)
    }

    private fun currentMarkup(): TAGMarkupDTO? {
      return if (openMarkupDeque.isEmpty()) null else openMarkupStack.peek()
    }

    fun openAnnotation(annotation: TAGAnnotationDTO) {
      if (annotationStack.isEmpty()) {
        val markup = currentMarkup()
        if (markup != null) {
          //          markup.addAnnotation(annotation);
        }
      } else {
        //        annotationStack.peek().addAnnotation(annotation);
      }
      annotationStack.push(annotation)
    }

    fun currentAnnotationDocument(): TAGDocumentDTO? {
      return null
      //      Long value = annotationStack.peek().getDocumentId();
      //      return tagStore.getDocumentDTO(value);
    }

    fun closeAnnotation() {
      val annotation = annotationStack.pop()
      update(annotation)
    }

  }

  internal class ImporterContext(private val lexer: LMNLLexer) {
    private val documentContextStack: Deque<DocumentContext> = ArrayDeque()
    private val _errors: MutableList<String> = ArrayList()

    fun nextToken(): Token {
      return lexer.nextToken()
    }

    val modeName: String
      get() = lexer.modeNames[lexer._mode]

    val ruleName: String
      get() = lexer.ruleNames[lexer.token.type - 1]

    fun pushDocumentContext(document: TAGDocumentDTO) {
      documentContextStack.push(DocumentContext(document, this))
    }

    fun currentDocumentContext(): DocumentContext {
      return documentContextStack.peek()
    }

    fun popDocumentContext(): DocumentContext {
      val documentContext = documentContextStack.pop()
      update(documentContext.document)
      if (!documentContext.openMarkupDeque.isEmpty()) {
        val openRanges = documentContext.openMarkupDeque.stream()
            .map { dto: TAGMarkupDTO? -> TAGMarkup(tagStore, dto) }
            .map { m: TAGMarkup -> "[" + m.extendedTag + "}" }
            .collect(Collectors.joining(", "))
        _errors.add("Unclosed LMNL range(s): $openRanges")
      }
      return documentContext
    }

    fun newMarkup(tagName: String?): TAGMarkupDTO {
      val tagMarkupDTO = TAGMarkupDTO(currentDocumentContext().document.dbId, tagName)
      update(tagMarkupDTO)
      return tagMarkupDTO
    }

    fun openMarkup(markup: TAGMarkupDTO) {
      currentDocumentContext().openMarkup(markup)
    }

    fun pushOpenMarkup(rangeName: String) {
      currentDocumentContext().pushOpenMarkup(rangeName)
    }

    fun popOpenMarkup() {
      currentDocumentContext().popOpenMarkup()
    }

    fun closeMarkup() {
      currentDocumentContext().closeMarkup()
    }

    fun addTextNode(textNode: TAGTextNodeDTO) {
      currentDocumentContext().addTextNode(textNode)
    }

    fun openAnnotation(annotation: TAGAnnotationDTO) {
      currentDocumentContext().openAnnotation(annotation)
    }

    fun currentAnnotationDocument(): TAGDocumentDTO? {
      return currentDocumentContext().currentAnnotationDocument()
    }

    fun closeAnnotation() {
      currentDocumentContext().closeAnnotation()
    }

    fun getErrors(): MutableList<String> {
      return _errors
    }

    fun hasErrors(): Boolean {
      return _errors.isNotEmpty()
    }

  }

  companion object {
    private val LOG = LoggerFactory.getLogger(LMNLImporter::class.java)
    private var tagStore: TAGStore? = null
    private fun joinDiscontinuedRanges(document: TAGDocument) {
      //    Map<String, TAGMarkupDTO> markupsToJoin = new HashMap<>();
      //    List<Long> markupIdsToRemove = new ArrayList<>();
      //    document.getMarkupStream()
      //        .filter(TAGMarkup::hasN)
      //        .forEach(markup -> {
      //          String tag = markup.getTag();
      //          AnnotationInfo annotation = markup.getAnnotationStream()
      //              .filter(a -> a.getName().equals("n"))
      //              .findFirst()
      //              .get();
      //          String key = tag + "-" + annotationText(annotation);
      //          if (markupsToJoin.containsKey(key)) {
      //            TAGMarkupDTO originalMarkup = markupsToJoin.get(key);
      //            markup.getDTO().getAnnotationIds().remove(annotation.getResourceId());
      ////            document.joinMarkup(originalMarkup, markup);
      //            markupIdsToRemove.add(markup.getResourceId());
      //          } else {
      //            markupsToJoin.put(key, markup.getDTO());
      //          }
      //        });

      //    document.getDTO().getMarkupIds().removeAll(markupIdsToRemove);
      ////    document.getMarkupStream()
      ////        .map(TAGMarkup::getAnnotationStream)
      ////        .flatMap(Function.identity())
      ////        .map(TAGAnnotation::getDocument)
      ////        .forEach(LMNLImporter::joinDiscontinuedRanges);
    }

    private fun annotationText(annotation: TAGAnnotation): String {
      return "TODO"
      //    return annotation.getDocument().getTextNodeStream()
      //        .map(TAGTextNode::getText)
      //        .collect(joining());
    }

    private fun update(tagdto: TAGDTO): Long {
      return tagStore!!.persist(tagdto)
    }
  }

  init {
    Companion.tagStore = tagStore
  }
}
