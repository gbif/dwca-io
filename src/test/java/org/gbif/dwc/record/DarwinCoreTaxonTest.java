package org.gbif.dwc.record;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;

import java.util.Date;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DarwinCoreTaxonTest {

  @Test
  public void testGetFullScientificName() {
    DarwinCoreTaxon dwc = new DarwinCoreTaxon("Festuca x brinkmannii A. Braun");
    dwc.setScientificNameAuthorship("A. Braun");
    assertEquals("Festuca x brinkmannii A. Braun", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    dwc = new DarwinCoreTaxon("Festuca x brinkmannii A. Braun");
    dwc.setScientificNameAuthorship("A.Braun");
    assertEquals("Festuca x brinkmannii A. Braun", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    dwc = new DarwinCoreTaxon("Festuca x brinkmannii");
    dwc.setScientificNameAuthorship("A. Braun");
    assertEquals("Festuca x brinkmannii A. Braun", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    dwc = new DarwinCoreTaxon("Festuca A. Braun brinkmannii subsp. brinkmannii");
    dwc.setScientificNameAuthorship("A. Braun");
    assertEquals("Festuca A. Braun brinkmannii subsp. brinkmannii", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    dwc = new DarwinCoreTaxon("Festuca brinkmannii");
    dwc.setScientificNameAuthorship("A. Braun");
    assertEquals("Festuca brinkmannii A. Braun", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    dwc = new DarwinCoreTaxon(" ");
    dwc.setScientificNameAuthorship("Rafinesque, 1795");
    dwc.setKingdom("Plantae");
    assertNull(dwc.getFullScientificName());
    assertEquals("Plantae", dwc.getLowestScientificName());

    dwc = new DarwinCoreTaxon(" ");
    dwc.setScientificNameAuthorship("L.");
    dwc.setKingdom("Plantae");
    dwc.setFamily("Asteraceae");
    assertNull(dwc.getFullScientificName());
    assertEquals("Asteraceae", dwc.getLowestScientificName());

    dwc = new DarwinCoreTaxon();
    dwc.setScientificNameAuthorship("L.");
    dwc.setKingdom("Plantae");
    dwc.setGenus("Poa");
    assertEquals("Poa", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    dwc.setSpecificEpithet("annua");
    assertEquals("Poa annua L.", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    dwc.setScientificNameAuthorship("");
    assertEquals("Poa annua", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    dwc.setScientificNameAuthorship("L.");
    dwc.setInfraspecificEpithet("alpina");
    assertEquals("Poa annua alpina L.", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    dwc.setVerbatimTaxonRank("var.");
    assertEquals("Poa annua alpina L.", dwc.getFullScientificName());
    assertEquals(dwc.getFullScientificName(), dwc.getLowestScientificName());

    // test not identical authorships
    dwc = new DarwinCoreTaxon();
    dwc.setScientificName("Aagaardia protensa Saether, 2000");
    dwc.setGenus("Aagaardia");
    dwc.setSpecificEpithet("protensa");
    dwc.setScientificNameAuthorship("Saether 2000");
    assertEquals(dwc.getScientificName(), dwc.getFullScientificName());

    // test genus
    dwc = new DarwinCoreTaxon();
    dwc.setScientificName("Aagaardia protensa Saether, 2000");
    dwc.setGenus("Aagaardia");
    dwc.setSpecificEpithet("protensa");
    dwc.setScientificNameAuthorship("Saether 2000");
    assertEquals(dwc.getScientificName(), dwc.getFullScientificName());

    // test authors with single letter that is part of the name
    dwc = new DarwinCoreTaxon();
    dwc.setScientificName("Trifolium repens");
    dwc.setGenus("Trifolium");
    dwc.setSpecificEpithet("repens");
    dwc.setScientificNameAuthorship("L.");
    assertEquals("Trifolium repens", dwc.getScientificName());
    assertEquals("Trifolium repens L.", dwc.getFullScientificName());

    dwc = new DarwinCoreTaxon();
    dwc.setScientificName("Trifolium thalii");
    dwc.setScientificNameAuthorship("Vill.");
    assertEquals("Trifolium thalii Vill.", dwc.getFullScientificName());

    dwc = new DarwinCoreTaxon();
    dwc.setScientificName("Trifolium alpestre");
    dwc.setScientificNameAuthorship("L.");
    assertEquals("Trifolium alpestre L.", dwc.getFullScientificName());

    // autonyms ???
    dwc = new DarwinCoreTaxon();
    dwc.setScientificName("Trifolium alpestre alpestre");
    dwc.setScientificNameAuthorship("L.");
    // this would be really correct for autonyms...
    //assertEquals("Trifolium alpestre L. alpestre", dwc.getFullScientificName());
    assertEquals("Trifolium alpestre alpestre L.", dwc.getFullScientificName());
  }

  @Test
  public void testInterpretedRank() {
    DarwinCoreTaxon dwc = new DarwinCoreTaxon("Festuca brinkmannii A. Braun");
    dwc.setKingdom("PLANTAE");
    dwc.setPhylum("Magnoliophyta");
    dwc.setFamily("Poaceae");
    dwc.setGenus("Festuca");
    dwc.setTaxonRank("species");
    assertEquals("species", dwc.getTaxonRankInterpreted());

    dwc.setSpecificEpithet("brinkmannii");
    assertEquals("species", dwc.getTaxonRankInterpreted());

    dwc.setInfraspecificEpithet("nonexistii");
    assertEquals("species", dwc.getTaxonRankInterpreted());

    dwc.setScientificName(null);
    assertEquals("species", dwc.getTaxonRankInterpreted());

    dwc.setTaxonRank(null);
    assertEquals("infraspecies", dwc.getTaxonRankInterpreted());

    dwc.setVerbatimTaxonRank("var.");
    assertEquals("var.", dwc.getTaxonRankInterpreted());

    dwc.setSpecificEpithet(null);
    assertEquals("genus", dwc.getTaxonRankInterpreted());

    dwc.setGenus(null);
    assertEquals("family", dwc.getTaxonRankInterpreted());
  }

  @Test
  public void testHigherClassification() {
    DarwinCoreTaxon dwc = new DarwinCoreTaxon("Festuca brinkmannii A. Braun");
    dwc.setKingdom("PLANTAE");
    dwc.setPhylum("Magnoliophyta");
    dwc.setFamily("Poaceae");
    dwc.setGenus("Festuca");
    assertEquals("Plantae | Magnoliophyta | Poaceae | Festuca", dwc.buildHigherClassification(null));
    assertEquals("Plantae | Magnoliophyta | Poaceae", dwc.buildHigherClassification('f'));
    assertEquals("Plantae | Magnoliophyta", dwc.buildHigherClassification('o'));
  }

  @Test
  public void testProperties() {
    Set<String> groups = ImmutableSet.of(DwcTerm.GROUP_TAXON, DwcTerm.GROUP_RECORD);
    DarwinCoreTaxon dwc = new DarwinCoreTaxon();
    for (DwcTerm t : DwcTerm.values()) {
      if (groups.contains(t.getGroup()) && !t.isClass()){
        String val = new Date().toString();
        dwc.setProperty(t, val);
        try {
          dwc.getProperty(t);
          assertEquals(val, dwc.getProperty(t));
        } catch (Exception e) {
          System.err.println(e.getMessage());
        }
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidProperties() {
    DarwinCoreTaxon dwc = new DarwinCoreTaxon();
    dwc.getProperty(GbifTerm.canonicalName);
  }
}
