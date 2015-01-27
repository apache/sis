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

import org.apache.sis.util.resources.Errors;


/**
 * Thrown when an operation can not use arbitrary implementation of an interface,
 * and a given instance does not meet the requirement. For example this exception
 * may be thrown when an operation requires an Apache SIS implementation of a
 * <a href="http://www.geoapi.org">GeoAPI</a> interface.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class UnsupportedImplementationException extends UnsupportedOperationException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8871937175259200449L;

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param message The detail message, or {@code null} if none.
     */
    public UnsupportedImplementationException(final String message) {
        super(message);
    }

    /**
     * Constructs an exception with an error message formatted for the specified class.
     *
     * @param classe The unexpected implementation class.
     */
    public UnsupportedImplementationException(final Class<?> classe) {
        super(Errors.format(Errors.Keys.UnsupportedImplementation_1, classe));
    }

    /**
     * Constructs an exception with an error message formatted for the specified class
     * and a cause.
     *
     * @param classe The unexpected implementation class.
     * @param cause  The cause for the exception, or {@code null} if none.
     */
    public UnsupportedImplementationException(final Class<?> classe, final Exception cause) {
        super(Errors.format(Errors.Keys.UnsupportedImplementation_1, classe), cause);
    }
}
