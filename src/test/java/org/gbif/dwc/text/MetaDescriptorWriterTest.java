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

package org.gbif.dwc.text;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.utils.file.FileUtils;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MetaDescriptorWriterTest {

  @Test
  public void testRoundtrip() {
    try {
      // read archive
      Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("archive-dwc"));
      assertNotNull(arch);
      assertNotNull(arch.getCore());
      assertTrue(arch.getCore().getId().getIndex() == 0);
      assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
      assertEquals(1, arch.getExtensions().size());
      assertEquals("\t", arch.getCore().getFieldsTerminatedBy());

      // write meta.xml
      File temp = File.createTempFile("meta", ".xml");
      temp.deleteOnExit();
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      MetaDescriptorWriter.writeMetaFile(temp, arch);

      Archive arch2 = ArchiveFactory.openArchive(temp);
      // core props
      ArchiveFile core = arch2.getCore();
      assertNotNull(core);
      assertNotNull(core.getId());
      assertTrue(core.hasTerm(DwcTerm.scientificName));
      assertEquals("DarwinCore.txt", core.getLocation());
      assertEquals("\t", core.getFieldsTerminatedBy());
      for (ArchiveField f : arch.getCore().getFields().values()) {
        assertTrue(core.hasTerm(f.getTerm().qualifiedName()));
        assertEquals(core.getField(f.getTerm().qualifiedName()).getIndex(), f.getIndex());
      }

      // extensions props
      assertEquals(1, arch2.getExtensions().size());
      ArchiveFile ext = arch2.getExtensions().iterator().next();
      assertEquals("VernacularName.txt", ext.getLocation());
      assertEquals(2, ext.getFields().size());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testRoundtripQuotes() {
    try {
      // read archive
      Archive arch = ArchiveFactory.openArchive(FileUtils.getClasspathFile("meta_quot.xml"));
      assertNotNull(arch);
      assertNotNull(arch.getCore());
      assertNotNull(arch.getCore().getId());
      assertTrue(arch.getCore().hasTerm(DwcTerm.scientificName));
      assertEquals(1, arch.getExtensions().size());

      // write meta.xml
      File temp = File.createTempFile("meta", ".xml");
      temp.deleteOnExit();
      //      System.out.println("Writing temporary test meta.xml file to " + temp.getAbsolutePath());
      MetaDescriptorWriter.writeMetaFile(temp, arch);

      Archive arch2 = ArchiveFactory.openArchive(temp);
      // core props
      ArchiveFile core = arch2.getCore();
      assertNotNull(core);
      assertNotNull(core.getId());
      assertTrue(core.hasTerm(DwcTerm.scientificName));
      assertEquals("test", core.getLocation());
      for (ArchiveField f : arch.getCore().getFields().values()) {
        assertTrue(core.hasTerm(f.getTerm().qualifiedName()));
        assertEquals(core.getField(f.getTerm().qualifiedName()).getIndex(), f.getIndex());
      }

      // extensions props
      assertEquals(1, arch2.getExtensions().size());
      ArchiveFile ext = arch2.getExtensions().iterator().next();
      assertEquals("test2", ext.getLocation());
      assertEquals(2, ext.getFields().size());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
