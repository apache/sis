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
 * The 0-dimensional topological primitive, usually realized by a {@link org.apache.sis.geometries.Point}.
 *
 * <p>A node has no boundary. It may be isolated in a face while being the end of an edge, as long as that
 * edge is not on the boundary of the face: the realization is a curve hanging in space and ending where it
 * meets a surface.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.3.2
 */
@UML(identifier="Node", specification=ISO_19107)
public interface Node extends DirectedNode {

    /**
     * Oriented edges having this node on their boundary: positive for the edges entering
     * this node (ending at it), and negative for the edges leaving it (starting at it).
     *
     * <p>In a 2-dimensional maximal complex, the edges can be listed as a clockwise circular sequence
     * around the node in any geometric realization. In a 3-dimensional complex the order is arbitrary.</p>
     *
     * @return oriented edges starting or ending at this node.
     *
     * @see ISO 19107:2019 - 10.3.2.1
     */
    @UML(identifier="coBoundary", specification=ISO_19107)
    @Override
    List<DirectedEdge> getCoBoundary();

    /**
     * Returns this node, which is its own positive orientation.
     *
     * @see ISO 19107:2019 - 10.1.4.1
     */
    @UML(identifier="topo", specification=ISO_19107)
    @Override
    default Node getTopo() {
        return this;
    }

    /**
     * Returns this node then its {@linkplain #getNegate() negative orientation}.
     *
     * @see ISO 19107:2019 - 10.3.2.2
     */
    @UML(identifier="proxy", specification=ISO_19107)
    @Override
    default List<DirectedNode> getProxy() {
        return List.of(this, getNegate());
    }

}
