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
package org.gbif.dwc;

import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StarIteratorTest {

  @Test
  public void testIterator() throws IOException, UnsupportedArchiveException {
    // test proper archive
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("archive-dwc").toPath());
    assertNotNull(arch.getCore());
    assertEquals(2, arch.getExtensions().size());
    int found = 0;
    int extensionRecords = 0;
    for (StarRecord rec : arch) {
      //System.out.println(rec.core().id() + " --> " + rec.size());
      // count all extension records
      extensionRecords += rec.size();

      if (rec.core().id().equals("544382")) {
        found++;
        assertEquals("Tursiops truncatus truncatus Montagu", rec.core().value(DwcTerm.scientificName));
        // test extension iter
        int i = 0;
        for (Record er : rec) {
          i++;
        }
        assertEquals(27, i);
      } else if (rec.core().id().equals("105833")) {
        found++;
//        105833  Chinese river dolphin English
//        105833  Chinese lake dolphin  English
//        105833  Pei c’hi	Chinese
//        105833  White flag dolphin  English
        int i = 0;
        for (Record er : rec) {
          i++;
          if ("Chinese river dolphin".equals(er.value(DwcTerm.vernacularName))) {
            assertEquals("English", er.value(DcTerm.language));
            assertEquals("English", er.value(DcTerm.language));
          } else if ("Chinese lake dolphin".equals(er.value(DwcTerm.vernacularName))) {
            assertEquals("English", er.value(DcTerm.language));
          } else if ("Pei c’hi".equals(er.value(DwcTerm.vernacularName))) {
            assertEquals("Chinese", er.value(DcTerm.language));
          } else {
            assertEquals("White flag dolphin", er.value(DwcTerm.vernacularName));
            assertEquals("English", er.value(DcTerm.language));
          }
        }
        System.out.println(rec);
        assertEquals(4, i);
      } else if (rec.core().id().equals("105838")) {
        found++;
//      105838  Delfin de La Plata  Spanish
//      105838  Franciscana Spanish
//      105838  Franciscano Portuguese
//      105838  La Plata dolphin  English
//      105838  Tonina  Spanish
//      105838  Toninha Portuguese
        int i = 0;
        for (Record er : rec) {
          i++;
          if ("Delfin de La Plata".equals(er.value(DwcTerm.vernacularName))) {
            assertEquals("Spanish", er.value(DcTerm.language));
          } else if ("Franciscana".equals(er.value(DwcTerm.vernacularName))) {
            assertEquals("Spanish", er.value(DcTerm.language));
          } else if ("Franciscano".equals(er.value(DwcTerm.vernacularName))) {
            assertEquals("Portuguese", er.value(DcTerm.language));
          } else if ("La Plata dolphin".equals(er.value(DwcTerm.vernacularName))) {
            assertEquals("English", er.value(DcTerm.language));
          } else if ("Tonina".equals(er.value(DwcTerm.vernacularName))) {
            assertEquals("Spanish", er.value(DcTerm.language));
          } else {
            assertEquals("Toninha", er.value(DwcTerm.vernacularName));
            assertEquals("Portuguese", er.value(DcTerm.language));
          }
        }
        assertEquals(6, i);
      }

    }
    assertEquals(3, found);
    assertEquals(1057, extensionRecords);
  }
}
