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

import java.util.Arrays;
import java.util.Collection;
import org.apache.sis.internal.filter.Node;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;

/**
 * The {@value #NAME} filter.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
final class DefaultLike extends Node implements PropertyIsLike {

    private final Expression expression;
    private final String pattern;
    private final String wildcard;
    private final String singleChar;
    private final String escape;
    private final boolean matchingCase;

    DefaultLike(Expression expression, String pattern, String wildcard, String singleChar, String escape, boolean matchingCase) {
        this.expression = expression;
        this.pattern = pattern;
        this.wildcard = wildcard;
        this.singleChar = singleChar;
        this.escape = escape;
        this.matchingCase = matchingCase;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Collection<?> getChildren() {
        return Arrays.asList(expression);
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
        return wildcard;
    }

    @Override
    public String getSingleChar() {
        return singleChar;
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
