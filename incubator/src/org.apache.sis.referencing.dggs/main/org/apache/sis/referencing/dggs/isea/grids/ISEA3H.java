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
package org.apache.sis.referencing.dggs.isea.grids;

import org.apache.sis.referencing.dggs.isea.cells.GridCell;
import org.apache.sis.referencing.dggs.isea.cells.GridCellIDType;
import org.apache.sis.referencing.dggs.isea.generic.Tuple;
import org.apache.sis.referencing.dggs.isea.geo.WGS84;
import org.apache.sis.referencing.dggs.isea.geometry.FaceCoordinates;
import org.apache.sis.referencing.dggs.isea.geometry.GeoCoordinates;
import org.apache.sis.referencing.dggs.isea.projections.ISEAProjection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ISEA Aperture 3 Hexagon (ISEA3H) Discrete Global Grid System (DGGS)
 *
 * The ISEA3H grid is constructed by using the icosahedron Snyder equal-area (ISEA) projection to map the surface of the
 * globe to the icosahedron. Thereby, the orientation of the icosahedron is chosen such that the north and the south
 * poles are mapped to the edge midpoints of the icosahedron. The equator is thus mapped symmetrically. A grid
 * (aperture 3) is constructed on the icosahedron, and this grid is mapped back by the inverse projection to the globe.
 *
 * The cells of the grid are identified by the resolution and their center points.
 *
 * The ISEA3H has been proposed by:
 *
 * Kevin Sahr, Denis White, and A. Jon Kimerling: Geodesic Discrete Global Grid Systems. Cartography and Geographic
 * Information Science, 30(2), 121–134, 2003. doi:10.1559/152304003100011090
 *
 * @author Franz-Benjamin Mocnik
 */
public class ISEA3H {
    private static final double _precision = 1e-9;
    private final ISEAProjection _projection = new ISEAProjection();
    private final int _resolution; // resolution
    private final long _numberOfHexagonalCells;
    private final int _numberOfPentagonalCells = 12;
    private final double _l0; // length of the triangle base at resolution 1
    private final double _inverseSqrt3l0; // 1 / \sqrt{3} * l_0
    private final double _l; // length of the triangle base at the given resolution
    private final double _l2; // l / 2
    private final double _l6; // l / 6
    private final double _l23; // l * 2 / 3
    private final double _inverseSqrt3 = 1 / Math.sqrt(3); // 1 / \sqrt{3}
    private final double _inverseSqrt3l; // 1 / \sqrt{3} * l
    private final double _inverseSqrt3l2; // 1 / (2 \sqrt{3}) * l
    private final double _triangleA; // l_0 / 2 // half base
    private final double _triangleB; // 1/\sqrt{3} * l_0 // distance center point to tip
    private final double _triangleC; // 1/(2 \sqrt{3}) * l_0 // distance base to center point
    private final double _triangleBCA; // (_triangleB + _triangleC) / _triangleA
    private int _numberOfThreads = 8;

    public ISEA3H(int resolution) {
        this(resolution, true);
    }

    public ISEA3H(int resolution, boolean rotatedProjection) {
        if (rotatedProjection) this._projection.setOrientationSymmetricEquator();
        this._resolution = resolution;
        long numberOfHexagonalCells = 1;
        for (int i = 1; i < this._resolution; i++) numberOfHexagonalCells = 3 * numberOfHexagonalCells + 1;
        this._numberOfHexagonalCells = 20 * numberOfHexagonalCells;
        this._l0 = this._projection.lengthOfTriangleBase();
        this._inverseSqrt3l0 = this._inverseSqrt3 * this._l0;
        this._l = Math.pow(this._inverseSqrt3, this._resolution - 1) * this._l0;
        this._l2 = this._l / 2.;
        this._l6 = this._l / 6.;
        this._l23 = this._l * 2 / 3.;
        this._inverseSqrt3l = Math.pow(this._inverseSqrt3, this._resolution) * this._l0;
        this._inverseSqrt3l2 = this._inverseSqrt3l / 2.;
        this._triangleA = this._l0 / 2.;
        this._triangleB = this._inverseSqrt3l0;
        this._triangleC = this._inverseSqrt3l0 / 2.;
        this._triangleBCA = (this._triangleB + this._triangleC) / this._triangleA;
    }

