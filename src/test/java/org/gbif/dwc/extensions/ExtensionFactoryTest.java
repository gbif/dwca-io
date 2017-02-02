package org.gbif.dwc.extensions;

import org.gbif.digester.ThesaurusHandlingRule;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.utils.file.FileUtils;
import org.gbif.xml.SAXUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import static junit.framework.TestCase.assertNotNull;

/**
 *
 */
public class ExtensionFactoryTest {

  private String VOCAB_URL = "http://rs.gbif.org/vocabulary/gbif/description_type.xml";
  private InputStream VOCAB = FileUtils.classpathStream("extension/vocabulary_gbif_description_type.xml");
  private InputStream EXTENSION = FileUtils.classpathStream("extension/extension_gbif_1.0_description.xml");

  private VocabularyFactory VOCAB_FACTORY = new VocabularyFactory(SAXUtils.getNsAwareSaxParserFactory());

  public ExtensionFactoryTest() throws IOException {
  }

  @Test
  public void testExtensionFactory() throws IOException, ParserConfigurationException, SAXException {

    ThesaurusHandlingRule thr = new ThesaurusHandlingRule(new MockVocabulariesManager(VOCAB_URL, VOCAB_FACTORY.build(VOCAB)));
    ExtensionFactory factory = new ExtensionFactory(thr, SAXUtils.getNsAwareSaxParserFactory());

    Extension ext = factory.build(EXTENSION, new URL(VOCAB_URL), false);

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
