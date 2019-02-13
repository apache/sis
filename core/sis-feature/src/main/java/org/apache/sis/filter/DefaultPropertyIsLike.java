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

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;

/**
 * TODO
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class DefaultPropertyIsLike implements PropertyIsLike {

    private final Expression expression;
    private final String pattern;
    private final String wildcardMulti;
    private final String wildcardSingle;
    private final String escape;
    private final boolean matchingCase;

    public DefaultPropertyIsLike(final Expression expression, final String pattern, final String wildcardMulti,
            final String wildcardSingle, final String escape, final boolean matchCase) {
        ensureNonNull("expression", expression);

        this.expression = expression;
        this.pattern = pattern;
        this.wildcardMulti = wildcardMulti;
        this.wildcardSingle = wildcardSingle;
        this.escape = escape;
        this.matchingCase = matchCase;
    }

    @Override
    public Expression getExpression() {
        return expression;
    }

    @Override
    public String getLiteral() {
        return pattern;
    }

    @Override
    public String getWildCard() {
        return wildcardMulti;
    }

    @Override
    public String getSingleChar() {
        return wildcardSingle;
    }

    @Override
    public String getEscape() {
        return escape;
    }

    @Override
    public boolean isMatchingCase() {
        return matchingCase;
    }

    @Override
    public boolean evaluate(Object object) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

}
