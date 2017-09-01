package org.gbif.dwc;

import java.io.File;

/**
 * A DarwinCore file can be represented under different "layout".
 *
 * Considering the file /myDcw/DarwinCore.txt
 *
 * If we provide the path /myDcw/DarwinCore.txt we have a FILE_ROOT.
 * If we provided the path /myDcw/ we have a DIRECTORY_ROOT.
 *
 */
public enum DwcLayout {
  DIRECTORY_ROOT, FILE_ROOT;

  public static DwcLayout fromFile(File root) {
    return root.isDirectory() ? DIRECTORY_ROOT : FILE_ROOT;
  }
}
