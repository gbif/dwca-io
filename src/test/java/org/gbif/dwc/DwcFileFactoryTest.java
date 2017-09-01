package org.gbif.dwc;

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DwcFileFactoryTest {

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
