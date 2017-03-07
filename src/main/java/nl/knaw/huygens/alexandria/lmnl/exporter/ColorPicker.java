package nl.knaw.huygens.alexandria.lmnl.exporter;

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
