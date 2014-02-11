package org.gbif.dwc.record;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.DwcTerm;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarwinCoreTaxon {

  private static final Logger LOG = LoggerFactory.getLogger(DarwinCoreTaxon.class);
  private static final String CLASSIFICATION_DELIMITER = ";";
  private static final Pattern NORM_AUTHORS = Pattern.compile("[^a-z0-9]+");
  private String taxonID;
  private String taxonConceptID;
  private String datasetID;
  private String datasetName;
  private String source;
  private String modified;
  private String accessrights;
  private String rights;
  private String rightsholder;
  private String language;
  private String higherClassification;
  private String kingdom;
  private String phylum;
  private String classs;
  private String order;
  private String family;
  private String genus;
  private String subgenus;
  private String genericName;
  private String specificEpithet;
  private String infraspecificEpithet;
  private String scientificName;
  private String scientificNameID;
  private String vernacularName;
  private String taxonRank;
  private String verbatimTaxonRank;
  private String infraspecificMarker;
  private String scientificNameAuthorship;
  private String nomenclaturalCode;
  private String namePublishedIn;
  private String namePublishedInID;
  private String taxonomicStatus;
  private String nomenclaturalStatus;
  private String nameAccordingTo;
  private String nameAccordingToID;
  private String parentNameUsageID;
  private String parentNameUsage;
  private String originalNameUsageID;
  private String originalNameUsage;
  private String acceptedNameUsageID;
  private String acceptedNameUsage;
  private String taxonRemarks;
  private String dynamicProperties;
  private String namePublishedInYear;

  public DarwinCoreTaxon() {
  }

  public DarwinCoreTaxon(String scientificName) {
    this.scientificName = norm(scientificName);
  }

  public String buildHigherClassification(Character lowestRank) {
    StringBuilder buf = new StringBuilder();
    if (kingdom != null) {
      buf.append(StringUtils.capitalize(kingdom.toLowerCase()));
      buf.append(CLASSIFICATION_DELIMITER);
    }
    if (lowestRank == null || lowestRank != 'k') {
      if (phylum != null) {
        buf.append(StringUtils.capitalize(phylum.toLowerCase()));
        buf.append(CLASSIFICATION_DELIMITER);
      }
      if (lowestRank == null || lowestRank != 'p') {
        if (classs != null) {
          buf.append(StringUtils.capitalize(classs.toLowerCase()));
          buf.append(CLASSIFICATION_DELIMITER);
        }
        if (lowestRank == null || lowestRank != 'c') {
          if (order != null) {
            buf.append(StringUtils.capitalize(order.toLowerCase()));
            buf.append(CLASSIFICATION_DELIMITER);
          }
          if (lowestRank == null || lowestRank != 'o') {
            if (family != null) {
              buf.append(StringUtils.capitalize(family.toLowerCase()));
              buf.append(CLASSIFICATION_DELIMITER);
            }
            if (lowestRank == null || lowestRank != 'f') {
              if (genus != null) {
                buf.append(StringUtils.capitalize(genus.toLowerCase()));
                buf.append(CLASSIFICATION_DELIMITER);
              }
              if (lowestRank == null || lowestRank != 'g') {
                if (subgenus != null) {
                  buf.append(StringUtils.capitalize(subgenus.toLowerCase()));
                  buf.append(CLASSIFICATION_DELIMITER);
                }
              }
            }
          }
        }
      }
    }

    if (buf.length() > 1) {
      buf.deleteCharAt(buf.length() - 1);
    }
    return buf.toString();
  }

  public void clear() {
    source = null;
    modified = null;
    accessrights = null;
    rights = null;
    rightsholder = null;
    language = null;
    datasetID = null;
    datasetName = null;
    for (DwcTerm t : DwcTerm.TAXONOMIC_TERMS) {
      setProperty(t, null);
    }
  }

  public String getAcceptedNameUsage() {
    return acceptedNameUsage;
  }

  public String getAcceptedNameUsageID() {
    return acceptedNameUsageID;
  }

  public String getAccessrights() {
    return accessrights;
  }

  public String getClasss() {
    return classs;
  }

  public String getDatasetID() {
    return datasetID;
  }

  public String getFamily() {
    return family;
  }

  public String getFullScientificName() {
    if (scientificNameAuthorship != null && scientificName != null) {
      String normedSciName = NORM_AUTHORS.matcher(scientificName.toLowerCase()).replaceAll(" ");
      String normedAuthors = NORM_AUTHORS.matcher(scientificNameAuthorship.toLowerCase()).replaceAll(" ");
      if (!normedSciName.contains(normedAuthors)) {
        return scientificName + " " + scientificNameAuthorship;
      }
    }
    if (scientificName == null) {
      String sciname = null;
      if (genericName != null || this.genus != null) {
        String genus = genericName != null ? genericName : this.genus;
        if (specificEpithet != null) {
          sciname = genus + " " + specificEpithet;
          if (infraspecificEpithet != null) {
            if (infraspecificMarker != null) {
              sciname += " " + infraspecificMarker;
            }
            sciname += " " + infraspecificEpithet;
          }
          // potentially add authorship in this case
          if (sciname != null && scientificNameAuthorship != null) {
            sciname += " " + scientificNameAuthorship;
          }
        } else {
          sciname = subgenus != null ? subgenus : genus;
        }
      }
      return sciname;
    }
    return scientificName;
  }

  public String getGenus() {
    return genus;
  }

  public String getGenericName() {
    return genericName;
  }

  public String getHigherClassification() {
    return higherClassification;
  }

  public String getInfraspecificEpithet() {
    return infraspecificEpithet;
  }

  public String getInfraspecificMarker() {
    return infraspecificMarker;
  }

  public String getKingdom() {
    return kingdom;
  }

  public String getLanguage() {
    return language;
  }

  public String getLowestScientificName() {
    String sciname = getFullScientificName();
    if (sciname == null) {
      if (family != null) {
        sciname = family;
      } else if (order != null) {
        sciname = order;
      } else if (classs != null) {
        sciname = classs;
      } else if (phylum != null) {
        sciname = phylum;
      } else if (kingdom != null) {
        sciname = kingdom;
      }
    }
    return sciname;
  }

  public String getModified() {
    return modified;
  }

  public String getNameAccordingTo() {
    return nameAccordingTo;
  }

  public String getNameAccordingToID() {
    return nameAccordingToID;
  }

  public String getNamePublishedIn() {
    return namePublishedIn;
  }

  public String getNamePublishedInID() {
    return namePublishedInID;
  }

  public String getNomenclaturalCode() {
    return nomenclaturalCode;
  }

  public String getNomenclaturalStatus() {
    return nomenclaturalStatus;
  }

  public String getOrder() {
    return order;
  }

  public String getOriginalNameUsage() {
    return originalNameUsage;
  }

  public String getOriginalNameUsageID() {
    return originalNameUsageID;
  }

  public String getParentNameUsage() {
    return parentNameUsage;
  }

  public String getParentNameUsageID() {
    return parentNameUsageID;
  }

  public String getPhylum() {
    return phylum;
  }

  /**
   * Gets a dwc property by its concept term.
   * This method is only able to access official Darwin Core or Dublin Core terms, not any custom extensions.
   *
   * @param prop the concept term to lookup
   * @return the terms value, null or IllegalArgumentException for unsupported terms
   */
  public String getProperty(Term prop) {
    String getter = String.format("get%s", getPropertyName(prop));
    try {
      Method m = getClass().getMethod(getter);
      return m.invoke(this).toString();

    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("non existing dwc property: " + prop);

    } catch (Exception e) {
      // should never happen
      throw new RuntimeException(e);
    }
  }

  private String getPropertyName(Term prop) {
    String propName = StringUtils.capitalize(prop.simpleName());
    if (propName.equalsIgnoreCase("Class")) {
      propName = "Classs";
    }
    return propName;
  }

  public String getRights() {
    return rights;
  }

  public String getRightsholder() {
    return rightsholder;
  }

  public String getScientificName() {
    return scientificName;
  }

  public String getScientificNameAuthorship() {
    return scientificNameAuthorship;
  }

  public String getScientificNameID() {
    return scientificNameID;
  }

  public String getSource() {
    return source;
  }

  public String getSpecificEpithet() {
    return specificEpithet;
  }

  public String getSubgenus() {
    return subgenus;
  }

  public String getTaxonConceptID() {
    return taxonConceptID;
  }

  public String getTaxonID() {
    return taxonID;
  }

  public String getTaxonomicStatus() {
    return taxonomicStatus;
  }

  public String getTaxonRank() {
    return taxonRank;
  }

  public String getTaxonRankInterpreted() {
    if (scientificName != null || taxonRank != null) {
      return taxonRank;
    }
    if (genus != null && specificEpithet != null && infraspecificEpithet != null) {
      if (infraspecificMarker != null) {
        return infraspecificMarker;
      }
      return "infraspecies";
    }
    if (genus != null && specificEpithet != null) {
      return "species";
    }
    if (subgenus != null) {
      return "subgenus";
    }
    if (genus != null) {
      return "genus";
    }
    if (family != null) {
      return "family";
    }
    if (order != null) {
      return "order";
    }
    if (classs != null) {
      return "class";
    }
    if (phylum != null) {
      return "phylum";
    }
    if (kingdom != null) {
      return "kingdom";
    }
    return null;
  }

  public String getTaxonRemarks() {
    return taxonRemarks;
  }

  public String getVerbatimTaxonRank() {
    return verbatimTaxonRank;
  }

  public String getVernacularName() {
    return vernacularName;
  }

  protected String norm(String x) {
    x = StringUtils.trimToNull(x);
    if (x != null && (x.equalsIgnoreCase("\\N") || x.equalsIgnoreCase("NULL"))) {
      x = null;
    }
    return x;
  }

  public void setAcceptedNameUsage(String acceptedNameUsage) {
    this.acceptedNameUsage = acceptedNameUsage;
  }

  public void setAcceptedNameUsageID(String acceptedNameUsageID) {
    this.acceptedNameUsageID = acceptedNameUsageID;
  }

  public void setAccessrights(String accessrights) {
    this.accessrights = norm(accessrights);
  }

  public void setClasss(String classs) {
    this.classs = norm(classs);
  }

  public void setDatasetID(String datasetID) {
    this.datasetID = norm(datasetID);
  }

  public void setFamily(String family) {
    this.family = norm(family);
  }

  public void setGenus(String genus) {
    this.genus = norm(genus);
  }

  public void setGenericName(String genericName) {
    this.genericName = genericName;
  }

  public void setHigherClassification(String higherClassification) {
    this.higherClassification = higherClassification;
  }

  public void setInfraspecificEpithet(String infraspecificEpithet) {
    this.infraspecificEpithet = norm(infraspecificEpithet);
  }

  public void setInfraspecificMarker(String infraspecificMarker) {
    this.infraspecificMarker = infraspecificMarker;
  }

  public void setKingdom(String kingdom) {
    this.kingdom = norm(kingdom);
  }

  public void setLanguage(String language) {
    this.language = norm(language);
  }

  public void setModified(String modified) {
    this.modified = norm(modified);
  }

  public void setNameAccordingTo(String nameAccordingTo) {
    this.nameAccordingTo = nameAccordingTo;
  }

  public void setNameAccordingToID(String nameAccordingToID) {
    this.nameAccordingToID = nameAccordingToID;
  }

  public void setNamePublishedIn(String namePublishedIn) {
    this.namePublishedIn = norm(namePublishedIn);
  }

  public void setNamePublishedInID(String namePublishedInID) {
    this.namePublishedInID = namePublishedInID;
  }

  public void setNomenclaturalCode(String nomenclaturalCode) {
    this.nomenclaturalCode = norm(nomenclaturalCode);
  }

  public void setNomenclaturalStatus(String nomenclaturalStatus) {
    this.nomenclaturalStatus = norm(nomenclaturalStatus);
  }

  public void setOrder(String order) {
    this.order = norm(order);
  }

  public void setOriginalNameUsage(String originalNameUsage) {
    this.originalNameUsage = originalNameUsage;
  }

  public void setOriginalNameUsageID(String originalNameUsageID) {
    this.originalNameUsageID = originalNameUsageID;
  }

  public void setParentNameUsage(String parentNameUsage) {
    this.parentNameUsage = parentNameUsage;
  }

  public void setParentNameUsageID(String parentNameUsageID) {
    this.parentNameUsageID = parentNameUsageID;
  }

  public void setPhylum(String phylum) {
    this.phylum = norm(phylum);
  }

  public boolean setProperty(Term prop, String value) {
    if (prop == null) {
      return false;
    }
    String setterName = null;
    try {
      setterName = String.format("set%s", getPropertyName(prop));
      Method m = getClass().getMethod(setterName, String.class);
      m.invoke(this, value);
    } catch (Exception e) {
      LOG.warn("Trying to access bad dwc property setter: " + setterName + " for property " + prop);
      return false;
    }
    return true;
  }

  public void setRights(String rights) {
    this.rights = norm(rights);
  }

  public void setRightsholder(String rightsholder) {
    this.rightsholder = norm(rightsholder);
  }

  public void setScientificName(String scientificName) {
    this.scientificName = norm(scientificName);
  }

  public void setScientificNameAuthorship(String scientificNameAuthorship) {
    this.scientificNameAuthorship = norm(scientificNameAuthorship);
  }

  public void setScientificNameID(String scientificNameID) {
    this.scientificNameID = scientificNameID;
  }

  public void setSource(String source) {
    this.source = norm(source);
  }

  public void setSpecificEpithet(String specificEpithet) {
    this.specificEpithet = norm(specificEpithet);
  }

  public void setSubgenus(String subgenus) {
    this.subgenus = norm(subgenus);
  }

  public void setTaxonConceptID(String taxonConceptID) {
    this.taxonConceptID = taxonConceptID;
  }

  public void setTaxonID(String taxonID) {
    this.taxonID = norm(taxonID);
  }

  public void setTaxonomicStatus(String taxonomicStatus) {
    this.taxonomicStatus = norm(taxonomicStatus);
  }

  public void setTaxonRank(String taxonRank) {
    this.taxonRank = norm(taxonRank);
  }

  public void setTaxonRemarks(String taxonRemarks) {
    this.taxonRemarks = norm(taxonRemarks);
  }

  public void setVerbatimTaxonRank(String verbatimTaxonRank) {
    this.verbatimTaxonRank = verbatimTaxonRank;
  }

  public void setVernacularName(String vernacularName) {
    this.vernacularName = vernacularName;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("taxonID", this.taxonID).append("scientificName", this.scientificName)
      .toString();
  }

  public String getDynamicProperties() {
    return dynamicProperties;
  }

  public void setDynamicProperties(String dynamicProperties) {
    this.dynamicProperties = dynamicProperties;
  }

  public String getNamePublishedInYear() {
    return namePublishedInYear;
  }

  public void setNamePublishedInYear(String namePublishedInYear) {
    this.namePublishedInYear = namePublishedInYear;
  }
}
