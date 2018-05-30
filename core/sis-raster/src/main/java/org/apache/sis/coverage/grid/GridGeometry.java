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
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.coverage.grid.GridEnvelope;


/**
 * Describes the valid extent of grid coordinates and the transform from those grid coordinates
 * to real world coordinates. Grid geometries contains:
 *
 * <ul>
 *   <li>A <cite>grid envelope</cite> (a.k.a. <cite>"grid extent"</cite>),
 *       often inferred from the {@link RenderedImage} size.</li>
 *   <li>A <cite>grid to CRS</cite> {@link MathTransform},
 *       which can be inferred from the grid envelope and the georeferenced envelope.</li>
 *   <li>A georeferenced {@link Envelope}, which can be inferred from the grid envelope
 *       and the <cite>grid to CRS</cite> transform.</li>
 *   <li>An optional {@link CoordinateReferenceSystem} (CRS) specified as part of the georeferenced envelope.
 *       This CRS is the target of the <cite>grid to CRS</cite> transform.</li>
 * </ul>
 *
 * All above properties except the CRS should be mandatory, but are allowed to be temporarily absent during
 * grid coverage construction. Temporarily absent properties are allowed because they may be inferred from
 * a wider context. For example a grid geometry know nothing about {@link RenderedImage},
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
     * @see #getGridToCRS()
     */
    public static final int GRID_TO_CRS = 8;

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
     * The math transform from grid indices to "real world" coordinates, or {@code null} if unknown.
     * This math transform is usually affine. It maps {@linkplain PixelInCell#CELL_CENTER pixel center}
     * to "real world" coordinate using the following line:
     *
     * {@preformat java
     *     DirectPosition aCellIndices = ...:
     *     DirectPosition aPixelCenter = gridToCRS.transform(pixels, aCellIndices);
     * }
     *
     * @see #CRS
     * @see #getGridToCRS()
     */
    protected final MathTransform gridToCRS;

    /**
     * Same transform than {@link #gridToCRS} but from {@linkplain PixelInCell#CELL_CORNER pixel corner}
     * instead than center. This transform is preferable to {@code gridToCRS} for transforming envelopes.
     *
     * @serial This field is serialized because it may be a value specified explicitly at construction time,
     *         in which case it can be more accurate than a computed value.
     */
    private final MathTransform cornerToCRS;

    /**
     * The resolution in units of the CRS axes.
     * Computed only when first needed.
     *
     * @see #resolution(boolean)
     */
    private transient double[] resolution;

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
    }

    /**
     * Creates a new grid geometry from a grid envelope and a math transform mapping pixel <em>center</em>.
     *
     * @param extent     the valid extent of grid coordinates, or {@code null} if unknown.
     * @param gridToCRS  the math transform which allows for the transformations from grid coordinates
     *                   ({@linkplain PixelInCell#CELL_CENTER pixel center}) to real world earth coordinates.
     *                   May be {@code null} if unknown.
     * @param crs        the coordinate reference system for the "real world" coordinates, or {@code null} if unknown.
     *                   This CRS is given to the {@linkplain #getEnvelope() envelope}.
     *
     * @throws MismatchedDimensionException if the math transform and the CRS do not have consistent dimensions.
     * @throws TransformException if the math transform can not compute the geospatial envelope from the grid envelope.
     */
    public GridGeometry(final GridEnvelope extent, final MathTransform gridToCRS, final CoordinateReferenceSystem crs)
            throws TransformException
    {
        this(extent, PixelInCell.CELL_CENTER, gridToCRS, crs);
    }

    /**
     * Creates a new grid geometry from a grid envelope and a math transform mapping pixel center or corner.
     * This is the most general constructor, the one that gives the maximal control over the grid geometry
     * to be created.
     *
     * @param extent     the valid extent of grid coordinates, or {@code null} if unknown.
     * @param anchor     {@link PixelInCell#CELL_CENTER} for OGC conventions or
     *                   {@link PixelInCell#CELL_CORNER} for Java2D/JAI conventions.
     * @param gridToCRS  the math transform which allows for the transformations from grid coordinates
     *                   to real world earth coordinates. May be {@code null} if unknown.
     * @param crs        the coordinate reference system for the "real world" coordinates, or {@code null} if unknown.
     *                   This CRS is given to the {@linkplain #getEnvelope() envelope}.
     * @throws MismatchedDimensionException if the math transform and the CRS do not have consistent dimensions.
     * @throws TransformException if the math transform can not compute the geospatial envelope from the grid envelope.
     */
    public GridGeometry(final GridEnvelope extent, final PixelInCell anchor, final MathTransform gridToCRS,
            final CoordinateReferenceSystem crs) throws TransformException
    {
        if (gridToCRS != null) {
            if (extent != null) {
                ensureDimensionMatches("extent", extent.getDimension(), gridToCRS.getSourceDimensions());
            }
            if (crs != null) {
                ensureDimensionMatches("crs", crs.getCoordinateSystem().getDimension(), gridToCRS.getTargetDimensions());
            }
        }
        this.extent      = GridExtent.castOrCopy(extent);
        this.gridToCRS   = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CENTER);
        this.cornerToCRS = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CORNER);
        GeneralEnvelope env = null;
        if (extent != null && gridToCRS != null) {
            final int dimension = extent.getDimension();
            env = new GeneralEnvelope(dimension);
            for (int i=0; i<dimension; i++) {
                env.setRange(i, extent.getLow(i), extent.getHigh(i) + 1);
            }
            env = Envelopes.transform(cornerToCRS, env);
            if (crs != null) {
                env.setCoordinateReferenceSystem(crs);
            }
        } else if (crs != null) {
            env = new GeneralEnvelope(crs);
            env.setToNaN();
        }
        envelope = (env != null) ? new ImmutableEnvelope(env) : null;
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
     * than the number of dimension of the envelope or the CRS, but not necessarily.
     *
     * @return the number of grid dimensions.
     */
    public int getDimension() {
        if (gridToCRS != null) {
            return gridToCRS.getSourceDimensions();
        }
        return extent.getDimension();
    }

    /**
     * Returns the "real world" coordinate reference system.
     *
     * @return the coordinate reference system (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no CRS
     *         (i.e. <code>{@linkplain #isDefined isDefined}({@linkplain #CRS})</code> returned {@code false}).
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() throws IncompleteGridGeometryException {
        if (envelope != null) {
            final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
            if (crs != null) {
                assert isDefined(CRS);
                return crs;
            }
        }
        assert !isDefined(CRS);
        throw new IncompleteGridGeometryException(/* Errors.Keys.UnspecifiedCrs */);
    }

    /**
     * Returns the bounding box of "real world" coordinates for this grid geometry. This envelope is the
     * {@linkplain #getExtent() grid extent} {@linkplain #getGridToCRS() transformed} to the "real world"
     * coordinate system.
     *
     * @return the bounding box in "real world" coordinates (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no envelope
     *         (i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #ENVELOPE})</code> returned {@code false}).
     */
    public Envelope getEnvelope() throws IncompleteGridGeometryException {
        if (envelope != null && !envelope.isAllNaN()) {
            assert isDefined(ENVELOPE);
            return envelope;
        }
        assert !isDefined(ENVELOPE);
        throw new IncompleteGridGeometryException(/* gridToCRS == null ?
                    Errors.Keys.UnspecifiedTransform : Errors.Keys.UnspecifiedGridExtent */);
    }

    /**
     * Returns the valid coordinate range of a grid coverage. The lowest valid grid coordinate is zero
     * for {@link java.awt.image.BufferedImage}, but may be non-zero for arbitrary {@link RenderedImage}.
     * A grid with 512 cells can have a minimum coordinate of 0 and maximum of 511.
     *
     * @return the grid envelope (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no extent
     *         (i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #EXTENT})</code> returned {@code false}).
     */
    public GridEnvelope getExtent() throws IncompleteGridGeometryException {
        if (extent != null) {
            assert isDefined(EXTENT);
            return extent;
        }
        assert !isDefined(EXTENT);
        throw new IncompleteGridGeometryException(/* Errors.Keys.UnspecifiedImageSize */);
    }

    /**
     * Returns the transform from grid coordinates to "real world" coordinates in pixel centers.
     * The transform is often an affine transform.
     * The coordinate reference system of the real world coordinates is given by
     * {@link org.opengis.coverage.Coverage#getCoordinateReferenceSystem()}.
     *
     * <p><strong>Note:</strong> OGC 01-004 requires that the transform maps <em>pixel centers</em> to real
     * world coordinates. This is different from some other systems that map pixel's upper left corner.</p>
     *
     * @return the transform (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no transform
     *         (i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #GRID_TO_CRS})</code> returned {@code false}).
     */
    public MathTransform getGridToCRS() throws IncompleteGridGeometryException {
        if (gridToCRS != null) {
            assert isDefined(GRID_TO_CRS);
            return gridToCRS;
        }
        assert !isDefined(GRID_TO_CRS);
        throw new IncompleteGridGeometryException(/* Errors.Keys.UnspecifiedTransform */);
    }

    /**
     * Returns the transform from grid coordinates to "real world" coordinates.
     * This is similar to {@link #getGridToCRS()} except that the transform may map
     * other pixel parts than {@linkplain PixelInCell#CELL_CENTER pixel center}.
     *
     * @param  anchor  the pixel part to map.
     * @return the transform (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no transform
     *         (i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #GRID_TO_CRS})</code> returned {@code false}).
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
        if (mt == null) {
            throw new IncompleteGridGeometryException(/* Errors.Keys.UnspecifiedTransform */);
        }
        return mt;
    }

    /**
     * Estimates the grid resolution in units of the coordinate reference system.
     * If non-null, the length of the returned array is the number of CRS dimensions.
     * If some resolutions are not constant factors (i.e. the {@code gridToCRS} transform for the
     * corresponding dimension is non-linear), then the resolution is set to one of the following values:
     *
     * <ul>
     *   <li>{@link Double#NaN} if {@code allowEstimates} is {@code false}.</li>
     *   <li>an arbitrary resolution otherwise (currently the resolution in the grid center,
     *       but this arbitrary choice may change in any future Apache SIS version).</li>
     * </ul>
     *
     * @param  allowEstimates  whether to provide some values even for resolutions that are not constant factors.
     * @return the grid resolution, or {@code null} if unknown.
     * @throws TransformException if an error occurred while computing the grid resolution.
     */
    public double[] resolution(final boolean allowEstimates) throws TransformException {
        /*
         * If the gridToCRS transform is linear, we do not even need to check the grid extent;
         * it can be null. Otherwise (if the transform is non-linear) the extent is mandatory.
         */
        Matrix mat = MathTransforms.getMatrix(gridToCRS);
        if (mat != null) {
            return resolution(mat, 1);
        }
        if (extent == null || gridToCRS == null) {
            return null;
        }
        /*
         * If we reach this line, the gridToCRS transform has some non-linear parts.
         * The easiest way to estimate a resolution is to ask for the derivative at
         * some arbitrary point. For this method, we take the grid center.
         */
        final int gridDimension = extent.getDimension();
        final GeneralDirectPosition gridCenter = new GeneralDirectPosition(gridDimension);
        for (int i=0; i<gridDimension; i++) {
            gridCenter.setOrdinate(i, extent.getLow(i) + 0.5*extent.getSpan(i));
        }
        final double[] res = resolution(gridToCRS.derivative(gridCenter), 0);
        if (!allowEstimates) {
            /*
             * If we reach this line, we successfully estimated the resolutions but we need to hide non-constant values.
             * We currently don't have an API for finding the non-linear dimensions. We assume that everthing else than
             * LinearTransform and pass-through dimensions are non-linear. This is not always true (e.g. in a Mercator
             * projection, the "longitude → easting" part is linear too), but should be okay for GridGeometry purposes.
             *
             * We keep trace of non-linear dimensions in a bitmask, with bits of non-linear dimensions set to 1.
             * This limit us to 64 dimensions, which is assumed more than enough.
             */
            long nonLinearDimensions = 0;
            for (final MathTransform step : MathTransforms.getSteps(gridToCRS)) {
                mat = MathTransforms.getMatrix(step);
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
                                    throw new ArithmeticException("Excessive number of dimensions.");
                                }
                                nonLinearDimensions |= (1 << j);
                            }
                        }
                        mask &= ~(1 << i);
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
                            throw new ArithmeticException("Excessive number of dimensions.");
                        }
                        mask |= (1 << i);
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
                     * Not a know transform. Assume dimension may become non-linear.
                     */
                    return null;
                }
            }
            /*
             * Set the resolution to NaN for all dimensions that we have determined to be non-linear.
             */
            while (nonLinearDimensions != 0) {
                final int i = Long.numberOfTrailingZeros(nonLinearDimensions);
                nonLinearDimensions &= ~(1 << i);
                res[i] = Double.NaN;
            }
        }
        return res;
    }

    /**
     * Computes the resolutions from the given matrix. This is the length of each row vector.
     *
     * @param  numToIgnore  number of rows and columns to ignore at the end of the matrix.
     *         This is 0 if the matrix is a derivative (i.e. we ignore nothing), or 1 if the matrix
     *         is an affine transform (i.e. we ignore the translation column and the [0 0 … 1] row).
     */
    private static double[] resolution(final Matrix gridToCRS, final int numToIgnore) {
        final double[] resolution = new double[gridToCRS.getNumRow() - numToIgnore];
        final double[] buffer = new double[gridToCRS.getNumCol() - numToIgnore];
        for (int j=0; j<resolution.length; j++) {
            for (int i=0; i<buffer.length; i++) {
                buffer[i] = gridToCRS.getElement(j,i);
            }
            resolution[j] = MathFunctions.magnitude(buffer);
        }
        return resolution;
    }

    /**
     * Returns {@code true} if all the parameters specified by the argument are set.
     *
     * @param  bitmask any combination of {@link #CRS}, {@link #ENVELOPE}, {@link #EXTENT} and {@link #GRID_TO_CRS}.
     * @return {@code true} if all specified attributes are defined (i.e. invoking the
     *         corresponding method will not thrown an {@link IncompleteGridGeometryException}).
     * @throws IllegalArgumentException if the specified bitmask is not a combination of known masks.
     */
    public boolean isDefined(final int bitmask) throws IllegalArgumentException {
        if ((bitmask & ~(CRS | ENVELOPE | EXTENT | GRID_TO_CRS)) != 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "bitmask", bitmask));
        }
        return ((bitmask & CRS)         == 0 || (envelope  != null && envelope.getCoordinateReferenceSystem() != null))
            && ((bitmask & ENVELOPE)    == 0 || (envelope  != null && !envelope.isAllNaN()))
            && ((bitmask & EXTENT)      == 0 || (extent    != null))
            && ((bitmask & GRID_TO_CRS) == 0 || (gridToCRS != null));
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
