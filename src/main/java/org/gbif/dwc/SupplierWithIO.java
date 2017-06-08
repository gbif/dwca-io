package org.gbif.dwc;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * {@link Supplier} that throws IOException on get().
 */
@FunctionalInterface
interface SupplierWithIO<T> {
  T get() throws IOException;
}
