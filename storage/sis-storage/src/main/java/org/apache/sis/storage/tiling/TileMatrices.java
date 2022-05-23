/*
 * (C) 2022, Geomatys
 */
package org.apache.sis.storage.tiling;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.LongStream;
import org.apache.sis.coverage.grid.GridClippingMode;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;

/**
 * TileMatrix utilities.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.3
 * @since   1.3
 */
public final class TileMatrices extends Static {

    private TileMatrices(){}

    /**
     * Compute tiling scheme of a list of tile geometries.
     *
     * @param grids tile grid geometries
     * @return tiling scheme and map of each tile indices or empty if any tile do not fit in the scheme.
     */
    public static Optional<Entry<GridGeometry, Map<GridGeometry,long[]>>> toTilingScheme(GridGeometry... grids) {

        final GridGeometry first = grids[0];
        GridGeometry tilingScheme;

        {   // Create a first tiling scheme, pick the first grid as a tile reference

            if (!first.isDefined(GridGeometry.EXTENT | GridGeometry.CRS | GridGeometry.GRID_TO_CRS)) {
                //we don't have enough informations to compute the tiling scheme
                return Optional.empty();
            }
            final MathTransform firstGridToCRS = first.getGridToCRS(PixelInCell.CELL_CENTER);
            if (!(firstGridToCRS instanceof LinearTransform) || !((LinearTransform) firstGridToCRS).isAffine()) {
                //detection works only for affine transforms
                return Optional.empty();
            }

            //create a first tiling scheme
            final GridGeometry forceLowerToZero = forceLowerToZero(first);
            final int[] subsampling = LongStream.of(forceLowerToZero.getExtent().getHigh().getCoordinateValues())
                    .mapToInt(Math::toIntExact)
                    .map((int operand) -> operand+1) //high values are inclusive
                    .toArray();
            tilingScheme = forceLowerToZero.derive().subgrid(null, subsampling).build();
        }

        //compute all tile indices
        final Map<GridGeometry,long[]> tiles = new HashMap<>();
        final int dimension = tilingScheme.getExtent().getDimension();
        tiles.put(first, tilingScheme.getExtent().getLow().getCoordinateValues());
        final long[] min = new long[dimension];
        final long[] max = new long[dimension];
        for (int i = 1; i < grids.length; i++) {
            final Optional<long[]> indice = getTileIndices(first, tilingScheme, grids[i]);
            if (!indice.isPresent()) return Optional.empty();
            long[] r = indice.get();
            tiles.put(grids[i], r);

            //keep track of min/max range
            for (int k = 0; k < dimension; k++) {
                min[k] = Math.min(min[k], r[k]);
                max[k] = Math.max(max[k], r[k]);
            }
        }

        //rebuild the tiling scheme extent to contain all tiles
        tilingScheme = new GridGeometry(
                new GridExtent(null, min, max, true),
                PixelInCell.CELL_CENTER,
                tilingScheme.getGridToCRS(PixelInCell.CELL_CENTER),
                tilingScheme.getCoordinateReferenceSystem());
        tilingScheme = forceLowerToZero(tilingScheme);

        //offset all indices
        for (Entry<GridGeometry,long[]> entry : tiles.entrySet()) {
            final long[] indices = entry.getValue();
            for (int i = 0; i < dimension; i++) {
                indices[i] -= min[i];
            }
        }

        return Optional.of(new AbstractMap.SimpleImmutableEntry<>(tilingScheme, tiles));
    }

    /**
     * Find tile indice in given tiling scheme.
     *
     * @param referenceTile a valid tile used a reference.
     * @param tilingScheme the tiling scheme geometry.
     * @param tileGrid searched tile grid geometry.
     * @return tile index or empty if tile do not fit in the scheme.
     */
    private static Optional<long[]> getTileIndices(GridGeometry referenceTile, GridGeometry tilingScheme, GridGeometry tileGrid) {
        if (!tileGrid.isDefined(GridGeometry.EXTENT | GridGeometry.CRS | GridGeometry.GRID_TO_CRS)) {
            //we don't have enough informations to compute the tile indices
            return Optional.empty();
        }
        if (!Utilities.equalsIgnoreMetadata(referenceTile.getCoordinateReferenceSystem(), tileGrid.getCoordinateReferenceSystem())) {
            //tile candidate has different CRS
            return Optional.empty();
        }
        final MathTransform gridToCRS = tileGrid.getGridToCRS(PixelInCell.CELL_CENTER);
        if (!(gridToCRS instanceof LinearTransform) || !((LinearTransform) gridToCRS).isAffine()) {
            //indice computation works only for affine transforms
            return Optional.empty();
        }

        //matrices must differ only by the last column (translation)
        final LinearTransform firstLinear = (LinearTransform) referenceTile.getGridToCRS(PixelInCell.CELL_CENTER);
        final Matrix matrix1 = firstLinear.getMatrix();
        final LinearTransform linear2 = (LinearTransform) gridToCRS;
        final Matrix matrix2 = linear2.getMatrix();
        for (int x = 0, xn = matrix1.getNumCol() - 1, yn = matrix1.getNumRow(); x < xn; x++) {
            for (int y = 0; y < yn; y++) {
                if (matrix1.getElement(y, x) != matrix2.getElement(y, x)) {
                    return Optional.empty();
                }
            }
        }

        //tiles must have the same extent size
        final GridExtent referenceExtent = referenceTile.getExtent();
        final GridExtent candidateExtent = tileGrid.getExtent();
        for (int i = 0, n = referenceExtent.getDimension(); i < n; i++) {
            if (referenceExtent.getSize(i) != candidateExtent.getSize(i)) {
                return Optional.empty();
            }
        }

        //compute the tile indice
        final GridExtent intersection = tilingScheme.derive().clipping(GridClippingMode.NONE).rounding(GridRoundingMode.ENCLOSING).subgrid(tileGrid).getIntersection();
        final long[] low = intersection.getLow().getCoordinateValues();
        final long[] high = intersection.getHigh().getCoordinateValues();

        //if tile overlaps several indices then it's not part of the tiling scheme
        if (!Arrays.equals(low, high)) {
            return Optional.empty();
        }

        return Optional.of(low);
    }

    /**
     * Shift lower extent to zero.
     */
    private static GridGeometry forceLowerToZero(final GridGeometry gg) {
        final GridExtent extent = gg.getExtent();
        if (!extent.startsAtZero()) {
            CoordinateReferenceSystem crs = null;
            if (gg.isDefined(GridGeometry.CRS)) crs = gg.getCoordinateReferenceSystem();
            final int dimension = extent.getDimension();
            final double[] vector = new double[dimension];
            final long[] high = new long[dimension];
            for (int i = 0; i < dimension; i++) {
                final long low = extent.getLow(i);
                high[i] = extent.getHigh(i) - low;
                vector[i] = low;
            }
            MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CENTER);
            gridToCRS = MathTransforms.concatenate(MathTransforms.translation(vector), gridToCRS);
            return new GridGeometry(new GridExtent(null, null, high, true), PixelInCell.CELL_CENTER, gridToCRS, crs);
        }
        return gg;
    }

}
