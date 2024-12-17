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
package org.apache.sis.storage;

import java.util.Set;


/**
 * Thrown when a write operation cannot be performed because the resource to write
 * is incompatible with the data store.
 * For example, the file format may have restrictions that prevent the encoding of the coordinate
 * reference system used by the resource. The {@link #getAspects()} method can help to identify
 * which aspects (class, <abbr>CRS</abbr>, <i>etc.</i>) are the causes of the incompatibility.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.2
 */
public class IncompatibleResourceException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1833794980891065300L;

    /**
     * Identification of which aspects are incompatible, or {@code null} if none.
     * If non-null, this is usually a singleton set.
     */
    @SuppressWarnings("serial")
    private Set<String> aspects;

    /**
     * Creates an exception with no cause and no details message.
     */
    public IncompatibleResourceException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public IncompatibleResourceException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public IncompatibleResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Adds an identification of the aspect which is the cause of the incompatibility.
     * It should be the name of a property such as {@code "crs"} or {@code "gridToCRS"}.
     * See {@link #getAspects()} for a list of suggested values.
     *
     * @param  name  an identification of the aspect which is incompatible.
     * @return {@code this} for method call chaining.
     * @since 1.5
     */
    public IncompatibleResourceException addAspect(final String name) {
        if (aspects == null) {
            aspects = Set.of(name);
        } else {
            // Inefficient, but rarely used.
            final int n = aspects.size();
            String[] names = aspects.toArray(new String[n+1]);
            names[n] = name;
            aspects = Set.of(names);
        }
        return this;
    }

    /**
     * Returns identifications of the aspects which are causes of the incompatibility.
     * Some values are:
     *
     * <ul>
     *   <li>{@code "class"}:        the resources is not an instance of the class expected by the writer.</li>
     *   <li>{@code "crs"}:          the coordinate reference system cannot be encoded.</li>
     *   <li>{@code "gridToCRS"}:    the "grid to <abbr>CRS</abbr>" component of the grid geometry of a raster cannot be encoded.</li>
     *   <li>{@code "gridGeometry"}: the grid geometry of a raster cannot be encoded for reason less specific than {@code gridToCRS}.</li>
     *   <li>{@code "raster"}:       the raster data cannot be encoded.</li>
     *   <li>{@code "unit"}:         the unit of measurement cannot be encoded.</li>
     * </ul>
     *
     * @return identifications of aspects which are incompatible.
     * @since 1.5
     */
    public Set<String> getAspects() {
        return (aspects != null) ? aspects : Set.of();
    }
}
