/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.dwc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class TabWriter implements AutoCloseable {

  private static final Pattern escapeChars = Pattern.compile("[\t\n\r]");
  private final Writer writer;

  public static TabWriter fromFile(File file) throws FileNotFoundException {
    return new TabWriter(new FileOutputStream(file));
  }

  public TabWriter(OutputStream stream) {
    writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
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
