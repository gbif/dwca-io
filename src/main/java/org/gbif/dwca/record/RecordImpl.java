package org.gbif.dwca.record;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.dwca.io.ArchiveFile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class RecordImpl implements Record {

  private static final TermFactory TERM_FACTORY = TermFactory.instance();

  private final ArchiveField id;
  private final Map<Term, ArchiveField> fields;
  protected String[] row;
  private final Term rowType;
  private final boolean replaceNulls;
  private final boolean replaceEntities;

  /**
   * @param replaceNulls if true record values will have literal nulls replaced with NULL.
   * @param replaceEntities if true html & xml entities in record values will be replaced with the interpreted value.
   */
  public RecordImpl(ArchiveField id, Collection<ArchiveField> fields, Term rowType, boolean replaceNulls, boolean replaceEntities) {
    this.id = id;
    this.fields = new HashMap<Term, ArchiveField>();
    for (ArchiveField f : fields) {
      this.fields.put(f.getTerm(), f);
    }
    this.rowType = rowType;
    this.replaceNulls = replaceNulls;
    this.replaceEntities = replaceEntities;
  }

  /**
   * @param replaceNulls if true record values will have literal nulls replaced with NULL.
   * @param replaceEntities if true html & xml entities in record values will be replaced with the interpreted value.
   */
  public RecordImpl(ArchiveField id, Map<Term, ArchiveField> fields, Term rowType, boolean replaceNulls, boolean replaceEntities) {
    this.id = id;
    this.fields = fields;
    this.rowType = rowType;
    this.replaceNulls = replaceNulls;
    this.replaceEntities = replaceEntities;
  }

  /**
   * @param replaceNulls if true record values will have literal nulls replaced with NULL.
   * @param replaceEntities if true html & xml entities in record values will be replaced with the interpreted value.
   */
  public RecordImpl(ArchiveFile af, boolean replaceNulls, boolean replaceEntities) {
    this.id = af.getId();
    this.fields = af.getFields();
    this.rowType = af.getRowType();
    this.replaceNulls = replaceNulls;
    this.replaceEntities = replaceEntities;
  }

  public String column(int index) {
    if (row.length > index) {
      // if requested return column value cleaned
      return CleanUtils.clean(row[index], replaceNulls, replaceEntities);
    }
    return null;
  }

  public boolean hasRow() {
    return this.row != null;
  }

  public String id() {
    if (id != null) {
      return column(id.getIndex());
    }
    return null;
  }

  public Term rowType() {
    return rowType;
  }

  public void setRow(String[] row) {
    this.row = row;
  }

  @Override
  public String toString() {
    return "Record{" + id() + "}[" + StringUtils.join(row, "|") + "]";
  }

  private String value(ArchiveField f) {
    if (f != null) {
      if (f.getIndex() == null) {
        // if no column mapped use default "global" value
        return f.getDefaultValue();
      }
      String val = column(f.getIndex());
      if (StringUtils.isBlank(val)) {
        // if column is empty use default value
        return f.getDefaultValue();
      }
      // otherwise return already cleand column value
      return val;
    }
    return null;
  }

  /**
   * Returns core data file values based on a term concept.
   *
   * @return the value of the term in the core data file
   */
  public String value(Term term) {
    if (term == null) {
      return null;
    }
    return value(fields.get(term));
  }

  public Set<Term> terms() {
    return fields.keySet();
  }
}
