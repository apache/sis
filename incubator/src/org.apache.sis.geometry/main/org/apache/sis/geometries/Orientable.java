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
package org.apache.sis.geometries;

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A primitive which can be reversed without changing its set-theoretic definition.
 *
 * <p>An orientable primitive is essentially a reference to a geometric primitive together with an
 * orientation reversal flag. Reversing a {@link Curve} reverses the direction in which it is
 * traversed, so that the bounded surface is always on the left of an oriented boundary curve.
 * Reversing a {@link Surface} reverses its <cite>up</cite> direction, so that the bounded solid is
 * always below an oriented boundary surface. Points and solids have no such interpretation in a
 * 3-dimensional space, therefore they are not orientable.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.15
 */
@UML(identifier="Orientable", specification=ISO_19107)
public sealed interface Orientable extends Primitive
        permits Curve,
                Surface
{

    /**
     * Whether an orientable primitive agrees with the orientation of its underlying primitive.
     *
     * @see ISO 19107:2019 - 6.4.15.2
     */
    public static enum Sign {
        /**
         * Same orientation as the underlying primitive, which the primitive itself always has.
         */
        POSITIVE,
        /**
         * Reverse orientation of the underlying primitive.
         */
        NEGATIVE
    }

    /**
     * Returns whether this geometry is traversed in the direction of its underlying primitive
     * ({@link Sign#POSITIVE}) or in the opposite direction ({@link Sign#NEGATIVE}).
     *
     * <p>The default is {@link Sign#POSITIVE}, which is what a geometry defined directly by its own
     * coordinates is: only a geometry that stands for the reverse of another one reports
     * {@link Sign#NEGATIVE}.</p>
     *
     * @return orientation of this geometry relative to its underlying primitive.
     *
     * @see ISO 19107:2019 - 6.4.15.2
     */
    @UML(identifier="orientation", specification=ISO_19107)
    default Sign getOrientationSign(){
        return Sign.POSITIVE;
    }

    /**
     * The proxy standing for the opposite orientation of the same {@linkplain #getPrimitive() primitive}.
     * It is spatially equal to this geometry and needs to carry no information other than the
     * identity of that primitive.
     *
     * @return proxy for the opposite orientation.
     *
     * @see ISO 19107:2019 - 6.4.15.3
     */
    @UML(identifier="proxy", specification=ISO_19107)
    default Orientable getProxy(){
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * The original primitive of which this geometry is one of the two orientations.
     *
     * @return primitive underlying this geometry.
     *
     * @see ISO 19107:2019 - 6.4.15.3
     */
    @UML(identifier="primitive", specification=ISO_19107)
    default Primitive getPrimitive(){
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the other member of the pair, i.e. the same primitive with the opposite orientation.
     *
     * <p>Note: this operation is named {@code opposite} in ISO 19107 figure 16.</p>
     *
     * @return this geometry with reversed orientation.
     *
     * @see ISO 19107:2019 - 6.4.15.3
     */
    // called opposite on figure 16
    @UML(identifier="reverse", specification=ISO_19107)
    default Orientable getReverse(){
        //TODO
        throw new UnsupportedOperationException();
    }

}
