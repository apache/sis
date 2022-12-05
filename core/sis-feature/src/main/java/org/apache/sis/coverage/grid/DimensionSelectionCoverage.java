package org.apache.sis.coverage.grid;

import java.awt.image.RenderedImage;
import org.apache.sis.internal.coverage.grid.GridDimensionSelection;
import org.opengis.coverage.CannotEvaluateException;

class DimensionSelectionCoverage extends DerivedGridCoverage {
    private final GridDimensionSelection.Specification spec;

    DimensionSelectionCoverage(GridCoverage source, GridDimensionSelection.Specification spec) {
        super(source, spec.getReducedGridGeometry());
        this.spec = spec;
    }

    @Override
    public RenderedImage render(GridExtent sliceExtent) throws CannotEvaluateException {
        if (sliceExtent == null) return source.render(null);
        else return source.render(spec.reverse(sliceExtent));
    }
}
