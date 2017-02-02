/*
 * Copyright 2009-2017 GBIF.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gbif.dwc.extensions;


import java.util.Objects;


/**
 * A single literal representation of a vocabulary concept in a given language.
 *
 * Taken from https://github.com/gbif/dwca-validator3/
 */
public class VocabularyTerm {

  private String title;
  private String lang;

  public void setLang(String lang) {
    this.lang = lang;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getLang() {
    return lang;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof org.gbif.dwc.extensions.VocabularyTerm)) {
      return false;
    }
    org.gbif.dwc.extensions.VocabularyTerm o = (org.gbif.dwc.extensions.VocabularyTerm) other;
    return Objects.equals(title, o.title) && Objects.equals(lang, o.lang);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, lang);
  }

  @Override
  public String toString() {
    return String.format("%s [%s]", title, lang);
  }

}
