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
  org.gbif.dwc.extensions.Vocabulary get(String uri);

  /**
   * Returns the parsed vocabulary located at the given URL. If downloaded already it will return the cached copy or
   * otherwise download it from the URL.
   *
   * @param url the resolvable URL that locates the xml vocabulary definition
   * @return
   */
  org.gbif.dwc.extensions.Vocabulary get(URL url);

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
