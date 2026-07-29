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
package org.apache.sis.storage.coverage;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.coverage.grid.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


/**
 * A set of utilities methods for the Grid Coverage package. Those methods are not really
 * rigorous; must of them should be seen as temporary implementations.
 *
 * @author Martin Desruisseaux (IRD)
 */
public final class CoverageUtilities {
    /**
     * Do not allows instantiation of this class.
     */
    private CoverageUtilities() {
    }

    /**
     * Shift lower coordinates to zero; this is what BufferedImage wants.
     */
    public static GridGeometry forceLowerToZero(final GridGeometry gg) {
        if (gg != null && gg.isDefined(GridGeometry.EXTENT)) {
            final GridExtent extent = gg.getExtent();
            if (!extent.startsAtZero()) {
                CoordinateReferenceSystem crs = null;
                if (gg.isDefined(GridGeometry.CRS)) crs = gg.getCoordinateReferenceSystem();
                final int dimension = extent.getDimension();
                final double[] vector = new double[dimension];
                final long[] high = new long[dimension];
                for (int i=0; i<dimension; i++) {
                    final long low = extent.getLow(i);
                    high[i] = extent.getHigh(i) - low;
                    vector[i] = low;
                }
                MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CENTER);
                gridToCRS = MathTransforms.concatenate(MathTransforms.translation(vector), gridToCRS);
                return new GridGeometry(new GridExtent(null, null, high, true), PixelInCell.CELL_CENTER, gridToCRS, crs);
            }
        }
        return gg;
    }

}
