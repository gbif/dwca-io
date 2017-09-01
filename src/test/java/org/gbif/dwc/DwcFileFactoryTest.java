package org.gbif.dwc;

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class DwcFileFactoryTest {

  @Test
  public void testEmlOnly() throws IOException {
    File eml = FileUtils.getClasspathFile("metadata/eml-alone/");
    Archive archive = DwcFileFactory.fromLocation(eml.toPath());
    assertNotNull(archive.getMetadataLocationFile());
    assertEquals(DwcLayout.DIRECTORY_ROOT, archive.getDwcLayout());

    eml = FileUtils.getClasspathFile("metadata/eml-alone/eml.xml");
    archive = DwcFileFactory.fromLocation(eml.toPath());
    assertNotNull(archive.getMetadataLocationFile());
    assertEquals(DwcLayout.FILE_ROOT, archive.getDwcLayout());
  }

  @Test
  public void testDetermineRowType() {
    Optional<Term> rowType = DwcFileFactory
            .determineRowType(Arrays.asList(DwcTerm.decimalLatitude, DwcTerm.occurrenceID));
    assertEquals(DwcTerm.Occurrence, rowType.get());
  }

  @Test
  public void testDetermineRecordIdentifier() {
    Optional<Term> id = DwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.decimalLatitude, DwcTerm.occurrenceID));
    assertEquals(DwcTerm.occurrenceID, id.get());

    id = DwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.taxonID, DwcTerm.scientificName));
    assertEquals(DwcTerm.taxonID, id.get());

    //eventId should be picked even if taxonID is there
    id = DwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.eventID, DwcTerm.scientificName, DwcTerm.taxonID));
    assertEquals(DwcTerm.taxonID, id.get());

    id = DwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.decimalLongitude, DwcTerm.scientificName,
            DcTerm.identifier));
    assertEquals(DcTerm.identifier, id.get());

    //eventId should be picked even if taxonID is there
    id = DwcFileFactory.determineRecordIdentifier(Arrays.asList(DwcTerm.decimalLongitude, DwcTerm.scientificName, DwcTerm.decimalLatitude));
    assertFalse(id.isPresent());
  }
}
