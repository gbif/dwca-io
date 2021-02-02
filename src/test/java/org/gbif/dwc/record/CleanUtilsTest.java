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
package org.gbif.dwc.record;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CleanUtilsTest {

  @Test
  public void testCleanFalse() {
    for (String x : new String[]{"", " ", "  ", ".", "a ", " Me &amp; Bobby McGee"}) {
      assertEquals(x, CleanUtils.clean(x, false, false));
    }
  }

  @Test
  public void testClean() {
    assertNull(CleanUtils.clean("", true, true));
    assertNull(CleanUtils.clean(null, true, true));
    assertNull(CleanUtils.clean(" ", true, true));
    assertNull(CleanUtils.clean("\\N", true, true));
    assertNull(CleanUtils.clean("NULL", true, true));

    assertEquals(" Me & Bobby McGee", CleanUtils.clean(" Me &amp; Bobby McGee", true, true));
    assertEquals("Me & Bobby McGee", CleanUtils.clean("Me &#0038; Bobby McGee", true, true));
    assertEquals("Me & Bobby McGee", CleanUtils.clean("Me &#38; Bobby McGee", true, true));
    assertEquals("Me & Bobby McGee", CleanUtils.clean("Me &#x26; Bobby McGee", true, true));
    assertEquals("Me & Bobby McGee", CleanUtils.clean("Me &#X26; Bobby McGee", true, true));

    assertEquals("Me &amp", CleanUtils.clean("Me &amp", true, true));
    assertEquals("Me &amp ;", CleanUtils.clean("Me &amp ;", true, true));
    assertEquals("Me & amp;", CleanUtils.clean("Me & amp;", true, true));
  }
}
