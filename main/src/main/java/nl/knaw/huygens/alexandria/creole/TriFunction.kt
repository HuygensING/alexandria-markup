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
import java.util.*
import java.util.function.Function

@FunctionalInterface
internal interface TriFunction<A, B, C, R> {

    fun apply(a: A, b: B, c: C): R

    fun <V> andThen(after: Function<in R, out V>): TriFunction<A, B, C, V> {
        Objects.requireNonNull(after)
        return { a: A, b: B, c: C -> after.apply(apply(a, b, c)) }
    }
}
