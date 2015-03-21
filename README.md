dwca-io
===========

Java library for reading and writing Darwin Core Archive files.
Formerly know as dwca-reader

## Example usage
### Reading DarwinCore archive
Read an archive and display the scientific name of each records:
```java
File myArchiveFile = new File("myArchive.zip");
File extractToFolder = new File("/tmp/myarchive");
Archive dwcArchive = ArchiveFactory.openArchive(myArchiveFile, extractToFolder);

Iterator<DarwinCoreRecord> it = dwcArchive.iteratorDwc();
DarwinCoreRecord dwc;
// loop over core darwin core records and display scientificName
while (it.hasNext()) {
  dwc = it.next();
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
