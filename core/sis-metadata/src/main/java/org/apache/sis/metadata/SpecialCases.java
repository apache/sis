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
package org.apache.sis.metadata;

import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Substitute on-the-fly the values of some properties handled in a special way.
 * The current implementation handles only the longitude and latitude bounds of
 * {@link GeographicBoundingBox}, which are returned as {@link Longitude} or
 * {@link Latitude} instances instead of {@link Double}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
final class SpecialCases extends PropertyAccessor {
    /**
     * Index of properties to handle in a special way.
     */
    private final int westBoundLongitude, eastBoundLongitude, southBoundLatitude, northBoundLatitude;

    /**
     * Creates a new property accessor for the specified metadata implementation.
     *
     * @param  standard The standard which define the {@code type} interface.
     * @param  type The interface implemented by the metadata, which must be
     *         the value returned by {@link #getStandardType(Class, String)}.
     * @param  implementation The class of metadata implementations, or {@code type} if none.
     */
    SpecialCases(final Citation standard, final Class<?> type, final Class<?> implementation) {
        super(standard, type, implementation);
        assert isSpecialCase(type) : type;
        westBoundLongitude = indexOf("westBoundLongitude", true);
        eastBoundLongitude = indexOf("eastBoundLongitude", true);
        southBoundLatitude = indexOf("southBoundLatitude", true);
        northBoundLatitude = indexOf("northBoundLatitude", true);
    }

    /**
     * Returns {@code true} if the given class is a special case handled by the {@link SpecialCases} class.
     *
     * @param  type The interface implemented by the metadata.
     * @return {@code true} if the given type is a special case.
     */
    static boolean isSpecialCase(final Class<?> type) {
        return type == GeographicBoundingBox.class;
    }

    /**
     * Delegates to {@link PropertyAccessor#type(int)}, then substitutes the type for the properties
     * handled in a special way.
     */
    @Override
    Class<?> type(final int index, final TypeValuePolicy policy) {
        Class<?> type = super.type(index, policy);
        switch (policy) {
            case PROPERTY_TYPE:
            case ELEMENT_TYPE: {
                if (index == westBoundLongitude || index == eastBoundLongitude) {
                    type = Longitude.class;
                } else if (index == southBoundLatitude || index == northBoundLatitude) {
                    type = Latitude.class;
                }
                break;
            }
        }
        return type;
    }

    /**
     * Delegates to {@link PropertyAccessor#get(int, Object)}, then substitutes the value for the properties
     * handled in a special way.
     */
    @Override
    Object get(final int index, final Object metadata) throws BackingStoreException {
        Object value = super.get(index, metadata);
        if (value != null) {
            if (index == westBoundLongitude || index == eastBoundLongitude) {
                value = new Longitude((Double) value);
            } else if (index == southBoundLatitude || index == northBoundLatitude) {
                value = new Latitude((Double) value);
            }
        }
        return value;
    }

    /**
     * Substitutes the value for the properties handled in a special way, then delegates to
     * {@link #set(int, Object, Object, int)}.
     */
    @Override
    Object set(final int index, final Object metadata, Object value, final int mode)
            throws UnmodifiableMetadataException, ClassCastException, BackingStoreException
    {
        if ((index == westBoundLongitude || index == eastBoundLongitude)) {
            if (value instanceof Longitude) {
                value = ((Longitude) value).degrees();
            }
            value = super.set(index, metadata, value, mode);
            if (value != null) {
                value = new Longitude((Double) value);
            }
            return value;
        } else if ((index == southBoundLatitude || index == northBoundLatitude)) {
            if (value instanceof Latitude) {
                value = ((Latitude) value).degrees();
            }
            value = super.set(index, metadata, value, mode);
            if (value != null) {
                value = new Latitude((Double) value);
            }
            return value;
        } else {
            return super.set(index, metadata, value, mode);
        }
    }
}
