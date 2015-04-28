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
package org.apache.sis.util;

import org.opengis.util.InternationalString;


/**
 * Interface of classes for which deprecated instances may exist. Despite the name, the entities deprecated
 * by this interface are unrelated to the entities deprecated by the Java {@link Deprecated} annotation.
 * This interface is for identifying deprecated <em>data</em> rather than language constructs.
 *
 * <div class="note"><b>Example:</b>
 * When an error is discovered in the definition of a Coordinate Reference System (CRS) in the EPSG database,
 * the EPSG maintainers do not change the data. Instead, they deprecate the erroneous definition and create a
 * new one with a new EPSG code. The {@link #isDeprecated()} method in this interface allows users to identify
 * CRS instances created from such deprecated database records, for example in order to log a warning when data
 * are projected to a deprecated CRS.</div>
 *
 * Some examples of deprecated instances are:
 *
 * <ul>
 *   <li>An {@link org.apache.sis.referencing.AbstractIdentifiedObject} (typically a CRS)
 *       which has been built from a deprecated EPSG code.</li>
 *   <li>A {@link org.apache.sis.referencing.NamedIdentifier} containing the legacy name
 *       of an object which has been renamed.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public interface Deprecable {
    /**
     * Returns {@code true} if this instance is deprecated.
     * In such case, the {@linkplain #getRemarks() remarks} may contain information about the new object to use.
     *
     * @return {@code true} if this instance is deprecated.
     */
    boolean isDeprecated();

    /**
     * If this instance is deprecated, the reason or the alternative to use.
     * Otherwise, an optional free text.
     *
     * <div class="note"><b>Example:</b> "superseded by code XYZ".</div>
     *
     * @return Comments about this instance, or {@code null} if none. Shall be the
     *         reason for deprecation or the alternative to use if this instance
     *         {@linkplain #isDeprecated() is deprecated}.
     */
    InternationalString getRemarks();
}
