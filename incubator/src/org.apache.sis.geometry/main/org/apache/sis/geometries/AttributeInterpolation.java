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
 * Defines how geometry attribute should be interpolation on the curves, surfaces or in volumes.
 * This information will be used by geometry transforming operations.
 *
 * TODO : experimentale but needed
 *
 * @author Johann Sorel (Geomatys)
 */
public enum AttributeInterpolation {

    /**
     * Used for any attribute which is in the geometry coordinate reference system.
     * Attribute must be in a compatible CRS.
     */
    TRANSLATE,
    /**
     * Computed value should be the value of the nearest geometric point
     */
    NEAREST,
    /**
     * Computed value should be linearly interpolated between geometric points.
     * It must follow the curve (distance along the curve) or surface.
     */
    LINEAR,
    /**
     * An attribute value applies to the whole segment until the next point.
     * For a curve of two points [A..B] this means the value should be used on the [A..B[ interval, excluding B.
     * For a surface or volume, the first attribute value applies to the whole surface or volume.
     */
    LEADING

}
