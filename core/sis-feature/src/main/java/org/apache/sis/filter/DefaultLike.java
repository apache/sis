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
import java.util.regex.Pattern;
import org.apache.sis.internal.filter.Node;
import org.apache.sis.util.ArgumentChecks;
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

    /**
     * Cached java regular expression.
     */
    private Pattern regex;

    DefaultLike(Expression expression, String pattern, String wildcard, String singleChar, String escape, boolean matchingCase) {
        ArgumentChecks.ensureNonNull("pattern", pattern);
        ArgumentChecks.ensureNonNull("wildcard", wildcard);
        ArgumentChecks.ensureNonNull("singleChar", singleChar);
        ArgumentChecks.ensureNonNull("escape", escape);
        if (wildcard.length() != 1) throw new IllegalArgumentException("WildCard string must be one character long.");
        if (singleChar.length() != 1) throw new IllegalArgumentException("SingleChar string must be one character long.");
        if (escape.length() != 1) throw new IllegalArgumentException("Escape string must be one character long.");

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
        final String value = expression.evaluate(object, String.class);
        if (value == null) return false;
        return getPattern().matcher(value).matches();
    }

    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    /**
     * Get or create java regex pattern.
     */
    private Pattern getPattern() {
        if (regex == null) {
            //convert pattern to java regex
            final StringBuilder sb = new StringBuilder();
            if (!matchingCase) {
                //add case insensitive
                sb.append("(?i)");
            }
            final int wld = wildcard.charAt(0);
            final int sgl = singleChar.charAt(0);
            final int esc = escape.charAt(0);

            for (int i = 0, n = pattern.length(); i < n; i++) {
                char c = pattern.charAt(i);

                //user defined special characters
                if (wld == c) {
                    sb.append('.');
                    sb.append('*');
                } else if (sgl == c) {
                    sb.append('.');
                } else if (esc == c) {
                    c = pattern.charAt(++i);
                    if (c == wld || c == sgl || c == esc) {
                        if (isMetaCharacter(c)) {
                            sb.append('\\');
                        }
                        sb.append(c);
                    } else {
                        throw new IllegalArgumentException("Escape character must be used only to escape wild, single and escape characters.");
                    }
                }
                //java regex reserved metacharacters
                else if (isMetaCharacter(c)) {
                    sb.append('\\');
                    sb.append(c);
                }
                //any other character
                else {
                    sb.append(c);
                }
            }

            regex = Pattern.compile(sb.toString());
        }
        return regex;
    }

    /**
     * Returns true if given character is a regular expression meta-character.
     */
    private static boolean isMetaCharacter(char c) {
        return c == '.'
            || c == '*'
            || c == '?'
            || c == '('
            || c == ')'
            || c == '['
            || c == ']'
            || c == '{'
            || c == '}'
            || c == '\\'
            || c == '^'
            || c == '$'
            || c == '|'
            || c == '+';
    }

}
