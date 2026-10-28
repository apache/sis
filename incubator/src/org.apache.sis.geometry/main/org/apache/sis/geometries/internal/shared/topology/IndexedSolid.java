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
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.topology.DirectedFace;
import org.apache.sis.geometries.topology.DirectedSolid;
import org.apache.sis.geometries.topology.Solid;


/**
 * View of a solid of an {@link IndexedComplex}.
 *
 * @author Johann Sorel (Geomatys)
 */
final class IndexedSolid extends IndexedTopo implements Solid {

    IndexedSolid(final IndexedComplex complex, final int index) {
        super(complex, index);
    }

    @Override
    String typeName() {
        return "Solid";
    }

    @Override
    public List<DirectedFace> getBoundary() {
        return complex.solidBoundary(index, false);
    }

    @Override
    public List<DirectedFace> getExteriorShell() {
        return complex.shell(index, 0, false);
    }

    @Override
    public List<List<DirectedFace>> getInteriorShells() {
        return complex.interiorShells(index);
    }

    @Override
    public DirectedSolid getNegate() {
        return new Negative(complex, index);
    }

    @Override
    public Polyhedron getGeometry() {
        return complex.polyhedron(index);
    }

    /**
     * View of the negative orientation of a solid, which is above all its faces.
     */
    static final class Negative extends IndexedTopo implements DirectedSolid {

        Negative(final IndexedComplex complex, final int index) {
            super(complex, index);
        }

        @Override
        String typeName() {
            return "Solid";
        }

        @Override
        public Orientable.Sign getOrientationSign() {
            return Orientable.Sign.NEGATIVE;
        }

        @Override
        public Solid getTopo() {
            return new IndexedSolid(complex, index);
        }

        @Override
        public DirectedSolid getNegate() {
            return getTopo();
        }

        @Override
        public List<DirectedSolid> getProxy() {
            return getTopo().getProxy();
        }

        @Override
        public List<DirectedFace> getBoundary() {
            return complex.solidBoundary(index, true);
        }

        /**
         * Returns the polyhedron of the solid, since a geometric solid has no orientation.
         */
        @Override
        public Polyhedron getGeometry() {
            return complex.polyhedron(index);
        }
    }
}
