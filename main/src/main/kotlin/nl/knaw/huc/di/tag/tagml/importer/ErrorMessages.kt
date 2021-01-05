package nl.knaw.huc.di.tag.tagml.importer

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

object ErrorMessages {
    const val NO_TEXT_BEFORE_ROOT = "No text allowed here, the root markup must be started first."
    const val SUSPENDED_ROOT_MARKUP = "The root markup %s cannot be suspended."
    const val UNDEFINED_NAMESPACE = "Namespace %s has not been defined."

    const val MISSING_OPEN_TAG = "Close tag <%s] found without corresponding open tag."
    const val MISSING_CLOSE_TAG = "Missing close tag(s) for: %s"

    const val CLOSED_LAYER = "%s cannot be used here, since the root markup of this layer has closed already."
    const val MILESTONE_ROOT = "The root markup cannot be a milestone tag."

    const val UNSUSPENDED_MARKUP = "Resume tag %s found, which has no corresponding earlier suspend tag <%s%s]."
    const val UNRESUMED_MARKUP = "Some suspended markup was not resumed: %s"
    const val IMMEDIATE_RESUME = "There is no text between this resume tag: %s and its corresponding suspend tag: %s. This is not allowed."

    const val NAMELESS_MARKUP = "Nameless markup is not allowed here."
    const val CLOSE_IN_BRANCH = "Markup [%s> opened before branch %s, should not be closed in a branch."
    const val MULTIPLE_CLOSE_IN_BRANCH = "Markup %s opened before branch %s, should not be closed in a branch."
    const val MULTIPLE_OPEN_IN_BRANCH = "Markup %s opened in branch %s must be closed before starting a new branch."
    const val UNEXPECTED_CLOSE = "Close tag <%s] found, expected %s.%s"
    const val ID_IN_USE = "Id '%s' was already used in markup [%s>."
    const val AMBIGUOUS_CLOSE_TAG = "There are multiple start-tags that can correspond with end-tag <%s]; add layer information to the end-tag to solve this ambiguity."
    const val TRAILING_TEXT_OR_MARKUP = "No text or markup allowed after the root markup %s has been ended."
    const val SUSPEND_DISCREPANCY = "There is a discrepancy in suspended markup between branches:%s"
    const val OPEN_MARKUP_DISCREPANCY = "There is an open markup discrepancy between the branches:%s"
    const val UNADDED_LAYER = "Layer %s has not been added at this point, use +%s to add a layer."

    const val UNKNOWN_ANNOTATION_TYPE = "Cannot determine the type of this annotation: %s"
    const val MIXED_ELEMENT_TYPES = "All elements of ListAnnotation %s should be of the same type."
    const val COMMA_SEPARATORS = "The elements of ListAnnotation %s should be separated by commas."
}
