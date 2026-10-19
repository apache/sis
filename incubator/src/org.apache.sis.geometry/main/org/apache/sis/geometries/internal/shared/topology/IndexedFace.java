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
package org.apache.sis.geometries.internal.shared.topology;

import java.util.List;
import org.apache.sis.geometries.Orientable;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.topology.DirectedEdge;
import org.apache.sis.geometries.topology.DirectedFace;
import org.apache.sis.geometries.topology.DirectedSolid;
import org.apache.sis.geometries.topology.Face;


/**
 * View of a face of an {@link IndexedComplex}.
 *
 * @author Johann Sorel (Geomatys)
 */
final class IndexedFace extends IndexedTopo implements Face {

    IndexedFace(final IndexedComplex complex, final int index) {
        super(complex, index);
    }

    @Override
    String typeName() {
        return "Face";
    }

    @Override
    public List<DirectedEdge> getBoundary() {
        return complex.faceBoundary(index, false);
    }

    /**
     * Returns the first ring, which is always the exterior one in an indexed complex.
     */
    @Override
    public List<DirectedEdge> getExteriorRing() {
        return complex.ring(index, 0, false);
    }

    @Override
    public List<List<DirectedEdge>> getInteriorRings() {
        return complex.interiorRings(index);
    }

    @Override
    public List<DirectedSolid> getCoBoundary() {
        return complex.faceCoBoundary(index, false);
    }

    @Override
    public DirectedFace getNegate() {
        return new Negative(complex, index);
    }

    @Override
    public Polygon getGeometry() {
        return complex.polygon(index, false);
    }

    /**
     * View of the negative orientation of a face, whose rings are traversed in reverse order.
     */
    static final class Negative extends IndexedTopo implements DirectedFace {

        Negative(final IndexedComplex complex, final int index) {
            super(complex, index);
        }

        @Override
        String typeName() {
            return "Face";
        }

        @Override
        public Orientable.Sign getOrientationSign() {
            return Orientable.Sign.NEGATIVE;
        }

        @Override
        public Face getTopo() {
            return new IndexedFace(complex, index);
        }

        @Override
        public DirectedFace getNegate() {
            return getTopo();
        }

        @Override
        public List<DirectedFace> getProxy() {
            return getTopo().getProxy();
        }

        @Override
        public List<DirectedEdge> getBoundary() {
            return complex.faceBoundary(index, true);
        }

        @Override
        public List<DirectedSolid> getCoBoundary() {
            return complex.faceCoBoundary(index, true);
        }

        /**
         * Returns the polygon of the face with all rings reversed.
         */
        @Override
        public Polygon getGeometry() {
            return complex.polygon(index, true);
        }
    }
}
