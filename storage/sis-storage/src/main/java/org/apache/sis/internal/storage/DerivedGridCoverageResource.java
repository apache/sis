package org.apache.sis.internal.storage;

import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

public abstract class DerivedGridCoverageResource implements GridCoverageResource {

    protected final GenericName name;
    protected final GridCoverageResource source;

    protected DerivedGridCoverageResource(GenericName name, GridCoverageResource source) {
        this.name = name;
        ensureNonNull("Source", source);
        this.source = source;
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException { return source.getEnvelope(); }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException { return source.getGridGeometry(); }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException { return source.getSampleDimensions(); }

    @Override
    public RasterLoadingStrategy getLoadingStrategy() throws DataStoreException { return source.getLoadingStrategy(); }

    @Override
    public boolean setLoadingStrategy(RasterLoadingStrategy strategy) throws DataStoreException { return source.setLoadingStrategy(strategy); }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException { return Optional.ofNullable(name); }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addSpatialRepresentation(null, getGridGeometry(), false);
        builder.addSource(source.getMetadata());
        return builder.buildAndFreeze();
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        /*
         * TODO: for now, consider it a no-op. Plugging directly into source might be a bad idea.
         *  1. We do not know in advance what modifications are done over the source.
         *     Therefore, we do not know how events should be amended to reflect the resource derivation.
         *     For now, make derived resource not listenable by default.
         *  2. We do not know if the same listener is already registered on source, and simply passing the listener
         *     to the source might cause redondant work.
         * Each implementation is free to implement it as it see fit.
         */
    }

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        // See addListener to know why it is a no-op by default.
    }
}
