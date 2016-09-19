package org.gbif.tabular;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link MappedTabularDataFileReader}
 */
public class MappedTabularDataFileReaderTest {

  //simply used to avoid infinite loop
  private static int LOOP_SAFEGUARD = 1000;

  @Test
  public void testMappedTabularDataFileReaderAllwaysQuotes() throws Exception {
    File csv = FileUtils.getClasspathFile("csv_optional_quotes_excel2008CSV.csv");

    Term[] columnsMapping = new Term[]{DwcTerm.occurrenceID,
            DwcTerm.scientificName, DwcTerm.locality};

    MappedTabularDataFileReader<Term> mappedReader =
            MappedTabularFiles.newTermMappedTabularFileReader(new FileInputStream(csv), ',', true, columnsMapping);

    Map<Term, String> mappedLine = mappedReader.read();
    assertEquals("1", mappedLine.get(DwcTerm.occurrenceID));
    assertEquals("This has a, comma", mappedLine.get(DwcTerm.locality));

    mappedLine = mappedReader.read();
    assertEquals("I say this is only a \"quote\"", mappedLine.get(DwcTerm.locality));

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

    MappedTabularDataFileReader<Term> mappedReader =
            MappedTabularFiles.newTermMappedTabularFileReader(new FileInputStream(csv), ',', true, columnsMapping);

    boolean exceptionTriggered = false;
    try {
      mappedReader.read();
    } catch (TabularLineSizeException tlsEx) {
      exceptionTriggered = true;
    }
    assertTrue("TabularLineSizeException is triggered", exceptionTriggered);
    mappedReader.close();
  }
}
