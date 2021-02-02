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

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DwcaStreamWriterTest {

  @Test
  public void writeEmpty() throws Exception {
    File dwca = FileUtils.createTempDir();
    DwcaStreamWriter dwcaWriter = null;
    try {
      dwcaWriter = new DwcaStreamWriter(dwca, DwcTerm.Taxon, DwcTerm.taxonID, true);
    } finally {
      assertNotNull(dwcaWriter);
      assertThrows(IllegalStateException.class, dwcaWriter::close);
      org.apache.commons.io.FileUtils.deleteQuietly(dwca);
    }
  }

  @Test
  public void write() throws Exception {
    File dwca = FileUtils.createTempDir();
    Map<Term, Integer> mapping = ImmutableMap.of(
        DwcTerm.taxonID, 0,
        DwcTerm.scientificName, 1,
        DwcTerm.taxonRank, 2);
    try (DwcaStreamWriter dwcaWriter = new DwcaStreamWriter(dwca, DwcTerm.Taxon, DwcTerm.taxonID, true)){
      String m = "<eml/>";
      dwcaWriter.setMetadata(m, "eml.xml");
      dwcaWriter.write(DwcTerm.Taxon, 0, mapping, ImmutableList.<String[]>builder()
          .add(new String[] { "tax-1", "Abies Mill.", "genus" })
          .add(new String[] { "tax-2", "Abies alba Mill.", "species" })
          .add(new String[] { "tax-3", "Piceae abies L.", "species" })
          .add(new String[] { "tax-4", "Piceae abies subsp. helvetica L.", "subspecies" })
          .build()
      );

      Archive arch = DwcFiles.fromLocation(dwca.toPath());
      assertEquals("eml.xml", arch.getMetadataLocation());
      assertEquals("<eml/>", arch.getMetadata());

    } finally {
      org.apache.commons.io.FileUtils.deleteQuietly(dwca);
    }
  }
}
