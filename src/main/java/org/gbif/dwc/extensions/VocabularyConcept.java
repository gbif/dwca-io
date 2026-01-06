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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A single, identifiable concept in a vocabulary. For example "DE" is an identifier for the concept of Germany, while
 * "Germany" is the preferred term or representation used in the English language. "Deutschland" represents the
 * preferred term in German, but both Germany and Deutschland are simply terms representing a single concept in a
 * vocabulary.
 *
 * Taken from https://github.com/gbif/dwca-validator3/
 */
public class VocabularyConcept implements Comparable<VocabularyConcept> {

  private static final Comparator<VocabularyConcept> COMPARATOR =
          Comparator.comparing(VocabularyConcept::getVocabulary,
                  Comparator.nullsFirst(Comparator.naturalOrder()))
                  .thenComparing(VocabularyConcept::getOrder)
                  .thenComparing(VocabularyConcept::getUri);

  private Vocabulary vocabulary;
  private String identifier; // usually short, e.g. DE
  private String description;
  private String uri; // a URI denoting the concept, mostly used in rdf
  private URL link; // web link to some more human documentation
  private int order = -1; // to maintain any custom order not based on a natural concept property
  private Set<VocabularyTerm> alternativeTerms = new HashSet<>();
  private Set<VocabularyTerm> preferredTerms = new HashSet<>();

  public void addAlternativeTerm(VocabularyTerm term) {
    alternativeTerms.add(term);
  }

  public void addPreferredTerm(VocabularyTerm term) {
    preferredTerms.add(term);
  }


  public Set<VocabularyTerm> getAlternativeTerms() {
    return alternativeTerms;
  }

  public String getDescription() {
    return description;
  }

  public String getIdentifier() {
    return identifier;
  }

  public URL getLink() {
    return link;
  }

  public Integer getOrder() {
    return order;
  }

  public VocabularyTerm getPreferredTerm(String lang) {
    VocabularyTerm tEN = null;
    for (VocabularyTerm t : preferredTerms) {
      if (t.getLang().equalsIgnoreCase(lang)) {
        return t;
      } else if (t.getLang().equalsIgnoreCase("en")) {
        tEN = t;
      }
    }
    return tEN;
  }

  public Set<VocabularyTerm> getPreferredTerms() {
    return preferredTerms;
  }

  /**
   * @return a set of all terms, preferred or alternative, for this concept
   */
  public Set<VocabularyTerm> getTerms() {
    Set<VocabularyTerm> t = new HashSet<>(preferredTerms);
    t.addAll(alternativeTerms);
    return t;
  }

  public String getUri() {
    return uri;
  }

  public Vocabulary getVocabulary() {
    return vocabulary;
  }


  public void setAlternativeTerms(Set<VocabularyTerm> alternativeTerms) {
    this.alternativeTerms = alternativeTerms;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public void setLink(String link) {
    try {
      this.link = new URL(link);
    } catch (MalformedURLException e) {
      // silently ignore malformed URLs
    }
  }

  public void setLink(URL link) {
    this.link = link;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public void setPreferredTerms(Set<VocabularyTerm> preferredTerms) {
    this.preferredTerms = preferredTerms;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public void setVocabulary(Vocabulary vocabulary) {
    this.vocabulary = vocabulary;
  }


  @Override
  public int hashCode() {
    return Objects.hash(vocabulary, identifier, uri);
  }

  @Override
  public int compareTo(VocabularyConcept object) {
    return COMPARATOR.compare(this, object);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof VocabularyConcept)) {
      return false;
    }
    VocabularyConcept o = (VocabularyConcept) other;
    return Objects.equals(vocabulary, o.vocabulary) &&
            Objects.equals(identifier, o.identifier) &&
            Objects.equals(uri, o.uri);
  }

  @Override
  public String toString() {
    return uri;
  }

}
