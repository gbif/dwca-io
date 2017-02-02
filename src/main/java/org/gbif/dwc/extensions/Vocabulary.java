package org.gbif.dwc.extensions;

/*
 * Copyright 2009-2017 GBIF.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


/**
 * Taken from https://github.com/gbif/dwca-validator3/
 */
public class Vocabulary implements Comparable<org.gbif.dwc.extensions.Vocabulary> {
  private String uri;
  private String title;
  private String description;
  private String subject;
  private URL link;
  private List<VocabularyConcept> concepts = new LinkedList<>();
  // the data this local vocabulary copy was last updated
  private Date lastUpdate = new Date();

  public void addConcept(VocabularyConcept concept) {
    concept.setVocabulary(this);

    if (concept.getOrder() == -1) {
      // set the order to be the next one
      int maxOrder = 0;
      for (VocabularyConcept tc : concepts) {
        if (tc.getOrder() >= 0 && maxOrder < tc.getOrder()) {
          maxOrder = tc.getOrder();
        }
      }
      concept.setOrder(maxOrder + 1);
    }
    concepts.add(concept);
  }


  public List<VocabularyConcept> getConcepts() {
    return concepts;
  }

  public void setConcepts(List<VocabularyConcept> concepts) {
    this.concepts = concepts;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public URL getLink() {
    return link;
  }

  public void setLink(URL link) {
    this.link = link;
  }

  public void setLink(String link) {
    URL url;
    try {
      url = new URL(link);
      this.link = url;
    } catch (MalformedURLException ignore) {
    }
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  @Override
  public String toString() {
    return uri;
  }

  @Override
  public int compareTo(org.gbif.dwc.extensions.Vocabulary object) {
    return Objects.compare(this.uri, object.uri, String::compareTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri);
  }


  /**
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof org.gbif.dwc.extensions.Vocabulary)) {
      return false;
    }
    org.gbif.dwc.extensions.Vocabulary o = (org.gbif.dwc.extensions.Vocabulary) other;
    return Objects.equals(uri, o.uri);
  }

}