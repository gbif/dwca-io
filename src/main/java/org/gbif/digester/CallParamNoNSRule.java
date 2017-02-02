package org.gbif.digester;

import org.apache.commons.digester3.CallParamRule;
import org.xml.sax.Attributes;

/**
 * Will not honor namespaces in attributes in any way
 *
 * This class allows for getting access to attributes in elements where the
 * namespaces differ, but will ignore the namespace of the attribute
 * Additionally, it only allows for accessing a single attribute (since NS are
 * ignored) It would not be difficult to honor namespaces, but is not currently
 * needed.
 *
 */
public class CallParamNoNSRule extends CallParamRule {
  /**
   * @param paramIndex The index to use (e.g. in the param list for a call
   *          method rule)
   * @param attributeName The local name of the attribute only
   */
  public CallParamNoNSRule(int paramIndex, String attributeName) {
    super(paramIndex, attributeName);
  }

  @Override
  public void begin(String namespace, String name, Attributes attributes) throws Exception {
    Object param = null;
    for (int i = 0; i < attributes.getLength(); i++) {

      // if it has no prefix, or has SOME prefix and ends in the attribute name
      // (___:attributeName)
      if (attributes.getQName(i).equals(attributeName)
              || attributes.getQName(i).endsWith(":" + attributeName)) {
        param = attributes.getValue(i);
        break;
      }
    }
    // add to the params stack
    if (param != null) {
      Object parameters[] = getDigester().peekParams();
      parameters[paramIndex] = param;
    }
  }
}