package nl.knaw.huygens.alexandria.lmnl.exporter;

import java.awt.Color;

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
