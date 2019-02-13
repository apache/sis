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

import java.util.Iterator;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.internal.feature.FeatureExpression;

// Branch-dependent imports
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;


/**
 * Base class of Apache SIS implementation of OGC expressions operating on feature instances.
 * This base class adds an additional method, {@link #expectedType(FeatureType)}, for fetching
 * in advance the expected type of expression results.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class AbstractExpression implements Expression, FeatureExpression {
    /**
     * Creates a new expression.
     */
    protected AbstractExpression() {
    }

    /**
     * Evaluates the expression for producing a result of the given type.
     * The default implementation evaluate the expression in the default
     * way and attempt to convert the result.
     *
     * @param  feature  to feature to evaluate with this expression.
     * @param  target   the desired type for the expression result.
     */
    @Override
    public <T> T evaluate(final Object feature, final Class<T> target) {
        ArgumentChecks.ensureNonNull("target", target);
        final Object value = evaluate(feature);
        try {
            return ObjectConverters.convert(value, target);
        } catch (UnconvertibleObjectException ex) {
            // TODO: should report the exception somewhere.
            return null;
        }
    }


    /**
     * Returns a graphical representation of the specified objects. This representation can be
     * printed to the {@linkplain System#out standard output stream} (for example) if it uses
     * a monospaced font and supports unicode.
     *
     * @param  root  The root name of the tree to format.
     * @param  objects The objects to format as root children.
     * @return A string representation of the tree.
     */
    static String toStringTree(String root, final Iterable<?> objects) {
        final StringBuilder sb = new StringBuilder();
        if (root != null) {
            sb.append(root);
        }
        if (objects != null) {
            final Iterator<?> ite = objects.iterator();
            while (ite.hasNext()) {
                sb.append('\n');
                final Object next = ite.next();
                final boolean last = !ite.hasNext();
                sb.append(last ? "└─ " : "├─ ");

                final String[] parts = String.valueOf(next).split("\n");
                sb.append(parts[0]);
                for (int k=1;k<parts.length;k++) {
                    sb.append('\n');
                    sb.append(last ? ' ' : '│');
                    sb.append("  ");
                    sb.append(parts[k]);
                }
            }
        }
        return sb.toString();
    }
}
