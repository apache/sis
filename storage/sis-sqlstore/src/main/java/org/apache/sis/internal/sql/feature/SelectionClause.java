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
package org.apache.sis.internal.sql.feature;

import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.metadata.sql.SQLBuilder;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.ValueReference;


/**
 * Builder for the SQL fragment on the right side of the {@code WHERE} keyword.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class SelectionClause extends SQLBuilder {
    /**
     * The table or view for which to create a SQL statement.
     */
    private final Table table;

    /**
     * Flag sets to {@code true} if a filter or expression can not be converted to SQL.
     * When a SQL string become flagged as invalid, it is truncated to the length that
     * it had the last time that it was valid.
     *
     * @see #invalidate()
     */
    boolean isInvalid;

    /**
     * Creates a new builder for the given table.
     *
     * @param  table  the table or view for which to create a SQL statement.
     */
    SelectionClause(final Table table) {
        super(table.database);
        this.table = table;
    }

    /**
     * Writes the name of a column, or marks the SQL as invalid if the column is not found.
     *
     * @param  ref  reference to a property to insert in SQL statement.
     */
    final void appendColumnName(final ValueReference<Feature,?> ref) {
        final Column c = table.getColumn(ref.getXPath());
        if (c != null) {
            appendIdentifier(c.name);
        } else {
            invalidate();
        }
    }

    /**
     * Writes a literal value, or marks this SQL as invalid if the value can not be formatted.
     */
    final void appendLiteral(final Object value) {
        if (value instanceof GeographicBoundingBox) {
            appendGeometry(null, new GeneralEnvelope((GeographicBoundingBox) value));
        } else if (value instanceof Envelope) {
            appendGeometry(null, (Envelope) value);
        } else if (value instanceof Geometry) {
            appendGeometry(Geometries.wrap((Geometry) value).orElse(null), null);
        } else {
            appendValue(value);
        }
    }

    /**
     * Appends the given geometry in Well-Known Text (WKT) format.
     * Exactly one of the given argument should be non-null.
     * If both arguments are null, the SQL is declared invalid.
     *
     * @todo Find a better approximation of desired flatness (the {@code span} local variable).
     *       It could be user-specified resolution.
     */
    private void appendGeometry(GeometryWrapper<?> wrapper, Envelope bounds) {
        if (bounds == null) {
            if (wrapper == null) {
                invalidate();
                return;
            }
            bounds = wrapper.getEnvelope();
        }
        if (bounds.getDimension() < Geometries.BIDIMENSIONAL) {
            invalidate();
            return;
        }
        final double span = (bounds.getSpan(0) + bounds.getSpan(1)) / Geometries.BIDIMENSIONAL;
        if (Double.isNaN(span)) {
            final GeneralEnvelope e = new GeneralEnvelope(bounds);
            for (int i=0; i<Geometries.BIDIMENSIONAL; i++) {
                final double lower = clampInfinity(e.getLower(i));
                final double upper = clampInfinity(e.getUpper(i));
                if (Double.isNaN(lower) || Double.isNaN(upper)) {
                    invalidate();
                    return;
                }
                e.setRange(i, lower, upper);
            }
            bounds = e;
        }
        if (wrapper == null) {
            wrapper = table.database.geomLibrary.toGeometry2D(bounds, WraparoundMethod.SPLIT);
        }
        final String wkt = wrapper.formatWKT(0.05 * span);
        append("ST_GeomFromText(").appendValue(wkt).append(')');
    }

    /**
     * Replaces infinity values by the maximal real number.
     */
    private static double clampInfinity(final double value) {
        if (value == Double.NEGATIVE_INFINITY) return -Double.MAX_VALUE;
        if (value == Double.POSITIVE_INFINITY) return +Double.MAX_VALUE;
        return value;
    }

    /**
     * Declares the SQL as invalid. It does not means that the whole SQL needs to be discarded.
     * The SQL may be truncated to the last point where it was considered valid.
     */
    final void invalidate() {
        isInvalid = true;
    }

    /**
     * Tries to append a SQL statement for the given filter.
     * This method returns {@code true} on success, or {@code false} if the statement can no be written.
     * In the latter case, the content of this {@code SelectionClause} is unchanged.
     *
     * @param  writer  the visitor to use for converting filters to SQL statements.
     * @param  filter  the filter to try to convert to SQL statements.
     * @return {@code true} on success, or {@code false} in this {@code SelectionClause} is unchanged.
     */
    final boolean tryAppend(final SelectionClauseWriter writer, final Filter<? super Feature> filter) {
        final int pos = buffer.length();
        if (pos != 0) {
            buffer.append(" AND ");
        }
        if (writer.write(this, filter)) {
            buffer.setLength(pos);
            isInvalid = false;
            return false;
        }
        return true;
    }
}
