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
package org.apache.sis.util.collection;

import java.io.IOException;
import java.sql.SQLException;


/**
 * Thrown to indicate that an operation could not complete because of a failure in the backing store
 * (a file or a database). This exception is thrown by implementations of API (collection, streams,
 * <i>etc.</i> that are not allowed to throw checked exceptions.
 * This exception usually has an {@link IOException} or a {@link SQLException} as its {@linkplain #getCause() cause}.
 *
 * <p>This method provides a {@link #unwrapOrRethrow(Class)} convenience method which can be used
 * for re-throwing the cause as in the example below. This allows client code to behave as if a
 * {@link java.util.Collection} interface was allowed to declare checked exceptions.</p>
 *
 * {@preformat java
 *     void myMethod() throws IOException {
 *         Collection c = ...;
 *         try {
 *             c.doSomeStuff();
 *         } catch (BackingStoreException e) {
 *             throw e.unwrapOrRethrow(IOException.class);
 *         }
 *     }
 * }
 *
 * <h2>Relationship with {@code java.io.UncheckedIOException}</h2>
 * JDK8 provides a {@link java.io.UncheckedIOException} which partially overlaps
 * the purpose of this {@code BackingStoreException}. While Apache SIS still uses
 * {@code BackingStoreException} as a general mechanism for any kind of checked exceptions,
 * client code would be well advised to catch both kind of exceptions for robustness.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   0.3
 * @module
 */
public class BackingStoreException extends RuntimeException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4549821631559359838L;

    /**
     * Constructs a new exception with no detail message.
     */
    public BackingStoreException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     */
    public BackingStoreException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause  the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public BackingStoreException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     * @param cause    the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public BackingStoreException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the underlying {@linkplain #getCause() cause} as an exception of the given type,
     * or re-throw the exception. More specifically, this method makes the following choices:
     *
     * <ul>
     *   <li>If the cause {@linkplain Class#isInstance(Object) is an instance} of the given type, returns the cause.</li>
     *   <li>Otherwise if the cause is an instance of {@link RuntimeException}, throws that exception.</li>
     *   <li>Otherwise re-throws {@code this}.</li>
     * </ul>
     *
     * This method should be used as in the example below:
     *
     * {@preformat java
     *     void myMethod() throws IOException {
     *         Collection c = ...;
     *         try {
     *             c.doSomeStuff();
     *         } catch (BackingStoreException e) {
     *             throw e.unwrapOrRethrow(IOException.class);
     *         }
     *     }
     * }
     *
     * If this exception has {@linkplain #getSuppressed() suppressed exceptions} and this method decided
     * that this exception should be discarded in favor of {@code <E>} or {@link RuntimeException} cause,
     * then this method copies the suppressed exceptions into the cause before to throw the cause.
     *
     * @param  <E>   the type of the exception to unwrap.
     * @param  type  the type of the exception to unwrap.
     * @return the cause as an exception of the given type (never {@code null}).
     * @throws RuntimeException if the cause is an instance of {@code RuntimeException},
     *         in which case that instance is re-thrown.
     * @throws BackingStoreException if the cause is neither the given type or an instance
     *         of {@link RuntimeException}, in which case {@code this} exception is re-thrown.
     */
    @SuppressWarnings("unchecked")
    public <E extends Exception> E unwrapOrRethrow(final Class<E> type)
            throws RuntimeException, BackingStoreException
    {
        final Throwable cause = getCause();
        if (type.isInstance(cause)) {
            copySuppressed(cause);
            return (E) cause;
        } else if (cause instanceof RuntimeException) {
            copySuppressed(cause);
            throw (RuntimeException) cause;
        } else {
            throw this;
        }
    }

    /**
     * Copies suppressed exceptions to the given target. This method is invoked before the cause is re-thrown.
     * Current version does not verify that this copy operation does not create duplicated values.
     * Most of the time, this exception has no suppressed exceptions and this method does nothing.
     *
     * <p>This copy operation is useful if a {@link BackingStoreException} was thrown inside a try-with-resource
     * block, especially when the {@link AutoCloseable} is a {@link java.util.stream.Stream}.</p>
     */
    private void copySuppressed(final Throwable cause) {
        for (final Throwable s : getSuppressed()) {
            cause.addSuppressed(s);
        }
    }
}
