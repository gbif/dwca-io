package org.gbif.dwc.text;

import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.utils.file.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DwcaWriterTest {
  private static Logger LOG = LoggerFactory.getLogger(DwcaWriterTest.class);
  
  @Test(expected = IllegalStateException.class)
  public void testAddingCoreIdTermTwice() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, DwcTerm.taxonID, dwcaDir, true);
    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.taxonID, "dummy1");
  }
  
  /**
   * Add a core column before adding a default value for this column.
   * @throws Exception
   */
  @Test(expected = IllegalStateException.class)
  public void testDefaultValueDefinedTwice() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);
    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.taxonID, "dummy1");
    writer.addDefaultValue(DwcTerm.Taxon, DwcTerm.taxonID, "dummy");
  }
  
  /**
   * Add a core column after added a default value for this column.
   * @throws Exception
   */
  @Test(expected = IllegalStateException.class)
  public void testDefaultValueDefinedTwice2() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);
    writer.addDefaultValue(DwcTerm.Taxon, DwcTerm.taxonID, "dummy");
    
    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.taxonID, "dummy1");
  }
  
  /**
   * Add extension value before adding a default value for the same column.
   * @throws Exception
   */
  @Test(expected = IllegalStateException.class)
  public void testDefaultValueDefinedTwiceExtension() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);
    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.taxonID, "dummy1");
    
    // add a extension record
    Map<Term, String> values = new HashMap<Term, String>();
    values.put(DwcTerm.resourceID, "11");
    writer.addExtensionRecord(DwcTerm.ResourceRelationship, values);
    
    // try to set a default value on that same term
    writer.addDefaultValue(DwcTerm.ResourceRelationship, DwcTerm.resourceID, "dummy");
  }
  
  /**
   * Add extension value after added a default value for the same column.
   * @throws Exception
   */
  @Test(expected = IllegalStateException.class)
  public void testDefaultValueDefinedTwiceExtension2() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);
    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.taxonID, "dummy1");
    
    writer.addDefaultValue(DwcTerm.ResourceRelationship, DwcTerm.resourceID, "dummy");
    
    // try to add a record with that same term
    Map<Term, String> values = new HashMap<Term, String>();
    values.put(DwcTerm.resourceID, "11");
    writer.addExtensionRecord(DwcTerm.ResourceRelationship, values);
  }
  
  @Test
  public void testHeaders1() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID, null);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID, null);
    writer.newRecord("dummy2");
    writer.addCoreColumn(DwcTerm.parentNameUsageID, null);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID, null);
  }

  @Test(expected = IllegalStateException.class)
  public void testHeaders2() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID, null);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID, null);
    writer.newRecord("dummy2");
    writer.addCoreColumn(DwcTerm.scientificName, null);
  }

  @Test(expected = IllegalStateException.class)
  public void testHeaders3() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID, null);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID, null);

    // define extension columns
    Map<Term, String> eData = Maps.newHashMap();
    eData.put(DwcTerm.locality, "locality1");
    eData.put(DwcTerm.occurrenceStatus, "present");
    writer.addExtensionRecord(GbifTerm.Distribution, eData);

    eData.put(DwcTerm.establishmentMeans, "alien");
    writer.addExtensionRecord(GbifTerm.Distribution, eData);
  }


  @Test
  public void testHeaderWriting() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    LOG.info("Test archive writer in {}", dwcaDir.getAbsolutePath());

    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID, null);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID, null);
    writer.addCoreColumn(DwcTerm.scientificName, null);
    writer.addCoreColumn(GbifTerm.canonicalName, null);
    writer.addCoreColumn(DwcTerm.taxonRank, "species");
    writer.addCoreColumn(DwcTerm.taxonomicStatus, null);
    writer.addCoreColumn(DwcTerm.kingdom, null);
    writer.addCoreColumn(DwcTerm.phylum, null);
    writer.addCoreColumn(DwcTerm.class_, null);
    writer.addCoreColumn(DwcTerm.order, null);
    writer.addCoreColumn(DwcTerm.family, null);
    writer.addCoreColumn(GbifTerm.depth, null);
    writer.addCoreColumn(GbifTerm.depthAccuracy, null);

    writer.newRecord("dummy2");
    writer.addCoreColumn(DwcTerm.kingdom, "Plantae");
    writer.addCoreColumn(DwcTerm.phylum, null);
    writer.addCoreColumn(DwcTerm.class_, null);
    writer.addCoreColumn(DwcTerm.order, null);
    writer.addCoreColumn(DwcTerm.family, "Asteraceae");

    writer.newRecord("dummy3");
    writer.addCoreColumn(GbifTerm.depth, "2");
    writer.addCoreColumn(GbifTerm.depthAccuracy, "1");

    // define extension columns
    Map<Term, String> eData = Maps.newHashMap();
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
    LOG.debug(Joiner.on("; ").useForNull("NULL").join(headers));
    assertEquals(14, headers.length);
    assertEquals("taxonID", headers[0]);
    assertEquals("parentNameUsageID", headers[1]);
    assertEquals("kingdom", headers[7]);

    // check if extension file contains headers
    headers = getFirstRow(df);
    LOG.debug(Joiner.on("; ").useForNull("NULL").join(headers));
    assertEquals(4, headers.length);
    assertEquals("taxonID", headers[0]);
  }

  private String[] getFirstRow(File f) throws IOException {
    BufferedReader r = FileUtils.getUtf8Reader(f);
    String firstRow = r.readLine();
    return firstRow.split("\t");
  }

  @Test
  public void testRoundtrip() throws Exception {
    try {
      TermFactory termFactory = TermFactory.instance();

      // read taxon archive
      Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("archive-dwc"));
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
        for (String rt : rec.extensions().keySet()) {
          Term rowType = termFactory.findTerm(rt);
          ArchiveFile af = arch.getExtension(rowType);
          // iterate over records for one extension
          for (Record row : rec.extension(rt)) {
            writer.addExtensionRecord(rowType, DwcaWriter.recordToMap(row, af));
          }
        }
      }
      writer.close();

      // reread and compare
      Archive arch2 = ArchiveFactory.openArchive(tempArch);
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
    writer.addCoreColumn(DwcTerm.parentNameUsageID, null);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID, null);
    writer.close();

    Archive arch = ArchiveFactory.openArchive(dwcaDir);
    Iterator<Record> recIt = arch.getCore().iterator();
    Record firstRecord = recIt.next();
    assertEquals("dummy1", firstRecord.id());
    assertEquals("dummy1", firstRecord.value(DwcTerm.taxonID));
  }
  
  @Test
  public void testWriterUsingDefaultTermValues() throws Exception {
    File dwcaDir = FileUtils.createTempDir();
    dwcaDir.deleteOnExit();
    LOG.info("Test archive writer in {}", dwcaDir.getAbsolutePath());

    DwcaWriter writer = new DwcaWriter(DwcTerm.Taxon, DwcTerm.taxonID, dwcaDir, true);

    writer.newRecord("dummy1");
    writer.addCoreColumn(DwcTerm.parentNameUsageID, null);
    writer.addCoreColumn(DwcTerm.acceptedNameUsageID, null);
    
    writer.addCoreDefaultValue(DwcTerm.collectionCode, "A2Z");
    writer.close();

    Archive arch = ArchiveFactory.openArchive(dwcaDir);
    Iterator<Record> recIt = arch.getCore().iterator();
    Record firstRecord = recIt.next();
    assertEquals("dummy1", firstRecord.id());
    assertEquals("dummy1", firstRecord.value(DwcTerm.taxonID));
    assertEquals("A2Z", firstRecord.value(DwcTerm.collectionCode));
    assertEquals("A2Z", arch.getCore().getField(DwcTerm.collectionCode).getDefaultValue());
  }
}
