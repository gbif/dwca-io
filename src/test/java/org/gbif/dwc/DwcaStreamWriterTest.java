package org.gbif.dwc;

import org.gbif.dwc.Archive;
import org.gbif.dwc.DwcaStreamWriter;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by markus on 17/02/2017.
 */
public class DwcaStreamWriterTest {

  @Test(expected = IllegalStateException.class)
  public void writeEmpty() throws Exception {
    File dwca = FileUtils.createTempDir();
    try (DwcaStreamWriter dwcaWriter = new DwcaStreamWriter(dwca, DwcTerm.Taxon, DwcTerm.taxonID, true)){

    } finally {
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
      dwcaWriter.addMetadata(m, "eml.xml");
      dwcaWriter.write(DwcTerm.Taxon, 0, mapping, ImmutableList.<String[]>builder()
          .add(new String[] { "tax-1", "Abies Mill.", "genus" })
          .add(new String[] { "tax-2", "Abies alba Mill.", "species" })
          .add(new String[] { "tax-3", "Piceae abies L.", "species" })
          .add(new String[] { "tax-4", "Piceae abies subsp. helvetica L.", "subspecies" })
          .build()
      );

      Archive arch = ArchiveFactory.openArchive(dwca);
      assertEquals("eml.xml", arch.getMetadataLocation());
      assertEquals("<eml/>", arch.getMetadata());

    } finally {
      org.apache.commons.io.FileUtils.deleteQuietly(dwca);
    }
  }

}
