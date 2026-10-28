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
import java.util.Set;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.apache.sis.geometries.Orientable;


/**
 * A non-decomposed element of a topological {@link Complex}.
 *
 * <p>A topological primitive usually stands for a geometric primitive of the same dimension. When a
 * geometric complex is the realization of a topological complex, the primitives of both are in a
 * one-to-one correspondence which preserves the dimension.</p>
 *
 * <p>Unlike a geometric primitive, which may stand alone, a topological primitive is always part of at
 * least one complex.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.1.3
 */
@UML(identifier="Primitive", specification=ISO_19107)
public interface Primitive extends Topology {

    /**
     * Returns this primitive alone: a primitive is its own interior.
     *
     * @return a set containing only this primitive.
     *
     * @see ISO 19107:2019 - 10.1.2.6
     */
    @UML(identifier="interior", specification=ISO_19107)
    @Override
    default Set<Primitive> getInterior() {
        return Set.of(this);
    }

    /**
     * The geometric primitive this primitive represents in its maximal complex, or {@code null} if the
     * maximal complex describes a logical structure without geometric realization.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Either every primitive of a maximal complex has a geometry, or none has.</li>
     *   <li>A geometric primitive is associated with at most one primitive of any given complex,
     *       but may be associated with primitives of different complexes.</li>
     * </ul>
     *
     * @return geometric realization of this primitive, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 10.1.3.2
     */
    @UML(identifier="geometry", specification=ISO_19107)
    org.apache.sis.geometries.Primitive getGeometry();

    /**
     * The complexes containing this primitive. They are all sub-complexes of the single
     * maximal complex containing this primitive.
     *
     * @return complexes containing this primitive.
     *
     * @see ISO 19107:2019 - 10.1.3.3
     */
    @UML(identifier="complex", specification=ISO_19107)
    Set<Complex> getComplexes();

    /**
     * Primitives isolated in this primitive: lying in its interior without any primitive of intermediate
     * dimension between them. Examples are a node inside a face and not attached to the boundary of that face,
     * or an edge inside a solid and not attached to its boundary faces.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The dimension of every isolated primitive is lower than the dimension of this primitive minus one.</li>
     * </ul>
     *
     * @return primitives isolated in this primitive, empty if none.
     *
     * @see ISO 19107:2019 - 10.1.3.4
     */
    @UML(identifier="isolated", specification=ISO_19107)
    Set<Primitive> getIsolated();

    /**
     * The primitive in which this primitive is isolated, or {@code null} if none.
     * A primitive without container lies on the boundary of another primitive,
     * unless it has the highest dimension of its complex.
     *
     * @return primitive in which this primitive is isolated, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 10.1.3.4
     */
    @UML(identifier="container", specification=ISO_19107)
    Primitive getContainer();

    /**
     * The two oriented primitives standing for this primitive,
     * the {@linkplain Orientable.Sign#POSITIVE positive} one first.
     *
     * @return the positive then the negative oriented primitive.
     *
     * @see ISO 19107:2019 - 10.1.4.5
     */
    @UML(identifier="proxy", specification=ISO_19107)
    List<? extends DirectedTopo> getProxy();

    /**
     * Returns the oriented primitive standing for this primitive with the given orientation.
     *
     * <p>The default implementation returns the first or the second {@linkplain #getProxy() proxy}.</p>
     *
     * @param  orientation  orientation of the requested oriented primitive.
     * @return the oriented primitive with the given orientation.
     *
     * @see ISO 19107:2019 - 10.1.3
     */
    @UML(identifier="asDirectedTopo", specification=ISO_19107)
    default DirectedTopo asDirectedTopo(final Orientable.Sign orientation) {
        return getProxy().get(orientation == Orientable.Sign.POSITIVE ? 0 : 1);
    }

}
