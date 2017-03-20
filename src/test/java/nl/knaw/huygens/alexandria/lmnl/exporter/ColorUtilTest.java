package nl.knaw.huygens.alexandria.lmnl.exporter;

import java.awt.Color;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColorUtilTest {
  Logger LOG = LoggerFactory.getLogger(this.getClass());

  @Test
  public void testColorInterpolation() {
    Color white = Color.YELLOW;
    Color red = Color.BLUE;
    Color color = ColorUtil.interpolate(white, red, 0f);
    LOG.info("color={}", ColorUtil.toLaTeX(color));
    color = ColorUtil.interpolate(white, red, 0.25f);
    LOG.info("color={}", ColorUtil.toLaTeX(color));
    color = ColorUtil.interpolate(white, red, 0.5f);
    LOG.info("color={}", ColorUtil.toLaTeX(color));
    color = ColorUtil.interpolate(white, red, 0.75f);
    LOG.info("color={}", ColorUtil.toLaTeX(color));
    color = ColorUtil.interpolate(white, red, 1f);
    LOG.info("color={}", ColorUtil.toLaTeX(color));
  }

}
