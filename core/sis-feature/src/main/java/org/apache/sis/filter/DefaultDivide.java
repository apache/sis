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

import java.util.Collections;
import org.apache.sis.feature.DefaultAttributeType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;

/**
 * Division expression.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
final class DefaultDivide extends AbstractBinaryExpression implements Divide {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3857311165395402436L;

    private static final AttributeType<Number> EXPECTED_TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(DefaultAttributeType.NAME_KEY, NAME),
                                          Number.class, 1, 1, null, (AttributeType<?>[]) null);

    public DefaultDivide(Expression expressoin1, Expression expression2) {
        super(expressoin1, expression2);
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public Object accept(ExpressionVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    protected char symbol() {
        return '/';
    }

    @Override
    public Object evaluate(Object object) {
        final Double val1 = expression1.evaluate(object, Double.class);
        final Double val2 = expression2.evaluate(object, Double.class);

        if (val1 == null || val2 == null) {
            return null;
        }

        return val1 / val2;
    }

    @Override
    public PropertyType expectedType(FeatureType type) {
        return EXPECTED_TYPE;
    }

}