    /**
     * Get the resolution.
     */
    public Integer getResolution() {
        return this._resolution;
    }

    /**
     * Set the number of threads.
     *
     * By default, 8 threads are used.
     *
     * @param n
     */
    public void setNumberOfThreads(int n) {
        this._numberOfThreads = n;
    }

    /**
     * Get the number of threads.
     *
     * @return
     */
    public int getNumberOfThreads() {
        return this._numberOfThreads;
    }

    /**
     * @return diameter of a hexagonal cell on the icosahedron, in kilometres
     */
    public double diameterOfHexagonalCellOnIcosahedron() {
        return this._l23;
    }

    /**
     * @return length of a side of a hexagonal cell on the icosahedron, in kilometres
     */
    public double lengthOfASideOfHexagonalCellOnIcosahedron() {
        return this._l / 3;
    }

    /**
     * @return lower bound for the length of a side of a hexagonal cell on the sphere, in kilometres
     */
    public double lowerBoundForLengthOfASideOfHexagonalCellOnSphere() {
        return this._projection.sphericalDistanceFromCenterToVerticesOnSphere() * WGS84.radiusAuthalic * 2 * Math.PI / (360 * Math.sqrt(Math.pow(3, this._resolution - 1) * 5));
    }

    /**
     * Returns the area of a hexagonal cell. The cells should all have the same area by construction, because the ISEA
     * projection is equal-area.
     *
     * @return area of a hexagonal cell, in square kilometres
     */
    public double areaOfAHexagonalCell() {
        return WGS84.areaOfEarth / (this._numberOfHexagonalCells + 5 / 6. * this._numberOfPentagonalCells);
    }

    /**
     * Returns the area of a pentagonal cell. The cells should all have the same area by construction, because the ISEA
     * projection is equal-area.
     *
     * @return area of a pentagonal cell, in in kilometres
     */
    public double areaOfAPentagonalCell() {
        return 5 / 6. * this.areaOfAHexagonalCell();
    }

    /**
     * @return number of hexagonal cells
     */
    public long numberOfHexagonalCells() {
        return this._numberOfHexagonalCells;
    }

    /**
     * @return number of pentagonal cells
     */
    public int numberOfPentagonalCells() {
        return this._numberOfPentagonalCells;
    }

    /**
     * Returns the grid cell for a given location
     *
     * @param lat latitude
     * @param lon longitude
     * @return corresponding grid cell
     * @throws Exception
     */
    public GridCell cellForLocation(double lat, double lon) throws Exception {
        return this.cellForLocation(new GeoCoordinates(lat, lon));
    }

    /**
     * Returns the grid cell for a given location
     *
     * @param c geographic coordinates
     * @return corresponding grid cell
     * @throws Exception
     */
    public GridCell cellForLocation(GeoCoordinates c) throws Exception {
        FaceCoordinates fc = this.cellForLocation(this._projection.sphereToIcosahedron(c));
        return this._newGridCell(this._projection.icosahedronToSphere(fc), fc);
    }

