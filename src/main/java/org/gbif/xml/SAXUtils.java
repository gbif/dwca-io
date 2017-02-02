package org.gbif.xml;

import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility classes related for SAX based parser to parse XML documents
 */
public class SAXUtils {

  private static final Logger LOG = LoggerFactory.getLogger(SAXUtils.class);

  public static SAXParserFactory getNsAwareSaxParserFactory() {
    SAXParserFactory saxf = null;
    try {
      saxf = SAXParserFactory.newInstance();
      saxf.setValidating(false);
      saxf.setNamespaceAware(true);
    } catch (Exception e) {
      LOG.error("Can't create namespace aware SAX Parser Factory: " + e.getMessage(), e);
    }
    return saxf;
  }
}
