package org.gbif.dwc;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.utils.file.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class UsageExample {

  @Test
  public void testUsageExample() throws IOException {
    Path myArchiveFile = FileUtils.getClasspathFile("checklist_980.zip").toPath();
    Path extractToFolder = FileUtils.createTempDir().toPath();
    extractToFolder.toFile().deleteOnExit();

    // Open the Darwin Core Archive
    Archive dwcArchive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder);

    System.out.println("Reading archive from " + dwcArchive.getLocation().getAbsolutePath());
    System.out.println("Archive of rowtype " + dwcArchive.getCore().getRowType() + " with " + dwcArchive.getExtensions().size() + " extensions");

    // Loop over core records and display id, basis of record and scientific name
    for (Record rec : dwcArchive.getCore()) {
      System.out.printf("%s: %s (%s)%n", rec.id(), rec.value(DwcTerm.basisOfRecord), rec.value(DwcTerm.scientificName));
    }

    // Loop over star records and display id, core record data, and extension data
    for (StarRecord rec : dwcArchive) {
      System.out.printf("%s: %s%n", rec.core().id(), rec.core().value(DwcTerm.scientificName));
      if (rec.hasExtension(GbifTerm.VernacularName)) {
        for (Record extRec : rec.extension(GbifTerm.VernacularName)) {
          System.out.println(" - " + extRec.value(DwcTerm.vernacularName));
        }
      }
    }
  }
}
