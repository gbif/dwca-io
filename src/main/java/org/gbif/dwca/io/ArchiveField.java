/*
 * Copyright 2010-2015 Global Biodiversity Informatics Facility.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gbif.dwca.io;

import javax.validation.constraints.NotNull;

import org.gbif.dwc.terms.Term;

/**
 * Container object representing a metafile content &lt;field&gt; tag as defined in the Darwin Core Text Guide.
 *
 * @see <a href="http://rs.tdwg.org/dwc/terms/guides/text/#fieldTag">Darwin Core Text Guide - Field element</a>
 *
 * @author mdoering
 * @author cgendreau
 *
 */
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
  private String vocabulary;

  private DataType type = DataType.string;

  /**
   * Mainly available for bean support. Other constructors should be preferred over this one.
   */
  public ArchiveField() {
  }

  public ArchiveField(Integer index, @NotNull Term term) {
    this(index, term, null, null, null, null);
  }

  public ArchiveField(@NotNull Term term, String defaultValue) {
    this(null, term, defaultValue, null, null, null);
  }

  public ArchiveField(Integer index, @NotNull Term term, DataType type) {
    this(index, term, null, type, null, null);
  }

  public ArchiveField(Integer index, @NotNull Term term, String defaultValue, DataType type) {
    this(index, term, defaultValue, type, null, null);
  }

  public ArchiveField(Integer index, @NotNull Term term, String defaultValue, DataType type, String delimitedBy) {
    this(index, term, defaultValue, type, delimitedBy, null);
  }

  /**
   *
   * Complete constructor to build a ArchiveField object.
   *
   * @param index Specifies the position of the column in the row. Starts at 0. Optional for field with defaultValue.
   * @param term A Unified Resource Identifier (URI) for the term represented by this field. Required by DarwinCore specification.
   * @param defaultValue Specifies value to use if one is not supplied for the field in a given row. (Optional)
   * @param type
   * @param delimitedBy separator used to handle multiple values. (Optional)
   * @param vocabulary A Unified Resource Identifier (URI) for a vocabulary that the source values for this field are based on. (Optional)
   */
  public ArchiveField(Integer index, @NotNull Term term, String defaultValue, DataType type, String delimitedBy, String vocabulary) {
    this.index = index;
    this.term = term;
    this.defaultValue = defaultValue;
    this.type = type;
    this.delimitedBy = delimitedBy;
    this.vocabulary = vocabulary;
  }

  /**
   * @see <a href="http://rs.tdwg.org/dwc/terms/guides/text/#fieldTag">Darwin Core Text Guide</a>
   * @return
   */
  public String getDefaultValue() {
    return defaultValue;
  }

  /**
   * @see <a href="http://rs.tdwg.org/dwc/terms/guides/text/#fieldTag">Darwin Core Text Guide</a>
   * @return
   */
  public Integer getIndex() {
    return index;
  }

  /**
   * @see <a href="http://rs.tdwg.org/dwc/terms/guides/text/#fieldTag">Darwin Core Text Guide</a>
   * @return
   */
  public Term getTerm() {
    return term;
  }

  public DataType getType() {
    return type;
  }

  public String getDelimitedBy() {
    return delimitedBy;
  }

  /**
   * @see <a href="http://rs.tdwg.org/dwc/terms/guides/text/#fieldTag">Darwin Core Text Guide</a>
   * @return
   */
  public String getVocabulary() {
    return vocabulary;
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

  public void setVocabulary(String vocabulary) {
    this.vocabulary = vocabulary;
  }

}
