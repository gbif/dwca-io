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

import java.util.Set;

public interface Record {

  /**
   * @return the record id
   */
  String id();

  /**
   * @return the row type of the record
   */
  Term rowType();

  /**
   * Returns a record value based on a term concept.
   */
  String value(Term term);

  /**
   * Returns a record value based on a column index.
   */
  String column(int index);

  /**
   * @return the set of all mapped / available concept terms
   */
  Set<Term> terms();

}
