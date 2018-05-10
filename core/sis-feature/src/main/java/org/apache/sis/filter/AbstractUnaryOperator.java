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

import java.io.Serializable;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;


/**
 * Base class for filters performing operations on one value.
 * The nature of the operation is dependent on the subclass.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class AbstractUnaryOperator implements Filter, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8183962550739028650L;

    /**
     * The expression to be used by this operator.
     *
     * @see #getExpression()
     */
    protected final Expression expression;

    /**
     * Creates a new unary operator.
     * It is caller responsibility to ensure that no argument is null.
     */
    AbstractUnaryOperator(final Expression expression) {
        this.expression = expression;
    }

    /**
     * Returns the mathematical symbol for this operator.
     */
    protected abstract char symbol();

    /**
     * Returns the expressions to be used by this operator.
     */
    public final Expression getExpression() {
        return expression;
    }

    /**
     * Returns a hash code value for this operator.
     */
    @Override
    public int hashCode() {
        // We use the symbol as a way to differentiate the subclasses.
        return expression.hashCode() ^ symbol();
    }

    /**
     * Compares this operator with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            return expression.equals(((AbstractUnaryOperator) obj).expression);
        }
        return false;
    }

    /**
     * Returns a string representation of this operator.
     */
    @Override
    public String toString() {
        return new StringBuilder(30).append(expression).append(':').append(symbol()).toString();
    }
}
