package org.gbif.dwc;

import org.gbif.dwc.meta.DwcMetaFiles;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.io.UnsupportedArchiveException;
import org.gbif.utils.file.CompressionUtil;
import org.gbif.utils.file.tabular.TabularDataFileReader;
import org.gbif.utils.file.tabular.TabularFileMetadata;
import org.gbif.utils.file.tabular.TabularFileMetadataExtractor;
import org.gbif.utils.file.tabular.TabularFiles;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


/**
 * Internal class responsible to handle the creation of {@link Archive} objects and related functions.
 *
 */
class InternalDwcFileFactory {

  private static final Logger LOG = LoggerFactory.getLogger(InternalDwcFileFactory.class);

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

  private static final List<String> DATA_FILE_SUFFICES = ImmutableList.of(".csv", ".txt", ".tsv", ".tab", ".text", ".data", ".dwca");

  // Utility class
  private InternalDwcFileFactory() {}

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

  private static List<File> extractPossibleDataFile(File dwcFolder) {
    List<File> dataFiles = new ArrayList<>();
    for (String suffix : DATA_FILE_SUFFICES) {
      FileFilter ff = FileFilterUtils.and(
              FileFilterUtils.suffixFileFilter(suffix, IOCase.INSENSITIVE), HiddenFileFilter.VISIBLE
      );
      dataFiles.addAll(Arrays.asList(dwcFolder.listFiles(ff)));
    }
    return dataFiles;
  }

  /**
   * Creates an {@link Archive} from a single file.
   *
   * @param dwcFile represents a single data file or a metadata file.
   * @return a new {@link Archive}, never null.
   * @throws IOException
   */
  private static Archive archiveFromSingleFile(Path dwcFile) throws IOException {
    Archive archive = new Archive();
    archive.setLocation(dwcFile.toFile());
    archive.setDwcLayout(DwcLayout.fromFile(dwcFile.toFile()));

    Optional<String> possibleMetadataFile = DwcMetaFiles.discoverMetadataFile(dwcFile);
    if(possibleMetadataFile.isPresent()) {
      archive.setMetadataLocation(possibleMetadataFile.get());
    }
    else{
      ArchiveFile coreFile = fromSingleFile(dwcFile);
      coreFile.addLocation(dwcFile.getFileName().toString());
      archive.setCore(coreFile);
    }

    return archive;
  }

  /**
   * @param dwcLocation the location of an expanded dwc archive directory or just a single dwc text file
   * @return new {@link Archive}, never null. But, the {@link Archive} can be empty (e.g. no core)
   */
  static Archive fromLocation(Path dwcLocation) throws IOException, UnsupportedArchiveException {
    if (!Files.exists(dwcLocation)) {
      throw new FileNotFoundException("dwcLocation does not exist: " + dwcLocation.toAbsolutePath());
    }
    // delegate to {@link #archiveFromSingleFile) if its a single file, not a folder
    if (Files.isRegularFile(dwcLocation)) {
      return archiveFromSingleFile(dwcLocation);
    }

    Archive archive = new Archive();
    applyIpt205Patch(dwcLocation);

    // Check for meta descriptor
    Path metaDescriptorFile = dwcLocation.resolve(Archive.META_FN);
    if (Files.exists(metaDescriptorFile)) {
      // read metaDescriptor file
      try {
        archive = DwcMetaFiles.fromMetaDescriptor(new FileInputStream(metaDescriptorFile.toFile()));
      } catch (SAXException | IOException e) {
        // using UnsupportedArchiveException for backward compatibility but IOException would be fine here
        throw new UnsupportedArchiveException(e);
      }
    } else {
      // meta.xml lacking.
      // Try to detect data files ourselves as best as we can.
      // look for a single, visible text data file
      List<File> dataFiles = extractPossibleDataFile(dwcLocation.toFile());
      if (dataFiles.size() == 1) {
        archive = archiveFromSingleFile(dataFiles.get(0).toPath());
      }
    }

    // check if we also have a metadata file next to this data file
    DwcMetaFiles.discoverMetadataFile(dwcLocation)
            .ifPresent(archive::setMetadataLocation);

    archive.setLocation(dwcLocation.toFile());
    archive.setDwcLayout(DwcLayout.DIRECTORY_ROOT);

    return archive;
  }

  static Archive fromCompressed(Path dwcaLocation, Path destination) throws IOException, UnsupportedArchiveException {
    if (!Files.exists(dwcaLocation)) {
      throw new FileNotFoundException("dwcaLocation does not exist: " + dwcaLocation.toAbsolutePath());
    }

    if (Files.exists(destination)) {
      // clean up any existing folder
      LOG.debug("Deleting existing archive folder [{}]", destination.toAbsolutePath());
      org.gbif.utils.file.FileUtils.deleteDirectoryRecursively(destination.toFile());
    }
    FileUtils.forceMkdir(destination.toFile());
    // try to decompress archive
    try {
      CompressionUtil.decompressFile(destination.toFile(), dwcaLocation.toFile(), true);
      // we keep subfolder, but often the entire archive is within one subfolder. Remove that root folder if present
      File[] rootFiles = destination.toFile().listFiles((FileFilter) HiddenFileFilter.VISIBLE);
      if (rootFiles.length == 1) {
        File root = rootFiles[0];
        if (root.isDirectory()) {
          // single root dir, flatten structure
          LOG.debug("Removing single root folder {} found in decompressed archive", root.getAbsoluteFile());
          for (File f : FileUtils.listFiles(root, TrueFileFilter.TRUE, null)) {
            File f2 = new File(destination.toFile(), f.getName());
            f.renameTo(f2);
          }
        }
      }
      // continue to read archive from the tmp dir
      return fromLocation(destination);
    } catch (CompressionUtil.UnsupportedCompressionType e) {
      throw new UnsupportedArchiveException(e);
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
  static ArchiveFile fromSingleFile(Path dataFile) throws UnsupportedArchiveException, IOException {
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
      // there are never any quotes in term names - remove them just in case the we didn't recognize them
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
