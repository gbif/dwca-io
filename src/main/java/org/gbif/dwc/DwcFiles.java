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
