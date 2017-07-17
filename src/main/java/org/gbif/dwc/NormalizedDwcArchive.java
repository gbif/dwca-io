package org.gbif.dwc;

import org.gbif.dwc.terms.Term;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.StarRecord;
import org.gbif.utils.file.ClosableIterator;

import java.io.IOException;
import java.util.Map;

/**
 *
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
