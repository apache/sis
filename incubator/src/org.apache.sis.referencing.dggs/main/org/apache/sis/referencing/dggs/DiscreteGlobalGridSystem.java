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
package org.apache.sis.referencing.dggs;

import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Integrated system comprising a hierarchy of discrete global grids, spatiotemporal referencing by zonal
 * identifiers and functions for quantization, zonal query, and interoperability.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#term-dggs
 */
public interface DiscreteGlobalGridSystem {

    /**
     * @return structure of the DGGS grids
     */
    DiscreteGlobalGridHierarchy getHierarchy();

    /**
     * @return base CRS
     */
    CoordinateReferenceSystem getCrs();

    /**
     * @return surface of the celestial body.
     */
    double getCelestialBodySurface();

    /**
     * @return name of the DGGS base polyhedron geometry
     */
    String getBasePolyhedron();

    /**
     * @return number of subdivision at each refinement level
     */
    int getRefinementRatio();

    /**
     * @return characteristics of the cell geometry refinement
     */
    List<RefinementStrategy> getRefinementStrategy();

    /**
     * List of characteristics that constraint the grid cells in this DGGS in decreasing order of priority.
     *
     * @return list, never null.
     */
    List<GridConstraints> getGridConstraints();

    /**
     * @return number of spatial dimensions
     */
    int getSpatialDimensions();

    /**
     * @return number of temporal dimensions
     */
    int getTemporalDimensions();

    /**
     * @return name of the possible geometry shapes in the DGGS
     */
    List<String> getZoneTypes();

    /**
     * @return alignement parameters of the base polyhedron compared to the CRS
     */
    PolyhedronParameters getParameters();
}
