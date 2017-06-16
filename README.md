# dwca-io

*Formerly know as dwca-reader*

The dwca-oi library provides:
 * Reader for [DarwinCore Archive](http://rs.tdwg.org/dwc/terms/guides/text/index.htm) file with or without extensions
 * Writer for simple [DarwinCore Archive](http://rs.tdwg.org/dwc/terms/guides/text/index.htm) file with or without extensions


## To build the project
```
mvn clean install
```

## Usage
### Reading DarwinCore archive
Read an archive and display the scientific name of each records:
```java
File myArchiveFile = new File("myArchive.zip");
File extractToFolder = new File("/tmp/myarchive");
Archive dwcArchive = ArchiveFactory.openArchive(myArchiveFile, extractToFolder);

// loop over core darwin core records and display scientificName
Iterator<DarwinCoreRecord> it = dwcArchive.iteratorDwc();
while (it.hasNext()) {
  DarwinCoreRecord dwc = it.next();
  System.out.println(dwc.getScientificName());
}
```
### Reading DarwinCore archive + extensions
Read from a folder(extracted archive) and display the scientific name of each records + vernacular name(s) from the extension:
```java
//WARNING: StarRecord requires underlying data files(including extensions) to be sorted by the coreid column
Archive dwcArchive = ArchiveFactory.openArchive(new File("/tmp/myarchive"));
System.out.println("Archive rowtype: " + dwcArchive.getCore().getRowType() + ", "
    + dwcArchive.getExtensions().size() + " extension(s)");
// loop over core darwin core star records
for (StarRecord rec : dwcArchive) {
  System.out.println(rec.core().id() + " scientificName: " + rec.core().value(DwcTerm.scientificName));
  if (rec.hasExtension(GbifTerm.VernacularName)) {
    for (Record extRec : rec.extension(GbifTerm.VernacularName)) {
      System.out.println(" -" + extRec.value(DwcTerm.vernacularName));
    }
  }
}
```
### Other supported file types
The `ArchiveFactory.openArchive` method also supports the following file types:
 * Single meta.xml file
 * Single data file with terms as header
 
## Maven
Ensure you have the GBIF repository in your `pom.xml`
```xml
<repositories>
  <repository>
    <id>gbif-repository</id>
    <url>http://repository.gbif.org/content/groups/gbif</url>
  </repository>
</repositories>
```
Add the dwca-io artifact
```xml
  <dependency>
    <groupId>org.gbif</groupId>
    <artifactId>dwca-io</artifactId>
    <version>1.31</version>
  </dependency>
```

## Change Log
[Change Log](CHANGELOG.md)

## Documentation
[JavaDocs](http://gbif.github.io/dwca-io/apidocs/)