    /**
     * Returns the coordinates of the center of the corresponding grid cell for given coordinates in the face
     *
     * @param c face coordinates
     * @return face coordinates of the center of the corresponding grid cell
     * @throws Exception
     */
    public FaceCoordinates cellForLocation(FaceCoordinates c) {
        double x = (this._coordinatesNotSwapped()) ? c.getX() : c.getY();
        double y = (this._coordinatesNotSwapped()) ? c.getY() : c.getX();
        int nxCenter = (int) Math.round(x / this._l2);
        double xCenter = nxCenter * this._l2;
        if (Math.abs(x - xCenter) <= this._l6) {
            int nyCenter12 = (int) Math.round(((nxCenter % 2 == 0) ? y : y - this._inverseSqrt3l2) / this._inverseSqrt3l);
            double yCenter12 = nyCenter12 * this._inverseSqrt3l + ((nxCenter % 2 == 0) ? 0 : this._inverseSqrt3l2);
            return this._faceCoordinatesSwapByResolution(c.getFace(), xCenter, yCenter12);
        } else {
            int nyCenter1 = (int) Math.round(y / this._inverseSqrt3l);
            double yCenter1 = nyCenter1 * this._inverseSqrt3l;
            int nyCenter2 = (int) Math.round((y - this._inverseSqrt3l2) / this._inverseSqrt3l);
            double yCenter2 = nyCenter2 * this._inverseSqrt3l + this._inverseSqrt3l2;
            FaceCoordinates cCandidate1 = this._faceCoordinatesSwapByResolution(c.getFace(), xCenter, (nxCenter % 2 == 0) ? yCenter1 : yCenter2);
            FaceCoordinates cCandidate2 = this._faceCoordinatesSwapByResolution(c.getFace(), (x > xCenter) ? xCenter + this._l2 : xCenter - this._l2, (nxCenter % 2 != 0) ? yCenter1 : yCenter2);
            return (c.distanceTo(cCandidate1) < c.distanceTo(cCandidate2)) ? cCandidate1 : cCandidate2;
        }
    }

    /**
     * The following equality holds: cellForLocation = _getCoordinatesOfCenter . _integerForFaceCoordinates
     *
     * However, the method cellForLocation is defined separately to speed up computations.
     *
     * @param face face to compute the coordinates on
     * @param nx steps into the direction of the vertex of the hexagon
     * @param ny steps into the direction of the edge of the hexagon
     * @return coordinates on the face
     */
    private FaceCoordinates _getCoordinatesOfCenter(int face, int nx, int ny) {
        double x = nx * this._l2;
        double y = (ny + ((nx % 2 == 0) ? 0 : .5)) * this._inverseSqrt3l;
        return this._faceCoordinatesSwapByResolution(face, x, y);
    }

    private FaceCoordinates _getCoordinatesOfCenter2(int face, int nx, int ny, int orientation) {
        double x = nx * this._l2;
        double y = (ny - orientation * ((nx % 2 == 0) ? 0 : .5)) * this._inverseSqrt3l;
        return this._faceCoordinatesSwapByResolution(face, x, y);
    }

    private Tuple<Integer, Integer> _integerForFaceCoordinates(FaceCoordinates c) {
        double x = (this._coordinatesNotSwapped()) ? c.getX() : c.getY();
        double y = (this._coordinatesNotSwapped()) ? c.getY() : c.getX();
        int nxCenter = (int) Math.round(x / this._l2);
        double xCenter = nxCenter * this._l2;
        if (Math.abs(x - xCenter) <= this._l6) return new Tuple(nxCenter, (int) Math.round(((nxCenter % 2 == 0) ? y : y - this._inverseSqrt3l2) / this._inverseSqrt3l));
        else {
            int nyCenter1 = (int) Math.round(y / this._inverseSqrt3l);
            double yCenter1 = nyCenter1 * this._inverseSqrt3l;
            int nyCenter2 = (int) Math.round((y - this._inverseSqrt3l2) / this._inverseSqrt3l);
            double yCenter2 = nyCenter2 * this._inverseSqrt3l + this._inverseSqrt3l2;
            FaceCoordinates cCandidate1 = this._faceCoordinatesSwapByResolution(c.getFace(), xCenter, (nxCenter % 2 == 0) ? yCenter1 : yCenter2);
            FaceCoordinates cCandidate2 = this._faceCoordinatesSwapByResolution(c.getFace(), (x > xCenter) ? xCenter + this._l2 : xCenter - this._l2, (nxCenter % 2 != 0) ? yCenter1 : yCenter2);
            return (c.distanceTo(cCandidate1) < c.distanceTo(cCandidate2)) ? new Tuple(nxCenter, (nxCenter % 2 == 0) ? nyCenter1 : nyCenter2) : new Tuple((x > xCenter) ? nxCenter + 1 : nxCenter - 1, (nxCenter % 2 != 0) ? nyCenter1 : nyCenter2);
        }
    }

