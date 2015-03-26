/***************************************************************************
 * Copyright 2010 Global Biodiversity Information Facility Secretariat
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***************************************************************************/

package org.gbif.dwca.io;

import org.gbif.registry.metadata.EMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * Utils class to write a meta.xml descriptor file.
 */
public class MetaDescriptorWriter {

  private static final String META_TEMPLATE = "meta.ftl";
  private static final String TEMPLATE_PATH = "/templates";
  private static final Configuration FTL = provideFreemarker();

  private MetaDescriptorWriter() {
  }

  public static void writeMetaFile(File f, Archive archive) throws IOException {
    Writer writer = new FileWriter(f);
    try {
      FTL.getTemplate(META_TEMPLATE).process(archive, writer);
    } catch (TemplateException e) {
      throw new IOException("Error while processing the meta.xml template", e);
    }
    writer.close();
  }

  /**
   * Provides a freemarker template loader. It is configured to access the utf8 templates folder on the classpath, i.e.
   * /src/resources/templates
   */
  private static Configuration provideFreemarker() {
    // load templates from classpath by prefixing /templates
    TemplateLoader tl = new ClassTemplateLoader(EMLWriter.class, TEMPLATE_PATH);

    Configuration fm = new Configuration();
    fm.setDefaultEncoding("utf8");
    fm.setTemplateLoader(tl);

    return fm;
  }
}
