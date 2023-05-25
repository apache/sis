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
package org.apache.sis.referencing.operation.projection;

import java.util.regex.Pattern;


/**
 * Variant of the map projection used. This interface is implemented by enumerations
 * in {@link NormalizedProjection} sub-classes that support many variants.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 */
interface ProjectionVariant {
    /**
     * Returns the regular expression pattern to use for determining if the name of an operation method
     * identifies this variant.
     *
     * @return the operation name pattern for this variant, or {@code null} if none.
     */
    Pattern getOperationNamePattern();

    /**
     * Returns the EPSG identifier to compare against the operation method.
     * If non-null, the identifier is presumed in the EPSG namespace and has precedence over the pattern.
     *
     * @return EPSG identifier for this variant, or {@code null} if none.
     */
    String getIdentifier();

    /**
     * Whether this variant is a spherical variant using authalic radius.
     * This method can be overridden for handling authalic radius, but not conformance sphere radius.
     * The latter is handled by {@link NormalizedProjection.ParameterRole#LATITUDE_OF_CONFORMAL_SPHERE_RADIUS}.
     *
     * <h4>When to use</h4>
     * Authalic radius are used with Equal Area projections.
     * For other kinds of projection, the radius of conformal sphere is preferred.
     *
     * @return whether this variant is a spherical variant using authalic radius.
     *
     * @see NormalizedProjection.ParameterRole#LATITUDE_OF_CONFORMAL_SPHERE_RADIUS
     */
    default boolean useAuthalicRadius() {
        return false;
    }
}
