package org.gbif.dwc;

import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.RecordImpl;
import org.gbif.dwca.record.RecordIterator;
import org.gbif.utils.file.ClosableIterator;
import org.gbif.utils.file.tabular.TabularDataFileReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * {@link ClosableIterator} of {@link Record}
 * This is an revised version of {@link RecordIterator}
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
   * @param replaceEntities if true html & xml entities in record values will be replaced with the interpreted value.
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
    } catch (IOException var2) {
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

}

