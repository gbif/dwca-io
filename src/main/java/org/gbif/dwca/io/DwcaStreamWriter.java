package org.gbif.dwca.io;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.Closer;
import org.apache.commons.io.IOUtils;
import org.gbif.api.model.registry.Dataset;
import org.gbif.dwc.terms.Term;
import org.gbif.io.TabWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * An archive writer that writes entire data files at once and does not check integrity of coreids.
 * In large archives using extensions this yields a much, much better performance than writing star record by star record.
 */
public class DwcaStreamWriter implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DwcaStreamWriter.class);

    private final File dir;
    private final Term core;
    private final Term coreIdTerm;
    private final boolean useHeaders;
    private final Archive archive = new Archive();
    private Dataset metadata;
    private Map<String, Dataset> constituents = Maps.newHashMap();

    /**
     * @param dir the directory to use as the archive
     * @param coreRowType the archives core row type
     * @param coreIdTerm if given used to map the id column of the core
     * @param useHeaders if true write a single header row for each data file
     */
    public DwcaStreamWriter(File dir, Term coreRowType, @Nullable Term coreIdTerm, boolean useHeaders) {
        this.dir = dir;
        this.core = coreRowType;
        this.coreIdTerm = coreIdTerm;
        this.useHeaders = useHeaders;
        archive.setLocation(dir);
    }

    private File dataFile(Term rowType) {
        return new File(dir, rowType.simpleName()+".tsv");
    }

    private static ArchiveField idField(int column) {
        ArchiveField field = new ArchiveField();
        field.setIndex(column);
        return field;
    }
    /**
     * @param coreIdColumn zero based index to the rows coreid
     * @param mapping zero based indexed of the rows
     */
    public void write(Term rowType, int coreIdColumn, Map<Term, Integer> mapping, Iterable<String[]> rows) {
        Preconditions.checkNotNull(rows);

        final int maxMapping = maxMappingColumn(mapping);
        try (TabWriter writer = addArchiveFile(rowType, coreIdColumn, mapping, maxMapping) ) {
            // write data
            for (String[] row : rows) {
                write(writer, row, maxMapping);
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    private static void write(TabWriter writer, String[] row, int maxMapping) throws IOException {
        if (row != null && row.length < maxMapping) {
            throw new IllegalArgumentException("Input rows are smaller than the defined mapping of " + maxMapping + " columns.");
        }
        writer.write(row);
    }

    private int maxMappingColumn(Map<Term, Integer> mapping) {
        return mapping.values().stream().max(Integer::compareTo).get();
    }

    private TabWriter addArchiveFile(Term rowType, int coreIdColumn, Map<Term, Integer> mapping, int maxMapping) throws IOException {
        Preconditions.checkNotNull(rowType);
        Preconditions.checkNotNull(mapping);
        Preconditions.checkArgument(!mapping.isEmpty());
        Preconditions.checkArgument(coreIdColumn >= 0);

        final File dataFile = dataFile(rowType);
        ArchiveFile af = ArchiveFile.buildTabFile();
        af.setEncoding("UTF8");
        af.setRowType(rowType);
        af.addLocation(dataFile.getName());
        af.setIgnoreHeaderLines(useHeaders ? 1 : 0);
        af.setId(idField(coreIdColumn));
        for (Map.Entry<Term, Integer> entry : mapping.entrySet()) {
            ArchiveField field = new ArchiveField();
            field.setTerm(entry.getKey());
            field.setIndex(entry.getValue());
            af.addField(field);
        }
        if (core.equals(rowType)) {
            if (coreIdTerm != null) {
                af.getId().setTerm(coreIdTerm);
            }
            archive.setCore(af);
        } else {
            archive.addExtension(af);
        }

        // write headers
        TabWriter writer = TabWriter.fromFile(dataFile);
        if (useHeaders){
            String[] header = new String[maxMapping+1];
            mapping.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach((e)->{
                header[e.getValue()] = e.getKey().simpleName();
            });
            if (coreIdTerm != null) {
                header[0] = coreIdTerm.simpleName();
            }
            writer.write(header);
        }

        return writer;
    }

    public interface RowWriteHandler extends AutoCloseable {
        void write(String[] row);
    }

    private class RowWriteHandlerImpl implements RowWriteHandler {
        private final TabWriter writer;
        private final int minColumns;

        RowWriteHandlerImpl(TabWriter writer, int minColumns) {
            this.writer = writer;
            this.minColumns = minColumns;
        }

        public void write(String[] row) {
            if (row != null && row.length < minColumns) {
                throw new IllegalArgumentException("Input rows are smaller than the defined mapping of " + minColumns + " columns.");
            }
            try {
                writer.write(row);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }

    /**
     * Useful for glueing mybatis result handlers directly into the dwca writer.
     * Make sure to close the write handler properly!
     * @param rowType
     * @param coreIdColumn
     * @param mapping
     * @return a handler accepting single rows to write into the data file
     */
    public RowWriteHandler writeHandler(Term rowType, int coreIdColumn, Map<Term, Integer> mapping) {
        try {
            final int maxMapping = maxMappingColumn(mapping);
            TabWriter writer = addArchiveFile(rowType, coreIdColumn, mapping, maxMapping);
            return new RowWriteHandlerImpl(writer, maxMapping);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setMetadata(Dataset d) {
        metadata = d;
    }

    /**
     * Adds a constituent dataset using the dataset key as the datasetID
     */
    public void addConstituent(Dataset eml) {
        addConstituent(eml.getKey().toString(), eml);
    }

    /**
     * Adds a constituent dataset.
     * The eml file will be called as the datasetID which has to be unique.
     */
    public void addConstituent(String datasetID, Dataset eml) {
        this.constituents.put(datasetID, eml);
    }

    /**
     * Writes meta.xml and eml.xml to the archive.
     */
    @Override
    public void close() throws IOException {
        checkCoreRowType();
        addEml();
        addConstituents();
        MetaDescriptorWriter.writeMetaFile(archive);
        LOG.info("Wrote archive to {}", archive.getLocation().getAbsolutePath());
    }

    /**
     * check if core row type has been written
     */
    private void checkCoreRowType() {
        if (archive.getCore() == null) {
            throw new IllegalStateException("The core data file has not yet been written for " + core.qualifiedName());
        }
    }

    private void addEml() throws IOException {
        if (metadata != null) {
            DwcaWriter.writeEml(metadata, new File(dir, "eml.xml"));
            archive.setMetadataLocation("eml.xml");
        }
    }

    private void addConstituents() throws IOException {
        if (!constituents.isEmpty()) {
            File ddir = new File(dir, Archive.CONSTITUENT_DIR);
            ddir.mkdirs();
            for (Map.Entry<String, Dataset> de : constituents.entrySet()) {
                DwcaWriter.writeEml(de.getValue(), new File(ddir, de.getKey()+".xml"));
            }
        }
    }
}
