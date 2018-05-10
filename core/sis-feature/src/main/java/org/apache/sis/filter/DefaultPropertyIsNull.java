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
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Expression;


/**
 * Filter operator that checks if an expression's value is {@code null}.  A {@code null}
 * is equivalent to no value present. The value 0 is a valid value and is not considered
 * {@code null}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class DefaultPropertyIsNull extends AbstractUnaryOperator implements PropertyIsNull, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3942075458551232678L;

    /**
     * Creates a new operator.
     * It is caller responsibility to ensure that no argument is null.
     */
    DefaultPropertyIsNull(final Expression expression) {
        super(expression);
    }

    /**
     * Returns the null symbol, to be used in string representation.
     */
    @Override
    protected char symbol() {
        return '∅';
    }

    /**
     * Returns {@code true} if the given value evaluates to {@code null}.
     */
    @Override
    public boolean evaluate(final Object object) {
        return expression.evaluate(object) == null;
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public Object accept(final FilterVisitor visitor, final Object extraData) {
        return visitor.visit(this, extraData);
    }
}
