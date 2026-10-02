/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geotiff.reader;

import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashMap;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;
import org.apache.sis.referencing.factory.InternalFactoryException;
import org.apache.sis.math.Vector;


/**
 * The conversion or transformation from pixel coordinates to model coordinates.
 * The target CRS may be the image CRS if the image is "georeferenceable" instead of georeferenced.
 *
 * This code is provided in a separated class for making easier to move it to some shared location
 * if another data store needs similar functionality in the future.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Localization {
    /**
     * Number of floating point values in each (I,J,K,X,Y,Z) record.
     */
    static final int RECORD_LENGTH = 6;

    /**
     * The desired precision of coordinate transformations in units of pixels.
     * This is an arbitrary value that may be adjusted in any future SIS version.
     *
     * @todo compute the value based on the cell size in order to have an accuracy of about 1 cm on Earth.
     */
    private static final double PRECISION = 1E-6;

    /**
     * Do not allow instantiation of this class.
     */
    private Localization() {
    }

    /**
     * Creates a new localization grid from the information found by {@link ImageFileDirectory}.
     * Current implementation creates two-dimensional transforms only.
     *
     * @param  modelTiePoints  the tie points to use for computing {@code gridToCRS}.
     * @return the grid geometry created from above properties. Never null.
     */
    static MathTransform nonLinear(final Vector modelTiePoints) throws FactoryException, TransformException {
        return localizationGrid(modelTiePoints, null);
    }

    /**
     * Builds a localization grid from the given GeoTIFF tie points.
     * This method may invoke itself recursively.
     *
     * @param  modelTiePoints  the model tie points read from GeoTIFF file.
     * @param  addTo           if non-null, add the transform result to this map.
     * @return the "grid to CRS" transform backed by the localization grid.
     */
    private static MathTransform localizationGrid(final Vector modelTiePoints, final Map<Envelope,MathTransform> addTo)
            throws FactoryException, TransformException
    {
        final int size = modelTiePoints.size();
        final int n = size / RECORD_LENGTH;
        if (n == 0) return null;
        final Vector x = modelTiePoints.subSampling(0, RECORD_LENGTH, n);
        final Vector y = modelTiePoints.subSampling(1, RECORD_LENGTH, n);
        try {
            final LocalizationGridBuilder grid = new LocalizationGridBuilder(x, y);
            final LinearTransform sourceToGrid = grid.getSourceToGrid();
            final double[] coordinates = new double[2];
            for (int i=0; i<size; i += RECORD_LENGTH) {
                coordinates[0] = modelTiePoints.doubleValue(i);
                coordinates[1] = modelTiePoints.doubleValue(i+1);
                sourceToGrid.transform(coordinates, 0, coordinates, 0, 1);
                grid.setControlPoint(Math.toIntExact(Math.round(coordinates[0])),
                                     Math.toIntExact(Math.round(coordinates[1])),
                                     modelTiePoints.doubleValue(i + (RECORD_LENGTH/2)),
                                     modelTiePoints.doubleValue(i + (RECORD_LENGTH/2 + 1)));
            }
            grid.setDesiredPrecision(PRECISION);
            final MathTransform tr = grid.create(null);
            if (addTo != null && addTo.put(grid.getSourceEnvelope(false), tr) != null) {
                throw new InternalFactoryException();   // Should never happen. If it does, we have a bug in our algorithm.
            }
            return tr;
        } catch (ArithmeticException | FactoryException e) {
            /*
             * May happen when the model tie points are not distributed on a regular grid.
             * The tie points may nevertheless be on a rectilinear grid, i.e. every combination
             * of the distinct x and y pixel coordinates may be present exactly once, with only
             * the spacing between those coordinates being unsuitable for the above inference.
             * This is the case of ICEYE images, where the tie points are at k × (size-1) / (n-1)
             * pixels, sometimes rounded to integers. The greatest common divisor of those
             * coordinates is then much smaller than the actual step, either because the step is
             * fractional or because the rounding makes one step differ from the others by one
             * pixel. Such grids are handled without splitting them, by using the ranks of the
             * distinct coordinates as grid indices.
             */
            final MathTransform rectilinear = rectilinearGrid(modelTiePoints, x, y, addTo);
            if (rectilinear != null) {
                return rectilinear;
            }
            /*
             * Otherwise the tie points are really irregular.
             * For example, Sentinel 1 images may have tie points spaced by 1320 pixels on the X axis,
             * except the very last point which is only 1302 pixels after the previous one. We try to
             * handle such grids by splitting them in two parts: one grid for the columns where points
             * are spaced by 1320 pixels and one grid for the last column. Such splitting needs to be
             * done horizontally and vertically, which result in four grids:
             *
             *    ┌──────────────────┬───┐
             *    │                  │   │
             *    │         0        │ 1 │
             *    │                  │   │
             *    ├──────────────────┼───┤ splitY
             *    │         2        │ 3 │
             *    └──────────────────┴───┘
             *                    splitX
             *
             * If the irregular spacing is on a single axis, then the threshold of the other axis is NaN,
             * the comparisons against it are always false and only two of the four parts receive points.
             * The empty parts are skipped.
             */
            final Set<Double> uniques = new HashSet<>(100);
            final double splitX = threshold(x, uniques);
            final double splitY = threshold(y, uniques);
            if (Double.isNaN(splitX) && Double.isNaN(splitY)) {
                throw e;                                            // Cannot do better. Report the failure.
            }
            final int[][] indices = new int[4][size];
            final int[]   lengths = new int[4];
            for (int i=0; i<size;) {
                final double px = modelTiePoints.doubleValue(i  );
                final double py = modelTiePoints.doubleValue(i+1);
                int part = 0;                                       // Number of the part where to add current point.
                if (px > splitX) part  = 1;                         // Point will be added to part #1 or #3.
                if (py > splitY) part |= 2;                         // Point will be added to part #2 or #3.
                int parts = 1 << part;                              // Bitmask of the parts where to add the point.
                if (px == splitX) parts |= 1 << (part | 1);         // Add also the point to part #1 or #3.
                if (py == splitY) parts |= 1 << (part | 2);         // Add also the point to part #2 or #3.
                if (parts == 0b0111) {
                    parts = 0b1111;                                 // Add also the point to part #3.
                    assert px == splitX && py == splitY;
                }
                final int upper = i + RECORD_LENGTH;
                do {
                    part = Integer.numberOfTrailingZeros(parts);
                    @SuppressWarnings("MismatchedReadAndWriteOfArray")
                    final int[] tileIndices = indices[part];
                    int k = lengths[part];
                    for (int j=i; j<upper; j++) {
                        tileIndices[k++] = j;
                    }
                    lengths[part] = k;
                } while ((parts &= ~(1 << part)) != 0);            // Clear the bit of the part we processed.
                i = upper;
            }
            /*
             * At this point, we finished to collect indices of the points to use for parts #0, 1, 2 and 3.
             * Verify that each part has less points than the initial vector (otherwise it would be a bug),
             * and identify which part is the biggest one. This is usually part #0.
             */
            int maxLength   = 0;
            int largestPart = 0;
            for (int i=0; i<indices.length; i++) {
                final int length = lengths[i];
                if (length >= size) throw e;                        // Safety against infinite recursion.
                indices[i] = Arrays.copyOf(indices[i], length);
                if (length > maxLength) {
                    maxLength = length;
                    largestPart = i;
                }
            }
            /*
             * The biggest part will define the global transform. All other parts will define a specialization
             * valid only in a sub-area. Put those information in a map for MathTransforms.specialize(…).
             */
            MathTransform global = null;
            final Map<Envelope,MathTransform> specialization = new LinkedHashMap<>(4);
            for (int i=0; i<indices.length; i++) {
                if (indices[i].length == 0) continue;               // Part without points (see above comment).
                final Vector sub = modelTiePoints.pick(indices[i]);
                if (i == largestPart) {
                    global = localizationGrid(sub, null);
                } else {
                    localizationGrid(sub, specialization);
                }
            }
            return MathTransforms.specialize(global, specialization);
        }
    }

    /**
     * Builds a single localization grid when the given tie points are on a rectilinear grid.
     * The tie points are on a rectilinear grid if every combination of the distinct <var>x</var>
     * and <var>y</var> pixel coordinates is present exactly once, and if those coordinates are
     * evenly spaced up to a rounding to integers. The latter condition is verified by
     * {@link #isUniformAfterRounding(double[], double)}.
     *
     * <p>Contrarily to the {@code localizationGrid(…)} fallback, this method does not split the
     * tie points: the grid indices are the ranks of the distinct pixel coordinates, and the linear
     * relationship between pixel coordinates and ranks is applied before the localization grid.
     * Consequently the transform has no discontinuity and honors all tie points.</p>
     *
     * @param  modelTiePoints  the model tie points read from GeoTIFF file.
     * @param  x               the <var>x</var> pixel coordinates of the tie points.
     * @param  y               the <var>y</var> pixel coordinates of the tie points.
     * @param  addTo           if non-null, add the transform result to this map.
     * @return the "grid to CRS" transform, or {@code null} if the tie points are not on a rectilinear grid.
     */
    private static MathTransform rectilinearGrid(final Vector modelTiePoints, final Vector x, final Vector y,
            final Map<Envelope,MathTransform> addTo) throws FactoryException, TransformException
    {
        final int size = modelTiePoints.size();
        final double[] ux = distinctSorted(x);
        final double[] uy = distinctSorted(y);
        final int nx = ux.length;
        final int ny = uy.length;
        if (nx < 2 || ny < 2 || ((long) nx) * ny != size / RECORD_LENGTH) {
            return null;                                            // Not a complete rectilinear grid.
        }
        final double sx = (ux[nx-1] - ux[0]) / (nx - 1);
        final double sy = (uy[ny-1] - uy[0]) / (ny - 1);
        if (!isUniformAfterRounding(ux, sx) || !isUniformAfterRounding(uy, sy)) {
            return null;                                            // Spacing is irregular for a real reason.
        }
        final LocalizationGridBuilder grid = new LocalizationGridBuilder(nx, ny);
        for (int i=0; i<size; i += RECORD_LENGTH) {
            final int gx = Arrays.binarySearch(ux, modelTiePoints.doubleValue(i  ));
            final int gy = Arrays.binarySearch(uy, modelTiePoints.doubleValue(i+1));
            if (gx < 0 || gy < 0) {
                return null;                                        // Paranoiac safety; should never happen.
            }
            grid.setControlPoint(gx, gy, modelTiePoints.doubleValue(i + (RECORD_LENGTH/2)),
                                         modelTiePoints.doubleValue(i + (RECORD_LENGTH/2 + 1)));
        }
        grid.setDesiredPrecision(PRECISION);
        final MathTransform tr = MathTransforms.concatenate(
                MathTransforms.linear(new Matrix3(1/sx, 0, -ux[0]/sx,
                                                  0, 1/sy, -uy[0]/sy,
                                                  0, 0, 1)),
                grid.create(null));
        if (addTo != null) {
            final Envelope domain = new GeneralEnvelope(new double[] {ux[0], uy[0]},
                                                        new double[] {ux[nx-1], uy[ny-1]});
            if (addTo.put(domain, tr) != null) {
                throw new InternalFactoryException();   // Should never happen. If it does, we have a bug in our algorithm.
            }
        }
        return tr;
    }

    /**
     * Returns the sorted distinct values of the given vector.
     *
     * @param  values  the <var>x</var> or <var>y</var> vector of tie points pixel coordinates.
     * @return the distinct values, in increasing order.
     */
    private static double[] distinctSorted(final Vector values) {
        final int n = values.size();
        final Set<Double> uniques = new HashSet<>(100);
        for (int i=0; i<n; i++) {
            uniques.add(values.doubleValue(i));
        }
        final double[] array = new double[uniques.size()];
        int i = 0;
        for (final Double value : uniques) {
            array[i++] = value;
        }
        Arrays.sort(array);
        return array;
    }

    /**
     * Returns whether the given sorted distinct coordinates are evenly spaced by the given step,
     * up to a rounding to integers. This is the case of ICEYE images, where the tie points are at
     * {@code k × (size-1) / (n-1)} pixels, sometimes rounded to integers: the coordinates deviate
     * from an evenly spaced sequence by less than one pixel, and the resulting error on the
     * interpolated coordinates is a small fraction of a pixel. This is not the case of Sentinel 1
     * images, where the last step is genuinely 18 pixels shorter than the others and where
     * splitting the grid gives a better result.
     *
     * @param  values  the distinct pixel coordinates, in increasing order.
     * @param  step    the average step between two consecutive values.
     * @return whether the values are evenly spaced up to a rounding to integers.
     */
    private static boolean isUniformAfterRounding(final double[] values, final double step) {
        for (int i = values.length - 1; --i >= 1;) {
            if (!(Math.abs(values[i] - (values[0] + i*step)) < 1)) {
                return false;                                       // Use `!` for catching NaN.
            }
        }
        return true;
    }

    /**
     * Finds the value at which the increment in localization grid seems to change.
     * This is used when not all tie points in a GeoTIFF images are distributed on
     * a regular grid (e.g. Sentinel 1 image). This method tells where to split in
     * two grids.
     *
     * @param  values   the x or y vector of tie points pixel coordinates.
     * @param  uniques  an initially empty set to be used for this method internal working.
     * @return value after which a different step is used, or {@code NaN} if none.
     *         The value returned by this method should be included in the first grid.
     */
    private static double threshold(final Vector values, final Set<Double> uniques) {
        final int n = values.size();
        for (int i=0; i<n; i++) {
            uniques.add(values.doubleValue(i));
        }
        final Double[] array = uniques.toArray(Double[]::new);
        uniques.clear();
        int i = array.length;
        if (i >= 3) {
            Arrays.sort(array);
            double value;
            final double inc = array[--i] - (value = array[--i]);
            do {
                final double lower = array[--i];
                if (value - lower != inc) {
                    return value;
                }
                value = lower;
            } while (i > 0);
        }
        return Double.NaN;
    }
}
