package org.gbif.dwc;

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.record.Record;
import org.gbif.utils.file.ClosableIterator;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to {@link InternalDwcFileFactory}.
 */
public class InternalDwcFileFactoryTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testFromCompressedZip() throws UnsupportedArchiveException, IOException {
    // test zip with 1 extension file
    File zip = FileUtils.getClasspathFile("archive-tax.zip");
    assertIdInCompressed(zip.toPath(), "113775");
  }

  @Test
  public void testFromCompressedTarGzip() throws UnsupportedArchiveException, IOException {
    // test gziped tar file with 1 extension
    File gzip = FileUtils.getClasspathFile("archive-tax.tar.gz");
    assertIdInCompressed(gzip.toPath(), "113775");
  }

  @Test(expected = FileNotFoundException.class)
  public void testNonExistingFileFromCompressed() throws Exception {
    // test zip with 1 extension file
    Path none = Paths.get("/ping/pong/nuts");
    // try to open archive
    InternalDwcFileFactory.fromCompressed(none, folder.newFolder().toPath());
  }

  @Test(expected = FileNotFoundException.class)
  public void testNonExistingFileFromLocation() throws Exception {
    File none = new File("/ping/pong/nuts");
    InternalDwcFileFactory.fromLocation(none.toPath());
  }

  /**
   * Givin a compressed file, make sure we can uncompressed it, read the core and find the provided id.
   * @param compressedFile
   * @param id
   * @throws IOException
   */
  private void assertIdInCompressed(Path compressedFile, String id) throws IOException {
    File tmpDir = folder.newFolder();

    // open archive from zip
    Archive arch = InternalDwcFileFactory.fromCompressed(compressedFile, tmpDir.toPath());
    assertNotNull(arch.getCore().getId());
    assertEquals(1, arch.getExtensions().size());

    boolean found = false;
    try (ClosableIterator<Record> it = DwcFiles.iterator(arch.getCore())){
      while(it.hasNext()){
        if(id.equals(it.next().id())){
          found = true;
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    assertTrue("Can find the id " + id + " inside the archive " + compressedFile.getFileName(), found);
  }

  @Test
  public void testDetermineRowType() {
    Optional<Term> rowType = InternalDwcFileFactory
            .determineRowType(Arrays.asList(DwcTerm.decimalLatitude, DwcTerm.occurrenceID));
    assertEquals(DwcTerm.Occurrence, rowType.get());
  }

  @Test
  public void testDetermineRecordIdentifier() {
    Optional<Term> id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.decimalLatitude, DwcTerm.occurrenceID));
    assertEquals(DwcTerm.occurrenceID, id.get());

    id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.taxonID, DwcTerm.scientificName));
    assertEquals(DwcTerm.taxonID, id.get());

    //eventId should be picked even if taxonID is there
    id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.eventID, DwcTerm.scientificName, DwcTerm.taxonID));
    assertEquals(DwcTerm.taxonID, id.get());

    id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.decimalLongitude, DwcTerm.scientificName,
            DcTerm.identifier));
    assertEquals(DcTerm.identifier, id.get());

    //eventId should be picked even if taxonID is there
    id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.decimalLongitude, DwcTerm.scientificName, DwcTerm.decimalLatitude));
    assertFalse(id.isPresent());
  }
}
