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
 * The 1-dimensional topological primitive, usually realized by a {@link org.apache.sis.geometries.Curve}.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.4.1
 */
@UML(identifier="Edge", specification=ISO_19107)
public interface Edge extends DirectedEdge {

    /**
     * The start and end nodes of this edge, as the expression <var>end</var> − <var>start</var>:
     * the end node is a positive {@link DirectedNode} and the start node a negative one.
     * When both nodes are the same, the two terms cancel out in the expression.
     *
     * @return the two oriented nodes at the ends of this edge.
     *
     * @see ISO 19107:2019 - 10.4.1.3
     */
    @UML(identifier="boundary", specification=ISO_19107)
    @Override
    List<DirectedNode> getBoundary();

    /**
     * Oriented faces using this edge on their boundary: positive when the face uses this edge,
     * negative when it uses its negative orientation.
     *
     * <p>The faces are listed in clockwise circular order, as seen from the end of the edge
     * in a geometric realization of the maximal complex containing this edge.</p>
     *
     * <p>In a 2-dimensional planar complex there are at most two faces, exactly two in a full topology:
     * the positive one on the left of the edge, and the negative one on its right.</p>
     *
     * @return oriented faces around this edge, in clockwise order seen from its end.
     *
     * @see ISO 19107:2019 - 10.4.1.2
     */
    @UML(identifier="coBoundary", specification=ISO_19107)
    @Override
    List<DirectedFace> getCoBoundary();

    /**
     * Returns this edge, which is its own positive orientation.
     *
     * @see ISO 19107:2019 - 10.1.4.1
     */
    @UML(identifier="topo", specification=ISO_19107)
    @Override
    default Edge getTopo() {
        return this;
    }

    /**
     * Returns this edge then its {@linkplain #getNegate() negative orientation}.
     *
     * @see ISO 19107:2019 - 10.4.1.4
     */
    @UML(identifier="proxy", specification=ISO_19107)
    @Override
    default List<DirectedEdge> getProxy() {
        return List.of(this, getNegate());
    }

}
