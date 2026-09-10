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


/**
 * How the boundary of a geometry is computed in the ambiguous situations raised by collections.
 *
 * <p>All three rules agree on primitives and on the interior of a collection, which always includes
 * the interiors of all the elements. They differ in whether a position lying on the boundaries of
 * several elements belongs to the boundary of the collection or to its interior.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.3, 10.8.3
 */
public enum BoundaryType {
    /**
     * Boundary derived from the metric of the underlying space: a position belongs to the boundary
     * of a geometry when its distance both to that geometry and to its complement is zero.
     *
     * @see ISO 19107:2019 - 6.4.3
     */
    METRIC,
    /**
     * A position belongs to the boundary of a collection when it lies on the boundaries of an odd
     * number of elements, and to its interior when it lies on an even number of them.
     *
     * @see ISO 19107:2019 - 10.8.3.2
     */
    MOD_2,
    /**
     * A position belongs to the boundary of a collection when it lies on the boundary of exactly
     * one element, and to its interior when it lies on the boundaries of more than one.
     *
     * @see ISO 19107:2019 - 10.8.3.3
     */
    AT_LEAST_2
}
