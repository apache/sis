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

import org.opengis.util.FactoryException;


/**
 * Thrown when a factory {@code createFoo(…)} method is given invalid parameters.
 * This exception may be thrown by factories that create an object from geodetic parameters
 * like semi-major or semi-minor axis length, latitude of natural origin, <i>etc</i>.
 * The cause may be a parameter having an illegal value, or a mandatory parameter which has not been specified.
 *
 * <div class="note"><b>Note:</b>
 * this exception is not for invalid authority codes. For such cases, see
 * {@link org.opengis.referencing.NoSuchAuthorityCodeException} instead.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class InvalidGeodeticParameterException extends FactoryException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -546427967147139788L;

    /**
     * Construct an exception with no detail message.
     */
    public InvalidGeodeticParameterException() {
    }

    /**
     * Construct an exception with the specified detail message.
     *
     * @param  message The detail message. The detail message is saved
     *         for later retrieval by the {@link #getMessage()} method.
     */
    public InvalidGeodeticParameterException(String message) {
        super(message);
    }

    /**
     * Construct an exception with the specified cause.
     *
     * <p>This constructor is not public because its behavior is slightly different than the default JDK behavior:
     * the message is set to the throwable message instead than to {@code throwable.toString()}.</p>
     *
     * @param  cause The cause for this exception. The cause is saved
     *         for later retrieval by the {@link #getCause()} method.
     */
    InvalidGeodeticParameterException(Throwable cause) {
        super(cause.getLocalizedMessage(), cause);
    }

    /**
     * Construct an exception with the specified detail message and cause.
     *
     * @param  message The detail message. The detail message is saved
     *         for later retrieval by the {@link #getMessage()} method.
     * @param  cause The cause for this exception. The cause is saved
     *         for later retrieval by the {@link #getCause()} method.
     */
    public InvalidGeodeticParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
