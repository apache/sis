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
package org.apache.sis.geometries.processor;

import java.util.ArrayList;
import java.util.List;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ProcessorUtils extends Static {

    /**
     * Ensure given geometries have the same CRS.
     * @param geom1 not null
     * @param geom2 not null
     */
    public static void ensureSameCRS(Geometry geom1, Geometry geom2) {
        final CoordinateReferenceSystem crs1 = geom1.getCoordinateReferenceSystem();
        final CoordinateReferenceSystem crs2 = geom2.getCoordinateReferenceSystem();
        if (!Utilities.equalsIgnoreMetadata(crs1, crs2)) {
            throw new OperationException("Geometries do not have the same CRS");
        }
    }

    /**
     * Ensure given geometries have the same CRS 2D.
     * @param geom1 not null
     * @param geom2 not null
     */
    public static void ensureSameCRS2D(Geometry geom1, Geometry geom2) {
        CoordinateReferenceSystem crs1 = geom1.getCoordinateReferenceSystem();
        CoordinateReferenceSystem crs2 = geom2.getCoordinateReferenceSystem();
        final int dim1 = crs1.getCoordinateSystem().getDimension();
        final int dim2 = crs2.getCoordinateSystem().getDimension();
        if (dim1 < 2 || dim2 < 2) {
            throw new OperationException("Geometries CRS must be at least 2D");
        }

        if (!Utilities.equalsIgnoreMetadata(crs1, crs2)) {
            if (dim1 != 2 || dim2 != 2) {
                CoordinateReferenceSystem subcrs1 = getCrs2d(crs1);
                CoordinateReferenceSystem subcrs2 = getCrs2d(crs2);
                if (subcrs1 == null || !Utilities.equalsIgnoreMetadata(subcrs1, subcrs2)) {
                    throw new OperationException("Geometries do not have the same CRS 2D");
                }
            }
        }
    }

    private static CoordinateReferenceSystem getCrs2d(CoordinateReferenceSystem crs) {
        CoordinateReferenceSystem subcrs = org.apache.sis.referencing.CRS.getComponentAt(crs, 0, 2);
        if (subcrs == null) subcrs = getOrCreateSubCRS(crs, 0, 2);
        if (subcrs == null) {
            //in some cases may be the only solution, TODO, how to ensure the horizontal part is on index [0,2] ?
            subcrs = org.apache.sis.referencing.CRS.getHorizontalComponent(crs);
        }
        return subcrs;
    }


    /**
     * @todo Duplicate of {@link org.apache.sis.referencing.privy.ReferencingFactoryContainer}?
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-162">SIS-162</a>
     */
    private static CoordinateReferenceSystem getOrCreateSubCRS(CoordinateReferenceSystem crs, int lower, int upper) {
        if (crs == null) return crs;

        int dimension = crs.getCoordinateSystem().getDimension();
        if (lower < 0 || lower > upper || upper > dimension) {
            throw new IndexOutOfBoundsException("["+lower+".."+upper+"] range is not possible with crs dimension "+ dimension);
        }

        // Dimension exactly matches, no need to decompse the CRS
        if (lower == 0 && dimension == upper) return crs;

        // CRS can not be decomposed
        if (!(crs instanceof CompoundCRS)) return null;

        final List<CoordinateReferenceSystem> parts = new ArrayList<>(1);
        final int res = decomposeCRS(crs, lower, upper, parts);
        if (res == -1) {
            // CRS could not be divided
            return null;
        }

        final int size = parts.size();
        if (size == 1) {
            return parts.get(0);
        } else try {
            return org.apache.sis.referencing.CRS.compound(parts.toArray(new CoordinateReferenceSystem[size]));
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Illegal CRS.", e);
        }
    }

    /**
     * Internal use only.
     * Fill a list of CoordinateReferenceSystem with CRS parts in the given lower/upper range.
     *
     * @param crs CoordinateReferenceSystem to decompose
     * @param lower dimension start range
     * @param upper dimension start range
     * @param parts used to stack CoordinateReferenceSystem when decomposing CRS.
     * @return number of dimensions used, -1 if the current crs could not be decomposed to match lower/upper bounds
     */
    private static int decomposeCRS(CoordinateReferenceSystem crs, int lower, int upper, final List<CoordinateReferenceSystem> parts) {
        final int dimension = crs.getCoordinateSystem().getDimension();

        if (lower == 0 && dimension <= upper) {
            // Dimension is smaller or exactly match, no need to decompse the crs
            parts.add(crs);
            return dimension;
        } else if (lower >= dimension){
            // Skip this CRS
            return dimension;
        }

        // CRS can not be decomposed
        if (!(crs instanceof CompoundCRS)) return -1;

        int nbDimRead = 0;
        final List<CoordinateReferenceSystem> components = ((CompoundCRS) crs).getComponents();
        for (CoordinateReferenceSystem component : components) {
            int res = decomposeCRS(component, lower, upper, parts);
            if (res == -1) {
                // Sub element could not be decomposed
                return -1;
            }
            nbDimRead += res;
            lower = Math.max(0, lower-res);
            upper = Math.max(0, upper-res);
            if (upper == 0) break;
        }

        return nbDimRead;
    }
}
