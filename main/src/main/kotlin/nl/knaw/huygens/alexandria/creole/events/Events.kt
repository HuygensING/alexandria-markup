package nl.knaw.huygens.alexandria.creole.events

/*
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

import nl.knaw.huygens.alexandria.creole.Basics
import nl.knaw.huygens.alexandria.creole.Basics.qName
import nl.knaw.huygens.alexandria.creole.Event

object Events {

    @JvmOverloads
    fun startTagEvent(qName: Basics.QName, id: String = ""): StartTagEvent {
        return StartTagEvent(qName, Basics.id(id))
    }

    fun startTagOpenEvent(qName: Basics.QName, id: String): Event {
        return StartTagOpenEvent(qName, Basics.id(id))
    }

    fun startTagOpenEvent(qName: Basics.QName): Event {
        return StartTagOpenEvent(qName, Basics.id(""))
    }

    fun startTagOpenEvent(name: String): Event {
        return StartTagOpenEvent(qName(name), Basics.id(""))
    }

    fun startTagCloseEvent(qName: Basics.QName): Event {
        return StartTagCloseEvent(qName, Basics.id(""))
    }

    fun startTagCloseEvent(qName: Basics.QName, id: String): Event {
        return StartTagCloseEvent(qName, Basics.id(id))
    }

    fun startTagCloseEvent(name: String): Event {
        return StartTagCloseEvent(qName(name), Basics.id(""))
    }

    fun endTagOpenEvent(qName: Basics.QName, id: String): Event {
        return EndTagOpenEvent(qName, Basics.id(id))
    }

    fun endTagOpenEvent(qName: Basics.QName): Event {
        return EndTagOpenEvent(qName, Basics.id(""))
    }

    fun endTagOpenEvent(name: String): Event {
        return EndTagOpenEvent(qName(name), Basics.id(""))
    }

    fun endTagCloseEvent(qName: Basics.QName, id: String): Event {
        return EndTagCloseEvent(qName, Basics.id(id))
    }

    fun endTagCloseEvent(qName: Basics.QName): Event {
        return EndTagCloseEvent(qName, Basics.id(""))
    }

    fun endTagCloseEvent(name: String): Event {
        return EndTagCloseEvent(qName(name), Basics.id(""))
    }

    @JvmOverloads
    fun endTagEvent(qName: Basics.QName, id: String = ""): EndTagEvent {
        return EndTagEvent(qName, Basics.id(id))
    }

    @JvmOverloads
    fun textEvent(text: String, context: Basics.Context = Basics.context()): TextEvent {
        return TextEvent(text, context)
    }

    /* Annotation events */

    fun startAnnotationOpenEvent(qName: String): StartAnnotationOpenEvent {
        return startAnnotationOpenEvent(qName(qName))
    }

    fun startAnnotationOpenEvent(qName: Basics.QName): StartAnnotationOpenEvent {
        return StartAnnotationOpenEvent(qName)
    }

    fun startAnnotationCloseEvent(qName: String): StartAnnotationCloseEvent {
        return startAnnotationCloseEvent(qName(qName))
    }

    fun startAnnotationCloseEvent(qName: Basics.QName): StartAnnotationCloseEvent {
        return StartAnnotationCloseEvent(qName)
    }

    fun endAnnotationOpenEvent(qName: String): EndAnnotationOpenEvent {
        return endAnnotationOpenEvent(qName(qName))
    }

    fun endAnnotationOpenEvent(qName: Basics.QName): EndAnnotationOpenEvent {
        return EndAnnotationOpenEvent(qName)
    }

    fun endAnnotationCloseEvent(qName: String): EndAnnotationCloseEvent {
        return endAnnotationCloseEvent(qName(qName))
    }

    fun endAnnotationCloseEvent(qName: Basics.QName): EndAnnotationCloseEvent {
        return EndAnnotationCloseEvent(qName)
    }

}/*
  data Event = StartTagEvent QName Id
             | EndTagEvent QName Id
             | TextEvent String Context
             | StartAnnotationEvent QName
             | EndAnnotationEvent QName
             | StartAtomEvent QName
             | EndAtomEvent QName
   */// TextEvent
