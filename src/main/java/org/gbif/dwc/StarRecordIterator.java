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

import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.record.StarRecordImpl;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.ClosableIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.collections4.iterators.PeekingIterator;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link ClosableIterator} of {@link StarRecord} which includes the core record and all its extension records.
 */
class StarRecordIterator implements ClosableIterator<StarRecord> {

  private final StarRecordImpl rec;
  private final ClosableIterator<Record> coreIter;
  private final Set<ClosableIterator<?>> closeable = new HashSet<>();
  private final Map<Term, PeekingIterator<Record>> extensionIters = new HashMap<>();
  private final Map<Term, Integer> extensionRecordsSkipped = new HashMap<>();

  StarRecordIterator(ClosableIterator<Record> coreIt, Map<Term, ClosableIterator<Record>> extensionIts) {
    coreIter = coreIt;
    List<Term> rowTypes = new ArrayList<>();

    if (extensionIts != null) {
      for (Term extTerm : extensionIts.keySet()) {
        rowTypes.add(extTerm);
        closeable.add(extensionIts.get(extTerm));
        extensionIters.put(extTerm, new PeekingIterator<>(extensionIts.get(extTerm)));
        extensionRecordsSkipped.put(extTerm, 0);
      }
    }
    rec = new StarRecordImpl(rowTypes);
  }

  @Override
  public void close() throws IOException {

    try {
      coreIter.close();
      for (ClosableIterator<?> it : closeable) {
        it.close();
      }
    } catch (Exception e) {
      throw new IOException(e);
    }

    Optional<Exception> result = closeable.stream()
            .flatMap(o -> {
              try {
                o.close();
                return null;
              } catch (Exception ex) {
                return Stream.of(ex);
              }
            })
            // now a stream of thrown exceptions.
            // can collect them to list or reduce into one exception
            .reduce((ex1, ex2) -> {
              ex1.addSuppressed(ex2);
              return ex1;
            });

    if (result.isPresent()) {
      throw new IOException(result.get());
    }

//    for (Map.Entry<Term, Integer> stringIntegerEntry : extensionRecordsSkipped.entrySet()) {
//      Integer skipped = stringIntegerEntry.getValue();
//      if (skipped > 0) {
//        LOG.debug("{} {} extension records without matching core", skipped, stringIntegerEntry.getKey());
//      }
//    }
  }

  @Override
  public boolean hasNext() {
    return coreIter.hasNext();
  }

  @Override
  public StarRecord next() {
    Record core = coreIter.next();
    rec.newCoreRecord(core);
    // add extension records if core id exists
    if (core.id() != null) {
      String id = core.id();
      for (Map.Entry<Term, PeekingIterator<Record>> ext : extensionIters.entrySet()) {
        PeekingIterator<Record> it = ext.getValue();
        Term rowType = ext.getKey();
        while (it.hasNext()) {
          String extId = it.peek().id();
          // make sure we have an extid
          if (StringUtils.isBlank(extId)) {
            it.next();
            continue;
          }
          if (id.equals(extId)) {
            // extension row belongs to this core record
            rec.addRecord(rowType, it.next());
          } else if (id.compareTo(extId) > 0) {
            // this extension id is smaller than the core id and should have been picked up by a core record already
            // seems to have no matching core record, so lets skip it
            it.next();
            extensionRecordsSkipped.put(rowType, extensionRecordsSkipped.get(rowType) + 1);
          } else {
            // higher id, we need to wait for this one
            break;
          }
        }
      }
    }

    return rec;
  }

}
