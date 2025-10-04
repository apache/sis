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

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.time.Instant;
import java.text.NumberFormat;
import java.math.RoundingMode;
import java.io.Serializable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.awt.image.RenderedImage;            // For javadoc only.
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.referencing.internal.shared.TemporalAccessor;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.io.TableAppender;
import org.apache.sis.xml.NilObject;
import org.apache.sis.xml.NilReason;
import static org.apache.sis.referencing.CRS.findOperation;

// Specific to the main branch:
import org.opengis.geometry.MismatchedDimensionException;


/**
 * Valid extent of grid coordinates together with the transform from those grid coordinates
 * to real world coordinates. {@code GridGeometry} contains:
 *
 * <ul class="verbose">
 *   <li>A {@linkplain #getExtent() grid extent} (a.k.a. <i>grid envelope</i>),
 *       often inferred from the {@link RenderedImage} size.</li>
 *   <li>A {@linkplain #getGridToCRS grid to CRS} transform,
 *       which may be inferred from the grid extent and the georeferenced envelope.</li>
 *   <li>A {@linkplain #getEnvelope() georeferenced envelope}, which can be inferred
 *       from the grid extent and the <i>grid to CRS</i> transform.</li>
 *   <li>An optional {@linkplain #getCoordinateReferenceSystem() Coordinate Reference System} (CRS)
 *       specified as part of the georeferenced envelope.
 *       This CRS is the target of the <i>grid to CRS</i> transform.</li>
 *   <li>An <em>estimation</em> of {@link #getResolution(boolean) grid resolution} along each CRS axes,
 *       computed from the <i>grid to CRS</i> transform and eventually from the grid extent.</li>
 *   <li>An {@linkplain #isConversionLinear indication of whether conversion for some axes is linear or not}.</li>
 * </ul>
 *
 * The first three properties should be mandatory,
 * but are allowed to be temporarily absent during grid coverage construction.
 * Temporarily absent properties are allowed because they may be inferred from a wider context.
 * For example, a {@code GridGeometry} knows nothing about {@link RenderedImage},
 * but {@code GridCoverage2D} has this information and may use it for providing a missing grid extent.
 * By default, any request for an undefined property will throw an {@link IncompleteGridGeometryException}.
 * In order to check if a property is defined, use {@link #isDefined(int)}.
 *
 * <h2>Non-linear referencing</h2>
 * A key property is the {@linkplain #getGridToCRS(PixelInCell) "grid to CRS"} conversion,
 * which defines how to map pixel coordinates to "real world" coordinates such as latitudes and longitudes.
 * This relationship is often linear (an affine transform), but {@linkplain #isConversionLinear not necessarily};
 * {@code GridGeometry} accepts non-linear conversions as well. Non-linear conversions may occur with images
 * using {@linkplain org.apache.sis.referencing.operation.builder.LocalizationGridBuilder localization grids},
 * but non-linear conversions should not be used for expressing map projections (projections should be specified
 * in the {@linkplain #getCoordinateReferenceSystem() Coordinate Reference System} (CRS) instead).
 *
 * <p>Some applications cannot handle non-linear "grid to CRS" conversions.
 * For example, encoding an image in a GeoTIFF file is much simpler if the "grid to CRS" conversion is linear.
 * The {@link DomainLinearizer} class can be used for replacing non-linear conversions by linear approximations.</p>
 *
 * <h2>Multi-threading</h2>
 * {@code GridGeometry} instances are immutable and thread-safe.
 * The same instance can be shared by different {@link GridCoverage} instances.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.0
 */
public class GridGeometry implements LenientComparable, Serializable {
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
     * A bitmask to specify the validity of the <q>grid to CRS</q> transform.
     *
     * @see #isDefined(int)
     * @see #getGridToCRS(PixelInCell)
     */
    public static final int GRID_TO_CRS = 8;

    /**
     * A bitmask to specify the validity of the "real world" coordinates of cell at indices (0, 0, …, 0).
     *
     * @see #getOrigin()
     * @since 1.5
     */
    public static final int ORIGIN = 128;

    /**
     * A bitmask to specify the validity of the grid resolution.
     *
     * @see #isDefined(int)
     * @see #getResolution(boolean)
     */
    public static final int RESOLUTION = 16;

    /**
     * A bitmask to specify the validity of the geographic bounding box.
     * This information can sometimes be derived from the envelope and the CRS.
     * It is an optional element even with a complete grid geometry since the
     * coordinate reference system is not required to have an horizontal component.
     *
     * @see #getGeographicExtent()
     */
    public static final int GEOGRAPHIC_EXTENT = 32;

    /**
     * A bitmask to specify the validity of the temporal period.
     * This information can sometimes be derived from the envelope and the CRS.
     * It is an optional element even with a complete grid geometry since the
     * coordinate reference system is not required to have a temporal component.
     *
     * @see #getTemporalExtent()
     */
    public static final int TEMPORAL_EXTENT = 64;

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
     * If non-null, the conversion shall map {@linkplain PixelInCell#CELL_CENTER cell center}.
     * This conversion is usually, but not necessarily, affine.
     *
     * @see #CRS
     * @see #getGridToCRS(PixelInCell)
     * @see PixelInCell#CELL_CENTER
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final MathTransform gridToCRS;

    /**
     * Same conversion as {@link #gridToCRS} but from {@linkplain PixelInCell#CELL_CORNER cell corner}
     * instead of center. This transform is preferable to {@code gridToCRS} for transforming envelopes.
     *
     * @serial This field is serialized because it may be a value specified explicitly at construction time,
     *         in which case it can be more accurate than a computed value.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    final MathTransform cornerToCRS;

    /**
     * An <em>estimation</em> of the grid resolution, in units of the CRS axes.
     * Computed from {@link #gridToCRS}, eventually together with {@link #extent}.
     * May be {@code null} if unknown. If non-null, the array length is equal to
     * the number of CRS dimensions.
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
    final long nonLinears;

    /**
     * The geographic bounding box as an unmodifiable metadata instance, or {@code null} if not yet computed.
     * If no geographic extent can be computed for the {@linkplain #envelope}, then this is set to {@link NilObject}.
     *
     * @see #GEOGRAPHIC_EXTENT
     * @see #getGeographicExtent()
     */
    private transient volatile GeographicBoundingBox geographicBBox;

    /**
     * The start time and end time, or {@code null} if not yet computed. If there is no time range, then the array is empty.
     * If there is only a start time or an end time, then the array length is 1. Otherwise the array length is 2.
     *
     * @see #TEMPORAL_EXTENT
     * @see #getTemporalExtent()
     */
    @SuppressWarnings("VolatileArrayField")                 // Safe because array will not be modified after construction.
    private transient volatile Instant[] timeRange;

    /**
     * An "empty" grid geometry with no value defined. All getter methods invoked on this instance will cause
     * {@link IncompleteGridGeometryException} to be thrown. This instance can be used as a place-holder when
     * the grid geometry cannot be obtained.
     */
    public static final GridGeometry UNDEFINED = new GridGeometry();

    /**
     * Constructor for {@link #UNDEFINED} singleton only.
     */
    private GridGeometry() {
        extent      = null;
        gridToCRS   = null;
        cornerToCRS = null;
        envelope    = null;
        resolution  = null;
        nonLinears  = 0;
    }

    /**
     * Creates a new grid geometry with the same values as the given grid geometry.
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
     * Creates a new grid geometry derived from the given grid geometry with a new extent and a modified transform.
     * This constructor is used for creating a grid geometry over a subregion (for example with the grid extent
     * computed by {@link GridDerivation#subgrid(Envelope, double...)}) or grid geometry for a subsampled raster.
     *
     * <h4>Conversion between old and new grid geometry</h4>
     * If {@code toOther} is non-null, it defines the conversion from grid coordinates of given {@code extent} to grid
     * coordinates of {@code other}. That transform should be a {@linkplain MathTransforms#scale(double...) scale} and
     * a {@linkplain MathTransforms#translation(double...) translation} only, but more complex transforms are accepted.
     * The {@link #cornerToCRS} transform of the new grid geometry will be set to the following concatenation:
     *
     * <blockquote>{@code this.cornerToCRS} = {@code toOther} → {@code other.cornerToCRS}</blockquote>
     *
     * The new {@linkplain #getEnvelope() grid geometry envelope} will be computed from the new extent and transform,
     * then {@linkplain GeneralEnvelope#intersect(Envelope) clipped} to the envelope of the other grid geometry.
     * This clip is for preventing the envelope to become larger under the effect of subsampling because
     * {@linkplain GridExtent#subsample(long[]) each cell become larger}. The clip is not applied when {@code toOther}
     * is {@code null} because in such case, we presume that the grid extent has been changed for another reason than
     * subsampling (e.g. application of a margin, in which case we want the envelope to be expanded).
     *
     * @param  other    the other grid geometry to copy.
     * @param  extent   the new extent for the grid geometry to construct, or {@code null} if none.
     * @param  toOther  transform from this grid coordinates to {@code other} grid coordinates, or {@code null} if none.
     * @throws NullPointerException if {@code extent} is {@code null} and the other grid geometry contains no other information.
     * @throws TransformException if the math transform cannot compute the geospatial envelope from the grid extent.
     *
     * @see GridDerivation#subgrid(Envelope, double...)
     *
     * @since 1.2
     */
    public GridGeometry(final GridGeometry other, final GridExtent extent, final MathTransform toOther) throws TransformException {
        final int dimension = other.getDimension();
        this.extent = extent;
        ensureDimensionMatches(dimension, extent);
        if (toOther == null || toOther.isIdentity()) {
            gridToCRS   = other.gridToCRS;
            cornerToCRS = other.cornerToCRS;
            resolution  = other.resolution;
            nonLinears  = other.nonLinears;
        } else {
            /*
             * The `toOther` transform applies on `cornerToCRS` because the corner of upper-left pixel before scaling
             * is still the corner of upper-left pixel after scaling, while "pixel center" is no longer the center of
             * the same pixel. We adjust `toOther` instead of invoking `PixelTranslation.translate(cornerToCRS, …)`
             * because we do not know which of `cornerToCRS` or `gridToCRS` has less NaN values.
             */
            if (other.gridToCRS != null) {
                final MathTransform centerShift = MathTransforms.concatenate(
                        MathTransforms.uniformTranslation(dimension, +0.5), toOther,
                        MathTransforms.uniformTranslation(dimension, -0.5));
                cornerToCRS = MathTransforms.concatenate(toOther, other.cornerToCRS);
                gridToCRS   = MathTransforms.concatenate(centerShift, other.gridToCRS);
                resolution  = resolution(gridToCRS, extent, PixelInCell.CELL_CENTER);
                nonLinears  = findNonLinearTargets(gridToCRS);
            } else {
                cornerToCRS = null;
                gridToCRS   = null;
                resolution  = resolution(toOther, extent, PixelInCell.CELL_CENTER);     // Save resolution even if `gridToCRS` is null.
                nonLinears  = findNonLinearTargets(toOther);
            }
        }
        /*
         * Recompute the envelope and clip only if a sub-sampling may have been applied (toOther != null).
         * The reason for clipping is because subsampling may cause cells to appear larger.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        ImmutableEnvelope envelope = other.envelope;            // We will share the same instance if possible.
        ImmutableEnvelope computed = computeEnvelope(
                gridToCRS,
                getCoordinateReferenceSystem(envelope),
                toOther == null ? null : envelope);             // Clip.
        if (computed == null || !computed.equals(envelope)) {
            envelope = computed;
        }
        this.envelope = envelope;
        if (envelope == null && gridToCRS == null) {
            ArgumentChecks.ensureNonNull("extent", extent);
        }
    }

    /*
     * Do not provide convenience constructor without PixelInCell or PixelOrientation argument.
     * Experience shows that 0.5 pixel offsets in image georeferencing is a recurrent problem.
     * We really want to force developers to think about whether their `gridToCRS` transform
     * locates pixel corner or pixel center.
     */

    /**
     * Creates a new grid geometry from a grid extent and a mapping from cell coordinates to "real world" coordinates.
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
     * <h4>API note</h4>
     * There is no default value for {@code anchor} because experience shows that images shifted by ½ pixel
     * (with pixels that may be tens of kilometres large) is a recurrent problem. We want to encourage developers
     * to always think about wether their <i>grid to CRS</i> transform is mapping pixel corner or center.
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
     * @throws IllegalGridGeometryException if the math transform cannot compute the geospatial envelope or resolution from the grid extent.
     */
    public GridGeometry(final GridExtent extent, final PixelInCell anchor, final MathTransform gridToCRS, final CoordinateReferenceSystem crs) {
        if (gridToCRS != null) {
            ensureDimensionMatches(gridToCRS.getSourceDimensions(), extent);
            ArgumentChecks.ensureDimensionMatches("crs", gridToCRS.getTargetDimensions(), crs);
        } else if (crs == null) {
            ArgumentChecks.ensureNonNull("extent", extent);
        }
        try {
            this.extent      = extent;
            this.gridToCRS   = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CENTER);
            this.cornerToCRS = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CORNER);
            this.envelope    = computeEnvelope(gridToCRS, crs, null);   // `gridToCRS` specified by the user, not `this.gridToCRS`.
            this.resolution  = resolution(gridToCRS, extent, anchor);   // `gridToCRS` or `cornerToCRS` does not matter here.
            this.nonLinears  = findNonLinearTargets(gridToCRS);
        } catch (TransformException e) {
            throw new IllegalGridGeometryException(e, "gridToCRS");
        }
    }

