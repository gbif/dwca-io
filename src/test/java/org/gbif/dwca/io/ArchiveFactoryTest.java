package org.gbif.dwca.io;

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.StarRecord;
import org.gbif.io.CSVReaderFactory;
import org.gbif.utils.collection.IterableUtils;
import org.gbif.utils.file.CompressionUtil;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.csv.CSVReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArchiveFactoryTest {

  private void assertNumberOfCoreRecords(Archive arch, int expectedRecords) throws IOException {
    int rows = 0;
    for (Record rec : arch.getCore()) {
      assertNotNull(rec);
      rows++;
    }
    assertEquals(expectedRecords, rows);
  }

  @Test
  public void testMetaHandlerUtf16le() throws Exception {
    for (String fn : new String[]{"/meta/meta.xml", "/meta-utf16le.xml","/xml-entity-meta/meta.xml"}) {
      System.out.println(fn);
      InputStream is = ClassLoader.class.getResourceAsStream(fn);
      ArchiveFactory.readMetaDescriptor(new Archive(), is);
    }
  }

  @Test
  public void testBuildReaderFile() throws IOException {
    Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("TDB_104.csv"));
    CSVReader reader = arch.getCore().getCSVReader();
    assertEquals(7, reader.next().length);
    reader.close();


    reader = ArchiveFactory.openArchive(FileUtils.getClasspathFile("iucn100.tab.txt")).getCore().getCSVReader();
    assertEquals(8, reader.next().length);
    reader.close();

    reader = ArchiveFactory.openArchive(FileUtils.getClasspathFile("iucn100.pipe.txt")).getCore().getCSVReader();
    assertEquals(8, reader.next().length);
    reader.close();

    reader = ArchiveFactory.openArchive(FileUtils.getClasspathFile("csv_quoted-unquoted_headers.csv")).getCore()
      .getCSVReader();
    assertEquals(4, reader.next().length);
    reader.close();
  }

  @Test
  public void testCoreRecords() throws IOException {
    // note that we dont read a dwc archive, but only test the csvreader!
    // we therefore do not detect header rows and count *all* rows instead

    assertNumberOfCoreRecords(ArchiveFactory.openArchive(FileUtils.getClasspathFile("iucn100.tab.txt")), 99);
    assertNumberOfCoreRecords(ArchiveFactory.openArchive(FileUtils.getClasspathFile("iucn100.pipe.txt")), 99);
    assertNumberOfCoreRecords(ArchiveFactory.openArchive(FileUtils.getClasspathFile("iucn100.csv")), 99);
    assertNumberOfCoreRecords(ArchiveFactory.openArchive(FileUtils.getClasspathFile("csv_quoted-unquoted_headers.csv")),
      3);
    assertNumberOfCoreRecords(ArchiveFactory.openArchive(FileUtils.getClasspathFile("csv_incl_single_quotes.csv")), 3);
  }

  /**
   * Test dwca-reader bug 83
   *
   * @see <a href="http://code.google.com/p/darwincore/issues/detail?id=83">Issue 83</a>
   */
  @Test
  public void testCsv() throws UnsupportedArchiveException, IOException {
    File csv = FileUtils.getClasspathFile("csv_always_quoted.csv");
    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(csv);

    boolean found = false;
    for (Record rec : arch.getCore()) {
      if ("18728553".equals(rec.id())) {
        found = true;
        assertEquals("Martins Wood, Ightham", rec.value(DwcTerm.locality));
      }
    }
    assertTrue(found);
  }

  /**
   * Test GNUB style dwca with a single tab delimited file that has a .tab suffix.
   */
  @Test
  public void testGnubTab() throws UnsupportedArchiveException, IOException {
    File tab = FileUtils.getClasspathFile("gnub.tab");
    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(tab);

    Record rec = arch.getCore().iterator().next();
    assertEquals("246daa62-6fce-448f-88b4-94b0ccc89cf1", rec.id());
  }

  /**
   * Test GNUB style dwca with a single tab delimited file that has a .tab suffix.
   */
  @Test
  public void testGnubTabZip() throws UnsupportedArchiveException, IOException {
    // test GNUB zip with 1 data file
    File tmpDir = Files.createTempDirectory("dwca-io-test").toFile();
    tmpDir.deleteOnExit();
    File zip = FileUtils.getClasspathFile("gnub.tab.zip");
    CompressionUtil.decompressFile(tmpDir, zip);

    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(tmpDir);

    Record rec = arch.getCore().iterator().next();
    assertEquals("246daa62-6fce-448f-88b4-94b0ccc89cf1", rec.id());
  }

  @Test
  public void testCsvExcelStyle() throws UnsupportedArchiveException, IOException {
    File csv = FileUtils.getClasspathFile("csv_optional_quotes_excel2008CSV.csv");
    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(csv);
    CSVReader reader = arch.getCore().getCSVReader();

    String[] atom = reader.next();
    assertEquals(3, atom.length);
    assertEquals("1", atom[0]);
    assertEquals("This has a, comma", atom[2]);
    atom = reader.next();
    assertEquals("I say this is only a \"quote\"", atom[2]);
    atom = reader.next();
    assertEquals("What though, \"if you have a quote\" and a comma", atom[2]);
    atom = reader.next();
    assertEquals("What, if we have a \"quote, which has a comma, or 2\"", atom[2]);

    reader.close();

  }


  /**
   * Testing CSV with optional quotes
   */
  @Test
  public void testCsvOptionalQuotes() throws UnsupportedArchiveException, IOException {
    File csv = FileUtils.getClasspathFile("csv_optional_quotes_excel2008CSV.csv");
    Archive arch = ArchiveFactory.openArchive(csv);
    String[] ids = {"1", "2", "3", "4"};
    String[] scinames = {"Gadus morhua", "Abies alba", "Pomatoma saltatrix", "Yikes ofcourses"};
    String[] localities =
      {"This has a, comma", "I say this is only a \"quote\"", "What though, \"if you have a quote\" and a comma",
        "What, if we have a \"quote, which has a comma, or 2\""};
    int row = 0;
    for (Record rec : arch.getCore()) {
      assertEquals(ids[row], rec.id());
      assertEquals(scinames[row], rec.value(DwcTerm.scientificName));
      assertEquals(localities[row], rec.value(DwcTerm.locality));
      row++;
    }
  }

  /**
   * Test IPT bug 2158
   *
   * @see <a href="http://code.google.com/p/gbif-providertoolkit/source/detail?r=2158">IPT revision 2158</a>
   */
  @Test
  public void testIssue2158() throws UnsupportedArchiveException, IOException {
    // test zip with 1 extension file
    File zip = FileUtils.getClasspathFile("archive-tax.zip");
    File tmpDir = Files.createTempDirectory("dwca-io-test").toFile();
    CompressionUtil.decompressFile(tmpDir, zip);
    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(tmpDir);
    assertNotNull(arch.getCore().getId());
    assertEquals(1, arch.getExtensions().size());

    boolean found = false;
    for (Record rec : arch.getCore()) {
      if ("113775".equals(rec.id())) {
        found = true;
        assertEquals(
          "Ehrenberg, 1832, in Hemprich and Ehrenberg, Symbolæ Phisicæ Mammalia, 2: ftn. 1 (last page of fascicle headed \"Herpestes leucurus H. E.\").",
          rec.value(DwcTerm.originalNameUsageID));
      }
    }
    assertTrue(found);
  }

  /**
   * The pensoft archive http://pensoft.net/dwc/bdj/checklist_980.zip
   * contains empty extension files which caused NPE in the dwca reader.
   */
  @Test
  public void testExtensionNPE() throws UnsupportedArchiveException, IOException {
    File zip = FileUtils.getClasspathFile("checklist_980.zip");
    File tmpDir = Files.createTempDirectory("dwca-io-test").toFile();
    CompressionUtil.decompressFile(tmpDir, zip);
    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(tmpDir);
    assertNotNull(arch.getCore().getId());
    assertEquals(3, arch.getExtensions().size());

    boolean found = false;
    for (StarRecord rec : arch) {
      if ("980-sp10".equals(rec.core().id())) {
        found = true;
      }
    }
    assertTrue(found);
  }

  /**
   * Test extension sorting verifying that all core records do have the right number of extension records attached
   * when using the star record iterator.
   */
  @Test
  @Ignore("currently fails with only 661 records coming through instead of 740")
  public void testStarIteratorExtRecords() throws Exception {
    File zip = FileUtils.getClasspathFile("checklist_980.zip");
    File tmpDir = Files.createTempDirectory("dwca-io-test").toFile();
    CompressionUtil.decompressFile(tmpDir, zip);
    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(tmpDir);
    int counter = 0;
    int occCounter = 0;
    Set<String> ids = Sets.newHashSet();
    for (StarRecord rec : arch) {
      counter++;
      ids.add(rec.core().id());
      List<Record> occs = rec.extension(DwcTerm.Occurrence);
      occCounter += occs.size();
    }
    assertEquals("Core taxon file has 356 records", 356, counter);
    assertEquals("Core taxon file has 356 unique ids", 356, ids.size());

    // read extension file on its own and extract core ids to be cross checked with core id set
    CSVReader occReader = CSVReaderFactory.build(arch.getExtension(DwcTerm.Occurrence));
    int occCounter2 = 0;
    for (String[] rec : IterableUtils.iterable(occReader)) {
      String id = rec[1];
      occCounter2++;
      assertTrue("Occurrence coreid " + id + " not existing", ids.contains(id));
    }
    assertEquals("Occurrence extension file has 740 records", 740, occCounter2);
    assertEquals("Occurrence start extensions should be 740 records", 740, occCounter);
  }

  @Test(expected = FileNotFoundException.class)
  public void testNonExistingFile() throws Exception {
    // test zip with 1 extension file
    File none = new File("/ping/pong/nuts");
    // try to open archive
    ArchiveFactory.openArchive(none);
  }

  @Test
  public void testOpenArchiveAsZip() throws UnsupportedArchiveException, IOException {
    // test zip with 1 extension file
    File zip = FileUtils.getClasspathFile("archive-tax.zip");
    File tmpDir = Files.createTempDirectory("dwca-io-test").toFile();
    tmpDir.deleteOnExit();

    // open archive from zip
    Archive arch = ArchiveFactory.openArchive(zip, tmpDir);
    assertNotNull(arch.getCore().getId());
    assertEquals(1, arch.getExtensions().size());

    boolean found = false;
    for (Record rec : arch.getCore()) {
      if ("113775".equals(rec.id())) {
        found = true;
      }
    }
    assertTrue(found);
  }

  @Test
  public void testOpenArchiveAsTarGzip() throws UnsupportedArchiveException, IOException {
    // test gziped tar file with 1 extension
    File zip = FileUtils.getClasspathFile("archive-tax.tar.gz");
    File tmpDir = Files.createTempDirectory("dwca-io-test").toFile();
    tmpDir.deleteOnExit();

    // open compressed archive
    Archive arch = ArchiveFactory.openArchive(zip, tmpDir);
    assertNotNull(arch.getCore().getId());
    assertEquals(1, arch.getExtensions().size());

    boolean found = false;
    for (Record rec : arch.getCore()) {
      if ("113775".equals(rec.id())) {
        found = true;
      }
    }
    assertTrue(found);
  }

  /**
   * Identifier not set properly when reading single csv file
   * the csv file attached is a utf16 little endian encoded file.
   * This encoding is known to cause problems and not supported.
   * If you look at the detected concept terms you will find that there is NO concept at all detected because of the
   * wrong character encoding used (the factory tries it with utf8).
   *
   * @see <a href="http://code.google.com/p/darwincore/issues/detail?id=78">Issue 78</a>
   */
  @Test
  public void testIssue78() throws IOException, UnsupportedArchiveException {
    // test folder with single text file in
    Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("MOBOTDarwinCore.csv"));
    assertNotNull(arch.getCore());
    assertNotNull(arch.getCore().getId());
    assertEquals(DwcTerm.occurrenceID, arch.getCore().getId().getTerm());
    assertNotNull(arch.getCore().getRowType());
    assertEquals(DwcTerm.Occurrence, arch.getCore().getRowType());
    assertTrue(arch.getCore().hasTerm(DwcTerm.occurrenceID));
    assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
    assertEquals("UTF-8", arch.getCore().getEncoding());

    int i = 0;
    for (Record rec : arch.getCore()) {
      i++;
      assertEquals(rec.id(), "MO:Tropicos:" + i);
    }
    assertEquals(3, i);
  }

  @Test
  public void testOpenArchive() throws IOException, UnsupportedArchiveException {
    // test proper archive
    Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("archive-dwc"));
    assertNotNull(arch.getCore());
    assertNotNull(arch.getCore().getId());
    assertNotNull(arch.getCore().getId());
    assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
    assertEquals(2, arch.getExtensions().size());
    assertEquals("DarwinCore.txt", arch.getCore().getLocation());

    // test meta.xml with xml entities as attribute values
    arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("xml-entity-meta"));
    assertNotNull(arch.getCore());
    assertNotNull(arch.getCore().getId());
    assertEquals(new Character('"'), arch.getCore().getFieldsEnclosedBy());
    assertEquals("test", arch.getCore().getLocation());

    // test direct pointer to core data file (with taxonID, meaning it has dwc:Taxon rowType)
    arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("archive-dwc/DarwinCore.txt"));
    assertNotNull(arch.getCore());
    assertNotNull(arch.getCore().getId());
    assertEquals(DwcTerm.taxonID, arch.getCore().getId().getTerm());
    assertNotNull(arch.getCore().getRowType());
    assertEquals(DwcTerm.Taxon, arch.getCore().getRowType());
    assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
    assertEquals(0, arch.getExtensions().size());
    Iterator<StarRecord> dwci = arch.iterator();
    StarRecord star = dwci.next();
    assertEquals("Globicephala melaena melaena Traill", star.core().value(DwcTerm.scientificName));
    assertNull(arch.getCore().getLocation());

    // test folder with single text file in (with taxonID, meaning it has dwc:Taxon rowType)
    arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("dwca"));
    assertNotNull(arch.getCore());
    assertNotNull(arch.getCore().getId());
    assertEquals(DwcTerm.taxonID, arch.getCore().getId().getTerm());
    assertNotNull(arch.getCore().getRowType());
    assertEquals(DwcTerm.Taxon, arch.getCore().getRowType());
    assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
    assertTrue(arch.getCore().hasTerm(DwcTerm.taxonID));
    assertEquals(0, arch.getExtensions().size());
    dwci = arch.iterator();
    star = dwci.next();
    assertEquals("Globicephala melaena melaena Traill", star.core().value(DwcTerm.scientificName));
    assertEquals("1559060", star.core().value(DwcTerm.taxonID));
    assertEquals("DarwinCore.txt", arch.getCore().getLocation());
  }

  @Test
  public void testOpenSmallArchiveWithEmptyLines() throws IOException, UnsupportedArchiveException {
    // test folder with single text file in
    Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("empty_line.tab"));
    assertNotNull(arch.getCore());
    assertNotNull(arch.getCore().getId());
    assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
    assertEquals(0, arch.getExtensions().size());
    Iterator<StarRecord> dwci = arch.iterator();
    StarRecord star = dwci.next();
    star = dwci.next();
    star = dwci.next();
    star = dwci.next();
    star = dwci.next();
    assertEquals("Delphinus delphis var. delphis", star.core().value(DwcTerm.scientificName));
    int i = 0;
    for (StarRecord rec : arch) {
      i++;
      if (i > 20) {
        break;
      }
    }
    assertEquals(6, i);
  }

  /**
   * Test bug 77.
   *
   * @see <a href="http://code.google.com/p/darwincore/issues/detail?id=77">Issue 77</a>
   */
  @Test
  public void testQuotedHeaders() throws IOException, UnsupportedArchiveException {
    // test folder with single text file in
    Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("quoted_headers_MOBOTDarwinCore.csv"));
    assertNotNull(arch.getCore());
    assertNotNull(arch.getCore().getId());
    assertTrue(arch.getCore().hasTerm(DwcTerm.occurrenceID));
    assertTrue(arch.getCore().hasTerm(DwcTerm.catalogNumber));
    assertTrue(arch.getCore().hasTerm(DwcTerm.institutionCode));
    assertTrue(arch.getCore().hasTerm(DwcTerm.basisOfRecord));
    assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
    assertTrue(arch.getCore().hasTerm(DwcTerm.maximumElevationInMeters));
    assertTrue(arch.getCore().hasTerm(DcTerm.references));

    int i = 0;
    for (Record rec : arch.getCore()) {
      i++;
      assertEquals(rec.id(), "MO:Tropicos:" + i);
    }
    assertEquals(2, i);
  }


  @Test
  public void testTab() throws UnsupportedArchiveException, IOException {
    File tab = FileUtils.getClasspathFile("dwca/DarwinCore.txt");
    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(tab);

    boolean found = false;
    int count = 0;
    for (Record rec : arch.getCore()) {
      count++;
      if ("1559060".equals(rec.id())) {
        found = true;
        assertEquals("Globicephala melaena melaena Traill", rec.value(DwcTerm.scientificName));
        assertEquals("Hershkovitz, P., Catalog of Living Whales, Smithsonian Institution, Bulletin 246, 1966, p. 91",
          rec.value(DwcTerm.nameAccordingTo));
        assertEquals("105849", rec.value(DwcTerm.parentNameUsageID));
      }
    }
    assertTrue(arch.getCore().getIgnoreHeaderLines() == 1);
    assertEquals(0, arch.getExtensions().size());
    assertEquals(24, count);
    assertTrue(found);
  }

  @Test
  public void testTab2() throws UnsupportedArchiveException, IOException {
    File tab = FileUtils.getClasspathFile("issues/Borza.txt");
    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(tab);

    boolean found = false;
    int count = 0;
    for (Record rec : arch.getCore()) {
      count++;
      if (count == 1) {
        //1 Borza:Corophiidae:1 Borza Borza:Corophiidae Corophiidae 1   Animalia  Arthropoda  Malacostraca  Amphipoda Corophiidae Chelicorophium    sowinskyi   Chelicorophium sowinskyi  "(Martynov, 1924)"  species   Péter Borza   Europe  Danube  Hungary     47.788111 18.960944           Preserved Specimen  1917-07-17  1917  7 17      Unger E
        assertEquals("1", rec.id());
        assertEquals("Chelicorophium sowinskyi", rec.value(DwcTerm.scientificName));
        // we do detect optional quotation in tab files...
        assertEquals("(Martynov, 1924)", rec.value(DwcTerm.scientificNameAuthorship));
        assertEquals("47.788111", rec.value(DwcTerm.decimalLatitude));
        assertEquals("18.960944", rec.value(DwcTerm.decimalLongitude));
      }
      if ("173".equals(rec.id())) {
        found = true;
        assertEquals("Chelicorophium curvispinum", rec.value(DwcTerm.scientificName));
        assertEquals("(G. O. Sars, 1895)", rec.value(DwcTerm.scientificNameAuthorship));
        assertEquals("47.965166", rec.value(DwcTerm.decimalLatitude));
        assertEquals("17.304666", rec.value(DwcTerm.decimalLongitude));
      }
    }
    assertTrue(arch.getCore().getIgnoreHeaderLines() == 1);
    assertEquals(435, count);
    assertTrue(found);
  }

  @Test
  public void testTabEol() throws UnsupportedArchiveException, IOException {
    File tab = FileUtils.getClasspathFile("issues/eol/my_darwincore.txt");
    // read archive from this tmp dir
    Archive arch = ArchiveFactory.openArchive(tab);

    boolean found = false;
    int count = 0;
    for (Record rec : arch.getCore()) {
      count++;
      if (count == 1) {
        //1 Borza:Corophiidae:1 Borza Borza:Corophiidae Corophiidae 1   Animalia  Arthropoda  Malacostraca  Amphipoda Corophiidae Chelicorophium    sowinskyi   Chelicorophium sowinskyi  "(Martynov, 1924)"  species   Péter Borza   Europe  Danube  Hungary     47.788111 18.960944           Preserved Specimen  1917-07-17  1917  7 17      Unger E
        assertEquals("1", rec.id());
        assertEquals("gadus morhua", rec.value(DwcTerm.scientificName));
        assertEquals("gadidae", rec.value(DwcTerm.family));
      } else if ("2".equals(rec.id())) {
        found = true;
        assertEquals("chanos chanos", rec.value(DwcTerm.scientificName));
        assertEquals("channidae", rec.value(DwcTerm.family));
      } else {
        assertEquals("3", rec.id());
        assertEquals("mola mola", rec.value(DwcTerm.scientificName));
        assertEquals("familyx", rec.value(DwcTerm.family));
      }
    }
    assertTrue(arch.getCore().getIgnoreHeaderLines() == 1);
    assertEquals(3, count);
    assertTrue(found);
  }

  /**
   * Ensure that extensions are just skipped for archives that do not have the core id in the mapped extension.
   * https://code.google.com/p/darwincore/issues/detail?id=232
   */
  @Test
  public void testNullCoreID() throws IOException {
    try {
      File tmpDir = Files.createTempDirectory("dwca-io-test").toFile();
      tmpDir.deleteOnExit();

      Archive archive = ArchiveFactory.openArchive(FileUtils.getClasspathFile("nullCoreID.zip"), tmpDir);
      Iterator<StarRecord> iter = archive.iterator();
      while (iter.hasNext()) {
        iter.next();
      }
    } catch (UnsupportedArchiveException e) {
      fail("Extensions with no core IDs should be ignored");
    }
  }

  /**
   * Test opening a single data file with both eventID column, meaning it has dwc:Event rowType.
   */
  @Test
  public void testOpenArchiveForEventCore() throws IOException, UnsupportedArchiveException {
    Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("event.txt"));
    assertNotNull(arch.getCore());
    assertNotNull(arch.getCore().getId());
    assertEquals(DwcTerm.eventID, arch.getCore().getId().getTerm());
    assertNotNull(arch.getCore().getRowType());
    assertEquals(DwcTerm.Event, arch.getCore().getRowType());
    assertTrue(arch.getCore().hasTerm(DwcTerm.samplingProtocol));
    assertEquals(0, arch.getExtensions().size());
    Iterator<StarRecord> dwci = arch.iterator();
    StarRecord star = dwci.next();
    assertEquals("Aubach above Wiesthal", star.core().value(DwcTerm.locality));
    assertNull(arch.getCore().getLocation());
  }

  /**
   * Test opening a single data file with a generic ID column and an eventID column meaning the Archive's ID-term
   * gets set to dc:identifier, but its rowType never gets set.
   */
  @Test
  public void testOpenArchiveForGenericCore() throws IOException, UnsupportedArchiveException {
    Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("event-plus-id.txt"));
    assertNotNull(arch.getCore());
    assertNotNull(arch.getCore().getId());
    assertEquals(DcTerm.identifier, arch.getCore().getId().getTerm());
    assertNull(arch.getCore().getRowType());
    assertTrue(arch.getCore().hasTerm(DwcTerm.samplingProtocol));
    assertEquals(0, arch.getExtensions().size());
    Iterator<StarRecord> dwci = arch.iterator();
    StarRecord star = dwci.next();
    assertEquals("Aubach above Wiesthal", star.core().value(DwcTerm.locality));
    assertNull(arch.getCore().getLocation());
  }
}
