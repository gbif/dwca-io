package org.gbif.dwca.io;

import org.gbif.api.model.registry.Dataset;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.DwcLayout;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.record.DarwinCoreRecord;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.RecordImpl;
import org.gbif.dwca.record.RecordIterator;
import org.gbif.dwca.record.StarRecord;
import org.gbif.dwca.record.StarRecordImpl;
import org.gbif.registry.metadata.parse.DatasetParser;
import org.gbif.utils.file.ClosableIterator;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.csv.CSVReader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Darwin Core star archive and its components (metadata, core, extensions).
 *
 * Note: This class contains @Deprecated classes and methods. Everything related to the iteration over the archive
 * is now @Deprecated and {@link DwcFiles} should be used instead.
 *
 * @see <a href="http://tdwg.github.io/dwc/terms/guides/text/">Darwin Core Text Guide</a>
 */
public class Archive implements Iterable<StarRecord> {
  public static final String CONSTITUENT_DIR = "dataset";
  public static final String META_FN = "meta.xml";

  /**
   * An iterator of fixed DarwinCoreRecords over the core file only. This iterator doesn't need any sorted data files
   * as it doesn't deal with extensions.
   */
  @Deprecated
  static class ArchiveDwcIterator implements ClosableIterator<DarwinCoreRecord> {

    private CSVReader coreReader;
    private ArchiveFile core;
    private int lineCount = 0;
    private final RecordImpl record;
    private boolean hasNext = true;
    private final Set<Term> mappedTerms = new HashSet<Term>();

    ArchiveDwcIterator(Archive archive) {
      record = new RecordImpl(archive.getCore(), true, true);
      core = archive.getCore();
      // remember used DwC and DC terms
      for (DwcTerm term : DwcTerm.values()) {
        if (core.hasTerm(term)) {
          mappedTerms.add(term);
        }
      }
      for (DcTerm term : DcTerm.values()) {
        if (core.hasTerm(term)) {
          mappedTerms.add(term);
        }
      }
      try {
        coreReader = archive.getCore().getCSVReader();
        // read first core row
        record.setRow(coreReader.next());
        if (!record.hasRow()) {
          hasNext = false;
        }
      } catch (Exception e) {
        hasNext = false;
        LOG.warn("Exception caught", e);
      }
    }

    public void close() {
      coreReader.close();
    }

    public boolean hasNext() {
      return hasNext;
    }

    public DarwinCoreRecord next() {
      DarwinCoreRecord dwc = new DarwinCoreRecord();
      lineCount++;
      try {
        for (Term term : mappedTerms) {
          dwc.setProperty(term, record.value(term));
        }
        dwc.setId(record.id());
        // read next line to see if it exists at all
        record.setRow(coreReader.next());
        if (!record.hasRow()) {
          hasNext = false;
        }

      } catch (Exception e) {
        LOG.warn("Bad row somewhere around core line: {}", lineCount, e);
      }
      return dwc;
    }

    public void remove() {
      throw new UnsupportedOperationException("Cannot remove a row from archive files");
    }

  }

  /**
   * This class is kept for legacy reason, See {@link DwcFiles} for new usage.
   *
   * An iterator over core records of the archive that returns StarRecords, i.e. a single core record with all its
   * related extension records attached. This is a convenient way to iterate over an entire archive accessing all
   * information including all extensions.
   *
   * Requires underlying data files to be sorted by the coreid column if iteration spans multiple data files, i.e. if
   * extensions exist.
   * Extension rows with a non existing coreid are skipped. This requires that we use the same sorting order in the
   * java code as we use for sorting the data files!
   */
  @Deprecated
  class ArchiveIterator implements ClosableIterator<StarRecord> {

    private final StarRecordImpl rec;
    private RecordIterator coreIter;
    private Set<RecordIterator> closables = new HashSet<RecordIterator>();
    private Map<Term, PeekingIterator<Record>> extensionIters = new HashMap<Term, PeekingIterator<Record>>();
    private Map<Term, Integer> extensionRecordsSkipped = new HashMap<Term, Integer>();

