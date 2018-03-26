package org.gbif.dwc;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.utils.file.ClosableIterator;

import java.io.IOException;
import java.util.Map;

/**
 * A {@link NormalizedDwcArchive} represents a DarwinCore archive that has been normalized in order to be able to
 * offer a {@link ClosableIterator} of {@link StarRecord}. A {@link StarRecord} requires normalization (which means here
 * sorting all files by core id) in order to give the core records + all extension records attached to it.
 *
 * If you don't need an iterator of {@link StarRecord}, you can use {@link DwcFiles#iterator(ArchiveFile, boolean, boolean)} to get
 * an iterator of {@link Record} (which is a record without its extension(s)).
 *
 * Internally, this class will use different iterators to traverse the core and its extensions through
 * {@link StarRecordIterator}.
 */
public class NormalizedDwcArchive {

  private final SupplierWithIO<ClosableIterator<Record>> coreItSupplier;
  private final SupplierWithIO<Map<Term, ClosableIterator<Record>>> extItSupplier;

  /**
   * package protected constructor. Use {@link DwcFiles}.
   * @param coreItSupplier
   * @throws IOException
   */
  NormalizedDwcArchive(SupplierWithIO<ClosableIterator<Record>> coreItSupplier) throws IOException {
    this(coreItSupplier, null);
  }

  /**
   * package protected constructor. Use {@link DwcFiles}.
   * @param coreItSupplier
   * @param extItSupplier
   * @throws IOException
   */
  NormalizedDwcArchive(SupplierWithIO<ClosableIterator<Record>> coreItSupplier, SupplierWithIO<Map<Term, ClosableIterator<Record>>> extItSupplier) throws IOException {
    this.coreItSupplier = coreItSupplier;
    this.extItSupplier = extItSupplier;
  }

  /**
   * Get a new {@link ClosableIterator} of {@link StarRecord}.
   * {@link ClosableIterator} is expected to be used with a try-with-resource.
   * @return
   * @throws IOException
   */
  public ClosableIterator<StarRecord> iterator() throws IOException {
    return new StarRecordIterator(coreItSupplier.get(), extItSupplier != null ? extItSupplier.get() : null);
  }
}
