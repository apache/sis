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
package org.apache.sis.storage.sql.feature;

import java.util.Map;
import java.util.AbstractMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.function.Consumer;
import java.sql.Connection;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.metadata.sql.internal.shared.SQLBuilder;
import org.apache.sis.filter.internal.shared.WarningEvent;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.system.Modules;
import org.apache.sis.util.Workaround;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.CodeList;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.ValueReference;


/**
 * Builder for the SQL fragment on the right side of the {@code WHERE} keyword.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SelectionClause extends SQLBuilder implements Consumer<WarningEvent> {
    /**
     * Whether the database rejects spatial functions that mix geometries with and without <abbr>CRS</abbr>.
     * We observed that PostGIS 3.4 produces an error not only when the geometry operands have different CRS,
     * but also when one operand has a CRS and the other operand has no explicit CRS. Example:
     *
     * <blockquote>Operation on mixed SRID geometries (Polygon, 4326) != (Polygon, 0)</blockquote>
     *
     * As a workaround, if the literal has no CRS, we assume that the CRS of the geometry column was implied.
     * In such cases, the SRID 0 is replaced by the SRID of the geometry column. Current version applies this
     * workaround for all databases, but we could make this field non-static if a future version of Apache SIS
     * decides to apply this policy on a case-by-case basis.
     *
     * <p>Note that above error is not always visible. For example, when using {@code ST_Intersects},
     * PostGIS first performs a quick filtering based on bounding boxes using the {@code &&} operator.
     * But that operator does not check the <abbr>SRID</abbr>. Therefore, no error message is raised
     * when the bounding boxes of the geometries do not intersect.</p>
     */
    @Workaround(library = "PostGIS", version = "3.4")
    static final boolean REPLACE_UNSPECIFIED_CRS = true;

    /**
     * The table or view for which to create a SQL statement.
     */
    private final Table table;

    /**
     * The parameters to set after the connection has been established.
     * For each entry, the key is the index in {@link #buffer} of the parameter to set
     * and the value is the object to convert to a value that can be set in the query.
     * The following values need to be converted:
     *
     * <ul>
     *   <li>Instances of {@link CoordinateReferenceSystem} shall be replaced by <abbr>SRID</abbr>.</li>
     * </ul>
     *
     * Elements must be sorted in increasing order of keys.
     *
     * @see #query(InfoStatements)
     */
    private final List<Map.Entry<Integer, CoordinateReferenceSystem>> parameters;

    /**
     * The coordinate reference system of the geometry columns referenced by the spatial functions to write.
     * This is {@code null} when not writing a spatial function, or if the function does not reference any
     * geometry column, or if it references more than one geometry column with different <abbr>CRS</abbr>.
     * If non-null, the optional may be empty if the geometry column does not declare a reference system.
     * In the latter case (empty), each row of the table can be a geometry in a different <abbr>CRS</abbr>.
     *
     * @see #REPLACE_UNSPECIFIED_CRS
     */
    private Optional<CoordinateReferenceSystem> columnCRS;

    /**
     * Flag sets to {@code true} if a filter or expression cannot be converted to SQL.
     * When a SQL string become flagged as invalid, it is truncated to the length that
     * it had the last time that it was valid.
     *
     * @see #invalidate()
     * @see #isInvalid()
     */
    private boolean isInvalid;

    /**
     * Creates a new builder for the given table.
     *
     * @param  table  the table or view for which to create a SQL statement.
     */
    SelectionClause(final Table table) {
        super(table.database);
        this.table = table;
        parameters = new ArrayList<>();
    }

    /**
     * Clears any <abbr>CRS</abbr> that was configured by {@code acceptColumnCRS(…)}.
     * This method should be invoked after the caller finished to write a spatial function.
     */
    final void clearColumnCRS() {
        columnCRS = null;
    }

    /**
     * If the referenced column is a geometry or raster column, remembers its default coordinate reference system.
     * This method can be invoked before {@link #appendLiteral(Object)} in order to set a default <abbr>CRS</abbr>
     * for literals that do not declare themselves their <abbr>CRS</abbr>.
     *
     * @param  ref  reference to a property to insert in SQL statement.
     * @return whether the caller needs to stop the check of operands.
     *
     * @see #REPLACE_UNSPECIFIED_CRS
     */
    final boolean acceptColumnCRS(final ValueReference<Feature,?> ref) {
        final Column c = table.getColumn(ref.getXPath());
        if (c != null && c.getGeometryType().isPresent()) {
            final Optional<CoordinateReferenceSystem> crs = c.getDefaultCRS();
            if (columnCRS == null) {
                columnCRS = crs;    // May be empty, which is not the same as null for this class.
            } else if (!CRS.equivalent(columnCRS.orElse(null), crs.orElse(null))) {
                clearColumnCRS();
                return true;
            }
        }
        return false;
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
     * Writes a literal value, or marks this SQL as invalid if the value cannot be formatted.
     */
    final void appendLiteral(final Object value) {
        if (value instanceof GeographicBoundingBox) {
            appendGeometry(null, new GeneralEnvelope((GeographicBoundingBox) value));
        } else if (value instanceof Envelope) {
            appendGeometry(null, (Envelope) value);
        } else {
            Geometries.wrap(value).ifPresentOrElse(
                    (wrapper) -> appendGeometry(wrapper, null),
                    ()        -> appendValue(value));
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
    private void appendGeometry(GeometryWrapper wrapper, Envelope bounds) {
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
        /*
         * Get the average span of the geometry. It will be used for computing a flatness factor.
         * It does not matter if the span is inaccurate, as the flatness factor is only a hint
         * ignored by most geometry libraries.
         */
        final double span = (bounds.getSpan(0) + bounds.getSpan(1)) / Geometries.BIDIMENSIONAL;
        if (Double.isNaN(span)) {
            final var e = new GeneralEnvelope(bounds);
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
        final Database<?> db = table.database;
        if (wrapper == null) {
            wrapper = db.geomLibrary.toGeometry2D(bounds, WraparoundMethod.SPLIT);
        }
        final String wkt = wrapper.formatWKT(0.05 * span);      // Arbitrary flateness factor.
        /*
         * Format a spatial function for building the geometry from the Well-Known Text.
         * The CRS, if available, will be specified as a SRID if the spatial support has
         * been recognized (otherwise we cannot map the CRS to the database-dependent SRID).
         */
        appendSpatialFunction("ST_GeomFromText");
        append('(').appendValue(wkt);
        if (db.dialect.supportsSRID() && db.getSpatialSchema().isPresent()) {
            CoordinateReferenceSystem crs = wrapper.getCoordinateReferenceSystem();
            if (REPLACE_UNSPECIFIED_CRS && columnCRS != null) {
                if (crs == null) {
                    crs = columnCRS.orElse(null);
                } else {
                    /*
                     * If `columnCRS` is empty, then we have a geometry column without CRS (SRID = 0).
                     * For making the literal consistent with the column, we could set `crs` to null.
                     * However, while the column has no CRS, the geometry instances on each row may have a CRS
                     * and clearing the literal CRS will result in "Operation on mixed SRID geometries" error.
                     *
                     * The opposite problem also exists: we could really have no CRS at all, neither in the column
                     * and in the geometry instances. In such case, not clearing the CRS may also cause above error.
                     * We have no easy way to determine if we should clear the CRS or not. The conservative approach
                     * applied for now is to leave the literal as the user specified it.
                     */
                }
            }
            if (crs != null) {
                buffer.append(", ");
                parameters.add(new AbstractMap.SimpleEntry<>(buffer.length(), crs));
                buffer.append('?');
            }
        }
        append(')');
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
     * Appends the name of a spatial function. The catalog and schema names are
     * included for making sure that it works even if the search path is not set.
     * The function name is written without quotes, because the functions kept by
     * {@link SelectionClauseWriter#removeUnsupportedFunctions(Database)} use the
     * case convention of the database.
     *
     * @param  name  name of the spatial function to append.
     */
    final void appendSpatialFunction(final String name) {
        final Database<?> db = table.database;
        appendIdentifier(db.catalogOfSpatialTables, db.schemaOfSpatialTables, name, false);
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

    /**
     * Returns whether an error occurred while writing the <abbr>SQL</abbr> statement.
     * If this method returns {@code true}, then the caller should truncate the SQL to
     * the last length which was known to be valid and fallback on Java code for the rest.
     */
    final boolean isInvalid() {
        return isInvalid;
    }

    /**
     * Declares the SQL as invalid. It does not means that the whole SQL needs to be discarded.
     * The SQL may be truncated to the last point where it was considered valid.
     */
    final void invalidate() {
        isInvalid = true;
    }

    /**
     * Returns the localized resources for warnings and error messages.
     */
    private Resources resources() {
        return Resources.forLocale(table.database.listeners.getLocale());
    }

    /**
     * Sets the logger, class and method names of the given record, then logs it.
     * This method declares {@link FeatureSet#features(boolean)} as the public source of the log.
     *
     * @param  record  the record to configure and log.
     */
    private void log(final LogRecord record) {
        record.setSourceClassName(FeatureSet.class.getName());
        record.setSourceMethodName("features");
        record.setLoggerName(Modules.SQL);
        table.database.listeners.warning(record);
    }

    /**
     * Invoked when a warning occurred during operations on filters or expressions.
     *
     * @param  event  the warning.
     */
    @Override
    public void accept(final WarningEvent event) {
        final LogRecord record = resources().createLogRecord(
                Level.WARNING,
                Resources.Keys.IncompatibleLiteralCRS_2,
                event.getOperatorType().map(CodeList::identifier).orElse("?"),
                event.getParameter(ValueReference.class).map(ValueReference<?,?>::getXPath).orElse("?"));
        record.setThrown(event.exception);
        log(record);
    }

    /**
     * Returns the <abbr>SQL</abbr> fragment built by this {@code SelectionClause}.
     * This method completes the information that we deferred until a connection is established.
     *
     * @param  spatialInformation  a cache of statements for fetching spatial information, or {@code null}.
     * @return the <abbr>SQL</abbr> fragment, or {@code null} if there is no {@code WHERE} clause to add.
     * @throws Exception if an SQL error, parsing error or other error occurred.
     */
    final String query(final Connection connection, InfoStatements spatialInformation) throws Exception {
        if (isEmpty()) {
            return null;
        }
        boolean close = false;
        for (int i = parameters.size(); --i >= 0;) {
            if (spatialInformation == null) {
                spatialInformation = table.database.createInfoStatements(connection);
                close = true;
            }
            final var entry = parameters.get(i);
            final int index = entry.getKey();
            final int srid  = spatialInformation.findSRID(entry.getValue());
            buffer.replace(index, index + 1, Integer.toString(srid));
        }
        if (close) {
            /*
             * We could put this in a `finally` block, but this method is already invoked
             * in a context where the caller will close the connection in case of failure.
             */
            spatialInformation.close();
        }
        return buffer.toString();
    }
}
