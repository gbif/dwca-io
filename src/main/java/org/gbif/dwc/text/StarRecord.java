package org.gbif.dwc.text;

import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.Term;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class StarRecord implements Iterable<Record> {

  private Record core;
  private final SortedMap<String, List<Record>> extensions;

  public StarRecord(Collection<String> extensions) {
    this.extensions = new TreeMap<String, List<Record>>();
    for (String rowType : extensions) {
      this.extensions.put(rowType, new ArrayList<Record>());
    }
  }

  public void addRecord(String rowType, Record record) {
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

  /**
   * Retrieves all extension records of a specific extension.
   * If the requested extension is not mapped null will be returned.
   *
   * @param rowType the Term representing the rowType
   *
   * @return possibly empty list of extension record or null if extension is not mapped at all
   */
  public List<Record> extension(Term rowType) {
    return extension(rowType.qualifiedName());
  }

  /**
   * Retrieves all extension records of a specific extension.
   * If the requested extension is not mapped null will be returned.
   *
   * @param rowType the exact qualified name of the rowType, e.g. http://rs.gbif.org/terms/1.0/VernacularName
   *
   * @return possibly empty list of extension record or null if extension is not mapped at all
   */
  public List<Record> extension(String rowType) {
    return extensions.get(rowType);
  }

  /**
   * Retrieves all extension records related to the core record across all extensions as a map.
   */
  public Map<String, List<Record>> extensions() {
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
  public Set<String> rowTypes() {
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
