package nl.knaw.huc.di.tag.tagml.importer2;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;

public class TextResource implements Resource {

  private String resourceId;
  private String text;
  private Resource delegate;

  TextResource(Resource resource) {
    delegate = resource;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(final String resourceId) {
    this.resourceId = resourceId;
  }

  public String getText() {
    return text;
  }

  public void setText(final String text) {
    this.text = text;
  }

  // Just delegated Resource methods after this

  @Override
  public AnonId getId() {
    return delegate.getId();
  }

  @Override
  public Resource inModel(final Model m) {
    return delegate.inModel(m);
  }

  @Override
  public boolean hasURI(final String uri) {
    return delegate.hasURI(uri);
  }

  @Override
  public String getURI() {
    return delegate.getURI();
  }

  @Override
  public String getNameSpace() {
    return delegate.getNameSpace();
  }

  @Override
  public String getLocalName() {
    return delegate.getLocalName();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public boolean equals(final Object o) {
    return delegate.equals(o);
  }

  @Override
  public Statement getRequiredProperty(final Property p) {
    return delegate.getRequiredProperty(p);
  }

  @Override
  public Statement getRequiredProperty(final Property p, final String lang) {
    return delegate.getRequiredProperty(p, lang);
  }

  @Override
  public Statement getProperty(final Property p) {
    return delegate.getProperty(p);
  }

  @Override
  public Statement getProperty(final Property p, final String lang) {
    return delegate.getProperty(p, lang);
  }

  @Override
  public StmtIterator listProperties(final Property p) {
    return delegate.listProperties(p);
  }

  @Override
  public StmtIterator listProperties(final Property p, final String lang) {
    return delegate.listProperties(p, lang);
  }

  @Override
  public StmtIterator listProperties() {
    return delegate.listProperties();
  }

  @Override
  public Resource addLiteral(final Property p, final boolean o) {
    return delegate.addLiteral(p, o);
  }

  @Override
  public Resource addLiteral(final Property p, final long o) {
    return delegate.addLiteral(p, o);
  }

  @Override
  public Resource addLiteral(final Property p, final char o) {
    return delegate.addLiteral(p, o);
  }

  @Override
  public Resource addLiteral(final Property value, final double d) {
    return delegate.addLiteral(value, d);
  }

  @Override
  public Resource addLiteral(final Property value, final float d) {
    return delegate.addLiteral(value, d);
  }

  @Override
  public Resource addLiteral(final Property p, final Object o) {
    return delegate.addLiteral(p, o);
  }

  @Override
  public Resource addLiteral(final Property p, final Literal o) {
    return delegate.addLiteral(p, o);
  }

  @Override
  public Resource addProperty(final Property p, final String o) {
    return delegate.addProperty(p, o);
  }

  @Override
  public Resource addProperty(final Property p, final String o, final String l) {
    return delegate.addProperty(p, o, l);
  }

  @Override
  public Resource addProperty(final Property p, final String lexicalForm, final RDFDatatype datatype) {
    return delegate.addProperty(p, lexicalForm, datatype);
  }

  @Override
  public Resource addProperty(final Property p, final RDFNode o) {
    return delegate.addProperty(p, o);
  }

  @Override
  public boolean hasProperty(final Property p) {
    return delegate.hasProperty(p);
  }

  @Override
  public boolean hasLiteral(final Property p, final boolean o) {
    return delegate.hasLiteral(p, o);
  }

  @Override
  public boolean hasLiteral(final Property p, final long o) {
    return delegate.hasLiteral(p, o);
  }

  @Override
  public boolean hasLiteral(final Property p, final char o) {
    return delegate.hasLiteral(p, o);
  }

  @Override
  public boolean hasLiteral(final Property p, final double o) {
    return delegate.hasLiteral(p, o);
  }

  @Override
  public boolean hasLiteral(final Property p, final float o) {
    return delegate.hasLiteral(p, o);
  }

  @Override
  public boolean hasLiteral(final Property p, final Object o) {
    return delegate.hasLiteral(p, o);
  }

  @Override
  public boolean hasProperty(final Property p, final String o) {
    return delegate.hasProperty(p, o);
  }

  @Override
  public boolean hasProperty(final Property p, final String o, final String l) {
    return delegate.hasProperty(p, o, l);
  }

  @Override
  public boolean hasProperty(final Property p, final RDFNode o) {
    return delegate.hasProperty(p, o);
  }

  @Override
  public Resource removeProperties() {
    return delegate.removeProperties();
  }

  @Override
  public Resource removeAll(final Property p) {
    return delegate.removeAll(p);
  }

  @Override
  public Resource begin() {
    return delegate.begin();
  }

  @Override
  public Resource abort() {
    return delegate.abort();
  }

  @Override
  public Resource commit() {
    return delegate.commit();
  }

  @Override
  public Resource getPropertyResourceValue(final Property p) {
    return delegate.getPropertyResourceValue(p);
  }

  @Override
  public boolean isAnon() {
    return delegate.isAnon();
  }

  @Override
  public boolean isLiteral() {
    return delegate.isLiteral();
  }

  @Override
  public boolean isURIResource() {
    return delegate.isURIResource();
  }

  @Override
  public boolean isResource() {
    return delegate.isResource();
  }

  @Override
  public <T extends RDFNode> T as(final Class<T> view) {
    return delegate.as(view);
  }

  @Override
  public <T extends RDFNode> boolean canAs(final Class<T> view) {
    return delegate.canAs(view);
  }

  @Override
  public Model getModel() {
    return delegate.getModel();
  }

  @Override
  public Object visitWith(final RDFVisitor rv) {
    return delegate.visitWith(rv);
  }

  @Override
  public Resource asResource() {
    return delegate.asResource();
  }

  @Override
  public Literal asLiteral() {
    return delegate.asLiteral();
  }

  @Override
  public Node asNode() {
    return delegate.asNode();
  }

}
