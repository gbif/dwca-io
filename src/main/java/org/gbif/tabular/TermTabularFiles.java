package org.gbif.tabular;

import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.utils.file.tabular.TabularDataFileReader;
import org.gbif.utils.file.tabular.TabularFiles;

import java.io.InputStream;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Static utility methods related to {@link TermTabularDataFileReader} instances.
 */
public class TermTabularFiles {

  /**
   * Get a new TermTabularDataFileReader for {@link Term}.
   *
   * @param in
   * @param delimiterChar
   * @param headerLine
   * @param columnMapping
   * @return
   */
  public static TermTabularDataFileReader newTermMappedTabularFileReader(InputStream in, char delimiterChar,
                                                                                 boolean headerLine,
                                                                                 Term[] columnMapping){
    Preconditions.checkNotNull(columnMapping, "columnMapping must be provided");

    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles
            .newTabularFileReader(in, delimiterChar, headerLine);
    return new TermTabularDataFileReader(tabularFileReader, columnMapping);
  }


  /**
   * Get a new TermTabularDataFileReader from a {@link ArchiveFile} definition for content as {@link InputStream}.
   *
   * @param in          the content to read
   * @param archiveFile the "definition" of the archive
   * @param headerLine  is the header line included in the content to read ?
   *
   * @return
   */
  public static TermTabularDataFileReader newDwcTabularFileReader(InputStream in, ArchiveFile archiveFile,
                                                                  boolean headerLine) {
    Preconditions.checkNotNull(archiveFile, "DarwinCore archive fields must be provided");
    Preconditions.checkArgument(archiveFile.getFieldsTerminatedBy().length() == 1,
            "DarwinCore archive getFieldsTerminatedBy must be 1 char");

    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles
            .newTabularFileReader(in, archiveFile.getFieldsTerminatedBy().charAt(0), headerLine);

    return new DwcTabularDataFileReader(tabularFileReader, archiveFile.getId(),
            Lists.newArrayList(archiveFile.getFields().values()));
  }

}
