package nl.knaw.huygens.alexandria.creole

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

import nl.knaw.huygens.alexandria.creole.Constructors.notAllowed
import nl.knaw.huygens.alexandria.creole.patterns.NotAllowed
import org.slf4j.LoggerFactory

class Validator internal constructor(private val schemaPattern: Pattern) {
    private val errorListener: ValidationErrorListener

    init {
        this.errorListener = ValidationErrorListener()
    }

    fun validate(events: MutableList<Event>): ValidationResult {
        val pattern = eventsDeriv(schemaPattern, events)
        LOG.debug("end pattern = {}", pattern)
        return ValidationResult()//
                .setSuccess(pattern.isNullable)//
                .setUnexpectedEvent(errorListener.unexpectedEvent)
    }

    internal fun eventsDeriv(pattern: Pattern, events: MutableList<Event>): Pattern {
        //    LOG.debug("expected events: {}", expectedEvents(pattern).stream().map(Event::toString).sorted().distinct().collect(toList()));
        //    LOG.debug("pattern:\n{}", patternTreeToDepth(pattern, 10));
        //    LOG.debug("leafpatterns:\n{}", leafPatterns(pattern).stream().map(Pattern::toString).distinct().collect(toList()));
        // eventDeriv p [] = p
        if (events.isEmpty()) {
            LOG.debug("\n{}", Utilities.patternTreeToDepth(pattern, 20))
            return pattern
        }

        //  eventDeriv p (h:t) = eventDeriv (eventDeriv p h) t
        val head = events.removeAt(0)
        LOG.debug("{}: {}", head.javaClass.getSimpleName(), head)
        val headDeriv = head.eventDeriv(pattern)
        //    LOG.debug("\n{}", Utilities.patternTreeToDepth(headDeriv, 20));

        if (headDeriv is NotAllowed) {
            // fail fast
            LOG.error("Unexpected " + head.javaClass.getSimpleName() + ": {}", head)
            errorListener.unexpectedEvent = head
            return notAllowed()
        }
        return eventsDeriv(headDeriv, events)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Validator::class.java!!)

        fun ofPattern(schemaPattern: Pattern): Validator {
            return Validator(schemaPattern)
        }
    }

}
