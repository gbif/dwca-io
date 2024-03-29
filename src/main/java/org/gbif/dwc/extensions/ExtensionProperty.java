/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.dwc.extensions;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

import java.net.URI;
import java.util.Objects;

/**
 * Taken from https://github.com/gbif/dwca-validator3/
 */
public class ExtensionProperty implements Comparable<ExtensionProperty>, Term {
  private Extension extension;
  private String name;
  private String namespace;
  private String qualname;
  private String type;
  private String group;
  private int columnLength = 255; // sensible default
  private String link;
  private String examples;
  private String description;
  private boolean required;
  private Vocabulary vocabulary;

  public ExtensionProperty() {
    super();
  }

  /**
   * Construct a new property with a single qualified name. Parses out the name and sets the namespace to end with a
   * slash or #
   *
   * @param qualName
   */
  public ExtensionProperty(String qualName) {
    super();
    setQualname(qualName);
  }

  /**
   * Compare by group and qualified name as default sorting order
   *
   * @see Comparable#compareTo(Object)
   */
  @Override
  public int compareTo(org.gbif.dwc.extensions.ExtensionProperty prop) {
    if (group != null) {
      int x = this.group.compareTo(prop.group);
      if (x != 0) {
        return x;
      }
    }
    return this.qualname.compareTo(prop.qualname);
  }

  /**
   * Just compare the unique qualified names to see if extension properties are equal
   *
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof org.gbif.dwc.extensions.ExtensionProperty)) {
      return false;
    }
    org.gbif.dwc.extensions.ExtensionProperty o = (org.gbif.dwc.extensions.ExtensionProperty) other;
    return Objects.equals(extension, o.extension) && Objects.equals(qualname, o.qualname);
  }

  /**
   * The length of the database column to be generated when the extension property is installed. Also used to trim
   * incoming data before SQL insert is generated. For LOB columns use -1 or any negative value
   *
   * @return
   */
  public int getColumnLength() {
    return columnLength;
  }

  public String getDescription() {
    return description;
  }

  public String getExamples() {
    return examples;
  }

  public org.gbif.dwc.extensions.Extension getExtension() {
    return extension;
  }

  public String getGroup() {
    return group;
  }

  public String getLink() {
    return link;
  }

  public String getName() {
    return name;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getQualname() {
    return qualname;
  }

  public String getType() {
    return type;
  }

  public Vocabulary getVocabulary() {
    return vocabulary;
  }

  /**
   * @see Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(extension, qualname);
  }

  public boolean hasTerms() {
    return vocabulary != null;
  }

  public boolean isRequired() {
    return required;
  }

  /*
   * (non-Javadoc)
   * @see org.gbif.dwc.terms.ConceptTerm#qualifiedName()
   */
  @Override
  public String qualifiedName() {
    return qualname;
  }

  @Override
  public boolean isClass() {
    return false;
  }

  @Override
  public String prefix() {
    return "";
  }

  @Override
  public URI namespace() {
    return URI.create(namespace);
  }

  /*
   * (non-Javadoc)
   * @see org.gbif.dwc.terms.ConceptTerm#qualifiedNormalisedName()
   */
  public String qualifiedNormalisedName() {
    return TermFactory.normaliseTerm(qualifiedName());
  }

  public void setColumnLength(int columnLength) {
    this.columnLength = columnLength;
  }

  // required for SAX parser
  public void setColumnLength(String columnLength) {
    try {
      this.columnLength = Integer.parseInt(columnLength);
    } catch (NumberFormatException e) {
      // swallow stupidity
    }
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setExamples(String examples) {
    this.examples = examples;
  }

  public void setExtension(Extension extension) {
    this.extension = extension;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public void setQualname(String qualname) {
    this.qualname = qualname;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  // required by SAX parser
  public void setRequired(String required) {
    if ("TRUE".equalsIgnoreCase(required) || "T".equalsIgnoreCase(required) || "1".equalsIgnoreCase(required)) {
      this.required = true;
    } else if ("FALSE".equalsIgnoreCase(required) || "F".equalsIgnoreCase(required) || "0".equalsIgnoreCase(required)) {
      this.required = false;
    }

    // or we just don't change if not understood
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setVocabulary(Vocabulary vocabulary) {
    this.vocabulary = vocabulary;
  }

  /*
   * (non-Javadoc)
   * @see org.gbif.dwc.terms.ConceptTerm#simpleName()
   */
  @Override
  public String simpleName() {
    return name;
  }

  @Override
  public String prefixedName() {
    return simpleName();
  }

  /*
   * (non-Javadoc)
   * @see org.gbif.dwc.terms.ConceptTerm#simpleNormalisedAlternativeNames()
   */
  public String[] simpleNormalisedAlternativeNames() {
    return new String[]{};
  }

  /*
   * (non-Javadoc)
   * @see org.gbif.dwc.terms.ConceptTerm#simpleNormalisedName()
   */
  public String simpleNormalisedName() {
    return TermFactory.normaliseTerm(simpleName());
  }

  /**
   * @see Object#toString()
   */
  @Override
  public String toString() {
    return qualname;
  }

}
