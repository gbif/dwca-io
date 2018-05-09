# Change Log

## 2.0-RC1 (2018-05-09)
 * Removal of all deprecated classes and methods, and a refactoring of the package structure.
 * Removal of obsolete classes DarwinCoreRecord and DarwinCoreTaxon, DownloadUtil.
 * Remove dependency on GBIF Registry Metadata and EML parsing.
 * Guava dependency now shaded.
 * Lock files for parallel access with a single process, or between processes.
 * Removal of all iterable interface from [Archive](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwca/io/Archive.html) in favour of [DwcFiles](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwc/DwcFiles.html).
 * [DwcFiles](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwc/DwcFiles.html) can now create [Archive](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwca/io/Archive.html) objects.

## 1.31 (2017-06-16)

[Complete Changelog](https://github.com/gbif/dwca-io/compare/dwca-io-1.30...dwca-io-1.31)

* New class to get content of Darwin Core files: [DwcFiles](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwc/DwcFiles.html). The class gives access to the implementation with support for multiline records. See [Issue #26](https://github.com/gbif/dwca-io/issues/26).
* Changed [ArchiveFile](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwca/io/ArchiveFile.html) to use the default values specified by the Darwin Core Text Guide. See [Issue #29](https://github.com/gbif/dwca-io/issues/29).
