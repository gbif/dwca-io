/***************************************************************************
 * Copyright 2010 Global Biodiversity Information Facility Secretariat
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***************************************************************************/

package org.gbif.io;

import org.gbif.dwca.io.ArchiveFile;
import org.gbif.utils.file.ClosableReportingIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVReader implements ClosableReportingIterator<String[]>, Iterable<String[]> {

  private static final Logger LOG = LoggerFactory.getLogger(CSVReader.class);
  public final int headerRows;
  public final String encoding;
  public final String delimiter;
  public final Character quoteChar;
  public final String[] header;
  private final StrTokenizer tokenizer;
  private String row;
  private int rows;
  private int readRows;
  private final Map<Integer, String> emptyLines;
  private final BufferedReader br;
  private boolean rowError;
  private String errorMessage;
  private Exception exception;

  public CSVReader(File source, String encoding, String delimiter, Character quotes, Integer headerRows)
    throws IOException {
    this(new FileInputStream(source), encoding, delimiter, quotes, headerRows);
  }

  public CSVReader(InputStream stream, String encoding, String delimiter, Character quotes, Integer headerRows)
    throws IOException {
    Cache<Integer, String> cache = CacheBuilder.newBuilder().maximumSize(1000).build();
    this.emptyLines = cache.asMap();
    this.rows = 0;
    this.readRows = 0;
    this.delimiter = delimiter;
    this.encoding = encoding;
    this.quoteChar = quotes;
    this.headerRows = headerRows == null || headerRows < 0 ? 0 : headerRows;
    tokenizer = new StrTokenizer();
    tokenizer.setDelimiterString(delimiter);
    if (quotes != null) {
      tokenizer.setQuoteChar(quotes);
    }
    tokenizer.setIgnoreEmptyTokens(false);
    tokenizer.reset();
    InputStreamReader reader = new InputStreamReader(stream, encoding);
    br = new BufferedReader(reader);
    row = br.readLine();
    // parse header row
    if (row == null) {
      header = null;
    } else {
      tokenizer.reset(row);
      header = tokenizer.getTokenArray();
    }
    // skip initial header rows?
    while (headerRows > 0) {
      headerRows--;
      row = br.readLine();
    }
  }

  public static CSVReader build(ArchiveFile source) throws IOException {
    return CSVReaderFactory.build(source);
  }

  public static CSVReader build(File source) throws IOException {
    return CSVReaderFactory.build(source);
  }

  public static CSVReader build(File source, String delimiter, boolean header) throws IOException {
    return CSVReaderFactory.build(source, delimiter, header);
  }

  public static CSVReader build(File source, String encoding, String delimiter, Character quotes, int headerRows)
    throws IOException {
    return CSVReaderFactory.build(source, encoding, delimiter, quotes, headerRows);
  }

  public static CSVReader build(File source, String encoding, String delimiter, int headerRows) throws IOException {
    return CSVReaderFactory.build(source, encoding, delimiter, headerRows);
  }

  public void close() {
    try {
      br.close();
    } catch (IOException e) {
      LOG.debug("Exception caught", e);
    }
  }

  /**
   * @return the current line number of the String[] iterator
   */
  public int currLineNumber() {
    return rows;
  }

  /**
   * @return a set of the line numbers of the firsts empty rows found in the file
   */
  public Set<Integer> getEmptyLines() {
    return emptyLines.keySet();
  }

  /**
   * @return the number of rows of data that were correctly read from the file
   */
  public int getReadRows() {
    return readRows;
  }

  /*
   * (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return row != null;
  }

  public ClosableReportingIterator<String[]> iterator() {
    return this;
  }

  /*
   * (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public String[] next() {
    if (row == null) {
      return null;
    }
    tokenizer.reset(row);
    resetReportingIterator();
    try {
      row = br.readLine();
      rows++;
      // skip empty lines
      while (row != null && row.length() == 0) {
        // save line number of empty line
        emptyLines.put(rows + headerRows + 1, "");
        row = br.readLine();
        rows++;
      }
      readRows++;
    } catch (IOException e) {
      LOG.debug("Exception caught", e);
      rowError = true;
      exception = e;

      // construct error message showing exception and problem row
      StringBuilder msg = new StringBuilder();
      msg.append("Exception caught: ");
      msg.append(e.getMessage());
      if (!Strings.isNullOrEmpty(row)) {
        msg.append("\n");
        msg.append("Row: ");
        msg.append(row);
      }
      errorMessage = msg.toString();

      // ensure iteration terminates
      row = null;
    }
    return tokenizer.getTokenArray();
  }

  /**
   * Reset all reporting parameters.
   */
  private void resetReportingIterator() {
    rowError = false;
    exception = null;
    errorMessage = null;
  }

  public void remove() {
    throw new UnsupportedOperationException("Remove not supported");
  }

  public boolean hasRowError() {
    return rowError;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Exception getException() {
    return exception;
  }
}
