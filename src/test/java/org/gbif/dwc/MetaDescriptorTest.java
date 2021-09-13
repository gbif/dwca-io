/*
 * Copyright 2021 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.dwc;

import org.gbif.dwc.meta.DwcMetaFiles;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests related to the MetaDescriptor operations.
 */
public class MetaDescriptorTest {
  public static TermFactory TERM_FACTORY = TermFactory.instance();

  private static final String NOMENCLATURAL_CODE_VOCABULARY = "http://rs.gbif.org/vocabulary/gbif/nomenclatural_code.xml";
  //for testing only, language vocabulary doesn't exist at rs.gbif.org
  private static final String LANGUAGE_VOCABULARY = "http://rs.gbif.org/vocabulary/gbif/language.xml";

  public class SAXExtractTerms extends DefaultHandler2 {
    private final List<String> terms;
    public SAXExtractTerms(List<String> terms) {
      this.terms = terms;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
      List<String> list = new ArrayList<>();
      list.add("rowType");
      list.add("term");
      for (String attName : list) {
        if (atts.getValue(attName) != null) {
          terms.add(atts.getValue(attName));
        }
      }
    }
  }

  @Test
  public void testXml() throws Exception {
    // read archive
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("archive-dwc").toPath());

    // write meta.xml
    File tmpMeta = File.createTempFile("meta", ".xml");
    System.out.println("Writing temporary test meta file to " + tmpMeta.getAbsolutePath());
    MetaDescriptorWriter.writeMetaFile(tmpMeta, arch);

    // verify rowType & terms are URIs
    List<String> terms = new ArrayList<>();
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    SAXParser saxParser = spf.newSAXParser();
    XMLReader xmlReader = saxParser.getXMLReader();
    xmlReader.setContentHandler(new SAXExtractTerms(terms));
    xmlReader.parse(new InputSource(new FileInputStream(tmpMeta)));

    assertEquals(18, terms.size());
    for (String term : terms) {
      URI uri = URI.create(term);
      assertNotNull(uri.getScheme(), uri + " is no full URI term");
      assertNotNull(uri.getAuthority(), uri + " is no full URI term");
      assertNotNull(uri.getPath(), uri + " is no full URI term");
    }
  }

  @Test
  public void testRoundtrip() {
    try {
      // read archive
      Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("archive-dwc").toPath());
      assertNotNull(arch);
      assertNotNull(arch.getCore());
      assertEquals(0, arch.getCore().getId().getIndex());
      assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
      assertEquals(2, arch.getExtensions().size());
      assertEquals("\t", arch.getCore().getFieldsTerminatedBy());
      assertNull(arch.getCore().getField(DwcTerm.scientificName).getDelimitedBy());
      assertEquals(";", arch.getCore().getField(DwcTerm.nomenclaturalStatus).getDelimitedBy());
      assertEquals(NOMENCLATURAL_CODE_VOCABULARY, arch.getCore().getField(DwcTerm.nomenclaturalCode).getVocabulary());
      assertEquals(LANGUAGE_VOCABULARY, arch.getExtension(GbifTerm.VernacularName).getField(DcTerm.language).getVocabulary());

      // write meta.xml
      File tmpDwca = createTmpMeta(arch);
      Files.createFile(tmpDwca.toPath().resolve("DarwinCore.txt"));
      Files.createFile(tmpDwca.toPath().resolve("VernacularName.txt"));
      Files.createFile(tmpDwca.toPath().resolve("media.txt"));

      Archive arch2 = DwcFiles.fromLocation(tmpDwca.toPath());
      // core props
      ArchiveFile core = arch2.getCore();
      assertNotNull(core);
      assertNotNull(core.getId());
      assertTrue(core.hasTerm(DwcTerm.scientificName));
      assertEquals("DarwinCore.txt", core.getLocation());
      assertEquals("\t", core.getFieldsTerminatedBy());
      assertNull(core.getField(DwcTerm.scientificName).getDelimitedBy());
      assertEquals(";", core.getField(DwcTerm.nomenclaturalStatus).getDelimitedBy());
      assertEquals(NOMENCLATURAL_CODE_VOCABULARY, core.getField(DwcTerm.nomenclaturalCode).getVocabulary());

      for (ArchiveField f : arch.getCore().getFields().values()) {
        assertTrue(core.hasTerm(f.getTerm().qualifiedName()));
        assertEquals(core.getField(f.getTerm().qualifiedName()).getIndex(), f.getIndex());
      }

      // extensions props
      assertEquals(2, arch2.getExtensions().size());
      Set<String> filenames = new HashSet<>();
      filenames.add("VernacularName.txt");
      filenames.add("media.txt");

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
    File tmpMeta = new File(tmpDir, Archive.META_FN);
    System.out.println("Writing temporary test meta file to " + tmpMeta.getAbsolutePath());
    MetaDescriptorWriter.writeMetaFile(tmpMeta, arch);
    return tmpDir;
  }

  @Test
  public void testRoundtripQuotes() {
    try {
      // read archive
      Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("xml-entity-meta").toPath());
      assertNotNull(arch);
      assertNotNull(arch.getCore());
      assertNotNull(arch.getCore().getId());
      assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
      assertEquals(1, arch.getExtensions().size());

      // write meta.xml
      File tmpDwca = createTmpMeta(arch);
      Files.createFile(tmpDwca.toPath().resolve("test"));
      Files.createFile(tmpDwca.toPath().resolve("test2"));
      Archive arch2 = DwcFiles.fromLocation(tmpDwca.toPath());

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

  /**
   * Test the reading of a static meta.xml file.
   */
  @Test
  public void testMetaDescriptorReading() throws Exception {
    // we can read only a meta.xml file as an Archive
    Archive arch = DwcMetaFiles.fromMetaDescriptor(new FileInputStream(FileUtils.getClasspathFile("meta/meta.xml")));

    //validate archive ID field
    ArchiveField af = arch.getCore().getId();
    assertEquals(Integer.valueOf(1), af.getIndex());
    //not specified, should be set to the default value
    assertEquals(ArchiveFile.DEFAULT_FIELDS_ENCLOSED_BY, arch.getCore().getFieldsEnclosedBy());

    //validate default
    af = arch.getCore().getField(DwcTerm.kingdom);
    assertEquals("Animalia", af.getDefaultValue());

    // validate vocabulary
    af = arch.getCore().getField(DwcTerm.nomenclaturalCode);
    assertEquals(NOMENCLATURAL_CODE_VOCABULARY, af.getVocabulary());

    //explicitly set to empty string which means we should not use a fieldsEnclosedBy (value == null)
    assertNull(arch.getExtension(TERM_FACTORY
            .findTerm("http://rs.tdwg.org/invented/Links")).getFieldsEnclosedBy());
  }
}
