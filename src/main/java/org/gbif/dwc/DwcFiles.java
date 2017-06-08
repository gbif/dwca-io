package org.gbif.dwc;

import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.StarRecord;
import org.gbif.utils.file.ClosableIterator;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.tabular.TabularDataFileReader;
import org.gbif.utils.file.tabular.TabularFileNormalizer;
import org.gbif.utils.file.tabular.TabularFiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;

import static org.gbif.dwca.io.ArchiveFile.getLocationFileSorted;

/**
 * Collections of static methods to work with Darwin Core (archive) files.
 */
public class DwcFiles {

  private static FileUtils F_UTILS = new FileUtils();

  /**
   * Collections of static methods, no constructors.
   */
  private DwcFiles() {
  }

  /**
   * @param replaceNulls    if true record values will have literal nulls replaced with NULL.
   * @param replaceEntities if true html & xml entities in record values will be replaced with the interpreted value.
   */
  public static ClosableIterator<Record> iterator(ArchiveFile source, boolean replaceNulls, boolean replaceEntities) throws IOException {
    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles.newTabularFileReader(
            Files.newBufferedReader(source.getLocationFile() != null ? source.getLocationFile().toPath() : source.getArchive().getLocation().toPath()),
            getFieldsTerminatedBy(source.getFieldsTerminatedBy()), source.getLinesTerminatedBy(), source.getFieldsEnclosedBy(),
            isHeaderLineIncluded(source.getIgnoreHeaderLines()), getLineToSkipBeforeHeader(source.getIgnoreHeaderLines()));
    return new DwcRecordIterator(tabularFileReader, source.getId(), source.getFields(), source.getRowType(), replaceNulls, replaceEntities);
  }

  /**
   * Get the fieldsTerminatedBy as char or throw exception.
   *
   * @param fieldsTerminatedBy
   *
   * @return
   */
  private static char getFieldsTerminatedBy(String fieldsTerminatedBy) {
    Objects.requireNonNull(fieldsTerminatedBy, "fieldsTerminatedBy shall be provided");
    if (fieldsTerminatedBy.length() != 1) {
      throw new IllegalArgumentException();
    }
    return fieldsTerminatedBy.charAt(0);
  }

  /**
   * Determines if a header line is included based on an optional integer.
   *
   * @param ignoreHeaderLines
   *
   * @return
   */
  private static boolean isHeaderLineIncluded(Integer ignoreHeaderLines) {
    return ignoreHeaderLines != null && ignoreHeaderLines > 0;
  }

  /**
   * Determines the number of line to skip before the header line or return null.
   *
   * @param ignoreHeaderLines
   *
   * @return
   */
  private static Integer getLineToSkipBeforeHeader(Integer ignoreHeaderLines) {
    if (ignoreHeaderLines != null && ignoreHeaderLines > 1) {
      return ignoreHeaderLines - 1;
    }
    return null;
  }

  /**
   * Prepare an {@link Archive} into a {@link NormalizedDwcArchive} which allows to get {@link StarRecord} {@link
   * ClosableIterator}.
   * This method will initiate the normalization process. This process can take some times depending on the size of
   * files and number
   * of extension.
   *
   * @param archive
   * @param replaceNulls
   * @param replaceEntities
   *
   * @return new {@link NormalizedDwcArchive} instance
   *
   * @throws IOException
   */
  public static NormalizedDwcArchive prepareArchive(final Archive archive, boolean replaceNulls, boolean replaceEntities) throws IOException {

    //if no extensions are provided we can simply use a normal iterator
    if (archive.getExtensions().isEmpty()) {
      return new NormalizedDwcArchive(() -> iterator(archive.getCore(), replaceNulls, replaceEntities));
    }

    //otherwise, we need to sort core + extensions
    normalizeAndSortArchiveFiles(archive);

    return new NormalizedDwcArchive(() -> buildSortedIterator(archive.getCore(), replaceNulls, replaceEntities),
            () -> buildSortedIteratorExt(archive, replaceNulls, replaceEntities));
  }

  /**
   * Sorts all files according in the {@link Archive} so that we can easily iterate over all files at once.
   */
  private static void normalizeAndSortArchiveFiles(Archive archive) throws IOException {
    normalizeAndSort(archive.getCore());
    for (ArchiveFile archiveFile : archive.getExtensions()) {
      normalizeAndSort(archiveFile);
    }
  }

