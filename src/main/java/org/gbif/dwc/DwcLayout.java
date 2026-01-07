/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.dwc;

import java.io.File;

/**
 * A DarwinCore file can be represented under different "layout".

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
