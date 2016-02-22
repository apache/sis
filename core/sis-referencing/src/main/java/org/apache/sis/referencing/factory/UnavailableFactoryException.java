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

import org.opengis.referencing.AuthorityFactory;


/**
 * Thrown when a factory can not be created because a resource is missing.
 * The most common case is when the {@link org.apache.sis.referencing.factory.epsg.EPSGFactory}
 * has no connection to an EPSG database.
 *
 * <div class="section">Relationship with other exceptions</div>
 * This exception means that the whole factory is unusable.
 * By contrast, {@link MissingFactoryResourceException} means that at least one particular object
 * can not be created, but other objects may be okay.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see ConcurrentAuthorityFactory#newDataAccess()
 */
public class UnavailableFactoryException extends MissingFactoryResourceException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -661925454228937249L;

    /**
     * The factory that can not perform its work, or {@code null} if unknown.
     */
    private AuthorityFactory factory;

    /**
     * Construct an exception with no detail message.
     */
    public UnavailableFactoryException() {
    }

    /**
     * Construct an exception with the specified detail message.
     *
     * @param  message The detail message. The detail message is saved
     *         for later retrieval by the {@link #getMessage()} method.
     */
    public UnavailableFactoryException(String message) {
        super(message);
    }

    /**
     * Construct an exception with the specified detail message and cause.
     * The cause is the exception thrown in the underlying database
     * (e.g. {@link java.io.IOException} or {@link java.sql.SQLException}).
     *
     * @param  message The detail message. The detail message is saved
     *         for later retrieval by the {@link #getMessage()} method.
     * @param  cause The cause for this exception. The cause is saved
     *         for later retrieval by the {@link #getCause()} method.
     */
    public UnavailableFactoryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Specifies which factory is unavailable. This information can be provided when the exception occurred
     * at some later stage <em>after</em> the factory construction (never inside the factory constructor),
     * for example the first time that the factory tried to create an object.
     *
     * <div class="note"><b>Example:</b>
     * {@link org.apache.sis.referencing.factory.sql.EPSGFactory} may have been successfully created with
     * a valid {@link javax.sql.DataSource}. But the call to {@link javax.sql.DataSource#getConnection()}
     * happens only later (the first time that user invokes a method requiring a search in the database).
     * In case of failure to connect to the database, user may discover late that the factory is actually
     * unavailable. User may want to be informed about which factory is unavailable, for example in order
     * to remove it from the list of factory managed by {@link MultiAuthoritiesFactory}.</div>
     *
     * @param factory The factory which is unavailable.
     */
    public void setUnavailableFactory(final AuthorityFactory factory) {
        this.factory = factory;
    }

    /**
     * Returns the factory which has been found unavailable, or {@code null} if unspecified.
     * See {@link #setUnavailableFactory(AuthorityFactory)} for more details.
     *
     * @return The factory that should be considered as unavailable.
     */
    public AuthorityFactory getUnavailableFactory() {
        return factory;
    }
}
