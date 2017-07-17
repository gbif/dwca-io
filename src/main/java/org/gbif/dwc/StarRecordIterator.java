package org.gbif.dwc;

import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.StarRecord;
import org.gbif.dwca.record.StarRecordImpl;
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

import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * {@link ClosableIterator} of {@link StarRecord} which includes the core record and all its extension records.
 * This is an revised version of {@link Archive.ArchiveIterator}
 */
class StarRecordIterator implements ClosableIterator<StarRecord> {

  private final StarRecordImpl rec;
  private final ClosableIterator<Record> coreIter;
  private final Set<ClosableIterator<?>> closeable = new HashSet<>();
  private final Map<Term, PeekingIterator<Record>> extensionIters = new HashMap<>();
  private final Map<Term, Integer> extensionRecordsSkipped = new HashMap<>();

  StarRecordIterator(ClosableIterator<Record> coreIt, Map<Term, ClosableIterator<Record>> extensionIts) throws IOException {
    coreIter = coreIt;
    List<Term> rowTypes = new ArrayList<>();

    if (extensionIts != null) {
      for (Term extTerm : extensionIts.keySet()) {
        rowTypes.add(extTerm);
        closeable.add(extensionIts.get(extTerm));
        extensionIters.put(extTerm, Iterators.peekingIterator(extensionIts.get(extTerm)));
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
          if (Strings.isNullOrEmpty(extId)) {
            it.next();
            continue;
          }
          if (id.equals(extId)) {
            // extension row belongs to this core record
            rec.addRecord(rowType, it.next());
          } else if (id.compareTo(extId) > 0) {
            // TODO: we need to use the exact same sorting order, ie comparator, as we use for sorting the data files!!!
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
