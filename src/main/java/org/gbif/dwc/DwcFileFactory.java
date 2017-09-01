package org.gbif.dwc;

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.io.UnsupportedArchiveException;
import org.gbif.utils.file.tabular.TabularDataFileReader;
import org.gbif.utils.file.tabular.TabularFileMetadata;
import org.gbif.utils.file.tabular.TabularFileMetadataExtractor;
import org.gbif.utils.file.tabular.TabularFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * WORK-IN-PROGESS, the class could be renamed without notice.
 * Its visibility is set to punlic only to accommodate {@link org.gbif.dwca.io.ArchiveFactory} during the transition.
 *
 */
public class DwcFileFactory {

  private static final Logger LOG = LoggerFactory.getLogger(DwcFileFactory.class);

  private static final TermFactory TERM_FACTORY = TermFactory.instance();
  private static final String DEFAULT_ENDLINE_CHAR = "\n";
  private static final char DEFAULT_DELIMITER_CHAR = ',';

  /**
   * Predefined mapping between {@link Term} and its rowType.
   * Ordering is important since the first found will be used.
   */
  private static final Map<Term, Term> TERM_TO_ROW_TYPE;
  static {
    Map<Term, Term> idToRowType = new LinkedHashMap<>();
    idToRowType.put(DwcTerm.occurrenceID, DwcTerm.Occurrence);
    idToRowType.put(DwcTerm.taxonID, DwcTerm.Taxon);
    idToRowType.put(DwcTerm.eventID, DwcTerm.Event);
    TERM_TO_ROW_TYPE = Collections.unmodifiableMap(idToRowType);
  }

  /**
   * Terms that can represent an identifier within a file
   */
  private static final List<Term> ID_TERMS = Collections.unmodifiableList(
          Arrays.asList(DwcTerm.occurrenceID, DwcTerm.taxonID, DwcTerm.eventID, DcTerm.identifier));
  
  // Utility class
  private DwcFileFactory() {}

  private static void applyIpt205Patch(final Path dwcFolder) {
    // Accommodate archives coming from legacy IPTs which put a "\" before each filename
    // http://dev.gbif.org/issues/browse/POR-2396
    // https://code.google.com/p/gbif-providertoolkit/issues/detail?id=1015
    Iterator<File> iter = FileUtils.iterateFiles(dwcFolder.toFile(), new String[]{"xml", "txt"}, false);
    while (iter.hasNext()) {
      File f = iter.next();
      if (f.getName().startsWith("\\")) {
        String orig = f.getName();
        String replacement = f.getName().replaceFirst("\\\\", "");
        LOG.info("Renaming file from {} to {}", orig, replacement);
        f.renameTo(new File(dwcFolder.toFile(), replacement));
      }
    }
  }

  /**
   * Return a {@link ArchiveFile} based on a single data file.
   * Delimiter, quote char and encoding will be extracted from the file using {@link TabularFileMetadataExtractor}.
   * Columns will be determined by extracting the first line of the file.
   * The end of line character can only be {@link #DEFAULT_ENDLINE_CHAR}.
   * @param dataFile
   * @return
   * @throws UnsupportedArchiveException
   * @throws IOException
   */
  public static ArchiveFile fromSingleFile(Path dataFile) throws UnsupportedArchiveException, IOException {
    Preconditions.checkArgument(Files.isRegularFile(dataFile), "dataFile shall be a file");
    ArchiveFile dwcFile = new ArchiveFile();
   // dwcFile.addLocation(null);
    dwcFile.setIgnoreHeaderLines(1);

    TabularFileMetadata tabularFileMetadata = TabularFileMetadataExtractor.extractTabularFileMetadata(dataFile);
    dwcFile.setFieldsTerminatedBy(Optional.ofNullable(tabularFileMetadata.getDelimiter()).orElse(DEFAULT_DELIMITER_CHAR).toString());
    dwcFile.setFieldsEnclosedBy(tabularFileMetadata.getQuotedBy());
    dwcFile.setEncoding(tabularFileMetadata.getEncoding().name());

    List<String> headers;
    try (TabularDataFileReader<List<String>> reader =
                 TabularFiles.newTabularFileReader(Files.newBufferedReader(dataFile, tabularFileMetadata.getEncoding()),
                         dwcFile.getFieldsTerminatedBy().charAt(0), DEFAULT_ENDLINE_CHAR,
                         dwcFile.getFieldsEnclosedBy(), true)) {
      headers = reader.getHeaderLine() == null ? Collections.emptyList() : reader.getHeaderLine();
    }

    // detect dwc terms as good as we can based on header row
    int index = 0;
    for (String head : headers) {
      // there are never any quotes in term names - remove them just in case the csvreader didnt recognize them
      if (head != null && head.length() > 1) {
        try {
          Term dt = TERM_FACTORY.findTerm(head);
          dwcFile.addField(new ArchiveField(index, dt, null, ArchiveField.DataType.string));
        } catch (IllegalArgumentException e) {
          LOG.warn("Illegal term name >>{}<< found in header, ignore column {}", head, index);
        }
      }
      index++;
    }

    List<Term> headerAsTerm = dwcFile.getFields().keySet()
            .stream()
            .collect(Collectors.toList());

    determineRecordIdentifier(headerAsTerm).ifPresent(
            t -> dwcFile.setId(dwcFile.getField(t))
    );

    determineRowType(headerAsTerm).ifPresent(dwcFile::setRowType);
    return dwcFile;
  }

  /**
   * Tries to determine the rowType based on a list of {@link Term}.
   *
   * @param terms the list can contain null values
   *
   * @return {@link Term} as {@code Optional} or {@code Optional.empty()} if can not be determined
   */
  static Optional<Term> determineRowType(List<Term> terms) {
    return TERM_TO_ROW_TYPE.entrySet().stream()
            .filter(ke -> terms.contains(ke.getKey()))
            .map(Map.Entry::getValue).findFirst();
  }

  /**
   * Tries to determine the record identifier based on a list of {@link Term}.
   *
   * @param terms the list can contain null values
   *
   * @return {@link Term} as {@code Optional} or {@code Optional.empty()} if can not be determined
   */
  static Optional<Term> determineRecordIdentifier(List<Term> terms) {
    //try to find the first matching term respecting the order defined by ID_TERMS
    return ID_TERMS.stream()
            .filter(terms::contains)
            .findFirst();
  }

}
