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
package org.apache.sis.internal.map;

import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A map context is the root node of a map.
 *
 * The map context contains all displayed layers like a MapGroup and
 * defines the area of interest which should be focused by default when
 * displayed.
 *
 * <p>
 * NOTE : this class is a first draft subject to modifications.
 * </p>
 *
 * @author Johann Sorel (Geomatys)
 * @since 1.0
 * @module
 */
public class MapContext extends MapGroup {

    private Envelope aoi;

    /**
     * Returns the map default area of interest.
     *
     * If the returned envelope is empty, at least the {@link CoordinateReferenceSystem}
     * defined should be used for displaying the map.
     *
     * @return map area of interest, may be null
     */
    public Envelope getAreaOfInterest() {
        return aoi;
    }

    /**
     * Set map default area of interest.
     *
     * The given envelope is unrelated to the data contained in the map context.
     * It may be wider, small and in a different {@link CoordinateReferenceSystem}.
     *
     * @param aoi map area of interest, may be null
     */
    public void setAreaOfInterest(Envelope aoi) {
        this.aoi = aoi;
    }

}
