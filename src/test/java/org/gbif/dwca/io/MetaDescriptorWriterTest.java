package org.gbif.dwca.io;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwca.tools.MetaValidator;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MetaDescriptorWriterTest {

  public class SAXExtractTerms extends DefaultHandler2 {
    private final List<String> terms;
    public SAXExtractTerms(List<String> terms) {
      this.terms = terms;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      for (String attName : Lists.newArrayList("rowType", "term")) {
        if (atts.getValue(attName) != null) {
          terms.add(atts.getValue(attName));
        }
      }
    }
  }

  @Test
  public void testXml() throws Exception {
    // read archive
    Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("archive-dwc"));

    // write meta.xml
    File tmpMeta = File.createTempFile("meta", ".xml");
    System.out.println("Writing temporary test meta file to " + tmpMeta.getAbsolutePath());
    MetaDescriptorWriter.writeMetaFile(tmpMeta, arch);

    // validate xml
    System.out.println("Validate xml");
    MetaValidator.validate(new FileInputStream(tmpMeta));

    // verify rowType & terms are URIs
    List<String> terms = Lists.newArrayList();
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    SAXParser saxParser = spf.newSAXParser();
    XMLReader xmlReader = saxParser.getXMLReader();
    xmlReader.setContentHandler(new SAXExtractTerms(terms));
    xmlReader.parse(new InputSource(new FileInputStream(tmpMeta)));

    assertEquals(18, terms.size());
    for (String term : terms) {
      URI uri = URI.create(term);
      assertNotNull(uri + " is no full URI term", uri.getScheme());
      assertNotNull(uri + " is no full URI term", uri.getAuthority());
      assertNotNull(uri + " is no full URI term", uri.getPath());
    }
  }

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
