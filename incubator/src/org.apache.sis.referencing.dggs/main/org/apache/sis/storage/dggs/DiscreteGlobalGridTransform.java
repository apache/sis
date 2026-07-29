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
package org.apache.sis.storage.dggs;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.rs.Code;
import org.apache.sis.storage.rs.CodeTransform;
import org.apache.sis.storage.rs.internal.shared.SubTransform;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class DiscreteGlobalGridTransform extends SubTransform {

    public static DiscreteGlobalGridTransform.Unstructured unstructured(DiscreteGlobalGridReferenceSystem dggrs, Object[] zoneIds) {
        return new Unstructured(dggrs, zoneIds);
    }

    public static DiscreteGlobalGridTransform.SubZoneTransform subZone(DiscreteGlobalGridReferenceSystem dggrs, Object baseZoneId, Integer relativeDepth) {
        return new SubZoneTransform(dggrs, baseZoneId, relativeDepth);
    }

    public static DiscreteGlobalGridTransform.SubZoneTransforms subZones(DiscreteGlobalGridReferenceSystem dggrs, Object[] baseZoneIds, Integer relativeDepth) {
        final SubZoneTransform[] subZones = new SubZoneTransform[baseZoneIds.length];
        for (int i = 0; i < subZones.length; i++) {
            subZones[i] = new SubZoneTransform(dggrs, baseZoneIds[i], relativeDepth);
        }
        return new SubZoneTransforms(subZones);
    }


    protected final DiscreteGlobalGridReferenceSystem dggrs;
    protected final Object[] baseZoneIds;
    protected final Integer relativeDepth;

    public DiscreteGlobalGridTransform(DiscreteGlobalGridReferenceSystem dggrs, Object[] baseZoneIds, Integer relativeDepth) {
        this.dggrs = dggrs;
        this.baseZoneIds = baseZoneIds;
        this.relativeDepth = relativeDepth;
    }

    @Override
    public DiscreteGlobalGridReferenceSystem getRS() {
        return dggrs;
    }


    public abstract GridExtent getExtent();

    @Override
    public int getDimension() {
        return 1;
    }

    /**
     * List of zones selected in the geometry.
     *
     * @return List of zone identifiers, never null
     */
    public abstract List<Object> getZoneIds() throws TransformException;

    /**
     * @return can be null
     */
    public Object[] getBaseZoneIds() {
        return baseZoneIds;
    }

    /**
     * @return can be null
     */
    public Integer getRelativeDepth() {
        return relativeDepth;
    }

    @Override
    public CodeTransform split(int offset, int size) {
        if (offset == 0 && size == 1) return this;
        throw new IllegalArgumentException("Can not split transform at offset " + offset +" with size " + size);
    }

    public static class Unstructured extends DiscreteGlobalGridTransform {

        private final Object[] zids;
        private final Map<Object,Integer> index = new HashMap<>();
        private final GridExtent extent;

        public Unstructured(DiscreteGlobalGridReferenceSystem dggrs, Object[] zoneIds) {
            super(dggrs, null, null);
            this.zids = zoneIds;
            for (int i = 0; i < zids.length; i++) {
                index.put(zids[i], i);
            }

            this.extent = new GridExtent(null, 0, zids.length, false);
        }

        @Override
        public GridExtent getExtent() {
            return extent;
        }

        @Override
        public List<Object> getZoneIds() {
            return Arrays.asList(zids);
        }

        @Override
        public Code toCode(int[] gridPosition) throws TransformException {
            return new Code(dggrs, new Object[]{zids[gridPosition[0]]});
        }

        @Override
        public int[] toGrid(Code location) throws TransformException {
            final Integer i = index.get(location.getOrdinate(0));
            if (i == null) throw new TransformException("Location code outside this grid : " + location.getOrdinate(0));
            return new int[]{i};
        }

        @Override
        public void toAddress(int[] gridPosition, Object[] location, int offset) throws TransformException {
            location[offset] = zids[gridPosition[offset]];
        }

        @Override
        public void toGrid(Object[] location, int[] gridPosition, int offset) throws TransformException {
            final Integer i = index.get(location[offset]);
            if (i == null) throw new TransformException("Location code outside this grid : " + location[offset]);
            gridPosition[offset] = i;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Arrays.deepHashCode(this.zids);
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
            final Unstructured other = (Unstructured) obj;
            return Arrays.deepEquals(this.zids, other.zids);
        }
    }

    public static class SubZoneTransform extends DiscreteGlobalGridTransform {

        private final GridExtent extent;
        //computed when needed
        private Object[] zids;
        private final Map<Object,Integer> index = new HashMap<>();

        private SubZoneTransform(DiscreteGlobalGridReferenceSystem dggrs, Object baseZoneId, Integer relativeDepth) {
            super(dggrs, new Object[]{baseZoneId}, relativeDepth);
            this.extent = new GridExtent(null, 0, dggrs.getGridSystem().getHierarchy().getZone(baseZoneId).countChildrenAtRelativeDepth(relativeDepth), false);
        }

        @Override
        public GridExtent getExtent() {
            return extent;
        }

        public Object[] getZids() throws TransformException {
            init();
            return zids;
        }

        private synchronized void init() throws TransformException {
            if (zids != null) return;

            final DiscreteGlobalGridHierarchy dggh = dggrs.getGridSystem().getHierarchy();
            zids = new Object[(int) extent.getSize(0)];
            final Zone zone = dggh.getZone(baseZoneIds[0]);
            final int searchedLevel = zone.getLocationType().getRefinementLevel() + relativeDepth;
            int idx = 0;
            try (Stream<Object> s = dggh.getGrids().get(searchedLevel).getZones(zone).map(Zone::getIdentifier)) {
                Iterator<Object> iterator = s.iterator();
                while (iterator.hasNext()) {
                    Object zid = iterator.next();
                    zids[idx] = zid;
                    index.put(zid, idx);
                    idx++;
                }
            }
        }

        @Override
        public List<Object> getZoneIds() throws TransformException {
            init();
            return Arrays.asList(zids);
        }

        @Override
        public Code toCode(int[] gridPosition) throws TransformException {
            init();
            return new Code(dggrs, new Object[]{zids[gridPosition[0]]});
        }

        @Override
        public int[] toGrid(Code location) throws TransformException {
            init();
            final Integer i = index.get(location.getOrdinate(0));
            if (i == null) throw new TransformException("Location code outside this grid : " + location.getOrdinate(0));
            return new int[]{i};
        }

        private Integer toGridInternal(Object zid) throws TransformException {
            init();
            return index.get(zid);
        }

        @Override
        public void toAddress(int[] gridPosition, Object[] location, int offset) throws TransformException {
            init();
            location[offset] = zids[gridPosition[offset]];
        }

        @Override
        public void toGrid(Object[] location, int[] gridPosition, int offset) throws TransformException {
            init();
            final Integer i = index.get(location[offset]);
            if (i == null) throw new TransformException("Location code outside this grid : " + location[offset]);
            gridPosition[offset] = i;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.getRelativeDepth();
            hash = 37 * hash + Arrays.deepHashCode(this.getBaseZoneIds());
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
            final SubZoneTransform other = (SubZoneTransform) obj;
            if (!Objects.equals(this.getRelativeDepth(), other.getRelativeDepth())) {
                return false;
            }
            return Arrays.deepEquals(this.getBaseZoneIds(), other.getBaseZoneIds());
        }
    }

    public static class SubZoneTransforms extends DiscreteGlobalGridTransform {

        private final SubZoneTransform[] tiles;
        private final long[] offsets;
        private final Object[] tileZoneIds;
        private final GridExtent extent;
        private final long count;

        private final AbstractList<Object> zids = new AbstractList<Object>() {
            @Override
            public Object get(int index) {
                int idx = Arrays.binarySearch(offsets, index);
                if (idx < 0) {
                    idx = -(idx +1);
                    // use the previous iterator
                    idx--;
                }
                if (idx == offsets.length) {
                    throw new IllegalArgumentException("Position is outside grid : " + index);
                }
                try {
                    return tiles[idx].getZids()[index - (int)offsets[idx]];
                } catch (TransformException ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }

            @Override
            public int size() {
                return (int) count;
            }
        };

        public SubZoneTransforms(SubZoneTransform[] tiles) {
            super(tiles[0].dggrs, toRootZids(tiles), tiles[0].relativeDepth);
            this.tiles = tiles;
            this.offsets = new long[tiles.length];
            this.tileZoneIds = new Object[tiles.length];
            long count = 0;
            for (int i = 0; i < tiles.length; i++) {
                offsets[i] = count;
                count += tiles[i].extent.getSize(0);
                tileZoneIds[i] = tiles[i].baseZoneIds[0];
            }
            this.extent = new GridExtent(null, 0, count, false);
            this.count = count;
        }

        @Override
        public GridExtent getExtent() {
            return extent;
        }

        private static Object[] toRootZids(SubZoneTransform[] tiles) {
            final Object[] z = new Object[tiles.length];
            for (int i = 0; i < tiles.length; i++) {
                z[i] = tiles[i].baseZoneIds[0];
            }
            return z;
        }

        @Override
        public List<Object> getZoneIds() {
            return zids;
        }

        @Override
        public void toAddress(int[] gridPosition, Object[] location, int offset) throws TransformException {
            final int gp = gridPosition[offset];

            int idx = Arrays.binarySearch(offsets, gp);
            if (idx < 0) {
                idx = -(idx +1);
                // use the previous iterator
                idx--;
            }
            if (idx == offsets.length) {
                throw new TransformException("Position is outside grid : " + gp);
            }
            tiles[idx].toAddress(new int[]{gp - (int)offsets[idx]}, location, offset);
        }

        @Override
        public void toGrid(Object[] location, int[] gridPosition, int offset) throws TransformException {
            final Object zid = location[offset];
            for (SubZoneTransform trs : tiles) {
                Integer g = trs.toGridInternal(zid);
                if (g != null) {
                    gridPosition[offset] = g;
                    return;
                }
            }
            throw new TransformException("Location code outside this grid : " + zid);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.getRelativeDepth();
            hash = 37 * hash + Arrays.deepHashCode(this.getBaseZoneIds());
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
            final SubZoneTransforms other = (SubZoneTransforms) obj;
            if (!Objects.equals(this.getRelativeDepth(), other.getRelativeDepth())) {
                return false;
            }
            return Arrays.deepEquals(this.getBaseZoneIds(), other.getBaseZoneIds());
        }
    }

}
