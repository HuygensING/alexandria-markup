package nl.knaw.huygens.alexandria.creole.patterns;

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
import nl.knaw.huygens.alexandria.creole.NameClass;

import java.util.List;

public class Atom extends AbstractPattern {
  final NameClass nc;
  final List<Annotation> annotations;

  public Atom(NameClass nc, List<Annotation> annotations) {
    this.nc = nc;
    this.annotations = annotations;
  }

  @Override
  void init() {
    nullable = false;
    allowsText = false;
    allowsAnnotations = false;
    onlyAnnotations = false;
  }
}
