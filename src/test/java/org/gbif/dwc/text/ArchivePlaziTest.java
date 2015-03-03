package org.gbif.dwc.text;

import org.gbif.dwc.record.StarRecord;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ArchivePlaziTest {

  private void assertNumberStarRecords(Archive arch, int expectedRecords) throws IOException {
    int rows = 0;
    for (StarRecord rec : arch) {
      assertNotNull(rec);
      rows++;
    }
    assertEquals(expectedRecords, rows);
  }

  @Test
  public void testBuildReaderFile() throws IOException {
    File zip = FileUtils.getClasspathFile("plazi/6632D8151686A3F8C71D4B5A5B1181A4.zip");
    File tmpDir = FileUtils.createTempDir();
    tmpDir.deleteOnExit();

    Archive arch = ArchiveFactory.openArchive(zip, tmpDir);
    assertNumberStarRecords(arch, 10);
  }
}
