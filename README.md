# Darwin Core Archive I/O (dwca-io)

*Formerly known as dwca-reader*

The dwca-io library provides:
 * Reader for [DarwinCore Archive](http://rs.tdwg.org/dwc/terms/guides/text/) file with or without extensions.
 * Reader for single tabular file using [Darwin Core terms](http://rs.tdwg.org/dwc/terms/#theterms) as headers
 * Support for discovery of metadata document (e.g. [EML](https://knb.ecoinformatics.org/#tools/eml)).
 * Writer for simple DarwinCore Archive file with or without extensions

## To build the project
Note: this project requires Java 8.
```
mvn clean install
```

## Usage
### Reading a simple Darwin Core Archive
Read an archive and display data from the core record:
```
Path myArchiveFile = Paths.get("myArchive.zip");
Path extractToFolder = Paths.get("/tmp/myarchive");
Archive dwcArchive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder);

// Loop over core core records and display id, basis of record and scientific name
for (Record rec : dwcArchive.getCore()) {
  System.out.println(String.format("%s: %s (%s)", rec.id(), rec.value(DwcTerm.basisOfRecord), rec.value(DwcTerm.scientificName)));
}
```
### Reading DarwinCore archive + extensions
Read from a folder (extracted archive) and display data from the core and the extension:
```
Path myArchiveFile = Paths.get("myArchive.zip");
Path extractToFolder = Paths.get("/tmp/myarchive");
Archive dwcArchive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder);

System.out.println("Archive rowtype: " + dwcArchive.getCore().getRowType() + ", "
    + dwcArchive.getExtensions().size() + " extension(s)");

for (StarRecord rec : dwcArchive) {
  System.out.println(String.format("%s: %s", rec.core().id(), rec.core().value(DwcTerm.scientificName)));
  if (rec.hasExtension(GbifTerm.VernacularName)) {
    for (Record extRec : rec.extension(GbifTerm.VernacularName)) {
      System.out.println(" - " + extRec.value(DwcTerm.vernacularName));
    }
  }
}
```

### Other supported file types
The `DwcFiles.fromLocation` method also supports the following file types:
* Single tabular data file with [Darwin Core terms](http://rs.tdwg.org/dwc/terms/#theterms) as header

## Notes
* The `delimitedBy` attribute of a field is not supported.
* The `dateFormat` attribute of a file is not supported.
* Iterating over an Archive with extensions requires pre-sorting the data files.  This can take seconds to minutes,
  depending on the size of the archive.  If you prefer, you can use `Archive#initialize()` to sort the archive beforehand.
* Archives with data from a single core/extension split across multiple files are not supported.

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

## Unsupported archives

[Darwin Core Text](https://dwc.tdwg.org/text/) specifies several features which are not supported by this library.

* A `<core>` or `<extension>` setting a `dateFormat`
* A `<files>` `<location>` which is a URL
* Multiple `<location>`s for a single `<files>` element

These features are very rarely used, and will not be implemented without good reason.
