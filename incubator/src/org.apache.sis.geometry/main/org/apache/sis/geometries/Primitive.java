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

import java.util.List;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A connected geometric object having a uniform dimension at each of its interior points.
 *
 * <p>Which primitives exist depends on the spatial dimension of the coordinate space:
 * {@link Point} is 0-dimensional, {@link Curve} is 1-dimensional, {@link Surface} is 2-dimensional
 * and {@link Solid} is 3-dimensional. {@link Empty} covers the −1 dimensional case.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>Every non-boundary point of a primitive is contained in a local neighbourhood
 *       topologically isomorphic to the interior of the unit disk of the Euclidean space
 *       of the same dimension.</li>
 *   <li>A primitive is not decomposed into other primitives in the system, even though curves and
 *       surfaces are made of curve and surface {@linkplain #getSegments() segments}.</li>
 *   <li>Segments cannot exist outside the context of the primitive which owns them.</li>
 * </ul>
 *
 * <p>Note: most primitives can be decomposed indefinitely, since adding a point in the middle of a
 * curve splits it into two curves. The usual "non-decomposable" definition of a primitive therefore
 * does not apply here; the only non-decomposable geometric object is a point.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.11
 */
@UML(identifier="Primitive", specification=ISO_19107)
public sealed interface Primitive extends Geometry
        permits Point,
                Orientable,
                Solid
{

    /**
     * Smaller primitives of the same dimension contained in this primitive,
     * each of them defining a portion of it.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>A point has no segment.</li>
     *   <li>For a curve, the order of the segments is the order in which they are traversed:
     *       this curve is traversed first, then each segment recursively in list order.</li>
     *   <li>For a surface or a solid, the order may be ignored.</li>
     * </ul>
     *
     * @return segments of this primitive, possibly empty.
     *
     * @see ISO 19107:2019 - 6.4.11.2
     */
    default List<Primitive> getSegments() {
        //TODO
        throw new UnsupportedOperationException();
    }
}
