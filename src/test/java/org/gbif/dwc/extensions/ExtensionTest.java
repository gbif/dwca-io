package org.gbif.dwc.extensions;

import org.gbif.dwc.terms.DwcTerm;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests related to {@link Extension}
 */
public class ExtensionTest {

  @Test
  public void testCompareTo() throws MalformedURLException {
    Extension a = new Extension(new URL("http://perdu.com"), false);
    a.setRowType(DwcTerm.Occurrence);
    Extension b = new Extension(new URL("https://gbif.org"), false);
    b.setRowType(DwcTerm.Taxon);
    assertTrue(a.compareTo(b) < 0);
  }
}
