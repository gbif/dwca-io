/*
 * Copyright 2010 Global Biodiversity Informatics Facility.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gbif.dwca.io;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.RecordIterator;
import org.gbif.util.CSVReaderHelper;
import org.gbif.utils.file.csv.CSVReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * This class can be used to encapsulate information about a file contained within a Darwin Core Archive. It generally
 * represents the fileType object described in the Darwin Core Archive XSD.
 *
 * @see <a href="http://rs.tdwg.org/dwc/text/tdwg_dwc_text.xsd">Darwin Core Archive XSD</a>
 */
public class ArchiveFile implements Iterable<Record> {
  private static final TermFactory TERM_FACTORY = TermFactory.instance();

  public static final Term DEFAULT_ID_TERM = TermFactory.instance().findTerm("ARCHIVE_RECORD_ID");
  public static final Character DEFAULT_FIELDS_ENCLOSED_BY = '"';
  public static final String DEFAULT_FIELDS_TERMINATED_BY = ",";
  public static final String DEFAULT_LINES_TERMINATED_BY = "\n";

  private static final Comparator<ArchiveField> AF_IDX_COMPARATOR = (o1, o2) -> {
    if (o1.getIndex() == null && o2.getIndex() == null) {
      return 0;
    } else if (o1.getIndex() == null) {
      return -1;
    } else if (o2.getIndex() == null) {
      return 1;
    } else if (o1.getIndex().equals(o2.getIndex())) {
      return o1.getTerm().qualifiedName().compareToIgnoreCase(o2.getTerm().qualifiedName());
    } else {
      return o1.getIndex().compareTo(o2.getIndex());
    }
  };

  private ArchiveField id;
  private Archive archive;
  private final LinkedList<String> locations = new LinkedList<String>();
  private String title;
  private String fieldsTerminatedBy = DEFAULT_FIELDS_TERMINATED_BY;
  private Character fieldsEnclosedBy = DEFAULT_FIELDS_ENCLOSED_BY;
  private String linesTerminatedBy = DEFAULT_LINES_TERMINATED_BY;
  private String encoding = "utf8";
  private Term rowType;
  private Integer ignoreHeaderLines = 0;

  private String dateFormat = "YYYY-MM-DD";

  private final Map<Term, ArchiveField> fields = new HashMap<>();
  private final List<ArchiveField> rawArchiveFields = new ArrayList<>();

  public static ArchiveFile buildCsvFile() {
    ArchiveFile af = new ArchiveFile();
    af.setFieldsEnclosedBy(DEFAULT_FIELDS_ENCLOSED_BY);
    af.setFieldsTerminatedBy(DEFAULT_FIELDS_TERMINATED_BY);
    return af;
  }

  public static ArchiveFile buildTabFile() {
    ArchiveFile af = new ArchiveFile();
    af.setFieldsEnclosedBy(null);
    af.setFieldsTerminatedBy("\t");
    return af;
  }

  public static File getLocationFileSorted(File location) {
    return new File(location.getParentFile(), location.getName() + "-sorted");
  }

  public static String getLocationSorted(String location) {
    return location + "-sorted";
  }

  public void addField(ArchiveField field) {
    fields.put(field.getTerm(), field);
    rawArchiveFields.add(field);
  }

  public void addLocation(String location) {
    if (title == null) {
      if (location != null && location.lastIndexOf('/') > 1) {
        title = location.substring(location.lastIndexOf('/') + 1, location.length());
      } else {
        title = location;
      }
    }
    locations.add(location);
  }

  public Archive getArchive() {
    return archive;
  }

