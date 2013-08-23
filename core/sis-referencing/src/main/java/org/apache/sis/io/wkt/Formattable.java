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
package org.apache.sis.io.wkt;


/**
 * Interface for objects that can be formatted as <cite>Well Known Text</cite> (WKT).
 * {@link WKTFormat} checks for this interface at formatting time for each element to format.
 * When a {@code Formattable} element is found, its {@link #formatTo(Formatter)} method is invoked
 * for allowing the element to control its formatting.
 *
 * {@note This interface follows a design very similar to <code>java.util.Formattable</code>.}
 *
 * <p>Most SIS implementations extend {@link FormattableObject} instead than implementing directly
 * this interface.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public interface Formattable {
    /**
     * Formats the inner part of this <cite>Well Known Text</cite> (WKT) element into the given formatter.
     * This method is automatically invoked by {@link WKTFormat} when a formattable element is found.
     *
     * <p>Element keyword and {@linkplain org.apache.sis.referencing.IdentifiedObjects#getIdentifierCode
     * authority code} shall not be formatted here. For example if this formattable element is for a
     * {@code GEOGCS} element, then this method shall write the content starting at the insertion point
     * shows below:</p>
     *
     * {@preformat text
     *     GEOGCS["WGS 84", AUTHORITY["EPSG","4326"]]
     *                    ↑
     *            (insertion point)
     * }
     *
     * @param  formatter The formatter where to format the inner content of this WKT element.
     * @return The WKT element keyword (e.g. {@code "GEOGCS"}).
     */
    String formatTo(Formatter formatter);
}