    /**
     * Returns the grid cell for the centroid of a given geometry
     *
     * @param g geometry
     * @return corresponding grid cell
     * @throws Exception
     */
    public GridCell cellForCentroid(Geometry g) throws Exception {
        Coordinate c = g.getCentroid().getCoordinate();
        return this.cellForLocation(c.y, c.x);
    }

    /**
     * Returns all cells.
     *
     * @return cells
     */
    public Collection<GridCell> cells() throws Exception {
        return this.cellsForBound(-90, 90, -180, 180);
    }

    /**
     * Returns cell IDs.
     *
     * Same as cells, apart that this method returns only IDs and is much more memory efficient.
     *
     * @return IDs of cells
     */
    public Collection<Long> cellIDs() throws Exception {
        return this.cellIDs(GridCellIDType.NON_ADAPTIVE);
    }
    public Collection<Long> cellIDs(GridCellIDType gridCellIDType) throws Exception {
        return this.cellIDsForBound(-90, 90, -180, 180, gridCellIDType);
    }

    /**
     * Save cell IDs to disk.
     *
     * For each face, one file is created. The cell IDs in the different files are not unique and non-unique IDs need to
     * be removed.
     *
     * @param file prefix of the files
     * @return IDs of cells
     */
    public void cellIDs(String file) throws Exception {
        this.cellIDs(file, GridCellIDType.NON_ADAPTIVE);
    }
    public void cellIDs(String file, GridCellIDType gridCellIDType) throws Exception {
        this._cells(new CellAggregatorByCellIDsToFile(file, gridCellIDType)).closeFile();
    }

    /**
     * Returns cells that are inside the bounds, or at least very near.
     *
     * Note that the result should, in fact, include all cells whose center points are inside the given bounds, but also
     * cells nearby. Observe that it can, however, not be guaranteed that all such cells are returned if the longitude
     * range is less than 360 degrees or the latitude range is less than 180 degrees.
     *
     * @param lat0
     * @param lat1
     * @param lon0
     * @param lon1
     * @return cells inside the bounds
     */
    public Collection<GridCell> cellsForBound(double lat0, double lat1, double lon0, double lon1) throws Exception {
        if (lat1 - lat0 >= 180 && lon1 - lon0 >= 360) return this._cells(new CellAggregatorByCells()).getCells();
        else return this._cellsForBound(new CellAggregatorByCells(), lat0, lat1, lon0, lon1).cellAggregator.getCells();
    }

    /**
     * Returns cell IDs that are inside the bounds, or at least very near.
     *
     * Same as cellsForBound, apart that this method returns only IDs and is much more memory efficient.
     *
     * @param lat0
     * @param lat1
     * @param lon0
     * @param lon1
     * @return IDs of cells inside the bounds
     */
    public Collection<Long> cellIDsForBound(double lat0, double lat1, double lon0, double lon1) throws Exception {
        return this.cellIDsForBound(lat0, lat1, lon0, lon1, GridCellIDType.NON_ADAPTIVE);
    }
    public Collection<Long> cellIDsForBound(double lat0, double lat1, double lon0, double lon1, GridCellIDType gridCellIDType) throws Exception {
        if (lat1 - lat0 >= 180 && lon1 - lon0 >= 360) return this._cells(new CellAggregatorByCellIDs(gridCellIDType)).getCellIDs();
        else return this._cellsForBound(new CellAggregatorByCellIDs(gridCellIDType), lat0, lat1, lon0, lon1).cellAggregator.getCellIDs();
    }

    private <T extends CellAggregator> ResultCellForBound<T> _cellsForBound(T ca, double lat0, double lat1, double lon0, double lon1) throws Exception {
        // compute center of bounding box
        double lat = (lat0 + lat1) / 2.;
        double lon = (lon0 + lon1 + ((lon0 <= lon1) ? 0 : 360)) / 2.;
        if (lon > 360) lon -= 360;
        FaceCoordinates fc = this._projection.sphereToIcosahedron(new GeoCoordinates(lat, lon));
        // compute
        return _cellsForBound(new ResultCellForBound<>(ca), fc, lat0, lat1, lon0, lon1);
    }

