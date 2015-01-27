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


/**
 * Throws when a null argument has been given to a method that doesn't accept them.
 * This exception extends {@link NullPointerException} in order to stress out that
 * the error is an illegal argument rather than an unexpected usage of a null pointer
 * inside a method body.
 *
 * <div class="note"><b>API note:</b>
 * We could argue that this exception should extend {@link IllegalArgumentException}.
 * However {@link NullPointerException} has become a more widely adopted practice and
 * is now the recommended one in the <cite>Effective Java</cite> book.</div>
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see ArgumentChecks#ensureNonNull(String, Object)
 */
public class NullArgumentException extends NullPointerException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7924011726325288438L;

    /**
     * Constructs an exception with no detail message.
     */
    public NullArgumentException() {
        super();
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param message The detail message.
     */
    public NullArgumentException(final String message) {
        super(message);
    }
}
