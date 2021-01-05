package nl.knaw.huygens.alexandria.freemarker

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

import freemarker.template.Configuration
import freemarker.template.DefaultObjectWrapper
import freemarker.template.TemplateException
import java.io.*

object FreeMarker {
  private val VERSION = Configuration.VERSION_2_3_25
  private val FREEMARKER = Configuration(VERSION)

  @JvmStatic
  fun templateToString(fmTemplate: String, fmRootMap: Any, clazz: Class<*>): String =
      processTemplate(fmTemplate, fmRootMap, clazz, StringWriter())

  @JvmStatic
  fun templateToFile(fmTemplate: String, file: File, fmRootMap: Any, clazz: Class<*>): String =
      try {
        val out = FileWriter(file)
        processTemplate(fmTemplate, fmRootMap, clazz, out)
      } catch (e: IOException) {
        throw UncheckedIOException(e)
      }

  private fun processTemplate(fmTemplate: String, fmRootMap: Any, clazz: Class<*>, out: Writer): String =
      try {
        FREEMARKER.setClassForTemplateLoading(clazz, "")
        val template = FREEMARKER.getTemplate(fmTemplate)
        template.outputEncoding = "UTF-8"
        template.process(fmRootMap, out)
        out.toString()
      } catch (e1: IOException) {
        throw RuntimeException(e1)
      } catch (e1: TemplateException) {
        throw RuntimeException(e1)
      }

  init {
    FREEMARKER.objectWrapper = DefaultObjectWrapper(VERSION)
  }
}