  /**
   * Sort a single {@link ArchiveFile}. Sorting implies a normalization phase to ensure we sort the file properly.
   * Note that the file will not be sorted if the sorted file is already there and its date is later than the file
   * we want to sort.
   *
   * @param archiveFile
   * @return the file was sorted or not. If the file was not sorted it simply means it was not required.
   *
   * @throws IOException
   */
  @VisibleForTesting
  protected static boolean normalizeAndSort(ArchiveFile archiveFile) throws IOException {

    File fileToSort = archiveFile.getLocationFile();
    File sortedFile = ArchiveFile.getLocationFileSorted(archiveFile.getLocationFile());
    File normalizedFile = getLocationFileNormalized(archiveFile.getLocationFile());
    String linesTerminatedBy = archiveFile.getLinesTerminatedBy();

    //if we already sorted the file and its source didn't change we can avoid doing it again
    if (sortedFile.exists() && Files.getLastModifiedTime(sortedFile.toPath()).toInstant().isAfter(
            Files.getLastModifiedTime(fileToSort.toPath()).toInstant())) {
      return false;
    }

    //if the linesTerminatedBy used is the same as TabularFileNormalizer and no quoted cells are used
    //we can skip normalization
    boolean normalizationRequired = TabularFileNormalizer.NORMALIZED_END_OF_LINE.equals(linesTerminatedBy) &&
            archiveFile.getFieldsEnclosedBy() == null;

    if (normalizationRequired) {
      TabularFileNormalizer.normalizeFile(archiveFile.getLocationFile().toPath(), normalizedFile.toPath(),
              Charset.forName(archiveFile.getEncoding()), getFieldsTerminatedBy(archiveFile.getFieldsTerminatedBy()),
              linesTerminatedBy, archiveFile.getFieldsEnclosedBy());
      fileToSort = normalizedFile;
    }

    F_UTILS.sort(fileToSort, ArchiveFile.getLocationFileSorted(archiveFile.getLocationFile()), archiveFile.getEncoding(),
            archiveFile.getId().getIndex(), archiveFile.getFieldsTerminatedBy(), archiveFile.getFieldsEnclosedBy(),
            TabularFileNormalizer.NORMALIZED_END_OF_LINE, archiveFile.getIgnoreHeaderLines());

    Files.deleteIfExists(normalizedFile.toPath());

    return true;
  }

  private static File getLocationFileNormalized(File location) {
    return new File(location.getParentFile(), location.getName() + "-normalized");
  }

  /**
   * Build an iterator (pointing to the sorted tabular file) for each extensions of the {@link Archive}.
   *
   * @param archive
   * @param replaceNulls
   * @param replaceEntities
   *
   * @return
   *
   * @throws IOException
   */
  private static Map<Term, ClosableIterator<Record>> buildSortedIteratorExt(Archive archive,
                                                                            boolean replaceNulls, boolean replaceEntities) throws IOException {
    Map<Term, ClosableIterator<Record>> extensionIts = new HashMap<>();
    for (ArchiveFile ext : archive.getExtensions()) {
      extensionIts.put(ext.getRowType(), buildSortedIterator(ext, replaceNulls, replaceEntities));
    }
    return extensionIts;
  }

  /**
   * Build an iterator on top of the provided {@link ArchiveFile} pointing to the sorted tabular file.
   * The sorted tabular file is also assumed to have been normalized.
   *
   * @param af
   * @param replaceNulls
   * @param replaceEntities
   *
   * @return
   *
   * @throws IOException
   */
  private static ClosableIterator<Record> buildSortedIterator(ArchiveFile af,
                                                              boolean replaceNulls, boolean replaceEntities) throws IOException {
    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles.newTabularFileReader(
            Files.newBufferedReader(getLocationFileSorted(af.getLocationFile()).toPath()),
            getFieldsTerminatedBy(af.getFieldsTerminatedBy()),
            TabularFileNormalizer.NORMALIZED_END_OF_LINE, af.getFieldsEnclosedBy(),
            isHeaderLineIncluded(af.getIgnoreHeaderLines()), getLineToSkipBeforeHeader(af.getIgnoreHeaderLines()));
    return new DwcRecordIterator(tabularFileReader, af.getId(), af.getFields(), af.getRowType(), replaceNulls, replaceEntities);
  }

}
