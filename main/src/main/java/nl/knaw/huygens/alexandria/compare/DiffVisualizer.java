package nl.knaw.huygens.alexandria.compare;

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

import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import prioritised_xml_collation.TAGToken;

import java.util.List;

interface DiffVisualizer {
  void startVisualization();

  void startOriginal(final String witness1);

  void originalTextNode(TAGTextNode t);

  void endOriginal();

  void startDiff(final String witness1, final String witness2);

  void startAligned();

  void alignedTextTokens(List<TAGToken> tokensWa, List<TAGToken> tokensWb);

  void endAligned();

  void startAddition();

  void addedTextToken(TAGToken t);

  void endAddition();

  void startOmission();

  void omittedTextToken(TAGToken t);

  void endOmission();

  void startReplacement();

  void originalTextToken(TAGToken t);

  void replacementSeparator();

  void editedTextToken(TAGToken t);

  void endReplacement();

  void endDiff();

  void startEdited(final String witness2);

  void editedTextNode(TAGTextNode t);

  void endEdited();

  void endVisualization();

  String getResult();

}
