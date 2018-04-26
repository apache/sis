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

import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.MatchAction;
import org.opengis.filter.expression.Expression;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class AbstractBinaryComparisonOperator implements BinaryComparisonOperator {

    protected final Expression exp1;
    protected final Expression exp2;
    protected final boolean matchCase;
    protected final MatchAction matchAction;

    public AbstractBinaryComparisonOperator(Expression exp1, Expression exp2, boolean matchCase, MatchAction matchAction) {
        this.exp1 = exp1;
        this.exp2 = exp2;
        this.matchCase = matchCase;
        this.matchAction = matchAction;
    }

    @Override
    public Expression getExpression1() {
        return exp1;
    }

    @Override
    public Expression getExpression2() {
        return exp2;
    }

    @Override
    public boolean isMatchingCase() {
        return matchCase;
    }

    @Override
    public MatchAction getMatchAction() {
        return matchAction;
    }


}
