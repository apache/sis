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

import org.opengis.filter.expression.Expression;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Workaround;


/**
 * Thrown when an operation can not complete because an expression is illegal or unsupported.
 * The invalid {@link Expression} may be a component of a larger object such as another expression
 * or a query {@link org.apache.sis.storage.Query}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class InvalidExpressionException extends RuntimeException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2600709421042246855L;

    /**
     * Constructs an exception with no detail message.
     */
    public InvalidExpressionException() {
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param  message  the detail message.
     */
    public InvalidExpressionException(final String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified detail message and cause.
     *
     * @param  message  the detail message.
     * @param  cause    the cause for this exception.
     */
    public InvalidExpressionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an exception with a message saying that the given expression is illegal or unsupported.
     * This constructor assumes that the expression was part of a larger object containing many expressions
     * identified by indices.
     *
     * @param  expression  the illegal expression, or {@code null} if unknown.
     * @param  index       column number (or other kind of index) where the invalid expression has been found.
     */
    public InvalidExpressionException(final Expression expression, final int index) {
        super(message(expression, index));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static String message(final Expression expression, final int index) {
        final String name;
        if (expression instanceof Node) {
            name = ((Node) expression).getName();
        } else {
            name = Classes.getShortClassName(expression);
        }
        return Resources.format(Resources.Keys.InvalidExpression_2, name, index);
    }
}
