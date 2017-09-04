package org.gbif.dwc.meta;

import org.gbif.utils.file.FileUtils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests related to {@link DwcMetaFiles}.
 */
public class DwcMetaFilesTest {

  @Test
  public void testDiscoverMetadataFile() {
    assertTrue(DwcMetaFiles.discoverMetadataFile(FileUtils.getClasspathFile("metadata/eml-alone/eml.xml").toPath()).isPresent());
  }
}
