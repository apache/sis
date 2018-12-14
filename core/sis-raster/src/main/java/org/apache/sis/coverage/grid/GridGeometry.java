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
import java.util.Objects;
import java.util.Locale;
import java.io.Serializable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.awt.image.RenderedImage;            // For javadoc only.
import org.opengis.metadata.Identifier;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Debug;
import org.apache.sis.io.TableAppender;

import static org.apache.sis.referencing.CRS.findOperation;


/**
 * Valid extent of grid coordinates together with the transform from those grid coordinates
 * to real world coordinates. {@code GridGeometry} contains:
 *
 * <ul class="verbose">
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
 * but {@code GridCoverage2D} does and may use that information for providing a missing grid extent.
 * By default, any request for an undefined property will throw an {@link IncompleteGridGeometryException}.
 * In order to check if a property is defined, use {@link #isDefined(int)}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
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
     * A bitmask to specify the validity of the grid extent property.
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

    /**
     * Creates a new grid geometry with the same values than the given grid geometry except the grid extent.
     * This constructor can be used for creating a grid geometry over a subregion, for example with the grid
     * extent computed by {@link #getExtent(Envelope)}.
     *
     * @param other   the other grid geometry to copy.
     * @param extent  the new extent for the grid geometry to construct, or {@code null} if none.
     * @throws NullPointerException if {@code extent} is {@code null} and the other grid geometry contains no other information.
     * @throws TransformException if the math transform can not compute the geospatial envelope from the grid extent.
     *
     * @see #getExtent(Envelope)
     */
    public GridGeometry(final GridGeometry other, final GridExtent extent) throws TransformException {
        ArgumentChecks.ensureNonNull("other", other);
        if (extent != null) {
            ensureDimensionMatches("extent", other.getDimension(), extent.getDimension());
        }
        this.extent = extent;
        gridToCRS   = other.gridToCRS;
        cornerToCRS = other.cornerToCRS;
        resolution  = other.resolution;
        nonLinears  = other.nonLinears;
        envelope    = computeEnvelope(other.gridToCRS, other.envelope != null ? other.envelope.getCoordinateReferenceSystem() : null);
        if (envelope == null && gridToCRS == null) {
            ArgumentChecks.ensureNonNull("extent", extent);
        }
    }

    /*
     * Do not provide convenience constructor without PixelInCell or PixelOrientation argument.
     * Experience shows that 0.5 pixel offsets in image georeferencing is a recurrent problem.
     * We really want to force developers to think about whether their 'gridToCRS' transform
     * locates pixel corner or pixel center.
     */

    /**
     * Creates a new grid geometry from a grid extent and a mapping from pixel coordinates to "real world" coordinates.
     * At least one of {@code extent}, {@code gridToCRS} or {@code crs} arguments shall be non-null.
     * If {@code gridToCRS} is non-null, then {@code anchor} shall be non-null too with one of the following values:
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
     * <div class="warning"><b>Upcoming API generalization:</b>
     * the {@code extent} type of this method may be changed to {@code GridEnvelope} interface in a future Apache SIS version.
     * This is pending <a href="https://github.com/opengeospatial/geoapi/issues/36">GeoAPI update</a>.
     * In addition, the {@code PixelInCell} code list currently defined in the {@code org.opengis.referencing.datum} package
     * may move in another package in a future GeoAPI version because this type is no longer defined by the ISO 19111 standard
     * after the 2018 revision.</div>
     *
     * @param  extent     the valid extent of grid coordinates, or {@code null} if unknown.
     * @param  anchor     {@linkplain PixelInCell#CELL_CENTER Cell center} for OGC conventions or
     *                    {@linkplain PixelInCell#CELL_CORNER cell corner} for Java2D/JAI conventions.
     * @param  gridToCRS  the mapping from grid coordinates to "real world" coordinates, or {@code null} if unknown.
     * @param  crs        the coordinate reference system of the "real world" coordinates, or {@code null} if unknown.
     * @throws NullPointerException if {@code extent}, {@code gridToCRS} and {@code crs} arguments are all null.
     * @throws MismatchedDimensionException if the math transform and the CRS do not have consistent dimensions.
     * @throws TransformException if the math transform can not compute the geospatial envelope or resolution from the grid extent.
     */
    public GridGeometry(final GridExtent extent, final PixelInCell anchor, final MathTransform gridToCRS,
            final CoordinateReferenceSystem crs) throws TransformException
    {
        if (gridToCRS != null) {
            if (extent != null) {
                ensureDimensionMatches("extent", gridToCRS.getSourceDimensions(), extent.getDimension());
            }
            if (crs != null) {
                ensureDimensionMatches("crs", gridToCRS.getTargetDimensions(), crs.getCoordinateSystem().getDimension());
            }
        } else if (crs == null) {
            ArgumentChecks.ensureNonNull("extent", extent);
        }
        this.extent      = extent;
        this.gridToCRS   = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CENTER);
        this.cornerToCRS = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CORNER);
        this.envelope    = computeEnvelope(gridToCRS, crs);     // 'gridToCRS' specified by the user, not 'this.gridToCRS'.
        /*
         * If the gridToCRS transform is linear, we do not even need to check the grid extent;
         * it can be null. Otherwise (if the transform is non-linear) the extent is mandatory.
         * The easiest way to estimate a resolution is then to ask for the derivative at some
         * arbitrary point. For this constructor, we take the grid center.
         *
         * Note that for this computation, it does not matter if 'gridToCRS' is the user-specified
         * transform or the 'this.gridToCRS' field value; both should produce equivalent results.
         */
        double[] resolution = null;
        final Matrix matrix = MathTransforms.getMatrix(gridToCRS);
        if (matrix != null) {
            resolution = resolution(matrix, 1);
        } else if (extent != null && gridToCRS != null) try {
            resolution = resolution(gridToCRS.derivative(extent.getCentroid()), 0);
        } catch (TransformException e) {
            recoverableException(e);
        }
        this.resolution = resolution;
        nonLinears = findNonLinearTargets(gridToCRS);
    }

    /**
     * Computes the envelope with the given coordinate reference system. This method is invoked from constructors.
     * The {@link #extent}, {@link #gridToCRS} and {@link #cornerToCRS} fields must be set before this method is invoked.
     *
     * @param  specified  the transform specified by the user. This is not necessarily {@link #gridToCRS}.
     * @param  crs        the coordinate reference system to declare in the envelope.
     */
    private ImmutableEnvelope computeEnvelope(final MathTransform specified, final CoordinateReferenceSystem crs) throws TransformException {
        GeneralEnvelope env = null;
        if (extent != null && cornerToCRS != null) {
            env = extent.toCRS(cornerToCRS, specified);
            env.setCoordinateReferenceSystem(crs);
        } else if (crs != null) {
            env = new GeneralEnvelope(crs);
            env.setToNaN();
        }
        return (env != null) ? new ImmutableEnvelope(env) : null;
    }

    /**
     * Creates a new grid geometry from a geospatial envelope and a mapping from pixel coordinates to "real world" coordinates.
     * At least one of {@code gridToCRS} or {@code envelope} arguments shall be non-null.
     * If {@code gridToCRS} is non-null, then {@code anchor} shall be non-null too with one of the values documented in the
     * {@link #GridGeometry(GridExtent, PixelInCell, MathTransform, CoordinateReferenceSystem) constructor expecting a grid
     * extent}.
     *
     * <p>The given envelope shall encompass all cell surfaces, from the left border of leftmost pixel to the right border
     * of the rightmost pixel and similarly along other axes. This constructor tries to store a geospatial envelope close
     * to the specified envelope, but there is no guarantees that the envelope returned by {@link #getEnvelope()} will be
     * equal to the given envelope. The envelope stored in the new {@code GridGeometry} may be slightly smaller, larger or
     * shifted because the floating point values used in geospatial envelope can not always be mapped to the integer
     * coordinates used in {@link GridExtent}.
     * The rules for deciding whether coordinates should be rounded toward nearest integers,
     * to {@linkplain Math#floor(double) floor} or to {@linkplain Math#ceil(double) ceil} values
     * are implementation details and may be adjusted in any Apache SIS versions.</p>
     *
     * <p>Because of the uncertainties explained in above paragraph, this constructor should be used only in last resort,
     * when the grid extent is unknown. For determinist results, developers should prefer the
     * {@linkplain #GridGeometry(GridExtent, PixelInCell, MathTransform, CoordinateReferenceSystem) constructor using grid extent}
     * as much as possible. In particular, this constructor is not suitable for computing grid geometry of tiles in a tiled image,
     * because the above-cited uncertainties may result in apparently random black lines between tiles.</p>
     *
     * <div class="warning"><b>Upcoming API change:</b>
     * The {@code PixelInCell} code list currently defined in the {@code org.opengis.referencing.datum} package
     * may move in another package in a future GeoAPI version because this type is no longer defined by the
     * ISO 19111 standard after the 2018 revision. This code list may be taken by ISO 19123 in a future revision.</div>
     *
     * @param  anchor     {@linkplain PixelInCell#CELL_CENTER Cell center} for OGC conventions or
     *                    {@linkplain PixelInCell#CELL_CORNER cell corner} for Java2D/JAI conventions.
     * @param  gridToCRS  the mapping from grid coordinates to "real world" coordinates, or {@code null} if unknown.
     * @param  envelope   the geospatial envelope, including its coordinate reference system if available.
     *                    There is no guarantees that the envelope actually stored in the {@code GridGeometry}
     *                    will be equal to this specified envelope.
     * @throws TransformException if the math transform can not compute the grid extent or the resolution.
     */
    @SuppressWarnings("null")
    public GridGeometry(final PixelInCell anchor, final MathTransform gridToCRS, final Envelope envelope) throws TransformException {
        if (gridToCRS == null) {
            ArgumentChecks.ensureNonNull("envelope", envelope);
        } else if (envelope != null) {
            ensureDimensionMatches("envelope", gridToCRS.getTargetDimensions(), envelope.getDimension());
        }
        this.gridToCRS   = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CENTER);
        this.cornerToCRS = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CORNER);
        Matrix scales = MathTransforms.getMatrix(gridToCRS);
        int numToIgnore = 1;
        if (envelope != null && cornerToCRS != null) {
            GeneralEnvelope env = Envelopes.transform(cornerToCRS.inverse(), envelope);
            extent = new GridExtent(env, null, null);
            env = extent.toCRS(cornerToCRS, gridToCRS);         // 'gridToCRS' specified by the user, not 'this.gridToCRS'.
            env.setCoordinateReferenceSystem(envelope.getCoordinateReferenceSystem());
            this.envelope = new ImmutableEnvelope(env);
            if (scales == null) try {
                scales = gridToCRS.derivative(extent.getCentroid());    // 'gridToCRS' can not be null if 'cornerToCRS' is non-null.
                numToIgnore = 0;
            } catch (TransformException e) {
                recoverableException(e);
            }
        } else {
            this.extent   = null;
            this.envelope = ImmutableEnvelope.castOrCopy(envelope);
        }
        resolution = (scales != null) ? resolution(scales, numToIgnore) : null;
        nonLinears = findNonLinearTargets(gridToCRS);
    }

    /**
     * Ensures that the given dimension is equals to the expected value. If not, throws an exception.
     *
     * @param argument  the name of the argument being tested.
     * @param expected  the expected number of dimension.
     * @param dimension the actual dimension of the argument value.
     */
    private static void ensureDimensionMatches(final String argument, final int expected, final int dimension)
            throws MismatchedDimensionException
    {
        if (dimension != expected) {
            throw new MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedDimension_3, argument, expected, dimension));
        }
    }

    /**
     * Invoked when a recoverable exception occurred. Those exceptions must be minor enough
     * that they can be silently ignored in most cases.
     */
    static void recoverableException(final Exception exception) {
        Logging.recoverableException(Logging.getLogger(Modules.RASTER), GridGeometry.class, "transform", exception);
    }

    /**
     * Creates a grid geometry with only an extent and a coordinate reference system.
     * This constructor can be used when the <cite>grid to CRS</cite> transform is unknown.
     *
     * @param  extent     the valid extent of grid coordinates, or {@code null} if unknown.
     * @param  crs        the coordinate reference system of the "real world" coordinates, or {@code null} if unknown.
     * @throws NullPointerException if {@code extent} and {@code crs} arguments are both null.
     */
    public GridGeometry(final GridExtent extent, final CoordinateReferenceSystem crs) {
        this.extent = extent;
        gridToCRS   = null;
        cornerToCRS = null;
        resolution  = null;
        nonLinears  = 0;
        if (crs == null) {
            ArgumentChecks.ensureNonNull("extent", extent);
            envelope = null;
        } else {
            final double[] coords = new double[crs.getCoordinateSystem().getDimension()];
            Arrays.fill(coords, Double.NaN);
            envelope = new ImmutableEnvelope(coords, coords, crs);
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
     * The returned envelope encompasses all cell surfaces, from the left border of leftmost pixel
     * to the right border of the rightmost pixel and similarly along other axes.
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

    /**
     * Returns the coordinate range of the grid intersecting the given spatiotemporal region.
     * The given envelope can be expressed in any coordinate reference system (CRS).
     * That envelope CRS may have fewer dimensions than this grid geometry CRS,
     * in which case grid dimensions not mapped to envelope dimensions will be returned unchanged.
     * In other words, the {@code GridGeometry} dimensions not found in the given {@code areaOfInterest}
     * will be unfiltered (not discarded).
     *
     * <p>If the envelope CRS is not specified, then it is assumed the same than the CRS of this grid geometry.
     * In such case the envelope needs to contain all dimensions.</p>
     *
     * @param  areaOfInterest  the desired spatiotemporal region in any CRS (transformations will be applied as needed).
     * @return a grid extent of the same dimension than the grid geometry which intersects the given area of interest.
     * @throws IncompleteGridGeometryException if this grid geometry has no extent or no "grid to CRS" transform.
     * @throws TransformException if an error occurred while converting the envelope coordinates to grid coordinates.
     *
     * @see #GridGeometry(GridGeometry, GridExtent)
     */
    public GridExtent getExtent(final Envelope areaOfInterest) throws IncompleteGridGeometryException, TransformException {
        ArgumentChecks.ensureNonNull("areaOfInterest", areaOfInterest);
        if (extent == null) {
            throw incomplete(EXTENT, Resources.Keys.UnspecifiedGridExtent);
        }
        MathTransform gridToAOI = cornerToCRS;
        if (gridToAOI == null) {
            throw incomplete(GRID_TO_CRS, Resources.Keys.UnspecifiedTransform);
        }
        int[] modifiedDimensions = null;
        try {
            /*
             * If the envelope CRS is different than the expected CRS, concatenate the envelope transformation
             * to the 'gridToCRS' transform.  We should not transform the envelope here - only concatenate the
             * transforms - because transforming envelopes twice add errors.
             */
            if (envelope != null) {
                final CoordinateReferenceSystem sourceCRS = envelope.getCoordinateReferenceSystem();
                if (sourceCRS != null) {
                    final CoordinateReferenceSystem targetCRS = areaOfInterest.getCoordinateReferenceSystem();
                    if (targetCRS != null && !Utilities.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                        final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox();
                        bbox.setBounds(areaOfInterest);
                        gridToAOI = MathTransforms.concatenate(gridToAOI, findOperation(sourceCRS, targetCRS, bbox).getMathTransform());
                    }
                }
            }
            /*
             * If the envelope dimensions does not encompass all grid dimensions, the envelope is probably non-invertible.
             * We need to reduce the number of grid dimensions in the transform for having a one-to-one relationship.
             */
            final int modifiedDimensionCount = gridToAOI.getTargetDimensions();
            ArgumentChecks.ensureDimensionMatches("areaOfInterest", modifiedDimensionCount, areaOfInterest);
            if (modifiedDimensionCount < gridToAOI.getSourceDimensions()) {
                final TransformSeparator sep = new TransformSeparator(gridToAOI);
                sep.setTrimSourceDimensions(true);
                gridToAOI = sep.separate();
                modifiedDimensions = sep.getSourceDimensions();
                if (modifiedDimensions.length != modifiedDimensionCount) {
                    throw new TransformException(Resources.format(Resources.Keys.CanNotMapToGridDimensions));
                }
            }
        } catch (FactoryException e) {
            throw new TransformException(Resources.format(Resources.Keys.CanNotMapToGridDimensions), e);
        }
        final GridExtent sub = new GridExtent(Envelopes.transform(gridToAOI.inverse(), areaOfInterest), extent, modifiedDimensions);
        return sub.equals(extent) ? extent : sub;
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
     * Current implementation is equivalent to the following:
     *
     * {@preformat java
     *   return toString(EXTENT | ENVELOPE | CRS | GRID_TO_CRS | RESOLUTION);
     * }
     */
    @Override
    public String toString() {
        return toString(EXTENT | ENVELOPE | CRS | GRID_TO_CRS | RESOLUTION);
    }

    /**
     * Returns a string representation of some elements of this grid geometry.
     * The string representation is for debugging purpose only and may change
     * in any future SIS version.
     *
     * @param  bitmask  combination of {@link #EXTENT}, {@link #ENVELOPE},
     *         {@link #CRS}, {@link #GRID_TO_CRS} and {@link #RESOLUTION}.
     * @return a string representation of the given elements.
     */
    @Debug
    public String toString(final int bitmask) {
        if ((bitmask & ~(EXTENT | ENVELOPE | CRS | GRID_TO_CRS | RESOLUTION)) != 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "bitmask", bitmask));
        }
        return new Formatter(new StringBuilder(512), bitmask, System.lineSeparator()).toString();
    }

    /**
     * Formats a string representation of this grid geometry in the specified buffer.
     * This is used for reducing the amount of copies in {@link GridCoverage#toString()}.
     */
    final void formatTo(final StringBuilder out, final String lineSeparator) {
        new Formatter(out, EXTENT | ENVELOPE | CRS | GRID_TO_CRS | RESOLUTION, lineSeparator).format();
    }

    /**
     * Helper class for formatting a {@link GridGeometry} instance.
     */
    private final class Formatter {
        /**
         * Combination of {@link #EXTENT}, {@link #ENVELOPE},
         * {@link #CRS}, {@link #GRID_TO_CRS} and {@link #RESOLUTION}.
         */
        private final int bitmask;

        /**
         * Where to write the {@link GridGeometry} string representation.
         */
        private final StringBuilder buffer;

        /**
         * Platform-specific end-of-line characters.
         */
        private final String lineSeparator;

        /**
         * Localized words.
         */
        private final Vocabulary vocabulary;

        /**
         * The locale for the texts. Not used for numbers and dates.
         */
        private final Locale locale;

        /**
         * The coordinate reference system, or {@code null} if none.
         */
        private final CoordinateReferenceSystem crs;

        /**
         * The coordinate system, or {@code null} if none.
         */
        private final CoordinateSystem cs;

        /**
         * Creates a new formatter for the given combination of {@link #EXTENT}, {@link #ENVELOPE},
         * {@link #CRS}, {@link #GRID_TO_CRS} and {@link #RESOLUTION}.
         */
        Formatter(final StringBuilder out, final int bitmask, final String lineSeparator) {
            this.buffer        = out;
            this.bitmask       = bitmask;
            this.lineSeparator = lineSeparator;
            this.locale        = Locale.getDefault(Locale.Category.DISPLAY);
            this.vocabulary    = Vocabulary.getResources(locale);
            this.crs           = (envelope != null) ? envelope.getCoordinateReferenceSystem() : null;
            this.cs            = (crs != null) ? crs.getCoordinateSystem() : null;
        }

        /**
         * Returns a string representation of the enclosing {@link GridGeometry} instance.
         */
        @Override
        public final String toString() {
            format();
            return buffer.toString();
        }

        /**
         * Formats a string representation of the enclosing {@link GridGeometry} instance
         * in the buffer specified at construction time.
         */
        final void format() {
            /*
             * Example: Grid extent
             * ├─ Dimension 0: [370 … 389]  (20 cells)
             * └─ Dimension 1: [ 41 … 340] (300 cells)
             */
            if (section(EXTENT, Vocabulary.Keys.GridExtent, extent)) {
                extent.appendTo(buffer, vocabulary, true);
            }
            /*
             * Example: Envelope
             * ├─ Geodetic latitude:  -69.75 … 80.25  Δφ = 0.5°
             * └─ Geodetic longitude:   4.75 … 14.75  Δλ = 0.5°
             */
            if (section(ENVELOPE, Vocabulary.Keys.Envelope, envelope)) {
                final boolean appendResolution = (bitmask & RESOLUTION) != 0 && resolution != null;
                final TableAppender table = new TableAppender(buffer, "");
                final int dimension = envelope.getDimension();
                for (int i=0; i<dimension; i++) {
                    final CoordinateSystemAxis axis = (cs != null) ? cs.getAxis(i) : null;
                    final String name = (axis != null) ? axis.getName().getCode() :
                            vocabulary.getString(Vocabulary.Keys.Dimension_1, i);
                    GridExtent.branch(table, i < dimension - 1);
                    table.append(name).append(": ").nextColumn();
                    table.setCellAlignment(TableAppender.ALIGN_RIGHT);
                    table.append(Double.toString(envelope.getLower(i))).nextColumn();
                    table.setCellAlignment(TableAppender.ALIGN_LEFT);
                    table.append(" … ").append(Double.toString(envelope.getUpper(i)));
                    if (appendResolution && !Double.isNaN(resolution[i])) {
                        final boolean isLinear = (i < Long.SIZE) && (nonLinears & (1L << i)) == 0;
                        table.nextColumn();
                        table.append("  Δ");
                        if (axis != null) {
                            table.append(axis.getAbbreviation());
                        }
                        table.nextColumn();
                        table.append(' ').append(isLinear ? '=' : '≈').append(' ');
                        appendResolution(table, i);
                    }
                    table.nextLine();
                }
                GridExtent.flush(table);
            } else if (section(RESOLUTION, Vocabulary.Keys.Resolution, resolution)) {
                /*
                 * Example: Resolution
                 * └─ 0.5° × 0.5°
                 */
                String separator = "└─ ";
                for (int i=0; i<resolution.length; i++) {
                    appendResolution(buffer.append(separator), i);
                    separator = " × ";
                }
                buffer.append(lineSeparator);
            }
            /*
             * Example: Coordinate reference system
             * └─ EPSG:4326 — WGS 84 (φ,λ)
             */
            if (section(CRS, Vocabulary.Keys.CoordinateRefSys, crs)) {
                buffer.append("└─ ");
                final Identifier id = IdentifiedObjects.getIdentifier(crs, null);
                if (id != null) {
                    buffer.append(IdentifiedObjects.toString(id)).append(" — ");
                }
                buffer.append(crs.getName()).append(lineSeparator);
            }
            /*
             * Example: Conversion
             * └─ 2D → 2D non linear in 2
             */
            if (section(GRID_TO_CRS, Vocabulary.Keys.Conversion, gridToCRS)) {
                final Matrix matrix = MathTransforms.getMatrix(gridToCRS);
                if (matrix != null) {
                    String separator = "└─ ";
                    for (final CharSequence line : CharSequences.splitOnEOL(Matrices.toString(matrix))) {
                        buffer.append(separator).append(line).append(lineSeparator);
                        separator = "   ";
                    }
                } else {
                    buffer.append("└─ ").append(gridToCRS.getSourceDimensions()).append("D → ")
                                        .append(gridToCRS.getTargetDimensions()).append('D');
                    long nonLinearDimensions = nonLinears;
                    String separator = " non linear in ";
                    while (nonLinearDimensions != 0) {
                        final int i = Long.numberOfTrailingZeros(nonLinearDimensions);
                        nonLinearDimensions &= ~(1L << i);
                        buffer.append(separator).append(cs != null ? cs.getAxis(i).getName().getCode() : String.valueOf(i));
                        separator = ", ";
                    }
                    buffer.append(lineSeparator);
                }
            }
        }

        /**
         * Starts a new section for the given property.
         *
         * @param  property  one of {@link #EXTENT}, {@link #ENVELOPE}, {@link #CRS}, {@link #GRID_TO_CRS} and {@link #RESOLUTION}.
         * @param  title     the {@link Vocabulary} key for the title to show for this section, if formatted.
         * @param  value     the value to be formatted in that section.
         * @return {@code true} if the caller shall format the value.
         */
        private boolean section(final int property, final short title, final Object value) {
            if ((bitmask & property) != 0) {
                buffer.append(vocabulary.getString(title));
                if (title == Vocabulary.Keys.Conversion) {
                    buffer.append(" (").append(vocabulary.getString(Vocabulary.Keys.OriginInCellCenter).toLowerCase(locale)).append(')');
                }
                buffer.append(lineSeparator);
                if (value != null) {
                    return true;
                }
                buffer.append("└─ ").append(vocabulary.getString(Vocabulary.Keys.Unspecified)).append(lineSeparator);
            }
            return false;
        }

        private void appendResolution(final Appendable out, final int dimension) {
            try {
                out.append(Float.toString((float) resolution[dimension]));
                if (cs != null) {
                    final String unit = String.valueOf(cs.getAxis(dimension).getUnit());
                    if (unit.isEmpty() || Character.isLetterOrDigit(unit.codePointAt(0))) {
                        out.append(' ');
                    }
                    out.append(unit);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
