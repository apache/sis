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

import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.internal.metadata.Resources;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Substitute on-the-fly the values of some ISO 19115 properties handled in a special way.
 * Current implementation handles the longitude and latitude bounds of {@link GeographicBoundingBox},
 * which are returned as {@link Longitude} or {@link Latitude} instances instead of {@link Double}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.4
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
     * @param  type            the interface implemented by the metadata, which must be
     *                         the value returned by {@link MetadataStandard#findInterface(CacheKey)}.
     * @param  implementation  the class of metadata implementations, or {@code type} if none.
     * @param  standardImpl    the implementation specified by the {@link MetadataStandard}, or {@code null} if none.
     *                         This is the same than {@code implementation} unless a custom implementation is used.
     */
    SpecialCases(final Class<?> type, final Class<?> implementation, final Class<?> standardImpl) {
        super(type, implementation, standardImpl);
        assert isSpecialCase(type) : type;
        westBoundLongitude = indexOf("westBoundLongitude", true);
        eastBoundLongitude = indexOf("eastBoundLongitude", true);
        southBoundLatitude = indexOf("southBoundLatitude", true);
        northBoundLatitude = indexOf("northBoundLatitude", true);
    }

    /**
     * Returns {@code true} if the given class is a special case handled by the {@link SpecialCases} class.
     *
     * @param  type  the interface implemented by the metadata.
     * @return {@code true} if the given type is a special case.
     */
    static boolean isSpecialCase(final Class<?> type) {
        return type == GeographicBoundingBox.class;
    }

    /**
     * Delegates to {@link PropertyAccessor#type(int, TypeValuePolicy)},
     * then substitutes the type for the properties handled in a special way.
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
     * Returns a remark or warning to format with the value at the given index, or {@code null} if none.
     * This is used for notifying the user that a geographic box is crossing the anti-meridian.
     */
    @Override
    CharSequence remarks(final int index, final Object metadata) {
        if (index == eastBoundLongitude) {
            Object east = super.get(index, metadata);
            if (east != null) {
                Object west = super.get(westBoundLongitude, metadata);
                if (west != null && Longitude.isWraparound((Double) west, (Double) east)) {
                    return Resources.formatInternational(Resources.Keys.BoxCrossesAntiMeridian);
                }
            }
        }
        return super.remarks(index, metadata);
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
                final double angle = (Double) value;
                value = Double.isNaN(angle) ? null : new Longitude(angle);
            } else if (index == southBoundLatitude || index == northBoundLatitude) {
                final double angle = (Double) value;
                value = Double.isNaN(angle) ? null : new Latitude(angle);
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

    /**
     * Returns {@code true} if the property at the given index is a {@code Map<Locale,Charset>}.
     */
    static boolean isLocaleAndCharset(final PropertyAccessor accessor, final int indexInData) {
        return accessor.isMap(indexInData) && accessor.type.getName().startsWith("org.opengis.metadata.")
                 && "localesAndCharsets".equals(accessor.name(indexInData, KeyNamePolicy.JAVABEANS_PROPERTY));
    }

    /**
     * Returns the identifier to use in replacement of the identifier given in {@link org.opengis.annotation.UML} annotations.
     * We usually want to use those identifiers as-is because they were specified by ISO standards, but we may an exception if
     * the identifier is actually a construction of two or more identifiers like {@code "defaultLocale+otherLocale"}.
     *
     * @param  name  the UML identifier(s) from ISO specification.
     * @return the potentially simplified identifier to use for displaying purpose.
     */
    static String rename(final String name) {
        if ("defaultLocale+otherLocale".equals(name)) {
            return "locale";
        }
        return name;
    }
}
