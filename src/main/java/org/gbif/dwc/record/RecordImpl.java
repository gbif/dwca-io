package org.gbif.dwc.record;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwc.text.ArchiveField;
import org.gbif.dwc.text.ArchiveFile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class RecordImpl implements Record {

  private static final Pattern NULL_REPL = Pattern.compile("^\\s*(null|\\\\N)?\\s*$", Pattern.CASE_INSENSITIVE);
  private static final TermFactory TERM_FACTORY = TermFactory.instance();

  private final ArchiveField id;
  private final Map<Term, ArchiveField> fields;
  protected String[] row;
  private final Term rowType;
  private final boolean replaceNulls;

  public RecordImpl(ArchiveField id, Collection<ArchiveField> fields, Term rowType, boolean replaceNulls) {
    this.id = id;
    this.fields = new HashMap<Term, ArchiveField>();
    for (ArchiveField f : fields) {
      this.fields.put(f.getTerm(), f);
    }
    this.rowType = rowType;
    this.replaceNulls = replaceNulls;
  }

  public RecordImpl(ArchiveField id, Map<Term, ArchiveField> fields, Term rowType, boolean replaceNulls) {
    this.id = id;
    this.fields = fields;
    this.rowType = rowType;
    this.replaceNulls = replaceNulls;
  }

  public RecordImpl(ArchiveFile af, boolean replaceNulls) {
    this.id = af.getId();
    this.fields = af.getFields();
    this.rowType = af.getRowType();
    this.replaceNulls = replaceNulls;
  }

  /**
   * Method that replaces common, literal NULL values with real nulls.
   * For example you often find "null", "NULL" or "\N" as values in text files.
   * This method is not used by the value() methods under the hood to allow access to raw data in case NULL makes
   * sense.
   *
   * @return the input string or null in case its a literal form of NULL
   */
  protected String replaceNull(String val) {
    if (val == null || NULL_REPL.matcher(val).find()) {
      return null;
    }
    return val;
  }

  public String column(int index) {
    if (row.length > index) {
      return row[index];
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
      if (StringUtils.trimToNull(val) == null) {
        // if column is empty use default value
        return f.getDefaultValue();
      }
      // otherwise return column value, if requested with cleaned nulls
      return replaceNulls ? replaceNull(val) : val;
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
