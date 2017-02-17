package org.gbif.dwca.io;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.gbif.api.model.registry.Dataset;
import org.gbif.dwc.terms.Term;
import org.gbif.io.TabWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * An archive writer that writes entire data files at once and does not check integrity of coreids.
 * In large archives using extensions this yields a much, much better performance than writing star record by star record.
 */
public class DwcaStreamWriter implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DwcaStreamWriter.class);

    private final File dir;
    private final Term core;
    private final boolean useHeaders;
    private final Archive archive = new Archive();
    private Dataset metadata;
    private Map<String, Dataset> constituents = Maps.newHashMap();

    /**
     * @param dir the directory to use as the archive
     * @param coreRowType the archives core row type
     * @param useHeaders if true write a single header row for each data file
     */
    public DwcaStreamWriter(File dir, Term coreRowType, boolean useHeaders) {
        this.dir = dir;
        this.core = coreRowType;
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
        Preconditions.checkNotNull(rowType);
        Preconditions.checkNotNull(mapping);
        Preconditions.checkNotNull(rows);
        Preconditions.checkArgument(!mapping.isEmpty());
        Preconditions.checkArgument(coreIdColumn >= 0);

        final File df = dataFile(rowType);
        final int maxMapping = mapping.values().stream().max(Integer::compareTo).get();
        try (TabWriter writer = TabWriter.fromFile(df) ) {
            ArchiveFile af = ArchiveFile.buildTabFile();
            af.setEncoding("UTF8");
            af.setRowType(rowType);
            af.addLocation(df.getName());
            af.setIgnoreHeaderLines(useHeaders ? 1 : 0);
            af.setId(idField(coreIdColumn));
            for (Map.Entry<Term, Integer> entry : mapping.entrySet()) {
                ArchiveField field = new ArchiveField();
                field.setTerm(entry.getKey());
                field.setIndex(entry.getValue());
                af.addField(field);
            }
            if (core.equals(rowType)) {
                archive.setCore(af);
            } else {
                archive.addExtension(af);
            }
            // write data
            if (useHeaders){
                String[] header = new String[maxMapping+1];
                mapping.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach((e)->{
                    header[e.getValue()] = e.getKey().simpleName();
                });
                writer.write(header);
            }
            // write data
            for (String[] row : rows) {
                if (row != null && row.length < maxMapping) {
                    throw new IllegalArgumentException("Input rows are smaller than the defined mapping of " + maxMapping + " columns.");
                }
                writer.write(row);
            }

        } catch (FileNotFoundException e) {
            Throwables.propagate(e);

        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public void setMetadata(Dataset d) {
        metadata = d;
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
