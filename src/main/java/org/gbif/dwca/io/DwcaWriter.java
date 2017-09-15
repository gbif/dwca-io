package org.gbif.dwca.io;
/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.registry.Dataset;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.record.Record;
import org.gbif.io.TabWriter;
import org.gbif.registry.metadata.EMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

/**
 * Simple writer class to create valid dwc archives using tab data files.
 * The meta.xml descriptor is generated automatically and an optional EML metadata document can be added.
 * The archive is NOT compressed but the final product is a directory with all the necessary files.
 * For usage of this class please @see DwcaWriterTest.
 */
public class DwcaWriter {
  private Logger log = LoggerFactory.getLogger(DwcaWriter.class);
  private final File dir;
  private final boolean useHeaders;
  private long recordNum;
  private String coreId;
  private Map<Term, String> coreRow;
  private final Term coreRowType;
  private final Term coreIdTerm;
  private final Map<Term, TabWriter> writers = Maps.newHashMap();
  private final Set<Term> headersOut = Sets.newHashSet();
  private final Map<Term, String> dataFileNames = Maps.newHashMap();
  // key=rowType, value=columns
  private final Map<Term, List<Term>> terms = Maps.newHashMap();
  // key=rowType, value=default values per column
  private final Map<Term, Map<Term, String>> defaultValues = Maps.newHashMap();
  private final Map<Term, Map<Term, String>> multiValueDelimiter = Maps.newHashMap();
  private Dataset eml;
  private Map<String, Dataset> constituents = Maps.newHashMap();

  /**
   * Creates a new writer without header rows.
   * @param coreRowType the core row type.
   * @param dir         the directory to create the archive in.
   */
  public DwcaWriter(Term coreRowType, File dir) throws IOException {
    this(coreRowType, dir, false);
  }

  /**
   * If headers are used the first record must include all terms ever used for that file.
   * If in subsequent rows additional terms are introduced an IllegalArgumentException is thrown.
   *
   * @param coreRowType    the core row type
   * @param dir            the directory to create the archive in
   * @param useHeaders if true the first row in every data file will include headers
   */
  public DwcaWriter(Term coreRowType, File dir, boolean useHeaders) throws IOException {
    this(coreRowType, null, dir, useHeaders);
  }
  
  /**
   * If headers are used the first record must include all terms ever used for that file.
   * If in subsequent rows additional terms are introduced an IllegalArgumentException is thrown.
   * 
   * @param coreRowType the core row type
   * @param coreIdTerm the term of the id column
   * @param dir the directory to create the archive in
   * @param useHeaders if true the first row in every data file will include headers
   */
  public DwcaWriter(Term coreRowType, Term coreIdTerm, File dir, boolean useHeaders) throws IOException {
    this.dir = dir;
    this.coreRowType = coreRowType;
    this.coreIdTerm = coreIdTerm;
    this.useHeaders = useHeaders;
    addRowType(coreRowType);
  }

  public static Map<Term, String> recordToMap(Record rec, ArchiveFile af) {
    Map<Term, String> map = new HashMap<Term, String>();
    for (Term t : af.getTerms()) {
      map.put(t, rec.value(t));
    }
    return map;
  }

  public static String dataFileName(Term rowType) {
    return rowType.simpleName().toLowerCase() + ".txt";
  }

  private void addRowType(Term rowType) throws IOException {
    terms.put(rowType, new ArrayList<Term>());

    String dfn = dataFileName(rowType);
    dataFileNames.put(rowType, dfn);
    File df = new File(dir, dfn);
    FileUtils.forceMkdir(df.getParentFile());
    OutputStream out = new FileOutputStream(df);
    TabWriter wr = new TabWriter(out);
    writers.put(rowType, wr);
  }

  /**
   * A new core record is started and the last core and all extension records are written.
   * @param id the new records id
   * @throws IOException
   */
  public void newRecord(String id) throws IOException {
    // flush last record
    flushLastCoreRecord();
    // start new
    recordNum++;
    coreId = id;
    coreRow = new HashMap<Term, String>();
  }

  private void flushLastCoreRecord() throws IOException {
    if (coreRow != null) {
      writeRow(coreRow, coreRowType);
    }
  }

  public long getRecordsWritten() {
    return recordNum;
  }

