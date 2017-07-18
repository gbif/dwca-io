# Change Log

## 1.32 (not released yet)
 * Deprecation of all iteration related classes/methods from [Archive](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwca/io/Archive.html)
 in favour of [DwcFiles](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwc/DwcFiles.html).

## 1.31 (2017-06-16)

[Complete Changelog](https://github.com/gbif/dwca-io/compare/dwca-io-1.30...dwca-io-1.31)

* New class to get content of Darwin Core files: [DwcFiles](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwc/DwcFiles.html). The class gives access to the implementation with support for multiline records. See [Issue #26](https://github.com/gbif/dwca-io/issues/26).
* Changed [ArchiveFile](http://gbif.github.io/dwca-io/apidocs/org/gbif/dwca/io/ArchiveFile.html) to use the default values specified by the Darwin Core Text Guide. See [Issue #29](https://github.com/gbif/dwca-io/issues/29).