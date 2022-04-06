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
