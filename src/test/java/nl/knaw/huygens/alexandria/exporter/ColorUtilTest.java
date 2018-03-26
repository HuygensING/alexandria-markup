package nl.knaw.huygens.alexandria.exporter;

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


import nl.knaw.huygens.alexandria.exporter.ColorUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class ColorUtilTest {
  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

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
