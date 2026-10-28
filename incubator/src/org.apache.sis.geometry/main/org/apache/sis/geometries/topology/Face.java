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
 * The 2-dimensional topological primitive, usually realized by a {@link org.apache.sis.geometries.Surface}.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.5.2
 */
@UML(identifier="Face", specification=ISO_19107)
public interface Face extends DirectedFace {

    /**
     * Oriented edges bounding this face: positive for the edges having this face on their left,
     * negative for those having it on their right. The edges of every ring are included,
     * those of the {@linkplain #getExteriorRing() exterior ring} first.
     *
     * @return oriented edges bounding this face, at least one.
     *
     * @see ISO 19107:2019 - 10.5.2.1
     */
    @UML(identifier="boundary", specification=ISO_19107)
    @Override
    List<DirectedEdge> getBoundary();

    /**
     * The ring of the {@linkplain #getBoundary() boundary} which is the exterior of this face,
     * as oriented edges in traversal order.
     *
     * <p>The same restriction applies as for the geometric boundary of a surface: on a closed surface such
     * as a sphere, no ring can be told to be the exterior one, and this method returns an empty list.</p>
     *
     * @return oriented edges of the exterior ring, or an empty list if it cannot be determined.
     *
     * @see ISO 19107:2019 - 10.5.2.1
     */
    List<DirectedEdge> getExteriorRing();

    /**
     * The rings of the {@linkplain #getBoundary() boundary} other than the {@linkplain #getExteriorRing() exterior one},
     * each as oriented edges in traversal order.
     *
     * @return oriented edges of every interior ring, empty if none.
     *
     * @see ISO 19107:2019 - 10.5.2.1
     */
    List<List<DirectedEdge>> getInteriorRings();

    /**
     * Oriented solids using this face on their boundary: positive when the solid uses this face,
     * negative when it uses its negative orientation. A face separates at most two solids.
     *
     * @return oriented solids on each side of this face, at most two.
     *
     * @see ISO 19107:2019 - 10.5.2.2
     */
    @UML(identifier="coBoundary", specification=ISO_19107)
    @Override
    List<DirectedSolid> getCoBoundary();

    /**
     * Returns this face, which is its own positive orientation.
     *
     * @see ISO 19107:2019 - 10.1.4.1
     */
    @UML(identifier="topo", specification=ISO_19107)
    @Override
    default Face getTopo() {
        return this;
    }

    /**
     * Returns this face then its {@linkplain #getNegate() negative orientation}.
     *
     * @see ISO 19107:2019 - 10.5.2.3
     */
    @UML(identifier="proxy", specification=ISO_19107)
    @Override
    default List<DirectedFace> getProxy() {
        return List.of(this, getNegate());
    }

}
