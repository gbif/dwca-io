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

// Loop over core records and display id, genus, specific epithet
for (Record rec : dwcArchive.getCore()) {
  System.out.printf("%s: %s %s%n", rec.id(), rec.value(DwcTerm.genus), rec.value(DwcTerm.specificEpithet));
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

// Loop over star records and display id, core record data, and extension data
for (StarRecord rec : dwcArchive) {
  System.out.printf("%s: %s %s%n", rec.core().id(), rec.core().value(DwcTerm.genus), rec.core().value(DwcTerm.specificEpithet));
  if (rec.hasExtension(DwcTerm.Occurrence)) {
    for (Record extRec : rec.extension(DwcTerm.Occurrence)) {
      System.out.println(" - " + extRec.value(DwcTerm.country));
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

## Maven
Ensure you have the GBIF repository in your `pom.xml`
```xml
<repositories>
  <repository>
    <id>gbif-repository</id>
    <url>https://repository.gbif.org/content/groups/gbif</url>
  </repository>
</repositories>
```

Add the dwca-io artifact
```xml
  <dependency>
    <groupId>org.gbif</groupId>
    <artifactId>dwca-io</artifactId>
    <version>{latest-version}</version>
  </dependency>
```

where `{latest-version}` can be found [here](https://github.com/gbif/dwca-io/tags)

## Change Log
[Change Log](CHANGELOG.md)

## Documentation
[JavaDocs](https://gbif.github.io/dwca-io/apidocs/)

## Unsupported archives

[Darwin Core Text](https://dwc.tdwg.org/text/) specifies several features which are not supported by this library.

* A `<core>` or `<extension>` setting a `dateFormat`
* A `<files>` `<location>` which is a URL

These features are very rarely used, and will not be implemented without good reason.