    /**
     * Computes the envelope with the given coordinate reference system. This method is invoked from constructors.
     * The {@link #extent}, {@link #gridToCRS} and {@link #cornerToCRS} fields must be set before this method is invoked.
     *
     * @param  specified  the transform specified by the user. This is not necessarily {@link #gridToCRS}.
     * @param  crs        the coordinate reference system to declare in the envelope. May be {@code null}.
     * @param  limits     if non-null, intersect with that envelope. The CRS must be the same as {@code crs}.
     */
    private ImmutableEnvelope computeEnvelope(final MathTransform specified, final CoordinateReferenceSystem crs,
            final Envelope limits) throws TransformException
    {
        final GeneralEnvelope env;
        if (extent != null && cornerToCRS != null) {
            env = extent.toEnvelope(cornerToCRS, false, specified, limits);
            env.setCoordinateReferenceSystem(crs);
            if (limits != null) {
                env.intersect(limits);
            }
        } else if (crs != null) {
            env = new GeneralEnvelope(crs);
            env.setToNaN();
        } else {
            return null;
        }
        return new ImmutableEnvelope(env);
    }

    /**
     * Creates a new grid geometry from a geospatial envelope and a mapping from cell coordinates to "real world" coordinates.
     * At least one of {@code gridToCRS} or {@code envelope} arguments shall be non-null.
     * If {@code gridToCRS} is non-null, then {@code anchor} shall be non-null too with one of the values documented in the
     * {@link #GridGeometry(GridExtent, PixelInCell, MathTransform, CoordinateReferenceSystem) constructor expecting a grid
     * extent}.
     *
     * <p>The given envelope shall encompass all cell surfaces, from the left border of leftmost cell to the right border
     * of the rightmost cell and similarly along other axes. This constructor tries to store a geospatial envelope close
     * to the specified envelope, but there is no guarantee that the envelope returned by {@link #getEnvelope()} will be
     * equal to the given envelope. The envelope stored in the new {@code GridGeometry} may be slightly smaller, larger or
     * shifted because the floating point values used in geospatial envelope cannot always be mapped to the integer
     * coordinates used in {@link GridExtent}.
     * The rules for deciding whether coordinates should be rounded toward nearest integers,
     * to {@linkplain Math#floor(double) floor} or to {@linkplain Math#ceil(double) ceil} values
     * are specified by the {@link GridRoundingMode} argument.</p>
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
     *                    There is no guarantee that the envelope actually stored in the {@code GridGeometry}
     *                    will be equal to this specified envelope.
     * @param  rounding   controls behavior of rounding from floating point values to integers.
     * @throws IllegalGridGeometryException if the math transform cannot compute the grid extent or the resolution.
     */
    public GridGeometry(final PixelInCell anchor, final MathTransform gridToCRS, final Envelope envelope, final GridRoundingMode rounding) {
        if (gridToCRS == null) {
            ArgumentChecks.ensureNonNull("envelope", envelope);
        } else {
            ArgumentChecks.ensureDimensionMatches("envelope", gridToCRS.getTargetDimensions(), envelope);
        }
        ArgumentChecks.ensureNonNull("rounding", rounding);
        this.gridToCRS   = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CENTER);
        this.cornerToCRS = PixelTranslation.translate(gridToCRS, anchor, PixelInCell.CELL_CORNER);
        Matrix scales = MathTransforms.getMatrix(gridToCRS);
        int numToIgnore = 1;
        if (envelope != null && cornerToCRS != null) {
            GeneralEnvelope env;
            try {
                env = Envelopes.transform(cornerToCRS.inverse(), envelope);
                extent = new GridExtent(env, false, rounding, GridClippingMode.STRICT, null, null, null, null);
                env = extent.toEnvelope(cornerToCRS, false, gridToCRS, envelope);
                // Use `gridToCRS` specified by the user, not `this.gridToCRS`.
            } catch (TransformException e) {
                throw new IllegalGridGeometryException(e, "gridToCRS");
            }
            env.setCoordinateReferenceSystem(envelope.getCoordinateReferenceSystem());
            this.envelope = new ImmutableEnvelope(env);
            if (scales == null) try {
                // `gridToCRS` cannot be null if `cornerToCRS` is non-null.
                scales = gridToCRS.derivative(new DirectPositionView.Double(extent.getPointOfInterest(anchor)));
                numToIgnore = 0;
            } catch (TransformException e) {
                recoverableException("<init>", e);
            }
        } else {
            this.extent   = null;
            this.envelope = ImmutableEnvelope.castOrCopy(envelope);
        }
        resolution = (scales != null) ? resolution(scales, numToIgnore) : null;
        nonLinears = findNonLinearTargets(gridToCRS);
    }

    /**
     * Ensures that the given dimension is equal to the expected value. If not, throws an exception.
     * This method assumes that the argument name is {@code "extent"}.
     *
     * @param extent    the extent to validate, or {@code null} if none.
     * @param expected  the expected number of dimension.
     */
    private static void ensureDimensionMatches(final int expected, final GridExtent extent) throws MismatchedDimensionException {
        if (extent != null) {
            final int dimension = extent.getDimension();
            if (dimension != expected) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "extent", expected, dimension));
            }
        }
    }

    /**
     * Invoked when a recoverable exception occurred. Those exceptions must be minor enough
     * that they can be silently ignored in most cases.
     *
     * @param  caller     the method where exception occurred.
     * @param  exception  the exception that occurred.
     */
    static void recoverableException(final String caller, final TransformException exception) {
        Logging.recoverableException(GridExtent.LOGGER, GridGeometry.class, caller, exception);
    }

    /**
     * Creates an axis-aligned grid geometry with an extent and an envelope.
     * This constructor can be used when the <i>grid to CRS</i> transform is unknown.
     * If only the coordinate reference system is known, then the envelope coordinates can be
     * {@linkplain GeneralEnvelope#isAllNaN() all NaN}.
     *
     * <p>The main purpose of this constructor is to create "desired" grid geometries, for example for use in
     * {@linkplain org.apache.sis.storage.GridCoverageResource#read(GridGeometry, int...) read} or
     * {@linkplain org.apache.sis.coverage.grid.GridCoverageProcessor#resample resample} operations.
     * For grid geometries describing preexisting data, it is safer and more flexible to use one of
     * the constructors expecting a {@link MathTransform} argument.</p>
     *
     * <h4>Dimension order</h4>
     * The given envelope shall always declare dimensions in same order as the given extent.
     * This constructor may reorder axes if {@code orientation} is {@link GridOrientation#DISPLAY},
     * but in such case this constructor will derive a new envelope and a new CRS from the given envelope.
     *
     * @param  extent       the valid extent of grid coordinates, or {@code null} if unknown.
     * @param  envelope     the envelope together with CRS of the "real world" coordinates, or {@code null} if unknown.
     * @param  orientation  high-level description of desired characteristics of the {@code gridToCRS} transform.
     *                      Ignored (can be null) if {@code envelope} is null.
     * @throws NullPointerException if {@code extent} and {@code envelope} arguments are both null,
     *         or if {@code envelope} is non-null but {@code orientation} is null.
     * @throws MismatchedDimensionException if the envelope does not have the same number of dimensions than the grid extent.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Axis-aligned_object">Axis-aligned object on Wikipedia</a>
     *
     * @since 1.1
     */
    public GridGeometry(GridExtent extent, final Envelope envelope, final GridOrientation orientation) {
        nonLinears = 0;
        /*
         * Potentially change axis order and orientation according the given `GridOrientation` (which may be null).
         * Current code assumes that units of measurement are unchanged, which should be true for CRSs built using
         * AxisConvention.DISPLAY_ORIENTED.
         */
        long flip = 0;                      // Bitmask specifying whether to reverse axis in each dimension.
        ImmutableEnvelope target = null;    // May have different axis order than the specified `envelope` CRS.
        int[] sourceDimensions = null;      // Indices in source envelope of axes colinear with the target envelope.
        if (envelope != null) {
            if (extent != null) {
                ArgumentChecks.ensureDimensionMatches("envelope", extent.getDimension(), envelope);
            }
            ArgumentChecks.ensureNonNull("orientation", orientation);
            if (orientation.crsVariant != null) {
                final AbstractCRS sourceCRS = AbstractCRS.castOrCopy(envelope.getCoordinateReferenceSystem());
                if (sourceCRS != null) {
                    final AbstractCRS targetCRS = sourceCRS.forConvention(orientation.crsVariant);
                    if (targetCRS != sourceCRS) {
                        final CoordinateSystem sourceCS = sourceCRS.getCoordinateSystem();
                        final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
                        sourceDimensions = AxisDirections.indicesOfLenientMapping(sourceCS, targetCS);
                        if (sourceDimensions != null) {
                            final double[] lowerCorner = new double[sourceDimensions.length];
                            final double[] upperCorner = new double[sourceDimensions.length];
                            for (int i=0; i < sourceDimensions.length; i++) {
                                final int s = sourceDimensions[i];
                                lowerCorner[i] = envelope.getMinimum(s);
                                upperCorner[i] = envelope.getMaximum(s);
                                if (sourceCS.getAxis(s).getDirection() != targetCS.getAxis(i).getDirection()) {
                                    flip |= Numerics.bitmask(i);
                                }
                            }
                            target = new ImmutableEnvelope(lowerCorner, upperCorner, targetCRS);
                        }
                    }
                }
            }
        }
        if (target == null) {
            target = ImmutableEnvelope.castOrCopy(envelope);
        }
        /*
         * If the envelope contains no useful information, then the grid extent is mandatory.
         * We do that for forcing grid geometries to contain at least one information.
         */
        boolean nilEnvelope = true;
        if (target == null || ((nilEnvelope = target.isAllNaN()) && target.getCoordinateReferenceSystem() == null)) {
            if (target != null && nilEnvelope) {
                throw new NullPointerException(Errors.format(Errors.Keys.UnspecifiedCRS));
            }
            ArgumentChecks.ensureNonNull("extent", extent);
            this.envelope = null;
        } else {
            this.envelope = target;
            if (extent != null) {
                // A non-null `sourceDimensions` implies non-null `orientation`.
                if (sourceDimensions != null && orientation.canReorderGridAxis) {
                    if (!ArraysExt.isRange(0, sourceDimensions)) {
                        extent = extent.reorder(sourceDimensions);
                    }
                    sourceDimensions = null;
                }
                if (!nilEnvelope) {
                    /*
                     * If we have both the extent and an envelope with at least one non-NaN coordinates,
                     * create the `cornerToCRS` transform. The `gridToCRS` calculation uses the knowledge
                     * that all scale factors are on diagonal, which allows simpler calculation than full
                     * matrix multiplication. Use double-double arithmetic everywhere.
                     */
                    @SuppressWarnings("null")           // `!nilEnvelope` implies non-null `orientation`.
                    final MatrixSIS affine = extent.cornerToCRS(target, orientation.flippedAxes ^ flip, sourceDimensions);
                    cornerToCRS = MathTransforms.linear(affine);
                    final int srcDim = cornerToCRS.getSourceDimensions();       // Translation column in matrix.
                    final int tgtDim = cornerToCRS.getTargetDimensions();       // Number of matrix rows before last row.
                    resolution = new double[tgtDim];
                    for (int j=0; j<tgtDim; j++) {
                        final int i = (sourceDimensions != null) ? sourceDimensions[j] : j;
                        DoubleDouble scale  = DoubleDouble.of(affine.getNumber(j, i), true);
                        DoubleDouble offset = DoubleDouble.of(affine.getNumber(j, srcDim), true);
                        resolution[j] = Math.abs(scale.doubleValue());
                        offset = offset.add(scale.scalb(-1));
                        affine.setNumber(j, srcDim, offset);
                    }
                    gridToCRS = MathTransforms.linear(affine);
                    this.extent = extent;
                    return;
                }
            }
        }
        this.extent = extent;
        gridToCRS   = null;
        cornerToCRS = null;
        resolution  = null;
    }

    /**
     * Creates a grid geometry with only an envelope.
     *
     * @param  envelope  the envelope together with CRS of the "real world" coordinates.
     * @throws IllegalArgumentException if the envelope has no CRS and only NaN coordinate values.
     *
     * @since 1.4
     */
    public GridGeometry(final Envelope envelope) {
        // Implicit null value check below.
        this.envelope = ImmutableEnvelope.castOrCopy(envelope);
        if (this.envelope.isAllNaN() && this.envelope.getCoordinateReferenceSystem() == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "envelope"));
        }
        extent      = null;
        gridToCRS   = null;
        cornerToCRS = null;
        resolution  = null;
        nonLinears  = 0;
    }

    /**
     * Creates a new grid geometry from the given components.
     * This constructor performs no verification (unless assertions are enabled).
     */
    GridGeometry(final GridExtent extent, final MathTransform gridToCRS, final MathTransform cornerToCRS,
                 final ImmutableEnvelope envelope, final double[] resolution, final long nonLinears)
    {
        this.extent      = extent;
        this.gridToCRS   = gridToCRS;
        this.cornerToCRS = cornerToCRS;
        this.envelope    = envelope;
        this.resolution  = resolution;
        this.nonLinears  = nonLinears;
        if (gridToCRS != null) {
            assert (extent     == null) || gridToCRS.getSourceDimensions() == extent.getDimension();
            assert (envelope   == null) || gridToCRS.getTargetDimensions() == envelope.getDimension();
            assert (resolution == null) || gridToCRS.getTargetDimensions() == resolution.length;
        }
    }

    /**
     * Creates a new grid geometry as the concatenation of the two specified grid geometries.
     * The number of dimensions of the new grid geometry is the sum of the number of dimensions
     * of the two specified grid geometries. The dimensions of the lower grid geometry are first,
     * followed by the dimensions of the upper grid geometry.
     *
     * @param  lower  the grid geometry providing the lowest dimensions.
     * @param  upper  the grid geometry providing the highest dimensions.
     * @throws IncompleteGridGeometryException if a property presents in one grid geometry is absent in the other.
     * @throws IllegalArgumentException if the concatenation results in duplicated
     *         {@linkplain GridExtent#getAxisType(int) grid axis types}.
     * @throws FactoryException if the geodetic factory failed to create the compound CRS.
     *
     * @see #selectDimensions(int...)
     *
     * @since 1.5
     */
    public GridGeometry(final GridGeometry lower, final GridGeometry upper) throws FactoryException {
        /*
         * Concatenate the extents first because they perform a check for axis duplication.
         * We set the extent to null only if both `lower` and `upper` have a null extent.
         * Otherwise, invoke `getExtent()` for causing an `IncompleteGridGeometryException`
         * to be thrown if only one grid geometry has an extent.
         */
        extent = (lower.extent == null && upper.extent == null) ? null
               : new GridExtent(lower.getExtent(), upper.getExtent());
        /*
         * Concatenate the "grid to CRS" transforms and derived information
         * (resolutions and non-linearity).
         */
        if (lower.gridToCRS == null && upper.gridToCRS == null) {
            gridToCRS   = null;
            cornerToCRS = null;
        } else {
            gridToCRS   = compound(lower, upper, PixelInCell.CELL_CENTER);
            cornerToCRS = compound(lower, upper, PixelInCell.CELL_CORNER);
        }
        nonLinears = lower.nonLinears | (upper.nonLinears << lower.getDimension());
        resolution = (lower.resolution == null && upper.resolution == null) ? null
                   : ArraysExt.concatenate(lower.getResolution(true), upper.getResolution(true));
        /*
         * The check for presence/absence of envelope is more complex because an envelope
         * may be considered missing, but still be non-null in order to store the CRS.
         */
        if (lower.envelope == null && upper.envelope == null) {
            envelope = null;
        } else {
            Envelope e1 = lower.envelope; if (e1 == null) e1 = lower.getEnvelope();
            Envelope e2 = upper.envelope; if (e2 == null) e2 = upper.getEnvelope();
            envelope = ImmutableEnvelope.castOrCopy(Envelopes.compound(e1, e2));
        }
    }

    /**
     * Aggregates the dimensions of the "grid to CRS" transforms of the given grid geometries.
     */
    private static MathTransform compound(final GridGeometry lower, final GridGeometry upper, final PixelInCell anchor) {
        return MathTransforms.compound(lower.getGridToCRS(anchor), upper.getGridToCRS(anchor));
    }

    /**
     * Converts a "grid to CRS" transform from "cell corner" convention to "cell center" convention.
     * This is a helper method for use of {@link #GridGeometry(GridExtent, MathTransform, MathTransform,
     * ImmutableEnvelope, double[], long)} constructor.
     *
     * @param  gridToCRS  the transform to convert, or {@code null} if none.
     * @return the converted transform, or {@code null} if the given transform was null.
     */
    private static MathTransform cornerToCenter(final MathTransform gridToCRS) {
        return PixelTranslation.translate(gridToCRS, PixelInCell.CELL_CORNER, PixelInCell.CELL_CENTER);
    }

    /**
     * Returns the number of dimensions of the <em>grid</em>. This is typically the same
     * than the number of {@linkplain #getEnvelope() envelope} dimensions or the number of
     * {@linkplain #getCoordinateReferenceSystem() coordinate reference system} dimensions,
     * but not necessarily.
     *
     * @return the number of grid dimensions.
     *
     * @see #selectDimensions(int[])
     * @see GridExtent#getDimension()
     */
    public final int getDimension() {
        if (extent != null) {
            return extent.getDimension();       // Most reliable source since that method is final.
        } else if (gridToCRS != null) {
            return gridToCRS.getSourceDimensions();
        } else if (envelope != null) {
            /*
             * Last resort only since we have no guarantee that the envelope dimension is the same
             * than the grid dimension (see above javadoc). The envelope should never be null at
             * this point since the constructor verified that at least one argument was non-null,
             * except if this method is invoked on UNDEFINED.
             */
            return envelope.getDimension();
        } else {
            throw incomplete(EXTENT, Resources.Keys.UnspecifiedGridExtent);
        }
    }

    /**
     * Returns the number of dimensions of the <em>CRS</em>. This is typically the same as the
     * number of {@linkplain #getDimension() grid dimensions}, but not necessarily.
     */
    final int getTargetDimension() {
        if (envelope != null) {
            return envelope.getDimension();     // Most reliable source since that class is final.
        } else if (gridToCRS != null) {
            return gridToCRS.getTargetDimensions();
        } else {
            /*
             * Last resort only since we have no guarantee that the grid dimension is the same
             * then the CRS dimension (converse of the rational in getDimension() method).
             */
            return extent.getDimension();
        }
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
    public GridExtent getExtent() {
        if (extent != null) {
            return extent;
        }
        throw incomplete(EXTENT, Resources.Keys.UnspecifiedGridExtent);
    }

    /**
     * Returns the conversion from grid coordinates to "real world" coordinates.
     * The conversion is often an affine transform, but not necessarily.
     * Conversions from cell indices to geospatial coordinates can be performed for example as below:
     *
     * {@snippet lang="java" :
     *     MathTransform  gridToCRS     = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
     *     DirectPosition indicesOfCell = new GeneralDirectPosition(2, 3, 4):
     *     DirectPosition aPixelCenter  = gridToCRS.transform(indicesOfCell, null);
     *     }
     *
     * Callers must specify whether they want the "real world" coordinates of cell center or cell corner.
     * The cell corner is the one for which all grid indices have the smallest values (closest to negative infinity).
     * As a rule of thumb:
     *
     * <ul>
     *   <li>Use {@link PixelInCell#CELL_CENTER} for transforming <em>points</em>.</li>
     *   <li>Use {@link PixelInCell#CELL_CORNER} for transforming <em>envelopes</em>
     *       with inclusive lower coordinates and <strong>exclusive</strong> upper coordinates.</li>
     * </ul>
     *
     * <h4>API note</h4>
     * There is no default value for {@code anchor} because experience shows that images shifted by ½ pixel
     * (with pixels that may be tens of kilometres large) is a recurrent problem. We want to encourage developers
     * to always think about wether the desired <i>grid to CRS</i> transform shall map pixel corner or center.
     *
     * @param  anchor  the cell part to map (center or corner).
     * @return the conversion from grid coordinates to "real world" coordinates (never {@code null}).
     * @throws IllegalArgumentException if the given {@code anchor} is not a known code list value.
     * @throws IncompleteGridGeometryException if this grid geometry has no transform —
     *         i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #GRID_TO_CRS})</code> returned {@code false}.
     */
    public MathTransform getGridToCRS(final PixelInCell anchor) {
        final MathTransform mt;
        switch (anchor) {
            case CELL_CENTER: mt = gridToCRS; break;
            case CELL_CORNER: mt = cornerToCRS; break;
            default: mt = PixelTranslation.translate(gridToCRS, PixelInCell.CELL_CENTER, anchor); break;
        }
        if (mt != null) {
            return mt;
        }
        throw incomplete(GRID_TO_CRS, Resources.Keys.UnspecifiedTransform);
    }

    /**
     * Returns a linear approximation of the conversion from grid coordinates to "real world" coordinates.
     * If the value returned by {@link #getGridToCRS(PixelInCell)} is already an instance of {@link LinearTransform},
     * then it is returned as is. Otherwise this method computes the tangent of the transform at the grid extent
     * {@linkplain GridExtent#getPointOfInterest(PixelInCell) point of interest} (usually the center of the grid).
     *
     * @param  anchor  the cell part to map (center or corner).
     * @return linear approximation of the conversion from grid coordinates to "real world" coordinates.
     * @throws IllegalArgumentException if the given {@code anchor} is not a known code list value.
     * @throws IncompleteGridGeometryException if this grid geometry has no transform,
     *         or if the transform is non-linear but this grid geometry has no extent.
     * @throws TransformException if an error occurred while computing the tangent.
     *
     * @since 1.3
     */
    public LinearTransform getLinearGridToCRS(final PixelInCell anchor) throws TransformException {
        final MathTransform tr = getGridToCRS(anchor);
        if (tr instanceof LinearTransform) {
            return (LinearTransform) tr;
        }
        return MathTransforms.linear(MathTransforms.getMatrix(tr,
                new DirectPositionView.Double(getExtent().getPointOfInterest(anchor))));
    }

    /*
     * Do not provide a convenience `getGridToCRS()` method without PixelInCell or PixelOrientation argument.
     * Experience shows that 0.5 pixel offset in image localization is a recurrent problem. We really want to
     * force developers to think about whether they want a gridToCRS transform locating pixel corner or center.
     */

    /**
     * Returns the coordinate reference system of the given envelope if defined, or {@code null} if none.
     * Contrarily to {@link #getCoordinateReferenceSystem()}, this method does not throw exception.
     */
    private static CoordinateReferenceSystem getCoordinateReferenceSystem(final Envelope envelope) {
        return (envelope != null) ? envelope.getCoordinateReferenceSystem() : null;
    }

    /**
     * Returns the "real world" coordinate reference system.
     *
     * @return the coordinate reference system (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no CRS —
     *         i.e. <code>{@linkplain #isDefined isDefined}({@linkplain #CRS})</code> returned {@code false}.
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem(envelope);
        if (crs != null) return crs;
        throw incomplete(CRS, Resources.Keys.UnspecifiedCRS);
    }

    /**
     * Returns the bounding box of "real world" coordinates for this grid geometry.
     * This envelope is computed from the {@linkplain #getExtent() grid extent}, which is
     * {@linkplain #getGridToCRS(PixelInCell) transformed} to the "real world" coordinate system.
     * The initial envelope encompasses all cell surfaces, from the left border of leftmost cell
     * to the right border of the rightmost cell and similarly along other axes.
     * If this grid geometry is a {@linkplain GridDerivation#subgrid(Envelope, double...) subgrid}, then the envelope is also
     * {@linkplain GeneralEnvelope#intersect(Envelope) clipped} to the envelope of the original (non subsampled) grid geometry.
     *
     * @return the bounding box in "real world" coordinates (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no envelope —
     *         i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #ENVELOPE})</code> returned {@code false}.
     */
    public Envelope getEnvelope() {
        if (!isEnvelopeUndefined()) {
            return envelope;
        }
        throw incomplete(ENVELOPE, (extent == null) ? Resources.Keys.UnspecifiedGridExtent : Resources.Keys.UnspecifiedTransform);
    }

    /**
     * Returns {@code true} if the envelope is null or undefined (all coordinates set to NaN).
     */
    private boolean isEnvelopeUndefined() {
        return (envelope == null) || envelope.isAllNaN();
    }

    /**
     * Returns the "real world" bounding box of this grid geometry transformed to the given CRS.
     * This envelope is computed from the {@linkplain #getExtent() grid extent} if available,
     * or from the {@linkplain #getEnvelope() envelope} otherwise.
     *
     * @param  crs  the desired coordinate reference system for the returned envelope.
     * @return the bounding box in "real world" coordinates (never {@code null}).
     * @throws IncompleteGridGeometryException if this grid geometry has no extent and no envelope.
     * @throws TransformException if the envelope cannot be transformed to the specified CRS.
     *
     * @since 1.2
     */
    public Envelope getEnvelope(final CoordinateReferenceSystem crs) throws TransformException {
        ArgumentChecks.ensureNonNull("crs", crs);
        final int   bitmask;        // CRS, EXTENT or GRID_TO_CRS
        final short errorKey;       // Resource key for error message.
        final CoordinateReferenceSystem sourceCRS = getCoordinateReferenceSystem(envelope);
        if (org.apache.sis.referencing.CRS.equivalent(sourceCRS, crs)) {
            return envelope;
        } else if (sourceCRS == null) {
            bitmask  = CRS;
            errorKey = Resources.Keys.UnspecifiedCRS;
        } else if (extent == null && envelope == null) {
            bitmask  = EXTENT;
            errorKey = Resources.Keys.UnspecifiedGridExtent;
        } else if (cornerToCRS == null && envelope == null) {
            bitmask  = GRID_TO_CRS;
            errorKey = Resources.Keys.UnspecifiedTransform;
        } else try {
            /*
             * At this point the envelope should never be null because of invariants enforced by constructors.
             * But we nevertheless perform some paranoiac checks. If we fail to transform the envelope, its okay.
             * The main transform is the one operating on grid extent. The envelope transformation is for taking
             * in account singularity points (mostly poles) and in case this grid geometry is a sub-grid geometry,
             * in which case the envelope may have been clipped and we want to keep that clip.
             */
            final boolean onlyEnvelope = (extent == null || cornerToCRS == null);
            final CoordinateOperation op = findOperation(sourceCRS, crs, geographicBBox());
            Envelope clip;
            try {
                clip = Envelopes.transform(op, envelope);
                if (onlyEnvelope) return clip;
            } catch (TransformException e) {
                if (onlyEnvelope) throw e;
                recoverableException("getEnvelope", e);
                clip = null;
            }
            MathTransform tr = MathTransforms.concatenate(cornerToCRS, op.getMathTransform());
            final GeneralEnvelope env = extent.toEnvelope(tr, false, tr, clip);
            env.setCoordinateReferenceSystem(op.getTargetCRS());
            env.normalize();
            if (clip != null) {
                env.intersect(clip);
            }
            return env;
        } catch (FactoryException e) {
            throw new TransformException(null, e);
        }
        throw incomplete(bitmask, errorKey);
    }

    /**
     * Returns the approximate latitude and longitude coordinates of the grid.
     * The prime meridian is Greenwich, but the geodetic reference frame is not necessarily WGS 84.
     * This is computed from the {@linkplain #getEnvelope() envelope} if the coordinate reference system
     * contains an horizontal component such as a geographic or projected CRS.
     *
     * <h4>API note</h4>
     * This method does not throw {@link IncompleteGridGeometryException} because the geographic extent
     * may be absent even with a complete grid geometry. Grid geometries are not required to have a
     * spatial component on Earth surface; a raster could be a vertical profile for example.
     *
     * @return the geographic bounding box in "real world" coordinates.
     */
    public Optional<GeographicBoundingBox> getGeographicExtent() {
        return Optional.ofNullable(geographicBBox());
    }

    /**
     * Returns the {@link #geographicBBox} value or {@code null} if none.
     * This method computes the box when first needed.
     */
    final GeographicBoundingBox geographicBBox() {
        GeographicBoundingBox bbox = geographicBBox;
        if (bbox == null) {
            if (getCoordinateReferenceSystem(envelope) != null && !envelope.isAllNaN()) {
                DefaultGeographicBoundingBox db;
                NilReason reason = null;
                try {
                    db = ReferencingServices.getInstance().setBounds(envelope, null, null);
                } catch (TransformException e) {
                    db = null;
                    reason = NilReason.INAPPLICABLE;
                }
                if (db != null) {
                    db.transitionTo(DefaultGeographicBoundingBox.State.FINAL);
                    bbox = db;
                } else {
                    if (reason == null) reason = NilReason.MISSING;
                    bbox = reason.createNilObject(GeographicBoundingBox.class);
                }
                geographicBBox = bbox;
            }
        }
        return (bbox instanceof NilObject) ? null : bbox;
    }

    /**
     * Returns the start time and end time of coordinates of the grid.
     * If the grid has no temporal dimension, then this method returns an empty array.
     * If only the start time or end time is defined, then returns an array of length 1.
     * Otherwise this method returns an array of length 2 with the start time in the first element
     * and the end time in the last element.
     *
     * @return time range as an array of length 0 (if none), 1 or 2.
     */
    public Instant[] getTemporalExtent() {
        Instant[] times = timeRange();
        if (times.length != 0) {
            times = times.clone();
        }
        return times;
    }

    /**
     * Returns the {@link #timeRange} value without cloning.
     * This method computes the time range when first needed.
     */
    private Instant[] timeRange() {
        Instant[] times = timeRange;
        if (times == null) {
            final TemporalAccessor t = TemporalAccessor.of(getCoordinateReferenceSystem(envelope), 0);
            times = (t != null) ? t.getTimeBounds(envelope) : TemporalAccessor.EMPTY;
            timeRange = times;
        }
        return times;
    }

    /**
     * Returns the "real world" coordinates of the cell at indices (0, 0, … 0).
     * The returned coordinates map the {@linkplain PixelInCell#CELL_CORNER cell corner}.
     * This method computes the origin from the "grid to CRS" transform if available.
     * If that transform is not available, then the minimal coordinate values of the
     * {@linkplain #getEnvelope() envelope} are returned.
     *
     * @return the "real world" coordinates of the cell at indices (0, 0, … 0).
     * @throws IncompleteGridGeometryException if this grid geometry has no origin —
     *         i.e. <code>{@linkplain #isDefined(int) isDefined}({@linkplain #ORIGIN})</code> returned {@code false}.
     *
     * @since 1.5
     */
    public double[] getOrigin() {
        if (cornerToCRS == null) {
            if (isEnvelopeUndefined()) {
                throw incomplete(ORIGIN, Resources.Keys.UnspecifiedTransform);
            }
            return getEnvelope().getLowerCorner().getCoordinate();
        }
        final double[] origin = new double[cornerToCRS.getTargetDimensions()];
        final Matrix matrix = MathTransforms.getMatrix(cornerToCRS);
        if (matrix != null) {
            final int lastColumn = matrix.getNumCol() - 1;
            for (int j=0; j < origin.length; j++) {
                if (Double.isNaN(origin[j] = matrix.getElement(j, lastColumn))) {
                    final Matrix other = MathTransforms.getMatrix(gridToCRS);
                    if (other != null) {
                        origin[j] = other.getElement(j, lastColumn);
                    }
                }
            }
        } else try {
            cornerToCRS.transform(new double[cornerToCRS.getSourceDimensions()], 0, origin, 0, 1);
        } catch (TransformException e) {
            throw new IllegalGridGeometryException(e, "gridToCRS");
        }
        return origin;
    }

    /**
     * Returns an <em>estimation</em> of the grid resolution, in units of the coordinate reference system axes.
     * The length of the returned array is the number of CRS dimensions, with {@code resolution[0]}
     * being the resolution along the first CRS axis, {@code resolution[1]} the resolution along the second CRS
     * axis, <i>etc</i>. Note that this axis order is not necessarily the same as grid axis order.
     *
     * <p>If the resolution at CRS dimension <var>i</var> is not a constant factor
     * (i.e. the <code>{@linkplain #isConversionLinear(int...) isConversionLinear}(i)</code> returns {@code false}),
     * then {@code resolution[i]} is set to one of the following values:</p>
     *
     * <ul>
     *   <li>{@link Double#NaN} if {@code allowEstimates} is {@code false}.</li>
     *   <li>An arbitrary representative resolution otherwise.
     *       Current implementation computes the resolution at {@link GridExtent#getPointOfInterest(PixelInCell) grid center},
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
     * Computes the resolution for the given grid extent and transform, or returns {@code null} if unknown.
     * Resolutions are given in order of target axes and give a scale factor from source to target coordinates.
     * If the {@code gridToCRS} transform is linear, we do not even need to check the grid extent; it can be null.
     * Otherwise (if the transform is non-linear) the extent is necessary. The easiest way to estimate a resolution
     * is then to ask for the derivative at some arbitrary point (the point of interest).
     *
     * <p>Note that for this computation, it does not matter if {@code gridToCRS} is the user-specified
     * transform or the {@code this.gridToCRS} field value; both should produce equivalent results.</p>
     *
     * @param  gridToCRS  a transform for which to compute the resolution, or {@code null} if none.
     * @param  domain     the domain for which to get a resolution, or {@code null} if none.
     *                    If non-null, must be the source of {@code gridToCRS}.
     * @param  anchor     the pixel corner versus pixel center convention to use.
     * @return the resolutions as positive numbers. May contain NaN values.
     */
    static double[] resolution(final MathTransform gridToCRS, final GridExtent domain, final PixelInCell anchor) {
        final Matrix matrix = MathTransforms.getMatrix(gridToCRS);
        if (matrix != null) {
            return resolution(matrix, 1);
        } else if (domain != null && gridToCRS != null) try {
            return resolution(gridToCRS.derivative(new DirectPositionView.Double(domain.getPointOfInterest(anchor))), 0);
        } catch (TransformException e) {
            recoverableException("resolution", e);
        }
        return null;
    }

    /**
     * Computes the resolutions from the given matrix. This is the magnitude of each row vector.
     * Resolutions are given in order of target axes.
     *
     * @param  gridToCRS    Jacobian matrix or affine transform for which to compute the resolution.
     * @param  numToIgnore  number of rows and columns to ignore at the end of the matrix.
     *         This is 0 if the matrix is a derivative (i.e. we ignore nothing), or 1 if the matrix
     *         is an affine transform (i.e. we ignore the translation column and the [0 0 … 1] row).
     * @return the resolutions as positive numbers. May contain NaN values.
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
     * Indicates whether the <i>grid to CRS</i> conversion is linear for all the specified CRS axes.
     * The conversion from grid coordinates to real world coordinates is often linear for some dimensions,
     * typically the horizontal ones at indices 0 and 1. But the vertical dimension (usually at index 2)
     * is often non-linear, for example with data at 0, 5, 10, 100 and 1000 metres.
     *
     * @param  targets  indices of CRS axes. This is not necessarily the same as indices of grid axes.
     * @return {@code true} if the conversion from grid coordinates to "real world" coordinates is linear
     *         for all the given CRS dimension.
     */
    public boolean isConversionLinear(final int... targets) {
        final int dimension = getTargetDimension();
        long mask = 0;
        for (final int d : targets) {
            mask |= Numerics.bitmask(Objects.checkIndex(d, dimension));
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
            final Matrix matrix = MathTransforms.getMatrix(step);
            if (matrix != null) {
                /*
                 * For linear transforms there are no bits to set. However if some bits were set by a previous
                 * iteration, we may need to move them (for example the transform may swap axes). We take the
                 * current bitmasks as source dimensions and find what are the target dimensions for them.
                 */
                long mask = nonLinearDimensions;
                nonLinearDimensions = 0;
                while (mask != 0) {
                    final int i = Long.numberOfTrailingZeros(mask);         // Source dimension of non-linear part
                    for (int j = matrix.getNumRow() - 1; --j >= 0;) {       // Possible target dimensions
                        if (matrix.getElement(j, i) != 0) {
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
                 * their order changed). However, we have a difficulty if the number of dimensions changes.
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
                return Numerics.bitmask(dimension) - 1;
            }
        }
        return nonLinearDimensions;
    }

    /**
     * Invoked when the number of non-linear dimensions exceeds the {@code GridGeometry} capacity.
     */
    static ArithmeticException excessiveDimension(final MathTransform gridToCRS) {
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
     * Verifies that this grid geometry defines an {@linkplain #extent} and a {@link #cornerToCRS} transform.
     * They are the information required for mapping the grid to a spatiotemporal envelope or position.
     * Note that this implies that {@link #envelope} is non-null (but not necessarily that its CRS is non-null).
     *
     * @param  center  {@code true} for "center to CRS" transform, {@code false} for "corner to CRS" transform.
     * @return {@link #gridToCRS} or {@link #cornerToCRS}.
     */
    final MathTransform requireGridToCRS(final boolean center) throws IncompleteGridGeometryException {
        if (extent == null) {
            throw incomplete(EXTENT, Resources.Keys.UnspecifiedGridExtent);
        }
        final MathTransform mt = center ? gridToCRS : cornerToCRS;
        if (mt == null) {
            throw incomplete(GRID_TO_CRS, Resources.Keys.UnspecifiedTransform);
        }
        return mt;
    }

    /**
     * Returns {@code true} if all the properties specified by the argument are set.
     * If this method returns {@code true}, then invoking the corresponding getter
     * methods will not throw {@link IncompleteGridGeometryException}.
     *
     * @param  bitmask  any combination of {@link #CRS}, {@link #ENVELOPE}, {@link #EXTENT},
     *         {@link #GRID_TO_CRS} and derived bit masks.
     * @return {@code true} if all specified properties are defined (i.e. invoking the
     *         corresponding getter methods will not throw {@link IncompleteGridGeometryException}).
     * @throws IllegalArgumentException if the specified bitmask is not a combination of known masks.
     *
     * @see #getCoordinateReferenceSystem()
     * @see #getEnvelope()
     * @see #getExtent()
     * @see #getResolution(boolean)
     * @see #getGridToCRS(PixelInCell)
     */
    public boolean isDefined(final int bitmask) {
        if ((bitmask & ~(CRS | ENVELOPE | EXTENT | GRID_TO_CRS | ORIGIN | RESOLUTION | GEOGRAPHIC_EXTENT | TEMPORAL_EXTENT)) != 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "bitmask", bitmask));
        }
        return ((bitmask & CRS)               == 0 || (null != getCoordinateReferenceSystem(envelope)))
            && ((bitmask & ENVELOPE)          == 0 || !isEnvelopeUndefined())
            && ((bitmask & EXTENT)            == 0 || (null != extent))
            && ((bitmask & GRID_TO_CRS)       == 0 || (null != gridToCRS))
            && ((bitmask & ORIGIN)            == 0 || (null != gridToCRS || !isEnvelopeUndefined()))
            && ((bitmask & RESOLUTION)        == 0 || (null != resolution))
            && ((bitmask & GEOGRAPHIC_EXTENT) == 0 || (null != geographicBBox()))
            && ((bitmask & TEMPORAL_EXTENT)   == 0 || (timeRange().length != 0));
    }

    /**
     * Returns {@code true} if this grid geometry contains only a grid extent and no other information.
     * Note: if {@link #gridToCRS} is {@code null}, then {@link #cornerToCRS} and {@link #resolution}
     * should be null as well.
     */
    final boolean isExtentOnly() {
        return gridToCRS == null && envelope == null && extent != null;
    }

    /**
     * Returns {@code true} if this grid geometry contains only an envelope and no other information.
     * Note: if {@link #gridToCRS} is {@code null}, then {@link #cornerToCRS} and {@link #resolution}
     * should be null as well.
     */
    final boolean isEnvelopeOnly() {
        return gridToCRS == null && extent == null && envelope != null;
    }

    /**
     * Returns an object that can be used for creating a new grid geometry derived from this grid geometry.
     * {@code GridDerivation} does not change the state of this {@code GridGeometry} but instead creates
     * new instances as needed. Examples of modifications include clipping to a sub-area or applying a sub-sampling.
     *
     * <p>Each {@code GridDerivation} instance can be used only once and should be used in a single thread.
     * {@code GridDerivation} preserves the number of dimensions. For example, {@linkplain GridDerivation#slice slicing}
     * sets the {@linkplain GridExtent#getSize(int) grid size} to 1 in all dimensions specified by a <i>slice point</i>,
     * but does not remove those dimensions from the grid geometry. For dimensionality reduction, see {@link #selectDimensions(int[])}.</p>
     *
     * <h4>Example</h4>
     * For clipping this grid geometry to a sub-area, one can use:
     *
     * {@snippet lang="java" :
     *     GridGeometry gg = ...;
     *     Envelope areaOfInterest = ...;
     *     gg = gg.derive().rounding(GridRoundingMode.ENCLOSING)
     *                     .subgrid(areaOfInterest).build();
     *     }
     *
     * @return an object for deriving a grid geometry from {@code this}.
     */
    public GridDerivation derive() {
        return new GridDerivation(this);
    }

    /**
     * Creates a new grid geometry upsampled by the given number of cells along each grid dimensions.
     * This method multiplies {@linkplain GridExtent#getLow(int) low} and {@linkplain GridExtent#getHigh(int) high}
     * coordinates by the given periods, then scales the {@link #getGridToCRS(PixelInCell) grid to CRS} transform
     * for compensating the grid change.
     * The grid geometry {@link #getEnvelope() envelope} is preserved after upsampling.
     *
     * <h4>Number of arguments</h4>
     * The {@code periods} array length should be equal to the {@linkplain #getDimension() number of grid dimensions}.
     * If the array is shorter, missing values default to 1 (i.e. samplings in unspecified dimensions are unchanged).
     * If the array is longer, extraneous values are ignored.
     *
     * @param  periods  the upsampling factors for each dimension of this grid geometry.
     * @return the upsampled grid geometry, or {@code this} is upsampling results in the same extent.
     * @throws IllegalArgumentException if a period is not greater than zero.
     *
     * @see GridExtent#upsample(long...)
     * @since 1.5
     */
    public GridGeometry upsample(final long... periods) {
        GridExtent newExtent = extent;
        if (newExtent != null) {
            newExtent = newExtent.upsample(periods);
            if (newExtent == extent) return this;       // Unchanged.
        }
        boolean changed = false;                        // Additional check in case `extent` was null.
        MathTransform newGridToCRS = null;
        double[] newResolution = null;
        if (cornerToCRS != null) {
            final int tgtDim = cornerToCRS.getTargetDimensions();
            final int srcDim = cornerToCRS.getSourceDimensions();
            MatrixSIS matrix = Matrices.copy(MathTransforms.getMatrix(cornerToCRS));
            final boolean isNonLinear = (matrix == null);
            if (isNonLinear) {
                matrix = Matrices.create(tgtDim+1, srcDim+1, ExtendedPrecisionMatrix.CREATE_IDENTITY);
            }
            /*
             * By dividing the matrix elements directly, we avoid some numerical errors.
             * This is because 100*0.1 is not exactly equal to 100/10.
             * Each period value must be multiplied by a full column.
             */
            newResolution = resolution.clone();
            for (int i = Math.min(srcDim, periods.length); --i >= 0;) {
                final double p = periods[i];
                if (p != 1) {
                    newResolution[i] /= p;
                    final DoubleDouble pd = DoubleDouble.of(p, true);
                    for (int j=0; j<tgtDim; j++) {
                        DoubleDouble e = DoubleDouble.of(matrix.getNumber(j, i), true);
                        matrix.setNumber(j, i, e.divide(pd));
                        changed = true;
                    }
                }
            }
            newGridToCRS = MathTransforms.linear(matrix);
            if (isNonLinear) {
                newGridToCRS = MathTransforms.concatenate(newGridToCRS, cornerToCRS);
            }
        }
        if (!changed) return this;
        return new GridGeometry(newExtent, cornerToCenter(newGridToCRS), newGridToCRS, envelope, newResolution, nonLinears);
    }

    /**
     * Translates grid coordinates by the given number of cells without changing "real world" coordinates.
     * The returned grid has the same {@linkplain GridExtent#getSize(int) size} than this grid,
     * i.e. both low and high grid coordinates are displaced by the same number of cells.
     * The "grid to CRS" transforms are adjusted accordingly in order to map to the same
     * "real world" coordinates.
     *
     * <h4>Number of arguments</h4>
     * The {@code translation} array length should be equal to the {@linkplain #getDimension() number of dimensions}.
     * If the array is shorter, missing values default to 0 (i.e. no translation in unspecified dimensions).
     * If the array is longer, extraneous values are ignored.
     *
     * @param  translation  translation to apply on each grid axis in order.
     * @return a grid geometry whose coordinates (both low and high ones) and
     *         the "grid to CRS" transforms have been translated by given numbers.
     *         If the given translation is a no-op (no value or only 0 ones), then this grid is returned as is.
     * @throws ArithmeticException if the translation results in coordinates that overflow 64-bits integer.
     *
     * @see GridExtent#translate(long...)
     *
     * @since 1.3
     */
    public final GridGeometry shiftGrid(final long... translation) {
        return shiftGrid(translation, false);
    }

    /**
     * Translates grid coordinates by the given number of cells, optionally in the reverse direction.
     * Invoking this method is equivalent to invoking {@link #shiftGrid(long...)}, except that this method
     * use the negative values of the given translation terms if the {@code negate} argument is {@code true}.
     *
     * @param  translation  translation to apply on each grid axis in order. Can be an array of any length.
     * @param  negate       whether to use the negative values of the given translation terms.
     * @return a grid geometry whose coordinates (both low and high ones) and
     *         the "grid to CRS" transforms have been translated by given numbers.
     *         If the given translation is a no-op (no value or only 0 ones), then this grid is returned as is.
     * @throws ArithmeticException if the translation results in coordinates that overflow 64-bits integer.
     *
     * @since 1.5
     */
    public GridGeometry shiftGrid(final long[] translation, final boolean negate) {
        ArgumentChecks.ensureNonNull("translation", translation);
        GridExtent newExtent = extent;
        if (newExtent != null) {
            newExtent = newExtent.translate(translation, negate);
            if (newExtent == extent) return this;
        }
        MathTransform t1 = gridToCRS;
        MathTransform t2 = cornerToCRS;
        if (t1 != null || t2 != null) {
            boolean isZero = true;
            final double[] vector = new double[getDimension()];
            for (int i=Math.min(vector.length, translation.length); --i >= 0;) {
                isZero &= (translation[i] == 0);
                double v = translation[i];
                vector[i] = negate ? v : -v;    // Really negate if `negate` is false.
            }
            if (isZero) return this;
            final MathTransform t = MathTransforms.translation(vector);
            t1 = MathTransforms.concatenate(t, t1);
            t2 = MathTransforms.concatenate(t, t2);
        }
        return new GridGeometry(newExtent, t1, t2, envelope, resolution, nonLinears);
    }

    /**
     * Translates the grid to lower coordinate values of zero without changing the "real world" coordinates.
     * The returned grid has the same {@linkplain GridExtent#getSize(int) size} than this grid,
     * i.e. both low and high grid coordinates are displaced by the same number of cells.
     * The "grid to CRS" transforms are adjusted accordingly in order to map to the same
     * "real world" coordinates.
     *
     * @return a grid geometry whose lower coordinates are zeros.
     * @throws ArithmeticException if the translation results in upper coordinates that overflow 64-bits integer.
     *
     * @since 1.5
     */
    public GridGeometry shiftGridToZeros() {
        if (extent == null || extent.startsAtZero()) {
            return this;
        }
        return shiftGrid(extent.getLow().getCoordinateValues(), true);
    }

    /**
     * Returns a grid geometry with the given grid extent, which implies a new "real world" computation.
     * The "grid to CRS" transforms and the resolution stay the same as this {@code GridGeometry}.
     * The "real world" envelope is recomputed for the new grid extent using the "grid to CRS" transforms.
     *
     * <p>The given extent is taken verbatim; this method does no clipping.
     * The given extent does not need to intersect the extent of this grid geometry.</p>
     *
     * @param  newExtent  extent of the grid geometry to return.
     * @return grid geometry with the given extent. May be {@code this} if there is no change.
     * @throws TransformException if the geospatial envelope cannot be recomputed with the new grid extent.
     *
     * @since 1.3
     */
    public GridGeometry relocate(final GridExtent newExtent) throws TransformException {
        if (newExtent.equals(extent)) {
            return this;
        }
        ensureDimensionMatches(getDimension(), newExtent);
        final ImmutableEnvelope relocated;
        if (cornerToCRS != null) {
            final GeneralEnvelope env = newExtent.toEnvelope(cornerToCRS, false, gridToCRS, null);
            env.setCoordinateReferenceSystem(getCoordinateReferenceSystem(envelope));
            relocated = new ImmutableEnvelope(env);
        } else {
            relocated = envelope;           // Either null or contains only the CRS.
        }
        return new GridGeometry(newExtent, gridToCRS, cornerToCRS, relocated, resolution, nonLinears);
    }

    /**
     * Returns a grid geometry that encompass only some dimensions of this grid geometry.
     * The specified dimensions will be copied into a new grid geometry if necessary.
     * The selection is applied on {@linkplain #getExtent() grid extent} dimensions;
     * they are not necessarily the same as the {@linkplain #getEnvelope() envelope} dimensions.
     * The given dimensions must be in strictly ascending order without duplicated values.
     * The number of dimensions of the sub grid geometry will be {@code dimensions.length}.
     *
     * <p>This method performs a <i>dimensionality reduction</i>.
     * This method cannot be used for changing dimension order.
     * The converse operation is the {@linkplain #GridGeometry(GridGeometry, GridGeometry) concatenation}.</p>
     *
     * @param  indices  the grid (not CRS) dimensions to select, in strictly increasing order.
     * @return the sub-grid geometry, or {@code this} if the given array contains all dimensions of this grid geometry.
     * @throws IndexOutOfBoundsException if an index is out of bounds.
     *
     * @see GridExtent#getSubspaceDimensions(int)
     * @see GridExtent#selectDimensions(int[])
     * @see org.apache.sis.referencing.CRS#selectDimensions(CoordinateReferenceSystem, int[])
     *
     * @since 1.3
     */
    public GridGeometry selectDimensions(int... indices) {
        indices = GridExtent.verifyDimensions(indices, getDimension());
        if (indices != null) try {
            return new SliceGeometry(this, null, indices, null).reduce(null, -1);
        } catch (FactoryException e) {
            throw new BackingStoreException(e);
        }
        return this;
    }

    /**
     * Creates a one-, two- or three-dimensional coordinate reference system for cell indices in the grid.
     * This method returns a CRS which is derived from the "real world" CRS or a subset of it.
     * If the "real world" CRS is an instance of {@link org.opengis.referencing.crs.SingleCRS},
     * then the derived CRS has the following properties:
     *
     * <ul>
     *   <li>{@link DerivedCRS#getBaseCRS()} is {@link #getCoordinateReferenceSystem()}.</li>
     *   <li>{@link DerivedCRS#getConversionFromBase()} is the inverse of {@link #getGridToCRS(PixelInCell)}.</li>
     * </ul>
     *
     * Otherwise if the "real world" CRS is an instance of {@link org.opengis.referencing.crs.CompoundCRS},
     * then only the first {@link org.opengis.referencing.crs.SingleCRS} (the head) is used.
     * This is usually (but not necessarily) the horizontal component of the spatial CRS.
     * The result is usually two-dimensional, but 1 and 3 dimensions are also possible.
     *
     * <p>Because of above relationship, it is possible to use the derived CRS in a chain of operations
     * with (for example) {@link org.apache.sis.referencing.CRS#findOperation CRS.findOperation(…)}.</p>
     *
     * @param  name    name of the CRS to create.
     * @param  anchor  the cell part to map (center or corner).
     * @return a derived CRS for coordinates (cell indices) associated to the grid extent.
     * @throws IncompleteGridGeometryException if the CRS, grid extent or "grid to CRS" transform is missing.
     *
     * @since 1.3
     */
    public DerivedCRS createImageCRS(final String name, final PixelInCell anchor) {
        ArgumentChecks.ensureNonEmpty("name", name);
        try {
            return GridExtentCRS.forCoverage(name, this, anchor, null);
        } catch (FactoryException e) {
            throw new BackingStoreException(e);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a transform from cell coordinates in this grid to cell coordinates in the given grid.
     * The returned transform handles change of Coordinate Reference System and wraparound axes
     * (e.g. longitude axis crossing the ±180° meridian) if applicable.
     *
     * <p><b>Note:</b> the transform created by this method may be non-invertible.</p>
     *
     * @param  target  the grid which will be the target of returned transform.
     * @param  anchor  {@code CELL_CENTER} for OGC conventions or
     *                 {@code CELL_CORNER} for Java2D/JAI conventions.
     * @return transform from cell coordinates in this grid to cell coordinates in the given grid.
     * @throws IncompleteGridGeometryException if there is not enough information in the two grid geometries.
     * @throws TransformException if the math transform cannot be created.
     *
     * @since 1.1
     */
    public MathTransform createTransformTo(final GridGeometry target, final PixelInCell anchor) throws TransformException {
        ArgumentChecks.ensureNonNull("target", target);
        ArgumentChecks.ensureNonNull("anchor", anchor);
        /*
         * Inverse `source` and `target` because `CoordinateOperationFinder.inverse(…)` does an
         * effort for using `WraparoundTransform` only if needed (contrarily to `gridToCRS(…)`).
         */
        final var finder = new CoordinateOperationFinder(target, this);
        finder.verifyPresenceOfCRS(false);
        finder.setAnchor(anchor);
        final MathTransform tr;
        try {
            tr = finder.inverse();
        } catch (FactoryException e) {
            throw new TransformException(e.getMessage(), e);
        }
        return MathTransforms.concatenate(getGridToCRS(anchor), tr);
    }

    /**
     * Returns the coordinate range of another grid geometry in units of cells of this grid geometry.
     * This method gets the {@linkplain #createTransformTo transform between cell coordinates} and
     * uses it for converting the extent of the {@code other} grid geometry. The grid coordinates
     * are rounded to integers in a way specified by the {@link GridRoundingMode} argument.
     * The result does not need to be contained or to intersect the extent of this grid geometry.
     *
     * <p>The {@code include} argument specifies whether to transform cell corners or cell centers:</p>
     * <ul>
     *   <li>With {@code CELL_CENTER}, the returned extent contains the cell centers of {@code other}.</li>
     *   <li>With {@code CELL_CORNER}, the returned extent contains the cell corners of {@code other}.
     *       It includes both lower and upper corners, thus covering the full cell areas.</li>
     * </ul>
     * <p>An extent of cell centers may be smaller than an extent of cell areas by a size,
     * on each border, of ½ of the size of the cells of {@code other}.</p>
     *
     * <p>Note that if the transformation involves, for example, a rotation or a map projection,
     * then the returned extent may encompass more cells than the real extent of {@code other}.
     * See {@linkplain Envelopes#transform(Envelope, CoordinateReferenceSystem) envelope transformations}
     * for more information.</p>
     *
     * @param  other     the other grid geometry from which to get the extent in units of this grid geometry.
     * @param  include   {@code CELL_CENTER} for an extent containing cell centers, or
     *                   {@code CELL_CORNER} for an extent containing all cell corners (i.e. full areas).
     * @param  rounding  whether to round grid coordinates to nearest, enclosing or contained bounds.
     * @return the extent of the other grid geometry in cell units of this grid geometry.
     * @throws IncompleteGridGeometryException if there is not enough information in the two grid geometries.
     * @throws TransformException if the grid extent cannot be transformed to the units of this grid geometry.
     *
     * @since 1.5
     */
    public GridExtent extentOf(final GridGeometry other, final PixelInCell include, final GridRoundingMode rounding)
            throws TransformException
    {
        GridExtent result = other.getExtent();
        if (cornerToCRS != other.cornerToCRS || gridToCRS != other.gridToCRS) {   // Quick check for a common case.
            final MathTransform tr = other.createTransformTo(this, include);
            if (!tr.isIdentity()) {
                final boolean isCenter = (include == PixelInCell.CELL_CENTER);
                final GeneralEnvelope bounds = result.toEnvelope(tr, isCenter, tr, null);
                result = new GridExtent(bounds, isCenter, rounding, GridClippingMode.NONE, null, null, extent, null);
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if this grid geometry intersects the given grid geometry.
     * This method compares the coordinates of the two {@linkplain #extentOf grid extents} when possible,
     * or between the two {@linkplain #getEnvelope(CoordinateReferenceSystem) envelopes} otherwise (fallback).
     * This method may conservatively return {@code true} if an accurate answer would be expensive to compute.
     * Therefore, a return value of {@code true} is not a guarantee that the two grids really intersect.
     * However, a return value of {@code false} guarantees that the two grids do <em>not</em> intersect.
     *
     * <p>The comparison is performed in units of this grid geometry by converting the other grid if necessary.
     * Consequently, if the <abbr>CRS</abbr> and "grid to CRS" transform are not the same in the two grids,
     * then this method may not be symmetric. In other words, it is <em>not</em> guaranteed that
     * {@code A.intersects(B) = B.intersects(A)} for all (<var>A</var>, <var>B</var>) pairs.</p>
     *
     * @param  other  the other grid geometry to test for intersection.
     * @return whether the other grid geometry intersects or may intersect this grid geometry.
     * @throws IncompleteGridGeometryException if there is not enough information in the two grid geometries.
     *
     * @see GridExtent#intersects(GridExtent)
     *
     * @since 1.5
     */
    public boolean intersects(final GridGeometry other) {
        return interacts(other, true);
    }

    /**
     * Returns {@code true} if this grid geometry contains the given grid geometry within a tolerance of ½ cell.
     * This method compares the coordinates of the two {@linkplain #extentOf grid extents} when possible,
     * or between the two {@linkplain #getEnvelope(CoordinateReferenceSystem) envelopes} otherwise (fallback).
     * The comparison is exact if the two grids use the same <abbr>CRS</abbr> and "grid to <abbr>CRS</abbr>".
     * Otherwise, a coordinate transformations may be applied with the following approximations:
     *
     * <ul>
     *   <li>The comparison is between extents containing {@linkplain PixelInCell#CELL_CENTER cell centers}.
     *       Because the {@code other} extent may be smaller compared to an extent computed over cell areas,
     *       this approach introduces a tolerance of ½ cell in units of the {@code other} grid.</li>
     *   <li>Cell coordinates are rounded to nearest integers.
     *       This approach adds another tolerance of ½ cell in units of {@code this} grid.</li>
     *   <li>If the coordinate transformation between the two grids is non-linear or produces a rotation,
     *       the transformed extent of {@code other} may contain more cells that the reality.
     *       In such cases, this method may return {@code false} even if a more extensive test
     *       would have returned {@code true}.</li>
     * </ul>
     *
     * This method may conservatively return {@code false} if an accurate answer would be too expensive
     * (because of the last point in above list), or if the coordinates cannot be transformed.
     *
     * <h4>Usage</h4>
     * This method behavior is well defined when the two grids are members of the same image mosaic or pyramid:
     * same <abbr>CRS</abbr> together with "grid to <abbr>CRS</abbr>" transforms that differ only by scale and
     * translation factors with integer values.
     * For other cases, this method should be considered as merely a hint because of above approximations.
     *
     * @param  other  the other grid geometry to test for inclusion.
     * @return whether the other grid geometry is contained in this grid geometry.
     * @throws IncompleteGridGeometryException if there is not enough information in the two grid geometries.
     *
     * @see GridExtent#contains(GridExtent)
     *
     * @since 1.5
     */
    public boolean contains(final GridGeometry other) {
        return interacts(other, false);
    }

    /**
     * Implementation of {@link #contains(GridGeometry)} and {@link #intersects(GridGeometry)} methods.
     */
    private boolean interacts(final GridGeometry other, final boolean intersects) {
        try {
            if (extent != null && other.extent != null) {
                var source = getExtent();
                var target = extentOf(other,
                        intersects ? PixelInCell.CELL_CORNER : PixelInCell.CELL_CENTER,
                        intersects ? GridRoundingMode.ENCLOSING : GridRoundingMode.NEAREST);
                return intersects ? source.intersects(target) : source.contains(target);
            }
            // Fallback used only if there is no grid extent.
            var source = AbstractEnvelope.castOrCopy(getEnvelope());
            var target = other.getEnvelope(getCoordinateReferenceSystem());
            return intersects ? source.intersects(target) : source.contains(target);
        } catch (TransformException e) {
            recoverableException(intersects ? "intersects" : "contains", e);
            return intersects;
        }
    }

    /**
     * Compares the specified object with this grid geometry for equality.
     * This method delegates to {@code equals(object, ComparisonMode.STRICT)}.
     *
     * @param  object  the object to compare with.
     * @return {@code true} if the given object is equal to this grid geometry.
     */
    @Override
    public boolean equals(final Object object) {
        return equals(object, ComparisonMode.STRICT);
    }

    /**
     * Compares the specified object with this grid geometry for equality.
     * If the mode is {@link ComparisonMode#IGNORE_METADATA} or more flexible,
     * then the {@linkplain GridExtent#getAxisType(int) axis types} are ignored.
     *
     * @param  object  the object to compare with this grid geometry for equality.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if the given object is equal to this grid geometry.
     *
     * @since 1.1
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (object instanceof GridGeometry) {
            final var that = (GridGeometry) object;
            if ((mode != ComparisonMode.STRICT || getClass().equals(object.getClass()))
                    && Utilities.deepEquals(extent,    that.extent,    mode)
                    && Utilities.deepEquals(gridToCRS, that.gridToCRS, mode))
            {
                final ImmutableEnvelope othenv = that.envelope;
                if (!mode.isApproximate()) {
                    return Utilities.deepEquals(envelope, othenv, mode);
                }
                if ((envelope == null) == (othenv == null) &&
                        Utilities.deepEquals(getCoordinateReferenceSystem(envelope),
                                             getCoordinateReferenceSystem(othenv), mode))
                {
                    return equalsApproximately(othenv);
                }
            }
        }
        return false;
    }

    /**
     * Returns whether the given envelope is equal to this grid geometry envelope with a tolerance threshold
     * computed from grid resolution. If this grid geometry has no envelope, then this method arbitrarily
     * returns {@code true} (this unusual behavior is required by {@link #equals(Object, ComparisonMode)}).
     */
    final boolean equalsApproximately(final ImmutableEnvelope othenv) {
        if (envelope != null) {
            for (int i = envelope.getDimension(); --i >= 0;) {
                // Arbitrary threshold of ½ pixel.
                final double ε = (resolution != null) ? resolution[i] * 0.5 : 0;
                if (!MathFunctions.epsilonEqual(envelope.getLower(i), othenv.getLower(i), ε) ||
                    !MathFunctions.epsilonEqual(envelope.getUpper(i), othenv.getUpper(i), ε))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a hash value for this grid geometry. This value needs not to remain
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
     * Returns the default set of flags to use for {@link #toString()} implementations.
     * Current implementation returns all core properties, augmented with only derived
     * properties that are defined and that are not redundant with other properties.
     */
    final int defaultFlags() {
        int flags = EXTENT | GRID_TO_CRS | CRS;
        if (envelope != null) flags |= ENVELOPE;
        final Matrix matrix = MathTransforms.getMatrix(gridToCRS);
        if (matrix != null) {
            final int lastColumn = matrix.getNumCol() - 1;
            for (int j = matrix.getNumRow() - 1; --j >= 0;) {
                if (Double.isNaN(matrix.getElement(j, lastColumn))) {
                    final Matrix other = MathTransforms.getMatrix(cornerToCRS);
                    if (other != null && !Double.isNaN(other.getElement(j, lastColumn))) {
                        // Found an information which will not be visible in the "Conversion" section.
                        flags |= ORIGIN;
                        break;
                    }
                }
            }
        } else if (gridToCRS != null) {
            flags |= ORIGIN;
        }
        if (resolution != null && isEnvelopeUndefined()) {
            flags |= RESOLUTION;
        }
        if (geographicBBox() != null) {
            flags |= GEOGRAPHIC_EXTENT;
        }
        if (timeRange().length != 0) {
            flags |= TEMPORAL_EXTENT;
        }
        return flags;
    }

    /**
     * Returns a string representation of this grid geometry.
     * The returned string is implementation dependent and may change in any future version.
     * Current implementation is equivalent to a call to {@link #toTree(Locale, int)} with
     * at least {@link #EXTENT}, {@link #ENVELOPE} and {@link #CRS} flags.
     * Whether more flags are present or not is unspecified.
     */
    @Override
    public String toString() {
        return toTree(Locale.getDefault(), defaultFlags()).toString();
    }

    /**
     * Returns a tree representation of some elements of this grid geometry.
     * The tree representation is for debugging or logging purposes
     * and may change in any future SIS version.
     *
     * @param  locale   the locale to use for textual labels.
     * @param  bitmask  combination of {@link #EXTENT}, {@link #ENVELOPE}, {@link #CRS}, {@link #GRID_TO_CRS},
     *                  {@link #ORIGIN}, {@link #RESOLUTION}, {@link #GEOGRAPHIC_EXTENT} and {@link #TEMPORAL_EXTENT}.
     * @return a tree representation of the specified elements.
     */
    @Debug
    public TreeTable toTree(final Locale locale, final int bitmask) {
        ArgumentChecks.ensureNonNull("locale", locale);
        final var tree = new DefaultTreeTable(TableColumn.VALUE_AS_TEXT);
        final TreeTable.Node root = tree.getRoot();
        root.setValue(TableColumn.VALUE_AS_TEXT, Classes.getShortClassName(this));
        formatTo(locale, Vocabulary.forLocale(locale), bitmask, root);
        return tree;
    }

    /**
     * Formats a string representation of this grid geometry in the specified tree.
     */
    final void formatTo(final Locale locale, final Vocabulary vocabulary, final int bitmask, final TreeTable.Node root) {
        if ((bitmask & ~(EXTENT | ENVELOPE | CRS | GRID_TO_CRS | ORIGIN | RESOLUTION | GEOGRAPHIC_EXTENT | TEMPORAL_EXTENT)) != 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "bitmask", bitmask));
        }
        try {
            new Formatter(locale, vocabulary, bitmask, root).format();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Helper class for formatting a {@link GridGeometry} instance.
     */
    private final class Formatter {
        /**
         * Combination of {@link #EXTENT}, {@link #ENVELOPE}, {@link #CRS}, {@link #GRID_TO_CRS} and derived bit masks.
         */
        private final int bitmask;

        /**
         * Temporary buffer for formatting node values.
         */
        private final StringBuilder buffer;

        /**
         * Where to write the {@link GridGeometry} string representation.
         */
        private final TreeTable.Node root;

        /**
         * The section under the {@linkplain #root} where to write elements.
         * This is updated when {@link #section(int, short, boolean, boolean)} is invoked.
         */
        private TreeTable.Node section;

        /**
         * Localized words.
         */
        private final Vocabulary vocabulary;

        /**
         * The locale for the texts, numbers (except grid extent) and dates.
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
         * The format to use for formatting numbers. Created when first needed.
         */
        private NumberFormat numberFormat;

        /**
         * Creates a new formatter for the given combination of {@link #EXTENT}, {@link #ENVELOPE},
         * {@link #CRS}, {@link #GRID_TO_CRS} and derived bit masks.
         */
        Formatter(final Locale locale, final Vocabulary vocabulary, final int bitmask, final TreeTable.Node out) {
            this.root       = out;
            this.bitmask    = bitmask;
            this.buffer     = new StringBuilder(256);
            this.locale     = locale;
            this.vocabulary = vocabulary;
            this.crs        = getCoordinateReferenceSystem(envelope);
            this.cs         = (crs != null) ? crs.getCoordinateSystem() : null;
        }

        /**
         * Returns the object to use for formatting numbers.
         */
        private NumberFormat numberFormat() {
            if (numberFormat == null) {
                numberFormat = NumberFormat.getNumberInstance(locale);
            }
            return numberFormat;
        }

        /**
         * Formats a string representation of the enclosing {@link GridGeometry} instance
         * in the buffer specified at construction time.
         */
        final void format() throws IOException {
            /*
             * Example: Grid extent
             * ├─ Dimension 0: [370 … 389]  (20 cells)
             * └─ Dimension 1: [ 41 … 340] (300 cells)
             *
             * We need to use the implementation provided by GridExtent in order
             * to format correctly the unsigned long numbers for very large sizes.
             */
            if (section(EXTENT, Vocabulary.Keys.GridExtent, true, false)) {
                extent.appendTo(buffer, vocabulary);
                writeNodes();
            }
            /*
             * Example: Geographic extent
             *  ├─Lower bound:  80°00′00″S  180°00′00″W  2019-05-01T21:00:00Z
             *  └─Upper bound:  90°00′00″N  180°00′00″E  2019-05-02T00:00:00Z
             *
             * The angle and date/time patterns are fixed for now, with a precision equivalent to about 30 metres.
             * The angles are rounded toward up and down for making sure that the box encloses fully the coverage.
             */
            if (section(GEOGRAPHIC_EXTENT, Vocabulary.Keys.GeographicExtent, false, false) ||
                section(TEMPORAL_EXTENT,   Vocabulary.Keys.TemporalExtent, false, false))
            {
                final var table = new TableAppender(buffer, "  ");
                final var nf = new AngleFormat("DD°MM′SS″", locale);
                final GeographicBoundingBox bbox = ((bitmask & GEOGRAPHIC_EXTENT) != 0) ? geographicBBox() : null;
                double westBoundLongitude = Double.NaN;
                double eastBoundLongitude = Double.NaN;
                final Instant[] times = ((bitmask & TEMPORAL_EXTENT) != 0) ? timeRange() : TemporalAccessor.EMPTY;
                vocabulary.appendLabel(Vocabulary.Keys.LowerBound, table);
                table.setCellAlignment(TableAppender.ALIGN_RIGHT);
                if (bbox != null) {
                    nf.setRoundingMode(RoundingMode.FLOOR);
                    westBoundLongitude = bbox.getWestBoundLongitude();
                    table.nextColumn(); table.append(nf.format(new Latitude(bbox.getSouthBoundLatitude())));
                    table.nextColumn(); table.append(nf.format(new Longitude(westBoundLongitude)));
                }
                if (times.length >= 1) {
                    table.nextColumn();
                    table.append(times[0].toString());
                }
                table.nextLine();
                table.setCellAlignment(TableAppender.ALIGN_LEFT);
                vocabulary.appendLabel(Vocabulary.Keys.UpperBound, table);
                table.setCellAlignment(TableAppender.ALIGN_RIGHT);
                if (bbox != null) {
                    nf.setRoundingMode(RoundingMode.CEILING);
                    eastBoundLongitude = bbox.getEastBoundLongitude();
                    table.nextColumn(); table.append(nf.format(new Latitude(bbox.getNorthBoundLatitude())));
                    table.nextColumn(); table.append(nf.format(new Longitude(eastBoundLongitude)));
                }
                if (times.length >= 2) {
                    table.nextColumn();
                    table.append(times[1].toString());
                }
                table.flush();
                if (Longitude.isWraparound(westBoundLongitude, eastBoundLongitude)) {
                    vocabulary.appendLabel(Vocabulary.Keys.Note, buffer);
                    buffer.append(' ')
                          .append(Messages.forLocale(locale).getString(Messages.Keys.BoxCrossesAntiMeridian));
                }
                writeNodes();
            }
            /*
             * Example: Envelope
             * ├─ Geodetic latitude:  -69.75 … 80.25  ∆φ = 0.5°
             * └─ Geodetic longitude:   4.75 … 14.75  ∆λ = 0.5°
             *
             * The minimum number of fraction digits is the number required for differentiating two consecutive cells.
             * The maximum number of fraction digits avoids to print more digits than the precision of `double` type.
             * Those numbers vary for each line depending on the envelope values and the resolution at that line.
             */
            if (section(ENVELOPE, Vocabulary.Keys.Envelope, true, false)) {
                final boolean appendResolution = (bitmask & RESOLUTION) != 0 && (resolution != null);
                final var table = new TableAppender(buffer, "");
                final int dimension = envelope.getDimension();
                final NumberFormat nf = numberFormat();
                for (int i=0; i<dimension; i++) {
                    final double lower = envelope.getLower(i);
                    final double upper = envelope.getUpper(i);
                    final double delta = (resolution != null) ? resolution[i] : Double.NaN;
                    nf.setMinimumFractionDigits(Numerics.fractionDigitsForDelta(delta));
                    nf.setMaximumFractionDigits(Numerics.suggestFractionDigits(lower, upper) - 1);    // The -1 is for rounding errors.
                    final CoordinateSystemAxis axis = (cs != null) ? cs.getAxis(i) : null;
                    final String name = (axis != null) ? axis.getName().getCode() : vocabulary.getString(Vocabulary.Keys.Dimension_1, i);
                    table.append(name).append(": ").nextColumn();
                    table.setCellAlignment(TableAppender.ALIGN_RIGHT);
                    table.append(nf.format(lower)).nextColumn();
                    table.setCellAlignment(TableAppender.ALIGN_LEFT);
                    table.append(" … ").append(nf.format(upper));
                    if (appendResolution) {
                        final boolean isLinear = (i < Long.SIZE) && (nonLinears & (1L << i)) == 0;
                        table.nextColumn();
                        table.append("  ∆");
                        if (axis != null) {
                            table.append(axis.getAbbreviation());
                        }
                        table.nextColumn();
                        table.append(' ').append(isLinear ? '=' : '≈').append(' ');
                        append(table, delta, Double.NaN, i);
                    }
                    table.nextLine();
                }
                table.flush();
                writeNodes();
                nf.setMinimumFractionDigits(0);
            }
            if (section(ORIGIN, Vocabulary.Keys.Origin, true, false)) {
                /*
                 * Example: Origin
                 * └─ 20° 30°
                 */
                buffer.append('(');
                append(getOrigin(), resolution, "  ");
                buffer.append(')');
                writeNode();
            }
            if (section(RESOLUTION, Vocabulary.Keys.Resolution, true, false)) {
                /*
                 * Example: Resolution
                 * └─ 0.5° × 0.5°
                 *
                 * By default, this is formatted only as a fallback if the envelope was not formatted.
                 * Otherwise, this information is already part of above envelope.
                 */
                append(resolution, null, " × ");
                writeNode();
            }
            /*
             * Example: Coordinate reference system
             * └─ EPSG:4326 — WGS 84 (φ,λ)
             */
            if (section(CRS, Vocabulary.Keys.CoordinateRefSys, true, false)) {
                final Identifier id = IdentifiedObjects.getIdentifier(crs, null);
                if (id != null) {
                    buffer.append(IdentifiedObjects.toString(id)).append(" — ");
                }
                buffer.append(crs.getName().getCode());
                writeNode();
            }
            /*
             * Example: Conversion
             * └─ 2D → 2D non linear in 2
             */
            final Matrix matrix = MathTransforms.getMatrix(gridToCRS);
            if (section(GRID_TO_CRS, Vocabulary.Keys.Conversion, true, matrix != null)) {
                if (matrix != null) {
                    writeNode(Matrices.toString(matrix));
                } else {
                    buffer.append(gridToCRS.getSourceDimensions()).append("D → ")
                          .append(gridToCRS.getTargetDimensions()).append("D ");
                    long nonLinearDimensions = nonLinears;
                    String separator = Resources.forLocale(locale)
                            .getString(Resources.Keys.NonLinearInDimensions_1, Long.bitCount(nonLinearDimensions));
                    while (nonLinearDimensions != 0) {
                        final int i = Long.numberOfTrailingZeros(nonLinearDimensions);
                        nonLinearDimensions &= ~(1L << i);
                        buffer.append(separator).append(' ')
                              .append(cs != null ? cs.getAxis(i).getName().getCode() : String.valueOf(i));
                        separator = ",";
                    }
                    writeNode();
                }
            }
        }

        /**
         * Starts a new section for the given property.
         *
         * @param  property    one of {@link #EXTENT}, {@link #ENVELOPE}, {@link #CRS}, {@link #GRID_TO_CRS} and derived bit masks.
         * @param  title       the {@link Vocabulary} key for the title to show for this section, if formatted.
         * @param  mandatory   whether to write "undefined" if the property is undefined.
         * @param  cellCenter  whether to add a "origin in cell center" text in the title. This is relevant only for conversion.
         * @return {@code true} if the caller shall format the value.
         */
        private boolean section(final int property, final short title, final boolean mandatory, final boolean cellCenter) {
            if ((bitmask & property) != 0) {
                CharSequence text = vocabulary.getString(title);
                if (cellCenter) {
                    text = buffer.append(text).append(" (")
                                 .append(vocabulary.getString(Vocabulary.Keys.OriginInCellCenter).toLowerCase(locale))
                                 .append(')').toString();
                    buffer.setLength(0);
                }
                section = root.newChild();
                section.setValue(TableColumn.VALUE_AS_TEXT, text);
                if (isDefined(property)) {
                    return true;
                }
                if (mandatory) {
                    writeNode(vocabulary.getString(Vocabulary.Keys.Unspecified));
                }
            }
            return false;
        }

        /**
         * Appends a single line as a node in the current section.
         */
        private void writeNode(final CharSequence line) {
            String text = line.toString().trim();
            if (!text.isEmpty()) {
                section.newChild().setValue(TableColumn.VALUE_AS_TEXT, text);
            }
        }

        /**
         * Appends a node with current {@link #buffer} content as a single line, then clears the buffer.
         */
        private void writeNode() {
            writeNode(buffer);
            buffer.setLength(0);
        }

        /**
         * Appends nodes with current {@link #buffer} content as multi-lines text, then clears the buffer.
         */
        private void writeNodes() {
            for (final CharSequence line : CharSequences.splitOnEOL(buffer)) {
                writeNode(line);
            }
            buffer.setLength(0);
        }

        /**
         * Appends the given values with their units of measurement.
         *
         * @param  values      the values to write.
         * @param  resolution  the resolution of the values, or {@link Double#NaN} is unknown.
         * @param  separator   the separator to insert between values.
         */
        private void append(final double[] values, final double[] resolution, final String separator) throws IOException {
            for (int i=0; i < values.length; i++) {
                if (i != 0) buffer.append(separator);
                double delta = (resolution != null) ? resolution[i] : Double.NaN;
                append(buffer, values[i], delta, i);
            }
        }

        /**
         * Appends a single value with a number of digits determined by the resolution.
         * Appends also the unit of measurement.
         *
         * @param  out    where to write the value.
         * @param  value  the value to write (may be {@link Double#NaN}).
         * @param  delta  the resolution of the value, or {@link Double#NaN} is unknown.
         * @param  dim    index of the coordinate system axis of the value.
         */
        private void append(final Appendable out, final double value, final double delta, final int dim) throws IOException {
            if (Double.isNaN(value)) {
                out.append('?');
            } else {
                final NumberFormat nf = numberFormat();
                final int n = Double.isNaN(delta)
                        ? Numerics.suggestFractionDigits(value) / 2
                        : Numerics.fractionDigitsForDelta(delta);
                nf.setMaximumFractionDigits(n + 1);
                out.append(nf.format(value));
            }
            if (cs != null) {
                final String unit = String.valueOf(cs.getAxis(dim).getUnit());
                if (unit.isEmpty() || Character.isLetterOrDigit(unit.codePointAt(0))) {
                    out.append(' ');
                }
                out.append(unit);
            }
        }
    }
}
