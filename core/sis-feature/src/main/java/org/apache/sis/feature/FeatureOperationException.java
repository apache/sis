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
package org.apache.sis.feature;

import org.apache.sis.util.LocalizedException;
import org.opengis.util.InternationalString;


/**
 * Thrown when a property value can not be computed.
 * This exception may occur during a call to {@link AbstractAttribute#getValue()} on an attribute
 * instance which computes its value dynamically instead of returning a stored value.
 * It may be for example the attributes produced by {@link FeatureOperations}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class FeatureOperationException extends IllegalStateException implements LocalizedException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7281160433831489357L;

    /**
     * A description of the computation error.
     */
    private final InternationalString message;

    /**
     * Creates a new exception with the given explanation message.
     *
     * @param message  a description of the computation error.
     */
    FeatureOperationException(final InternationalString message) {
        super(message.toString());
        this.message = message;
    }

    /**
     * Creates a new exception with the given explanation message and cause.
     *
     * @param message  a description of the computation error.
     * @param cause    the cause of the error.
     */
    FeatureOperationException(final InternationalString message, final Exception cause) {
        super(message.toString(), cause);
        this.message = message;
    }

    /**
     * Returns the message in various locales.
     *
     * @return the exception message.
     */
    @Override
    public InternationalString getInternationalMessage() {
        return message;
    }
}
