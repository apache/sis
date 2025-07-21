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
package org.apache.sis.filter;


/**
 * Exception thrown when the XPath in an expression is invalid or unsupported.
 * Apache SIS currently supports only a small subset of XPath syntax, mostly paths of
 * the form {@code "a/b/c"} (and not everywhere) and the {@code "Q{namespace}"} syntax.
 *
 * <h4>Relationship with standard libraries</h4>
 * The standard Java libraries provides a {@link javax.xml.xpath.XPathException}.
 * This {@code InvalidXPathException} differs in that it is an unchecked exception
 * and is thrown in the context of operations with OGC filters and expressions.
 * In some implementations, {@code InvalidXPathException} may have a {@code XPathException} as its cause.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see javax.xml.xpath.XPathException
 *
 * @since 1.5
 */
public class InvalidXPathException extends IllegalArgumentException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1654277877397802378L;

    /**
     * Creates an exception with no message.
     */
    public InvalidXPathException() {
        super();
    }

    /**
     * Creates an exception with the specified message.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     */
    public InvalidXPathException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause.
     *
     * @param cause  the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public InvalidXPathException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified message and cause.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     * @param cause    the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public InvalidXPathException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
