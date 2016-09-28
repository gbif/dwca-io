package org.gbif.tabular;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link TermTabularDataFileReader}
 */
public class TermTabularDataFileReaderTest {

  //simply used to avoid infinite loop
  private static int LOOP_SAFEGUARD = 1000;

  @Test
  public void testMappedTabularDataFileReaderAlwaysQuotes() throws Exception {
    File csv = FileUtils.getClasspathFile("csv_optional_quotes_excel2008CSV.csv");

    Term[] columnsMapping = new Term[]{DwcTerm.occurrenceID,
            DwcTerm.scientificName, DwcTerm.locality};

    TermTabularDataFileReader mappedReader =
            TermTabularFiles.newTermMappedTabularFileReader(new FileInputStream(csv), ',', true, columnsMapping);

    TermTabularDataLine mappedLine = mappedReader.read();
    assertEquals(1, mappedLine.getLineNumber());
    assertEquals("1", mappedLine.getMappedData().get(DwcTerm.occurrenceID));
    assertEquals("This has a, comma", mappedLine.getMappedData().get(DwcTerm.locality));


    mappedLine = mappedReader.read();
    assertEquals("I say this is only a \"quote\"", mappedLine.getMappedData().get(DwcTerm.locality));

    int recordCount = 0;
    while (mappedReader.read() != null && recordCount < LOOP_SAFEGUARD) {
      recordCount++;
    }
    assertTrue("Reader loop terminate before LOOP_SAFEGUARD", recordCount < LOOP_SAFEGUARD);

    mappedReader.close();
  }

  @Test
  public void testMappedTabularDataFileReaderException() throws Exception {
    File csv = FileUtils.getClasspathFile("csv_optional_quotes_excel2008CSV.csv");

    //only declare 2 mapping (the file includes 3 columns)
    Term[] columnsMapping = new Term[]{DwcTerm.occurrenceID, DwcTerm.scientificName};

    TermTabularDataFileReader mappedReader =
            TermTabularFiles.newTermMappedTabularFileReader(new FileInputStream(csv), ',', true, columnsMapping);

    TermTabularDataLine mappedLine = mappedReader.read();
    assertEquals(1, mappedLine.getLineNumber());
    assertEquals("1", mappedLine.getMappedData().get(DwcTerm.occurrenceID));
    assertEquals("Returned number of column matches the content of the file", 3, mappedLine.getNumberOfColumn());
    mappedReader.close();

    //declare 1 field more
    columnsMapping = new Term[]{DwcTerm.occurrenceID, DwcTerm.scientificName, DwcTerm.locality, DwcTerm.country};
    mappedReader =
            TermTabularFiles.newTermMappedTabularFileReader(new FileInputStream(csv), ',', true, columnsMapping);

    mappedLine = mappedReader.read();
    assertEquals(1, mappedLine.getLineNumber());
    assertEquals("1", mappedLine.getMappedData().get(DwcTerm.occurrenceID));
    assertEquals("Returned number of column matches the content of the file", 3, mappedLine.getNumberOfColumn());
    mappedReader.close();
  }
}
