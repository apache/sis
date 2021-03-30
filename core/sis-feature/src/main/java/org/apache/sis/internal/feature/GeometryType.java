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
package org.apache.sis.internal.feature;


/**
 * Implementation-neutral description of the type of geometry.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see Geometries#getGeometryClass(GeometryType)
 *
 * @since 1.1
 * @module
 */
public enum GeometryType {
    /**
     * Base class of all geometries, with the possible exception of point in some implementation.
     *
     * @see Geometries#rootClass
     */
    GEOMETRY,

    /**
     * Zero-dimensional geometry containing a single point.
     * Note that this is not necessarily a subtype of {@link #GEOMETRY}.
     * The notable exception is Java2D.
     *
     * @see Geometries#pointClass
     */
    POINT,

    /**
     * Sequence of points connected by straight, non-self intersecting line pieces.
     * This is a one-dimensional geometry.
     *
     * @see Geometries#polylineClass
     */
    LINESTRING,

    /**
     * Geometry with a positive area (two-dimensional).
     * The sequence of points form a closed, non-self intersecting ring.
     *
     * @see Geometries#polygonClass
     */
    POLYGON,

    /**
     * Set of points.
     */
    MULTI_POINT,

    /**
     * Set of linestrings.
     */
    MULTI_LINESTRING,

    /**
     * Set of polygons.
     */
    MULTI_POLYGON,

    /**
     * Set of geometries of any type except other geometry collection.
     */
    GEOMETRY_COLLECTION;
}
