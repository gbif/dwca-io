/***************************************************************************
 * Copyright 2010 Global Biodiversity Information Facility Secretariat
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***************************************************************************/

package org.gbif.dwc.record;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.text.ArchiveField;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class RecordImplTest {


  private String setRows(String val, RecordImpl ... recs) {
    for (RecordImpl r : recs) {
      r.setRow(new String[]{val});
    }
    return val;
  }

  /**
   * Test record implementations value method and make sure literal nulls are replaced if requested.
   */
  @Test
  public void testReplaceNull() {
    final Term t = DwcTerm.scientificName;
    ArchiveField af = new ArchiveField();
    af.setTerm(t);
    af.setIndex(0);
    af.setType(ArchiveField.DataType.string);
    List<ArchiveField> fields = Lists.newArrayList(af);
    final RecordImpl r = new RecordImpl(null, fields, null, true);
    final RecordImpl r2 = new RecordImpl(null, fields, null, false);

    String val = setRows(null, r, r2);
    assertNull(r.value(t));
    assertNull(r2.value(t));

    val = setRows("", r, r2);
    assertNull(r.value(t));
    assertNull(r2.value(t));

    val = setRows(" ", r, r2);
    assertNull(r.value(t));
    assertNull(r2.value(t));

    val = setRows("    ", r, r2);
    assertNull(r.value(t));
    assertNull(r2.value(t));

    val = setRows("null", r, r2);
    assertNull(r.value(t));
    assertEquals(val, r2.value(t));

    val = setRows("NULL", r, r2);
    assertNull(r.value(t));
    assertEquals(val, r2.value(t));

    val = setRows("\\N", r, r2);
    assertNull(r.value(t));
    assertEquals(val, r2.value(t));

    val = setRows(" Null  ", r, r2);
    assertNull(r.value(t));
    assertEquals(val, r2.value(t));

    val = setRows("n", r, r2);
    assertEquals(val, r.value(t));
    assertEquals(val, r2.value(t));

    val = setRows("n ", r, r2);
    assertEquals(val, r.value(t));
    assertEquals(val, r2.value(t));

    val = setRows(" - ", r, r2);
    assertEquals(val, r.value(t));
    assertEquals(val, r2.value(t));

    val = setRows("nulle", r, r2);
    assertEquals(val, r.value(t));
    assertEquals(val, r2.value(t));

    val = setRows("n ull", r, r2);
    assertEquals(val, r.value(t));
    assertEquals(val, r2.value(t));
  }

  @Test
  public void testGetFullScientificName() {
    ArchiveField id = new ArchiveField(0, DwcTerm.taxonID, null, null);
    Set<ArchiveField> fields = new HashSet<ArchiveField>();
    fields.add(new ArchiveField(1, DwcTerm.scientificName, null, null));
    fields.add(new ArchiveField(2, DwcTerm.scientificNameAuthorship, null, null));
    fields.add(new ArchiveField(null, DwcTerm.kingdom, "Plantae", null));
    fields.add(new ArchiveField(4, DwcTerm.phylum, null, null));
    fields.add(new ArchiveField(6, DwcTerm.class_, null, null));
    fields.add(new ArchiveField(7, DwcTerm.acceptedNameUsage, null, null));
    RecordImpl rec = new RecordImpl(id, fields, DwcTerm.Taxon, true);

    String[] row =
      {"5432", "Abies alba Mill.", "Mill.", "Harry", "Monocotyledonae", "Bertram", "Pincodiae", "Picea picaea L."};
    rec.setRow(row);

    assertEquals(row[1], rec.value(DwcTerm.scientificName));
    assertEquals(row[2], rec.value(DwcTerm.scientificNameAuthorship));
    assertEquals("Plantae", rec.value(DwcTerm.kingdom));
    assertEquals(row[4], rec.value(DwcTerm.phylum));
    assertEquals(row[6], rec.value(DwcTerm.class_));
    assertEquals(row[6], rec.value(DwcTerm.class_));
    assertNull(rec.value(DwcTerm.order));
    assertEquals(row[7], rec.value(DwcTerm.acceptedNameUsage));

    assertEquals(row[0], rec.id());
    assertEquals(DwcTerm.Taxon, rec.rowType());
  }

  @Test
  public void testDefaultValue() {
    final String DATASET = "ITIS";
    ArchiveField id = new ArchiveField(0, DwcTerm.taxonID, null, null);
    Set<ArchiveField> fields = new HashSet<ArchiveField>();
    fields.add(new ArchiveField(1, DwcTerm.datasetName, DATASET, null));
    RecordImpl rec = new RecordImpl(id, fields, DwcTerm.Taxon, true);

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
