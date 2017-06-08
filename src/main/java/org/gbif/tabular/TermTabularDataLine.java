package org.gbif.tabular;

import org.gbif.dwc.terms.Term;

import java.util.List;
import java.util.Map;

/**
 * Warning: this class will probably be removed shortly
 *
 * Container class for a line in a tabular data files.
 * This class is immutable but the provided Map is not guarantee to be immutable.
 *
 */
public class TermTabularDataLine {

  private final long lineNumber;
  private final Map<Term, String> mappedData;
  private final List<String> unmappedData;
  private final int numberOfColumn;

  /**
   *
   * @param lineNumber the number of this line in the source file
   * @param mappedData content of the line mapped to a {@link Term}
   * @param numberOfColumn number of column used by the line (can potentially be different from the mapped data keys)
   */
  public TermTabularDataLine(long lineNumber, Map<Term, String> mappedData, int numberOfColumn) {
    this(lineNumber, mappedData, numberOfColumn, null);
  }

  /**
   *
   * @param lineNumber the number of this line in the source file
   * @param mappedData content of the line mapped to a {@link Term}
   * @param numberOfColumn number of column used by the line (can potentially be different from the mapped data keys)
   * @param unmappedData if the line contained more data than declared, this list contains it.
   */
  public TermTabularDataLine(long lineNumber, Map<Term, String> mappedData, int numberOfColumn, List<String> unmappedData) {
    this.lineNumber = lineNumber;
    this.mappedData = mappedData;
    this.numberOfColumn = numberOfColumn;
    this.unmappedData = unmappedData;
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

  public List<String> getUnmappedData() {
    return unmappedData;
  }
}
