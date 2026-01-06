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
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class UsageExampleTest {

  @Test
  public void testUsageExample() throws IOException {
    Path myArchiveFile = FileUtils.getClasspathFile("checklist_980.zip").toPath();
    Path extractToFolder = FileUtils.createTempDir().toPath();
    extractToFolder.toFile().deleteOnExit();

    // Open the Darwin Core Archive
    Archive dwcArchive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder);

    System.out.println("Reading archive from " + dwcArchive.getLocation().getAbsolutePath());
    System.out.println("Archive of rowtype " + dwcArchive.getCore().getRowType() + " with " + dwcArchive.getExtensions().size() + " extensions");

    // Loop over core records and display id, genus, specific epithet
    for (Record rec : dwcArchive.getCore()) {
      System.out.printf("%s: %s %s%n", rec.id(), rec.value(DwcTerm.genus), rec.value(DwcTerm.specificEpithet));
    }

    // Loop over star records and display id, core record data, and extension data
    for (StarRecord rec : dwcArchive) {
      System.out.printf("%s: %s %s%n", rec.core().id(), rec.core().value(DwcTerm.genus), rec.core().value(DwcTerm.specificEpithet));
      if (rec.hasExtension(DwcTerm.Occurrence)) {
        for (Record extRec : rec.extension(DwcTerm.Occurrence)) {
          System.out.println(" - " + extRec.value(DwcTerm.country));
        }
      }
    }
  }
}
