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
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.topology.DirectedEdge;
import org.apache.sis.geometries.topology.DirectedFace;
import org.apache.sis.geometries.topology.DirectedNode;
import org.apache.sis.geometries.topology.Edge;


/**
 * View of an edge of an {@link IndexedComplex}.
 *
 * @author Johann Sorel (Geomatys)
 */
final class IndexedEdge extends IndexedTopo implements Edge {

    IndexedEdge(final IndexedComplex complex, final int index) {
        super(complex, index);
    }

    @Override
    String typeName() {
        return "Edge";
    }

    /**
     * Returns the start node negatively, then the end node positively.
     */
    @Override
    public List<DirectedNode> getBoundary() {
        return List.of(complex.directedNode(~complex.edgeNodes[2*index]),
                       complex.directedNode(complex.edgeNodes[2*index + 1]));
    }

    @Override
    public List<DirectedFace> getCoBoundary() {
        return complex.edgeCoBoundary(index, false);
    }

    @Override
    public DirectedEdge getNegate() {
        return new Negative(complex, index);
    }

    @Override
    public LineString getGeometry() {
        return complex.lineString(complex.edgeNodes[2*index], complex.edgeNodes[2*index + 1]);
    }

    /**
     * View of the negative orientation of an edge, which goes from the end node to the start node.
     */
    static final class Negative extends IndexedTopo implements DirectedEdge {

        Negative(final IndexedComplex complex, final int index) {
            super(complex, index);
        }

        @Override
        String typeName() {
            return "Edge";
        }

        @Override
        public Orientable.Sign getOrientationSign() {
            return Orientable.Sign.NEGATIVE;
        }

        @Override
        public Edge getTopo() {
            return new IndexedEdge(complex, index);
        }

        @Override
        public DirectedEdge getNegate() {
            return getTopo();
        }

        @Override
        public List<DirectedEdge> getProxy() {
            return getTopo().getProxy();
        }

        /**
         * Returns the end node of the edge negatively, then its start node positively.
         */
        @Override
        public List<DirectedNode> getBoundary() {
            return List.of(complex.directedNode(~complex.edgeNodes[2*index + 1]),
                           complex.directedNode(complex.edgeNodes[2*index]));
        }

        @Override
        public List<DirectedFace> getCoBoundary() {
            return complex.edgeCoBoundary(index, true);
        }

        /**
         * Returns the segment of the edge, from the end node to the start node.
         */
        @Override
        public LineString getGeometry() {
            return complex.lineString(complex.edgeNodes[2*index + 1], complex.edgeNodes[2*index]);
        }
    }
}
