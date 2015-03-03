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
package org.gbif.dwc.tools;

import org.gbif.dwc.record.DarwinCoreRecord;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.ArchiveField;
import org.gbif.dwc.text.ArchiveFile;
import org.gbif.file.CSVReader;
import org.gbif.utils.file.ClosableIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Tool that takes a dwca file and renames its core ids according to some id generator.
 * All extension records and ID terms pointing to the core ID are renamed accordingly to preserve the archive
 * integrity.
 */
public class CoreIdRenamer {

  private final IdGenerator idGen;

  public CoreIdRenamer() {
    idGen = new IntSequenceGenerator(1);
  }

  public CoreIdRenamer(IdGenerator idGen) {
    this.idGen = idGen;
  }

  public CoreIdRenamer(int firstId) {
    idGen = new IntSequenceGenerator(firstId);
  }

  public void rename(File dwca) throws IOException {
    rename(ArchiveFactory.openArchive(dwca));
  }

  public void rename(Archive arch) throws IOException {
    ArchiveField id = arch.getCore().getId();
    TermFactory fact = TermFactory.instance();
    Term rowType = arch.getCore().getRowType();

    // detect dependent terms with ids to be renamed accordingly
    List<Term> relatedTerms = new ArrayList<Term>();
    if (DwcTerm.Taxon == rowType) {
      // potentially 3 foreign keys with values pointing to the coreID
      if (arch.getCore().hasTerm(DwcTerm.parentNameUsageID)) {
        relatedTerms.add(DwcTerm.parentNameUsageID);
      }
      if (arch.getCore().hasTerm(DwcTerm.acceptedNameUsageID)) {
        relatedTerms.add(DwcTerm.acceptedNameUsageID);
      }
      if (arch.getCore().hasTerm(DwcTerm.originalNameUsageID)) {
        relatedTerms.add(DwcTerm.originalNameUsageID);
      }
    } else if (DwcTerm.Occurrence == rowType) {
      // none as far as we know
    } else {
      throw new IllegalStateException("Unknown core rowtype - cannot rename the archive ids");
    }

    // map old to new ids and store them in memory
    Map<String, String> old2new = new HashMap<String, String>();
    ClosableIterator<DarwinCoreRecord> iter = arch.iteratorDwc();
    System.out.println("Mapping record ids ...");
    while (iter.hasNext()) {
      DarwinCoreRecord rec = iter.next();
      old2new.put(rec.id(), idGen.nextId());
    }
    System.out.println("Mapped " + old2new.size() + " record ids. Rewrite files ...");

    // rename all files
    rewriteFile(arch.getCore(), old2new, relatedTerms);
    for (ArchiveFile af : arch.getExtensions()) {
      rewriteFile(arch.getCore(), old2new, relatedTerms);
    }
  }

  private void rewriteFile(ArchiveFile original, Map<String, String> old2new, List<Term> relatedTerms)
    throws IOException {
    File newFile = new File(original.getLocationFile().getParent(), original.getLocationFile().getName() + "-NEW");
    Writer writer =
      new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile, false), original.getEncoding()));

    List<Integer> renameCols = new ArrayList<Integer>();
    renameCols.add(original.getId().getIndex());
    for (Term t : relatedTerms) {
      if (original.hasTerm(t)) {
        renameCols.add(original.getField(t).getIndex());
      }
    }
    // iterate thru rows
    CSVReader reader = original.getCSVReader();
    final String quote = original.getFieldsEnclosedBy() == null ? "" : original.getFieldsEnclosedBy().toString();
    final String quoteDelimQuote = quote + original.getFieldsTerminatedBy() + quote;

    // headers
    for (int hr = reader.headerRows; hr > 0; hr--) {
      // write row with given delimter, quotation and line ending
      String rowAsString = quote + StringUtils.join(reader.header, quoteDelimQuote) + quote;
      writer.write(rowAsString);
      writer.write('\n');
    }
    // records
    for (String[] row : reader) {
      for (Integer col : renameCols) {
        row[col] = old2new.get(row[col]);
      }
      // write row with given delimter, quotation and line ending
      String rowAsString = quote + StringUtils.join(row, quoteDelimQuote) + quote;
      writer.write(rowAsString);
      writer.write('\n');
    }
    writer.flush();
    writer.close();
  }

  public static void main(String[] args) throws IOException {
    CoreIdRenamer renamer = new CoreIdRenamer();
    Archive arch = ArchiveFactory.openArchive(new File("/Users/markus/Desktop/ecat_checklist"));
    arch.getCore().setRowType(DwcTerm.Taxon);
    renamer.rename(arch);
  }
}
