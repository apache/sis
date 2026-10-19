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
 * A {@link Face} with an orientation, used in the terms of an {@link Expression} and to define the boundary
 * of a {@link Solid}. It is the topological equivalent of an oriented {@link org.apache.sis.geometries.Surface}.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.5.3
 */
@UML(identifier="DirectedFace", specification=ISO_19107)
public interface DirectedFace extends DirectedTopo {

    /**
     * Returns 2: a face is a surface.
     *
     * @see ISO 19107:2019 - 10.5.2.4
     */
    @UML(identifier="dimension", specification=ISO_19107)
    @Override
    default int getDimension() {
        return 2;
    }

    /**
     * Oriented edges bounding this oriented face, positive when the face is on their left.
     * See {@link Face#getBoundary()} for the organization in rings.
     *
     * @return oriented edges bounding this oriented face.
     *
     * @see ISO 19107:2019 - 10.5.2.1
     */
    @UML(identifier="boundary", specification=ISO_19107)
    @Override
    List<DirectedEdge> getBoundary();

    /**
     * Oriented solids having this oriented face on their boundary, at most two.
     * See {@link Face#getCoBoundary()} for the orientation of the solids.
     *
     * @return oriented solids on each side of this oriented face.
     *
     * @see ISO 19107:2019 - 10.5.2.2
     */
    @UML(identifier="coBoundary", specification=ISO_19107)
    @Override
    List<DirectedSolid> getCoBoundary();

    /**
     * The same face with the opposite orientation.
     *
     * @see ISO 19107:2019 - 10.1.4.3
     */
    @UML(identifier="negate", specification=ISO_19107)
    @Override
    DirectedFace getNegate();

    /**
     * The face of which this object is one of the two orientations.
     *
     * @see ISO 19107:2019 - 10.5.2.3
     */
    @UML(identifier="topo", specification=ISO_19107)
    @Override
    Face getTopo();

    /**
     * The two oriented faces standing for the face, the positive one first.
     *
     * @see ISO 19107:2019 - 10.5.2.3
     */
    @UML(identifier="proxy", specification=ISO_19107)
    @Override
    List<DirectedFace> getProxy();

}
