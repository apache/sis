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
package org.apache.sis.storage.geotiff;

import java.util.NoSuchElementException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.math.Vector;


/**
 * Helper class for creating a {@link GridGeometry} from GeoTIFF data.
 * The coordinate reference system part is built by {@link CRSBuilder}.
 *
 * <div class="section">Pixel center versus pixel corner</div>
 * The policy about whether the conversion map pixel corner or pixel center if GeoTIFF files does not seem
 * totally clear. But the practice at least with GDAL seems to consider the following as equivalent:
 *
 * {@preformat text
 *     ModelTiepointTag = (0.0, 0.0, 0.0, -180.0, 90.0, 0.0)
 *     ModelPixelScaleTag = (0.002777777778, 0.002777777778, 0.0)
 *     GeoKeyDirectoryTag:
 *         GTModelTypeGeoKey    = 2    (ModelTypeGeographic)
 *         GTRasterTypeGeoKey   = 1    (RasterPixelIsArea)
 *         GeographicTypeGeoKey = 4326 (GCS_WGS_84)
 * }
 *
 * and
 *
 * {@preformat text
 *     ModelTiepointTag = (-0.5, -0.5, 0.0, -180.0, 90.0, 0.0)
 *     ModelPixelScaleTag = (0.002777777778, 0.002777777778, 0.0)
 *     GeoKeyDirectoryTag:
 *         GTModelTypeGeoKey    = 2    (ModelTypeGeographic)
 *         GTRasterTypeGeoKey   = 2    (RasterPixelIsPoint)
 *         GeographicTypeGeoKey = 4326 (GCS_WGS_84)
 * }
 *
 * The former is {@link PixelInCell#CELL_CORNER} convention while the later is {@link PixelInCell#CELL_CENTER}.
 * Note that the translation coefficients in the <cite>grid to CRS</cite> matrix is {@code crs - grid × scale}.
 * So compared to the {@code CELL_CORNER} case, the {@code CELL_CENTER} case has a translation of +0.5 × scale.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class GridGeometryBuilder {
    /**
     * The reader for which we will create coordinate reference systems.
     * This is used for reporting warnings.
     */
    private final Reader reader;

    ////////////////////////////////////////////////////////////////////////////////////////
    ////                                                                                ////
    ////    Information to be set by ImageFileDirectory during GeoTIFF file parsing.    ////
    ////                                                                                ////
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * References the {@link GeoKeys} needed for building the Coordinate Reference System.
     */
    public Vector keyDirectory;

    /**
     * The numeric values referenced by the {@link #keyDirectory}.
     */
    public Vector numericParameters;

    /**
     * The characters referenced by the {@link #keyDirectory}.
     */
    public String asciiParameters;

    /**
     * Raster model tie points. This vector contains ordinate values structured as (I,J,K, X,Y,Z) records.
     * The (I,J,K) ordinate values specify the point at location (I,J) in raster space with pixel-value K,
     * and (X,Y,Z) ordinate values specify the point in the Coordinate Reference System. In most cases the
     * coordinate system is only two-dimensional, in which case both K and Z should be set to zero.
     */
    public Vector modelTiePoints;

    /**
     * The conversion from grid coordinates to CRS coordinates as an affine transform.
     * The "grid to CRS" transform can be determined in different ways, from simpler to more complex:
     *
     * <ul>
     *   <li>By a combination of a single {@link #modelTiePoints} with the 3 values given in
     *       {@link Tags#ModelPixelScaleTag} as documented in the Javadoc of that tag.</li>
     *   <li>By a {@link Tags#ModelTransformation} giving all coefficients of the 4×4 matrix}.
     *       Note that the third row and the third column have all their value set to 0 if the
     *       space model (or the coordinate reference system) should be two-dimensional.</li>
     *   <li>By building a non-linear transformation from all {@link #modelTiePoints}.
     *       Such transformation can not be stored in a matrix, so will leave this field {@code null}.</li>
     * </ul>
     *
     * By convention, the translation column is set to NaN values if it needs to be computed from the tie point.
     */
    private MatrixSIS affine;

    /**
     * {@code true} if {@link #affine} has been specified by a complete matrix ({@link Tags#ModelTransformation}),
     * or {@code false} if it has been specified by the scale factors only ({@link Tags#ModelPixelScaleTag}).
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



    ////////////////////////////////////////////////////////////////////////////////////////
    ////                                                                                ////
    ////    Information to be computed by GridGeometryBuilder based on above data.      ////
    ////                                                                                ////
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The grid geometry to be created by {@link #build(GridExtent)}.
     */
    public GridGeometry gridGeometry;

    /**
     * Suggested value for a general description of the transformation form grid coordinates to "real world" coordinates.
     * This information is obtained as a side-effect of {@link #build(GridExtent)} call.
     */
    private String description;

    /**
     * {@code POINT} if {@link GeoKeys#RasterType} is {@link GeoCodes#RasterPixelIsPoint},
     * {@code AREA} if it is {@link GeoCodes#RasterPixelIsArea}, or null if unspecified.
     * This information is obtained as a side-effect of {@link #build(GridExtent)} call.
     */
    private CellGeometry cellGeometry;

    /**
     * Creates a new builder.
     *
     * @param reader  where to report warnings if any.
     */
    GridGeometryBuilder(final Reader reader) {
        this.reader = reader;
    }

    /**
     * If {@link #affine} has been specified with only the scale factor, computes the translation terms now.
     * If needed, this method computes the translation terms from the (usually singleton) tie point.
     * This happen when the GeoTIFF file has a {@link Tags#ModelPixelScaleTag} and {@link Tags#ModelTiePoints}.
     * The later should have a single record.
     *
     * @return {@code true} on success (including nothing to compute), or {@code false} if the computation attempt
     *         failed because of missing {@link Tags#ModelTiePoints}.
     *
     * @see ImageFileDirectory#validateMandatoryTags()
     */
    public boolean validateMandatoryTags() {
        final MatrixSIS affine = this.affine;
        if (affine == null || completeMatrixSpecified) {
            return true;
        }
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
             *                          crs    =  modelTiePoints[i + RECORD_LENGTH/2]
             *                          scale  =  affine(i,i)  —  on the diagonal
             */
            if (distance != Double.POSITIVE_INFINITY) {
                final DoubleDouble t = new DoubleDouble();
                final int numDim = affine.getNumRow() - 1;
                final int trCol  = affine.getNumCol() - 1;
                for (int j=0; j<numDim; j++) {
                    t.value = -modelTiePoints.doubleValue(nearest + j);
                    t.error = DoubleDouble.errorForWellKnownValue(t.value);
                    t.multiply(affine.getNumber(j, j));
                    t.add(modelTiePoints.doubleValue(nearest + j + Localization.RECORD_LENGTH / 2));
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
     * After this method call (if successful), {@link #gridGeometry} is guaranteed non-null
     * and can be used as a flag for determining that the build has been completed.
     *
     * @param  extent  the image width and height in pixels. Must be two-dimensional.
     * @return {@link #gridGeometry}, guaranteed non-null.
     * @throws FactoryException if an error occurred while creating a CRS or a transform.
     */
    public GridGeometry build(final GridExtent extent) throws FactoryException {
        CoordinateReferenceSystem crs = null;
        if (keyDirectory != null) {
            final CRSBuilder helper = new CRSBuilder(reader);
            try {
                crs = helper.build(keyDirectory, numericParameters, asciiParameters);
                description  = helper.description;
                cellGeometry = helper.cellGeometry;
            } catch (NoSuchIdentifierException | ParameterNotFoundException e) {
                short key = Resources.Keys.UnsupportedProjectionMethod_1;
                if (e instanceof NoSuchAuthorityCodeException) {
                    key = Resources.Keys.UnknownCRS_1;
                }
                reader.owner.warning(reader.resources().getString(key, reader.owner.getDisplayName()), e);
            } catch (IllegalArgumentException | NoSuchElementException | ClassCastException e) {
                if (!helper.alreadyReported) {
                    reader.owner.warning(null, e);
                }
            }
        }
        boolean pixelIsPoint = CellGeometry.POINT.equals(cellGeometry);
        try {
            final MathTransform gridToCRS;
            if (affine != null) {
                gridToCRS = MathTransforms.linear(affine);
            } else {
                gridToCRS = Localization.nonLinear(modelTiePoints);
                pixelIsPoint = true;
            }
            gridGeometry = new GridGeometry(extent, pixelIsPoint ? PixelInCell.CELL_CENTER : PixelInCell.CELL_CORNER, gridToCRS, crs);
        } catch (TransformException e) {
            gridGeometry = new GridGeometry(extent, crs);
            reader.owner.warning(null, e);
            /*
             * Note: we catch TransformExceptions because they may be caused by erroneous data in the GeoTIFF file,
             * but let FactoryExceptions propagate because they are more likely to be a SIS configuration problem.
             */
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
     * <p><b>Pre-requite:</b></p>
     * <ul>
     *   <li>{@link #build(GridExtent)} must have been invoked successfully before this method.</li>
     *   <li>{@link ImageFileDirectory} must have filled its part of metadata before to invoke this method.</li>
     * </ul>
     *
     * This method invokes {@link MetadataBuilder#newGridRepresentation(MetadataBuilder.GridType)}
     * with the appropriate {@code GEORECTIFIED} or {@code GEOREFERENCEABLE} type.
     *
     * @param  metadata  the helper class where to write metadata values.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     */
    public void completeMetadata(final MetadataBuilder metadata) {
        final boolean isGeorectified = (modelTiePoints == null) || (affine != null);
        metadata.newGridRepresentation(isGeorectified ? MetadataBuilder.GridType.GEORECTIFIED
                                                      : MetadataBuilder.GridType.GEOREFERENCEABLE);
        metadata.setGeoreferencingAvailability(affine != null, false, false);
        if (gridGeometry != null && gridGeometry.isDefined(GridGeometry.CRS)) {
            metadata.addReferenceSystem(gridGeometry.getCoordinateReferenceSystem());
        }
        metadata.setGridToCRS(description);
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
        if (CellGeometry.POINT.equals(cellGeometry)) {
            po = PixelOrientation.CENTER;
        } else if (CellGeometry.AREA.equals(cellGeometry)) {
            po = PixelOrientation.UPPER_LEFT;
        } else {
            return;
        }
        metadata.setPointInPixel(po);
    }
}
