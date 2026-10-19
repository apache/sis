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

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.apache.sis.geometries.Orientable;


/**
 * A topological primitive with an orientation, the topological equivalent of {@link Orientable}.
 *
 * <p>Since a primitive has only two orientations whatever its dimension, each primitive has two oriented
 * primitives. Each primitive type extends its oriented type, so that a primitive is its own positive
 * orientation, as in the algebraic <var>x</var> = +<var>x</var>. A {@link Node} is a positive
 * {@link DirectedNode}, an {@link Edge} a positive {@link DirectedEdge}, and so on.</p>
 *
 * <p>Oriented primitives of adjacent dimensions agree on the orientation: if a positive edge is on the
 * boundary of a face, the positive face is on the co-boundary of that edge.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The {@linkplain #getBoundary() boundary} is the boundary of the {@linkplain #getTopo() primitive},
 *       each term multiplied by the {@linkplain #getOrientationSign() orientation}. Likewise for the
 *       {@linkplain #getCoBoundary() co-boundary}.</li>
 *   <li>The {@linkplain #getNegate() opposite} has the same primitive and the other orientation.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.1.4
 */
@UML(identifier="DirectedTopo", specification=ISO_19107)
public interface DirectedTopo extends Primitive {

    /**
     * Whether this oriented primitive agrees with the orientation of its {@linkplain #getTopo() primitive}.
     * The default is {@link Orientable.Sign#POSITIVE}, which is always the case of a primitive itself.
     *
     * @return orientation of this object relative to its primitive.
     *
     * @see ISO 19107:2019 - 10.1.4.2
     */
    @UML(identifier="orientation", specification=ISO_19107)
    default Orientable.Sign getOrientationSign() {
        return Orientable.Sign.POSITIVE;
    }

    /**
     * The same primitive with the opposite orientation.
     *
     * @return this object with reversed orientation.
     *
     * @see ISO 19107:2019 - 10.1.4.3
     */
    @UML(identifier="negate", specification=ISO_19107)
    DirectedTopo getNegate();

    /**
     * This oriented primitive as an expression of a single term, keeping its orientation.
     *
     * @return expression made of this oriented primitive.
     *
     * @see ISO 19107:2019 - 10.1.4.4
     */
    @UML(identifier="expression", specification=ISO_19107)
    Expression getExpression();

    /**
     * The primitive of which this object is one of the two orientations.
     * A primitive returns itself.
     *
     * @return primitive underlying this object.
     *
     * @see ISO 19107:2019 - 10.1.4.5
     */
    @UML(identifier="topo", specification=ISO_19107)
    Primitive getTopo();

}
