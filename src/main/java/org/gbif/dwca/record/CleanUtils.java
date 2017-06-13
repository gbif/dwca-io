package org.gbif.dwca.record;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

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