  private void writeRow(Map<Term, String> rowMap, Term rowType) throws IOException {
    TabWriter writer = writers.get(rowType);
    List<Term> columns = terms.get(rowType);
    if (useHeaders && !headersOut.contains(rowType)){
      // write header row
      writeHeader(writer, rowType, columns);
    }

    // make sure coreId is not null for extensions
    if (coreRowType != rowType && coreId == null){
      log.warn("Adding an {} extension record to a core without an Id! Skip this record", rowType);

    } else {
      String[] row = new String[columns.size() + 1];
      row[0] = coreId;
      for (Map.Entry<Term, String> conceptTermStringEntry : rowMap.entrySet()) {
        int column = 1 + columns.indexOf(conceptTermStringEntry.getKey());
        row[column] = conceptTermStringEntry.getValue();
      }
      writer.write(row);
    }
  }

  private void writeHeader(TabWriter writer, Term rowType, List<Term> columns) throws IOException {
    int idx = 0;
    String[] row = new String[columns.size() + 1];
    Term idTerm;
    if (DwcTerm.Taxon == coreRowType){
      idTerm = DwcTerm.taxonID;
    } else if (DwcTerm.Occurrence == coreRowType){
      idTerm = DwcTerm.occurrenceID;
    } else if (DwcTerm.Identification == coreRowType){
      idTerm = DwcTerm.identificationID;
    } else if (DwcTerm.Event == coreRowType){
      idTerm = DwcTerm.eventID;
    } else {
      // default to generic dc identifier for id column
      idTerm = DcTerm.identifier;
    }
    row[idx] = idTerm.simpleName();

    for (Term term : columns) {
      idx ++;
      row[idx] = term.simpleName();
    }
    writer.write(row);

    headersOut.add(rowType);
  }


  /**
   * Add a single value for the current core record.
   * Calling this method requires that #newRecord() has been called at least once,
   * otherwise an IllegalStateException is thrown.
   * @param term
   * @param value
   */
  public void addCoreColumn(Term term, String value) {
    // ensure we do not overwrite the coreIdTerm if one is defined
    if (coreIdTerm != null && coreIdTerm.equals(term)) {
      throw new IllegalStateException("You cannot add a term that was specified as coreId term");
    }
    
    List<Term> coreTerms = terms.get(coreRowType);
    if (!coreTerms.contains(term)) {
      if (useHeaders && recordNum>1){
        throw new IllegalStateException("You cannot add new terms after the first row when headers are enabled");
      }
      coreTerms.add(term);
    }
    try {
      coreRow.put(term, value);
    } catch (NullPointerException e) {
      // no core record has been started yet
      throw new IllegalStateException("No core record has been created yet. Call newRecord() at least once");
    }
  }

  /**
   * Convenience method to add an empty core column.
   */
  public void addCoreColumn(Term term) {
    addCoreColumn(term, (String) null);
  }

  /**
   * Null safe convenience method to write integers.
   * See addCoreColumn(Term term, String value) for docs
   */
  public void addCoreColumn(Term term, @Nullable Integer value) {
    addCoreColumn(term, value == null ? null : value.toString());
  }

  /**
   * Null safe convenience method to write booleans.
   * See addCoreColumn(Term term, String value) for docs
   */
  public void addCoreColumn(Term term, @Nullable Boolean value) {
    addCoreColumn(term, value == null ? null : value.toString());
  }

  /**
   * Null safe convenience method to write enumeration values.
   * See addCoreColumn(Term term, String value) for docs
   */
  public void addCoreColumn(Term term, @Nullable Enum value) {
    addCoreColumn(term, value == null ? null : value.name().toLowerCase().replaceAll("_", " "));
  }

  /**
   * Null safe convenience method to write object values using the toString method.
   * See addCoreColumn(Term term, String value) for docs
   */
  public void addCoreColumn(Term term, @Nullable Object value) {
    addCoreColumn(term, value == null ? null : value.toString());
  }

  /**
   * Add a default value to a term of the core.
   *
   * @param term
   * @param defaultValue
   */
  public void addCoreDefaultValue(Term term, String defaultValue){
    addDefaultValue(coreRowType, term, defaultValue);
  }
  
