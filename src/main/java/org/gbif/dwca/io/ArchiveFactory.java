/*
 * Copyright 2010-2015 Global Biodiversity Informatics Facility.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gbif.dwca.io;

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwca.io.ArchiveField.DataType;
import org.gbif.util.DownloadUtil;
import org.gbif.utils.file.CompressionUtil;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory used to build {@link Archive} object from a DarwinCore Archive file.
 * 
 * @author mdoering
 *
 */
public class ArchiveFactory {

  private static final TermFactory TERM_FACTORY = TermFactory.instance();

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveFactory.class);
  private static final List<String> DATA_FILE_SUFFICES = ImmutableList.of(".csv", ".txt", ".tsv", ".tab", ".text", ".data", ".dwca");

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


  private static final SAXParserFactory SAX_FACTORY = SAXParserFactory.newInstance();

  static {
    SAX_FACTORY.setNamespaceAware(true);
    SAX_FACTORY.setValidating(false);
  }

  /**
   * Opens an archive from a URL, downloading and decompressing it.
   *
   * @param archiveUrl the location of a compressed archive or single data file
   * @param workingDir writable directory to download to and decompress archive
   */
  public static Archive openArchive(URL archiveUrl, File workingDir) throws IOException, UnsupportedArchiveException {
    File downloadTo = new File(workingDir, "dwca-download");
    File dwca = new File(workingDir, "dwca");
    DownloadUtil.download(archiveUrl, downloadTo);
    return openArchive(downloadTo, dwca);
  }

  /**
   * Opens an archive from a local file and decompresses or copies it into the given archive directory.
   * Make sure the archive directory does not contain files already, any existing files will be removed!
   *
   * If the source archive is an uncompressed, single data file and a valid archive, it will be copied as is
   * to the archiveDir.
   *
   * @param archiveFile the location of a compressed archive or single data file
   * @param archiveDir  empty, writable directory used to keep decompress archive in
   */
  public static Archive openArchive(File archiveFile, File archiveDir) throws IOException, UnsupportedArchiveException {
    if (archiveDir.exists()) {
      // clean up any existing folder
      LOG.debug("Deleting existing archive folder [{}]", archiveDir.getAbsolutePath());
      org.gbif.utils.file.FileUtils.deleteDirectoryRecursively(archiveDir);
    }
    FileUtils.forceMkdir(archiveDir);
    // try to decompress archive
    try {
      CompressionUtil.decompressFile(archiveDir, archiveFile, true);
      // we keep subfolder, but often the entire archive is within one subfolder. Remove that root folder if present
      File[] rootFiles = archiveDir.listFiles((FileFilter) HiddenFileFilter.VISIBLE);
      if (rootFiles.length == 1) {
        File root = rootFiles[0];
        if (root.isDirectory()) {
          // single root dir, flatten structure
          LOG.debug("Removing single root folder {} found in decompressed archive", root.getAbsoluteFile());
          for (File f : FileUtils.listFiles(root, TrueFileFilter.TRUE, null)) {
            File f2 = new File(archiveDir, f.getName());
            f.renameTo(f2);
          }
        }
      }
      // continue to read archive from the tmp dir
      return openArchive(archiveDir);

    } catch (CompressionUtil.UnsupportedCompressionType e) {
      LOG.debug("Could not uncompress archive [{}], try to read as single text file", archiveFile, e);
      // If its a text file only we will get this exception - but also for corrupt compressions
      // try to open as text file only and if successful copy file to archive dir
      Archive arch = openArchiveDataFile(archiveFile);
      Files.copy(archiveFile, new File(archiveDir, archiveFile.getName()));
      return arch;
    }
  }

  /**
   * Opens a dwca archive which is just a single decompressed data file with headers, e.g. a csv or tab delimited file
   */
  public static Archive openArchiveDataFile(File dataFile) throws IOException, UnsupportedArchiveException {
    Archive archive = new Archive();
    archive.setLocation(dataFile);

    ArchiveFile coreFile = readFileHeaders(dataFile);
    archive.setCore(coreFile);

    // check if we also have a metadata file next to this data file
    discoverMetadataFile(archive, dataFile.getParentFile());

    // final validation
    return validateArchive(archive);
  }

  /**
   * @param dwcaFolder the location of an expanded dwc archive directory or just a single dwc text file
   */
  public static Archive openArchive(File dwcaFolder) throws IOException, UnsupportedArchiveException {
    if (!dwcaFolder.exists()) {
      throw new FileNotFoundException("Archive folder not existing: " + dwcaFolder.getAbsolutePath());
    }
    // delegate to open data file method if its a single file, not a folder
    if (dwcaFolder.isFile()) {
      return openArchiveDataFile(dwcaFolder);
    }

    Archive archive = new Archive();
    archive.setLocation(dwcaFolder);

    // Accommodate archives coming from legacy IPTs which put a "\" before each filename
    // http://dev.gbif.org/issues/browse/POR-2396
    // https://code.google.com/p/gbif-providertoolkit/issues/detail?id=1015
    Iterator<File> iter = FileUtils.iterateFiles(dwcaFolder, new String[] {"xml", "txt"}, false);
    while (iter.hasNext()) {
      File f = iter.next();
      if (f.getName().startsWith("\\")) {
        String orig = f.getName();
        String replacement = f.getName().replaceFirst("\\\\", "");
        LOG.info("Renaming file from {} to {}", orig, replacement);
        f.renameTo(new File(dwcaFolder, replacement));
      }
    }

    // read metadata
    File mf = new File(dwcaFolder, Archive.META_FN);
    if (mf.exists()) {
      // read metafile
      readMetaDescriptor(archive, new FileInputStream(mf));

    } else {
      // meta.xml lacking.
      // Try to detect data files ourselves as best as we can.
      // look for a single, visible text data file
      List<File> dataFiles = new ArrayList<File>();
      for (String suffix : DATA_FILE_SUFFICES) {
        FileFilter ff = FileFilterUtils.and(
            FileFilterUtils.suffixFileFilter(suffix, IOCase.INSENSITIVE), HiddenFileFilter.VISIBLE
        );
        dataFiles.addAll(Arrays.asList(dwcaFolder.listFiles(ff)));
      }

      if (dataFiles.size() == 1) {
        File dataFile = new File(dwcaFolder, dataFiles.get(0).getName());
        ArchiveFile coreFile = readFileHeaders(dataFile);
        coreFile.getLocations().clear();
        coreFile.addLocation(dataFile.getName());
        archive.setCore(coreFile);

      } else {
        throw new UnsupportedArchiveException(
          "The archive given is a folder with more or less than 1 data files having a csv, txt or tab suffix");
      }
    }

    // check if we also have a metadata file next to this data file
    discoverMetadataFile(archive, mf.getParentFile());

    // final validation
    return validateArchive(archive);
  }


  private static void discoverMetadataFile(Archive archive, File folder) {
    if (archive.getMetadataLocation() == null) {
      // search for popular metadata filenames
      for (String metadataFN : Lists.newArrayList("eml.xml", "metadata.xml")) {
        File emlFile = new File(folder, metadataFN);
        if (emlFile.exists()) {
          archive.setMetadataLocation(metadataFN);
          break;
        }
      }
    }
  }

  private static ArchiveFile readFileHeaders(File dataFile) throws UnsupportedArchiveException, IOException {
    ArchiveFile dwcFile = new ArchiveFile();
    dwcFile.addLocation(null);
    dwcFile.setIgnoreHeaderLines(1);

    String[] headers;
    try (CSVReader reader = CSVReaderFactory.build(dataFile)) {
      // copy found delimiters & encoding
      dwcFile.setEncoding(reader.encoding);
      dwcFile.setFieldsTerminatedBy(reader.delimiter);
      dwcFile.setFieldsEnclosedBy(reader.quoteChar);
      headers = reader.getHeader();
    }

    // detect dwc terms as good as we can based on header row
    int index = 0;
    for (String head : headers) {
      // there are never any quotes in term names - remove them just in case the csvreader didnt recognize them
      if (head != null && head.length() > 1) {
        try {
          Term dt = TERM_FACTORY.findTerm(head);
          ArchiveField field = new ArchiveField(index, dt, null, DataType.string);
          dwcFile.addField(field);
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

    determineRowType(headerAsTerm).ifPresent(
            t -> dwcFile.setRowType(t)
    );
    return dwcFile;
  }

  @VisibleForTesting
  protected static void readMetaDescriptor(Archive archive, InputStream metaDescriptor) throws UnsupportedArchiveException {

    try {
      SAXParser p = SAX_FACTORY.newSAXParser();
      MetaXMLSaxHandler mh = new MetaXMLSaxHandler(archive);
      LOG.debug("Reading archive metadata file");
      p.parse(new BOMInputStream(metaDescriptor), mh);
    } catch (Exception e1) {
      LOG.warn("Exception caught", e1);
      throw new UnsupportedArchiveException(e1);
    }
  }

  private static Archive validateArchive(Archive archive) throws UnsupportedArchiveException {
    validateCoreFile(archive.getCore(), !archive.getExtensions().isEmpty());
    for (ArchiveFile af : archive.getExtensions()) {
      validateExtensionFile(af);
    }
    // report basic stats
    LOG.debug("Archive contains " + archive.getExtensions().size() + " described extension files");
    LOG.debug("Archive contains " + archive.getCore().getFields().size() + " core properties");
    return archive;
  }

  private static void validateCoreFile(ArchiveFile f, boolean hasExtensions) throws UnsupportedArchiveException {
    if (hasExtensions) {
      if (f.getId() == null) {
        LOG.warn(
          "DwC-A core data file " + f.getTitle() + " is lacking an id column. No extensions allowed in this case");
      }
    }
    validateFile(f);
  }

  private static void validateExtensionFile(ArchiveFile f) throws UnsupportedArchiveException {
    if (f.getId() == null) {
      throw new UnsupportedArchiveException(
        "DwC-A data file " + f.getTitle() + " requires an id or foreign key to the core id");
    }
    validateFile(f);
  }

  private static void validateFile(ArchiveFile f) throws UnsupportedArchiveException {
    if (f == null) {
      throw new UnsupportedArchiveException("DwC-A data file is NULL");
    }
    if (f.getLocationFile() == null) {
      throw new UnsupportedArchiveException("DwC-A data file " + f.getTitle() + " requires a location");
    }
    if (f.getEncoding() == null) {
      throw new UnsupportedArchiveException("DwC-A data file " + f.getTitle() + " requires a character encoding");
    }
  }

  /**
   * Tries to determine the rowType of a file based on its headers.
   *
   * @param headers
   *
   * @return
   */
  static Optional<Term> determineRowType(List<Term> headers) {
    return TERM_TO_ROW_TYPE.entrySet().stream()
            .filter(ke -> headers.contains(ke.getKey()))
            .map(Map.Entry::getValue).findFirst();
  }

  /**
   * Tries to determine the record identifier of a file based on its headers.
   *
   * @param headers the list can contain null value when a column is used but the Term is undefined
   *
   * @return
   */
  static Optional<Term> determineRecordIdentifier(List<Term> headers) {
    //try to find the first matching term respecting the order defined by ID_TERMS
    return ID_TERMS.stream()
            .filter(t -> headers.contains(t))
            .findFirst();
  }

}
