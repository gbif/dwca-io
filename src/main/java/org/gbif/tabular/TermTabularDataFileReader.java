package org.gbif.tabular;

import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.tabular.TabularDataFileReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Warning: this class will probably be removed shortly
 *
 * Decorator of {@link TabularDataFileReader} to map elements of a line to a TermTabularDataLine.
 * Supports the reading of lines smaller or larger than the declared set of columns.
 *
 */
public class TermTabularDataFileReader implements TabularDataFileReader<TermTabularDataLine> {

  private final TabularDataFileReader<List<String>> tabularDataFileReader;
  private final Term[] columnMapping;
  private int lineNumber = 0;

  /**
   *
   * @param tabularDataFileReader a TabularDataFileReader implementation to read lines as List of String
   * @param columnMapping mapping of the columns. Index of the array matched the index of the element in the data line
   */
  public TermTabularDataFileReader(TabularDataFileReader<List<String>> tabularDataFileReader, Term[] columnMapping){
    this.tabularDataFileReader = tabularDataFileReader;
    this.columnMapping = columnMapping;
  }

  @Override
  public List<String> getHeaderLine() throws IOException {
    return tabularDataFileReader.getHeaderLine();
  }

  /**
   *
   * @return
   * @throws IOException
   */
  public TermTabularDataLine read() throws IOException {
    List<String> tabularLine = tabularDataFileReader.read();

    // check for end of file
    if(tabularLine == null){
      return null;
    }
    int numOfColumns = Math.min(columnMapping.length, tabularLine.size());
    Map<Term, String> line = Maps.newHashMapWithExpectedSize(numOfColumns);
    for (int i = 0; i < numOfColumns; i++) {
      line.put(columnMapping[i], tabularLine.get(i));
    }
    lineNumber++;

    //if the line contains more data than the number of declared column, set it as unamppedData
    if(tabularLine.size() > columnMapping.length) {
      return new TermTabularDataLine(lineNumber, line, tabularLine.size() ,
              tabularLine.subList(columnMapping.length, tabularLine.size()));
    }
    return new TermTabularDataLine(lineNumber, line, tabularLine.size());
  }

  @Override
  public long getLastRecordLineNumber() {
    return tabularDataFileReader.getLastRecordLineNumber();
  }

  @Override
  public long getLastRecordNumber() {
    return tabularDataFileReader.getLastRecordNumber();
  }

  @Override
  public void close() throws IOException {
    tabularDataFileReader.close();
  }

}
