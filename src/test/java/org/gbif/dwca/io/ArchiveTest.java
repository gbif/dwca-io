package org.gbif.dwca.io;

import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.record.StarRecord;
import org.gbif.utils.file.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ArchiveTest {

  @Test
  public void testIteratorDwc() throws Exception {
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("simplest-archive").toPath());

    for (StarRecord rec : arch) {
      assertEquals("Quercus alba", rec.core().value(DwcTerm.scientificName));
      assertEquals("Species", rec.core().value(DwcTerm.taxonRank));
    }
  }

  @Test
  public void testConstituents() throws Exception {
    File dir = FileUtils.getClasspathFile("constituentsdwca");

    Archive arch = new Archive();
    arch.setLocation(dir);
    arch.setMetadataLocation("eml.xml");
    ArchiveField id = new ArchiveField(0, null, null, null);
    ArchiveField datasetId = new ArchiveField(1, DwcTerm.datasetID, null, null);
    ArchiveField sciname = new ArchiveField(2, DwcTerm.scientificName, null, null);

    Map<Term, ArchiveField> fields = new HashMap<Term, ArchiveField>();
    fields.put(DwcTerm.taxonomicStatus, sciname);
    fields.put(DwcTerm.datasetID, datasetId);

    Map<String, File> cons = arch.getConstituentMetadata();
    assertEquals(6, cons.size());
    for (Map.Entry<String, File> c : cons.entrySet()) {
      final String name = c.getKey();
      final File file = c.getValue();
      assertEquals(name, file.getName().split("\\.")[0]);
    }
  }
}
