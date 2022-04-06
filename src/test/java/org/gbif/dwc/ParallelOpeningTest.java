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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test checks we don't get a race condition between two <strong>processes</strong> opening the same DWC-A at the
 * same time.
 *
 * It needs to be run from the command line, in parallel.
 *
 * mvn exec:java -Dexec.mainClass=org.gbif.dwc.ParallelOpeningTest
 *
 * Monitor open files with something like:
 *
 * while :; do for i in `pgrep -f ParallelOpeningTest`; do echo $i && ls -l /proc/$i/fd | grep /tmp/par && echo; sleep 1; done; done
 */
public class ParallelOpeningTest {
  private static final Logger LOG = LoggerFactory.getLogger(ParallelOpeningTest.class);

  private Archive arch;

  /**
   * Check we don't get a race condition when opening an archive which needs sorting.
   *
   * This downloads a large archive (we don't want it in Git) and checks it has at least one extension.
   */
  public void testParallelOpening() throws Exception {
    Path archivePath = Paths.get("/tmp/parallel-opening-test-archive.zip");
    Path extractToFolder = Paths.get("/tmp/parallel-opening-test-archive");
    if (!archivePath.toFile().exists()) {
      URL download = new URL("http://api.gbif.org/v1/occurrence/download/request/0012957-180131172636756.zip");
      Files.copy(download.openStream(), archivePath);
      extractToFolder.toFile().mkdir();
      arch = DwcFiles.fromCompressed(archivePath, extractToFolder);
    }

    arch = DwcFiles.fromLocation(extractToFolder);

    assertFalse(arch.getExtensions().isEmpty());

    // Initialize (i.e. normalize and sort) the archive in the background.
    LOG.info("Initializing archive");
    arch.initialize();
    LOG.info("Initialization completed, locks should have been released.");

    int counter = 0;
    for (StarRecord rec : arch) {
      if (counter == 0) {
        LOG.info("Read first record {}", rec);
      }
      counter++;
    }

    LOG.info("Counted {} records", counter);

    LOG.info("All files should have been closed.");
    Thread.sleep(30_000);

    assertTrue(counter > 500000, "Many records extracted");
  }

  public static void main(String... args) throws Exception {
    ParallelOpeningTest pot = new ParallelOpeningTest();
    pot.testParallelOpening();
  }
}
