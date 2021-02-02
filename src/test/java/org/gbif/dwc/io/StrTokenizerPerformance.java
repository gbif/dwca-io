/*
 * Copyright 2021 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.dwc.io;

import org.gbif.utils.file.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.text.StrTokenizer;
import org.junit.jupiter.api.Test;

public class StrTokenizerPerformance {

  private long test(StrTokenizer tokenizer, File source) throws IOException {
    FileInputStream fis = new FileInputStream(source);
    InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8);
    BufferedReader br = new BufferedReader(reader);

    // keep track of time while iterating
    long start = System.currentTimeMillis();
    String row = br.readLine();
    while (row != null) {
      tokenizer.reset(row);
      String[] columns = tokenizer.getTokenArray();
      row = br.readLine();
    }
    long dur = System.currentTimeMillis() - start;
    br.close();
    return dur;
  }

  @Test
  public void testCharVsStringPerformance() throws IOException {
    File source = FileUtils.getClasspathFile("irmng.tail");

    // test CHAR
    StrTokenizer tokenizer = new StrTokenizer();
    tokenizer.setDelimiterChar('\t');
    tokenizer.setEmptyTokenAsNull(true);
    tokenizer.setIgnoreEmptyTokens(false);
    long time = test(tokenizer, source);
    System.out.println(time + " milliseconds for CHAR based tokenizer.");

    // test STRING
    tokenizer = new StrTokenizer();
    tokenizer.setDelimiterString("\t");
    tokenizer.setEmptyTokenAsNull(true);
    time = test(tokenizer, source);
    System.out.println(time + " milliseconds for STRING based tokenizer.");
  }
}
