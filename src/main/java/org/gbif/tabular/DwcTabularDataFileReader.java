package org.gbif.tabular;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.utils.file.tabular.TabularDataFileReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * Warning: this class will probably be removed shortly
 *
 * Specialized TermTabularDataFileReader for DarwinCore archive file.
 * In addition of reading the underlying csv file this class will handle id field and default values that can
 * be provided in DarwinCore.
 */
public class DwcTabularDataFileReader extends TermTabularDataFileReader {

  //refers to a column in the archive representing the id of the line that is not mapped to a real term
  public static Term DEFAULT_ID_TERM = TermFactory.instance().findTerm("dwcaId");

  private Map<Term, String> defaultValues = Maps.newHashMap();

  public DwcTabularDataFileReader(TabularDataFileReader<List<String>> tabularDataFileReader, ArchiveField idField,
                                  List<ArchiveField> fields) {
    super(tabularDataFileReader, buildColumnMapping(idField, fields));

    for(ArchiveField af : fields) {
      if(!Strings.isNullOrEmpty(af.getDefaultValue())){
        defaultValues.put(af.getTerm(), af.getDefaultValue());
      }
    }
  }

  private static Term[] buildColumnMapping(ArchiveField idField, List<ArchiveField> dwcTerms){
    Map<Integer, Term> columnMapping = Maps.newTreeMap();
    columnMapping.put(idField.getIndex(), idField.getTerm() != null ? idField.getTerm() : DEFAULT_ID_TERM);
    for(ArchiveField af : dwcTerms) {
      if(af.getIndex() != null && af.getIndex() >= 0){
        columnMapping.put(af.getIndex(), af.getTerm());
      }
    }
    return columnMapping.values().toArray(new Term[0]);
  }

  @Override
  public TermTabularDataLine read() throws IOException {
    TermTabularDataLine line = super.read();

    if(line == null){
      return null;
    }

    if(defaultValues.isEmpty()){
      return line;
    }

    Map<Term, String> mappedData = Maps.newHashMap(line.getMappedData());
    int numberOfColumn = line.getNumberOfColumn();

    for(Term term : defaultValues.keySet()) {
      if(mappedData.containsKey(term)){
        if(Strings.isNullOrEmpty(mappedData.get(term))){
          mappedData.put(term, defaultValues.get(term));
        }
      }
      else {
        mappedData.put(term, defaultValues.get(term));
        numberOfColumn++;
      }
    }

    return new TermTabularDataLine(line.getLineNumber(), mappedData, numberOfColumn);
  }

}
