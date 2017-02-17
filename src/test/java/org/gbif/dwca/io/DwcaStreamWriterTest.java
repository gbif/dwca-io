package org.gbif.dwca.io;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FilenameUtils;
import org.gbif.api.model.registry.Dataset;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.*;

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
            Dataset d = new Dataset();
            d.setTitle("Abies of the Alps");
            d.setDescription("Abies of the Alps excl Switzerland.");
            dwcaWriter.setMetadata(d);
            dwcaWriter.write(DwcTerm.Taxon, 0, mapping, ImmutableList.<String[]>builder()
                    .add(new String[] { "tax-1", "Abies Mill.", "genus" })
                    .add(new String[] { "tax-2", "Abies alba Mill.", "species" })
                    .add(new String[] { "tax-3", "Piceae abies L.", "species" })
                    .add(new String[] { "tax-4", "Piceae abies subsp. helvetica L.", "subspecies" })
                    .build()
            );

        } finally {
            org.apache.commons.io.FileUtils.deleteQuietly(dwca);
        }
    }

}