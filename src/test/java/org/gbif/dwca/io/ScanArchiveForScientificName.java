package org.gbif.dwca.io;

import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwca.record.StarRecord;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;

public class ScanArchiveForScientificName {

  public static void main(String[] args) throws IOException, UnsupportedArchiveException {
    // opens CSV files with headers or dwc-a directories with a meta.xml descriptor
    Archive arch = DwcFiles.fromLocation(FileUtils.getClasspathFile("archive-dwc").toPath());

    // does scientific name exist?
    if (!arch.getCore().hasTerm(DwcTerm.scientificName)) {
      System.out.println("This application requires a DWC-A with scientific names");
      System.exit(1);
    }

    // loop over core Darwin Core records
    for (StarRecord dwc : arch) {
      if (dwc.core().value(DwcTerm.scientificName).startsWith("Ambispora callosa")) {
        System.out.println(dwc.core().value(DwcTerm.scientificName));
        System.out.println(dwc.core().value(DwcTerm.scientificNameAuthorship));
        System.out.println(dwc);
      }
    }
  }

}
