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
package org.gbif.dwc.extensions;

import org.gbif.dwc.digester.ThesaurusHandlingRule;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.xml.SAXUtils;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExtensionFactoryTest {

  @Test
  public void testExtensionFactory() throws IOException, ParserConfigurationException, SAXException {
    String vocabUrl = "http://rs.gbif.org/vocabulary/gbif/description_type.xml";
    InputStream vocab = FileUtils.classpathStream("extension/vocabulary_gbif_description_type.xml");
    InputStream extension = FileUtils.classpathStream("extension/extension_gbif_1.0_description.xml");

    VocabularyFactory vocabFactory = new VocabularyFactory(SAXUtils.getNsAwareSaxParserFactory());

    ThesaurusHandlingRule thr = new ThesaurusHandlingRule(new MockVocabulariesManager(vocabUrl, vocabFactory.build(vocab)));
    ExtensionFactory factory = new ExtensionFactory(thr, SAXUtils.getNsAwareSaxParserFactory());

    Extension ext = factory.build(extension, new URL(vocabUrl), false);

    assertNotNull(ext);
    assertNotNull(ext.getProperty(DcTerm.type).getVocabulary());
  }

  /**
   * Mock VocabulariesManager that support only 1 vocabulary
   */
  private static class MockVocabulariesManager implements VocabulariesManager{

    private final String vocabUrl;
    private final Vocabulary vocab;

    public MockVocabulariesManager(String vocabUrl, Vocabulary vocab){
      this.vocabUrl = vocabUrl;
      this.vocab = vocab;
    }

    @Override
    public Vocabulary get(String s) {
      return null;
    }

    @Override
    public Vocabulary get(URL url) {
      if(vocabUrl.equalsIgnoreCase(url.toString())){
        return vocab;
      }
      return null;
    }

    @Override
    public Map<String, String> getI18nVocab(String s, String s1) {
      return null;
    }

    @Override
    public List<Vocabulary> list() {
      return null;
    }
  }
}
