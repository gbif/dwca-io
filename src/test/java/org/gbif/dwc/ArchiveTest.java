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

import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArchiveTest {

  /**
   * Check we can handle the simplest of archives.
   */
  @Test
  public void testIteratorDwc() throws Exception {
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("simplest-archive").toPath());

    int count = 0;
    for (StarRecord rec : arch) {
      assertEquals("Quercus alba", rec.core().value(DwcTerm.scientificName));
      assertEquals("Species", rec.core().value(DwcTerm.taxonRank));
      count++;
    }
    assertEquals(2, count);
  }

  /**
   * Check we can handle an archive file with multiple header lines
   */
  @Test
  public void testMultilineHeader() throws Exception {
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("multiline-header").toPath());

    int count = 0;
    for (StarRecord rec : arch) {
      count++;
      assertEquals(String.valueOf(count), rec.core().id());
      assertEquals("Quercus alba", rec.core().value(DwcTerm.scientificName));
      assertEquals("Species", rec.core().value(DwcTerm.taxonRank));
    }
    assertEquals(2, count);
  }

  /**
   * Check we can handle an archive file with multiple |SV files in the core and extension.
   */
  @Test
  public void testMultifile() throws Exception {
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("multifile-psv").toPath());

    int count = 0;
    for (StarRecord rec : arch) {
      count++;
      assertEquals(String.valueOf(count), rec.core().id());
      assertEquals("Quercus alba", rec.core().value(DwcTerm.scientificName));
      assertEquals("Species", rec.core().value(DwcTerm.taxonRank));
      assertEquals("id"+count, rec.extension(GbifTerm.Multimedia).get(0).value(DcTerm.identifier));
    }
    assertEquals(6, count);
  }

  /**
   * Check we can handle an archive file with multiple files each with multiple header lines
   */
  @Test
  public void testMultifileMultilineHeader() throws Exception {
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("multifile-multiline-header").toPath());

    int count = 0;
    for (StarRecord rec : arch) {
      count++;
      assertEquals(String.valueOf(count), rec.core().id());
      assertEquals("Quercus alba", rec.core().value(DwcTerm.scientificName));
      assertEquals("Species", rec.core().value(DwcTerm.taxonRank));
    }
    assertEquals(6, count);
  }

  @Test
  public void testConstituents() {
    File dir = FileUtils.getClasspathFile("constituentsdwca");

    Archive arch = new Archive();
    arch.setLocation(dir);
    arch.setMetadataLocation("eml.xml");
    ArchiveField id = new ArchiveField(0, null, null, null);
    ArchiveField datasetId = new ArchiveField(1, DwcTerm.datasetID, null, null);
    ArchiveField sciname = new ArchiveField(2, DwcTerm.scientificName, null, null);

    Map<Term, ArchiveField> fields = new HashMap<>();
    fields.put(DwcTerm.taxonomicStatus, sciname);
    fields.put(DwcTerm.datasetID, datasetId);

    Map<String, File> cons = arch.getConstituentMetadata();
    assertEquals(6, cons.size());
    for (Map.Entry<String, File> c : cons.entrySet()) {
      final String name = c.getKey();
      final File file = c.getValue();
      assertEquals(name, file.getName().split("\\.")[0]);
    }
  }
}
