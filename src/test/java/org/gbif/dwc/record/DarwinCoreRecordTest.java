package org.gbif.dwc.record;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.text.ArchiveField;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class DarwinCoreRecordTest {

  @Test
  public void testProperties() {
    DarwinCoreRecord dwc = new DarwinCoreRecord();
    for (DwcTerm t : DwcTerm.values()) {
      // only test non class terms
      if (t.isClass()) {
        continue;
      }
      // ignore measurements and relationship group terms
      if (DwcTerm.GROUP_MEASUREMENTORFACT.equalsIgnoreCase(t.getGroup())) {
        continue;
      }
      if (DwcTerm.GROUP_RESOURCERELATIONSHIP.equalsIgnoreCase(t.getGroup())) {
        continue;
      }
      String val = new Date().toString();
      dwc.setProperty(t, val);
      assertEquals("missing term " + t.qualifiedName(), val, dwc.getProperty(t));
    }
  }

  @Test
  public void testDefaultValue() {
    final String DATASET = "ITIS";
    ArchiveField id = new ArchiveField(0, DwcTerm.taxonID, null, null);
    Set<ArchiveField> fields = new HashSet<ArchiveField>();
    fields.add(new ArchiveField(1, DwcTerm.datasetName, DATASET, null));
    RecordImpl rec = new RecordImpl(id, fields, DwcTerm.Taxon.qualifiedName(), true);
    DarwinCoreRecord dwc = new DarwinCoreRecord();

    String[] row = {"5432", "IPNI"};
    rec.setRow(row);
    assertEquals(row[1], rec.value(DwcTerm.datasetName));

    row = new String[] {"5432", null};
    rec.setRow(row);
    assertEquals(DATASET, rec.value(DwcTerm.datasetName));

    row = new String[] {"5432", ""};
    rec.setRow(row);
    assertEquals(DATASET, rec.value(DwcTerm.datasetName));
  }
}
