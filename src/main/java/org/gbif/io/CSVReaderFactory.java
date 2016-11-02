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

package org.gbif.io;

import org.gbif.dwca.io.ArchiveFile;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.UnkownDelimitersException;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Available for backward compatibility only.
 *
 * Use org.gbif.utils.file.csv.CSVReaderFactory instead
 */
public class CSVReaderFactory {

  private static final Logger LOG = LoggerFactory.getLogger(CSVReaderFactory.class);

  public static CSVReader build(ArchiveFile source) throws IOException {
    return new CSVReader(source.getLocationFile(), source.getEncoding(), source.getFieldsTerminatedBy(),
            source.getFieldsEnclosedBy(), source.getIgnoreHeaderLines());
  }

  /**
   * Available for backward compatibility
   * @param source
   * @param encoding
   * @return
   * @throws UnkownDelimitersException
   */
  private static ArchiveFile buildArchiveFile(File source, String encoding) throws UnkownDelimitersException {
    ArchiveFile dwcFile = new ArchiveFile();
    // add file
    dwcFile.addLocation(source.getAbsolutePath());

    // detect character encoding
    dwcFile.setEncoding(encoding);

    // set header row to 1
    dwcFile.setIgnoreHeaderLines(1);

    org.gbif.utils.file.csv.CSVReaderFactory.CSVMetadata csvMetadata = org.gbif.utils.file.csv.CSVReaderFactory.extractCsvMetadata(source, encoding);

    dwcFile.setFieldsTerminatedBy(csvMetadata.getDelimiter());
    dwcFile.setFieldsEnclosedBy(csvMetadata.getQuotedBy());


    String msg = "Detected field delimiter >>>" + dwcFile.getFieldsTerminatedBy() + "<<<";
    if (dwcFile.getFieldsEnclosedBy() != null) {
      msg += " and quoted by >>>" + dwcFile.getFieldsEnclosedBy() + "<<<";
    }
    LOG.debug(msg);

    return dwcFile;
  }

}
