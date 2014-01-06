package org.gbif.dwc.text;

import org.gbif.dwc.record.DarwinCoreRecord;
import org.gbif.dwc.terms.DwcTerm;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ScanArchiveForScientificName {

  public static void main(String[] args) throws IOException, UnsupportedArchiveException {
    // opens csv files with headers or dwc-a direcotries with a meta.xml descriptor
    Archive arch = ArchiveFactory
      .openArchive(new File("/Volumes/Scratch/ecat-data-col/checklists/beac2c55-d889-4358-a414-b1db79ba3536/dwc-a"));

    // does scientific name exist?
    if (!arch.getCore().hasTerm(DwcTerm.scientificName)) {
      System.out.println("This application requires dwc-a with scientific names");
      System.exit(1);
    }

    // loop over core darwin core records
    Iterator<DarwinCoreRecord> iter = arch.iteratorDwc();
    DarwinCoreRecord dwc;
    while (iter.hasNext()) {
      dwc = iter.next();
      if (dwc.getScientificName().startsWith("Ambispora callosa")) {
        System.out.println(dwc.getScientificName());
        System.out.println(dwc.getScientificNameAuthorship());
        System.out.println(dwc);
      }
    }
  }

}
