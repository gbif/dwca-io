package org.gbif.dwc;

import org.gbif.dwc.record.StarRecord;
import org.gbif.utils.file.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class ParallelOpeningTest implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(ParallelOpeningTest.class);

  private Archive arch;

  /**
   * Check we don't get a race condition when opening an archive which needs sorting.
   *
   * This downloads a large archive (we don't want it in Git) and checks it has at least one extension.
   */
  @Test
  public void testParallelOpening() throws Exception {
    URL ipni = new URL("http://rs.gbif.org/datasets/itis.zip");
    Path archivePath = FileUtils.createTempDir().toPath().resolve("itis.zip");
    Files.copy(ipni.openStream(), archivePath);

    Path extractToFolder = FileUtils.createTempDir().toPath();
    extractToFolder.toFile().deleteOnExit();

    arch = DwcFiles.fromCompressed(archivePath, extractToFolder);

    Assert.assertFalse(arch.getExtensions().isEmpty());

    // Initialize (i.e. normalize and sort) the archive in the background.
    new Thread(this).start();
    // Wait ½s, which should be enough for the CSV header to be written, but not the sorted data rows.
    Thread.sleep(500);

    int counter = 0;
    for (StarRecord rec : arch) {
      if (counter == 0) {
        LOG.info("Read first record {}", rec);
      }
      counter++;
    }

    LOG.info("Counted {} records", counter);

    Assert.assertTrue("Many records extracted", counter > 500000);

    Files.delete(archivePath);
    FileUtils.deleteDirectoryRecursively(extractToFolder.toFile());
  }

  @Override
  public void run() {
    LOG.info("Initializing archive in a background thread");
    try {
      arch.initialize();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
