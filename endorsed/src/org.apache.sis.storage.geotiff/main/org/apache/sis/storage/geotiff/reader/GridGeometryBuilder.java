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

import java.time.Instant;
import java.util.NoSuchElementException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.geotiff.base.Resources;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.math.Vector;


/**
 * Helper class for creating a {@link GridGeometry} from GeoTIFF data.
 * The coordinate reference system part is built by {@link CRSBuilder}.
 *
 * <h2>Pixel center versus pixel corner</h2>
 * The policy about whether the conversion maps pixel corner or pixel center in GeoTIFF files does not seem
 * totally clear. But the practice at least with GDAL seems to consider the following as equivalent:
 *
 * <pre class="text">
 *     ModelTiepointTag = (0.0, 0.0, 0.0, -180.0, 90.0, 0.0)
 *     ModelPixelScaleTag = (0.002777777778, 0.002777777778, 0.0)
 *     GeoKeyDirectoryTag:
 *         GTModelTypeGeoKey    = 2    (ModelTypeGeographic)
 *         GTRasterTypeGeoKey   = 1    (RasterPixelIsArea)
 *         GeographicTypeGeoKey = 4326 (GCS_WGS_84)</pre>
 *
 * and
 *
 * <pre class="text">
 *     ModelTiepointTag = (-0.5, -0.5, 0.0, -180.0, 90.0, 0.0)
 *     ModelPixelScaleTag = (0.002777777778, 0.002777777778, 0.0)
 *     GeoKeyDirectoryTag:
 *         GTModelTypeGeoKey    = 2    (ModelTypeGeographic)
 *         GTRasterTypeGeoKey   = 2    (RasterPixelIsPoint)
 *         GeographicTypeGeoKey = 4326 (GCS_WGS_84)</pre>
 *
 * The former is {@link PixelInCell#CELL_CORNER} convention while the latter is {@link PixelInCell#CELL_CENTER}.
 * Note that the translation coefficients in the <i>grid to CRS</i> matrix is {@code crs - grid × scale}.
 * So compared to the {@code CELL_CORNER} case, the {@code CELL_CENTER} case has a translation of +0.5 × scale.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GridGeometryBuilder extends GeoKeysLoader {
    /**
     * Number of dimensions of the horizontal part.
     */
    public static final int BIDIMENSIONAL = 2;

    //  ╔════════════════════════════════════════════════════════════════════════════════╗
    //  ║                                                                                ║
    //  ║    Information to be set by ImageFileDirectory during GeoTIFF file parsing.    ║
    //  ║                                                                                ║
    //  ╚════════════════════════════════════════════════════════════════════════════════╝

    /*
     * Fields inherited from `GeoKeysLoader`:
     *   - keyDirectory:       references the GeoKeys needed for building the Coordinate Reference System.
     *   - numericParameters:  the numeric values referenced by the `keyDirectory`.
     *   - asciiParameters:    the characters referenced by the `keyDirectory`.
     */

    /**
     * Raster model tie points. This vector contains coordinate values structured as (I,J,K, X,Y,Z) records.
     * The (I,J,K) coordinate values specify the point at location (I,J) in raster space with pixel-value K,
     * and (X,Y,Z) coordinate values specify the point in the Coordinate Reference System. In most cases the
     * coordinate system is only two-dimensional, in which case both K and Z should be set to zero.
     */
    public Vector modelTiePoints;

    /**
     * The conversion from grid coordinates to <abbr>CRS</abbr> coordinates as an affine transform.
     * The "grid to CRS" transform can be determined in different ways, from simpler to more complex:
     *
     * <ul>
     *   <li>By a combination of a single {@link #modelTiePoints} with the 3 values given in
     *       {@code ModelPixelScaleTag} as documented in the Javadoc of that tag.</li>
     *   <li>By a {@code ModelTransformationTag} giving all coefficients of the 4×4 matrix}.
     *       Note that the third row and the third column have all their value set to 0 if the
     *       space model (or the coordinate reference system) should be two-dimensional.</li>
     *   <li>By building a non-linear transformation from all {@link #modelTiePoints}.
     *       Such transformation cannot be stored in a matrix, so will leave this field {@code null}.</li>
     * </ul>
     *
     * By convention, the translation column is set to NaN values if it needs to be computed from the tie point.
     */
    private MatrixSIS affine;

    /**
     * {@code true} if {@link #affine} has been specified by a complete matrix ({@code ModelTransformationTag}),
     * or {@code false} if it has been specified by the scale factors only ({@code ModelPixelScaleTag}).
     */
    private boolean completeMatrixSpecified;

    /**
     * Sets the {@link #affine} transform from a complete matrix.
     *
     * @param  terms  the matrix in a row-major fashion.
     * @param  size   the matrix size, either 3 or 4.
     */
    public void setGridToCRS(final Vector terms, final int size) {
        final int length = terms.size();
        completeMatrixSpecified = true;
        affine = Matrices.createZero(size, size);
        affine.setElement(size-1, size-1, 1);
        for (int i=0; i<length; i++) {
            affine.setElement(i / size, i % size, terms.doubleValue(i));
        }
    }

    /**
     * Sets only the scale terms of the {@link #affine} transform.
     * The translation terms are set to NaN, meaning they will need to be determined later.
     */
    public void setScaleFactors(final Vector terms) {
        final int size = terms.size();
        completeMatrixSpecified = false;
        affine = Matrices.createZero(size+1, size+1);
        affine.setElement(size, size, 1);
        for (int i=0; i<size; i++) {
            double e = terms.doubleValue(i);
            if (i == 1) e = -e;                             // Make y scale factor negative.
            affine.setElement(i, i, e);
            affine.setElement(i, size, Double.NaN);
        }
    }



    //  ╔════════════════════════════════════════════════════════════════════════════════╗
    //  ║                                                                                ║
    //  ║    Information to be computed by GridGeometryBuilder based on above data.      ║
    //  ║                                                                                ║
    //  ╚════════════════════════════════════════════════════════════════════════════════╝

    /**
     * Suggested value for a general description of the transformation form grid coordinates to "real world" coordinates.
     * This information is obtained as a side-effect of {@link #build(StoreListeners, long, long, Instant)} call.
     *
     * @see #completeMetadata(GridGeometry, MetadataBuilder)
     */
    private String description;

    /**
     * {@code POINT} if {@link GeoKeys#RasterType} is {@link GeoCodes#RasterPixelIsPoint},
     * {@code AREA} if it is {@link GeoCodes#RasterPixelIsArea}, or null if unspecified.
     * This information is obtained as a side-effect of {@link #build(StoreListeners, long, long, Instant)} call.
     *
     * @see #completeMetadata(GridGeometry, MetadataBuilder)
     */
    private CellGeometry cellGeometry;

    /**
     * Creates a new builder.
     */
    public GridGeometryBuilder() {
    }

    /**
     * If {@link #affine} has been specified with only the scale factor, computes the translation terms now.
     * If needed, this method computes the translation terms from the (usually singleton) tie point.
     * This happen when the GeoTIFF file has a {@code ModelPixelScaleTag} and {@code ModelTiePointTag}.
     * The latter should have a single record.
     *
     * @return {@code true} on success (including nothing to compute), or
     *         {@code false} if the computation attempt failed because of missing {@code ModelTiePointTag}.
     *
     * @see ImageFileDirectory#validateMandatoryTags()
     */
    public boolean validateMandatoryTags() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final MatrixSIS affine = this.affine;
        if (affine == null || completeMatrixSpecified) {
            return true;
        }
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Vector modelTiePoints = this.modelTiePoints;
        if (modelTiePoints != null) {
            /*
             * The GeoTIFF specification recommends that the first point is located at grid indices (0,0).
             * But as a safety, we will nevertheless search in the grid for the point closest to origin.
             * If the grid is affine, using the corner closest to (0,0) reduces rounding errors compared
             * to using another corner. If the grid is not affine, then ModelPixelScaleTag should not have
             * been defined for that file…
             */
            int nearest = 0;                                // Index of the record nearest to origin.
            double distance = Double.POSITIVE_INFINITY;     // Distance squared of the nearest record.
            final int size = modelTiePoints.size();
            for (int i=0; i<size; i += Localization.RECORD_LENGTH) {
                double t;
                final double d = (t = modelTiePoints.doubleValue(i    )) * t
                               + (t = modelTiePoints.doubleValue(i + 1)) * t
                               + (t = modelTiePoints.doubleValue(i + 2)) * t;
                if (d < distance) {
                    distance = d;
                    nearest = i;
                    if (d == 0) break;                      // Optimization for the standard case.
                }
            }
            /*
             * Grid to CRS conversion:  crs = grid × scale + translation
             * We rearrange as:         translation = crs - grid × scale
             * where:                   grid   =  modelTiePoints[i]
             *                          crs    =  modelTiePoints[i + RECORD_LENGTH / BIDIMENSIONAL]
             *                          scale  =  affine(i,i)  —  on the diagonal
             */
            if (distance != Double.POSITIVE_INFINITY) {
                final boolean decimal = true;               // Whether values were intended to be exact in base 10.
                final int numDim = affine.getNumRow() - 1;
                final int trCol  = affine.getNumCol() - 1;
                for (int j=0; j<numDim; j++) {
                    final double src = -modelTiePoints.doubleValue(nearest + j);
                    final double tgt =  modelTiePoints.doubleValue(nearest + j + Localization.RECORD_LENGTH / BIDIMENSIONAL);
                    var t = DoubleDouble.of(src, decimal).multiply(affine.getNumber(j,j), decimal).add(tgt, decimal);
                    affine.setNumber(j, trCol, t);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the grid geometry and collect related metadata.
     * This method shall be invoked exactly once after {@link #validateMandatoryTags()}.
     * After this method call (if successful), the returned value is guaranteed non-null
     * and can be used as a flag for determining that the build has been completed.
     *
     * @param  listeners  the listeners where to report warnings.
     * @param  width      the image width in pixels.
     * @param  height     the image height in pixels.
     * @param  imageDate  the date/time found in the {@code DATE_TIME} tag, or {@code null} if none.
     * @return the grid geometry, guaranteed non-null.
     * @throws FactoryException if an error occurred while creating a CRS or a transform.
     */
    @SuppressWarnings("fallthrough")
    public GridGeometry build(final StoreListeners listeners, final long width, final long height, final Instant imageDate)
            throws FactoryException
    {
        CoordinateReferenceSystem crs = null;
        if (keyDirectory != null) {
            final var helper = new CRSBuilder(listeners);
            try {
                crs = helper.build(this);
                description  = helper.description;
                cellGeometry = helper.cellGeometry;
            } catch (NoSuchIdentifierException | ParameterNotFoundException e) {
                short key = Resources.Keys.UnsupportedProjectionMethod_1;
                if (e instanceof NoSuchAuthorityCodeException) {
                    key = Resources.Keys.UnknownCRS_1;
                }
                listeners.warning(Resources.forLocale(listeners.getLocale()).getString(key, listeners.getSourceName()), e);
            } catch (IllegalArgumentException | NoSuchElementException | ClassCastException e) {
                if (!helper.alreadyReported) {
                    canNotCreate(listeners, e);
                }
            }
        }
        /*
         * If the CRS is non-null, then the spatial part is either two- or three-dimensional.
         * A temporal axis may be added to the non-null CRS.
         */
        final double timeCoordinate;
        final TemporalCRS temporalCRS;
        final int spatialDimension = (crs != null) ? crs.getCoordinateSystem().getDimension() : BIDIMENSIONAL;
        final int dimension;
        if (imageDate != null) {
            dimension = spatialDimension + 1;
            temporalCRS = CommonCRS.defaultTemporal();
            timeCoordinate = DefaultTemporalCRS.castOrCopy(temporalCRS).toValue(imageDate);
            if (crs != null) {
                crs = CRS.compound(crs, temporalCRS);
            }
        } else {
            dimension = spatialDimension;
            timeCoordinate = Double.NaN;
            temporalCRS = null;
        }
        final var axisTypes = new DimensionNameType[dimension];
        final var high = new long[dimension];
        if (temporalCRS != null) {
            axisTypes[spatialDimension] = DimensionNameType.TIME;
        }
        switch (spatialDimension) {
            default: axisTypes[2] = DimensionNameType.VERTICAL; // Fallthrough everywhere.
            case 2:  axisTypes[1] = DimensionNameType.ROW;      high[1] = height - 1;
            case 1:  axisTypes[0] = DimensionNameType.COLUMN;   high[0] = width  - 1;
            case 0:  break;
        }
        final var extent = new GridExtent(axisTypes, null, high, true);
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        PixelInCell anchor = (cellGeometry == CellGeometry.POINT) ? PixelInCell.CELL_CENTER : PixelInCell.CELL_CORNER;
        GridGeometry gridGeometry;
        try {
            MathTransform gridToCRS = null;
            if (affine != null) {
                /*
                 * The `affine` matrix is always 4×4 in a GeoTIFF file, which may be larger than requested.
                 * Resize the matrix to the size that we need. Maybe the last dimension, initially ignored,
                 * become used by the temporal dimension, so we need to clear that dimension for safety.
                 */
                final Matrix m = Matrices.resizeAffine(affine, dimension + 1, dimension + 1);
                if (temporalCRS != null) {
                    for (int i=0; i <= dimension; i++) {
                        m.setElement(spatialDimension, i, 0);
                        m.setElement(i, spatialDimension, 0);
                    }
                    m.setElement(spatialDimension, dimension, timeCoordinate);
                    m.setElement(spatialDimension, spatialDimension, Double.NaN);   // Unknown duration.
                }
                /*
                 * If the CRS has no vertical component, then the matrix row and column for the vertical coordinates
                 * should be ignored. However, we observed inconsistency in some GeoTIFF files between the number of
                 * CRS dimensions and the matrix rows which have been assigned non-zero values. Because rows of only
                 * zero values cause problems, we assign a NaN value in one of their columns.
                 */
                Matrices.forceNonZeroScales(m, Double.NaN);
                gridToCRS = factory.createAffineTransform(m);
            } else if (modelTiePoints != null) {
                anchor    = PixelInCell.CELL_CENTER;
                gridToCRS = Localization.nonLinear(modelTiePoints);
                gridToCRS = factory.createPassThroughTransform(0, gridToCRS, spatialDimension - BIDIMENSIONAL);
                if (temporalCRS != null) {
                    gridToCRS = MathTransforms.compound(gridToCRS, MathTransforms.linear(Double.NaN, timeCoordinate));
                }
            }
            gridGeometry = new GridGeometry(extent, anchor, gridToCRS, crs);
        } catch (TransformException e) {
            /*
             * Note: we catch TransformExceptions because they may be caused by erroneous data in the GeoTIFF file,
             * but let FactoryExceptions propagate because they are more likely to be a SIS configuration problem.
             */
            GeneralEnvelope envelope = null;
            if (crs != null) {
                envelope = new GeneralEnvelope(crs);
                envelope.setToNaN();
                if (temporalCRS != null && anchor == PixelInCell.CELL_CENTER) {
                    // The coordinate is the lower value (start) of the time range.
                    envelope.setRange(spatialDimension, timeCoordinate, Double.NaN);
                }
            }
            gridGeometry = new GridGeometry(extent, envelope, GridOrientation.UNKNOWN);
            canNotCreate(listeners, e);
        }
        keyDirectory      = null;            // Not needed anymore, so let GC do its work.
        numericParameters = null;
        asciiParameters   = null;
        modelTiePoints    = null;
        affine            = null;
        return gridGeometry;
    }

    /**
     * Completes ISO 19115 metadata with some GeoTIFF values inferred from the geotags.
     *
     * <h4>Prerequisite</h4>
     * <ul>
     *   <li>{@link #build(StoreListeners, long, long, Instant)} must have been invoked successfully before this method.</li>
     *   <li>{@link ImageFileDirectory} must have filled its part of metadata before to invoke this method.</li>
     * </ul>
     *
     * This method invokes {@link MetadataBuilder#newGridRepresentation(MetadataBuilder.GridType)}
     * with the appropriate {@code GEORECTIFIED} or {@code GEOREFERENCEABLE} type.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/*}</li>
     *   <li>{@code metadata/identificationInfo/spatialRepresentationType}</li>
     *   <li>{@code metadata/referenceSystemInfo}</li>
     * </ul>
     *
     * @param  gridGeometry  the grid geometry computed by {@link #build(StoreListeners, long, long, Instant)}.
     * @param  metadata      the helper class where to write metadata values.
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
     */
    public void completeMetadata(final GridGeometry gridGeometry, final MetadataBuilder metadata) {
        if (metadata.addSpatialRepresentation(description, gridGeometry, true)) {
            /*
             * Whether the pixel value is thought of as filling the cell area or is considered as point measurements at
             * the vertices of the grid (not in the interior of a cell).  This is determined by the value associated to
             * GeoKeys.RasterType, which can be GeoCodes.RasterPixelIsArea or GeoCodes.RasterPixelIsPoint.
             *
             * Note: the pixel orientation (UPPER_LEFT versus CENTER) should be kept consistent with the discussion in
             * GridGeometryBuilder class javadoc.
             */
            metadata.setCellGeometry(cellGeometry);
            final PixelOrientation po;
            if (cellGeometry == CellGeometry.POINT) {
                po = PixelOrientation.CENTER;
            } else if (cellGeometry == CellGeometry.AREA) {
                po = PixelOrientation.UPPER_LEFT;
            } else {
                return;
            }
            metadata.setPointInPixel(po);
        }
    }

    /**
     * Logs a warning telling that we cannot create a grid geometry for the given reason.
     */
    private static void canNotCreate(final StoreListeners listeners, final Exception e) {
        listeners.warning(Resources.forLocale(listeners.getLocale()).getString(
                Resources.Keys.CanNotComputeGridGeometry_1, listeners.getSourceName()), e);
    }
}
