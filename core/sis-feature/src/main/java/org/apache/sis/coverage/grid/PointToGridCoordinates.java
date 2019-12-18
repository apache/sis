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

import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.CRS;


/**
 * Holds the object necessary for converting a geospatial coordinates to grid coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class PointToGridCoordinates {
    /**
     * The source coordinate reference system of the converter,
     * or {@code null} if assumed the same than the coverage CRS.
     */
    final CoordinateReferenceSystem sourceCRS;

    /**
     * The transform from {@link #sourceCRS} to grid coordinates.
     */
    final MathTransform crsToGrid;

    /**
     * Creates a new objects holding the objects for converting from the given source CRS.
     *
     * <div class="note"><b>Note about coverage argument:</b>
     * the {@code coverage} argument is for fetching the coverage CRS when needed.
     * We could get that CRS from {@link GridCoverage#getCoordinateReferenceSystem()},
     * but we use {@link GridCoverage#getCoordinateReferenceSystem()} instead for giving users a
     * chance to override. We do not give the coverage CRS in argument because we want to invoke
     * {@link GridCoverage#getCoordinateReferenceSystem()} only if {@code sourceCRS} is non-null,
     * because {@code getCoordinateReferenceSystem()} may throw {@link IncompleteGridGeometryException}.
     * </div>
     *
     * @param  sourceCRS      the source CRS, or {@code null} if assumed the same than the coverage CRS.
     * @param  coverage       the coverage for which we are building those information.
     * @param  gridGeometry   the coverage grid geometry.
     * @throws TransformException if the {@link #crsToGrid} transform can not be built.
     */
    PointToGridCoordinates(final CoordinateReferenceSystem sourceCRS, final GridCoverage coverage,
                           final GridGeometry gridGeometry) throws TransformException
    {
        this.sourceCRS = sourceCRS;
        MathTransform tr = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER).inverse();
        if (sourceCRS != null) try {
            CoordinateOperation op = CRS.findOperation(sourceCRS,
                    coverage.getCoordinateReferenceSystem(),        // See comment in above javadoc.
                    gridGeometry.geographicBBox());
            tr = MathTransforms.concatenate(op.getMathTransform(), tr);
        } catch (FactoryException e) {
            throw new TransformException(e.getMessage(), e);
        }
        crsToGrid = tr;
    }
}