    /**
     * @param replaceNulls if true replaces common literal null values in all records
     */
    ArchiveIterator(Archive archive, boolean replaceNulls, boolean replaceEntities) {
      List<Term> rowTypes = new ArrayList<Term>();

      try {
        if (extensions.isEmpty()) {
          // no need to sort
          coreIter = RecordIterator.build(archive.getCore(), replaceNulls, replaceEntities);
        } else {
          // sort data files to align extension records into a single star record
          if (!archive.sorted) {
            archive.sortFiles();
          }
          coreIter = buildSortedIterator(archive.getCore(), replaceNulls, replaceEntities);
        }
      } catch (IOException e) {
        LOG.warn("IOException opening core file", e);
      }

      for (ArchiveFile af : archive.getExtensions()) {
        rowTypes.add(af.getRowType());
        RecordIterator iter =
          extensions.isEmpty() ? RecordIterator.build(af, replaceNulls, replaceEntities) : buildSortedIterator(af,
            replaceNulls, replaceNulls);
        closables.add(iter);
        extensionIters.put(af.getRowType(), Iterators.peekingIterator(iter));
        extensionRecordsSkipped.put(af.getRowType(), 0);
      }

      rec = new StarRecordImpl(rowTypes);
    }

    private RecordIterator buildSortedIterator(ArchiveFile af, boolean replaceNulls, boolean replaceEntities) {
      // we need to sort the data files
      String original = af.getLocation();
      // temporarily modify archive file to create iterator over sorted file
      af.getLocations().clear();
      af.addLocation(ArchiveFile.getLocationSorted(original));
      RecordIterator iter = RecordIterator.build(af, replaceNulls, replaceEntities);
      // revert to original
      af.getLocations().clear();
      af.addLocation(original);
      return iter;
    }

    @Override
    public void close() {
      coreIter.close();
      for (ClosableIterator<Record> it : closables) {
        try {
          it.close();
        } catch (Exception e) {
          LOG.debug("Can't close ClosableIterator", e);
        }
      }
      for (Map.Entry<Term, Integer> stringIntegerEntry : extensionRecordsSkipped.entrySet()) {
        Integer skipped = stringIntegerEntry.getValue();
        if (skipped > 0) {
          LOG.debug("{} {} extension records without matching core", skipped, stringIntegerEntry.getKey());
        }
      }
    }

    public boolean hasNext() {
      return coreIter.hasNext();
    }

    public StarRecord next() {
      Record core = coreIter.next();
      rec.newCoreRecord(core);
      // add extension records if core id exists
      if (core.id() != null) {
        String id = core.id();
        for (Map.Entry<Term, PeekingIterator<Record>> ext : extensionIters.entrySet()) {
          PeekingIterator<Record> it = ext.getValue();
          Term rowType = ext.getKey();
          while (it.hasNext()) {
            String extId = it.peek().id();
            // make sure we have an extid
            if (Strings.isNullOrEmpty(extId)) {
              it.next();
              continue;
            }
            if (id.equals(extId)) {
              // extension row belongs to this core record
              rec.addRecord(rowType, it.next());
            } else if (id.compareTo(extId) > 0) {
              // TODO: we need to use the exact same sorting order, ie comparator, as we use for sorting the data files!!!
              // this extension id is smaller than the core id and should have been picked up by a core record already
              // seems to have no matching core record, so lets skip it
              it.next();
              extensionRecordsSkipped.put(rowType, extensionRecordsSkipped.get(rowType) + 1);
            } else {
              // higher id, we need to wait for this one
              break;
            }
          }
        }
      }

      return rec;
    }

    public void remove() {
      throw new UnsupportedOperationException("Cannot remove a row from archive files");
    }

  }

  private static final Logger LOG = LoggerFactory.getLogger(Archive.class);

  private String metadataLocation;
  private Dataset metadata;
  private File location;
  private ArchiveFile core;
  private Set<ArchiveFile> extensions = new HashSet<ArchiveFile>();
  private boolean sorted = false;

  private DwcLayout dwcLayout;

  public void addExtension(ArchiveFile extension) {
    extension.setArchive(this);
    extensions.add(extension);
  }

  public ArchiveFile getCore() {
    return core;
  }

  /**
   * Get an extension by its rowType.
   * @param rowType
   * @return ArchiveFile or {@code null} is not found
   */
  public ArchiveFile getExtension(Term rowType) {
    for (ArchiveFile af : extensions) {
      if (af.getRowType() != null && af.getRowType().equals(rowType)) {
        return af;
      }
    }
    return null;
  }

  public Set<ArchiveFile> getExtensions() {
    return extensions;
  }

  public File getLocation() {
    return location;
  }

  public Dataset getMetadata() throws MetadataException {
    if (metadata == null) {
      File mf = getMetadataLocationFile();
      try {
        InputStream stream;
        if (mf.exists()) {
          stream = FileUtils.getInputStream(mf);
        } else {
          // try as url
          URL url = new URL(metadataLocation);
          stream = url.openStream();
        }
        metadata = DatasetParser.build(stream);
      } catch (IOException e) {
        throw new MetadataException(e);
      } catch (RuntimeException e) {
        throw new MetadataException(e);
      }
    }
    return metadata;
  }

