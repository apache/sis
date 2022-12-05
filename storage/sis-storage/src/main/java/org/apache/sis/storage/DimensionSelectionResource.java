package org.apache.sis.storage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.coverage.grid.GridDimensionSelection;
import org.apache.sis.internal.storage.DerivedGridCoverageResource;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.util.GenericName;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

class DimensionSelectionResource extends DerivedGridCoverageResource {

    private final GridCoverageProcessor processor;
    private final GridDimensionSelection.Specification spec;

    protected DimensionSelectionResource(GenericName name, GridCoverageResource source, GridDimensionSelection.Specification spec, GridCoverageProcessor processor) {
        super(name, source);
        ensureNonNull("Specification", spec);
        this.spec = spec;
        this.processor = processor;
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return spec.getReducedGridGeometry();
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        if (domain == null) domain = spec.getSourceGeometry();
        else {
            domain = spec.getReducedGridGeometry().derive().subgrid(domain).build();
            try {
                domain = spec.reverse(domain);
            } catch (NoninvertibleTransformException e) {
                throw new BackingStoreException("Cannot determine source geometry from reduced one", e);
            }
        }

        final GridCoverage sourceData = source.read(domain, ranges);
        return processor.selectDimensions(sourceData, spec.getGridAxesToPreserve());
    }
}
