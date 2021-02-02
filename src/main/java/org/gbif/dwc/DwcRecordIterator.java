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
package org.gbif.dwc;

import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.RecordImpl;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.ClosableIterator;
import org.gbif.utils.file.tabular.TabularDataFileReader;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * {@link ClosableIterator} of {@link Record}
 */
class DwcRecordIterator implements ClosableIterator<Record> {

  private final TabularDataFileReader<List<String>> tabularFileReader;

  private final ArchiveField id;
  private final Map<Term, ArchiveField> fields;
  private final Term rowType;
  private final boolean replaceEntities;
  private final boolean replaceNulls;

  private List<String> nextLine;
  private boolean end = false;

  /**
   * @param replaceNulls    if true record values will have literal nulls replaced with NULL.
   * @param replaceEntities if true HTML & XML entities in record values will be replaced with the interpreted value.
   */
  DwcRecordIterator(TabularDataFileReader<List<String>> tabularFileReader, ArchiveField id, Map<Term,
          ArchiveField> fields, Term rowType, boolean replaceNulls, boolean replaceEntities) {
    this.tabularFileReader = tabularFileReader;
    this.id = id;
    this.fields = fields;
    this.rowType = rowType;
    this.replaceNulls = replaceNulls;
    this.replaceEntities = replaceEntities;
  }

  @Override
  public void close() throws IOException {
    end = true;
    tabularFileReader.close();
    nextLine = null;
  }

  @Override
  public boolean hasNext() {

    if (nextLine != null) {
      return true;
    }

    if (end) {
      return false;
    }

    try {
      nextLine = tabularFileReader.read();
    } catch (ParseException | IOException var2) {
      try {
        this.close();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      throw new IllegalStateException(var2);
    }
    return nextLine != null;
  }

  @Override
  public Record next() {
    RecordImpl record;
    if (hasNext()) {
      record = new RecordImpl(id, fields, rowType, replaceNulls, replaceEntities);
      List<String> currentLine = nextLine;
      nextLine = null;
      record.setRow(currentLine.toArray(new String[currentLine.size()]));
    } else {
      end = true;
      record = null;
    }
    return record;
  }

  @Override
  public String toString() {
    return String.format("DwcRecordIterator %s, %s, %s, %s, %s, %s",
        id, fields, rowType, replaceNulls, replaceEntities, tabularFileReader);
  }
}
