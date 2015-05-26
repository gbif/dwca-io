package org.gbif.dwca.io;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.io.MetaDescriptorWriter;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import com.google.common.collect.Sets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MetaDescriptorWriterTest {

  @Test
  public void testRoundtrip() {
    try {
      // read archive
      Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("archive-dwc"));
      assertNotNull(arch);
      assertNotNull(arch.getCore());
      assertTrue(arch.getCore().getId().getIndex() == 0);
      assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
      assertEquals(2, arch.getExtensions().size());
      assertEquals("\t", arch.getCore().getFieldsTerminatedBy());
      assertNull(arch.getCore().getField(DwcTerm.scientificName).getDelimitedBy());
      assertEquals(";", arch.getCore().getField(DwcTerm.nomenclaturalStatus).getDelimitedBy());

      // write meta.xml
      File tmpDwca = createTmpMeta(arch);

      Archive arch2 = ArchiveFactory.openArchive(tmpDwca);
      // core props
      ArchiveFile core = arch2.getCore();
      assertNotNull(core);
      assertNotNull(core.getId());
      assertTrue(core.hasTerm(DwcTerm.scientificName));
      assertEquals("DarwinCore.txt", core.getLocation());
      assertEquals("\t", core.getFieldsTerminatedBy());
      assertNull(arch.getCore().getField(DwcTerm.scientificName).getDelimitedBy());
      assertEquals(";", arch.getCore().getField(DwcTerm.nomenclaturalStatus).getDelimitedBy());
      for (ArchiveField f : arch.getCore().getFields().values()) {
        assertTrue(core.hasTerm(f.getTerm().qualifiedName()));
        assertEquals(core.getField(f.getTerm().qualifiedName()).getIndex(), f.getIndex());
      }

      // extensions props
      assertEquals(2, arch2.getExtensions().size());
      Set<String> filenames = Sets.newHashSet("VernacularName.txt", "media.txt");
      for (ArchiveFile ext : arch2.getExtensions()) {
        assertTrue(filenames.contains(ext.getLocation()));
        filenames.remove(ext.getLocation());
      }
      assertTrue(filenames.isEmpty());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  private File createTmpMeta(Archive arch) throws IOException {
    File tmpDir = Files.createTempDirectory("dwca-io-test").toFile();
    tmpDir.deleteOnExit();
    File tmpMeta = new File(tmpDir, "meta.xml");
    System.out.println("Writing temporary test meta file to " + tmpMeta.getAbsolutePath());
    MetaDescriptorWriter.writeMetaFile(tmpMeta, arch);
    return tmpDir;
  }

  @Test
  public void testRoundtripQuotes() {
    try {
      // read archive
      Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("xml-entity-meta"));
      assertNotNull(arch);
      assertNotNull(arch.getCore());
      assertNotNull(arch.getCore().getId());
      assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
      assertEquals(1, arch.getExtensions().size());

      // write meta.xml
      File tmpDwca = createTmpMeta(arch);
      Archive arch2 = ArchiveFactory.openArchive(tmpDwca);

      // core props
      ArchiveFile core = arch2.getCore();
      assertNotNull(core);
      assertNotNull(core.getId());
      assertTrue(core.hasTerm(DwcTerm.scientificName));
      assertEquals("test", core.getLocation());
      for (ArchiveField f : arch.getCore().getFields().values()) {
        assertTrue(core.hasTerm(f.getTerm().qualifiedName()));
        assertEquals(core.getField(f.getTerm().qualifiedName()).getIndex(), f.getIndex());
      }

      // extensions props
      assertEquals(1, arch2.getExtensions().size());
      ArchiveFile ext = arch2.getExtensions().iterator().next();
      assertEquals("test2", ext.getLocation());
      assertEquals(2, ext.getFields().size());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
