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

import java.util.Locale;
import org.apache.sis.internal.storage.Resources;
import org.opengis.util.GenericName;


/**
 * Thrown when a store can not write the given feature because its type is not one of the supported types.
 * The {@link org.opengis.feature.FeatureType} is given by {@link org.opengis.feature.Feature#getType()},
 * and the type expected by the data store is given by {@link FeatureSet#getType()}. Those two values must
 * match, except when the type of the feature set is {@linkplain WritableFeatureSet#updateType updated}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class IllegalFeatureTypeException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1426887859737756607L;

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public IllegalFeatureTypeException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause and no details message.
     *
     * @param cause  the cause for this exception.
     */
    public IllegalFeatureTypeException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public IllegalFeatureTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with a default message in the given locale.
     *
     * @param locale     the message locale.
     * @param format     short name of the format that do not accept the given feature type.
     * @param dataType   name of the feature type that can not be accepted by the data store.
     */
    public IllegalFeatureTypeException(final Locale locale, final String format, final GenericName dataType) {
        super(locale, Resources.Keys.IllegalFeatureType_2, format, dataType);
    }
}
