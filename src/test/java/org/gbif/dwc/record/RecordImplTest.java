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

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.text.ArchiveField;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class RecordImplTest {

  @Test
  public void testReplaceNull() {
    RecordImpl r = new RecordImpl(null, (Map<Term, ArchiveField>) null, null, true);
    assertNull(r.replaceNull(null));
    assertNull(r.replaceNull(""));
    assertNull(r.replaceNull(" "));
    assertNull(r.replaceNull("    "));
    assertNull(r.replaceNull("null"));
    assertNull(r.replaceNull("NULL"));
    assertNull(r.replaceNull("\\N"));
    assertNull(r.replaceNull(" Null  "));
    assertEquals("n", r.replaceNull("n"));
    assertEquals("n ", r.replaceNull("n "));
    assertEquals(" - ", r.replaceNull(" - "));
    assertEquals("nulle", r.replaceNull("nulle"));
    assertEquals("n ull", r.replaceNull("n ull"));
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
    RecordImpl rec = new RecordImpl(id, fields, DwcTerm.Taxon.qualifiedName(), true);

    String[] row =
      {"5432", "Abies alba Mill.", "Mill.", "Harry", "Monocotyledonae", "Bertram", "Pincodiae", "Picea picaea L."};
    rec.setRow(row);

    assertEquals(row[1], rec.value(DwcTerm.scientificName));
    assertEquals(row[2], rec.value(DwcTerm.scientificNameAuthorship));
    assertEquals("Plantae", rec.value(DwcTerm.kingdom));
    assertEquals(row[4], rec.value(DwcTerm.phylum));
    assertEquals(row[6], rec.value(DwcTerm.class_));
    assertEquals(row[6], rec.value(DwcTerm.class_.qualifiedName()));
    assertNull(rec.value(DwcTerm.order));
    assertEquals(row[7], rec.value(DwcTerm.acceptedNameUsage));

    assertEquals(row[0], rec.id());
    assertEquals(DwcTerm.Taxon.qualifiedName(), rec.rowType());
  }

  @Test
  public void testDefaultValue() {
    final String DATASET = "ITIS";
    ArchiveField id = new ArchiveField(0, DwcTerm.taxonID, null, null);
    Set<ArchiveField> fields = new HashSet<ArchiveField>();
    fields.add(new ArchiveField(1, DwcTerm.datasetName, DATASET, null));
    RecordImpl rec = new RecordImpl(id, fields, DwcTerm.Taxon.qualifiedName(), true);

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
