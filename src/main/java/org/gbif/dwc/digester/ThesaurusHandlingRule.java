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
package org.gbif.dwc.digester;

import org.gbif.dwc.extensions.ExtensionProperty;
import org.gbif.dwc.extensions.VocabulariesManager;
import org.gbif.dwc.extensions.Vocabulary;

import java.net.URL;

import org.apache.commons.digester3.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

/**
 * This will call the root of the stack to find the url2thesaurus, and then set the appropriate thesaurus on the
 * extension. Namespaces are completely ignored. The "thesaurus" attribute is searched for and if found, the thesaurus
 * is set if found.
 */
public class ThesaurusHandlingRule extends Rule {

  private static final Logger LOG = LoggerFactory.getLogger(ThesaurusHandlingRule.class);

  public static final String ATTRIBUTE_THESAURUS = "thesaurus";

  private VocabulariesManager vocabManager;

  public ThesaurusHandlingRule(VocabulariesManager vocabManager) {
    super();
    this.vocabManager = vocabManager;
  }

  @Override
  public void begin(String namespace, String name, Attributes attributes) throws Exception {

    for (int i = 0; i < attributes.getLength(); i++) {
      if (ThesaurusHandlingRule.ATTRIBUTE_THESAURUS.equals(attributes.getQName(i))) {
        Vocabulary tv = null;
        try {
          URL vocabURL = new URL(attributes.getValue(i));
          tv = vocabManager.get(vocabURL);
        } catch (Exception e) {
          LOG.error("Could not load vocabulary with location {}: {}", new Object[]{attributes.getValue(i), e.getMessage(), e});
        }

        if (tv != null) {
          Object extensionPropertyAsObject = getDigester().peek();
          if (extensionPropertyAsObject instanceof ExtensionProperty) {
            ExtensionProperty eProperty = (ExtensionProperty) extensionPropertyAsObject;
            eProperty.setVocabulary(tv);
            LOG.debug("Vocabulary with URI[{}] added to extension property", tv.getUri());
          }
        } else {
          LOG.warn("No vocabulary exists for the URL[{}]", attributes.getValue(i));
        }

        break; // since we found the attribute
      }
    }
  }
}
