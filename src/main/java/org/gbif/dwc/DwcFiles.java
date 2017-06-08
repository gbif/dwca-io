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

/**
 * Collections of static methods to work with Darwin Core (archive) files.
 */
public class DwcFiles {

  private static FileUtils F_UTILS = new FileUtils();

  /**
   * @param replaceNulls    if true record values will have literal nulls replaced with NULL.
   * @param replaceEntities if true html & xml entities in record values will be replaced with the interpreted value.
   */
  public static ClosableIterator<Record> iterator(ArchiveFile source, boolean replaceNulls, boolean replaceEntities) throws IOException {
    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles.newTabularFileReader(
            Files.newBufferedReader(source.getLocationFile() != null ? source.getLocationFile().toPath() : source.getArchive().getLocation().toPath()),
            source.getFieldsTerminatedBy().charAt(0), source.getLinesTerminatedBy(), source.getFieldsEnclosedBy(),
            source.getIgnoreHeaderLines() != 0);
    return new DwcRecordIterator(tabularFileReader, source.getId(), source.getFields(), source.getRowType(), replaceNulls, replaceEntities);
  }

  /**
   * Prepare an {@link Archive} into a {@link NormalizedDwcArchive} which allows to get {@link StarRecord} {@link ClosableIterator}.
   * @param archive
   * @param replaceNulls
   * @param replaceEntities
   * @throws IOException
   * @return
   */
  public static NormalizedDwcArchive prepareArchive(final Archive archive, boolean replaceNulls, boolean replaceEntities) throws IOException {

    //if no extensions are provided we can simply use a normal iterator
    if(archive.getExtensions().isEmpty()) {
      return new NormalizedDwcArchive(() -> iterator(archive.getCore(), replaceNulls, replaceEntities));
    }

    //otherwise, we need to sort core + extensions
    sortFiles(archive);

    return new NormalizedDwcArchive(() -> buildSortedIterator(archive.getCore(), replaceNulls, replaceEntities),
            () ->  buildSortedIteratorExt(archive, replaceNulls, replaceEntities));
  }

  /**
   * Sorts all files according in the {@link Archive} so that we can easily iterate over all files at once.
   */
  private static void sortFiles(Archive archive) throws IOException {
    sort(archive.getCore());
    for(ArchiveFile archiveFile : archive.getExtensions()){
      sort(archiveFile);
    }
  }

  /**
   * Sort a single {@link ArchiveFile}. Sorting implies a normalization phase to ensure we sort the file properly.
   *
   * @param archiveFile
   * @throws IOException
   */
  private static void sort(ArchiveFile archiveFile) throws IOException {

    File locationFileNormalized = getLocationFileNormalized(archiveFile.getLocationFile());

    TabularFileNormalizer.normalizeFile(archiveFile.getLocationFile().toPath(), locationFileNormalized.toPath(),
            Charset.forName(archiveFile.getEncoding()), archiveFile.getFieldsTerminatedBy().charAt(0),
            archiveFile.getLinesTerminatedBy(), archiveFile.getFieldsEnclosedBy());

    F_UTILS.sort(locationFileNormalized, ArchiveFile.getLocationFileSorted(archiveFile.getLocationFile()), archiveFile.getEncoding(),
            archiveFile.getId().getIndex(), archiveFile.getFieldsTerminatedBy(), archiveFile.getFieldsEnclosedBy(),
            TabularFileNormalizer.NORMALIZED_END_OF_LINE, archiveFile.getIgnoreHeaderLines());

    locationFileNormalized.delete();
  }

  private static File getLocationFileNormalized(File location) {
    return new File(location.getParentFile(), location.getName() + "-normalized");
  }

  /**
   * Build an iterator (pointing to the sorted tabular file) for each extensions of the {@link Archive}.
   * @param archive
   * @param replaceNulls
   * @param replaceEntities
   * @return
   * @throws IOException
   */
  private static Map<Term, ClosableIterator<Record>> buildSortedIteratorExt(Archive archive,
                                                                            boolean replaceNulls, boolean replaceEntities) throws IOException {
    Map<Term, ClosableIterator<Record>> extensionIts = new HashMap<>();
    for(ArchiveFile ext : archive.getExtensions()){
      extensionIts.put(ext.getRowType(), buildSortedIterator(ext, replaceNulls, replaceEntities));
    }
    return extensionIts;
  }

  /**
   * Build an iterator on top of the provided {@link ArchiveFile} pointing to the sorted tabular file.
   * The sorted tabular file is also assumed to have been normalized.
   * @param af
   * @param replaceNulls
   * @param replaceEntities
   * @return
   * @throws IOException
   */
  private static ClosableIterator<Record> buildSortedIterator(ArchiveFile af,
                                                              boolean replaceNulls, boolean replaceEntities) throws IOException {
    TabularDataFileReader<List<String>> tabularFileReader = TabularFiles.newTabularFileReader(
            Files.newBufferedReader(ArchiveFile.getLocationFileSorted(af.getLocationFile()).toPath()),
            af.getFieldsTerminatedBy().charAt(0),
            TabularFileNormalizer.NORMALIZED_END_OF_LINE, af.getFieldsEnclosedBy(),
            af.getIgnoreHeaderLines() != 0);
    return new DwcRecordIterator(tabularFileReader, af.getId(), af.getFields(), af.getRowType(), replaceNulls, replaceEntities);
  }

}
