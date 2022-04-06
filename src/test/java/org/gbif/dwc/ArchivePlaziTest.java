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
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ArchivePlaziTest {

  private void assertNumberStarRecords(Archive arch, int expectedRecords) {
    int rows = 0;
    for (StarRecord rec : arch) {
      assertNotNull(rec);
      rows++;
    }
    assertEquals(expectedRecords, rows);
  }

  @Test
  public void testBuildReaderFile() throws IOException {
    File zip = FileUtils.getClasspathFile("plazi/6632D8151686A3F8C71D4B5A5B1181A4.zip");
    File tmpDir = FileUtils.createTempDir();
    tmpDir.deleteOnExit();

    Archive arch = DwcFiles.fromCompressed(zip.toPath(), tmpDir.toPath());
    assertNumberStarRecords(arch, 10);
  }
}
