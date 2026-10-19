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
package org.apache.sis.geometries.topology;

import java.util.List;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * The 3-dimensional topological primitive, usually realized by a {@link org.apache.sis.geometries.Solid}.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.6.1
 */
@UML(identifier="Solid", specification=ISO_19107)
public interface Solid extends DirectedSolid {

    /**
     * Oriented faces bounding this solid: positive for the faces having this solid below them,
     * negative for those having it above them. The faces of every shell are included,
     * those of the {@linkplain #getExteriorShell() exterior shell} first.
     *
     * @return oriented faces bounding this solid, at least one.
     *
     * @see ISO 19107:2019 - 10.6.1.2
     */
    @UML(identifier="boundary", specification=ISO_19107)
    @Override
    List<DirectedFace> getBoundary();

    /**
     * The shell of the {@linkplain #getBoundary() boundary} which is the exterior of this solid.
     *
     * @return oriented faces of the exterior shell.
     *
     * @see ISO 19107:2019 - 10.6.1.2
     */
    List<DirectedFace> getExteriorShell();

    /**
     * The shells of the {@linkplain #getBoundary() boundary} bounding the voids of this solid.
     *
     * @return oriented faces of every interior shell, empty if none.
     *
     * @see ISO 19107:2019 - 10.6.1.2
     */
    List<List<DirectedFace>> getInteriorShells();

    /**
     * Returns this solid, which is its own positive orientation.
     *
     * @see ISO 19107:2019 - 10.1.4.1
     */
    @UML(identifier="topo", specification=ISO_19107)
    @Override
    default Solid getTopo() {
        return this;
    }

    /**
     * Returns this solid then its {@linkplain #getNegate() negative orientation}.
     *
     * @see ISO 19107:2019 - 10.6.1.4
     */
    @UML(identifier="proxy", specification=ISO_19107)
    @Override
    default List<DirectedSolid> getProxy() {
        return List.of(this, getNegate());
    }

}
