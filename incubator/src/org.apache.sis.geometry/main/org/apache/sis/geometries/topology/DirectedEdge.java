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
 * An {@link Edge} with an orientation, used in the terms of an {@link Expression}.
 * It is the topological equivalent of an oriented {@link org.apache.sis.geometries.Curve},
 * standing for the edge traversed in one direction or the other.
 *
 * <p>In a 2-dimensional planar complex, an oriented edge bounds at most one face, on its left.
 * In 3 dimensions, or in a non-planar 2-dimensional complex, it may bound several faces.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.4.2
 */
@UML(identifier="DirectedEdge", specification=ISO_19107)
public interface DirectedEdge extends DirectedTopo {

    /**
     * Returns 1: an edge is a curve.
     *
     * @see ISO 19107:2019 - 10.4.1.5
     */
    @UML(identifier="dimension", specification=ISO_19107)
    @Override
    default int getDimension() {
        return 1;
    }

    /**
     * The two oriented nodes bounding this oriented edge: the node where it ends is positive,
     * and the node where it starts is negative.
     *
     * @return the two oriented nodes at the ends of this oriented edge.
     *
     * @see ISO 19107:2019 - 10.4.1.3
     */
    @UML(identifier="boundary", specification=ISO_19107)
    @Override
    List<DirectedNode> getBoundary();

    /**
     * Oriented faces having this oriented edge on their boundary.
     * See {@link Edge#getCoBoundary()} for the orientation and the order of the faces.
     *
     * @return oriented faces around this oriented edge.
     *
     * @see ISO 19107:2019 - 10.4.1.2
     */
    @UML(identifier="coBoundary", specification=ISO_19107)
    @Override
    List<DirectedFace> getCoBoundary();

    /**
     * The same edge with the opposite orientation.
     *
     * @see ISO 19107:2019 - 10.1.4.3
     */
    @UML(identifier="negate", specification=ISO_19107)
    @Override
    DirectedEdge getNegate();

    /**
     * The edge of which this object is one of the two orientations.
     *
     * @see ISO 19107:2019 - 10.4.1.4
     */
    @UML(identifier="topo", specification=ISO_19107)
    @Override
    Edge getTopo();

    /**
     * The two oriented edges standing for the edge, the positive one first.
     *
     * @see ISO 19107:2019 - 10.4.1.4
     */
    @UML(identifier="proxy", specification=ISO_19107)
    @Override
    List<DirectedEdge> getProxy();

}
