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
 * The subtypes of {@link Geometry} supported by an implementation.
 *
 * <p>This code list is the software contract between an implementation and its users regarding the
 * geometry types it supports, based on dimension and interpolation mechanism.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>All the {@link Geometry} subtypes supported by an implementation are enumerated here.</li>
 *   <li>{@link #GEOMETRY}, {@link #EMPTY} and {@link #POINT} are always present.</li>
 * </ul>
 *
 * <p>Note: ISO 19107 defines a short list, but in different parts of the UML it refers to more
 * accurate types.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.6
 */
@UML(identifier="GeometryType", specification=ISO_19107)
public enum GeometryType {
    /**
     * The empty set, of topological dimension −1.
     */
    EMPTY,
    /**
     * The abstract root of all geometry types.
     */
    GEOMETRY,
    /**
     * A collection behaving as the set union of its elements.
     */
    COLLECTION,

    //point types
    /**
     * A single location, of topological dimension 0.
     */
    POINT,

    //curve types
    /**
     * A curve of unspecified interpolation, of topological dimension 1.
     */
    CURVE,
    /**
     * A curve using a linear interpolation in the coordinate system.
     */
    LINE,
    /**
     * A curve following the shortest path on the geometric reference surface.
     */
    GEODESIC,
    /**
     * A curve of constant azimuth, also called loxodrome.
     */
    RHUMB,
    /**
     * A curve interpolated by spline functions.
     */
    SPLINECURVE,

    //surface types
    /**
     * A surface of unspecified interpolation, of topological dimension 2.
     */
    SURFACE,
    /**
     * A surface defined only by its boundary rings and a spanning surface.
     */
    POLYGON,
    /**
     * A surface interpolated by spline functions.
     */
    SPLINESURFACE,

    //solid types
    /**
     * A solid interpolated by spline functions, of topological dimension 3.
     */
    SPLINESOLID,
}
