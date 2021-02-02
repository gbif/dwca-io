/*
 * Copyright 2021 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.dwc.record;

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
  @Override
  public Record core() {
    return core;
  }

  @Override
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
  @Override
  public List<Record> extension(Term rowType) {
    return extensions.get(rowType);
  }

  /**
   * Retrieves all extension records related to the core record across all extensions as a map.
   */
  @Override
  public Map<Term, List<Record>> extensions() {
    return extensions;
  }

  /**
   * Get a new iterator over all extension records, no matter to which extension they belong.
   *
   * @return the ExtensionRecord iterator, reusing the same instance for each call
   */
  @Override
  public Iterator<Record> iterator() {
    List<Record> records = new ArrayList<>();
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
  @Override
  public Set<Term> rowTypes() {
    return extensions.keySet();
  }

  /**
   * @return the number of associated extension records across all rowTypes
   */
  @Override
  public int size() {
    int x = 0;
    for (List<Record> recs : extensions.values()) {
      x += recs.size();
    }
    return x;
  }

  @Override
  public String toString() {
    return "StarRecord with core "+core.toString()+" and extensions "+extensions;
  }
}
