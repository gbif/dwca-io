package org.gbif.dwc.text;
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

import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.file.TabWriter;
import org.gbif.metadata.eml.Eml;
import org.gbif.metadata.eml.EmlWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple writer class to create valid dwc archives using tab data files.
 * The meta.xml descriptor is generated automatically and an optinal EML metadata document can be added.
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
  private final Map<Term, TabWriter> writers = Maps.newHashMap();
  private final Set<Term> headersOut = Sets.newHashSet();
  private final Map<Term, String> dataFileNames = Maps.newHashMap();
  // key=rowType, value=columns
  private final Map<Term, List<Term>> terms = Maps.newHashMap();
  private Eml eml;


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
    this.dir = dir;
    this.coreRowType = coreRowType;
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
   * @return new map of all current data file names by their rowTypes.
   */
  public Map<Term, String> getDataFiles() {
    return Maps.newHashMap(dataFileNames);
  }

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

  public void setEml(Eml eml) {
    this.eml = eml;
  }

  /**
   * Writes meta.xml and eml.xml to the archive and closes tab writers.
   *
   * @deprecated Use {@link #close()} instead. This method will be removed in version 1.12.
   */
  @Deprecated
  public void finalize() throws IOException {
    close();
  }

  /**
   * Writes meta.xml and eml.xml to the archive and closes tab writers.
   */
  public void close() throws IOException {
    addEml();
    addMeta();
    // flush last record
    flushLastCoreRecord();
    // TODO: add missing columns in second iteration of data files

    // close writers
    for (TabWriter w : writers.values()) {
      w.close();
    }
  }

  private void addEml() throws IOException {
    if (eml != null) {
      File emlFile = new File(dir, "eml.xml");
      try {
        EmlWriter.writeEmlFile(emlFile, eml);
      } catch (TemplateException e) {
        throw new IOException("EML template exception: " + e.getMessage());
      }
    }
  }

  private void addMeta() throws IOException {
    File metaFile = new File(dir, "meta.xml");

    Archive arch = new Archive();
    if (eml != null) {
      arch.setMetadataLocation("eml.xml");
    }
    arch.setCore(buildArchiveFile(arch, coreRowType));
    for (Term rowType : this.terms.keySet()) {
      if (!coreRowType.equals(rowType)) {
        arch.addExtension(buildArchiveFile(arch, rowType));
      }
    }
    try {
      MetaDescriptorWriter.writeMetaFile(metaFile, arch);
    } catch (TemplateException e) {
      throw new IOException("Meta.xml template exception: " + e.getMessage());
    }
  }

  private ArchiveFile buildArchiveFile(Archive archive, Term rowType) {
    ArchiveFile af = ArchiveFile.buildTabFile();
    af.setArchive(archive);
    af.addLocation(dataFileNames.get(rowType));

    af.setEncoding("utf-8");
    af.setIgnoreHeaderLines(useHeaders ? 1 : 0);
    af.setRowType(rowType.qualifiedName());

    ArchiveField id = new ArchiveField();
    id.setIndex(0);
    af.setId(id);

    int idx = 0;
    for (Term c : terms.get(rowType)) {
      idx++;
      ArchiveField field = new ArchiveField();
      field.setIndex(idx);
      field.setTerm(c);
      af.addField(field);
    }

    return af;
  }
}
