package nl.knaw.huygens.alexandria.lmnl.exporter;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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


/**
 * Created by bramb on 24/02/2017.
 */
public class ColorPicker {
  private final String[] colors;
  int i = 0;

  public ColorPicker(String... colors) {
    this.colors = colors;
  }

  public String nextColor() {
    String color = colors[i];
    i = (i < colors.length - 1) ? i + 1 : 0;
    return color;
  }
}
