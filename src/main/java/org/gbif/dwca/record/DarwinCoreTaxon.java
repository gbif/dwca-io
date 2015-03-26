package org.gbif.dwca.record;

import org.gbif.dwc.terms.Term;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import com.google.common.base.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarwinCoreTaxon {

  private static final Logger LOG = LoggerFactory.getLogger(DarwinCoreTaxon.class);
  private static final String CLASSIFICATION_DELIMITER = " | ";
  private static final Pattern NORM_AUTHORS = Pattern.compile("[^a-z0-9]+");
  private String type;
  private String modified;
  private String language;
  private String license;
  private String rightsHolder;
  private String accessRights;
  private String bibliographicCitation;
  private String references;

  private String institutionID;
  private String collectionID;
  private String datasetID;
  private String institutionCode;
  private String collectionCode;
  private String datasetName;
  private String ownerInstitutionCode;
  private String basisOfRecord;
  private String informationWithheld;
  private String dataGeneralizations;
  private String dynamicProperties;
  private String taxonID;
  private String scientificNameID;
  private String acceptedNameUsageID;
  private String parentNameUsageID;
  private String originalNameUsageID;
  private String nameAccordingToID;
  private String namePublishedInID;
  private String taxonConceptID;
  private String scientificName;
  private String acceptedNameUsage;
  private String parentNameUsage;
  private String originalNameUsage;
  private String nameAccordingTo;
  private String namePublishedIn;
  private String namePublishedInYear;
  private String higherClassification;
  private String kingdom;
  private String phylum;
  private String classs;
  private String order;
  private String family;
  private String genus;
  private String subgenus;
  private String specificEpithet;
  private String infraspecificEpithet;
  private String taxonRank;
  private String verbatimTaxonRank;
  private String scientificNameAuthorship;
  private String vernacularName;
  private String nomenclaturalCode;
  private String taxonomicStatus;
  private String nomenclaturalStatus;
  private String taxonRemarks;

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

    if (buf.length() > CLASSIFICATION_DELIMITER.length()) {
      buf.delete(buf.length() - CLASSIFICATION_DELIMITER.length(), buf.length());
    }
    return buf.toString();
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
      if (this.genus != null) {
        if (specificEpithet != null) {
          sciname = genus + " " + specificEpithet;
          if (infraspecificEpithet != null) {
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

  /**
   * Gets a dwc property by its concept term.
   * This method is only able to access official Darwin Core or Dublin Core terms, not any custom extensions.
   *
   * @param prop the concept term to lookup
   *
   * @return the terms value, null or IllegalArgumentException for unsupported terms
   */
  public String getProperty(Term prop) {
    String getter = String.format("get%s", getPropertyName(prop));
    try {
      Method m = getClass().getMethod(getter);
      Object val = m.invoke(this);
      return val == null ? null : val.toString();

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

  public String getTaxonRankInterpreted() {
    if (scientificName != null || taxonRank != null) {
      return taxonRank;
    }
    if (genus != null && specificEpithet != null && infraspecificEpithet != null) {
      if (taxonRank != null) {
        return taxonRank;
      }
      if (verbatimTaxonRank != null) {
        return verbatimTaxonRank;
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

  /**
   * The method tries to best assemble the most complete scientific name possible incl the name authorship.
   * It first tries to use scientificName and scientificNameAuthorship if existing.
   * Otherwise it uses the atomized name parts. Warning: this uses the genus property which might be wrong in case
   * of synonym names, see https://code.google.com/p/darwincore/issues/detail?id=151
   * Note also that the assembled name never includes a rank marker.
   *
   * @return the best guess of the full scientific name with authorship
   */
  protected String norm(String x) {
    x = StringUtils.trimToNull(x);
    if (x != null && (x.equalsIgnoreCase("\\N") || x.equalsIgnoreCase("NULL"))) {
      x = null;
    }
    return x;
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

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("taxonID", this.taxonID)
      .append("scientificName", this.scientificName)
      .toString();
  }

  public String getAcceptedNameUsage() {
    return acceptedNameUsage;
  }

  public String getAcceptedNameUsageID() {
    return acceptedNameUsageID;
  }

  public String getAccessRights() {
    return accessRights;
  }

  public String getBasisOfRecord() {
    return basisOfRecord;
  }

  public String getBibliographicCitation() {
    return bibliographicCitation;
  }

  public String getClasss() {
    return classs;
  }

  public String getCollectionCode() {
    return collectionCode;
  }

  public String getCollectionID() {
    return collectionID;
  }

  public String getDataGeneralizations() {
    return dataGeneralizations;
  }

  public String getDatasetID() {
    return datasetID;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public String getDynamicProperties() {
    return dynamicProperties;
  }

  public String getFamily() {
    return family;
  }

  public String getGenus() {
    return genus;
  }

  public String getHigherClassification() {
    return higherClassification;
  }

  public String getInformationWithheld() {
    return informationWithheld;
  }

  public String getInfraspecificEpithet() {
    return infraspecificEpithet;
  }

  public String getInstitutionCode() {
    return institutionCode;
  }

  public String getInstitutionID() {
    return institutionID;
  }

  public String getKingdom() {
    return kingdom;
  }

  public String getLanguage() {
    return language;
  }

  public String getLicense() {
    return license;
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

  public String getNamePublishedInYear() {
    return namePublishedInYear;
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

  public String getOwnerInstitutionCode() {
    return ownerInstitutionCode;
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

  public String getReferences() {
    return references;
  }

  public String getRightsHolder() {
    return rightsHolder;
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

  public String getTaxonRemarks() {
    return taxonRemarks;
  }

  public String getType() {
    return type;
  }

  public String getVerbatimTaxonRank() {
    return verbatimTaxonRank;
  }

  public String getVernacularName() {
    return vernacularName;
  }

  public void setAcceptedNameUsage(String acceptedNameUsage) {
    this.acceptedNameUsage = acceptedNameUsage;
  }

  public void setAcceptedNameUsageID(String acceptedNameUsageID) {
    this.acceptedNameUsageID = acceptedNameUsageID;
  }

  public void setAccessRights(String accessRights) {
    this.accessRights = norm(accessRights);
  }

  public void setBasisOfRecord(String basisOfRecord) {
    this.basisOfRecord = norm(basisOfRecord);
  }

  public void setBibliographicCitation(String bibliographicCitation) {
    this.bibliographicCitation = bibliographicCitation;
  }

  public void setClasss(String classs) {
    this.classs = norm(classs);
  }

  public void setCollectionCode(String collectionCode) {
    this.collectionCode = norm(collectionCode);
  }

  public void setCollectionID(String collectionID) {
    this.collectionID = norm(collectionID);
  }

  public void setDataGeneralizations(String dataGeneralizations) {
    this.dataGeneralizations = norm(dataGeneralizations);
  }

  public void setDatasetID(String datasetID) {
    this.datasetID = norm(datasetID);
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public void setDynamicProperties(String dynamicProperties) {
    this.dynamicProperties = dynamicProperties;
  }

  public void setFamily(String family) {
    this.family = norm(family);
  }

  public void setGenus(String genus) {
    this.genus = norm(genus);
  }

  public void setHigherClassification(String higherClassification) {
    this.higherClassification = higherClassification;
  }

  public void setInformationWithheld(String informationWithheld) {
    this.informationWithheld = norm(informationWithheld);
  }

  public void setInfraspecificEpithet(String infraspecificEpithet) {
    this.infraspecificEpithet = norm(infraspecificEpithet);
  }

  public void setInstitutionCode(String institutionCode) {
    this.institutionCode = norm(institutionCode);
  }

  public void setInstitutionID(String institutionID) {
    this.institutionID = institutionID;
  }

  public void setKingdom(String kingdom) {
    this.kingdom = norm(kingdom);
  }

  public void setLanguage(String language) {
    this.language = norm(language);
  }

  public void setLicense(String license) {
    this.license = norm(license);
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

  public void setNamePublishedInYear(String namePublishedInYear) {
    this.namePublishedInYear = namePublishedInYear;
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

  public void setOwnerInstitutionCode(String ownerInstitutionCode) {
    this.ownerInstitutionCode = ownerInstitutionCode;
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

  public void setReferences(String references) {
    this.references = norm(references);
  }

  public void setRightsHolder(String rightsHolder) {
    this.rightsHolder = norm(rightsHolder);
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

  public void setType(String type) {
    this.type = type;
  }

  public void setVerbatimTaxonRank(String verbatimTaxonRank) {
    this.verbatimTaxonRank = verbatimTaxonRank;
  }

  public void setVernacularName(String vernacularName) {
    this.vernacularName = vernacularName;
  }

  @Override
  public int hashCode() {
    return Objects
      .hashCode(type, modified, language, license, rightsHolder, accessRights, bibliographicCitation, references,
        institutionID, collectionID, datasetID, institutionCode, collectionCode, datasetName, ownerInstitutionCode,
        basisOfRecord, informationWithheld, dataGeneralizations, dynamicProperties, taxonID, scientificNameID,
        acceptedNameUsageID, parentNameUsageID, originalNameUsageID, nameAccordingToID, namePublishedInID,
        taxonConceptID, scientificName, acceptedNameUsage, parentNameUsage, originalNameUsage, nameAccordingTo,
        namePublishedIn, namePublishedInYear, higherClassification, kingdom, phylum, classs, order, family, genus,
        subgenus, specificEpithet, infraspecificEpithet, taxonRank, verbatimTaxonRank, scientificNameAuthorship,
        vernacularName, nomenclaturalCode, taxonomicStatus, nomenclaturalStatus, taxonRemarks);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DarwinCoreTaxon other = (DarwinCoreTaxon) obj;
    return Objects.equal(this.type, other.type) && Objects.equal(this.modified, other.modified) && Objects
      .equal(this.language, other.language) && Objects.equal(this.license, other.license) && Objects
             .equal(this.rightsHolder, other.rightsHolder) && Objects.equal(this.accessRights, other.accessRights)
           && Objects.equal(this.bibliographicCitation, other.bibliographicCitation) && Objects
      .equal(this.references, other.references) && Objects.equal(this.institutionID, other.institutionID) && Objects
             .equal(this.collectionID, other.collectionID) && Objects.equal(this.datasetID, other.datasetID) && Objects
             .equal(this.institutionCode, other.institutionCode) && Objects
             .equal(this.collectionCode, other.collectionCode) && Objects.equal(this.datasetName, other.datasetName)
           && Objects.equal(this.ownerInstitutionCode, other.ownerInstitutionCode) && Objects
             .equal(this.basisOfRecord, other.basisOfRecord) && Objects
             .equal(this.informationWithheld, other.informationWithheld) && Objects
             .equal(this.dataGeneralizations, other.dataGeneralizations) && Objects
             .equal(this.dynamicProperties, other.dynamicProperties) && Objects.equal(this.taxonID, other.taxonID)
           && Objects.equal(this.scientificNameID, other.scientificNameID) && Objects
             .equal(this.acceptedNameUsageID, other.acceptedNameUsageID) && Objects
             .equal(this.parentNameUsageID, other.parentNameUsageID) && Objects
             .equal(this.originalNameUsageID, other.originalNameUsageID) && Objects
             .equal(this.nameAccordingToID, other.nameAccordingToID) && Objects
             .equal(this.namePublishedInID, other.namePublishedInID) && Objects
             .equal(this.taxonConceptID, other.taxonConceptID) && Objects
             .equal(this.scientificName, other.scientificName) && Objects
             .equal(this.acceptedNameUsage, other.acceptedNameUsage) && Objects
             .equal(this.parentNameUsage, other.parentNameUsage) && Objects
             .equal(this.originalNameUsage, other.originalNameUsage) && Objects
             .equal(this.nameAccordingTo, other.nameAccordingTo) && Objects
             .equal(this.namePublishedIn, other.namePublishedIn) && Objects
             .equal(this.namePublishedInYear, other.namePublishedInYear) && Objects
             .equal(this.higherClassification, other.higherClassification) && Objects.equal(this.kingdom, other.kingdom)
           && Objects.equal(this.phylum, other.phylum) && Objects.equal(this.classs, other.classs) && Objects
             .equal(this.order, other.order) && Objects.equal(this.family, other.family) && Objects
             .equal(this.genus, other.genus) && Objects.equal(this.subgenus, other.subgenus) && Objects
             .equal(this.specificEpithet, other.specificEpithet) && Objects
             .equal(this.infraspecificEpithet, other.infraspecificEpithet) && Objects
             .equal(this.taxonRank, other.taxonRank) && Objects.equal(this.verbatimTaxonRank, other.verbatimTaxonRank)
           && Objects.equal(this.scientificNameAuthorship, other.scientificNameAuthorship) && Objects
             .equal(this.vernacularName, other.vernacularName) && Objects
             .equal(this.nomenclaturalCode, other.nomenclaturalCode) && Objects
             .equal(this.taxonomicStatus, other.taxonomicStatus) && Objects
             .equal(this.nomenclaturalStatus, other.nomenclaturalStatus) && Objects
             .equal(this.taxonRemarks, other.taxonRemarks);
  }
}