  public CSVReader getCSVReader() throws IOException {
    return CSVReaderHelper.build(this);
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public String getEncoding() {
    return encoding;
  }

  public ArchiveField getField(Term term) {
    if (term == null) {
      return null;
    }
    return fields.get(term);
  }

  public ArchiveField getField(String term) {
    return getField(TERM_FACTORY.findTerm(term));
  }

  public Map<Term, ArchiveField> getFields() {
    return fields;
  }

  public Character getFieldsEnclosedBy() {
    return fieldsEnclosedBy;
  }

  /**
   *
   * @return
   */
  public List<ArchiveField> getFieldsSorted() {
    List<ArchiveField> list = new ArrayList<>(fields.values());
    Collections.sort(list, AF_IDX_COMPARATOR);
    return list;
  }

  /**
   * Return a copy of the raw {@link ArchiveField}.
   * This function is mostly used for validation purpose.
   * May include duplicates.
   * @return
   */
  public List<ArchiveField> getRawArchiveFields() {
    return new ArrayList<>(rawArchiveFields);
  }

  /**
   * Deprecated: use getFieldsSorted(), see https://github.com/gbif/dwca-io/issues/41 for details
   *
   * Generates an ordered array representing all the {@link Term} matching the position in the underlying file.
   * The array can contain {@code null} if no {@link Term} is mapped at a specific position.
   * The size of the array is defined by the maximum index used within {@link ArchiveField} + 1 (since indices are 0
   * based).
   *
   * @return Array of {@link Term} representing the header or an empty Array if no headers are present.
   */
  @Deprecated
  public Term[] getHeader() {
    List<ArchiveField> archiveFieldsWithIndex = getFieldsSorted()
            .stream().filter(af -> af.getIndex() != null)
            .collect(Collectors.toList());

    Optional<Integer> idIndex = id != null ? Optional.of(id.getIndex()) : Optional.empty();

    if(archiveFieldsWithIndex.isEmpty() && !idIndex.isPresent()){
      return new Term[0];
    }

    int maxIndex = archiveFieldsWithIndex.stream()
            .mapToInt(ArchiveField::getIndex).max().getAsInt();
    maxIndex = Math.max(maxIndex, idIndex.orElse(-1));

    Term[] terms = new Term[maxIndex + 1];
    // handle id column, assign default Term, it will be rewritten below if assigned to a term
    idIndex.ifPresent(idx -> terms[idx] = DEFAULT_ID_TERM);
    archiveFieldsWithIndex.stream().forEach(af -> terms[af.getIndex()] = af.getTerm());
    return terms;
  }

  /**
   * Get default values mapped to their {@link Term} (if any).
   *
   * @return default values or {@code Optional.empty()} if none
   */
  public Optional<Map<Term, String>> getDefaultValues() {
    //check if there is default value(s) defined (look for absence of index)
    Map<Term, String> defaultValues = fields.values().stream()
            .filter(af -> StringUtils.isNotBlank(af.getDefaultValue()))
            .collect(Collectors.toMap(ArchiveField::getTerm, ArchiveField::getDefaultValue));
    return defaultValues.isEmpty() ? Optional.empty() : Optional.of(defaultValues);
  }

  public String getFieldsTerminatedBy() {
    return fieldsTerminatedBy;
  }

  public ArchiveField getId() {
    return id;
  }

  public Integer getIgnoreHeaderLines() {
    return ignoreHeaderLines;
  }

  public String getLinesTerminatedBy() {
    return linesTerminatedBy;
  }

  /**
   * Get the first file location.
   * TODO: check if we got more than 1 file and implement some unix concat into a single file first before sorting that
   * @return first location or null if none
   */
  public String getLocation() {
    if(locations.isEmpty()){
      return null;
    }
    return locations.getFirst();
  }

  public File getLocationFile() {
    File dataFile;
    if (archive != null) {
      if (getLocation() == null) {
        // use only archive
        dataFile = archive.getLocation();
      } else if (getLocation().startsWith("/")) {
        // absolute already
        dataFile = new File(getLocation());
      } else {
        // use source file relative to archive dir
        Path archiveLocation = archive.getLocation().toPath();
        File directory = Files.isDirectory(archiveLocation) ? archiveLocation.toFile() : archiveLocation.getParent().toFile();
        dataFile = new File(directory, getLocation());
      }
    } else {
      dataFile = new File(getLocation());
    }

    return dataFile;
  }

  public List<String> getLocations() {
    return locations;
  }

  public Term getRowType() {
    return rowType;
  }

  public Set<Term> getTerms() {
    return fields.keySet();
  }

  public String getTitle() {
    return title;
  }

  public boolean hasTerm(Term term) {
    return getField(term) != null;
  }

  public boolean hasTerm(String term) {
    return getField(term) != null;
  }

  public Iterator<Record> iterator() {
    return RecordIterator.build(this, true, true);
  }

  public void setArchive(Archive archive) {
    this.archive = archive;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public void setEncoding(String encoding) {
    this.encoding = StringUtils.trimToNull(encoding);
  }

  public void setFieldsEnclosedBy(Character fieldsEnclosedBy) {
    this.fieldsEnclosedBy = fieldsEnclosedBy;
  }

  public void setFieldsTerminatedBy(String fieldsTerminatedBy) {
    this.fieldsTerminatedBy = StringUtils.defaultIfEmpty(fieldsTerminatedBy, null);
  }

  public void setId(ArchiveField id) {
    this.id = id;
  }

  public void setIgnoreHeaderLines(Integer ignoreHeaderLines) {
    if (ignoreHeaderLines == null || ignoreHeaderLines < 0) {
      ignoreHeaderLines = 0;
    }
    this.ignoreHeaderLines = ignoreHeaderLines;
  }

  public void setLinesTerminatedBy(String linesTerminatedBy) {
    this.linesTerminatedBy = StringUtils.defaultIfEmpty(linesTerminatedBy, null);
  }

  public void setRowType(Term rowType) {
    this.rowType = rowType;
  }
}
