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

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Main interface of a {@link org.gbif.dwc.extensions.Vocabulary} manager.
 */
public interface VocabulariesManager {
  /**
   * Retrieve vocabulary by its unique global URI identifier from installed vocabularies.
   *
   * @param uri unique URI identifying the vocabulary as given in the vocabulary definition
   * @return the installed vocabulary or null if not found
   */
  Vocabulary get(String uri);

  /**
   * Returns the parsed vocabulary located at the given URL. If downloaded already it will return the cached copy or
   * otherwise download it from the URL.
   *
   * @param url the resolvable URL that locates the xml vocabulary definition
   * @return
   */
  Vocabulary get(URL url);

  /**
   * Returns a regular map than can be used to populate html select drop downs with
   * keys=vocabulary concept identifiers and values=preferred term for the given language.
   * Defaults to english if no term for the requested language exists.
   *
   * @param uri the identifier for the vocabulary
   * @param lang a 2 character iso language code, e.g. DE
   * @return
   */
  Map<String, String> getI18nVocab(String uri, String lang);

  /**
   * Lists all locally known vocabularies
   *
   * @return
   */
  List<Vocabulary> list();
}
