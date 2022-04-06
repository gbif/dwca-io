/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.dwc;

import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test related to {@link ArchiveFile}.
 */
public class ArchiveFileTest {

  @Test
  public void testIterator() throws UnsupportedArchiveException, IOException {
    // test proper archive
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("archive-dwc/DarwinCore.txt").toPath());
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
    assertNotNull(last);
    assertEquals("3082", last.id());
  }

  @Test
  public void testRowTypeEquivalence() {
    ArchiveFile af = new ArchiveFile();
    af.setRowType(DwcTerm.Occurrence);
    assertEquals(af.getRowType(), DwcTerm.Occurrence);
    assertEquals(DwcTerm.Occurrence, af.getRowType());
    assertEquals(DwcTerm.Occurrence.qualifiedName(), af.getRowType().qualifiedName());
  }

  @Test
  public void testGetHeader() throws UnsupportedArchiveException, IOException {
    ArchiveFile core = getCore("archive-dwc/DarwinCore.txt");
    List<List<Term>> header = core.getHeader();
    assertEquals(12, header.size());
    assertEquals(DwcTerm.scientificName, header.get(2).get(0));
  }

  @Test
  public void testIdWithTermAssociated() throws UnsupportedArchiveException, IOException {
    ArchiveFile core = getCore("meta-xml-variants/dwca-id-with-term");
    List<List<Term>> header = core.getHeader();
    assertEquals(6, header.size());
    assertEquals(DwcTerm.occurrenceID, header.get(0).get(0));
    assertEquals(0, header.get(3).size());
  }

  @Test
  public void testDefaultValues() throws UnsupportedArchiveException, IOException {
    ArchiveFile core = getCore("meta-xml-variants/dwca-id-with-term");
    Optional<Map<Term, String>> defaultValues = core.getDefaultValues();
    assertTrue(defaultValues.isPresent());
    assertEquals("Plantae", defaultValues.get().get(DwcTerm.kingdom));
    //declared with an index
    assertEquals("XYZ", defaultValues.get().get(DwcTerm.nomenclaturalCode));
  }

  @Test
  public void testEmptyArchiveFile() {
    ArchiveFile archiveFile = new ArchiveFile();
    List<List<Term>> header = archiveFile.getHeader();
    assertEquals(0, header.size());
    assertFalse(archiveFile.getDefaultValues().isPresent());
  }

  private ArchiveFile getCore(String testFilePath) throws IOException {
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile(testFilePath).toPath());
    return arch.getCore();
  }
  
}
