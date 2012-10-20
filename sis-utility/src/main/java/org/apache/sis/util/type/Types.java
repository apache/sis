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
package org.apache.sis.util.type;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.MissingResourceException;
import java.io.IOException;
import java.io.InputStream;

import org.opengis.annotation.UML;
import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Static;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Convenience methods working on GeoAPI types.
 * The methods in this class can be used for:
 *
 * <ul>
 *   <li>Creating {@link InternationalString} instances from {@link CharSequence} instances.</li>
 *   <li>Mapping ISO identifiers to the GeoAPI types (interfaces or {@linkplain CodeList code lists}).</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.19)
 * @version 0.3
 * @module
 */
public final class Types extends Static {
    /**
     * The types for ISO 19115 UML identifiers. The keys are UML identifiers. Values
     * are either class names as {@link String} objects, or the {@link Class} instances.
     *
     * @see #forName(String)
     */
    private static Map<Object,Object> typeForNames;

    /**
     * Do not allow instantiation of this class.
     */
    private Types() {
    }

    /**
     * Returns the ISO name for the given class, or {@code null} if none.
     * Examples:
     *
     * <ul>
     *   <li><code>getStandardName({@linkplain org.opengis.metadata.citation.Citation}.class)</code>   returns {@code "CI_Citation"}.</li>
     *   <li><code>getStandardName({@linkplain org.opengis.referencing.cs.AxisDirection}.class)</code> returns {@code "CS_AxisDirection"}. </li>
     * </ul>
     *
     * @param  type The GeoAPI interface from which to get the ISO name, or {@code null}.
     * @return The ISO name for the given interface, or {@code null} if none or if the given type is {@code null}.
     */
    public static String getStandardName(final Class<?> type) {
        if (type != null) {
            final UML uml = type.getAnnotation(UML.class);
            if (uml != null) {
                return uml.identifier();
            }
        }
        return null;
    }

    /**
     * Returns the GeoAPI interface for the given ISO name, or {@code null} if none.
     * The identifier argument shall be the value documented in the {@link UML#identifier()}
     * annotation associated with the GeoAPI interface.
     * Examples:
     *
     * <ul>
     *   <li>{@code forStandardName("CI_Citation")}      returns <code>{@linkplain org.opengis.metadata.citation.Citation}.class</code></li>
     *   <li>{@code forStandardName("CS_AxisDirection")} returns <code>{@linkplain org.opengis.referencing.cs.AxisDirection}.class</code></li>
     * </ul>
     *
     * Only identifiers for the stable part of GeoAPI are recognized. This method does not handle
     * the identifiers for the {@code geoapi-pending} module.
     *
     * @param  identifier The ISO {@linkplain UML} identifier, or {@code null}.
     * @return The GeoAPI interface, or {@code null} if the given identifier is {@code null} or unknown.
     */
    public static synchronized Class<?> forStandardName(final String identifier) {
        if (identifier == null) {
            return null;
        }
        if (typeForNames == null) {
            final Class<UML> c = UML.class;
            final InputStream in = c.getResourceAsStream("class-index.properties");
            if (in == null) {
                throw new MissingResourceException("class-index.properties", c.getName(), identifier);
            }
            final Properties props = new Properties();
            try {
                props.load(in);
                in.close();
            } catch (IOException | IllegalArgumentException e) {
                throw new BackingStoreException(e);
            }
            typeForNames = new HashMap<>(props);
        }
        final Object value = typeForNames.get(identifier);
        if (value == null || value instanceof Class<?>) {
            return (Class<?>) value;
        }
        final Class<?> type;
        try {
            type = Class.forName((String) value);
        } catch (ClassNotFoundException e) {
            throw new TypeNotPresentException((String) value, e);
        }
        typeForNames.put(identifier, type);
        return type;
    }

    /**
     * Returns the given characters sequence as an international string. If the given sequence is
     * null or an instance of {@link InternationalString}, this this method returns it unchanged.
     * Otherwise, this method copies the {@link InternationalString#toString()} value in a new
     * {@link SimpleInternationalString} instance and returns it.
     *
     * @param  string The characters sequence to convert, or {@code null}.
     * @return The given sequence as an international string,
     *         or {@code null} if the given sequence was null.
     */
    public static InternationalString toInternationalString(final CharSequence string) {
        if (string == null || string instanceof InternationalString) {
            return (InternationalString) string;
        }
        return new SimpleInternationalString(string.toString());
    }

    /**
     * Returns the given array of {@code CharSequence}s as an array of {@code InternationalString}s.
     * If the given array is null or an instance of {@code InternationalString[]}, then this method
     * returns it unchanged. Otherwise a new array of type {@code InternationalString[]} is created
     * and every elements from the given array is copied or
     * {@linkplain #toInternationalString(CharSequence) converted} in the new array.
     *
     * <p>If a defensive copy of the {@code strings} array is wanted, then the caller needs to check
     * if the returned array is the same instance than the one given in argument to this method.</p>
     *
     * @param  strings The characters sequences to convert, or {@code null}.
     * @return The given array as an array of type {@code InternationalString[]},
     *         or {@code null} if the given array was null.
     */
    public static InternationalString[] toInternationalStrings(final CharSequence... strings) {
        if (strings == null || strings instanceof InternationalString[]) {
            return (InternationalString[]) strings;
        }
        final InternationalString[] copy = new InternationalString[strings.length];
        for (int i=0; i<strings.length; i++) {
            copy[i] = toInternationalString(strings[i]);
        }
        return copy;
    }
}
