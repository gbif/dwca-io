package org.gbif.dwca.io;

public class UnsupportedArchiveException extends RuntimeException {

  public UnsupportedArchiveException(Exception e) {
    super(e);
  }

  public UnsupportedArchiveException(String message) {
    super(message);
  }

  public UnsupportedArchiveException(String message, Exception e) {
    super(message, e);
  }
}
