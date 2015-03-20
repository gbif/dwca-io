dwca-io
===========

Java library for reading and writing Darwin Core Archive files.
Formerly know as dwca-reader

## Example usage

Read an archive and display the scientific name of each records:
```java
	Archive dwcArchive = ArchiveFactory.openArchive(new File("myArchive.zip"));

	Iterator<DarwinCoreRecord> it = dwcArchive.iteratorDwc();
	DarwinCoreRecord dwc;
	// loop over core darwin core records and display scientificName
	while (it.hasNext()) {
		dwc = it.next();
		System.out.println(dwc.getScientificName());
	}
```