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
