/*
 * Copyright 2021 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.dwc;

import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.ClosableIterator;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.InputStreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

  private DwcLayout dwcLayout;

  // Tracks whether the archive is sorted so star records can be iterated.
  private boolean normalizedAndSorted = false;

  public void addExtension(ArchiveFile extension) {
    extension.setArchive(this);
    extensions.add(extension);
  }

  /**
   * Get the archive core.
   * @return The core ArchiveFile, or {@code null} if there isn't one.
   */
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
        metadata = new InputStreamUtils().readEntireStream(stream, FileUtils.UTF8);
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
    Map<String, File> constituents = new HashMap<>();
    File constDir = new File(location, CONSTITUENT_DIR);
    if (constDir.exists()) {
      File[] files = constDir.listFiles((File dir, String filename) -> filename.endsWith(".xml"));
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
   * Perform basic structural validation of the Darwin Core Archive: that the meta.xml exists,
   * is valid XML, and necessary keys are defined.
   *
   * @throws UnsupportedArchiveException
   */
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

  /**
   * Performs necessary preparation for iteration over StarRecords.  If the Archive has extensions, all the data files
   * must be sorted by the identifier column to allow iteration.  This can take significant processing time.
   */
  public void initialize() throws IOException {
    if (normalizedAndSorted) return;

    Objects.requireNonNull(getCore(), "The archive shall have a core");

    // If no extensions are provided we don't need to sort the file
    if (getExtensions().isEmpty()) {
      LOG.debug("Archive has no extensions, so initialization is unnecessary.");
      normalizedAndSorted = true;
      return;
    }

    LOG.info("Initializing Darwin Core Archive for iteration. This can take some minutes on large archives.");

    // Otherwise, we need to sort core + extensions
    getCore().normalizeAndSort();
    for (ArchiveFile archiveFile : getExtensions()) {
      archiveFile.normalizeAndSort();
    }

    normalizedAndSorted = true;
  }

  /**
   * @return a complete iterator using star records with all extension records that replaces literal null values and
   * HTML entities.
   * <p>
   * Replaces common, literal NULL values with real nulls, e.g. "\N" or "NULL", and replaces HTML & XML entities in
   * record values with the interpreted value.
   * <p>
   * Archives with extensions, where the core id contains extravagant Unicode characters, may not be handled correctly.
   * (This will not affect you, since you don't have ancient scripts, emoji or mathematical symbols in your core ids,
   * but it is documented for completeness. Further detail in {@link FileUtils#sort(File, File, String, int, String, Character, String, int)}.)
   */
  @Override
  public ClosableIterator<StarRecord> iterator() {
    return iterator(true, true);
  }

  /**
   * @return a complete iterator using star records with all extension records that may replace literal null values and
   * HTML entities.
   * <p>
   * Archives with extensions, where the core id contains extravagant Unicode characters, may not be handled correctly.
   * (This will not affect you, since you don't have ancient scripts, emoji or mathematical symbols in your core ids,
   * but it is documented for completeness. Further detail in {@link FileUtils#sort(File, File, String, int, String, Character, String, int)}.)
   *
   * @param replaceNulls if true replaces common, literal NULL values with real nulls, e.g. "\N" or "NULL"
   * @param replaceEntities if true HTML & XML entities in record values will be replaced with the interpreted value.
   */
  public ClosableIterator<StarRecord> iterator(boolean replaceNulls, boolean replaceEntities) {
    try {
      initialize();

      if (getExtensions().isEmpty()) {
        return new StarRecordIterator(
            getCore().iterator(replaceNulls, replaceEntities),
            null
        );
      } else {
        return new StarRecordIterator(
            getCore().sortedIterator(replaceNulls, replaceEntities),
            getExtensionIterators(replaceNulls, replaceEntities)
        );
      }
    } catch (Exception e) {
      throw new UnsupportedArchiveException(e);
    }
  }

  /**
   * Build an iterator (pointing to the sorted tabular file) for each extension of the {@link Archive}.
   *
   * @param replaceNulls
   * @param replaceEntities
   *
   * @return a map of iterators
   *
   * @throws IOException
   */
  private Map<Term, ClosableIterator<Record>> getExtensionIterators(boolean replaceNulls, boolean replaceEntities) throws IOException {
    Map<Term, ClosableIterator<Record>> extensionIterators = new HashMap<>();
    for (ArchiveFile ext : getExtensions()) {
      extensionIterators.put(ext.getRowType(), ext.sortedIterator(replaceNulls, replaceEntities));
    }
    return extensionIterators;
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

  @Override
  public String toString() {
    String result = "";
    result += location == null ? "no archive file" : location.getAbsoluteFile();
    return result;
  }
}
