package org.gbif.dwc.text;

public class UnkownCharsetException extends UnsupportedArchiveException {

  public UnkownCharsetException(Exception e) {
    super(e);
  }

  public UnkownCharsetException(String message) {
    super(message);
  }

  public UnkownCharsetException(String message, Exception e) {
    super(message, e);
  }
}
