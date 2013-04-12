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
 * Interface of classes for which deprecated instances may exist. Deprecated instances exist in some
 * {@linkplain org.opengis.referencing.AuthorityFactory authority factories} like the EPSG database.
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
 * @since   0.3 (derived from geotk-3.20)
 * @version 0.3
 * @module
 */
public interface Deprecable {
    /**
     * Returns {@code true} if this instance is deprecated.
     *
     * @return {@code true} if this instance is deprecated.
     */
    boolean isDeprecated();

    /**
     * If this instance is deprecated, the reason or the alternative to use.
     * Otherwise, an optional free text.
     *
     * @return Comments about this instance, or {@code null} if none. Shall be the
     *         reason for deprecation or the alternative to use if this instance
     *         {@linkplain #isDeprecated() is deprecated}.
     */
    InternationalString getRemarks();
}
