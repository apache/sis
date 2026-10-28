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
 * A {@link Node} with an orientation, used in the terms of an {@link Expression}.
 * The end node of an edge appears positively in the boundary of that edge, and its start node negatively.
 * There is no separate geometric equivalent, since a point has no orientation.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.3.3
 */
@UML(identifier="DirectedNode", specification=ISO_19107)
public interface DirectedNode extends DirectedTopo {

    /**
     * Returns 0: a node is a point.
     *
     * @see ISO 19107:2019 - 10.3.2.4
     */
    @UML(identifier="dimension", specification=ISO_19107)
    @Override
    default int getDimension() {
        return 0;
    }

    /**
     * Returns an empty list: a node has no boundary, which is a zero expression.
     *
     * @see ISO 19107:2019 - 10.3.2.3
     */
    @UML(identifier="boundary", specification=ISO_19107)
    @Override
    default List<? extends DirectedTopo> getBoundary() {
        return List.of();
    }

    /**
     * Oriented edges having this oriented node on their boundary.
     * See {@link Node#getCoBoundary()} for the orientation of the edges.
     *
     * @return oriented edges starting or ending at this node.
     *
     * @see ISO 19107:2019 - 10.3.2.1
     */
    @UML(identifier="coBoundary", specification=ISO_19107)
    @Override
    List<DirectedEdge> getCoBoundary();

    /**
     * The same node with the opposite orientation.
     *
     * @see ISO 19107:2019 - 10.1.4.3
     */
    @UML(identifier="negate", specification=ISO_19107)
    @Override
    DirectedNode getNegate();

    /**
     * The node of which this object is one of the two orientations.
     *
     * @see ISO 19107:2019 - 10.1.4.6
     */
    @UML(identifier="topo", specification=ISO_19107)
    @Override
    Node getTopo();

    /**
     * The two oriented nodes standing for the node, the positive one first.
     *
     * @see ISO 19107:2019 - 10.3.2.2
     */
    @UML(identifier="proxy", specification=ISO_19107)
    @Override
    List<DirectedNode> getProxy();

}