    private <T extends CellAggregator> T _cells(T ca) throws Exception {
        ISEA3H t = this;
        ExecutorService executor = Executors.newFixedThreadPool(this._numberOfThreads);
        List<Future<T>> futureList = new ArrayList<>();
        for (int face = 0; face < this._projection.numberOfFaces(); face++) {
            final int f = face;
            futureList.add(executor.submit(new Callable<T>() {
                public T call() throws Exception {
                    return t._cellsForFace(new ResultCellForBound<T>((T) ca.cloneEmpty()), f).cellAggregator;
                }
            }));
        }
        for (Future<T> future : futureList) ca.addAll(future.get());
        executor.shutdown();
        return ca;
    }

    private <T extends CellAggregator> ResultCellForBound<T> _cellsForBound(ResultCellForBound<T> result, FaceCoordinates fcStart, double lat0, double lat1, double lon0, double lon1) throws Exception {
        // if fcStart is already in result, skip the computation
        if (result.visitedCells.contains(fcStart.getFace())) return result;
        result.visitedCells.add(fcStart.getFace());

        // prepare dNs
        List<Tuple<Integer, Integer>> dNs = new ArrayList<>();
        dNs.add(new Tuple(1, 1));
        dNs.add(new Tuple(-1, 1));
        dNs.add(new Tuple(1, -1));
        dNs.add(new Tuple(-1, -1));

        // compute starting coordinates
        Tuple<Integer, Integer> fcn = this._integerForFaceCoordinates(fcStart);
        if (!this._isCoordinatesInFace(this._getCoordinatesOfCenter(fcStart.getFace(), fcn._1, fcn._2))) {
            if (this._isCoordinatesInFace(this._getCoordinatesOfCenter(fcStart.getFace(), fcn._1 - 1, fcn._2))) fcn = new Tuple(fcn._1 - 1, fcn._2);
            else if (this._isCoordinatesInFace(this._getCoordinatesOfCenter(fcStart.getFace(), fcn._1 + 1, fcn._2))) fcn = new Tuple(fcn._1 + 1, fcn._2);
            else if (this._isCoordinatesInFace(this._getCoordinatesOfCenter(fcStart.getFace(), fcn._1, fcn._2 - 1))) fcn = new Tuple(fcn._1, fcn._2 - 1);
            else if (this._isCoordinatesInFace(this._getCoordinatesOfCenter(fcStart.getFace(), fcn._1, fcn._2 + 1))) fcn = new Tuple(fcn._1, fcn._2 + 1);
            else if (this._isCoordinatesInFace(this._getCoordinatesOfCenter(fcStart.getFace(), fcn._1 - 1, fcn._2 - 1))) fcn = new Tuple(fcn._1 -1, fcn._2 - 1);
            else if (this._isCoordinatesInFace(this._getCoordinatesOfCenter(fcStart.getFace(), fcn._1 + 1, fcn._2 - 1))) fcn = new Tuple(fcn._1 + 1, fcn._2 - 1);
            else if (this._isCoordinatesInFace(this._getCoordinatesOfCenter(fcStart.getFace(), fcn._1 - 1, fcn._2 + 1))) fcn = new Tuple(fcn._1 - 1, fcn._2 + 1);
            else if (this._isCoordinatesInFace(this._getCoordinatesOfCenter(fcStart.getFace(), fcn._1 + 1, fcn._2 + 1))) fcn = new Tuple(fcn._1 + 1, fcn._2 + 1);
        }

        // collect cells
        int face = fcStart.getFace();
        for (Tuple<Integer, Integer> dN : dNs) {
            FaceCoordinates fc;
            GeoCoordinates gc;
            Set<Integer> success = new HashSet();
            Set<Integer> successLast;
            boolean hasFoundInside;
            boolean hasFoundOutsideX = false;
            boolean hasFoundOutsideY = false;
            Map<Integer, FaceCoordinates> faceTodo = new HashMap();
            int nx = fcn._1 - dN._1 + ((dN._1 < 0) ? dN._1 : 0);
            while (true) {
                nx += dN._1;
                int ny = fcn._2 - dN._2;
                successLast = success;
                Integer maxMinValue = (!successLast.isEmpty()) ? ((dN._2 >= 0) ? Collections.max(successLast) : Collections.min(successLast)) : null;
                success = new HashSet<>();
                hasFoundInside = false;
                while (true) {
                    ny += dN._2;
                    fc = this._getCoordinatesOfCenter(face, nx, ny);
                    gc = this._projection.icosahedronToSphere(fc);
                    if (this._isInside(gc, lat0, lat1, lon0, lon1)) hasFoundInside = true;
                    else if (hasFoundInside || maxMinValue == null || ((dN._2 >= 0) ? ny > maxMinValue : ny < maxMinValue)) break;
                    else continue;
                    if (this._isCoordinatesInFace(fc)) {
                        success.add(ny);
                        result.cellAggregator.add(face, this._newGridCell(gc, fc));
                    } else {
                        if ((!hasFoundOutsideX && ny != fcn._2) || (!hasFoundOutsideY && ny == fcn._2)) {
                            if (ny != fcn._2) hasFoundOutsideX = true;
                            else hasFoundOutsideY = true;
                            FaceCoordinates fc2 = this._projection.sphereToIcosahedron(gc);
                            if (faceTodo.containsKey(fc2.getFace())) faceTodo.put(fc2.getFace(), fc2);
                            else {
                                int sizeBefore = result.cellAggregator.size();
                                result = this._cellsForBound(result, fc2, lat0, lat1, lon0, lon1);
                                if (result.cellAggregator.size() != sizeBefore) faceTodo.put(fc2.getFace(), null);
                            }
                        }
                        if (!success.isEmpty()) break;
                    }
                }
                if (success.isEmpty()) break;
            }
            for (Map.Entry<Integer, FaceCoordinates> e : faceTodo.entrySet()) if (e.getValue() != null && !result.cellAggregator.contains(this.cellForLocation(this._projection.icosahedronToSphere(e.getValue())))) result = this._cellsForBound(result, e.getValue(), lat0, lat1, lon0, lon1);
        }

        return result;
    }

