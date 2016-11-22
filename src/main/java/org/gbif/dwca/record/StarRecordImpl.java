package org.gbif.dwca.record;

import org.gbif.dwc.terms.Term;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StarRecordImpl implements StarRecord {

  private Record core;
  private final Map<Term, List<Record>> extensions;

  public StarRecordImpl(Collection<Term> extensions) {
    this.extensions = new HashMap<Term, List<Record>>();
    for (Term rowType : extensions) {
      this.extensions.put(rowType, new ArrayList<Record>());
    }
  }

  public void addRecord(Term rowType, Record record) {
    if (!extensions.containsKey(rowType)) {
      throw new IllegalArgumentException("RowType not supported");
    }
    extensions.get(rowType).add(record);
  }

  /**
   * @return the core record
   */
  public Record core() {
    return core;
  }

  public boolean hasExtension(Term rowType) {
    return extensions.containsKey(rowType) && !extensions.get(rowType).isEmpty();
  }

  /**
   * Retrieves all extension records of a specific extension.
   * If the requested extension is not mapped null will be returned.
   *
   * @param rowType the Term representing the rowType
   *
   * @return possibly empty list of extension record or null if extension is not mapped at all
   */
  public List<Record> extension(Term rowType) {
    return extensions.get(rowType);
  }

  /**
   * Retrieves all extension records related to the core record across all extensions as a map.
   */
  public Map<Term, List<Record>> extensions() {
    return extensions;
  }

  /**
   * Get a new iterator over all extension records, no matter to which extension they belong.
   *
   * @return the ExtensionRecord iterator, reusing the same instance for each call
   */
  public Iterator<Record> iterator() {
    List<Record> records = new ArrayList<Record>();
    for (List<Record> recs : extensions.values()) {
      records.addAll(recs);
    }
    return records.iterator();
  }

  public void newCoreRecord(Record core) {
    this.core = core;
    for (List<Record> lists : extensions.values()) {
      lists.clear();
    }
  }

  /**
   * @return set of extension rowTypes associated with this star record
   */
  public Set<Term> rowTypes() {
    return extensions.keySet();
  }

  /**
   * @return the number of associated extension records across all rowTypes
   */
  public int size() {
    int x = 0;
    for (List<Record> recs : extensions.values()) {
      x += recs.size();
    }
    return x;
  }
}
