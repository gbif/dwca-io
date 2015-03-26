package org.gbif.dwca.io;

import org.gbif.dwc.terms.Term;

public class ArchiveField {

  public enum DataType {
    string("xs:string"), bool("xs:boolean"), integer("xs:integer"), decimal("xs:decimal"), date("xs:dateTime"),
    uri("xs:URI");

    private final String xsdType;

    DataType(String xsd) {
      xsdType = xsd;
    }

    public static DataType findByExtensionEnumTypeName(String type) {
      if (type.equalsIgnoreCase("boolean")) {
        return bool;
      }
      for (DataType dt : DataType.values()) {
        if (dt.name().equalsIgnoreCase(type)) {
          return dt;
        }
      }
      return null;
    }

    public static DataType findByXmlSchemaType(String type) {
      for (DataType dt : DataType.values()) {
        if (dt.xsdType.equalsIgnoreCase(type)) {
          return dt;
        }
      }
      return null;
    }
  }

  private Integer index;
  private Term term;
  private String defaultValue;
  private String delimitedBy;

  private DataType type = DataType.string;

  public ArchiveField() {

  }

  public ArchiveField(Integer index, Term term, String defaultValue, DataType type) {
    this.index = index;
    this.term = term;
    this.defaultValue = defaultValue;
    this.type = type;
  }

  public ArchiveField(Integer index, Term term, String defaultValue, DataType type, String delimitedBy) {
    this.index = index;
    this.term = term;
    this.defaultValue = defaultValue;
    this.type = type;
    this.delimitedBy = delimitedBy;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public Integer getIndex() {
    return index;
  }

  public Term getTerm() {
    return term;
  }

  public DataType getType() {
    return type;
  }

  public String getDelimitedBy() {
    return delimitedBy;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public void setTerm(Term term) {
    this.term = term;
  }

  public void setType(DataType type) {
    this.type = type;
  }

  public void setDelimitedBy(String delimitedBy) {
    this.delimitedBy = delimitedBy;
  }

}
