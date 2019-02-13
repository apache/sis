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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.MatchAction;
import org.opengis.filter.expression.Expression;

/**
 * Immutable abstract "binary comparison operator".
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class AbstractBinaryComparisonOperator extends AbstractBinaryOperator implements BinaryComparisonOperator,Serializable{

    protected final boolean match;
    protected final MatchAction matchAction;

    public AbstractBinaryComparisonOperator(final Expression left, final Expression right, final boolean match, final MatchAction matchAction) {
        super(left, right);
        this.match = match;
        this.matchAction = matchAction;
        ArgumentChecks.ensureNonNull("matchAction", matchAction);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isMatchingCase() {
        return match;
    }

    @Override
    public MatchAction getMatchAction() {
        return matchAction;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final boolean evaluate(final Object candidate) {
        final Object objleft = expression1.evaluate(candidate);
        final Object objright = expression2.evaluate(candidate);

        if (objleft instanceof Collection && objright instanceof Collection) {
            return evaluateOne(objleft, objright);
        } else if (objleft instanceof Collection) {
            final Collection col = (Collection) objleft;
            if (col.isEmpty()) return false;
            switch (matchAction) {
                case ALL:
                    for (Object o : col) {
                        if (!evaluateOne(o, objright)) return false;
                    }
                    return true;
                case ANY:
                    for (Object o : col) {
                        if (evaluateOne(o, objright)) return true;
                    }
                    return false;
                case ONE:
                    boolean found = false;
                    for (Object o : col) {
                        if (evaluateOne(o, objright)) {
                            if (found) return false;
                            found = true;
                        }
                    }
                    return found;
                default: return false;
            }
        } else if (objright instanceof Collection) {
            final Collection col = (Collection) objright;
            if (col.isEmpty()) return false;
            switch (matchAction) {
                case ALL:
                    for (Object o : col) {
                        if (!evaluateOne(objleft,o)) return false;
                    }
                    return true;
                case ANY:
                    for (Object o : col) {
                        if (evaluateOne(objleft,o)) return true;
                    }
                    return false;
                case ONE:
                    boolean found = false;
                    for (Object o : col) {
                        if (evaluateOne(objleft,o)) {
                            if (found) return false;
                            found = true;
                        }
                    }
                    return found;
                default: return false;
            }
        } else {
            return evaluateOne(objleft, objright);
        }
    }

    protected abstract boolean evaluateOne(Object objleft,Object objright);

    protected Integer compare(Object objleft, Object objright){

        if (!(objleft instanceof Comparable)) {
            return null;
        }

        //see if the right type might be more appropriate for test
        if ( !(objleft instanceof Date) ){

            if (objright instanceof Date) {
                //object right class is more appropriate

                Object cdLeft = ObjectConverters.convert(objleft, Date.class);
                if (cdLeft != null) {
                    return ((Comparable) cdLeft).compareTo(objright);
                }

            }

        }

        objright = ObjectConverters.convert(objright, objleft.getClass());

        if (objleft instanceof java.sql.Date && objright instanceof java.sql.Date) {
            final Calendar cal1 = Calendar.getInstance();
            cal1.setTime((java.sql.Date) objleft);
            cal1.set(Calendar.HOUR_OF_DAY, 0);
            cal1.set(Calendar.MINUTE, 0);
            cal1.set(Calendar.SECOND, 0);
            cal1.set(Calendar.MILLISECOND, 0);

            final Calendar cal2 = Calendar.getInstance();
            cal2.setTime((java.sql.Date) objright);
            cal2.set(Calendar.HOUR_OF_DAY, 0);
            cal2.set(Calendar.MINUTE, 0);
            cal2.set(Calendar.SECOND, 0);
            cal2.set(Calendar.MILLISECOND, 0);

            return cal1.compareTo(cal2);
        }

        if (objright == null) {
            return null;
        }
        return ((Comparable) objleft).compareTo(objright);
    }


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + (this.expression1 != null ? this.expression1.hashCode() : 0);
        hash = 61 * hash + (this.expression2 != null ? this.expression2.hashCode() : 0);
        hash = 61 * hash + (this.match ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractBinaryComparisonOperator other = (AbstractBinaryComparisonOperator) obj;
        if (this.expression1 != other.expression1 && (this.expression1 == null || !this.expression1.equals(other.expression1))) {
            return false;
        }
        if (this.expression2 != other.expression2 && (this.expression2 == null || !this.expression2.equals(other.expression2))) {
            return false;
        }
        if (this.match != other.match) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName() + " (matchcase=");
        sb.append(match).append(")\n");
        sb.append(AbstractExpression.toStringTree("",Arrays.asList(expression1,expression2)));
        return sb.toString();
    }

}
