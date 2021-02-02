package org.gbif.dwc;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

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
