package org.gbif.dwca.record;

import org.gbif.dwc.terms.Term;

import java.util.Set;

public interface Record {

  /**
   * @return the record id
   */
  String id();

  /**
   * @return the row type of the record
   */
  Term rowType();

  /**
   * Returns a record value based on a term concept.
   */
  String value(Term term);

  /**
   * Returns a record value based on a column index.
   */
  String column(int index);

  /**
   * @return the set of all mapped / available concept terms
   */
  Set<Term> terms();

}
