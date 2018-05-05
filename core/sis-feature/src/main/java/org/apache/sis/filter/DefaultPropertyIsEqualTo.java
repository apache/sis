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

import java.util.Objects;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.MatchAction;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;

/**
 * Immutable PropertyIsEqualTo filter.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class DefaultPropertyIsEqualTo extends AbstractComparisonOperator implements PropertyIsEqualTo {

    DefaultPropertyIsEqualTo(Expression exp1, Expression exp2, boolean matchCase, MatchAction matchAction) {
        super(exp1, exp2, matchCase, matchAction);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean evaluate(Object object) {
        final Object r1 = expression1.evaluate(object);
        final Object r2 = expression2.evaluate(object);
        //TODO be more relax on equality testing, for example Date,TimeStamp,Instant or numerics
        return Objects.equals(r1, r2);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }
}
