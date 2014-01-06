package org.gbif.dwc.text;

public class UnkownDelimitersException extends UnsupportedArchiveException {

  public UnkownDelimitersException(Exception e) {
    super(e);
  }

  public UnkownDelimitersException(String message) {
    super(message);
  }

  public UnkownDelimitersException(String message, Exception e) {
    super(message, e);
  }
}
