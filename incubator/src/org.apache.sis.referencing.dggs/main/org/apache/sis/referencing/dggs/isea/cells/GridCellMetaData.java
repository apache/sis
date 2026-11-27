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
package org.apache.sis.referencing.dggs.isea.cells;

import org.apache.sis.referencing.dggs.isea.geo.WGS84;
import org.apache.sis.referencing.dggs.isea.grids.ISEA3H;

import java.util.HashMap;
import java.util.Map;

/**
 * Meta data for a grid cell
 *
 * @author Franz-Benjamin Mocnik
 */public class GridCellMetaData {
    private static GridCellMetaData _gridCellMetaData = new GridCellMetaData();
    private static final int _maxNumberOfDecimalPlaces = 6;
    private Map<Integer, Integer> _numberOfDecimalPlaces = new HashMap();

    private GridCellMetaData() {}

    /**
     * Get an instance of this (singleton) class
     *
     * @return singleton class
     */
    public static GridCellMetaData getInstance() {
        return GridCellMetaData._gridCellMetaData;
    }

    /**
     * Returns the number of decimal places to be used for the ID for a given resolution and a given ID type
     *
     * @param resolution
     * @param gridCellIDType
     * @return number of decimal places
     */
    public int numberOfDecimalPlaces(int resolution, GridCellIDType gridCellIDType) {
        if (gridCellIDType == GridCellIDType.NON_ADAPTIVE) return this._maxNumberOfDecimalPlaces;
        int nodp;
        if (this._numberOfDecimalPlaces.containsKey(resolution)) nodp = this._numberOfDecimalPlaces.get(resolution);
        else {
            double distBetweenCells = 2 * (new ISEA3H(resolution)).lowerBoundForLengthOfASideOfHexagonalCellOnSphere();
            nodp = (int)Math.ceil(-Math.log10(distBetweenCells / (2 * Math.PI * WGS84.radiusAuthalic / 360)));
            this._numberOfDecimalPlaces.put(resolution, nodp);
        }
        switch (gridCellIDType) {
            case ADAPTIVE_UNIQUE:
                return Math.max(Math.min(nodp, this._maxNumberOfDecimalPlaces), 0);
            case ADAPTIVE_1_PERCENT:
                return Math.max(Math.min(nodp + 2, this._maxNumberOfDecimalPlaces), 0);
            default:
                return this._maxNumberOfDecimalPlaces;
        }
    }
}
