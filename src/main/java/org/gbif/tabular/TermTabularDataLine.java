package org.gbif.tabular;

import org.gbif.dwc.terms.Term;

import java.util.Map;

/**
 * Container class for a line in a tabular data files.
 * This class is immutable but the provided Map is not guarantee to be immutable.
 *
 */
public class TermTabularDataLine {

  private final long lineNumber;
  private final Map<Term, String> mappedData;
  private final int numberOfColumn;

  /**
   *
   * @param lineNumber the number of this line in the source file
   * @param mappedData content of the line mapped to instance of T
   * @param numberOfColumn number of column used by the line (can potentially be different from the mapped data keys)
   */
  public TermTabularDataLine(long lineNumber, Map<Term, String> mappedData, int numberOfColumn) {
    this.lineNumber = lineNumber;
    this.mappedData = mappedData;
    this.numberOfColumn = numberOfColumn;
  }

  public Map<Term, String> getMappedData() {
    return mappedData;
  }

  public long getLineNumber() {
    return lineNumber;
  }

  public int getNumberOfColumn() {
    return numberOfColumn;
  }
}
