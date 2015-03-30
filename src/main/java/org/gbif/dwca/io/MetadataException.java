package org.gbif.dwca.io;

/**
 * Exception indicating invalid or non existing metadata.
 */
public class MetadataException extends Exception {

  public MetadataException(Exception e) {
    super(e);
  }

  public MetadataException(String message) {
    super(message);
  }

  public MetadataException(String message, Exception e) {
    super(message, e);
  }
}
