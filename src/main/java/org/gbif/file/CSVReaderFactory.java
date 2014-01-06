/***************************************************************************
 * Copyright 2010 Global Biodiversity Information Facility Secretariat
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***************************************************************************/

package org.gbif.file;

import org.gbif.dwc.text.ArchiveFile;
import org.gbif.dwc.text.UnkownCharsetException;
import org.gbif.dwc.text.UnkownDelimitersException;
import org.gbif.utils.file.CharsetDetection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVReaderFactory {

  private static final Logger LOG = LoggerFactory.getLogger(CSVReaderFactory.class);
  private static final int ROWS_TO_INSPECT = 10;

  public static CSVReader build(ArchiveFile source) throws IOException {
    return new CSVReader(source.getLocationFile(), source.getEncoding(), source.getFieldsTerminatedBy(),
      source.getFieldsEnclosedBy(), source.getIgnoreHeaderLines());
  }

  public static CSVReader build(File source) throws IOException {
    String encoding = detectEncoding(source);
    ArchiveFile af = buildArchiveFile(source, encoding);
    return build(af);
  }

  public static CSVReader build(File source, String delimiter, boolean header) throws IOException {
    return new CSVReader(source, detectEncoding(source), delimiter, null, header ? 1 : 0);
  }

  public static CSVReader build(File source, String encoding, String delimiter, Character quotes, Integer headerRows)
    throws IOException {
    return new CSVReader(source, encoding, delimiter, quotes, headerRows);
  }

  public static CSVReader build(File source, String encoding, String delimiter, Integer headerRows) throws IOException {
    return new CSVReader(source, encoding, delimiter, '"', headerRows);
  }

  public static CSVReader build(InputStream stream, String encoding, String delimiter, Character quotes,
    Integer headerRows) throws IOException {
    return new CSVReader(stream, encoding, delimiter, quotes, headerRows);
  }

  private static ArchiveFile buildArchiveFile(File source, String encoding) throws UnkownDelimitersException {
    ArchiveFile dwcFile = new ArchiveFile();
    // add file
    dwcFile.addLocation(source.getAbsolutePath());

    // detect character encoding
    dwcFile.setEncoding(encoding);

    // set header row to 1
    dwcFile.setIgnoreHeaderLines(1);

    // try csv, tab and then other popular delimiters
    // keep number of resulting columns for comparisons
    int maxColumns = 0;

    // CSV
    String[] potentialDelimiters = {",", "\t", ";", "|"};
    for (String delim : potentialDelimiters) {
      // test with various quotes including a dynamic one if the first char in each field is consistently the same
      List<Character> potentialQuotes = new ArrayList<Character>();

      CSVReader reader;
      try {
        reader = CSVReader.build(source, encoding, delim, null, 1);
        Character firstChar = likelyQuoteChar(reader);
        reader.close();
        if (firstChar != null) {
          potentialQuotes.add(firstChar);
        }
      } catch (IOException ignored) {
      }
      // prefer quotes for CSVs
      if (delim.equals(",")) {
        potentialQuotes.add('"');
        potentialQuotes.add('\'');
        potentialQuotes.add(null);
      } else {
        potentialQuotes.add(null);
        potentialQuotes.add('"');
        potentialQuotes.add('\'');
      }

      for (Character quote : potentialQuotes) {
        try {
          reader = CSVReader.build(source, encoding, delim, quote, 0);
          int x = consistentRowSize(reader);
          //          System.out.println("Delim >>>"+delim+"<<<  Quote >>>"+quote+"<<<   rowSize="+x);
          if (x > maxColumns) {
            dwcFile.setFieldsTerminatedBy(delim);
            dwcFile.setFieldsEnclosedBy(quote);
            maxColumns = x;
          }
          reader.close();
        } catch (IOException ignored) {
          // swallow, maybe different delimiters work
          // if all fail we will throw an exception at the end
        }
      }
    }

    if (maxColumns < 1) {
      throw new UnkownDelimitersException("Unable to detect field delimiter");
    }

    String msg = "Detected field delimiter >>>" + dwcFile.getFieldsTerminatedBy() + "<<<";
    if (dwcFile.getFieldsEnclosedBy() != null) {
      msg += " and quoted by >>>" + dwcFile.getFieldsEnclosedBy() + "<<<";
    }
    LOG.debug(msg);

    return dwcFile;
  }

  /**
   * @return the number of consistent columns, -1 if non consistent or column numbers-2 in case the column numbers only
   *         differ by 1 at max.
   */
  private static int consistentRowSize(CSVReader reader) {
    int rowNum = 0;
    int columns = 0;
    boolean plusMinusOne = false;
    while (reader.hasNext() && rowNum < ROWS_TO_INSPECT) {
      String[] row = reader.next();
      if (rowNum == 0) {
        columns = row.length;
      }
      if (Math.abs(columns - row.length) > 1) {
        return -1;
      }
      if (columns != row.length) {
        plusMinusOne = true;
      }
      rowNum++;
    }
    if (plusMinusOne) {
      return columns - 2;
    }
    return columns;
  }

  private static String detectEncoding(File source) throws UnkownCharsetException {
    Charset encoding;
    try {
      encoding = CharsetDetection.detectEncoding(source, 16384);
      if (encoding == null) {
        throw new UnkownCharsetException("Unable to detect the files character encoding");
      }
    } catch (IOException e) {
      throw new UnkownCharsetException(e);
    }
    return encoding.displayName();
  }

  /**
   * Checks if all non empty/null fields start with the same character.
   *
   * @return the first character if consistent, otherwise null
   */
  private static Character likelyQuoteChar(CSVReader reader) {
    Character quote = null;
    int line = 0;
    while (reader.hasNext() && line < 10) {
      line++;
      String[] row = reader.next();
      if (row != null) {
        for (String col : row) {
          if (col != null && col.length() > 0) {
            // same char at start & end?
            if (col.length() > 1 && col.charAt(0) == col.charAt(col.length() - 1)) {
              // only consider non alphanumerics
              char potQuote = col.charAt(0);
              if (Character.isLetterOrDigit(potQuote)) {
                break;
              }
              if (quote == null) {
                quote = potQuote;
              } else {
                if (!quote.equals(potQuote)) {
                  quote = null;
                  break;
                }
              }
            }
          }
        }
      }
    }
    return quote;
  }

}
