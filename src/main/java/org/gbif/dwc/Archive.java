package org.gbif.dwc;

import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.ClosableIterator;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import org.gbif.utils.file.InputStreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Darwin Core Archive and its components (metadata, core, extensions).
 *
 * @see <a href="http://tdwg.github.io/dwc/terms/guides/text/">Darwin Core Text Guide</a>
 */
public class Archive implements Iterable<StarRecord> {
  public static final String CONSTITUENT_DIR = "dataset";
  public static final String META_FN = "meta.xml";

  private static final Logger LOG = LoggerFactory.getLogger(Archive.class);

  private String metadataLocation;
  private String metadata;
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

  public String getMetadata() throws MetadataException {
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
        metadata = new InputStreamUtils().readEntireStream(stream, "UTF-8");
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
   * Scans the archive for a semi-standard support of dataset constituent metadata.
   * A dataset constituent is a sub-dataset which is referenced via dwc:datasetID in the data.
   * The agreement first introduced by Catalogue of Life for their GSDs is to have a new folder "dataset" that keeps
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

  public void validate() throws UnsupportedArchiveException {
    if (core == null) {
      throw new UnsupportedArchiveException("Parts of DwC-A are missing");
    }
    core.validateAsCore(!extensions.isEmpty());
    LOG.trace("Core is valid");
    for (ArchiveFile af : extensions) {
      af.validateAsExtension();
      LOG.trace("Extension {} is valid", af);
    }
    // report basic stats
    LOG.debug("Archive contains {} described extension files", extensions.size());
    LOG.debug("Archive contains {} core properties", core.getFields().size());
  }

  // TODO: tidy up.
  private NormalizedDwcArchive ndwca;

  /**
   * This method is kept for legacy reason, See {@link DwcFiles} for new usage.
   *
   * @return a complete iterator using star records with all extension records that replace literal null values and
   * html entities.
   */
  public ClosableIterator<StarRecord> iterator() {
    try {
      if (ndwca == null) {
        ndwca = DwcFiles.prepareArchive(this);
      }
      return ndwca.iterator();
    } catch (Exception e) {}
    return null;
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
