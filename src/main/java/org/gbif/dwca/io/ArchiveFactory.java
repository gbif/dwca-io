/*
 * Copyright 2010-2015 Global Biodiversity Informatics Facility.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gbif.dwca.io;

import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.meta.DwcMetaFiles;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory used to build {@link Archive} object from a DarwinCore Archive file.
 *
 * Replaced by DwcFiles, except that this also does some validation of the archive structure.
 */
public class ArchiveFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveFactory.class);

  /**
   * Opens an archive from a local file and decompresses or copies it into the given archive directory.
   * Make sure the archive directory does not contain files already, any existing files will be removed!
   *
   * If the source archive is an uncompressed, single data file and a valid archive, it will be copied as is
   * to the archiveDir.
   *
   * @param archiveFile the location of a compressed archive or single data file
   * @param archiveDir  empty, writable directory used to keep decompress archive in
   */
  public static Archive openArchive(File archiveFile, File archiveDir) throws IOException, UnsupportedArchiveException {

    try {
      Archive archive = DwcFiles.fromCompressed(archiveFile.toPath(), archiveDir.toPath());
      // keep validation for backward compatibility
      archive.validate();
      return archive;
    }
    catch (UnsupportedArchiveException uaEx){
      LOG.debug("Could not uncompress archive [{}], try to read as single text file", archiveFile, uaEx);
      // If its a text file only we will get this exception - but also for corrupt compressions
      // try to open as text file only and if successful copy file to archive dir
      Archive arch = openArchiveDataFile(archiveFile);
      Files.copy(archiveFile, new File(archiveDir, archiveFile.getName()));
      return arch;
    }
  }

  /**
   * Opens a dwca archive which is just a single decompressed data file with headers, e.g. a csv or tab delimited file
   */
  public static Archive openArchiveDataFile(File dataFile) throws IOException, UnsupportedArchiveException {
    Archive archive = DwcFiles.fromLocation(dataFile.toPath());

    // for backward compatibility reason we keep this behavior to check if we have a metadata file next to
    // a single file.
    if(StringUtils.isBlank(archive.getMetadataLocation())) {
      // check if we also have a metadata file next to this data file
      DwcMetaFiles.discoverMetadataFile(dataFile.getParentFile().toPath())
              .ifPresent(archive::setMetadataLocation);
    }

    // final validation
    archive.validate();
    return archive;
  }

  /**
   * @param dwcaFolder the location of an expanded dwc archive directory or just a single dwc text file
   */
  public static Archive openArchive(File dwcaFolder) throws IOException, UnsupportedArchiveException {

    Archive archive = DwcFiles.fromLocation(dwcaFolder.toPath());

    // final validation
    archive.validate();
    return archive;
  }

}
