package org.gbif.tabular;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 *
 */
public class MappedTabularDataFileReaderTest {

    @Test
    public void testCsvAllwaysQuotes() throws Exception {
        File csv = FileUtils.getClasspathFile("csv_optional_quotes_excel2008CSV.csv");

        Term[] columnsMapping = new Term[]{DwcTerm.occurrenceID,
                DwcTerm.scientificName, DwcTerm.locality};


        MappedTabularDataFileReader<Term> mappedReader =
                MappedTabularFiles.newTermMappedTabularFileReader(new FileInputStream(csv), ',', true, columnsMapping);

        Map<Term, String> mappedLine = mappedReader.read();
        assertEquals("1", mappedLine.get(DwcTerm.occurrenceID));
        assertEquals("This has a, comma",  mappedLine.get(DwcTerm.locality));

        mappedLine = mappedReader.read();
        assertEquals("I say this is only a \"quote\"", mappedLine.get(DwcTerm.locality));

        mappedReader.close();
    }
}
