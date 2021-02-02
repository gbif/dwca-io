package org.gbif.dwc.meta;

import org.gbif.utils.file.FileUtils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to {@link DwcMetaFiles}.
 */
public class DwcMetaFilesTest {

  @Test
  public void testDiscoverMetadataFile() {
    assertTrue(DwcMetaFiles.discoverMetadataFile(FileUtils.getClasspathFile("archive-dwc/eml.xml").toPath()).isPresent());
  }
}
