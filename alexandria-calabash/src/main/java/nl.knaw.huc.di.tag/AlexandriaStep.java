package nl.knaw.huc.di.tag;

/*-
 * #%L
 * alexandria-calabash
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

@XMLCalabash(
    name = "tag:load",
    type = "{http://xmlcalabash.com/ns/extensions}tag-load")

public class AlexandriaStep extends DefaultStep {
  private WritablePipe result = null;

  public AlexandriaStep(final XProcRuntime runtime, final XAtomicStep step) {
    super(runtime, step);
  }

  public void setOutput(String port, WritablePipe pipe) {
    result = pipe;
  }

  public void reset() {
    result.resetWriter();
  }

  public void run() throws SaxonApiException {
    super.run();
    RuntimeValue option = step.getOption(new QName("tagmlfile"));
    String string = option.getString();

    TreeWriter tree = new TreeWriter(runtime);
    tree.startDocument(step.getNode().getBaseURI());
    tree.addStartElement(XProcConstants.c_result);
    tree.startContent();
    tree.addText("This is my code");
    tree.addText("tagmlfile=" + string);
    tree.addEndElement();
    tree.endDocument();
    result.write(tree.getResult());
  }
}