    private <T extends CellAggregator> ResultCellForBound<T> _cellsForFace(ResultCellForBound<T> result, int face) throws Exception {
        int d = this._projection.faceOrientation(face);
        boolean notSwapped = (this._coordinatesNotSwapped());

        // compute
        FaceCoordinates fc;
        GeoCoordinates gc;
        int nyMax = (int) Math.round((notSwapped ? Math.pow(3, (this._resolution - 1) / 2.) : 2 * Math.pow(3, (this._resolution - 2) / 2.)));
        int nyMin = -(int) (notSwapped ? (nyMax - 1) / 2. : nyMax / 2.);
        int nxMin = 0;
        int nxMax = 0;
        int counter = 0;
        for (int ny = nyMax; ny >= nyMin; ny--) {
            if (notSwapped) {
                if (counter == 1) {
                    nxMax++;
                    nxMin = -nxMax;
                }
                if (counter == 3) {
                    nxMax++;
                    nxMin = -nxMax;
                    counter = 0;
                }
                counter++;
            } else {
                nxMin = (d > 0) ? -(int)Math.floor((nyMax - ny) / 2.) : -(int)Math.ceil((nyMax - ny) / 2.);
                nxMax = (d > 0) ? (int)Math.ceil((nyMax - ny) / 2.) : (int)Math.floor((nyMax - ny) / 2.);
            }
            for (int nx = nxMin; nx <= nxMax; nx++) {
                fc = this._getCoordinatesOfCenter2(face, notSwapped ? nx : d * ny, notSwapped ? d * ny : nx, d);
                gc = this._projection.icosahedronToSphere(fc);
                result.cellAggregator.add(face, this._newGridCell(gc, fc));
            }
        }

        return result;
    }

