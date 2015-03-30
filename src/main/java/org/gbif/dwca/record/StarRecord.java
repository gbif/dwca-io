package org.gbif.dwca.record;

import org.gbif.dwc.terms.Term;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StarRecord extends Iterable<Record> {

  /**
   * @return the core record
   */
  Record core();

  boolean hasExtension(Term rowType);

  /**
   * Retrieves all extension records related to the core record across all extensions as a map.
   */
  Map<Term, List<Record>> extensions();

  /**
   * Retrieves all extension records of a specific extension.
   * If the requested extension is not mapped null will be returned.
   *
   * @param rowType the Term representing the rowType
   *
   * @return possibly empty list of extension record or null if extension is not mapped at all
   */
  List<Record> extension(Term rowType);

  /**
   * @return set of extension rowTypes associated with this star record
   */
  Set<Term> rowTypes();

  /**
   * @return the number of associated extension records across all rowTypes
   */
  int size();
}
