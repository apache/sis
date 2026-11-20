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

import java.util.stream.Stream;

/**
 * A geometry can be represented by several forms.
 * A representation can be viewed a geometric abstraction level.
 *
 * Each representation gain or loss some capabilities.
 * It is possible to downgrade a Geometry representation from one type to another using the methods :
 * parametrize, discretize and tesselate.
 *
 * For example : a Sphere.
 * A sphere is an analytic geometry with a center and a radius, it is an exact geometric definition but it can not hold attributes on it's surface.
 * The sphere can be parametrize in a number of Nurb surfaces without lossing exactness, but also gaining attributes on it's surface.
 * It can then be discretize to a polyhedron, lossing in surface precision but having attributes attached to exact vertices.
 * And it can be tesselated to triangles for GPU processing.
 *
 * @author Johann Sorel (Geomatys)
 */
public enum Representation {
    EMPTY,
    ANALYTIC,
    PARAMETRIC,
    DISCRETE,
    TESSELATED;

    /**
     * @return the most abstract representation not empty
     */
    public static Representation mostAbstract(Representation r1, Representation r2) {
        final int ordinal1 = r1.ordinal();
        if (ordinal1 == 0) return r2;
        final int ordinal2 = r2.ordinal();
        if (ordinal2 == 0) return r1;
        return ordinal1 < ordinal2 ? r1 : r2;
    }

    /**
     * @return the most abstract representation
     *         returns EMPTY is stream is empty or contains only empty elements
     */
    public static Representation mostAbstract(Stream<Representation> stream) {
        return stream.reduce(Representation::mostAbstract).orElse(EMPTY);
    }
}
