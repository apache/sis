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

import java.util.Collection;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.spatial.GeolocationInformation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.math.Vector;


/**
 * The conversion or transformation from pixel coordinates to model coordinates.
 * The target CRS may be the image CRS if the image is "georeferenceable" instead of georeferenced.
 *
 * This is a placeholder before a real {@code GridGeometry} class is ported to Apache SIS.
 * We implement {@code CoordinateOperation} for now for allowing access to the math transform from
 * outside this package, but we will probably not keep this class hierarchy in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@SuppressWarnings("serial")
final class GridGeometry extends AbstractCoordinateOperation implements GeolocationInformation {
    /**
     * Number of floating point values in each (I,J,K,X,Y,Z) record.
     */
    private static final int RECORD_LENGTH = 6;

    /**
     * The desired precision of coordinate transformations in units of pixels.
     * This is an arbitrary value that may be adjusted in any future SIS version.
     */
    private static final double PRECISION = 1E-6;

    /**
     * Creates a new transformation from the information found by {@link ImageFileDirectory}.
     */
    GridGeometry(final String name, final CoordinateReferenceSystem crs, final Vector modelTiePoints)
            throws FactoryException, TransformException
    {
        super(Collections.singletonMap(NAME_KEY, name), null, crs, null, localizationGrid(modelTiePoints));
    }

    /**
     * Builds a localization grid from the given GeoTIFF tie points.
     * This is a workaround for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private static MathTransform localizationGrid(final Vector modelTiePoints) throws FactoryException, TransformException {
        final int size = modelTiePoints.size();
        final int n = size / RECORD_LENGTH;
        final LocalizationGridBuilder grid = new LocalizationGridBuilder(
                modelTiePoints.subSampling(0, RECORD_LENGTH, n),
                modelTiePoints.subSampling(1, RECORD_LENGTH, n));

        final LinearTransform sourceToGrid = grid.getSourceToGrid();
        final double[] ordinates = new double[2];
        for (int i=0; i<size; i += RECORD_LENGTH) {
            ordinates[0] = modelTiePoints.doubleValue(i);
            ordinates[1] = modelTiePoints.doubleValue(i+1);
            sourceToGrid.transform(ordinates, 0, ordinates, 0, 1);
            grid.setControlPoint((int) Math.round(ordinates[0]),
                                 (int) Math.round(ordinates[1]),
                                 modelTiePoints.doubleValue(i+3),
                                 modelTiePoints.doubleValue(i+4));
        }
        grid.setDesiredPrecision(PRECISION);
        return grid.create(null);
    }

    /**
     * Computes translation terms in the given matrix from the (usually singleton) tie point.
     *
     * @param  gridToCRS       the matrix to update. That matrix shall contain the scale factors before to invoke this method.
     * @param  modelTiePoints  the vector of model tie points. Only the first point will be used.
     * @return {@code true} if the given vector is non-null and contains at least one complete record.
     */
    static boolean setTranslationTerms(final MatrixSIS gridToCRS, final Vector modelTiePoints) {
        if (modelTiePoints == null || modelTiePoints.size() < RECORD_LENGTH) {
            return false;
        }
        final DoubleDouble t = new DoubleDouble();
        final int numDim = gridToCRS.getNumRow() - 1;
        final int trCol  = gridToCRS.getNumCol() - 1;
        for (int j=0; j<numDim; j++) {
            t.value = -modelTiePoints.doubleValue(j);
            t.error = DoubleDouble.errorForWellKnownValue(t.value);
            t.divide(gridToCRS.getNumber(j, j));
            t.add(modelTiePoints.doubleValue(j + RECORD_LENGTH/2));
            gridToCRS.setNumber(j, trCol, t);
        }
        return true;
    }

    /**
     * Writes the check point or Ground Control Points (GCP) in the metadata.
     *
     * @param metadata        where to write the ground control points.
     * @param modelTiePoints  the vector of model tie points.
     */
    static void addControlPoints(final CoordinateReferenceSystem crs, final MetadataBuilder metadata, final Vector modelTiePoints)
            throws FactoryException, TransformException
    {
        metadata.addControlPoints(crs, modelTiePoints, RECORD_LENGTH/2, RECORD_LENGTH);
    }

    /**
     * Provides an overall assessment of quality of geolocation information.
     */
    @Override
    public Collection<DataQuality> getQualityInfo() {
        return Collections.emptyList();
    }
}
