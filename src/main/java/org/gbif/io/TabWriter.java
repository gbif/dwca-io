package org.gbif.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class TabWriter {

  private static final Pattern escapeChars = Pattern.compile("[\t\n\r]");
  private int records = 0;
  private Writer writer;

  public TabWriter(OutputStream stream) {
    try {
      writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF8"));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
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

  public void close() throws IOException {
    writer.close();
  }
}
