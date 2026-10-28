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
 * A {@link Solid} with an orientation, used in the terms of an {@link Expression}.
 * There is no separate geometric equivalent, since a geometric solid is not orientable.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.6.2
 */
@UML(identifier="DirectedSolid", specification=ISO_19107)
public interface DirectedSolid extends DirectedTopo {

    /**
     * Returns 3: a solid is a volume.
     *
     * @see ISO 19107:2019 - 10.1.2.3
     */
    @UML(identifier="dimension", specification=ISO_19107)
    @Override
    default int getDimension() {
        return 3;
    }

    /**
     * Oriented faces bounding this oriented solid, positive when the solid is below them.
     * See {@link Solid#getBoundary()} for the organization in shells.
     *
     * @return oriented faces bounding this oriented solid.
     *
     * @see ISO 19107:2019 - 10.6.1.2
     */
    @UML(identifier="boundary", specification=ISO_19107)
    @Override
    List<DirectedFace> getBoundary();

    /**
     * Returns an empty list: a solid has the highest dimension, so it is on the boundary of nothing.
     *
     * <p>Difference with ISO 19107: the standard returns {@code NULL}.</p>
     *
     * @see ISO 19107:2019 - 10.6.1.3
     */
    @UML(identifier="coBoundary", specification=ISO_19107)
    @Override
    default List<? extends DirectedTopo> getCoBoundary() {
        return List.of();
    }

    /**
     * The same solid with the opposite orientation.
     *
     * @see ISO 19107:2019 - 10.1.4.3
     */
    @UML(identifier="negate", specification=ISO_19107)
    @Override
    DirectedSolid getNegate();

    /**
     * The solid of which this object is one of the two orientations.
     *
     * @see ISO 19107:2019 - 10.6.1.4
     */
    @UML(identifier="topo", specification=ISO_19107)
    @Override
    Solid getTopo();

    /**
     * The two oriented solids standing for the solid, the positive one first.
     *
     * @see ISO 19107:2019 - 10.6.1.4
     */
    @UML(identifier="proxy", specification=ISO_19107)
    @Override
    List<DirectedSolid> getProxy();

}
