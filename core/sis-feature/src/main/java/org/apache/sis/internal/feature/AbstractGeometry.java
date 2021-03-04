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
 * Base class of all geometries in Apache SIS. A geometry may be either an implementation provided
 * directly by Apache SIS, or a wrapper around an external library such as Java Topology Suite (JTS)
 * or ESRI API.
 *
 * <p>In current version, this class is defined solely for tracking geometries implementations or wrappers
 * in Apache SIS code base. {@code AbstractGeometry} API will be expanded in future versions, in particular
 * by implementing the {@link org.opengis.geometry.Geometry} interface. This work is pending GeoAPI revisions
 * (as of GeoAPI 3.0, the {@code Geometry} interface has not been updated to latest ISO standards).</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class AbstractGeometry {
    /**
     * Creates a new geometry.
     */
    protected AbstractGeometry() {
    }
}
