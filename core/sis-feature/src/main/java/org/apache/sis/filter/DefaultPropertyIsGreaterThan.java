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

import org.opengis.filter.FilterVisitor;
import org.opengis.filter.MatchAction;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.expression.Expression;

/**
 * Immutable "is greater than" filter.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class DefaultPropertyIsGreaterThan extends AbstractBinaryComparisonOperator implements PropertyIsGreaterThan {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1944045883935206615L;

    public DefaultPropertyIsGreaterThan(final Expression left, final Expression right, final boolean match, final MatchAction matchAction) {
        super(left,right,match,matchAction);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean evaluateOne(Object l, Object r) {
        final Integer v = compare(l,r);
        return (v == null) ? false : (v > 0) ;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object accept(final FilterVisitor visitor, final Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    protected char symbol() {
        return '>';
    }

}
