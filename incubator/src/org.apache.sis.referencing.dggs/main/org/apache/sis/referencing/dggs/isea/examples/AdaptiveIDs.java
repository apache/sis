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
package org.apache.sis.referencing.dggs.isea.examples;

import org.apache.sis.referencing.dggs.isea.cells.GridCellIDType;
import org.apache.sis.referencing.dggs.isea.cells.GridCellMetaData;

/**
 * Computes the number of decimal places needed at a given resolution, depending on the type of cell ID.
 *
 * @author Franz-Benjamin Mocnik
 */
public class AdaptiveIDs {
    public static void main(String[] args) throws Exception {
        for (int resolution = 1; resolution <= 22; resolution++) {
            System.out.println("====== " + resolution);
            System.out.println(GridCellMetaData.getInstance().numberOfDecimalPlaces(resolution, GridCellIDType.NON_ADAPTIVE));
            System.out.println(GridCellMetaData.getInstance().numberOfDecimalPlaces(resolution, GridCellIDType.ADAPTIVE_UNIQUE));
            System.out.println(GridCellMetaData.getInstance().numberOfDecimalPlaces(resolution, GridCellIDType.ADAPTIVE_1_PERCENT));
        }
    }
}
