package org.gbif.dwca.io;

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.record.DarwinCoreRecord;
import org.gbif.io.CSVReader;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.mockito.Matchers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArchiveTest {

  @Test
  public void testIteratorDwc() throws Exception {
    File csv = FileUtils.getClasspathFile("null_data.txt");
    CSVReader reader = new CSVReader(csv, "utf8", "\t", null, 1);

    Archive arch = new Archive();
    ArchiveField id = new ArchiveField(0, null, null, null);
    ArchiveField taxStatus = new ArchiveField(1, DwcTerm.taxonomicStatus, "a", null);
    ArchiveField dcmodified = new ArchiveField(6, DcTerm.modified, "mod", null);

    ArchiveFile core = mock(ArchiveFile.class);
    when(core.getCSVReader()).thenReturn(reader);
    when(core.getId()).thenReturn(id);
    when(core.getArchive()).thenReturn(arch);
    when(core.getField(Matchers.<Term>any())).thenReturn(taxStatus);
    Map<Term, ArchiveField> fields = new HashMap<Term, ArchiveField>();
    fields.put(DwcTerm.taxonomicStatus, taxStatus);
    fields.put(DcTerm.modified, dcmodified);
    when(core.getFields()).thenReturn(fields);
    when(core.hasTerm(eq(DwcTerm.taxonomicStatus))).thenReturn(true);
    when(core.hasTerm(eq(DcTerm.modified))).thenReturn(true);
    arch.setCore(core);

    Iterator<DarwinCoreRecord> iter = arch.iteratorDwc();
    while (iter.hasNext()) {
      DarwinCoreRecord rec = iter.next();
      assertEquals("a", rec.getTaxonomicStatus());
      assertEquals("mod", rec.getModified());
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
