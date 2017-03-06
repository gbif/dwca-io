/***************************************************************************
 * Copyright 2010-2017 Global Biodiversity Information Facility Secretariat
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

package org.gbif.util;

import org.gbif.dwca.io.ArchiveFile;
import org.gbif.utils.file.csv.CSVReader;

import java.io.IOException;

/**
 * Helper class to get {@link CSVReader} instances from {@link ArchiveFile}.
 *
 */
public class CSVReaderHelper {

  public static CSVReader build(ArchiveFile source) throws IOException {
    return new CSVReader(source.getLocationFile(), source.getEncoding(), source.getFieldsTerminatedBy(),
            source.getFieldsEnclosedBy(), source.getIgnoreHeaderLines());
  }

}