    private class ResultCellForBound<T extends CellAggregator> {
        public final T cellAggregator;
        public final Set<Integer> visitedCells = new HashSet();

        public ResultCellForBound(T cellAggregator) {
            this.cellAggregator = cellAggregator;
        }
    }

    private interface CellAggregator<T> {
        public abstract CellAggregator<T> cloneEmpty();
        public abstract void add(int face, GridCell c) throws Exception;
        public abstract void addAll(T ca);
        public abstract int size();
        public abstract boolean contains(GridCell c);
    }

    private class CellAggregatorByCells implements CellAggregator<CellAggregatorByCells> {
        private Set<GridCell> _cells = new HashSet();

        @Override
        public CellAggregator<CellAggregatorByCells> cloneEmpty() {
            return new CellAggregatorByCells();
        }

        @Override
        public void add(int face, GridCell c) {
            this._cells.add(c);
        }

        @Override
        public void addAll(CellAggregatorByCells ca) {
            this._cells.addAll(ca.getCells());
        }

        @Override
        public int size() {
            return this._cells.size();
        }

        @Override
        public boolean contains(GridCell c) {
            return this._cells.contains(c);
        }

        public Set<GridCell> getCells() {
            return this._cells;
        }
    }

    private class CellAggregatorByCellIDs implements CellAggregator<CellAggregatorByCellIDs> {
        private Set<Long> _cells = new HashSet();
        private GridCellIDType _gridCellIDType;

        public CellAggregatorByCellIDs(GridCellIDType gridCellIDType) {
            this._gridCellIDType = gridCellIDType;
        }

        @Override
        public CellAggregator<CellAggregatorByCellIDs> cloneEmpty() {
            return new CellAggregatorByCellIDs(this._gridCellIDType);
        }

        @Override
        public void add(int face, GridCell c) {
            this._cells.add(c.getID(this._gridCellIDType));
        }

        @Override
        public void addAll(CellAggregatorByCellIDs ca) {
            this._cells.addAll(ca.getCellIDs());
        }

        @Override
        public int size() {
            return this._cells.size();
        }

        @Override
        public boolean contains(GridCell c) {
            return this._cells.contains(c.getID(this._gridCellIDType));
        }

        public Set<Long> getCellIDs() {
            return this._cells;
        }
    }

    private class CellAggregatorByCellIDsToFile implements CellAggregator<CellAggregatorByCellIDsToFile> {
        private ArrayList<Long> _cells = new ArrayList<>();
        private List<CellAggregatorByCellIDsToFile> _caList = new ArrayList();
        private final String _filename;
        private GridCellIDType _gridCellIDType;
        private Integer _face = null;
        private int _chunk = 0;
        private static final int _chunkSize = 10000000;

        public CellAggregatorByCellIDsToFile(String filename, GridCellIDType gridCellIDType) {
            this._filename = filename;
            this._gridCellIDType = gridCellIDType;
        }

        @Override
        public CellAggregator<CellAggregatorByCellIDsToFile> cloneEmpty() {
            return new CellAggregatorByCellIDsToFile(this._filename, this._gridCellIDType);
        }

        @Override
        public void add(int face, GridCell c) throws IOException {
            this._face = face;
            this._cells.add(c.getID(this._gridCellIDType));
            if (this._cells.size() >= this._chunkSize) this._writeChunkToFile();
        }