  public String getMetadataLocation() {
    return metadataLocation;
  }

  public File getMetadataLocationFile() {
    if (metadataLocation != null) {
      return new File(location, metadataLocation);
    }
    return null;
  }

  /**
   * Scans the archive for a semi standard support of dataset constituent metadata.
   * A dataset constituent is a subdataset which is referenced via dwc:datasetID in the data.
   * The agreement first introduced by catalogue of life for their GSDs is to have a new folder "dataset" that keeps
   * a metadata file for each constituent named just as the datasetID and suffixed with .xml.
   *
   * @return map of constituent datasetID to metadata file inside the archive
   */
  public Map<String, File> getConstituentMetadata() {
    Map<String, File> constituents = Maps.newHashMap();
    File constDir = new File(location, CONSTITUENT_DIR);
    if (constDir.exists()) {
      File[] files = constDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) { return filename.endsWith(".xml"); }
          });
      if (files != null) {
        for (File cf : files) {
          String name = cf.getName().split("\\.")[0];
          constituents.put(name, cf);
        }
      }
    }
    return constituents;
  }

  /**
   * This method is kept for legacy reason, See {@link DwcFiles} for new usage.
   *
   * @return a complete iterator using star records with all extension records that replace literal null values and
   * html entities.
   */
  @Deprecated
  public ClosableIterator<StarRecord> iterator() {
    return new ArchiveIterator(this, true, true);
  }

  /**
   * This method is kept for legacy reason, See {@link DwcFiles} for new usage.
   *
   * @return a complete iterator using star records with all extension records that are not replacing literal null
   *         values or html entities.
   */
  @Deprecated
  public ClosableIterator<StarRecord> iteratorRaw() {
    return new ArchiveIterator(this, false, false);
  }

  /**
   * @return an iterator over simple darwin core records based on the core data file(s). The DarwinCoreRecord instance
   *         is reused to give better performance, so create a clone before referencing it.
   */
  @Deprecated
  public ClosableIterator<DarwinCoreRecord> iteratorDwc() {
    return new ArchiveDwcIterator(this);
  }

  public void setCore(ArchiveFile core) {
    core.setArchive(this);
    this.core = core;
  }

  public void setExtensions(Set<ArchiveFile> extensions) {
    this.extensions = extensions;
  }

  public void setLocation(File location) {
    this.location = location;
  }

  public void setMetadataLocation(String metadataLocation) {
    this.metadataLocation = metadataLocation;
  }

  public DwcLayout getDwcLayout() {
    return dwcLayout;
  }

  public void setDwcLayout(DwcLayout dwcLayout) {
    this.dwcLayout = dwcLayout;
  }

  /**
   * Sorts all files according to file id, so that we can easily iterate over all files at once.
   */
  private void sortFiles() throws IOException {
    FileUtils futil = new FileUtils();
    // core
    try {
      futil.sort(core.getLocationFile(), ArchiveFile.getLocationFileSorted(core.getLocationFile()), core.getEncoding(),
        core.getId().getIndex(), core.getFieldsTerminatedBy(), core.getFieldsEnclosedBy(), core.getLinesTerminatedBy(),
        core.getIgnoreHeaderLines());
    } catch (IOException e) {
      LOG.error("Error sorting core file " + core.getLocationFile() + " : " + e.getMessage());
      throw e;
    } catch (RuntimeException e) {
      LOG.error("Error sorting core file " + core.getLocationFile() + " : " + e.getMessage());
      throw e;
    }
    // extensions
    for (ArchiveFile ext : extensions) {
      try {
        futil.sort(ext.getLocationFile(), ArchiveFile.getLocationFileSorted(ext.getLocationFile()), ext.getEncoding(),
          ext.getId().getIndex(), ext.getFieldsTerminatedBy(), ext.getFieldsEnclosedBy(), ext.getLinesTerminatedBy(),
          ext.getIgnoreHeaderLines());
      } catch (IOException e) {
        LOG.error("Error sorting extension file " + ext.getLocationFile() + " : " + e.getMessage());
        throw e;
      } catch (RuntimeException e) {
        LOG.error("Error sorting extension file " + ext.getLocationFile() + " : " + e.getMessage());
        throw e;
      }
    }
    sorted = true;
  }

  @Override
  public String toString() {
    String result = "";
    result += location == null ? "no archive file" : location.getAbsoluteFile();
    return result;
  }

}
