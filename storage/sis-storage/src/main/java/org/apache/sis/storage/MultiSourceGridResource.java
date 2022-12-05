package org.apache.sis.storage;

import java.util.List;
import java.util.Optional;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

abstract class MultiSourceGridResource implements GridCoverageResource {

    private final GenericName name;

    /**
     *
     * @param name Optional. The {@link #getIdentifier() identifier} of this resource.
     */
    MultiSourceGridResource(GenericName name) {
        this.name = name;
    }

    abstract List<GridCoverageResource> sources();
    @Override
    public Optional<Envelope> getEnvelope() { return Optional.empty(); }

    @Override
    public Optional<GenericName> getIdentifier() { return Optional.ofNullable(name); }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        MetadataBuilder builder = new MetadataBuilder();
        builder.addSpatialRepresentation(null, getGridGeometry(), false);
        for (GridCoverageResource source : sources()) {
            // TODO: not sure it is the right thing to do. I'm a little afraid of the performance impact.
            builder.addSource(source.getMetadata());
        }

        return builder.buildAndFreeze();
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {}

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {}
}
