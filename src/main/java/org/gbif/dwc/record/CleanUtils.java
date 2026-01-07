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
package org.gbif.dwc.record;

import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

public class CleanUtils {
  private static final Pattern NULL_REPL = Pattern.compile("^\\s*(null|\\\\N)?\\s*$", Pattern.CASE_INSENSITIVE);

  private CleanUtils() {
  }

  /**
   * Does basic entity replacements if requested to string values.
   * @param value the original string
   * @param nulls if true replaces common, literal NULL values with real nulls, e.g. "\N" or "NULL"
   * @param entities if true replaces html4, xml and numerical entities with their unicode character
   */
  public static String clean(String value, boolean nulls, boolean entities) {
    if (value == null || (nulls && NULL_REPL.matcher(value).find()) ) {
      return null;
    }
    return entities ? StringEscapeUtils.unescapeHtml4(value) : value;
  }
}
