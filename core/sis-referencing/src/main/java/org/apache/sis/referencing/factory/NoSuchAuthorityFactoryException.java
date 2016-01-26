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
package org.apache.sis.referencing.factory;

import org.opengis.referencing.NoSuchAuthorityCodeException;


/**
 * Thrown when no factory has been found for a given authority name.
 * This exception is a little bit more specific than {@link NoSuchAuthorityCodeException}
 * since it means that in a code like {@code "FOO:456"}, the unrecognized part was {@code "FOO"}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class NoSuchAuthorityFactoryException extends NoSuchAuthorityCodeException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -871607314555498523L;

    /**
     * Constructs an exception with the specified detail message and authority name.
     *
     * @param  message   The detail message, saved for later retrieval by the {@link #getMessage()} method.
     * @param  authority The authority, saved for retrieval by the {@link #getAuthority()} method.
     */
    public NoSuchAuthorityFactoryException(final String message, final String authority) {
        super(message, authority, null, authority);
    }
}
