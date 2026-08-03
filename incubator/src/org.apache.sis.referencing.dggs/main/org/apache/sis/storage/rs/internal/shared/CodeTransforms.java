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
package org.apache.sis.storage.rs.internal.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.rs.Code;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.rs.CodeTransform;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CodeTransforms {

    private CodeTransforms(){}

    public static CodeTransform toTransform(GridGeometry gg) {
        return new Grid(gg);
    }

    public static CodeTransform toTransform(ReferenceSystem rs) {
        return new Undefined(rs);
    }

    public static CodeTransform toTransform(ReferenceSystem rs, List<?> zids) {
        return new Listed(rs, zids);
    }

    public static CodeTransform compound(CodeTransform ... trss) {
        if (trss == null || trss.length == 0) return null;
        if (trss.length == 1) return trss[0];
        SubTransform rgt = new Compound((SubTransform)trss[0], (SubTransform)trss[1]);
        for (int i = 2; i < trss.length; i++) {
            rgt = new Compound(rgt, (SubTransform) trss[2]);
        }
        return rgt;
    }

    /**
     * Try to extract a crs part from grid geometry.
     *
     * @param base
     * @param crs
     * @return
     * @throws FactoryException
     */
    public static GridGeometry slice(GridGeometry base, CoordinateReferenceSystem crs) throws FactoryException {
        final List<SingleCRS> singles = (List) ReferenceSystems.getSingleComponents(base.getCoordinateReferenceSystem(), true);
        int idx = 0;
        for (SingleCRS s : singles) {
            if (s == crs) {
                break;
            }
            idx += s.getCoordinateSystem().getDimension();
        }
        if (idx == base.getDimension()) {
            throw new FactoryException("Slice crs not found");
        }

        final MathTransform gridToCRS = base.getGridToCRS(PixelInCell.CELL_CENTER);
        final TransformSeparator ts = new TransformSeparator(gridToCRS);
        ts.addTargetDimensionRange(idx, idx+crs.getCoordinateSystem().getDimension());
        final MathTransform trs = ts.separate();
        final int[] sourceDimensions = ts.getSourceDimensions();

        return base.selectDimensions(sourceDimensions);
    }


    private static class Undefined extends SubTransform {

        final ReferenceSystem rs;

        public Undefined(ReferenceSystem rs) {
            this.rs = rs;
        }

        @Override
        public void toAddress(int[] gridPosition, Object[] location, int offset) throws TransformException {
            throw new TransformException("Not supported.");
        }

        @Override
        public void toGrid(Object[] location, int[] gridPosition, int offset) throws TransformException {
            throw new TransformException("Not supported.");
        }

        @Override
        public ReferenceSystem getRS() {
            return rs;
        }

        @Override
        public int getDimension() {
            return ReferenceSystems.getDimension(rs);
        }

        @Override
        public CodeTransform split(int offset, int size) {
            if (offset == 0 && size == getDimension()) return this;
            throw new IllegalArgumentException("Can not split transform at offset " + offset +" with size " + size);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 11 * hash + Objects.hashCode(this.rs);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Undefined other = (Undefined) obj;
            return Objects.equals(this.rs, other.rs);
        }
    }
    public static class Grid extends SubTransform {

        final GridGeometry grid;
        final MathTransform gridToCRS;
        MathTransform crsToGrid;
        final int dimension;
        private Double singleOrigin;

        public Grid(GridGeometry grid) {
            this.grid = grid;
            this.gridToCRS = grid.getGridToCRS(PixelInCell.CELL_CENTER);
            this.dimension = grid.getDimension();

            //check if we are dealing with a single slice with a NaN scale
            if (dimension == 1 && grid.getExtent().getSize(0) == 1) {
                final Matrix matrix = MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER));
                if (matrix != null && Double.isNaN(matrix.getElement(0, 0))) {
                    final int lastColumn = matrix.getNumCol() - 1;
                    singleOrigin = matrix.getElement(0, lastColumn);
                }
            }

        }

        public GridGeometry getGrid() {
            return grid;
        }

        @Override
        public ReferenceSystem getRS() {
            return grid.isDefined(GridGeometry.CRS) ? grid.getCoordinateReferenceSystem() : null;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public void toAddress(int[] gridPosition, Object[] location, int offset) throws TransformException {
            if (singleOrigin != null && gridPosition[offset] == 0) {
                location[offset] = singleOrigin;
                return;
            }
            final double[] gp = new double[dimension];
            for (int i = 0; i < dimension; i++) gp[i] = gridPosition[offset+i];
            gridToCRS.transform(gp, 0, gp, 0, 1);
            for (int i = 0; i < dimension; i++) location[offset+i] = gp[i];
        }

        @Override
        public void toGrid(Object[] location, int[] gridPosition, int offset) throws TransformException {
            if (singleOrigin != null) {
                gridPosition[offset] = 0;
                return;
            }
            if (crsToGrid == null) {
                //no synchronisation here, in worse case it will be computed a few times
                // but the result will always be the same
                this.crsToGrid = this.gridToCRS.inverse();
            }

            final double[] gp = new double[dimension];
            for(int i = 0; i < dimension; i++) gp[i] = (double) location[offset+i];
            crsToGrid.transform(gp, 0, gp, 0, 1);
            for(int i = 0; i < dimension; i++) gridPosition[offset+i] = (int) gp[i];
        }

        @Override
        public CodeTransform split(int offset, int size) {
            if (offset == 0 && size == dimension) return this;

            final int[] selection = new int[size];
            for (int i = 0; i < size; i++) selection[i] = offset + i;

            final TransformSeparator ts = new TransformSeparator(grid.getGridToCRS(PixelInCell.CELL_CORNER));
            ts.addSourceDimensions(selection);
            final MathTransform trs;
            try {
                trs = ts.separate();
            } catch (FactoryException ex) {
                throw new IllegalArgumentException("Can not split transform at offset " + offset +" with size " + size);
            }

            int idx = 0;
            int crsSize = 0;
            final List<ReferenceSystem> singleComponents = ReferenceSystems.getSingleComponents(grid.getCoordinateReferenceSystem(), true);
            final List<CoordinateReferenceSystem> toAgg = new ArrayList<>();
            for (ReferenceSystem rs : singleComponents) {
                CoordinateReferenceSystem crs = (CoordinateReferenceSystem) rs;
                if (idx >= offset) {
                    toAgg.add(crs);
                    crsSize += crs.getCoordinateSystem().getDimension();
                    if (crsSize >= size) break;
                }
                idx += crs.getCoordinateSystem().getDimension();
            }
            if (crsSize != size) throw new IllegalArgumentException("Can not split transform at offset " + offset +" with size " + size);

            final CoordinateReferenceSystem sliceCrs;
            try {
                sliceCrs = CRS.compound(toAgg.toArray(CoordinateReferenceSystem[]::new));
            } catch (FactoryException ex) {
                throw new IllegalArgumentException("Can not split transform at offset " + offset +" with size " + size, ex);
            }
            final GridExtent ext = grid.getExtent().selectDimensions(selection);
            final GridGeometry slice = new GridGeometry(ext, PixelInCell.CELL_CORNER, trs, sliceCrs);
            return new Grid(slice);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Grid other = (Grid) obj;
            return Objects.equals(this.grid, other.grid);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 37 * hash + Objects.hashCode(this.grid);
            return hash;
        }
    }

    public static class Listed extends SubTransform {

        final ReferenceSystem rs;
        final List<?> zids;
        private final Map<Object,Integer> index = new HashMap<>();

        public Listed(ReferenceSystem rs, List<?> zids) {
            this.rs = rs;
            this.zids = zids;
            for (int i = 0, n = zids.size(); i < n; i++) {
                index.put(zids.get(i), i);
            }
        }

        public List<?> getList() {
            return zids;
        }

        @Override
        public ReferenceSystem getRS() {
            return rs;
        }

        @Override
        public int getDimension() {
            return 1;
        }

        @Override
        public Code toCode(int[] gridPosition) throws TransformException {
            return new Code(rs, new Object[]{zids.get(gridPosition[0])});
        }

        @Override
        public int[] toGrid(Code location) throws TransformException {
            final Integer i = index.get(location.getOrdinate(0));
            if (i == null) throw new TransformException("Location code outside this grid : " + location.getOrdinate(0));
            return new int[]{i};
        }

        @Override
        public void toAddress(int[] gridPosition, Object[] location, int offset) throws TransformException {
            location[offset] = zids.get(gridPosition[offset]);
        }

        @Override
        public void toGrid(Object[] location, int[] gridPosition, int offset) throws TransformException {
            final Integer i = index.get(location[offset]);
            if (i == null) throw new TransformException("Location code outside this grid : " + location[offset]);
            gridPosition[offset] = i;
        }

        @Override
        public CodeTransform split(int offset, int size) {
            if (offset == 0 && size == 1) return this;
            throw new IllegalArgumentException("Can not split transform at offset " + offset +" with size " + size);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.rs);
            hash = 89 * hash + Objects.hashCode(this.zids);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Listed other = (Listed) obj;
            if (!Objects.equals(this.rs, other.rs)) {
                return false;
            }
            return Objects.equals(this.zids, other.zids);
        }

    }

    private static class Compound extends SubTransform {

        final SubTransform trs1;
        final SubTransform trs2;
        private final int dimension1;
        private final int dimension;
        final ReferenceSystem rs;

        private Compound(SubTransform trs1, SubTransform trs2) {
            this.trs1 = trs1;
            this.trs2 = trs2;
            dimension1 = trs1.getDimension();
            dimension = dimension1 + trs2.getDimension();
            rs = ReferenceSystems.createCompound(trs1.getRS(), trs2.getRS());
        }

        @Override
        public ReferenceSystem getRS() {
            return rs;
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public void toAddress(int[] gridPosition, Object[] location, int offset) throws TransformException {
            trs1.toAddress(gridPosition, location, offset);
            trs2.toAddress(gridPosition, location, offset+trs1.getDimension());
        }

        @Override
        public void toGrid(Object[] location, int[] gridPosition, int offset) throws TransformException {
            trs1.toGrid(location, gridPosition, offset);
            trs2.toGrid(location, gridPosition, offset+trs1.getDimension());
        }

        @Override
        public CodeTransform split(int offset, int size) {
            if (offset == 0 && size == dimension) {
                return this;
            } else if (offset < dimension1) {
                return trs1.split(offset, size);
            } else {
                return trs2.split(offset-dimension1, size);
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.trs1);
            hash = 89 * hash + Objects.hashCode(this.trs2);
            hash = 89 * hash + Objects.hashCode(this.rs);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Compound other = (Compound) obj;
            if (!Objects.equals(this.trs1, other.trs1)) {
                return false;
            }
            if (!Objects.equals(this.trs2, other.trs2)) {
                return false;
            }
            return Objects.equals(this.rs, other.rs);
        }
    }


}
