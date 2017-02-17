package org.gbif.io;

import java.io.*;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class TabWriter implements AutoCloseable {

  private static final Pattern escapeChars = Pattern.compile("[\t\n\r]");
  private int records = 0;
  private Writer writer;

  public static TabWriter fromFile(File file) throws FileNotFoundException {
    return new TabWriter(new FileOutputStream(file));
  }

  public TabWriter(OutputStream stream) {
    try {
      writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF8"));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  public TabWriter(Writer writer) {
    this.writer = writer;
  }

  public void write(String[] row) throws IOException {
    if (row == null || row.length == 0) {
      return;
    }
    String rowString = tabRow(row);
    if (rowString != null) {
      writer.write(rowString);
    }
  }

  private String tabRow(String[] columns) {
    // escape \t \n \r chars !!!
    boolean empty = true;
    for (int i = 0; i < columns.length; i++) {
      if (columns[i] != null) {
        empty = false;
        columns[i] = StringUtils.trimToNull(escapeChars.matcher(columns[i]).replaceAll(" "));
      }
    }
    if (empty) {
      // dont create a row at all!
      return null;
    }
    return StringUtils.join(columns, '\t') + "\n";
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }
}
