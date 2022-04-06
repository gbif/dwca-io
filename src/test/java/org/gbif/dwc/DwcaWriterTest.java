/*
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
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class DwcaWriterTest {

  private static final Logger LOG = LoggerFactory.getLogger(DwcaWriterTest.class);
  
  @Test
  public void testAddingCoreIdTermTwice() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, DwcTerm.taxonID, dwcaDir, true);
    writer.newRecord("dummy1");
    assertThrows(IllegalStateException.class, () -> writer.addCoreColumn(DwcTerm.taxonID, "dummy1"));
  }
  
  @Test
  public void testHeaders1() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID);
    writer.newRecord("dummy2");
    writer.addCoreColumn(DwcTerm.parentNameUsageID);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID);
  }

  @Test
  public void testHeaders2() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID);
    writer.newRecord("dummy2");
    assertThrows(IllegalStateException.class, () -> writer.addCoreColumn(DwcTerm.scientificName));
  }

  @Test
  public void testHeaders3() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID);

    // define extension columns
    Map<Term, String> eData = new HashMap<>();
    eData.put(DwcTerm.locality, "locality1");
    eData.put(DwcTerm.occurrenceStatus, "present");
    writer.addExtensionRecord(GbifTerm.Distribution, eData);

    eData.put(DwcTerm.establishmentMeans, "alien");
    assertThrows(IllegalStateException.class, () -> writer.addExtensionRecord(GbifTerm.Distribution, eData));
  }


  @Test
  public void testHeaderWriting() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    LOG.info("Test archive writer in {}", dwcaDir.getAbsolutePath());

    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID);
    writer.addCoreColumn(DwcTerm.scientificName);
    writer.addCoreColumn(GbifTerm.canonicalName);
    writer.addCoreColumn(DwcTerm.taxonRank, "species");
    writer.addCoreColumn(DwcTerm.taxonomicStatus);
    writer.addCoreColumn(DwcTerm.kingdom);
    writer.addCoreColumn(DwcTerm.phylum);
    writer.addCoreColumn(DwcTerm.class_);
    writer.addCoreColumn(DwcTerm.order);
    writer.addCoreColumn(DwcTerm.family);
    writer.addCoreColumn(GbifTerm.depth);
    writer.addCoreColumn(GbifTerm.depthAccuracy);

    writer.newRecord("dummy2");
    writer.addCoreColumn(DwcTerm.kingdom, "Plantae");
    writer.addCoreColumn(DwcTerm.phylum);
    writer.addCoreColumn(DwcTerm.class_);
    writer.addCoreColumn(DwcTerm.order);
    writer.addCoreColumn(DwcTerm.family, "Asteraceae");

    writer.newRecord("dummy3");
    writer.addCoreColumn(GbifTerm.depth, "2");
    writer.addCoreColumn(GbifTerm.depthAccuracy, "1");

    // define extension columns
    Map<Term, String> eData = new HashMap<>();
    // distributions
    eData.put(DwcTerm.locality, "locality1");
    eData.put(DwcTerm.occurrenceStatus, "present");
    eData.put(DwcTerm.establishmentMeans, "alien");
    writer.addExtensionRecord(GbifTerm.Distribution, eData);

    eData.put(DwcTerm.locality, "locality2");
    writer.addExtensionRecord(GbifTerm.Distribution, eData);

    writer.close();

    File cf = new File(dwcaDir, writer.getDataFiles().get(DwcTerm.Taxon));
    File df = new File(dwcaDir, writer.getDataFiles().get(GbifTerm.Distribution));

    // check if taxon file contains headers
    String[] headers = getFirstRow(cf);
    LOG.debug(String.join("; ", headers));
    assertEquals(14, headers.length);
    assertEquals("taxonID", headers[0]);
    assertEquals("parentNameUsageID", headers[1]);
    assertEquals("kingdom", headers[7]);

    // check if extension file contains headers
    headers = getFirstRow(df);
    LOG.debug(String.join("; ", headers));
    assertEquals(4, headers.length);
    assertEquals("taxonID", headers[0]);
  }

  private String[] getFirstRow(File f) throws IOException {
    BufferedReader r = FileUtils.getUtf8Reader(f);
    String firstRow = r.readLine();
    return firstRow.split("\t");
  }

  @Test
  public void testRoundtrip() {
    try {
      // read taxon archive
      Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("archive-dwc").toPath());
      assertEquals(2, arch.getExtensions().size());
      int coreRecords = 0;
      int allRecords = 0;

      // write taxon archive
      File tempArch = FileUtils.createTempDir();
      tempArch.deleteOnExit();
      System.out.println("Writing temporary test archive to " + tempArch.getAbsolutePath());
      DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, tempArch);
      for (StarRecord rec : arch) {
        // core
        coreRecords++;
        allRecords += rec.size();

        writer.newRecord(rec.core().id());
        for (Term term : arch.getCore().getTerms()) {
          writer.addCoreColumn(term, rec.core().value(term));
        }
        // extensions
        for (Term rt : rec.extensions().keySet()) {
          ArchiveFile af = arch.getExtension(rt);
          // iterate over records for one extension
          for (Record row : rec.extension(rt)) {
            writer.addExtensionRecord(rt, DwcaWriter.recordToMap(row, af));
          }
        }
      }
      writer.close();

      // reread and compare
      Archive arch2 = DwcFiles.fromLocation(tempArch.toPath());

      int coreRecords2 = 0;
      int allRecords2 = 0;
      for (StarRecord rec : arch2) {
        // core
        coreRecords2++;
        allRecords2 += rec.size();
      }

      // compare
      assertEquals(coreRecords, coreRecords2);
      assertEquals(allRecords, allRecords2);


    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
  
  @Test
  public void testWriterUsingCoreIdTerm() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    LOG.info("Test archive writer in {}", dwcaDir.getAbsolutePath());

    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, DwcTerm.taxonID, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID);
    writer.close();

    Archive arch = DwcFiles.fromLocation(dwcaDir.toPath());
    Iterator<Record> recIt = arch.getCore().iterator();
    Record firstRecord = recIt.next();
    assertEquals("dummy1", firstRecord.id());
    assertEquals("dummy1", firstRecord.value(DwcTerm.taxonID));
  }
  
  /**
   * Test the writing of an archive that includes some default values in the core and in one extension.
   */
  @Test
  public void testWriterUsingDefaultValues() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    LOG.info("Test archive writer in {}", dwcaDir.getAbsolutePath());

    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, DwcTerm.taxonID, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID, "1");
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID, "2");
    writer.addCoreColumn(DwcTerm.countryCode);
    
    // add a VernacularName extension record
    Map<Term,String> extensionRecord = new HashMap<>();
    extensionRecord.put(DwcTerm.vernacularName, "Komodo Dragon");
    extensionRecord.put(DcTerm.language, null);
    writer.addExtensionRecord(GbifTerm.VernacularName, extensionRecord);
    
    writer.addCoreDefaultValue(DwcTerm.collectionCode, "A2Z");
    writer.addCoreDefaultValue(DwcTerm.countryCode, "CA");
    writer.addDefaultValue(GbifTerm.VernacularName, DcTerm.language, "en");
    
    // add a second records and overwrite the default value
    writer.newRecord("dummy2");
    writer.addCoreColumn(DwcTerm.parentNameUsageID, "1");
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID, "2");
    writer.addCoreColumn(DwcTerm.countryCode, "ID");
    
    // add a VernacularName extension record
    extensionRecord = new HashMap<>();
    extensionRecord.put(DwcTerm.vernacularName, "Varano De Komodo");
    extensionRecord.put(DcTerm.language, "es");
    writer.addExtensionRecord(GbifTerm.VernacularName, extensionRecord);
    
    writer.close();

    // validate core content
    Archive arch = DwcFiles.fromLocation(dwcaDir.toPath());
    Iterator<Record> recIt = arch.getCore().iterator();
    Record firstRecord = recIt.next();
    assertEquals("dummy1", firstRecord.id());
    assertEquals("dummy1", firstRecord.value(DwcTerm.taxonID));
    assertEquals("A2Z", firstRecord.value(DwcTerm.collectionCode));
    assertEquals("CA", firstRecord.value(DwcTerm.countryCode));
    assertEquals("A2Z", arch.getCore().getField(DwcTerm.collectionCode).getDefaultValue());
    assertEquals("CA", arch.getCore().getField(DwcTerm.countryCode).getDefaultValue());
    
    Record secondRecord = recIt.next();
    assertEquals("dummy2", secondRecord.id());
    assertEquals("dummy2", secondRecord.value(DwcTerm.taxonID));
    assertEquals("A2Z", secondRecord.value(DwcTerm.collectionCode));
    assertEquals("ID", secondRecord.value(DwcTerm.countryCode));
    
    // validate extension content
    Iterator<Record> extRecIt = arch.getExtension(GbifTerm.VernacularName).iterator();
    assertEquals("en", arch.getExtension(GbifTerm.VernacularName).getField(DcTerm.language).getDefaultValue());
    firstRecord = extRecIt.next();
    assertEquals("dummy1", firstRecord.id());
    assertEquals("en", firstRecord.value(DcTerm.language));
    
    secondRecord = extRecIt.next();
    assertEquals("dummy2", secondRecord.id());
    assertEquals("es", secondRecord.value(DcTerm.language));
  }

  @Test
  public void testWriteMetadata() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    LOG.info("Test archive writer in {}", dwcaDir.getAbsolutePath());

    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, DwcTerm.taxonID, dwcaDir, true);

    writer.setMetadata("<eml/>", "eml.xml");
    writer.close();

    Archive arch = DwcFiles.fromLocation(dwcaDir.toPath());
    assertEquals("eml.xml", arch.getMetadataLocation());
    assertEquals("<eml/>", arch.getMetadata());
  }
}
