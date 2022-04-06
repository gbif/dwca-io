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
package org.gbif.dwc.io;

import org.apache.commons.lang3.text.StrTokenizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StrTokenizerTest {

  @Test
  public void testCsvQuoted() {
    StrTokenizer tokenizer = new StrTokenizer();
    tokenizer.setDelimiterString(",");
    tokenizer.setQuoteChar('"');
    tokenizer.setEmptyTokenAsNull(true);
    tokenizer.setIgnoreEmptyTokens(false);

    tokenizer.reset("121,432423, 9099053,\"Frieda karla L.,DC.\",Ahrens");
    String[] columns = tokenizer.getTokenArray();
    assertEquals("121", columns[0]);
    assertEquals("432423", columns[1]);
    assertEquals(" 9099053", columns[2]);
    assertEquals("Frieda karla L.,DC.", columns[3]);
    assertEquals("Ahrens", columns[4]);

    tokenizer.reset("   ,4321");
    columns = tokenizer.getTokenArray();
    assertEquals("   ", columns[0]);
    assertEquals("4321", columns[1]);

    tokenizer.reset(" ,,,,zzz  ");
    columns = tokenizer.getTokenArray();
    assertEquals(" ", columns[0]);
    assertNull(columns[1]);
    assertNull(columns[2]);
    assertNull(columns[3]);
    assertEquals("zzz  ", columns[4]);

    tokenizer.reset(",,,,zzz  ");
    columns = tokenizer.getTokenArray();
    assertNull(columns[0]);
    assertNull(columns[1]);
    assertNull(columns[2]);
    assertNull(columns[3]);
    assertEquals("zzz  ", columns[4]);
  }

  @Test
  public void testCsvUnquoted() {
    StrTokenizer tokenizer = new StrTokenizer();
    tokenizer.setDelimiterString(",");
    tokenizer.setEmptyTokenAsNull(true);
    tokenizer.setIgnoreEmptyTokens(false);

    tokenizer.reset("121,432423, 9099053,Frieda karla L.,DC.,Ahrens");
    String[] columns = tokenizer.getTokenArray();
    assertEquals("121", columns[0]);
    assertEquals("432423", columns[1]);
    assertEquals(" 9099053", columns[2]);
    assertEquals("Frieda karla L.", columns[3]);
    assertEquals("DC.", columns[4]);
    assertEquals("Ahrens", columns[5]);

    tokenizer.reset(",,,,zzz  ");
    columns = tokenizer.getTokenArray();
    assertNull(columns[0]);
    assertNull(columns[1]);
    assertNull(columns[2]);
    assertNull(columns[3]);
    assertEquals("zzz  ", columns[4]);
  }

  @Test
  public void testPipes() {
    StrTokenizer tokenizer = new StrTokenizer();
    tokenizer.setDelimiterChar('|');
    tokenizer.setQuoteChar('"');
    tokenizer.setEmptyTokenAsNull(true);
    tokenizer.setIgnoreEmptyTokens(false);

    tokenizer.reset("121|432423| 9099053|\"Frieda karla L.|DC.\"|Ahrens");
    String[] columns = tokenizer.getTokenArray();
    assertEquals("121", columns[0]);
    assertEquals("432423", columns[1]);
    assertEquals(" 9099053", columns[2]);
    assertEquals("Frieda karla L.|DC.", columns[3]);
    assertEquals("Ahrens", columns[4]);

    tokenizer.reset("   |4321");
    columns = tokenizer.getTokenArray();
    assertEquals("   ", columns[0]);
    assertEquals("4321", columns[1]);

    tokenizer.reset(" ||||zzz  ");
    columns = tokenizer.getTokenArray();
    assertEquals(" ", columns[0]);
    assertNull(columns[1]);
    assertNull(columns[2]);
    assertNull(columns[3]);
    assertEquals("zzz  ", columns[4]);

    tokenizer.reset("||||zzz  ");
    columns = tokenizer.getTokenArray();
    assertNull(columns[0]);
    assertNull(columns[1]);
    assertNull(columns[2]);
    assertNull(columns[3]);
    assertEquals("zzz  ", columns[4]);
  }

  @Test
  public void testTabQuoted() {
    StrTokenizer tokenizer = new StrTokenizer();
    tokenizer.setDelimiterString("\t");
    tokenizer.setQuoteChar('"');
    tokenizer.setEmptyTokenAsNull(true);
    tokenizer.setIgnoreEmptyTokens(false);

    tokenizer.reset("121\t432423\t 9099053\t\"Frieda karla L.,DC.\"\tAhrens");
    String[] columns = tokenizer.getTokenArray();
    assertEquals("121", columns[0]);
    assertEquals("432423", columns[1]);
    assertEquals(" 9099053", columns[2]);
    assertEquals("Frieda karla L.,DC.", columns[3]);
    assertEquals("Ahrens", columns[4]);

    tokenizer.reset("   \t4321");
    columns = tokenizer.getTokenArray();
    assertEquals("   ", columns[0]);
    assertEquals("4321", columns[1]);

    tokenizer.reset(" \t\t\t\tzzz  ");
    columns = tokenizer.getTokenArray();
    assertEquals(" ", columns[0]);
    assertNull(columns[1]);
    assertNull(columns[2]);
    assertNull(columns[3]);
    assertEquals("zzz  ", columns[4]);

    tokenizer.reset("\t\t\t\tzzz  ");
    columns = tokenizer.getTokenArray();
    assertNull(columns[0]);
    assertNull(columns[1]);
    assertNull(columns[2]);
    assertNull(columns[3]);
    assertEquals("zzz  ", columns[4]);
  }

  @Test
  public void testTabUnquoted() {
    StrTokenizer tokenizer = new StrTokenizer();
    tokenizer.setDelimiterString("\t");
    tokenizer.setEmptyTokenAsNull(true);
    tokenizer.setIgnoreEmptyTokens(false);

    tokenizer.reset("121\t432423\t 9099053\t\"Frieda karla L.,DC.\"\tAhrens");
    String[] columns = tokenizer.getTokenArray();
    assertEquals("121", columns[0]);
    assertEquals("432423", columns[1]);
    assertEquals(" 9099053", columns[2]);
    assertEquals("\"Frieda karla L.,DC.\"", columns[3]);
    assertEquals("Ahrens", columns[4]);

    tokenizer.reset("   \t4321");
    columns = tokenizer.getTokenArray();
    assertEquals("   ", columns[0]);
    assertEquals("4321", columns[1]);

    tokenizer.reset(" \t\t\t\tzzz  ");
    columns = tokenizer.getTokenArray();
    assertEquals(" ", columns[0]);
    assertNull(columns[1]);
    assertNull(columns[2]);
    assertNull(columns[3]);
    assertEquals("zzz  ", columns[4]);

    tokenizer.reset("\t\t\t\tzzz  ");
    columns = tokenizer.getTokenArray();
    assertNull(columns[0]);
    assertNull(columns[1]);
    assertNull(columns[2]);
    assertNull(columns[3]);
    assertEquals("zzz  ", columns[4]);
  }

}
