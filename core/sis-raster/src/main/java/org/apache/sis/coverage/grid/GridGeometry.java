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

import java.util.Objects;
import java.io.Serializable;
import java.awt.image.RenderedImage;            // For javadoc only.
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;


/**
 * Valid extent of grid coordinates together with the transform from those grid coordinates
 * to real world coordinates. {@code GridGeometry} contains:
 *
 * <ul>
 *   <li>A {@linkplain #getExtent() grid extent} (a.k.a. <cite>grid envelope</cite>),
 *       often inferred from the {@link RenderedImage} size.</li>
 *   <li>A {@linkplain #getGridToCRS grid to CRS} transform,
 *       which may be inferred from the grid extent and the georeferenced envelope.</li>
 *   <li>A {@linkplain #getEnvelope() georeferenced envelope}, which can be inferred
 *       from the grid extent and the <cite>grid to CRS</cite> transform.</li>
 *   <li>An optional {@linkplain #getCoordinateReferenceSystem() Coordinate Reference System} (CRS)
 *       specified as part of the georeferenced envelope.
 *       This CRS is the target of the <cite>grid to CRS</cite> transform.</li>
 *   <li>An <em>estimation</em> of {@link #getResolution(boolean) grid resolution} along each CRS axes,
 *       computed from the <cite>grid to CRS</cite> transform and eventually from the grid extent.</li>
 *   <li>An {@linkplain #isConversionLinear indication of whether conversion for some axes is linear or not}.</li>
 * </ul>
 *
 * The first three properties should be mandatory, but are allowed to be temporarily absent during
 * grid coverage construction. Temporarily absent properties are allowed because they may be inferred
 * from a wider context. For example a grid geometry know nothing about {@link RenderedImage},
 * but {@code GridCoverage2D} does and may use that information for providing a missing grid envelope.
 * By default, any request for an undefined property will throw an {@link IncompleteGridGeometryException}.
 * In order to check if a property is defined, use {@link #isDefined(int)}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Alessio Fabiani (Geosolutions)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class GridGeometry implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -954786616001606624L;

    /**
     * A bitmask to specify the validity of the Coordinate Reference System property.
     *
     * @see #isDefined(int)
     * @see #getCoordinateReferenceSystem()
     */
    public static final int CRS = 1;

    /**
     * A bitmask to specify the validity of the geodetic envelope property.
     *
     * @see #isDefined(int)
     * @see #getEnvelope()
     */
    public static final int ENVELOPE = 2;

    /**
     * A bitmask to specify the validity of the grid envelope property.
     *
     * @see #isDefined(int)
     * @see #getExtent()
     */
    public static final int EXTENT = 4;

    /**
     * A bitmask to specify the validity of the <cite>"grid to CRS"</cite> transform.
     *
     * @see #isDefined(int)
     * @see #getGridToCRS(PixelInCell)
     */
    public static final int GRID_TO_CRS = 8;

    /**
     * A bitmask to specify the validity of the grid resolution.
     *
     * @see #isDefined(int)
     * @see #getResolution(boolean)
     */
    public static final int RESOLUTION = 16;

    /**
     * The valid domain of a grid coverage, or {@code null} if unknown. The lowest valid grid coordinate is zero
     * for {@link java.awt.image.BufferedImage}, but may be non-zero for arbitrary {@link RenderedImage}.
     * A grid with 512 cells can have a minimum coordinate of 0 and maximum of 511.
     *
     * @see #EXTENT
     * @see #getExtent()
     */
    protected final GridExtent extent;

    /**
     * The geodetic envelope, or {@code null} if unknown. If non-null, this envelope is usually the grid {@link #extent}
     * {@linkplain #gridToCRS transformed} to real world coordinates. The Coordinate Reference System} (CRS) of this
     * envelope defines the "real world" CRS of this grid geometry.
     *
     * @see #ENVELOPE
     * @see #getEnvelope()
     */
    protected final ImmutableEnvelope envelope;

    /**
     * The conversion from grid indices to "real world" coordinates, or {@code null} if unknown.
     * If non-null, the conversion shall map {@linkplain PixelInCell#CELL_CENTER pixel center}.
     * This conversion is usually, but not necessarily, affine.
     *
     * @see #CRS
     * @see #getGridToCRS(PixelInCell)
     * @see PixelInCell#CELL_CENTER
     */
    protected final MathTransform gridToCRS;

    /**
     * Same conversion than {@link #gridToCRS} but from {@linkplain PixelInCell#CELL_CORNER pixel corner}
     * instead than center. This transform is preferable to {@code gridToCRS} for transforming envelopes.
     *
     * @serial This field is serialized because it may be a value specified explicitly at construction time,
     *         in which case it can be more accurate than a computed value.
     */
    private final MathTransform cornerToCRS;

    /**
     * An <em>estimation</em> of the grid resolution, in units of the CRS axes.
     * Computed from {@link #gridToCRS}, eventually together with {@link #extent}.
     *
     * @see #RESOLUTION
     * @see #getResolution(boolean)
     */
    protected final double[] resolution;

    /**
     * Whether the conversions from grid coordinates to the CRS are linear, for each target axis.
     * The bit located at {@code 1L << dimension} is set to 1 when the conversion at that dimension is non-linear.
     * The dimension indices are those of the CRS, not the grid. The use of {@code long} type limits the capacity
     * to 64 dimensions. But actually {@code GridGeometry} can contain more dimensions provided that index of the
     * last non-linear dimension is not greater than 64.
     *
     * @see #isConversionLinear(int...)
     */
    private final long nonLinears;

    /**
     * Creates a new grid geometry with the same values than the given grid geometry.
     * This is a copy constructor for subclasses.
     *
     * @param other  the other grid geometry to copy.
     */
    protected GridGeometry(final GridGeometry other) {
        extent      = other.extent;
        gridToCRS   = other.gridToCRS;
        cornerToCRS = other.cornerToCRS;
        envelope    = other.envelope;
        resolution  = other.resolution;
        nonLinears  = other.nonLinears;
    }

    /*
     * Do not provide convenience constructor without PixelInCell or PixelOrientation argument.
     * Experience shows that 0.5 pixel offsets in image georeferencing is a recurrent problem.
     * We really want to force developers to think about whether their 'gridToCRS' transform
     * locates pixel corner or pixel center.
     */

    /**
     * Creates a new grid geometry from a grid envelope and a mapping from pixel coordinates to "real world" coordinates.
     * At least one of {@code extent}, {@code gridToCRS} or {@code crs} arguments shall be non-null.
     * If {@code gridToCRS} is non-null, than {@code anchor} shall be non-null too with one of the following values:
     *
     * <ul>
     *   <li>{@link PixelInCell#CELL_CENTER} if conversions of cell indices by {@code gridToCRS} give "real world"
     *       coordinates close to the center of each cell.</li>
     *   <li>{@link PixelInCell#CELL_CORNER} if conversions of cell indices by {@code gridToCRS} give "real world"
     *       coordinates at the corner of each cell. The cell corner is the one for which all grid indices have the
     *       smallest values (closest to negative infinity).</li>
     * </ul>
     *
     * <div class="note"><b>API note:</b>
     * there is no default value for {@code anchor} because experience shows that images shifted by ½ pixel
     * (with pixels that may be tens of kilometres large) is a recurrent problem. We want to encourage developers
     * to always think about wether their <cite>grid to CRS</cite> transform is mapping pixel corner or center.</div>
     *
     * <div class="note"><b>Upcoming API generalization:</b>
     * the {@code extent} type of this method may be changed to {@code GridEnvelope} interface in a future Apache SIS version.
     * This is pending <a href="https://github.com/opengeospatial/geoapi/issues/36">GeoAPI update</a>.</div>
     *
     * @param  extent     the valid extent of grid coordinates, or {@code null} if unknown.
     * @param  anchor     {@linkplain PixelInCell#CELL_CENTER Cell center} for OGC conventions or
     *                    {@linkplain PixelInCell#CELL_CORNER cell corner} for Java2D/JAI conventions.
     * @param  gridToCRS  the mapping from grid coordinates to "real world" coordinates, or {@code null} if unknown.
     * @param  crs        the coordinate reference system of the "real world" coordinates, or {@code null} if unknown.
     * @throws NullPointerException if {@code extent}, {@code gridToCRS} and {@code crs} arguments are all null.
     * @throws MismatchedDimensionException if the math transform and the CRS do not have consistent dimensions.
     * @throws TransformException if the math transform can not compute the geospatial envelope or the resolution
     *         from the grid envelope.
     */
    public GridGeometry(final GridExtent extent, final PixelInCell anchor, final MathTransform gridToCRS,
            final CoordinateReferenceSystem crs) throws TransformException
    {
        if (gridToCRS != null) {
            if (extent != null) {
                ensureDimensionMatches("extent", extent.getDimension(), gridToCRS.getSourceDimensions());
            }
            if (crs != null) {
                ensureDimensionMatches("crs", crs.getCoordinateSystem().getDimension(), gridToCRS.getTargetDimensions());
            }
        } else if (crs == null) {
            ArgumentChecks.ensureNonNull("extent", extent);
        }
        this.extent      = extent;
        this.gridToCRS   = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CENTER);
        this.cornerToCRS = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CORNER);
        GeneralEnvelope env = null;
        if (extent != null && gridToCRS != null) {
            env = extent.toCRS(cornerToCRS);
            env.setCoordinateReferenceSystem(crs);
        } else if (crs != null) {
            env = new GeneralEnvelope(crs);
            env.setToNaN();
        }
        envelope = (env != null) ? new ImmutableEnvelope(env) : null;
        /*
         * If the gridToCRS transform is linear, we do not even need to check the grid extent;
         * it can be null. Otherwise (if the transform is non-linear) the extent is mandatory.
         * The easiest way to estimate a resolution is then to ask for the derivative at some
         * arbitrary point. For this constructor, we take the grid center.
         */
        final Matrix mat = MathTransforms.getMatrix(gridToCRS);
        if (mat != null) {
            resolution = resolution(mat, 1);
        } else if (extent != null && gridToCRS != null) {
            resolution = resolution(gridToCRS.derivative(extent.getCentroid()), 0);
        } else {
            resolution = null;
        }
        nonLinears = findNonLinearTargets(gridToCRS);
    }

    /**
     * Ensures that the given dimension is equals to the expected value. If not, throws an exception.
     *
     * @param argument  the name of the argument being tested.
     * @param dimension the dimension of the argument value.
     * @param expected  the expected dimension.
     */
    private static void ensureDimensionMatches(final String argument, final int dimension, final int expected)
            throws MismatchedDimensionException
    {
        if (dimension != expected) {
            throw new MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedDimension_3, argument, dimension, expected));
        }
    }

    /**
     * Returns the number of dimensions of the <em>grid</em>. This is typically the same
     * than the number of {@linkplain #getEnvelope() envelope} dimensions or the number of
     * {@linkplain #getCoordinateReferenceSystem() coordinate reference system} dimensions,
     * but not necessarily.
     *
     * @return the number of grid dimensions.
     */
    public int getDimension() {
        if (gridToCRS != null) {
            return gridToCRS.getSourceDimensions();
        } else if (extent != null) {
            return extent.getDimension();
        } else {
            /*
             * Last resort only since we have no guarantee that the envelope dimension is the same
             * than the grid dimension (see above javadoc). The envelope should never be null at
             * this point since the constructor verified that at least one argument was non-null.
             */
            return envelope.getDimension();
        }
    }

    /**
     * Returns the number of dimensions of the <em>CRS</em>. This is typically the same than the
     * number of {@linkplain #getDimension() grid dimensions}, but not necessarily.
     */
    private int getTargetDimension() {
        if (gridToCRS != null) {
            return gridToCRS.getTargetDimensions();
        } else if (envelope != null) {
            return envelope.getDimension();
        } else {
            /*
             * Last resort only since we have no guarantee that the grid dimension is the same
             * then the CRS dimension (converse of the rational in getDimension() method).
             */
            return extent.getDimension();
        }
    }

    /**
     * Returns the "real world" coordinate reference system.
     *
     * @return the coordinate reference system (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no CRS —
     *         i.e. <code>{@linkplain #isDefined isDefined}({@linkplain #CRS})</code> returned {@code false}.
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() throws IncompleteGridGeometryException {
        if (envelope != null) {
            final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
            if (crs != null) return crs;
        }
        throw incomplete(CRS, Resources.Keys.UnspecifiedCRS);
    }

    /**
     * Returns the bounding box of "real world" coordinates for this grid geometry.
     * This envelope is computed from the {@linkplain #getExtent() grid extent}, which is
     * {@linkplain #getGridToCRS(PixelInCell) transformed} to the "real world" coordinate system.
     *
     * @return the bounding box in "real world" coordinates (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no envelope —
     *         i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #ENVELOPE})</code> returned {@code false}.
     */
    public Envelope getEnvelope() throws IncompleteGridGeometryException {
        if (envelope != null && !envelope.isAllNaN()) {
            return envelope;
        }
        throw incomplete(ENVELOPE, (extent == null) ? Resources.Keys.UnspecifiedGridExtent : Resources.Keys.UnspecifiedTransform);
    }

    /**
     * Returns the valid coordinate range of a grid coverage. The lowest valid grid coordinate is zero
     * for {@link java.awt.image.BufferedImage}, but may be non-zero for arbitrary {@link RenderedImage}.
     * A grid with 512 cells can have a minimum coordinate of 0 and maximum of 511.
     *
     * <div class="warning"><b>Upcoming API generalization:</b>
     * the return type of this method may be changed to {@code GridEnvelope} interface in a future Apache SIS version.
     * This is pending <a href="https://github.com/opengeospatial/geoapi/issues/36">GeoAPI update</a>.</div>
     *
     * @return the valid extent of grid coordinates (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no extent —
     *         i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #EXTENT})</code> returned {@code false}.
     */
    public GridExtent getExtent() throws IncompleteGridGeometryException {
        if (extent != null) {
            return extent;
        }
        throw incomplete(EXTENT, Resources.Keys.UnspecifiedGridExtent);
    }

    /*
     * Do not provide a convenience 'getGridToCRS()' method without PixelInCell or PixelOrientation argument.
     * Experience shows that 0.5 pixel offset in image localization is a recurrent problem. We really want to
     * force developers to think about whether they want a gridToCRS transform locating pixel corner or center.
     */

    /**
     * Returns the conversion from grid coordinates to "real world" coordinates.
     * The conversion is often an affine transform, but not necessarily.
     * Conversions from pixel indices to geospatial coordinates can be performed for example as below:
     *
     * {@preformat java
     *     MathTransform  gridToCRS     = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
     *     DirectPosition indicesOfCell = new GeneralDirectPosition(2, 3, 4):
     *     DirectPosition aPixelCenter  = gridToCRS.transform(indicesOfCell, null);
     * }
     *
     * Callers must specify whether they want the "real world" coordinates of pixel center or pixel corner.
     * The cell corner is the one for which all grid indices have the smallest values (closest to negative infinity).
     * As a rule of thumb:
     *
     * <ul>
     *   <li>Use {@link PixelInCell#CELL_CENTER} for transforming <em>points</em>.</li>
     *   <li>Use {@link PixelInCell#CELL_CORNER} for transforming <em>envelopes</em>
     *       with inclusive lower coordinates and <strong>exclusive</strong> upper coordinates.</li>
     * </ul>
     *
     * <div class="note"><b>API note:</b>
     * there is no default value for {@code anchor} because experience shows that images shifted by ½ pixel
     * (with pixels that may be tens of kilometres large) is a recurrent problem. We want to encourage developers
     * to always think about wether the desired <cite>grid to CRS</cite> transform shall map pixel corner or center.</div>
     *
     * @param  anchor  the pixel part to map.
     * @return the conversion from grid coordinates to "real world" coordinates (never {@code null}).
     * @throws IllegalArgumentException if the given {@code anchor} is not a known code list value.
     * @throws IncompleteGridGeometryException if this grid geometry has no transform —
     *         i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #GRID_TO_CRS})</code> returned {@code false}.
     */
    public MathTransform getGridToCRS(final PixelInCell anchor) throws IncompleteGridGeometryException {
        final MathTransform mt;
        if (PixelInCell.CELL_CENTER.equals(anchor)) {
            mt = gridToCRS;
        } else if (PixelInCell.CELL_CORNER.equals(anchor)) {
            mt = cornerToCRS;
        }  else {
            mt = PixelTranslation.translate(gridToCRS, PixelInCell.CELL_CENTER, anchor);
        }
        if (mt != null) {
            return mt;
        }
        throw incomplete(GRID_TO_CRS, Resources.Keys.UnspecifiedTransform);
    }

    /**
     * Returns an <em>estimation</em> of the grid resolution, in units of the coordinate reference system axes.
     * The length of the returned array is the number of CRS dimensions, with {@code resolution[0]}
     * being the resolution along the first CRS axis, {@code resolution[1]} the resolution along the second CRS
     * axis, <i>etc</i>. Note that this axis order is not necessarily the same than grid axis order.
     *
     * <p>If the resolution at CRS dimension <var>i</var> is not a constant factor
     * (i.e. the <code>{@linkplain #isConversionLinear(int...) isConversionLinear}(i)</code> returns {@code false}),
     * then {@code resolution[i]} is set to one of the following values:</p>
     *
     * <ul>
     *   <li>{@link Double#NaN} if {@code allowEstimates} is {@code false}.</li>
     *   <li>An arbitrary representative resolution otherwise.
     *       Current implementation computes the resolution at {@linkplain GridExtent#getCentroid() grid center},
     *       but different implementations may use alternative algorithms.</li>
     * </ul>
     *
     * @param  allowEstimates  whether to provide some values even for resolutions that are not constant factors.
     * @return an <em>estimation</em> of the grid resolution (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no resolution —
     *         i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #RESOLUTION})</code> returned {@code false}.
     */
    public double[] getResolution(final boolean allowEstimates) {
        if (resolution != null) {
            final double[] res = resolution.clone();
            if (!allowEstimates) {
                long nonLinearDimensions = nonLinears;
                while (nonLinearDimensions != 0) {
                    final int i = Long.numberOfTrailingZeros(nonLinearDimensions);
                    nonLinearDimensions &= ~(1L << i);
                    res[i] = Double.NaN;
                }
            }
            return res;
        }
        throw incomplete(RESOLUTION, (gridToCRS == null) ? Resources.Keys.UnspecifiedTransform : Resources.Keys.UnspecifiedGridExtent);
    }

    /**
     * Computes the resolutions from the given matrix. This is the magnitude of each row vector.
     *
     * @param  numToIgnore  number of rows and columns to ignore at the end of the matrix.
     *         This is 0 if the matrix is a derivative (i.e. we ignore nothing), or 1 if the matrix
     *         is an affine transform (i.e. we ignore the translation column and the [0 0 … 1] row).
     */
    private static double[] resolution(final Matrix gridToCRS, final int numToIgnore) {
        final double[] resolution = new double[gridToCRS.getNumRow() - numToIgnore];
        final double[] buffer     = new double[gridToCRS.getNumCol() - numToIgnore];
        for (int j=0; j<resolution.length; j++) {
            for (int i=0; i<buffer.length; i++) {
                buffer[i] = gridToCRS.getElement(j,i);
            }
            resolution[j] = MathFunctions.magnitude(buffer);
        }
        return resolution;
    }

    /**
     * Indicates whether the <cite>grid to CRS</cite> conversion is linear for all the specified CRS axes.
     * The conversion from grid coordinates to real world coordinates is often linear for some dimensions,
     * typically the horizontal ones at indices 0 and 1. But the vertical dimension (usually at index 2)
     * is often non-linear, for example with data at 0, 5, 10, 100 and 1000 metres.
     *
     * @param  targets  indices of CRS axes. This is not necessarily the same than indices of grid axes.
     * @return {@code true} if the conversion from grid coordinates to "real world" coordinates is linear
     *         for all the given CRS dimension.
     */
    public boolean isConversionLinear(final int... targets) {
        final int dimension = getTargetDimension();
        long mask = 0;
        for (final int d : targets) {
            ArgumentChecks.ensureValidIndex(dimension, d);
            if (d < Long.SIZE) mask |= (1L << d);
        }
        return (nonLinears & mask) == 0;
    }

    /**
     * Guesses which target dimensions may be non-linear. We currently don't have an API for finding the non-linear dimensions.
     * Current implementation assumes that everything else than {@code LinearTransform} and pass-through dimensions are non-linear.
     * This is not always true (e.g. in a Mercator projection, the "longitude → easting" part is linear too), but should be okay
     * for {@code GridGeometry} purposes.
     *
     * <p>We keep trace of non-linear dimensions in a bitmask, with bits of non-linear dimensions set to 1.
     * This limit us to 64 dimensions, which is assumed more than enough. Note that {@code GridGeometry} can
     * contain more dimensions provided that index of the last non-linear dimension is not greater than 64.</p>
     *
     * @param  gridToCRS  the transform to "real world" coordinates, or {@code null} if unknown.
     * @return a bitmask of dimensions, or 0 (i.e. conversion assumed fully linear) if the given transform was null.
     */
    private static long findNonLinearTargets(final MathTransform gridToCRS) {
        long nonLinearDimensions = 0;
        for (final MathTransform step : MathTransforms.getSteps(gridToCRS)) {
            final Matrix mat = MathTransforms.getMatrix(step);
            if (mat != null) {
                /*
                 * For linear transforms there is no bits to set. However if some bits were set by a previous
                 * iteration, we may need to move them (for example the transform may swap axes). We take the
                 * current bitmasks as source dimensions and find what are the target dimensions for them.
                 */
                long mask = nonLinearDimensions;
                nonLinearDimensions = 0;
                while (mask != 0) {
                    final int i = Long.numberOfTrailingZeros(mask);         // Source dimension of non-linear part
                    for (int j = mat.getNumRow() - 1; --j >= 0;) {          // Possible target dimensions
                        if (mat.getElement(j, i) != 0) {
                            if (j >= Long.SIZE) {
                                throw excessiveDimension(gridToCRS);
                            }
                            nonLinearDimensions |= (1L << j);
                        }
                    }
                    mask &= ~(1L << i);
                }
            } else if (step instanceof PassThroughTransform) {
                /*
                 * Assume that all modified coordinates use non-linear transform. We do not inspect the
                 * sub-transform recursively because if it had a non-linear step, PassThroughTransform
                 * should have moved that step outside the sub-transform for easier concatenation with
                 * the LinearTransforms before of after that PassThroughTransform.
                 */
                long mask = 0;
                final int dimIncrease = step.getTargetDimensions() - step.getSourceDimensions();
                final int maxBits = Long.SIZE - Math.max(dimIncrease, 0);
                for (final int i : ((PassThroughTransform) step).getModifiedCoordinates()) {
                    if (i >= maxBits) {
                        throw excessiveDimension(gridToCRS);
                    }
                    mask |= (1L << i);
                }
                /*
                 * The mask we just computed identifies non-linear source dimensions, but we need target
                 * dimensions. They are usually the same (the pass-through coordinate values do not have
                 * their order changed). However we have a difficulty if the number of dimensions changes.
                 * We know that the change happen in the sub-transform, but we do not know where exactly.
                 * For example if the mask is 001010 and the number of dimensions increases by 1, we know
                 * that we still have "00" at the beginning and "0" at the end of the mask, but we don't
                 * know what happen between the two. Does "101" become "1101" or "1011"? We conservatively
                 * take "1111", i.e. we unconditionally set all bits in the middle to 1.
                 *
                 * Mathematics:
                 *   (Long.highestOneBit(mask) << 1) - 1
                 *   is a mask identifying all source dimensions before trailing pass-through dimensions.
                 *
                 *   maskHigh = (Long.highestOneBit(mask) << (dimIncrease + 1)) - 1
                 *   is a mask identifying all target dimensions before trailing pass-through dimensions.
                 *
                 *   maskLow = Long.lowestOneBit(mask) - 1
                 *   is a mask identifying all leading pass-through dimensions (both source and target).
                 *
                 *   maskHigh & ~maskLow
                 *   is a mask identifying only target dimensions after leading pass-through and before
                 *   trailing pass-through dimensions. In our case, all 1 bits in maskLow are also 1 bits
                 *   in maskHigh. So we can rewrite as
                 *
                 *   maskHigh - maskLow
                 *   and the -1 terms cancel each other.
                 */
                if (dimIncrease != 0) {
                    mask = (Long.highestOneBit(mask) << (dimIncrease + 1)) - Long.lowestOneBit(mask);
                }
                nonLinearDimensions |= mask;
            } else {
                /*
                 * Not a known transform. Assume all dimensions may become non-linear.
                 */
                final int dimension = gridToCRS.getTargetDimensions();
                if (dimension > Long.SIZE) {
                    throw excessiveDimension(gridToCRS);
                }
                return (dimension >= Long.SIZE) ? -1 : (1L << dimension) - 1;
            }
        }
        return nonLinearDimensions;
    }

    /**
     * Invoked when the number of non-linear dimensions exceeds the {@code GridGeometry} capacity.
     */
    private static ArithmeticException excessiveDimension(final MathTransform gridToCRS) {
        return new ArithmeticException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, gridToCRS.getTargetDimensions()));
    }

    /**
     * Invoked when a property has been requested for which we have for information.
     *
     * @param  bitmask   the requested property, for assertion purpose.
     * @param  errorKey  the resource key to use in error message.
     * @return the exception to be thrown by the caller.
     */
    private IncompleteGridGeometryException incomplete(final int bitmask, final short errorKey) {
        assert getClass() != GridGeometry.class || !isDefined(bitmask);
        return new IncompleteGridGeometryException(Resources.format(errorKey));
    }

    /**
     * Returns {@code true} if all the parameters specified by the argument are set.
     * If this method returns {@code true}, then invoking the corresponding getter
     * methods will not throw {@link IncompleteGridGeometryException}.
     *
     * @param  bitmask  any combination of {@link #CRS}, {@link #ENVELOPE}, {@link #EXTENT},
     *         {@link #GRID_TO_CRS} and {@link #RESOLUTION}.
     * @return {@code true} if all specified attributes are defined (i.e. invoking the
     *         corresponding method will not thrown an {@link IncompleteGridGeometryException}).
     * @throws IllegalArgumentException if the specified bitmask is not a combination of known masks.
     */
    public boolean isDefined(final int bitmask) throws IllegalArgumentException {
        if ((bitmask & ~(CRS | ENVELOPE | EXTENT | GRID_TO_CRS | RESOLUTION)) != 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "bitmask", bitmask));
        }
        return ((bitmask & CRS)         == 0 || (envelope   != null && envelope.getCoordinateReferenceSystem() != null))
            && ((bitmask & ENVELOPE)    == 0 || (envelope   != null && !envelope.isAllNaN()))
            && ((bitmask & EXTENT)      == 0 || (extent     != null))
            && ((bitmask & GRID_TO_CRS) == 0 || (gridToCRS  != null))
            && ((bitmask & RESOLUTION)  == 0 || (resolution != null));
    }

    /**
     * Returns a hash value for this grid geometry. This value need not remain
     * consistent between different implementations of the same class.
     */
    @Override
    public int hashCode() {
        int code = (int) serialVersionUID;
        if (gridToCRS != null) {
            code += gridToCRS.hashCode();
        }
        if (extent != null) {
            code += extent.hashCode();
        }
        // We do not check the envelope since it has a determinist relationship with other attributes.
        return code;
    }

    /**
     * Compares the specified object with this grid geometry for equality.
     *
     * @param  object  the object to compare with.
     * @return {@code true} if the given object is equals to this grid geometry.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final GridGeometry that = (GridGeometry) object;
            return Objects.equals(this.extent,    that.extent)    &&
                   Objects.equals(this.gridToCRS, that.gridToCRS) &&
                   Objects.equals(this.envelope,  that.envelope);
        }
        return false;
    }

    /**
     * Returns a string representation of this grid geometry. The returned string
     * is implementation dependent. It is provided for debugging purposes only.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + extent + ", " + gridToCRS + ']';
    }
}
