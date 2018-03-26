package org.gbif.dwc;

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.Archive;
import org.gbif.dwc.UnsupportedArchiveException;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwca.io.ArchiveFactory;

import java.io.File;
import java.io.IOException;

public class UsageExample {

  public static void main(String[] args) throws IOException, UnsupportedArchiveException {
    // opens csv files with headers or dwc-a direcotries with a meta.xml descriptor
    Archive arch = ArchiveFactory.openArchive(new File(args[0]));

    System.out.println("Reading archive from "+arch.getLocation().getAbsolutePath());
    System.out.println("Archive of rowtype "+arch.getCore().getRowType()+" with "+arch.getExtensions().size()+" extensions");
    // loop over star records. i.e. core with all linked extension records
    for (StarRecord rec : arch) {
      // print core ID + scientific name
      System.out.println(rec.core().id()
        + " sciname:" + rec.core().value(DwcTerm.scientificName)
        + " bor:" + rec.core().value(DwcTerm.basisOfRecord)
      );
      // print out all rowTypes
      for (Record erec : rec) {
        // print out extension rowtype
        System.out.println(erec.rowType() + ", id="+erec.value(DcTerm.identifier));
      }
    }
  }

}
