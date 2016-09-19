package org.gbif.tabular;

/**
 * Exception used when a line of a tabular file doesn't match the declared size (number of cells).
 */
public class TabularLineSizeException extends RuntimeException {

  public TabularLineSizeException(String message){
    super(message);
  }

}
