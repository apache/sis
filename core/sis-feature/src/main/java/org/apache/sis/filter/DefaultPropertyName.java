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

import java.util.Map;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.PropertyName;

/**
 * Immutable property name expression.
 * A property name does not store any value, it acts as an indirection to a
 * property value of the evaluated feature.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class DefaultPropertyName extends AbstractExpression implements PropertyName {

    private static final long serialVersionUID = -8474562134021521300L;

    private final String property;

    /**
     * 
     * @param property attribute name
     */
    public DefaultPropertyName(final String property) {
        ensureNonNull("property name", property);
        this.property = property;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getPropertyName() {
        return property;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object evaluate(final Object candidate) {

        if(candidate instanceof Feature){
            try{
                return ((Feature) candidate).getPropertyValue(property);
            }catch(PropertyNotFoundException ex){
                return null;
            }
        }else if(candidate instanceof Map){
            return ((Map) candidate).get(property);
        }

        return null;
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
    public String toString() {
        return "{"+property+"}";
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
        final DefaultPropertyName other = (DefaultPropertyName) obj;
        if ((this.property == null) ? (other.property != null) : !this.property.equals(other.property)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (this.property != null ? this.property.hashCode() : 0);
        return hash;
    }
}
