package org.gbif.dwc.extensions;

/*
 * Copyright 2009-2017 GBIF.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * A Darwin Core extension definition
 *
 * Taken from https://github.com/gbif/dwca-validator3/
 */
public class Extension implements Comparable<org.gbif.dwc.extensions.Extension>{

  private static final Comparator<org.gbif.dwc.extensions.Extension> COMPARATOR =
          Comparator.comparing(((org.gbif.dwc.extensions.Extension ext) -> ext.getRowType() == null ? null :
                  ext.getRowType().qualifiedName()), Comparator.nullsLast(Comparator.naturalOrder()))
                  .thenComparing(ext -> ext.getUrl() == null ? null : ext.getUrl().toString(),
                          Comparator.nullsLast(Comparator.naturalOrder()));

  private String title; // human title
  private String name; // table, file & xml tag naming. no whitespace allowed
  private URL url;
  private Term rowType;
  private String subject;
  private String description;
  private String namespace;
  private URL link; // to documentation
  private boolean installed;
  private List<ExtensionProperty> properties = new ArrayList<>();
  private boolean core = false;
  private boolean dev = false;
  private Date modified = new Date();

  public Extension(URL url, boolean dev) {
    super();
    this.url = url;
    this.dev = dev;
  }

  public void addProperty(ExtensionProperty property) {
    property.setExtension(this);
    properties.add(property);
  }

  @Override
  public int compareTo(org.gbif.dwc.extensions.Extension object) {
    return COMPARATOR.compare(this, object);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof org.gbif.dwc.extensions.Extension)) {
      return false;
    }
    org.gbif.dwc.extensions.Extension o = (org.gbif.dwc.extensions.Extension) other;
    return Objects.equals(rowType, o.rowType) &&
            Objects.equals(url.toString(), o.url.toString());
  }

  public String getDescription() {
    return description;
  }

  public URL getLink() {
    return link;
  }

  public Date getModified() {
    return modified;
  }

  public String getName() {
    return name;
  }

  public String getNamespace() {
    return namespace;
  }

  public List<ExtensionProperty> getProperties() {
    return properties;
  }

  public ExtensionProperty getProperty(Term term) {
    return getProperty(term.qualifiedName());
  }

  public ExtensionProperty getProperty(String term) {
    if (term == null) {
      return null;
    }
    for (ExtensionProperty p : properties) {
      if (term.equalsIgnoreCase(p.getQualname())) {
        return p;
      }
    }
    return null;
  }

  public Term getRowType() {
    return rowType;
  }

  public String getSubject() {
    return subject;
  }

  public String getTitle() {
    return title;
  }

  public URL getUrl() {
    return url;
  }

  /**
   * @see Objects#hash
   */
  @Override
  public int hashCode() {
    return Objects.hash(rowType, url);
  }

  public boolean hasProperty(Term term) {
    return getProperty(term) != null;
  }

  public boolean hasProperty(String term) {
    return getProperty(term) != null;
  }

  public boolean isCore() {
    return core;
  }

  public boolean isDev() {
    return dev;
  }

  public boolean isInstalled() {
    return installed;
  }

  public void setCore(boolean core) {
    this.core = core;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setInstalled(boolean installed) {
    this.installed = installed;
  }

  public void setLink(String link) {
    URL url;
    try {
      url = new URL(link);
      this.link = url;
    } catch (MalformedURLException e) {
    }
  }

  public void setLink(URL link) {
    this.link = link;
  }

  public void setModified(Date modified) {
    this.modified = modified;
  }

  public void setName(String name) {
    this.name = name.replaceAll("\\s", "_");
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public void setProperties(List<ExtensionProperty> properties) {
    this.properties = properties;
  }

  public void setRowType(String rowType) {
    setRowType(TermFactory.instance().findTerm(rowType));
  }

  public void setRowType(Term rowType) {
    this.rowType = rowType;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("name:" + this.name).append(", rowType:" + this.rowType).toString();
  }

}
