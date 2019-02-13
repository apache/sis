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
import org.opengis.filter.expression.BinaryExpression;
import org.opengis.filter.expression.Expression;


/**
 * Base class for expressions performing operations on two values.
 * The nature of the operation is dependent on the subclass.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class AbstractBinaryExpression extends AbstractExpression implements BinaryExpression, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4430693449544064634L;

    /**
     * The first of the two expressions to be used by this operator.
     *
     * @see #getExpression1()
     */
    protected final Expression expression1;

    /**
     * The second of the two expressions to be used by this operator.
     *
     * @see #getExpression2()
     */
    protected final Expression expression2;

    /**
     * Creates a new binary operator.
     * It is caller responsibility to ensure that no argument is null.
     */
    AbstractBinaryExpression(final Expression expression1, final Expression expression2) {
        this.expression1 = expression1;
        this.expression2 = expression2;
    }

    /**
     * Returns the mathematical symbol for this binary operator.
     * For comparison operators, the symbol should be one of the following:
     * {@literal < > ≤ ≥ = ≠}.
     */
    protected abstract char symbol();

    /**
     * Returns the first of the two expressions to be used by this operator.
     */
    @Override
    public final Expression getExpression1() {
        return expression1;
    }

    /**
     * Returns the second of the two expressions to be used by this operator.
     */
    @Override
    public final Expression getExpression2() {
        return expression2;
    }

    /**
     * Returns a hash code value for this operator.
     */
    @Override
    public int hashCode() {
        // We use the symbol as a way to differentiate the subclasses.
        return (31 * expression1.hashCode() + expression2.hashCode()) ^ symbol();
    }

    /**
     * Compares this operator with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            final AbstractBinaryExpression other = (AbstractBinaryExpression) obj;
            return expression1.equals(other.expression1) &&
                   expression2.equals(other.expression2);
        }
        return false;
    }

    /**
     * Returns a string representation of this operator.
     */
    @Override
    public String toString() {
        return new StringBuilder(30).append(expression1).append(' ').append(symbol()).append(' ')
                                    .append(expression2).toString();
    }
}