  /**
   * Add a default value to a term of the provided rowType.
   * 
   * @param rowType
   * @param term
   * @param defaultValue
   */
  public void addDefaultValue(Term rowType, Term term, String defaultValue){
    
    if(!defaultValues.containsKey(rowType)){
      defaultValues.put(rowType, new HashMap<Term, String>());
    }
    Map<Term,String> currentDefaultValues= defaultValues.get(rowType);
    if(currentDefaultValues.containsKey(term)){
      throw new IllegalStateException("The default value of term "+ term + " is already defined");
    }
    currentDefaultValues.put(term, defaultValue);
  }

  /**
   * Declares the multi value delimiter for a term of the core rowType.
   *
   * @param term
   * @param defaultValue
   */
  public void addCoreMultiValueDelimiter(Term term, String defaultValue){
    addMultiValueDelimiter(coreRowType, term, defaultValue);
  }

  /**
   * Declares the multi value delimiter for a term of the provided rowType.
   */
  public void addMultiValueDelimiter(Term rowType, Term term, String delimiter){

    if(!multiValueDelimiter.containsKey(rowType)){
      multiValueDelimiter.put(rowType, new HashMap<Term, String>());
    }
    Map<Term,String> delimiters= multiValueDelimiter.get(rowType);
    if(delimiters.containsKey(term)){
      throw new IllegalStateException("The delimiter of term "+ term + " is already defined");
    }
    delimiters.put(term, delimiter);
  }

  /**
   * @return new map of all current data file names by their rowTypes.
   */
  public Map<Term, String> getDataFiles() {
    return Maps.newHashMap(dataFileNames);
  }

  /**
   * Add an extension record associated with the current core record.
   * 
   * @param rowType
   * @param row
   * @throws IOException
   */
  public void addExtensionRecord(Term rowType, Map<Term, String> row) throws IOException {
    // make sure we know the extension rowtype
    if (!terms.containsKey(rowType)) {
      addRowType(rowType);
    }
    
    // make sure we know all terms
    List<Term> knownTerms = terms.get(rowType);
    final boolean isFirst = knownTerms.isEmpty();
    for (Term term : row.keySet()) {
      if (!knownTerms.contains(term)) {
        if (useHeaders && !isFirst){
          throw new IllegalStateException("You cannot add new terms after the first row when headers are enabled");
        }
        knownTerms.add(term);
      }
    }

    // write extension record
    writeRow(row, rowType);
  }

  public void setEml(Dataset eml) {
    this.eml = eml;
  }

  /**
   * Adds a constituent dataset using the dataset key as the datasetID
   */
  public void addConstituent(Dataset eml) {
    addConstituent(eml.getKey().toString(), eml);
  }

  /**
   * Adds a constituent dataset.
   * The eml file will be called as the datasetID which has to be unique.
   */
  public void addConstituent(String datasetID, Dataset eml) {
    this.constituents.put(datasetID, eml);
  }

  /**
   * @return the set of available rowTypes in this archive
   */
  public Set<Term> getRowTypes() {
    return terms.keySet();
  }

  /**
   * @return the list of term columns as used for the given row type
   */
  public List<Term> getTerms(Term rowType) {
    if (terms.containsKey(rowType)) {
      return terms.get(rowType);
    }
    return Lists.newArrayList();
  }

  /**
   * Writes meta.xml and eml.xml to the archive and closes tab writers.
   *
   */
  public void close() throws IOException {
    addEml();
    addConstituents();
    addMeta();
    // flush last record
    flushLastCoreRecord();
    // TODO: add missing columns in second iteration of data files

    // close writers
    for (TabWriter w : writers.values()) {
      w.close();
    }
  }

  protected static void writeEml(Dataset d, File f) throws IOException {
    if (d != null) {
      try (Writer writer = new FileWriter(f)){
        EMLWriter.newInstance().writeTo(d, writer);
      }
    }
  }

  private void addEml() throws IOException {
    writeEml(eml, new File(dir, "eml.xml"));
  }

  private void addConstituents() throws IOException {
    if (!constituents.isEmpty()) {
      File ddir = new File(dir, Archive.CONSTITUENT_DIR);
      ddir.mkdirs();
      for (Map.Entry<String, Dataset> de : constituents.entrySet()) {
        writeEml(de.getValue(), new File(ddir, de.getKey()+".xml"));
      }
    }
  }