        @Override
        public void addAll(CellAggregatorByCellIDsToFile ca) {
            this._caList.add(ca);
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean contains(GridCell c) {
            return false;
        }

        private void _writeChunkToFile() throws IOException {
            Collections.sort(this._cells);
            File file = new File(this._filename + ".face" + this._face + "." + this._chunk);
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))){
                for (Long a : this._cells) {
                    bufferedWriter.append(a.toString());
                    bufferedWriter.newLine();
                }
            }
            this._chunk++;
            this._cells = new ArrayList<>();
        }

        public void closeFile() throws IOException {
            if (this._cells.size() > 0) this._writeChunkToFile();
            for (CellAggregatorByCellIDsToFile ca : this._caList) ca.closeFile();
        }
    }

    private boolean _isInside(GeoCoordinates c, double lat0, double lat1, double lon0, double lon1) {
        boolean bLat = lat0 <= c.getLat() && c.getLat() <= lat1;
        boolean bLon = (lon0 <= lon1) ? lon0 <= c.getLon() && c.getLon() <= lon1 : lon0 < c.getLon() || c.getLon() < lon1;
        return bLat && bLon;
    }

    private GridCell _newGridCell(GeoCoordinates gc, FaceCoordinates fc) throws Exception {
        int d = this._projection.faceOrientation(fc);
        boolean isPentagon = (Math.abs(Math.abs(fc.getX()) - this._triangleA) < ISEA3H._precision && Math.abs(fc.getY() + d * this._triangleC) < ISEA3H._precision) || (Math.abs(fc.getX()) < ISEA3H._precision && Math.abs(fc.getY() - d * this._triangleB) < ISEA3H._precision);
        return new GridCell(this._resolution, gc, isPentagon);
    }

    /**
     * Returns a buffer for a bounding box of geographic coordinates that needs to be considered in order to ensure
     * that all grid cells, whose center point is within the given bounding box, are contained in the bounding box or
     * the buffer
     *
     * @param lat0
     * @param lat1
     * @param lon0
     * @param lon1
     * @return buffer
     * @throws Exception
     */
    public Double bufferEstimator(double lat0, double lat1, double lon0, double lon1) throws Exception {
        lat0 = Math.max(lat0, -90);
        lat1 = Math.min(lat1, 90);
        if (lon1 - lon0 >= 360) {
            lon0 = -180;
            lon1 = 180;
        }
        FaceCoordinates southWest = this._projection.sphereToIcosahedron(new GeoCoordinates(lat0, lon0));
        FaceCoordinates northEast = this._projection.sphereToIcosahedron(new GeoCoordinates(lat1, lon1));
        GeoCoordinates southWest2 = this._projection.icosahedronToSphere(new FaceCoordinates(southWest.getFace(), southWest.getX() - this.diameterOfHexagonalCellOnIcosahedron() / 2, southWest.getY() - this.diameterOfHexagonalCellOnIcosahedron() / 2));
        GeoCoordinates northEast2 = this._projection.icosahedronToSphere(new FaceCoordinates(northEast.getFace(), northEast.getX() - this.diameterOfHexagonalCellOnIcosahedron() / 2, northEast.getY() - this.diameterOfHexagonalCellOnIcosahedron() / 2));
        List<Double> l = new ArrayList<>();
        l.add(Math.abs(southWest2.getLat() - lat0));
        l.add(Math.abs(southWest2.getLon() - lon0));
        l.add(Math.abs(northEast2.getLat() - lat1));
        l.add(Math.abs(northEast2.getLon() - lon1));
        Optional<Double> result = l.stream().max(Double::compareTo);
        return (result.isPresent()) ? result.get() : null;
    }

    private boolean _isCoordinatesInFace(FaceCoordinates c) {
        int d = this._projection.faceOrientation(c);

        // test whether coordinate is left of the triangle, right of the triangle, or below the triangle
        if (d * c.getY() > c.getX() * this._triangleBCA + this._triangleB + ISEA3H._precision) return false;
        if (d * c.getY() > -c.getX() * this._triangleBCA + this._triangleB + ISEA3H._precision) return false;
        if (d * c.getY() < -this._triangleC - ISEA3H._precision) return false;

        return true;
    }

    private FaceCoordinates _faceCoordinatesSwapByResolution(int face, double x, double y) {
        return new FaceCoordinates(face, this._coordinatesNotSwapped() ? x : y, this._coordinatesNotSwapped() ? y : x);
    }

    private boolean _coordinatesNotSwapped() {
        return this._resolution % 2 != 0;
    }
}
