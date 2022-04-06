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

import org.gbif.dwc.terms.Term;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StarRecord extends Iterable<Record> {

  /**
   * @return the core record
   */
  Record core();

  boolean hasExtension(Term rowType);

  /**
   * Retrieves all extension records related to the core record across all extensions as a map.
   */
  Map<Term, List<Record>> extensions();

  /**
   * Retrieves all extension records of a specific extension.
   * If the requested extension is not mapped null will be returned.
   *
   * @param rowType the Term representing the rowType
   *
   * @return possibly empty list of extension record or null if extension is not mapped at all
   */
  List<Record> extension(Term rowType);

  /**
   * @return set of extension rowTypes associated with this star record
   */
  Set<Term> rowTypes();

  /**
   * @return the number of associated extension records across all rowTypes
   */
  int size();
}