  private void addMeta() throws IOException {
    File metaFile = new File(dir, Archive.META_FN);

    Archive arch = new Archive();
    if (eml != null) {
      arch.setMetadataLocation("eml.xml");
    }
    arch.setCore(buildArchiveFile(arch, coreRowType, coreIdTerm));
    for (Term rowType : this.terms.keySet()) {
      if (!coreRowType.equals(rowType)) {
        arch.addExtension(buildArchiveFile(arch, rowType, null));
      }
    }
    MetaDescriptorWriter.writeMetaFile(metaFile, arch);
  }

  /**
   * Build an ArchiveFile for core or extension(s).
   *
   * @param archive
   * @param rowType
   * @param idTerm the term of the id column, may be null
   * @return
   */
  private ArchiveFile buildArchiveFile(Archive archive, Term rowType, Term idTerm) {
    ArchiveFile af = ArchiveFile.buildTabFile();
    af.setArchive(archive);
    af.addLocation(dataFileNames.get(rowType));

    af.setEncoding("utf-8");
    af.setIgnoreHeaderLines(useHeaders ? 1 : 0);
    af.setRowType(rowType);

    ArchiveField id = new ArchiveField();
    id.setIndex(0);
    af.setId(id);
    // always use the index 0 for idTerm
    if (idTerm != null) {
      af.addField(buildArchiveField(0, idTerm));
    }
    
    Map<Term,String> termDefaultValueMap = defaultValues.get(rowType);
    Map<Term,String> termMultiValueDelimiterMap = multiValueDelimiter.get(rowType);
    List<Term> rowTypeTerms = terms.get(rowType);
    int idx = 0;
    String defaultValue;
    String mvDelim;
    for (Term c : rowTypeTerms) {
      idx++;
      defaultValue = (termDefaultValueMap !=null ? termDefaultValueMap.get(c) : null);
      mvDelim = (termMultiValueDelimiterMap !=null ? termMultiValueDelimiterMap.get(c) : null);
      af.addField(buildArchiveField(idx, c, defaultValue, mvDelim));
    }
    
    // check if default values are provided for this rowType
    if(termDefaultValueMap != null){
      for (Term t : termDefaultValueMap.keySet()) {
        if(!rowTypeTerms.contains(t)){
          af.addField(buildArchiveFieldDefaultValue(t, termDefaultValueMap.get(t)));
        }
      }
    }

    return af;
  }

  /**
   * Build an ArchiveField with a defaultValue and no index.
   *
   * @param term
   * @param defaultValue
   * @return
   */
  private ArchiveField buildArchiveFieldDefaultValue(Term term, String defaultValue){
    Preconditions.checkNotNull(term, "Can't use a null term");
    Preconditions.checkNotNull(defaultValue, "Can't use a null defaultValue");

    return new ArchiveField(term, defaultValue);
  }

  /**
   * Build an ArchiveField with no defaultValue.
   *
   * @param idx
   * @param term
   * @return
   */
  private ArchiveField buildArchiveField(Integer idx, Term term){
    return buildArchiveField(idx, term, null);
  }

  /**
   *
   * Build an ArchiveField from optional parameters.
   *
   * @param idx index or null
   * @param term term or null
   * @param defaultValue default value or null
   * @return
   */
  private ArchiveField buildArchiveField(Integer idx, Term term, String defaultValue){
    return buildArchiveField(idx, term, defaultValue, null);
  }

  /**
   *
   * Build an ArchiveField from optional parameters.
   *
   * @param idx index or null
   * @param term term or null
   * @param defaultValue default value or null
   * @param multiValueDelimiter value delimiter or null
   */
  private ArchiveField buildArchiveField(Integer idx, Term term, String defaultValue, String multiValueDelimiter){
    Preconditions.checkNotNull(idx, "Can't use a null index");
    Preconditions.checkNotNull(term, "Can't use a null term");

    ArchiveField field = new ArchiveField(idx, term);

    if (StringUtils.isNotBlank(defaultValue)){
      field.setDefaultValue(defaultValue);
    }
    if (StringUtils.isNotEmpty(multiValueDelimiter)){
      field.setDelimitedBy(multiValueDelimiter);
    }
    return field;
  }
}
