package org.apache.sis.internal.coverage.grid;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.opengis.referencing.datum.PixelInCell.CELL_CENTER;

/**
 * Provide utility methods to reduce/select dimensions of a grid-geometry, and provide an object holding information
 * needed to travel between source to reduced space.
 */
public final class GridDimensionSelection extends Static {
    private GridDimensionSelection() {}

    public static Optional<Specification> squeeze(GridGeometry source) {
        final int[] axesToPreserve = findUnsqueezableDimensions(source);
        if (axesToPreserve.length == source.getExtent().getDimension()) return Optional.empty();
        else if (axesToPreserve.length == 0) throw new IllegalArgumentException("All input grid dimensions are squeezable. Squeezing it would degenerate to a 0 dimension grid.");
        else return Optional.of(preserve(source, axesToPreserve));
    }

    public static Specification remove(GridGeometry source, int... gridAxesToRemove) {
        return preserve(source, reverse(source, gridAxesToRemove));
    }

    public static Specification preserve(GridGeometry source, int... gridAxesToPreserve) {
        ensureNonNull("Source", source);
        final GridExtent extent = source.getExtent();
        ArgumentChecks.ensureNonEmpty("Grid axes to preserve", gridAxesToPreserve, 0, extent.getDimension(), true);
        Arrays.sort(gridAxesToPreserve);
        final GridGeometry reducedGeom = source.selectDimensions(gridAxesToPreserve);

        final int sourceDim = extent.getDimension();
        final int targetDim = gridAxesToPreserve.length;
        int newSpaceIdx = 0;
        final MatrixSIS mat = Matrices.create(sourceDim + 1, targetDim + 1, new double[Math.multiplyExact(sourceDim + 1, targetDim + 1)]);
        mat.setElement(sourceDim, targetDim, 1.0);
        for (int row = 0 ; row < sourceDim ; row++) {
            if (Arrays.binarySearch(gridAxesToPreserve, row) >= 0) {
                mat.setElement(row, newSpaceIdx++, 1.0);
            } else {
                mat.setElement(row, targetDim, extent.getLow(row));
            }
        }
        final LinearTransform reducedToOrigin = MathTransforms.linear(mat);
        return new Specification(reducedGeom, gridAxesToPreserve, reducedToOrigin, source);
    }

    public static class Specification {
        private final GridGeometry reducedGridGeometry;
        private final int[] gridAxesToPreserve;
        private final LinearTransform rollbackAxes;
        private final GridGeometry sourceGeometry;

        public Specification(GridGeometry reducedGridGeometry, int[] gridAxesToPreserve, LinearTransform rollbackAxes, GridGeometry sourceGeometry) {
            this.reducedGridGeometry = reducedGridGeometry;
            this.gridAxesToPreserve = gridAxesToPreserve;
            this.rollbackAxes = rollbackAxes;
            this.sourceGeometry = sourceGeometry;
        }

        public GridGeometry getReducedGridGeometry() {
            return reducedGridGeometry;
        }

        public int[] getGridAxesToPreserve() {
            return gridAxesToPreserve.clone();
        }

        public LinearTransform getRollbackAxes() {
            return rollbackAxes;
        }

        public GridGeometry getSourceGeometry() {
            return sourceGeometry;
        }

        public GridExtent reverse(GridExtent extent) {
            final GridExtent sourceExtent = sourceGeometry.getExtent();
            final long[] newLow  = sourceExtent.getLow().getCoordinateValues();
            final long[] newHigh = sourceExtent.getHigh().getCoordinateValues();
            for (int i = 0 ; i < gridAxesToPreserve.length ; i++) {
                int j = gridAxesToPreserve[i];
                newLow[j] = extent.getLow(i);
                newHigh[j] = extent.getHigh(i);
            }
            return new GridExtent(null, newLow, newHigh, true);
        }

        public GridGeometry reverse(GridGeometry domain) throws NoninvertibleTransformException {
            if (domain.isDefined(GridGeometry.CRS) && !Utilities.equalsIgnoreMetadata(reducedGridGeometry.getCoordinateReferenceSystem(), domain.getCoordinateReferenceSystem())) {
                throw new IllegalArgumentException("Input geometry CRS must match this specification CRS");
            }

            final MathTransform inflatedGridToCrs;
            if (domain.isDefined(GridGeometry.GRID_TO_CRS)) {
                inflatedGridToCrs = null;
            } else if (Utilities.equalsIgnoreMetadata(domain.getGridToCRS(CELL_CENTER), reducedGridGeometry.getGridToCRS(CELL_CENTER))) {
                inflatedGridToCrs = sourceGeometry.getGridToCRS(CELL_CENTER);
            } else {
                final MathTransform reducedToSource = MathTransforms.concatenate(
                        reducedGridGeometry.getGridToCRS(CELL_CENTER).inverse(),
                        rollbackAxes,
                        sourceGeometry.getGridToCRS(CELL_CENTER)
                );

                inflatedGridToCrs = MathTransforms.concatenate(
                        rollbackAxes.inverse(),
                        domain.getGridToCRS(CELL_CENTER),
                        reducedToSource
                );
            }

            final CoordinateReferenceSystem inflatedCrs = sourceGeometry.isDefined(GridGeometry.CRS) ? sourceGeometry.getCoordinateReferenceSystem() : null;
            return new GridGeometry(reverse(domain.getExtent()), CELL_CENTER, inflatedGridToCrs, inflatedCrs);
        }
    }

    private static int[] findUnsqueezableDimensions(GridGeometry sourceGeom) {
        final GridExtent extent = sourceGeom.getExtent();
        return IntStream.range(0, extent.getDimension())
                .filter(i -> extent.getSize(i) > 1)
                .toArray();
    }

    private static int[] reverse(GridGeometry source, int[] axes) {
        final int[] sorted = axes.clone();
        Arrays.sort(sorted);

        return IntStream.range(0, source.getExtent().getDimension())
                .filter(i -> Arrays.binarySearch(sorted, i) < 0)
                .toArray();
    }
}
