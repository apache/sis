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

import org.apache.sis.referencing.dggs.isea.cells.GridCell;
import org.apache.sis.referencing.dggs.isea.geometry.FaceCoordinates;
import org.apache.sis.referencing.dggs.isea.geometry.GeoCoordinates;
import org.apache.sis.referencing.dggs.isea.grids.ISEA3H;
import org.apache.sis.referencing.dggs.isea.projections.ISEAProjection;

import java.util.Collection;

/**
 * Demonstrates the use of the projection from the sphere to the icosahedron and back, as well as the use of the ISEA3H
 * grid.
 *
 * @author Franz-Benjamin Mocnik
 */
public class Example {
    public static void main(String[] args) throws Exception {
        // PROJECTION
        ISEAProjection p = new ISEAProjection();

        // project coordinates from the sphere to the icosahedron and back
        for (int i = 0; i < 1; i++) {
            GeoCoordinates c = new GeoCoordinates(Math.random() * 180 - 90, Math.random() * 360);
            FaceCoordinates c2 = p.sphereToIcosahedron(c);
            GeoCoordinates c3 = p.icosahedronToSphere(c2);
            System.out.println(c);
            System.out.println(c2);
            System.out.println(c3);
            System.out.println("------");
        }

        // GRID
        ISEA3H g = new ISEA3H(14);

        // print properties of the grid
        System.out.format("number of hexagon cells: %d%n", g.numberOfHexagonalCells());
        System.out.format("number of pentagon cells: %d%n", g.numberOfPentagonalCells());
        System.out.format("diameter of a hexagon cell: %f%n", g.diameterOfHexagonalCellOnIcosahedron());
        System.out.format("area of a hexagon cell: %f%n", g.areaOfAHexagonalCell());
        System.out.println("------");

        // get cells in given bounds
        Collection<GridCell> cells = g.cellsForBound(41, 42, 6, 7);
        System.out.println(cells.size());
        System.out.println("------");

        // determine cell for geographic coordinates
        for (int i = 0; i < 1; i++) {
            double lat = Math.random() * 180 - 90;
            double lon = Math.random() * 360;
            GridCell c = g.cellForLocation(lat, lon);
            System.out.println(c);
            System.out.println("------");
        }
    }
}
