package org.gbif.dwca.tools;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.UnsupportedArchiveException;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.StarRecord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArchiveScanner {

  public static void main(String[] args) throws IOException, UnsupportedArchiveException {
    if (args.length < 1) {
      System.out.println(
        "Please specify a Darwin Core Archive folder or single Darwin Core CSV file as the first argument.\nAn optional second integer argument lets you specify the number of records to view, a third the offset to skip.");
      System.exit(0);
    }
    File archiveFile = new File(args[0]);
    if (!archiveFile.exists()) {
      System.out.println("Cannot find archive file: " + archiveFile);
      System.exit(0);
    }
    int maxNumbers = 0;
    if (args.length > 1) {
      try {
        maxNumbers = Integer.parseInt(args[1]);
      } catch (NumberFormatException ignored) {
        System.out.println("Second argument should an integer defining maxNumbers");
      }
    }
    int offset = 0;
    if (args.length > 2) {
      try {
        offset = Integer.parseInt(args[2]);
      } catch (NumberFormatException ignored) {
        System.out.println("Third offset argument is no integer");
      }
    }

    System.out.println("Opening archive: " + archiveFile.getAbsolutePath());
    Archive arch = ArchiveFactory.openArchive(archiveFile);

    if (arch.getCore() == null) {
      System.out.println("Cannot locate the core data file");
      System.exit(0);
    }
    System.out.println("Core file(s) found: " + arch.getCore().getLocations());
    System.out.println("Core row type: " + arch.getCore().getRowType());
    System.out.println("Core identifier column: " + arch.getCore().getId().getIndex());

    List<DwcTerm> terms = new ArrayList<DwcTerm>();
    terms.add(DwcTerm.scientificName);
    terms.add(DwcTerm.taxonRank);
    terms.add(DwcTerm.parentNameUsageID);
    terms.add(DwcTerm.acceptedNameUsageID);
    terms.add(DwcTerm.taxonomicStatus);
    terms.add(DwcTerm.nomenclaturalStatus);
    terms.add(DwcTerm.kingdom);
    terms.add(DwcTerm.family);
    for (DwcTerm t : terms) {
      if (!arch.getCore().hasTerm(t)) {
        System.out.println("Cannot locate term " + t);
      }
    }

    System.out.println("Number of extensions " + arch.getExtensions().size());

    // count records
    int i = 0;
    for (StarRecord rec : arch) {
      i++;
    }
    System.out.println("Archive contains " + i + " core records.");

    // show some records
    i = 0;
    if (maxNumbers > 0) {
      for (StarRecord rec : arch) {
        i++;
        if (i > offset + maxNumbers) {
          break;
        }
        if (offset < i) {
          System.out.println("record " + i + ":  " + rec);
          for (Record erec : rec) {
            System.out.println("  : " + erec);
          }
        }
      }
    }
  }

}
