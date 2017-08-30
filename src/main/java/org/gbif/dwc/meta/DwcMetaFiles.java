package org.gbif.dwc.meta;

import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.UnsupportedArchiveException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.input.BOMInputStream;
import org.xml.sax.SAXException;

/**
 * Collections of static methods to work with Metadata files (e.g. meta.xml, eml.xml) in
 * the context of Darwin Core (archive) files.
 */
public class DwcMetaFiles {

  private static final SAXParserFactory SAX_FACTORY = SAXParserFactory.newInstance();
  static {
    SAX_FACTORY.setNamespaceAware(true);
    SAX_FACTORY.setValidating(false);
  }

  //common filenames used for the metadata file
  private static final List<String> POSSIBLE_METADATA_FILE =
          Collections.unmodifiableList(Arrays.asList("eml.xml", "metadata.xml"));

  private DwcMetaFiles(){}

  /**
   * Read the provided meta descriptor (e.g. meta.xml) and return a {@link Archive}.
   * @param metaDescriptor
   * @throws SAXException
   * @throws IOException
   * @throws UnsupportedArchiveException
   */
  public static Archive fromMetaDescriptor(InputStream metaDescriptor) throws SAXException, IOException, UnsupportedArchiveException {
    Archive archive = new Archive();
    try (BOMInputStream bomInputStream = new BOMInputStream(metaDescriptor)) {
      SAXParser p = SAX_FACTORY.newSAXParser();
      MetaXMLSaxHandler mh = new MetaXMLSaxHandler(archive);
      p.parse(bomInputStream, mh);
    } catch (ParserConfigurationException e) {
      throw new SAXException(e);
    }
    return archive;
  }

  /**
   * Try to discover a metadata file inside a DarwinCore folder. Usually, the metadata file is a EML file.
   * @param dwcFolder
   * @return
   */
  public static Optional<String> discoverMetadataFile(Path dwcFolder) {
    // search for popular metadata filenames
    for (String metadataFN : POSSIBLE_METADATA_FILE) {
      File emlFile = new File(dwcFolder.toFile(), metadataFN);
      if (emlFile.exists()) {
        return Optional.of(metadataFN);
      }
    }
    return Optional.empty();
  }
}
