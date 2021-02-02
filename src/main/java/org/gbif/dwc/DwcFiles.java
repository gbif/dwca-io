/*
 * Copyright 2021 Global Biodiversity Information Facility (GBIF)
 *
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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Collections of static methods to work with Darwin Core (archive) files.
 *
 * TODO: methods without validation, probably for use with the validator.
 */
public class DwcFiles {

  /**
   * Collections of static methods, no constructors.
   */
  private DwcFiles() {
  }

  /**
   * Build a Darwin Core {@link Archive} from a location. The location can be an uncompressed directory or an uncompressed file.
   *
   * @param dwcLocation the location of an expanded Darwin Core Archive directory, or a single Darwin Core text file
   *
   * @return new {@link Archive}, never null. But, the {@link Archive} can be empty (e.g. no core)
   */
  public static Archive fromLocation(Path dwcLocation) throws IOException, UnsupportedArchiveException {
    // delegate to InternalDwcFileFactory
    Archive archive = InternalDwcFileFactory.fromLocation(dwcLocation);
    archive.validate();
    return archive;
  }

  /**
   * Build a Darwin Core {@link Archive} from a location. The location can be an uncompressed directory or an uncompressed file.
   *
   * This method skips basic validation, and should only be used by a tool that does its own validation.
   *
   * @param dwcLocation the location of an expanded Darwin Core Archive directory, or a single Darwin Core text file
   *
   * @return new {@link Archive}, never null. But, the {@link Archive} can be empty (e.g. no core)
   */
  public static Archive fromLocationSkipValidation(Path dwcLocation) throws IOException, UnsupportedArchiveException {
    // delegate to InternalDwcFileFactory
    return InternalDwcFileFactory.fromLocation(dwcLocation);
  }

  /**
   * Build an {@link Archive} from a compressed file. The compressed file will be extracted in the provided directory.
   * The supported compressions are zip and gzip.
   *
   * @param dwcaLocation the location of a compressed Darwin Core Archive
   * @param destination  the destination of the uncompressed content.
   *
   * @return new {@link Archive}, never null. But, the {@link Archive} can be empty (e.g. no core)
   *
   * @throws IOException
   * @throws UnsupportedArchiveException
   */
  public static Archive fromCompressed(Path dwcaLocation, Path destination) throws IOException, UnsupportedArchiveException {
    // delegate to InternalDwcFileFactory
    Archive archive = InternalDwcFileFactory.fromCompressed(dwcaLocation, destination);
    archive.validate();
    return archive;
  }
}
