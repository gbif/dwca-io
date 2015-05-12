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

package org.gbif.dwca.io;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.io.UnsupportedArchiveException;
import org.gbif.dwca.record.Record;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArchiveFileTest {

  @Test
  public void testIterator() throws UnsupportedArchiveException, IOException {
    // test proper archive
    Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("archive-dwc/DarwinCore.txt"));
    ArchiveFile af = arch.getCore();

    assertNotNull(af);
    assertNotNull(af.getId());
    assertTrue(af.hasTerm(DwcTerm.scientificName));

    // test iterator
    int counter = 0;
    Record last = null;
    for (Record rec : af) {
      counter++;
      last = rec;
      if (counter == 1) {
        assertEquals("1559060", rec.id());
      }
    }
    assertEquals(3248, counter);
    assertEquals("3082", last.id());
  }

  @Test
  public void testRowTypeEquivalence() {
    ArchiveFile af = new ArchiveFile();
    af.setRowType(DwcTerm.Occurrence);
    assertEquals(af.getRowType(), DwcTerm.Occurrence);
    assertTrue(af.getRowType().equals(DwcTerm.Occurrence));
    assertTrue(af.getRowType().qualifiedName().equals(DwcTerm.Occurrence.qualifiedName()));
  }
}
