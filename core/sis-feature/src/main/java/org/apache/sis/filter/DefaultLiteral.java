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

import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Literal;

/**
 * Immutable literal expression.
 *
 * @author Johann Sorel (Geomatys)
 * @param <T> literal value type
 * @since   0.7
 * @version 0.7
 * @module
 */
public class DefaultLiteral<T> extends AbstractExpression implements Literal {

    private static final long serialVersionUID = 3240145927452086297L;

    private final T value;

    /**
     *
     * @param value literal value
     */
    public DefaultLiteral(final T value) {
        this.value = value;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public T evaluate(final Object candidate) {
        return value;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object accept(final ExpressionVisitor visitor, final Object extraData) {
        return visitor.visit(this, extraData);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public T getValue() {
        return value;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return value == null ? "NULL" : value.toString();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultLiteral<T> other = (DefaultLiteral<T>) obj;

        if (this.value == null && other.value == null) {
            return true;
        }
        if (this.value == null) {
            return false;
        }
        if (other.value == null) {
            return false;
        }

        final Object otherVal;
        try{
            otherVal = ObjectConverters.convert(other.value, this.value.getClass());
        }catch(UnconvertibleObjectException ex){
            //type can not be matched
            return false;
        }
        return this.value.equals(otherVal);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.value != null ? this.value.hashCode() : 0);
        return hash;
    }

}
