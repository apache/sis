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

import org.apache.sis.util.ArgumentChecks;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;


/**
 * Temporal operations between two time values.
 * The nature of the operation depends on the subclass.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class TemporalFunction implements Filter {

    /**
     * The first of the two expressions to be used by this function.
     *
     * @see #getExpression1()
     */
    protected final Expression expression1;

    /**
     * The second of the two expressions to be used by this function.
     *
     * @see #getExpression2()
     */
    protected final Expression expression2;

    /**
     * Creates a new temporal function.
     *
     * @param  expression1  the first of the two expressions to be used by this function.
     * @param  expression2  the second of the two expressions to be used by this function.
     */
    TemporalFunction(final Expression expression1, final Expression expression2) {
        ArgumentChecks.ensureNonNull("expression1", expression1);
        ArgumentChecks.ensureNonNull("expression2", expression2);
        this.expression1 = expression1;
        this.expression2 = expression2;
    }

    /**
     * Returns the first of the two expressions to be used by this function.
     * This is the value specified at construction time.
     */
    public final Expression getExpression1() {
        return expression1;
    }

    /**
     * Returns the second of the two expressions to be used by this function.
     * This is the value specified at construction time.
     */
    public final Expression getExpression2() {
        return expression2;
    }

    /**
     * The "After" filter.
     */
    static final class After extends TemporalFunction implements org.opengis.filter.temporal.After {

        /** Creates a new expression for the {@value #NAME} operation. */
        After(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "AnyInteracts" filter.
     */
    static final class AnyInteracts extends TemporalFunction implements org.opengis.filter.temporal.AnyInteracts {

        /** Creates a new expression for the {@value #NAME} operation. */
        AnyInteracts(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "Before" filter.
     */
    static final class Before extends TemporalFunction implements org.opengis.filter.temporal.Before {

        /** Creates a new expression for the {@value #NAME} operation. */
        Before(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "Begins" filter.
     */
    static final class Begins extends TemporalFunction implements org.opengis.filter.temporal.Begins {

        /** Creates a new expression for the {@value #NAME} operation. */
        Begins(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "BegunBy" filter.
     */
    static final class BegunBy extends TemporalFunction implements org.opengis.filter.temporal.BegunBy {

        /** Creates a new expression for the {@value #NAME} operation. */
        BegunBy(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "During" filter.
     */
    static final class During extends TemporalFunction implements org.opengis.filter.temporal.During {

        /** Creates a new expression for the {@value #NAME} operation. */
        During(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "EndedBy" filter.
     */
    static final class EndedBy extends TemporalFunction implements org.opengis.filter.temporal.EndedBy {

        /** Creates a new expression for the {@value #NAME} operation. */
        EndedBy(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "Ends" filter.
     */
    static final class Ends extends TemporalFunction implements org.opengis.filter.temporal.Ends {

        /** Creates a new expression for the {@value #NAME} operation. */
        Ends(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "Meets" filter.
     */
    static final class Meets extends TemporalFunction implements org.opengis.filter.temporal.Meets {

        /** Creates a new expression for the {@value #NAME} operation. */
        Meets(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "MetBy" filter.
     */
    static final class MetBy extends TemporalFunction implements org.opengis.filter.temporal.MetBy {

        /** Creates a new expression for the {@value #NAME} operation. */
        MetBy(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "OverlappedBy" filter.
     */
    static final class OverlappedBy extends TemporalFunction implements org.opengis.filter.temporal.OverlappedBy {

        /** Creates a new expression for the {@value #NAME} operation. */
        OverlappedBy(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "TContains" filter.
     */
    static final class TContains extends TemporalFunction implements org.opengis.filter.temporal.TContains {

        /** Creates a new expression for the {@value #NAME} operation. */
        TContains(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "TEquals" filter.
     */
    static final class TEquals extends TemporalFunction implements org.opengis.filter.temporal.TEquals {

        /** Creates a new expression for the {@value #NAME} operation. */
        TEquals(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The "TOverlaps" filter.
     */
    static final class TOverlaps extends TemporalFunction implements org.opengis.filter.temporal.TOverlaps {

        /** Creates a new expression for the {@value #NAME} operation. */
        TOverlaps(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public boolean evaluate(Object object) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }
}
