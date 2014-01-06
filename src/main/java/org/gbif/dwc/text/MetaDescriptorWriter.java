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

package org.gbif.dwc.text;

import org.gbif.file.FreemarkerWriter;

import java.io.File;
import java.io.IOException;

import freemarker.template.TemplateException;

/**
 * Utils class to write a meta.xml descriptor file.
 */
public class MetaDescriptorWriter extends FreemarkerWriter {

  private static final String META_TEMPLATE = "meta.ftl";

  private MetaDescriptorWriter() {
  }

  public static void writeMetaFile(File f, Archive archive) throws IOException, TemplateException {
    writeFile(f, META_TEMPLATE, archive);
  }
}
