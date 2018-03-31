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
 * Root node of a map.
 *
 * The map context contains all layers to display (given by the {@link #getComponents() group components})
 * and defines the {@linkplain #getAreaOfInterest() area of interest} which should be zoomed by default
 * when the map is rendered.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class MapContext extends MapGroup {
    /**
     * The area of interest.
     */
    private Envelope areaOfInterest;

    /**
     * Creates an initially empty map context.
     */
    public MapContext() {
    }

    /**
     * Returns the map area to show by default. This is not necessarily the
     * {@linkplain org.apache.sis.storage.DataSet#getEnvelope() envelope of data}
     * since one may want to zoom in a different spatio-temporal area.
     *
     * <p>The {@linkplain org.apache.sis.geometry.GeneralEnvelope#getCoordinateReferenceSystem() envelope CRS}
     * defines the map projection to use for rendering the map. It may be different than the CRS of the data.
     * The returned envelope may have {@linkplain org.apache.sis.geometry.GeneralEnvelope#isAllNaN() all its
     * coordinates set to NaN} if only the {@link CoordinateReferenceSystem} is specified.</p>
     *
     * @return map area to show by default, or {@code null} is unspecified.
     *
     * @see org.apache.sis.storage.DataSet#getEnvelope()
     */
    public Envelope getAreaOfInterest() {
        return areaOfInterest;
    }

    /**
     * Sets the map area to show by default.
     * The given envelope is not necessarily related to the data contained in the map context.
     * It may be wider, small and in a different {@link CoordinateReferenceSystem}.
     *
     * @param  aoi  new map area to show by default, or {@code null} is unspecified.
     */
    public void setAreaOfInterest(Envelope aoi) {
        areaOfInterest = aoi;
    }
}
