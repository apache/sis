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
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.topology.DirectedEdge;
import org.apache.sis.geometries.topology.DirectedNode;
import org.apache.sis.geometries.topology.Node;


/**
 * View of a node of an {@link IndexedComplex}.
 *
 * @author Johann Sorel (Geomatys)
 */
final class IndexedNode extends IndexedTopo implements Node {

    IndexedNode(final IndexedComplex complex, final int index) {
        super(complex, index);
    }

    @Override
    String typeName() {
        return "Node";
    }

    @Override
    public List<DirectedEdge> getCoBoundary() {
        return complex.nodeCoBoundary(index, false);
    }

    @Override
    public DirectedNode getNegate() {
        return new Negative(complex, index);
    }

    @Override
    public Point getGeometry() {
        return complex.point(index);
    }

    /**
     * View of the negative orientation of a node.
     */
    static final class Negative extends IndexedTopo implements DirectedNode {

        Negative(final IndexedComplex complex, final int index) {
            super(complex, index);
        }

        @Override
        String typeName() {
            return "Node";
        }

        @Override
        public Orientable.Sign getOrientationSign() {
            return Orientable.Sign.NEGATIVE;
        }

        @Override
        public Node getTopo() {
            return new IndexedNode(complex, index);
        }

        @Override
        public DirectedNode getNegate() {
            return getTopo();
        }

        @Override
        public List<DirectedNode> getProxy() {
            return getTopo().getProxy();
        }

        @Override
        public List<DirectedEdge> getCoBoundary() {
            return complex.nodeCoBoundary(index, true);
        }

        /**
         * Returns the point of the node, since a point has no orientation.
         */
        @Override
        public Point getGeometry() {
            return complex.point(index);
        }
    }
}
