package nl.knaw.huygens.alexandria.lmnl.exporter;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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


import java.awt.*;

public class ColorUtil {

  public static Color interpolate(Color start, Color end, float p) {
    float[] startHSB = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
    float[] endHSB = Color.RGBtoHSB(end.getRed(), end.getGreen(), end.getBlue(), null);

    float brightness = (startHSB[2] + endHSB[2]) / 2;
    float saturation = (startHSB[1] + endHSB[1]) / 2;

    float hueMax = 0;
    float hueMin = 0;
    if (startHSB[0] > endHSB[0]) {
      hueMax = startHSB[0];
      hueMin = endHSB[0];
    } else {
      hueMin = startHSB[0];
      hueMax = endHSB[0];
    }

    float hue = ((hueMax - hueMin) * p) + hueMin;

    return Color.getHSBColor(hue, saturation, brightness);
  }

  public static String toLaTeX(Color color) {
    return "{rgb,255:red," + color.getRed() + ";green," + color.getGreen() + ";blue," + color.getBlue() + "}";
  }
}
