package org.gbif.dwc.record;

import org.gbif.dwc.terms.Term;

import java.util.Collection;

public interface Record {

  String column(int index);

  String id();

  String rowType();

  String value(Term term);

  String value(String qterm);

  Collection<Term> terms();

}
