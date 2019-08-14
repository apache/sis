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

import java.util.Collection;
import java.util.List;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import static org.apache.sis.util.ArgumentChecks.*;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;

/**
 * Abstract function.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class AbstractFunction extends Node implements Function {

    protected final String name;
    protected final List<Expression> parameters;
    protected final Literal fallback;

    public AbstractFunction(final String name, final Expression[] parameters, final Literal fallback) {
        ensureNonNull("name", name);
        this.name = name;
        this.parameters = UnmodifiableArrayList.wrap(parameters);
        this.fallback = fallback;
    }

    @Override
    protected Collection<?> getChildren() {
        return parameters;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<Expression> getParameters() {
        return parameters;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Literal getFallbackValue() {
        return fallback;
    }

    /**
     * {@inheritDoc }
     * Use the converters utility class to convert the default result object
     * to the wished class.
     */
    @Override
    public <T> T evaluate(final Object candidate, final Class<T> target) {
        final Object value = evaluate(candidate);
        if (target == null) {
            return (T) value; // TODO - unsafe cast!!!!
        }
        try {
            return ObjectConverters.convert(value, target);
        } catch (UnconvertibleObjectException ex) {
            return null;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object accept(final ExpressionVisitor visitor, final Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 89 * hash + (this.parameters != null ? this.parameters.hashCode() : 0);
        hash = 89 * hash + (this.fallback != null ? this.fallback.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractFunction other = (AbstractFunction) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.parameters != other.parameters && (this.parameters == null || !this.parameters.equals(other.parameters))) {
            return false;
        }
        if (this.fallback != other.fallback && (this.fallback == null || !this.fallback.equals(other.fallback))) {
            return false;
        }
        return true;
    }

}
