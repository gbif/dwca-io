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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to {@link InternalDwcFileFactory}.
 */
public class InternalDwcFileFactoryTest {

  @TempDir
  public File folder;

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

  @Test
  public void testNonExistingFileFromCompressed() {
    // test zip with 1 extension file
    Path none = Paths.get("/ping/pong/nuts");
    // try to open archive
    assertThrows(FileNotFoundException.class,
        () -> InternalDwcFileFactory.fromCompressed(none, folder.toPath()));
  }

  @Test
  public void testNonExistingFileFromLocation() {
    File none = new File("/ping/pong/nuts");
    assertThrows(FileNotFoundException.class, () -> InternalDwcFileFactory.fromLocation(none.toPath()));
  }

  /**
   * Givin a compressed file, make sure we can uncompressed it, read the core and find the provided id.
   */
  private void assertIdInCompressed(Path compressedFile, String id) throws IOException {
    File tmpDir = folder;

    // open archive from zip
    Archive arch = InternalDwcFileFactory.fromCompressed(compressedFile, tmpDir.toPath());
    assertNotNull(arch.getCore().getId());
    assertEquals(1, arch.getExtensions().size());

    boolean found = false;
    try (ClosableIterator<Record> it = arch.getCore().iterator()){
      while(it.hasNext()){
        if(id.equals(it.next().id())){
          found = true;
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    assertTrue(found, "Can find the id " + id + " inside the archive " + compressedFile.getFileName());
  }

  @Test
  public void testDetermineRowType() {
    Optional<Term> rowType = InternalDwcFileFactory
            .determineRowType(Arrays.asList(DwcTerm.decimalLatitude, DwcTerm.occurrenceID));
    assertTrue(rowType.isPresent());
    assertEquals(DwcTerm.Occurrence, rowType.get());
  }

  @Test
  public void testDetermineRecordIdentifier() {
    Optional<Term> id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.decimalLatitude, DwcTerm.occurrenceID));
    assertTrue(id.isPresent());
    assertEquals(DwcTerm.occurrenceID, id.get());

    id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.taxonID, DwcTerm.scientificName));
    assertTrue(id.isPresent());
    assertEquals(DwcTerm.taxonID, id.get());

    //eventId should be picked even if taxonID is there
    id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.eventID, DwcTerm.scientificName, DwcTerm.taxonID));
    assertTrue(id.isPresent());
    assertEquals(DwcTerm.taxonID, id.get());

    id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.decimalLongitude, DwcTerm.scientificName,
            DcTerm.identifier));
    assertTrue(id.isPresent());
    assertEquals(DcTerm.identifier, id.get());

    //eventId should be picked even if taxonID is there
    id = InternalDwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.decimalLongitude, DwcTerm.scientificName, DwcTerm.decimalLatitude));
    assertFalse(id.isPresent());
  }
}
