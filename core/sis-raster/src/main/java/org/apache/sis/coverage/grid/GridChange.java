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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import java.util.Locale;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;


/**
 * Information about the conversion from one grid geometry to another grid geometry.
 * This class holds the {@link MathTransform}s converting cell coordinates from the
 * source grid to cell coordinates in the target grid, together with the grid extent
 * that results from this conversion.
 *
 * <div class="note"><b>Usage:</b>
 * This class can be helpful for implementation of
 * {@link org.apache.sis.storage.GridCoverageResource#read(GridGeometry, int...)}.
 * Example:
 *
 * {@preformat java
 *     class MyDataStorage extends GridCoverageResource {
 *         &#64;Override
 *         public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
 *             GridChange change = new GridChange(domain, getGridGeometry());
 *             GridExtent toRead = change.getTargetExtent();
 *             int[] subsampling = change.getTargetSubsamplings());
 *             // Do reading here.
 *         }
 *     }
 * }
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class GridChange implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7819047186271172885L;

    /**
     * The conversions from source grid to target grid, mapping cell corners or cell centers.
     */
    private final MathTransform mapCorners, mapCenters;

    /**
     * Intersection of the two grid geometry extents, in units of the target grid geometry.
     * This is the expected ranges after conversions from source grid coordinates to target
     * grid coordinates, clipped to target extent and ignoring {@linkplain #scales}.
     *
     * @see #getTargetExtent()
     */
    private final GridExtent targetExtent;

    /**
     * The target grid geometry specified to the constructor. The extent of that geometry
     * is not necessarily the {@link #targetExtent}.
     *
     * @see #getTargetGeometry(int...)
     */
    private final GridGeometry givenGeometry;

    /**
     * An estimation of the multiplication factors when converting cell coordinates from source
     * grid to target grid. Those factors appear in the order of <em>target</em> grid axes.
     * May be {@code null} if the conversion is identity.
     *
     * @see #getTargetSubsamplings()
     */
    private final double[] scales;

    /**
     * Creates a description of the conversion from given source grid geometry to given target grid geometry.
     * The following information are mandatory:
     * <ul>
     *   <li>Source {@linkplain GridGeometry#getExtent() extent}.</li>
     *   <li>Source "{@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS}" conversion.</li>
     *   <li>Target "grid to CRS" conversion.</li>
     * </ul>
     *
     * The following information are optional but recommended:
     * <ul>
     *   <li>Source coordinate reference system.</li>
     *   <li>Target {@linkplain GridGeometry#getCoordinateReferenceSystem() coordinate reference system}.</li>
     *   <li>Target {@linkplain GridGeometry#getExtent() extent}.</li>
     * </ul>
     *
     * This constructor assumes {@link GridRoundingMode#ENCLOSING} and no margin.
     *
     * @param  source  the source grid geometry.
     * @param  target  the target grid geometry.
     * @throws IncompleteGridGeometryException if a "grid to CRS" conversion is not defined,
     *         or if the {@code source} does not specify an {@linkplain GridGeometry#getExtent() extent}.
     * @throws TransformException if an error occurred during conversion from source to target.
     */
    public GridChange(final GridGeometry source, final GridGeometry target) throws TransformException {
        this(source, target, GridRoundingMode.ENCLOSING, null);
    }

    /**
     * Creates a description of the conversion from given source grid geometry to given target grid geometry.
     * The following information are mandatory:
     * <ul>
     *   <li>Source {@linkplain GridGeometry#getExtent() extent}.</li>
     *   <li>Source "{@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS}" conversion.</li>
     *   <li>Target "grid to CRS" conversion.</li>
     * </ul>
     *
     * The following information are optional but recommended:
     * <ul>
     *   <li>Source coordinate reference system.</li>
     *   <li>Target {@linkplain GridGeometry#getCoordinateReferenceSystem() coordinate reference system}.</li>
     *   <li>Target {@linkplain GridGeometry#getExtent() extent}.</li>
     * </ul>
     *
     * An optional {@code margin} can be specified for increasing the size of the grid extent computed by this constructor.
     * For example if the caller wants to apply bilinear interpolations in an image, it will needs 1 more pixel on each
     * image border. If the caller wants to apply bi-cubic interpolations, it will needs 2 more pixels on each image border.
     * If the {@code margin} array length is shorter than the target dimension, then zero is assumed for all missing dimensions.
     *
     * @param  source     the source grid geometry.
     * @param  target     the target grid geometry.
     * @param  rounding   controls behavior of rounding from floating point values to integers.
     * @param  margin     if non-null, expand the extent by that amount of cells on each target dimension.
     * @throws IncompleteGridGeometryException if a "grid to CRS" conversion is not defined,
     *         or if the {@code source} does not specify an {@linkplain GridGeometry#getExtent() extent}.
     * @throws TransformException if an error occurred during conversion from source to target.
     */
    public GridChange(final GridGeometry source, final GridGeometry target, final GridRoundingMode rounding, final int... margin)
            throws TransformException
    {
        ArgumentChecks.ensureNonNull("source",   source);
        ArgumentChecks.ensureNonNull("target",   target);
        ArgumentChecks.ensureNonNull("rounding", rounding);
        givenGeometry = target;
        if (target.equals(source)) {
            // Optimization for a common case.
            mapCorners = mapCenters = MathTransforms.identity(target.getDimension());
            targetExtent = target.getExtent();                                        // May throw IncompleteGridGeometryException.
        } else {
            final CoordinateOperation crsChange;
            try {
                crsChange = Envelopes.findOperation(source.envelope, target.envelope);
            } catch (FactoryException e) {
                throw new TransformException(Errors.format(Errors.Keys.CanNotTransformEnvelope), e);
            }
            final GridExtent domain = source.getExtent();                             // May throw IncompleteGridGeometryException.
            mapCorners = path(source, crsChange, target, PixelInCell.CELL_CORNER);
            mapCenters = path(source, crsChange, target, PixelInCell.CELL_CENTER);
            final GridExtent extent = new GridExtent(domain.toCRS(mapCorners, mapCenters), rounding, margin, target.extent, null);
            targetExtent = extent.equals(target.extent) ? target.extent : extent.equals(domain) ? domain : extent;
            /*
             * Get an estimation of the scale factors when converting from source to target.
             * If all scale factors are 1, we will not store the array for consistency with
             * above block for identity case.
             */
            final double[] resolution = GridGeometry.resolution(mapCenters, domain);
            for (int i=resolution.length; --i >= 0;) {
                if (resolution[i] != 1) {
                    scales = resolution;
                    return;
                }
            }
        }
        scales = null;
    }

    /**
     * Returns the concatenation of all transformation steps from the given source to the given target.
     *
     * @param  source     the source grid geometry.
     * @param  crsChange  the change of coordinate reference system, or {@code null} if none.
     * @param  target     the target grid geometry.
     * @param  anchor     whether we want the transform for cell corner or cell center.
     */
    private static MathTransform path(final GridGeometry source, final CoordinateOperation crsChange,
            final GridGeometry target, final PixelInCell anchor) throws NoninvertibleTransformException
    {
        MathTransform step1 = source.getGridToCRS(anchor);
        MathTransform step2 = target.getGridToCRS(anchor);
        if (crsChange != null) {
            step1 = MathTransforms.concatenate(step1, crsChange.getMathTransform());
        }
        if (step1.equals(step2)) {                                          // Optimization for a common case.
            return MathTransforms.identity(step1.getSourceDimensions());
        } else {
            return MathTransforms.concatenate(step1, step2.inverse());
        }
    }

    /**
     * Returns an <em>estimation</em> of the scale factor when converting source grid coordinates
     * to target grid coordinates. This is for information purpose only since this method combines
     * potentially different scale factors for all dimensions.
     *
     * @return an <em>estimation</em> of the scale factor for all dimensions.
     */
    public double getGlobalScale() {
        if (scales != null) {
            double sum = 0;
            int count = 0;
            for (final double value : scales) {
                if (Double.isFinite(value)) {
                    sum += value;
                    count++;
                }
            }
            if (count != 0) {
                return sum / count;
            }
        }
        return 1;
    }

    /**
     * Returns the conversion from source grid coordinates to target grid coordinates.
     * The {@code anchor} argument specifies whether the conversion maps cell centers
     * or cell corners of both grids.
     *
     * @param  anchor  the cell part to map (center or corner).
     * @return conversion from source grid coordinates to target grid coordinates.
     *
     * @see GridGeometry#getGridToCRS(PixelInCell)
     */
    public MathTransform getConversion(final PixelInCell anchor) {
        if (PixelInCell.CELL_CENTER.equals(anchor)) {
            return mapCenters;
        } else if (PixelInCell.CELL_CORNER.equals(anchor)) {
            return mapCorners;
        }  else {
            return PixelTranslation.translate(mapCenters, PixelInCell.CELL_CENTER, anchor);
        }
    }

    /**
     * Returns the intersection of the two grid geometry extents, in units of the target grid cells.
     * This is the expected ranges of grid coordinates after conversions from source to target grid,
     * clipped to target grid extent and ignoring {@linkplain #getTargetSubsamplings() subsamplings}.
     *
     * @return intersection of grid geometry extents in units of target cells.
     */
    public GridExtent getTargetExtent() {
        return targetExtent;
    }

    /**
     * Returns an <em>estimation</em> of the steps for accessing cells along each axis of target grid.
     * Given a {@linkplain #getConversion(PixelInCell) conversion} from source grid coordinates
     * (<var>x</var>, <var>y</var>, <var>z</var>) to target grid coordinates
     * (<var>x′</var>, <var>y′</var>, <var>z′</var>) defined as below (generalize to as many dimensions as needed):
     *
     * <ul>
     *   <li><var>x′</var> = s₀⋅<var>x</var></li>
     *   <li><var>y′</var> = s₁⋅<var>y</var></li>
     *   <li><var>z′</var> = s₂⋅<var>z</var></li>
     * </ul>
     *
     * Then this method returns {|s₀|, |s₁|, |s₂|} rounded toward zero and clamped to 1
     * (i.e. all values in the returned array are strictly positive, no zero values).
     * It means that an iteration over source grid coordinates with a step Δ<var>x</var>=1
     * corresponds approximately to an iteration in target grid coordinates with a step of Δ<var>x′</var>=s₀,
     * a step Δ<var>y</var>=1 corresponds approximately to a step Δ<var>y′</var>=s₁, <i>etc.</i>
     * If the conversion changes grid axis order, then the order of elements in the returned array
     * is the order of axes in the {@linkplain #getTargetExtent() target range}.
     *
     * <p>In a <em>inverse</em> conversion from target to source grid, the value returned by this
     * method would be the subsampling to apply while reading the target grid.</p>
     *
     * @return an <em>estimation</em> of the steps for accessing cells along each axis of target range.
     */
    public int[] getTargetSubsamplings() {
        final int[] subsamplings;
        if (scales == null) {
            subsamplings = new int[targetExtent.getDimension()];
            Arrays.fill(subsamplings, 1);
        } else {
            subsamplings = new int[scales.length];
            for (int i=0; i<subsamplings.length; i++) {
                subsamplings[i] = Math.max(1, (int) Math.nextUp(scales[i]));    // Really want rounding toward 0.
            }
        }
        return subsamplings;
    }

    /**
     * Returns the grid geometry resulting from subsampling the target grid with the given periods.
     * The {@code periods} argument is usually the array returned by {@link #getTargetSubsamplings()}, but
     * not necessarily. The {@linkplain GridGeometry#getExtent() extent} of the returned grid geometry will
     * be derived from {@link #getTargetExtent()} as below for each dimension <var>i</var>:
     *
     * <ul>
     *   <li>The {@linkplain GridExtent#getLow(int)  low}  is divided by {@code periods[i]}, rounded toward zero.</li>
     *   <li>The {@linkplain GridExtent#getSize(int) size} is divided by {@code periods[i]}, rounded toward zero.</li>
     *   <li>The {@linkplain GridExtent#getHigh(int) high} is recomputed from above low and size.</li>
     * </ul>
     *
     * The {@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS} transform is scaled accordingly
     * in order to map approximately to the same {@linkplain GridGeometry#getEnvelope() envelope}.
     *
     * @param  periods  the subsampling to apply on each grid dimension. All values shall be greater than zero.
     *         If the array length is shorter than the number of dimensions, missing values are assumed to be 1.
     * @return a grid geometry derived from the target geometry with the given subsampling.
     * @throws TransformException if an error occurred during computation of target grid geometry.
     */
    public GridGeometry getTargetGeometry(final int... periods) throws TransformException {
        GridExtent extent = getTargetExtent();
        double[] factors = null;
        Matrix toGiven = null;
        if (periods != null) {
            // Validity of the periods values will be verified by GridExtent.subsampling(…) invoked below.
            final GridExtent unscaled = extent;
            final int dimension = extent.getDimension();
            for (int i = Math.min(dimension, periods.length); --i >= 0;) {
                final int s = periods[i];
                if (s != 1) {
                    if (factors == null) {
                        extent = extent.subsample(periods);
                        factors = new double[dimension];
                        Arrays.fill(factors, 1);
                        if (!extent.startsAtZero()) {
                            toGiven = Matrices.createIdentity(dimension + 1);
                        }
                    }
                    final double sd = s;
                    factors[i] = sd;
                    if (toGiven != null) {
                        toGiven.setElement(i, i, sd);
                        toGiven.setElement(i, dimension, unscaled.getLow(i) - extent.getLow(i) * sd);
                    }
                }
            }
        }
        final MathTransform mt;
        if (factors == null) {
            if (extent == givenGeometry.extent) {
                return givenGeometry;
            }
            mt = null;
        } else {
            mt = (toGiven != null) ? MathTransforms.linear(toGiven) : MathTransforms.scale(factors);
        }
        /*
         * Assuming:
         *
         *   • All low coordinates = 0
         *   • h₁ the high coordinate before subsampling
         *   • h₂ the high coordinates after subsampling
         *   • c  a conversion factor from grid indices to "real world" coordinates
         *   • s  a subsampling ≧ 1
         *
         * Then the envelope upper bounds x is:
         *
         *   • x = (h₁ + 1) × c
         *   • x = (h₂ + f) × c⋅s      which implies       h₂ = h₁/s      and       f = 1/s
         *
         * If we modify the later equation for integer division instead than real numbers, we have:
         *
         *   • x = (h₂ + f) × c⋅s      where        h₂ = floor(h₁/s)      and       f = ((h₁ mod s) + 1)/s
         *
         * Because s ≧ 1, then f ≦ 1. But the f value actually used by GridExtent.toCRS(…) is hard-coded to 1
         * since it assumes that all cells are whole, i.e. it does not take in account that the last cell may
         * actually be fraction of a cell. Since 1 ≧ f, the computed envelope may be larger. This explains the
         * need for envelope clipping performed by GridGeometry constructor.
         */
        return new GridGeometry(givenGeometry, extent, mt);
    }

    /**
     * Returns a hash code value for this grid change.
     *
     * @return hash code value.
     */
    @Override
    public int hashCode() {
        /*
         * The mapCorners is closely related to mapCenters, so we omit it.
         * The scales array is derived from mapCenters, so we omit it too.
         */
        return mapCorners.hashCode() + 59 * targetExtent.hashCode();
    }

    /**
     * Compares this grid change with the given object for equality.
     *
     * @param  other  the other object to compare with this grid change.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other != null && other.getClass() == getClass()) {
            final GridChange that = (GridChange) other;
            return mapCorners.equals(that.mapCorners) &&
                   mapCenters.equals(that.mapCenters) &&
                   targetExtent.equals(that.targetExtent) &&
                   Arrays.equals(scales, that.scales);
        }
        return false;
    }

    /**
     * Returns a tree representation of this grid change. The tree representation
     * is for debugging purpose only and may change in any future SIS version.
     *
     * @param  locale  the locale to use for textual labels.
     * @return a tree representation of this grid change.
     */
    @Debug
    private TreeTable toTree(final Locale locale) {
        final TableColumn<CharSequence> column = TableColumn.VALUE_AS_TEXT;
        final TreeTable tree = new DefaultTreeTable(column);
        final TreeTable.Node root = tree.getRoot();
        root.setValue(column, Classes.getShortClassName(this));
        /*
         * GridChange (example)
         *   └─Target range
         *       ├─Dimension 0: [ 2000 … 5475] (3476 cells)
         *       └─Dimension 1: [-1000 … 7999] (9000 cells)
         */
        TreeTable.Node section = root.newChild();
        section.setValue(column, "Target range");
        final StringBuilder buffer = new StringBuilder(256);
        getTargetExtent().appendTo(buffer, Vocabulary.getResources(locale));
        for (final CharSequence line : CharSequences.splitOnEOL(buffer)) {
            String text = line.toString().trim();
            if (!text.isEmpty()) {
                section.newChild().setValue(column, text);
            }
        }
        /*
         * GridChange (example)
         *   └─Target subsamplings
         *       ├─{50, 300}
         *       └─Global ≈ 175.0
         */
        buffer.setLength(0);
        buffer.append('{');
        for (int s : getTargetSubsamplings()) {
            if (buffer.length() > 1) buffer.append(", ");
            buffer.append(s);
        }
        section = root.newChild();
        section.setValue(column, "Target subsamplings");
        section.newChild().setValue(column, buffer.append('}').toString()); buffer.setLength(0);
        section.newChild().setValue(column, buffer.append("Global ≈ ").append((float) getGlobalScale()).toString());
        return tree;
    }

    /**
     * Returns a string representation of this grid change for debugging purpose.
     * The returned string is implementation dependent and may change in any future version.
     *
     * @return a string representation of this grid change for debugging purpose.
     */
    @Override
    public String toString() {
        return toTree(null).toString();
    }
}
