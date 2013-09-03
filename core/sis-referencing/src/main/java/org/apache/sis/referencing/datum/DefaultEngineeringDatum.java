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
package org.apache.sis.referencing.datum;

import java.util.Map;
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.datum.EngineeringDatum;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Immutable;
import org.apache.sis.io.wkt.Formatter;


/**
 * Defines the origin of an engineering coordinate reference system.
 * An engineering datum is used in a region around that origin.
 * This origin can be fixed with respect to the earth (such as a defined point at a construction site),
 * or be a defined point on a moving vehicle (such as on a ship or satellite).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.2)
 * @version 0.3
 * @module
 */
@Immutable
@XmlType(name = "EngineeringDatumType")
@XmlRootElement(name = "EngineeringDatum")
public class DefaultEngineeringDatum extends AbstractDatum implements EngineeringDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1498304918725248637L;

    /**
     * Constructs a new datum with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param datum The datum to copy.
     */
    public DefaultEngineeringDatum(final EngineeringDatum datum) {
        super(datum);
    }

    /**
     * Constructs an engineering datum from a name.
     *
     * @param name The datum name.
     */
    public DefaultEngineeringDatum(final String name) {
        this(Collections.singletonMap(NAME_KEY, name));
    }

    /**
     * Constructs an engineering datum from a set of properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
     *
     * @param properties Set of properties. Shall contains at least {@code "name"}.
     */
    public DefaultEngineeringDatum(final Map<String,?> properties) {
        super(properties);
    }

    /**
     * Returns a SIS datum implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultEngineeringDatum castOrCopy(final EngineeringDatum object) {
        return (object == null) || (object instanceof DefaultEngineeringDatum) ?
                (DefaultEngineeringDatum) object : new DefaultEngineeringDatum(object);
    }

    /**
     * Compares this datum with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        return  (object instanceof EngineeringDatum) && super.equals(object, mode);
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     */
    @Override
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        return super.hashCode(mode) ^ (int) serialVersionUID;
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> (WKT)</a> element.
     * The keyword is "{@code LOCAL_DATUM}" in WKT 1.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name, which is {@code "LOCAL_DATUM"}.
     */
    @Override
    public String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        return "LOCAL_DATUM";
    }
}
