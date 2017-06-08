package org.gbif.tabular;

import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.tabular.TabularDataFileReader;
import org.gbif.utils.file.tabular.TabularFiles;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * Warning: this class will probably be removed shortly
 *
 * Static utility methods related to {@link TermTabularDataFileReader} instances.
 */
public class TermTabularFiles {

  /**
   * Get a new TermTabularDataFileReader for {@link Term}.
   *
   * @param reader
   * @param delimiterChar
   * @param headerLine
   * @param columnMapping
   * @return
   */
  public static TermTabularDataFileReader newTermMappedTabularFileReader(Reader reader, char delimiterChar,
                                                                         boolean headerLine,
                                                                         Term[] columnMapping) throws IOException {
    Preconditions.checkNotNull(columnMapping, "columnMapping must be provided");
    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles
            .newTabularFileReader(reader, delimiterChar, headerLine);
    return new TermTabularDataFileReader(tabularFileReader, columnMapping);
  }

//
//  /**
//   * Get a new TermTabularDataFileReader from a {@link ArchiveFile} definition for content as {@link InputStream}.
//   *
//   * @param in          the content to read
//   * @param archiveFile the "definition" of the archive
//   * @param headerLine  is the header line included in the content to read ?
//   *
//   * @return
//   */
//  public static TermTabularDataFileReader newDwcTabularFileReader(Reader reader, ArchiveFile archiveFile,
//                                                                  boolean headerLine) {
//    Preconditions.checkNotNull(archiveFile, "DarwinCore archive fields must be provided");
//    Preconditions.checkArgument(archiveFile.getFieldsTerminatedBy().length() == 1,
//            "DarwinCore archive getFieldsTerminatedBy must be 1 char");
//
//    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles
//            .newTabularFileReader(in, archiveFile.getFieldsTerminatedBy().charAt(0), headerLine);
//
//    return new DwcTabularDataFileReader(tabularFileReader, archiveFile.getId(),
//            Lists.newArrayList(archiveFile.getFields().values()));
//  }

}
