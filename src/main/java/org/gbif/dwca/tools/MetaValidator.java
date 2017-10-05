package org.gbif.dwca.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Utility class that exposes a singleton instance of an xml validator for the dwc archive meta.xml descriptor.
 * @Deprecated use validator for that
 */
@Deprecated
public class MetaValidator {
  private static final Logger LOG = LoggerFactory.getLogger(MetaValidator.class);
  private static final String SCHEMA_LANG = "http://www.w3.org/2001/XMLSchema";
  private static final String XSD_SCHEMA = "https://raw.githubusercontent.com/tdwg/dwc/master/standard/documents/text/tdwg_dwc_text.xsd";
  private static Validator VALIDATOR;

  public static void validate(String xml) throws IllegalArgumentException, IOException, SAXException {
    validate(new StreamSource(new StringReader(xml)));
  }

  public static void validate(InputStream xml) throws IllegalArgumentException, IOException, SAXException {
    validate(new StreamSource(xml));
  }

  /**
   * @throws IllegalArgumentException
   *      If the <code>Source</code>
   *      is an XML artifact that the implementation cannot
   *      validate (for example, a processing instruction).
   *
   * @throws SAXException
   *      If the {@link org.xml.sax.ErrorHandler} throws a {@link SAXException} or
   *      if a fatal error is found and the {@link org.xml.sax.ErrorHandler} returns
   *      normally.
   *
   * @throws IOException
   *      If the validator is processing a
   *      {@link javax.xml.transform.sax.SAXSource} and the
   *      underlying {@link org.xml.sax.XMLReader} throws an
   *      {@link IOException}.
   *
   *
   * @throws NullPointerException If <code>source</code> is
   *   <code>null</code>.
   */
  public static void validate(Source source) throws IllegalArgumentException, IOException, SAXException {
    getValidator().validate(source);
    LOG.debug("XML passed validation");
  }

  public static Validator getValidator() throws IOException, SAXException {
    if (VALIDATOR == null) {
      // define the type of schema - we use W3C:
      // resolve validation driver:
      SchemaFactory factory = SchemaFactory.newInstance(SCHEMA_LANG);
      // create schema by reading it from gbif online resources:
      Schema schema = factory.newSchema(new StreamSource(XSD_SCHEMA));
      VALIDATOR = schema.newValidator();
    }
    return VALIDATOR;
  }
}
