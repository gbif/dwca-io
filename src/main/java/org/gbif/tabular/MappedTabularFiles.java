package org.gbif.tabular;

import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.tabular.TabularDataFileReader;
import org.gbif.utils.file.tabular.TabularFiles;

import java.io.InputStream;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * Static utility methods related to {@link MappedTabularDataFileReader} instances.
 */
public class MappedTabularFiles {

  /**
   * Get a new MappedTabularDataFileReader for {@link Term}.
   *
   * @param in
   * @param delimiterChar
   * @param headerLine
   * @param columnMapping
   * @return
   */
  public static MappedTabularDataFileReader<Term> newTermMappedTabularFileReader(InputStream in, char delimiterChar,
                                                                                 boolean headerLine,
                                                                                 Term[] columnMapping){
    Preconditions.checkNotNull(columnMapping, "columnMapping must be provided");

    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles
            .newTabularFileReader(in, delimiterChar, headerLine);
    return new MappedTabularDataFileReader<Term>(tabularFileReader, columnMapping);
  }
}
