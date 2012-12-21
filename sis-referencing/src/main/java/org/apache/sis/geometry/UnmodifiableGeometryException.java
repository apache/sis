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
package org.apache.sis.geometry;


/**
 * Indicates that an operation is not allowed on a {@linkplain Geometry geometry} object
 * because it is unmodifiable. Note that unmodifiable geometries are not necessarily immutable;
 * they are just not allowed to be modified through the {@code setFoo(...)} method that
 * raised this exception. Whatever an unmodifiable geometry is immutable or not is
 * implementation dependent.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class UnmodifiableGeometryException extends UnsupportedOperationException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8679047625299612669L;

    /**
     * Creates an exception with no message.
     */
    public UnmodifiableGeometryException() {
        super();
    }

    /**
     * Creates an exception with the specified message.
     *
     * @param  message The detail message. The detail message is saved for
     *         later retrieval by the {@link #getMessage()} method.
     */
    public UnmodifiableGeometryException(final String message) {
        super(message);
    }
}
