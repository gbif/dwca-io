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
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.utils.file.ClosableIterator;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.tabular.TabularDataFileReader;
import org.gbif.utils.file.tabular.TabularFileNormalizer;
import org.gbif.utils.file.tabular.TabularFiles;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates information about a file contained within a Darwin Core Archive. It
 * represents the fileType object described in the Darwin Core Archive XSD.
 *
 * @see <a href="http://rs.tdwg.org/dwc/text/tdwg_dwc_text.xsd">Darwin Core Archive XSD</a>
 */
public class ArchiveFile implements Iterable<Record> {
  private static final Logger LOG = LoggerFactory.getLogger(ArchiveFile.class);

  private static final FileUtils FILE_UTILS = new FileUtils();
  private static final TermFactory TERM_FACTORY = TermFactory.instance();

  public static final Term DEFAULT_ID_TERM = TERM_FACTORY.findPropertyTerm("ARCHIVE_RECORD_ID");
  public static final Character DEFAULT_FIELDS_ENCLOSED_BY = '"';
  public static final String DEFAULT_FIELDS_TERMINATED_BY = ",";
  public static final String DEFAULT_LINES_TERMINATED_BY = "\n";

  /**
   * Comparator to sort by column index, then term name.
   */
  private static final Comparator<ArchiveField> AF_IDX_COMPARATOR = (o1, o2) -> {
    if (o1.getIndex() == null && o2.getIndex() == null) {
      return 0;
    } else if (o1.getIndex() == null) {
      return -1;
    } else if (o2.getIndex() == null) {
      return 1;
    } else if (o1.getIndex().equals(o2.getIndex())) {
      return o1.getTerm().qualifiedName().compareTo(o2.getTerm().qualifiedName());
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
  private String encoding = FileUtils.UTF8;
  private Term rowType; // Default is http://rs.tdwg.org/dwc/xsd/simpledarwincore/SimpleDarwinRecord
  private Integer ignoreHeaderLines = 0;
  private String dateFormat = "YYYY-MM-DD";

  // TODO: Change to SortedMap and remove rawArchiveFields?
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

  protected void validateAsCore(boolean hasExtensions) throws UnsupportedArchiveException {
    if (hasExtensions) {
      if (id == null) {
        LOG.warn("DwC-A core data file »" + title + "« is lacking an id column. No extensions allowed in this case");
      }
    }
    validate();
  }

  protected void validateAsExtension() throws UnsupportedArchiveException {
    if (id == null) {
      throw new UnsupportedArchiveException("DwC-A data file »" + title + "« requires an id or foreign key to the core id");
    }
    validate();
  }

  protected void validate() throws UnsupportedArchiveException {
    if (getLocation() == null) {
      throw new UnsupportedArchiveException("DwC-A data file »" + title + "« requires a location");
    }
    if (!getLocationFile().exists()) {
      throw new UnsupportedArchiveException("DwC-A data file »" + title + "« does not exist");
    }
    if (encoding == null) {
      throw new UnsupportedArchiveException("DwC-A data file »" + title + "« requires a character encoding");
    }
  }

  /**
   * Normalizes and sorts the data file, so it is suitable for in-order iteration as a core or extension file.
   *
   * @return the file was sorted or not. If the file was not sorted it simply means it was not required.
   */
  /*
   * Sorting implies a normalization phase to ensure we sort the file properly.
   * Note that the file will not be sorted if the sorted file is already there and its date is later than the file
   * we want to sort.
   *
   * This method is synchronized to prevent multiple threads trying to normalize/sort a file at the same time,
   * and uses an advisory lock to avoid conflict between multiple processes.
   */
  protected synchronized boolean normalizeAndSort() throws IOException {
    File fileToSort = getLocationFile();
    File sortedFile = getLocationFileSorted(getLocationFile());

    File lockFile = getLocationLockFile(getLocationFile());
    RandomAccessFile lockFileRA = new RandomAccessFile(lockFile, "rw");

    FileLock lock = lockFileRA.getChannel().tryLock();
    if (lock == null) {
      LOG.warn("Another process has locked this DWCA for initialization; waiting until the lock is released.");
      lock = lockFileRA.getChannel().lock();
      LOG.warn("Other process has released lock; lock taken, proceeding.");
    }

    try {
      // If we already sorted the file and its source didn't change we can avoid doing it again
      if (sortedFile.exists() && Files.getLastModifiedTime(sortedFile.toPath()).toInstant().isAfter(
          Files.getLastModifiedTime(fileToSort.toPath()).toInstant())) {
        LOG.debug("File {} is already sorted ({}B)", sortedFile, sortedFile.length());
        return false;
      }

      File normalizedFile = normalizeIfRequired();
      if (normalizedFile != null) {
        fileToSort = normalizedFile;
      }

      FILE_UTILS.sort(fileToSort, getLocationFileSorted(getLocationFile()), getEncoding(),
          getId().getIndex(), getFieldsTerminatedBy(), getFieldsEnclosedBy(),
          TabularFileNormalizer.NORMALIZED_END_OF_LINE, getIgnoreHeaderLines());

      if (normalizedFile != null) {
        Files.deleteIfExists(normalizedFile.toPath());
      }

      return true;
    } finally {
      lock.release();
      lockFileRA.close();
    }
  }

  /**
   * Apply file normalization if required.
   **
   * @return normalizedFile or null if normalization was not applied
   *
   * @throws IOException
   */
  protected File normalizeIfRequired() throws IOException {
    // If the linesTerminatedBy used is the same as TabularFileNormalizer and no quoted cells are used
    // we can skip normalization
    boolean normalizationRequired = ! TabularFileNormalizer.NORMALIZED_END_OF_LINE.equals(getLinesTerminatedBy())
        || getFieldsEnclosedBy() != null;

    if (normalizationRequired) {
      File normalizedFile = getLocationFileNormalized(getLocationFile());
      TabularFileNormalizer.normalizeFile(getLocationFile().toPath(), normalizedFile.toPath(),
          Charset.forName(getEncoding()), getFieldsTerminatedByChar(),
          getLinesTerminatedBy(), getFieldsEnclosedBy());
      return normalizedFile;
    }
    return null;
  }

  protected static File getLocationFileNormalized(File location) {
    return new File(location.getParentFile(), location.getName() + "-normalized");
  }

  protected static File getLocationFileSorted(File location) {
    return new File(location.getParentFile(), location.getName() + "-sorted");
  }

  private static File getLocationLockFile(File location) {
    return new File(location.getParentFile(), location.getName() + "-lock");
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
    return getField(TERM_FACTORY.findPropertyTerm(term));
  }

  public Map<Term, ArchiveField> getFields() {
    return fields;
  }

  public Character getFieldsEnclosedBy() {
    return fieldsEnclosedBy;
  }

  /**
   * Provides the List of {@link ArchiveField}s, ordered by the column they are mapped to, with unmapped (default/fixed value)
   * columns first.
   *
   * @return Ordered data structure of archive fields.
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
   * Generates a list of {@link Term}s mapped to each column in the underlying file.
   *
   * A list may be empty when no Term is mapped at a specific position, or may contain more than one Term where
   * the same column is mapped more than once.
   *
   * The length of the list is the maximum index used in the {@link ArchiveField}s + 1.
   *
   * @return Data structure representing the header of the file.
   */
  public List<List<Term>> getHeader() {
    List<ArchiveField> archiveFieldsWithIndex = getFieldsSorted()
            .stream().filter(af -> af.getIndex() != null)
            .collect(Collectors.toList());

    Optional<Integer> idIndex = id != null ? Optional.of(id.getIndex()) : Optional.empty();

    if (archiveFieldsWithIndex.isEmpty() && !idIndex.isPresent()){
      return new ArrayList<>();
    }

    int maxIndex = archiveFieldsWithIndex.stream().mapToInt(ArchiveField::getIndex).max().getAsInt();
    maxIndex = Math.max(maxIndex, idIndex.orElse(-1));

    List<List<Term>> terms = new ArrayList<>();
    for (int i = 0; i <= maxIndex; i++) {
      terms.add(new ArrayList<Term>());
    }
    archiveFieldsWithIndex.stream().forEach(af -> terms.get(af.getIndex()).add(af.getTerm()));
    // Assign default term for id column.
    idIndex.ifPresent(idx -> terms.get(idx).add(DEFAULT_ID_TERM));
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

  /**
   * Get the fieldsTerminatedBy as a character.
   *
   * @return character which terminates fields.
   */
  protected char getFieldsTerminatedByChar() {
    Objects.requireNonNull(fieldsTerminatedBy, "fieldsTerminatedBy shall be provided");
    if (fieldsTerminatedBy.length() != 1) {
      throw new IllegalArgumentException();
    }
    return fieldsTerminatedBy.charAt(0);
  }

  public ArchiveField getId() {
    return id;
  }

  public Integer getIgnoreHeaderLines() {
    return ignoreHeaderLines;
  }

  /**
   * Determines if header line are included based on an optional integer.
   *
   * @return true if one or more header lines are included.
   */
  protected boolean areHeaderLinesIncluded() {
    return ignoreHeaderLines != null && ignoreHeaderLines > 0;
  }

  /**
   * Determines the number of line to skip <strong>before a header line</strong> or return null.
   *
   * The TabularDataFileReader assumes a header line of column names, so we subtract 1.
   *
   * @return number of lines to skip, or null.
   */
  protected Integer getLinesToSkipBeforeHeader() {
    if (ignoreHeaderLines != null && ignoreHeaderLines > 1) {
      return ignoreHeaderLines - 1;
    }
    return null;
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

  /**
   * Get a {@link ClosableIterator}, over the records in this file, replacing nulls and entities.
   *
   * @return
   */
  @Override
  public ClosableIterator<Record> iterator() {
    return iterator(true, true);
  }

  private Reader getReader(boolean sorted) throws IOException {
    // ArchiveFile location, or Archive in case this is a fake single-file "archive".
    File file = getLocationFile() != null ? getLocationFile() : getArchive().getLocation();
    if (sorted) {
      file = getLocationFileSorted(file);
    }
    return Files.newBufferedReader(file.toPath(), Charset.forName(getEncoding()));
  }

  /**
   * Get a {@link ClosableIterator} over the records in this file, optionally replacing nulls and entities.
   *
   * @param replaceNulls    if true replaces common, literal NULL values with real nulls, e.g. "\N" or "NULL"
   * @param replaceEntities if true HTML & XML entities in record values will be replaced with the interpreted value.
   */
  public ClosableIterator<Record> iterator(boolean replaceNulls, boolean replaceEntities) {
    try {
      TabularDataFileReader<List<String>> tabularFileReader = TabularFiles.newTabularFileReader(getReader(false),
          getFieldsTerminatedByChar(), getLinesTerminatedBy(), getFieldsEnclosedBy(),
          areHeaderLinesIncluded(), getLinesToSkipBeforeHeader());
      return new DwcRecordIterator(tabularFileReader, getId(), getFields(), getRowType(), replaceNulls, replaceEntities);
    } catch (IOException e) {
      throw new UnsupportedArchiveException(e);
    }
  }

  /**
   * Build an iterator pointing to the sorted tabular file.
   * The sorted tabular file is also assumed to have been normalized.
   *
   * @param replaceNulls
   * @param replaceEntities
   *
   * @return
   *
   * @throws IOException
   */
  protected ClosableIterator<Record> sortedIterator(boolean replaceNulls, boolean replaceEntities) throws IOException {
    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles.newTabularFileReader(getReader(true),
            getFieldsTerminatedByChar(), TabularFileNormalizer.NORMALIZED_END_OF_LINE, getFieldsEnclosedBy(),
            areHeaderLinesIncluded(), getLinesToSkipBeforeHeader());
    return new DwcRecordIterator(tabularFileReader, getId(), getFields(), getRowType(), replaceNulls, replaceEntities);
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

  @Override
  public String toString() {
    return "ArchiveFile " + title;
  }
}
