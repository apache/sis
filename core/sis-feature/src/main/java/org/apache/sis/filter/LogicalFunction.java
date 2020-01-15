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
import java.util.List;
import java.util.Collection;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.filter.Node;
import org.apache.sis.internal.util.UnmodifiableArrayList;

// Branch-dependent imports
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;


/**
 * Logical filter (AND, OR) using an arbitrary number of filters.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class LogicalFunction extends Node {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3696645262873257479L;

    /**
     * The filter on which to apply the logical operator.
     */
    protected final Filter[] filters;

    /**
     * Creates a new logical function applied on the given filters.
     */
    protected LogicalFunction(final Collection<? extends Filter> f) {
        ArgumentChecks.ensureNonNull("filters", f);
        filters = f.toArray(new Filter[f.size()]);
        for (int i=0; i<filters.length; i++) {
            ArgumentChecks.ensureNonNullElement("filters", i, filters[i]);
        }
        ArgumentChecks.ensureSizeBetween("filters", 2, Integer.MAX_VALUE, filters.length);
    }

    /**
     * Returns a list containing all of the child filters of this object.
     * This list will contain at least two elements.
     */
    @Override
    public final List<Filter> getChildren() {
        return UnmodifiableArrayList.wrap(filters);
    }

    /**
     * Returns a hash code value for this filter.
     */
    @Override
    public final int hashCode() {
        return getClass().hashCode() ^ Arrays.hashCode(filters);
    }

    /**
     * Compares this filter with the given object for equality.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == getClass()) {
            return Arrays.equals(filters, ((LogicalFunction) obj).filters);
        }
        return false;
    }


    /**
     * The "And" operation (⋀).
     *
     * @see org.apache.sis.filter.ComparisonFunction.Between
     */
    static final class And extends LogicalFunction implements org.opengis.filter.And {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 152892064260384713L;

        /** Creates a new expression for the given filters. */
        And(final Collection<? extends Filter> filters) {
            super(filters);
        }

        /** Returns a name for this filter. */
        @Override public    String getName() {return "And";}
        @Override protected char   symbol()  {return filters.length <= 2 ? '∧' : '⋀';}

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }

        /** Executes the logical operation. */
        @Override public boolean evaluate(final Object object) {
            for (final Filter filter : filters) {
                if (!filter.evaluate(object)) {
                    return false;
                }
            }
            return true;
        }
    }


    /**
     * The "Or" operation (⋁).
     */
    static final class Or extends LogicalFunction implements org.opengis.filter.Or {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 3805785720811330282L;

        /** Creates a new expression for the given filters. */
        Or(final Collection<? extends Filter> filters) {
            super(filters);
        }

        /** Returns a name for this filter. */
        @Override public    String getName() {return "Or";}
        @Override protected char   symbol()  {return filters.length <= 2 ? '∨' : '⋁';}

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }

        /** Executes the logical operation. */
        @Override public boolean evaluate(final Object object) {
            for (Filter filter : filters) {
                if (filter.evaluate(object)) {
                    return true;
                }
            }
            return false;
        }
    }
}
