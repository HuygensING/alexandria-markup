package nl.knaw.huygens.alexandria.exporter

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

import java.awt.Color

internal object ColorUtil {
  @JvmStatic
  fun interpolate(start: Color, end: Color, p: Float): Color {
    val startHSB = Color.RGBtoHSB(start.red, start.green, start.blue, null)
    val endHSB = Color.RGBtoHSB(end.red, end.green, end.blue, null)
    val brightness = (startHSB[2] + endHSB[2]) / 2
    val saturation = (startHSB[1] + endHSB[1]) / 2
    var hueMax = 0f
    var hueMin = 0f
    if (startHSB[0] > endHSB[0]) {
      hueMax = startHSB[0]
      hueMin = endHSB[0]
    } else {
      hueMin = startHSB[0]
      hueMax = endHSB[0]
    }
    val hue = (hueMax - hueMin) * p + hueMin
    return Color.getHSBColor(hue, saturation, brightness)
  }

  @JvmStatic
  fun toLaTeX(color: Color): String {
    return "{rgb,255:red," + color.red + ";green," + color.green + ";blue," + color.blue + "}"
  }
}
