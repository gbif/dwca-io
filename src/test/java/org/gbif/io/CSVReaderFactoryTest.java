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

package org.gbif.io;

import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class CSVReaderFactoryTest {

  @Test
  public void detectCsvAlwaysQuoted() throws IOException {
    String[] files = {"csv_always_quoted.csv"};
    for (String fn : files) {
      File source = FileUtils.getClasspathFile(fn);
      CSVReader reader = CSVReaderFactory.build(source);
      assertEquals(",", reader.delimiter);
      assertEquals(new Character('"'), reader.quoteChar);
      assertEquals(1, reader.headerRows);
      reader.close();
    }
  }

  /**
   * As CSV files with rare optional quotes are hard to detect but cause problems
   * we prefer to default to the " quotation in case comma seperated files are used.
   * This is why test detectCsvUnquoted() is outcommented right now!
   */
  @Test
  public void detectCsvOptionallyQuoted() throws IOException {
    String[] files =
      {"csv_optional_quotes_puma.csv", "csv_optional_quotes_excel2008CSV.csv", "csv_incl_single_quotes.csv",
        "iucn100.csv", "csv_unquoted.txt", "csv_unquoted_coordinates.txt"};
    for (String fn : files) {
      File source = FileUtils.getClasspathFile(fn);
      CSVReader reader = CSVReaderFactory.build(source);
      assertEquals(",", reader.delimiter);
      assertEquals(new Character('"'), reader.quoteChar);
      assertEquals(1, reader.headerRows);
      reader.close();
    }
  }
  //@Test

  /**
   * We dont want unquoted CSVs, See detectCsvOptionallyQuoted()
   */
  public void detectCsvUnquoted() throws IOException {
    String[] files = {"csv_unquoted.txt"};
    for (String fn : files) {
      File source = FileUtils.getClasspathFile(fn);
      CSVReader reader = CSVReaderFactory.build(source);
      assertEquals(",", reader.delimiter);
      assertNull(reader.quoteChar);
      assertEquals(1, reader.headerRows);
      reader.close();
    }
  }

  @Test
  public void detectPipe() throws IOException {
    String[] files = new String[] {"iucn100.pipe.txt"};
    for (String fn : files) {
      File source = FileUtils.getClasspathFile(fn);
      CSVReader reader = CSVReaderFactory.build(source);
      assertEquals("|", reader.delimiter);
      assertNull(reader.quoteChar);
      assertEquals(1, reader.headerRows);
      reader.close();
    }
  }

  @Test
  public void detectSemicolon() throws IOException {
    String[] files = {"TDB_104.csv"};
    for (String fn : files) {
      File source = FileUtils.getClasspathFile(fn);
      CSVReader reader = CSVReaderFactory.build(source);
      assertEquals(";", reader.delimiter);
      assertNull(reader.quoteChar);
      assertEquals(1, reader.headerRows);
      reader.close();
    }
  }

  @Test
  public void detectTab() throws IOException {
    String[] files =
      {"tab_ipni.txt", "tab_small.txt", "iucn100.tab.txt", "issues/ebird.tab.txt", "empty_line.tab", "irmng.tail",
        "MOBOTDarwinCore.csv"};
    for (String fn : files) {
      File source = FileUtils.getClasspathFile(fn);
      CSVReader reader = CSVReaderFactory.build(source);
      assertEquals("\t", reader.delimiter);
      assertNull(reader.quoteChar);
      assertEquals(1, reader.headerRows);
      reader.close();
    }
  }

  @Test
  public void detectTabQuoted() throws IOException {
    String[] files = {"issues/eol/my_darwincore.txt", "issues/eol/my_dataobject.txt", "issues/Borza.txt"};
    for (String fn : files) {
      File source = FileUtils.getClasspathFile(fn);
      CSVReader reader = CSVReaderFactory.build(source);
      assertEquals("\t", reader.delimiter);
      assertTrue(reader.quoteChar == '"');
      assertEquals(1, reader.headerRows);
      reader.close();
    }
  }

}
