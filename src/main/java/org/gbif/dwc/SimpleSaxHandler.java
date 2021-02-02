/*
 * Copyright 2021 Global Biodiversity Information Facility (GBIF)
 *
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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simple metadata SAX base handler that collects all character data inside elements into a string buffer, resetting
 * the buffer with every element start and storing the string version of the buffer in this.content when the end of the
 * element is reached.
 * Make sure to call the super methods when implementing this handler!
 */
public abstract class SimpleSaxHandler extends DefaultHandler {
  protected final Logger log = LoggerFactory.getLogger(getClass());
  protected String content;
  private StringBuffer chars;
  private Pattern normWhitespace = Pattern.compile("\\s+");
  protected String parents = "";

  @Override
  public void characters(char[] ch, int start, int length) {
    chars.append(ch, start, length);
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    content = StringUtils.trimToNull(chars.toString());
    // norm whitespace
    if (content != null) {
      content = normWhitespace.matcher(content).replaceAll(" ");
    }
    parents = parents.substring(0, parents.length() - localName.length() - 1);
  }

  @Override
  public void startDocument() {
    parents = "";
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    chars = new StringBuffer();
    parents += "/" + localName.toLowerCase();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
