package nl.knaw.huygens.alexandria.view

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

import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TAGViewFactoryTest : AlexandriaBaseStoreTest() {

    @Test
    fun fromJsonWithBadDefinition() {
        runInStore { store: TAGStore ->
            val json = ("""{
                |"includeLayer" : ["A","B"]
                |}""".trimMargin())
            val view = createView(store, json)
            assertThat(view.isValid).isFalse()
        }
    }

    @Test
    fun fromJsonWithLayersInclusion() {
        val included: Set<String> = setOf("A", "B")
        runInStore { store: TAGStore ->
            val expected = TAGView(store).withLayersToInclude(included)
            val json = "{'includeLayers':['A','B']}".replace("'", "\"")
            val view = createView(store, json)
            assertThat(view.isValid).isTrue()
            assertThat(view).isEqualToComparingFieldByField(expected)
        }
    }

    private fun createView(store: TAGStore, json: String): TAGView {
        return TAGViewFactory(store).fromJsonString(json)
    }

    @Test
    fun fromJsonWithLayersExclusion() {
        val excluded: Set<String> = setOf("A", "B")
        runInStore { store: TAGStore ->
            val expected = TAGView(store).withLayersToExclude(excluded)
            val json = "{'excludeLayers':['A','B']}".replace("'", "\"")
            val view = createView(store, json)
            assertThat(view.isValid).isTrue()
            assertThat(view).isEqualToComparingFieldByField(expected)
        }
    }

    @Test
    fun fromJsonWithMarkupInclusion() {
        val included: Set<String> = setOf("chapter", "p")
        runInStore { store: TAGStore ->
            val expected = TAGView(store).withMarkupToInclude(included)
            val json = "{'includeMarkup':['chapter','p']}".replace("'", "\"")
            val view = createView(store, json)
            assertThat(view.isValid).isTrue()
            assertThat(view).isEqualToComparingFieldByField(expected)
        }
    }

    @Test
    fun fromJsonWithMarkupExclusion() {
        val excluded: Set<String> = setOf("verse", "l")
        runInStore { store: TAGStore ->
            val expected = TAGView(store).withMarkupToExclude(excluded)
            val json = "{'excludeMarkup':['verse','l']}".replace("'", "\"")
            val view = createView(store, json)
            assertThat(view.isValid).isTrue()
            assertThat(view).isEqualToComparingFieldByField(expected)
        }
    }

    @Test
    fun fromDefinitionWithMarkupInclusion() {
        val included: Set<String> = setOf("chapter", "p")
        runInStore { store: TAGStore ->
            val expected = TAGView(store).withMarkupToInclude(included)
            val def = TAGViewDefinition().withIncludeMarkup(included)
            val view = createView(store, def)
            assertThat(view.isValid).isTrue()
            assertThat(view).isEqualToComparingFieldByField(expected)
        }
    }

    private fun createView(store: TAGStore, def: TAGViewDefinition): TAGView =
        TAGViewFactory(store).fromDefinition(def)

    @Test
    fun fromDefinitionWithMarkupExclusion() {
        val excluded: Set<String> = setOf("verse", "l")
        runInStore { store: TAGStore ->
            val expected = TAGView(store).withMarkupToExclude(excluded)
            val def = TAGViewDefinition().withExcludeMarkup(excluded)
            val view = createView(store, def)
            assertThat(view.isValid).isTrue()
            assertThat(view).isEqualToComparingFieldByField(expected)
        }
    }

    @Test
    fun fromDefinitionWithLayersInclusion() {
        val included: Set<String> = setOf("L1", "L2")
        runInStore { store: TAGStore ->
            val expected = TAGView(store).withLayersToInclude(included)
            val def = TAGViewDefinition().withIncludeLayers(included)
            val view = createView(store, def)
            assertThat(view.isValid).isTrue()
            assertThat(view).isEqualToComparingFieldByField(expected)
        }
    }

    @Test
    fun fromDefinitionWithLayersExclusion() {
        val excluded: Set<String> = setOf("L3", "L4")
        runInStore { store: TAGStore ->
            val expected = TAGView(store).withLayersToExclude(excluded)
            val def = TAGViewDefinition().withExcludeLayers(excluded)
            val view = createView(store, def)
            assertThat(view.isValid).isTrue()
            assertThat(view).isEqualToComparingFieldByField(expected)
        }
    }
}
