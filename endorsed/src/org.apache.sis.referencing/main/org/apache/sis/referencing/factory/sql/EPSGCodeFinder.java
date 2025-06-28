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
package org.apache.sis.referencing.factory.sql;

import java.util.Set;
import java.util.List;
import java.util.LinkedHashSet;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.pending.jdk.JDK19;
import org.apache.sis.metadata.privy.ReferencingServices;
import org.apache.sis.metadata.sql.privy.SQLUtilities;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.ConcurrentAuthorityFactory;
import static org.apache.sis.metadata.privy.NameToIdentifier.Simplifier.ESRI_DATUM_PREFIX;

// Specific to the geoapi-4.0 branch:
import org.opengis.referencing.crs.DerivedCRS;


/**
 * An implementation of {@link IdentifiedObjectFinder} which scans over a smaller set of authority codes.
 * This is used for finding the EPSG code of a given Coordinate Reference System or other geodetic object.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class EPSGCodeFinder extends IdentifiedObjectFinder {
    /**
     * The data access object to use for searching EPSG codes.
     * Supplied at construction time and assumed alive for the duration of this {@code EPSGCodeFinder} life
     * (i.e. this class does not create and does not close DAO by itself).
     */
    private final EPSGDataAccess dao;

    /**
     * The type of object to search, or {@code null} for using {@code object.getClass()}.
     * This is set to a non-null value when searching for dependencies, in order to avoid
     * confusion in an implementation class implements more than one GeoAPI interfaces.
     *
     * @see #isInstance(Class, IdentifiedObject)
     */
    private Class<? extends IdentifiedObject> declaredType;

    /**
     * Creates a new finder for the given data access object.
     */
    EPSGCodeFinder(final EPSGDataAccess dao) {
        super(dao.owner);
        this.dao = dao;
    }

    /**
     * Lookups objects which are approximately equal to the specified object.
     * This method temporarily disables warnings about deprecated objects.
     */
    @Override
    public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
        final boolean old = dao.quiet;
        dao.quiet = true;
        try {
            return super.find(object);
        } finally {
            dao.quiet = old;
        }
    }

    /**
     * Returns a description of the condition to put in a {@code WHERE} clause for an object having
     * the given dependency.
     *
     * <h4>Implementation note</h4>
     * The {@code super.find(…)} method performs a check (not documented in public API) for detecting
     * when it is invoked recursively, which is the case here. Consequently, the {@code super.find(…)}
     * behavior below is slightly different than usual: since invoked recursively, {@code super.find(…)}
     * checks the cache of the {@link ConcurrentAuthorityFactory} wrapper. If found, the dependency will
     * also be stored in the cache. This is desirable because this method may be invoked (indirectly) in
     * a loop for many CRS objects sharing the same {@link CoordinateSystem} or {@link Datum} dependencies.
     *
     * @param  column      column in the SQL query containing EPSG codes of dependency.
     * @param  type        GeoAPI interface implemented by the dependency to search.
     * @param  dependency  the dependency for which to search EPSG codes, or {@code null}.
     * @param  ignoreAxes  whether to force ignoring axes. Useful for base CRS of projected CRS.
     * @return EPSG codes of given dependency, or {@code null} if no dependency were found.
     */
    private <T extends IdentifiedObject> Condition dependencies(final String column,
            final Class<T> type, final T dependency, final boolean ignoreAxes) throws FactoryException
    {
        if (dependency != null) {
            final Class<? extends IdentifiedObject> pt = declaredType;
            final boolean previous = isIgnoringAxes();
            final Set<IdentifiedObject> find;
            try {
                setIgnoringAxes(ignoreAxes | previous);
                declaredType = type;
                find = find(dependency);
            } finally {
                declaredType = pt;
                setIgnoringAxes(previous);
            }
            final Set<Number> filters = JDK19.newLinkedHashSet(find.size());
            for (final IdentifiedObject dep : find) {
                Identifier id = IdentifiedObjects.getIdentifier(dep, Citations.EPSG);
                if (id != null) try {                                                   // Should never be null, but let be safe.
                    filters.add(Integer.valueOf(id.getCode()));
                } catch (NumberFormatException e) {
                    Logging.recoverableException(EPSGDataAccess.LOGGER, EPSGCodeFinder.class, "getCodeCandidates", e);
                }
            }
            if (!filters.isEmpty()) {
                return new Condition(column, filters);
            }
        }
        return null;
    }

    /**
     * A condition to put in a SQL {@code WHERE} clause. SQL query will be one of the forms shown below,
     * where {@code <column>} and {@code <values>} are {@link #column} and {@link #values} respectively.
     *
     * {@snippet lang="sql" :
     *     SELECT <codeColumn> FROM <table> WHERE <column> IN (<values>)
     *     SELECT <codeColumn> FROM <table> WHERE <column> >= <value - ε> AND <column> <= <value + ε>
     *     }
     *
     * The latter form is used if {@code <filters>} is a floating point value.
     * Otherwise, {@code <filters>} are typically EPSG codes of dependencies.
     */
    private static class Condition {
        /** A sentinel value for filtering by name. */
        static final Condition NAME = new Condition("NAME", Set.of());

        /** The column on which the condition apply. */
        final String column;

        /** The values of the conditions. */
        final Set<Number> values;

        /** Creates a new condition. */
        Condition(final String column, final Set<Number> values) {
            this.column = column;
            this.values = values;
        }

        /**
         * Appends this condition into the given buffer. If {@code isNext} is {@code true}, then an {@code "AND"}
         * keyword is appended before the condition. Otherwise this method presumes that the given buffer already
         * ends with {@code "WHERE "} or {@code "AND "} keyword. This method does <strong>not</strong> append a
         * new {@code "AND"} keyword after the condition.
         *
         * @param  isNext  whether to append a {@code "AND"} keyword before the condition.
         * @param  buffer  where to append the SQL fragment.
         * @return whether a SQL fragment has been appended.
         */
        boolean appendToWhere(final StringBuilder buffer, final boolean isNext) {
            if (values.isEmpty()) return false;
            if (isNext) buffer.append(" AND ");
            buffer.append(column);
            if (values.size() == 1) {
                buffer.append('=').append(CollectionsExt.first(values));
            } else {
                buffer.append(" IN (");
                for (final Number code : values) {
                    buffer.append(code).append(',');
                }
                buffer.setCharAt(buffer.length() - 1, ')');
            }
            return true;
        }

        /**
         * Appends an ordering criterion. The buffer shall ends with {@code "ORDER BY"} keyword before
         * this method is invoked. If this method appends a criterion, then a comma will be appended
         * after that criterion for allowing chaining.
         *
         * @param  buffer  where to append the SQL fragment.
         */
        void appendToOrderBy(final StringBuilder buffer) {
        }

        /**
         * Returns a string representation of this condition for debugging purpose.
         */
        @Override
        public final String toString() {
            final var buffer = new StringBuilder(50);
            appendToWhere(buffer, false);
            return buffer.toString();
        }
    }

    /**
     * A condition for a floating point value with a tolerance.
     */
    private static final class FloatCondition extends Condition {
        /** Creates a new condition for the given value. */
        FloatCondition(final String column, final double value) {
            super(column, Set.of(value));
        }

        /**
         * Appends a condition with a numeric value assumed a linear distance in metres.
         * The tolerance threshold is 1 centimetre for a planet of the size of Earth.
         */
        @Override
        boolean appendToWhere(final StringBuilder buffer, final boolean isNext) {
            if (isNext) buffer.append(" AND ");
            final double value = values.iterator().next().doubleValue();
            final double tolerance = Math.abs((Formulas.LINEAR_TOLERANCE / ReferencingServices.AUTHALIC_RADIUS) * value);
            buffer.append(column).append(">=").append(value - tolerance).append(" AND ")
                  .append(column).append("<=").append(value + tolerance);
            return true;
        }

        /**
         * Appends an ordering condition.
         */
        @Override
        void appendToOrderBy(final StringBuilder buffer) {
            final double value = values.iterator().next().doubleValue();
            buffer.append("ABS(").append(column).append('-').append(value).append("), ");
        }
    }

    /**
     * Returns {@code true} if the given object implements the given interface, ignoring interfaces
     * that are not subtypes of {@link #declaredType}. This method is used for avoiding confusion if
     * the given object implements more than one GeoAPI interfaces. Test order matter: CRS should be
     * tested first in order to have precedence over datum types.
     */
    private boolean isInstance(final Class<? extends IdentifiedObject> type, final IdentifiedObject object) {
        return (declaredType == null || type.isAssignableFrom(declaredType)) && type.isInstance(object);
    }

    /**
     * Returns a set of authority codes that <strong>may</strong> identify the same object as the specified one.
     * This implementation tries to get a smaller set than what {@link EPSGDataAccess#getAuthorityCodes(Class)}
     * would produce. Deprecated objects must be last in iteration order.
     */
    @Override
    protected Set<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
        final TableInfo   table;                        // Contains `codeColumn` and `table` names.
        final Condition[] filters;                      // Conditions to put in the WHERE clause.
crs:    if (isInstance(CoordinateReferenceSystem.class, object)) {
            /*
             * For compound CRS, the SQL statement may be something like below
             *
             *   SELECT COORD_REF_SYS_CODE FROM "Coordinate Reference System"
             *     WHERE CAST(COORD_REF_SYS_KIND AS VARCHAR(80)) LIKE 'compound%'
             *       AND CMPD_HORIZCRS_CODE IN (?,…)
             *       AND CMPD_VERTCRS_CODE IN (?,…)
             */
            table = TableInfo.CRS;
            if (isInstance(CompoundCRS.class, object)) {
                final List<CoordinateReferenceSystem> components = ((CompoundCRS) object).getComponents();
                if (components != null) {       // Paranoiac check.
                    final int n = components.size();
                    if (n == 2) {
                        filters = new Condition[2];
                        for (int i=0; i<=1; i++) {
                            if ((filters[i] = dependencies((i==0) ? "CMPD_HORIZCRS_CODE" : "CMPD_VERTCRS_CODE",
                                    CoordinateReferenceSystem.class, components.get(i), false)) == null)
                            {
                                return Set.of();
                            }
                        }
                        break crs;
                    }
                    if (n == 1) {               // Should not happen.
                        return getCodeCandidates(components.get(0));
                    }
                }
            }
            /*
             * For Coordinate Reference System, the SQL statement may be something like below
             * (with DATUM_CODE replaced by SOURCE_GEOGCRS_CODE in a projected CRS):
             *
             *   SELECT COORD_REF_SYS_CODE FROM "Coordinate Reference System"
             *     WHERE CAST(COORD_REF_SYS_KIND AS VARCHAR(80)) LIKE 'geographic%'
             *       AND DATUM_CODE IN (?,…) AND DEPRECATED=0
             *     ORDER BY COORD_REF_SYS_CODE
             */
            final Condition filter;
            if (object instanceof DerivedCRS) {              // No need to use isInstance(Class, Object) from here.
                filter = dependencies("SOURCE_GEOGCRS_CODE", SingleCRS.class, ((DerivedCRS) object).getBaseCRS(), true);
            } else if (object instanceof GeodeticCRS) {
                filter = dependencies("DATUM_CODE", GeodeticDatum.class, ((GeodeticCRS) object).getDatum(), true);
            } else if (object instanceof VerticalCRS) {
                filter = dependencies("DATUM_CODE", VerticalDatum.class, ((VerticalCRS) object).getDatum(), true);
            } else if (object instanceof TemporalCRS) {
                filter = dependencies("DATUM_CODE", TemporalDatum.class, ((TemporalCRS) object).getDatum(), true);
            } else if (object instanceof SingleCRS) {
                filter = dependencies("DATUM_CODE", Datum.class, ((SingleCRS) object).getDatum(), true);
            } else {
                return Set.of();
            }
            if (filter == null) {
                return Set.of();
            }
            filters = new Condition[] {filter};
        } else if (isInstance(Datum.class, object)) {
            /*
             * We currently have no better way to filter datum (or reference frames) than their names.
             * Filtering must be at least as tolerant as AbstractDatum.isHeuristicMatchForName(String).
             * The SQL statement will be something like below:
             *
             *   SELECT DATUM_CODE FROM "Datum"
             *    WHERE ELLIPSOID_CODE IN (?,…)
             *      AND (LOWER(DATUM_NAME) LIKE '?%')
             */
            table = TableInfo.DATUM;
            if (isInstance(GeodeticDatum.class, object)) {
                filters = new Condition[] {
                    dependencies("ELLIPSOID_CODE", Ellipsoid.class, ((GeodeticDatum) object).getEllipsoid(), true),
                    Condition.NAME
                };
                if (filters[0] == null) {
                    return Set.of();
                }
            } else {
                filters = new Condition[] {
                    Condition.NAME
                };
            }
        } else if (isInstance(Ellipsoid.class, object)) {
            /*
             * The SQL query will be something like below:
             *
             *   SELECT ELLIPSOID_CODE FROM "Ellipsoid"
             *     WHERE SEMI_MAJOR_AXIS >= ?-ε AND SEMI_MAJOR_AXIS <= ?+ε
             *     ORDER BY ABS(SEMI_MAJOR_AXIS-?)
             */
            table   = TableInfo.ELLIPSOID;
            filters = new Condition[] {
                new FloatCondition("SEMI_MAJOR_AXIS", ((Ellipsoid) object).getSemiMajorAxis())
            };
        } else {
            // Not a supported type. Returns all codes.
            return super.getCodeCandidates(object);
        }
        /*
         * At this point we collected the information needed for creating the main SQL query.
         * If the filters include a filter by names, we will need to take aliases in account.
         * The following block prepares in advance the SQL query that we will need to execute,
         * but does not execute it now. Note that this block overwrites the `buffer` content,
         * so that buffer shall not contain valuable information yet.
         */
        final var buffer = new StringBuilder(350);     // Temporary buffer for building SQL query.
        final Set<String> namePatterns;
        final String aliasSQL;
        if (ArraysExt.containsIdentity(filters, Condition.NAME)) {
            namePatterns = new LinkedHashSet<>();
            namePatterns.add(toDatumPattern(object.getName().getCode(), buffer));
            for (final GenericName id : object.getAlias()) {
                namePatterns.add(toDatumPattern(id.tip().toString(), buffer));
            }
            buffer.setLength(0);
            buffer.append("SELECT OBJECT_CODE FROM \"Alias\" WHERE OBJECT_TABLE_NAME='")
                  .append(dao.translator.toActualTableName(table.unquoted()))
                  .append("' AND ");
            // PostgreSQL does not require explicit cast when the value is a literal instead of "?".
            appendFilterByName(namePatterns, "ALIAS", buffer);
            aliasSQL = dao.translator.apply(buffer.toString());
            buffer.setLength(0);
        } else {
            namePatterns = null;
            aliasSQL = null;
        }
        /*
         * Prepare the first part of SQL statement, which may be like below:
         *
         *    SELECT <codeColumn> FROM <table>
         *      WHERE CAST(<typeColumn> AS VARCHAR(80)) LIKE 'type%'
         *        AND <filter.column> IN (<filter.values>)
         *        AND (LOWER(<nameColumn>) LIKE '<name>%')
         *
         * The query is assembled in the `buffer`. The first WHERE condition specifies the desired type.
         * That condition may be absent. The next conditions specify desired values. It may be EPSG codes
         * of dependencies or parameter values as floating points. The last condition is on the object name.
         * It may be absent (typically, only datums or reference frames have that condition).
         */
        buffer.append("SELECT ").append(table.codeColumn).append(" FROM ").append(table.table);
        table.where(object, buffer);                // Unconditionally append a "WHERE" clause.
        boolean isNext = false;
        for (final Condition filter : filters) {
            isNext |= filter.appendToWhere(buffer, isNext);
        }
        /*
         * We did not finished to build the SQL query, but the remaining part may require a JDBC connection.
         * We do not use PreparedStatement because the number of parameters varies, and we may need to use a
         * Statement two times for completely different queries.
         */
        try (Statement stmt = dao.connection.createStatement()) {
            if (namePatterns != null) {
                if (isNext) buffer.append(" AND ");
                isNext = false;
                appendFilterByName(namePatterns, table.nameColumn, buffer);
                try (ResultSet result = stmt.executeQuery(aliasSQL)) {
                    while (result.next()) {
                        final int code = result.getInt(1);
                        if (!result.wasNull()) {            // Should never be null but we are paranoiac.
                            if (!isNext) {
                                isNext = true;
                                buffer.append(" OR ").append(table.codeColumn).append(" IN (");
                            } else {
                                buffer.append(',');
                            }
                            buffer.append(code);
                        }
                    }
                }
                if (isNext) buffer.append(')');
            }
            buffer.append(getSearchDomain() == Domain.ALL_DATASET
                          ? " ORDER BY ABS(DEPRECATED), "
                          : " AND DEPRECATED=0 ORDER BY ");     // Do not put spaces around "=" - SQLTranslator searches for this exact match.
            for (final Condition filter : filters) {
                filter.appendToOrderBy(buffer);
            }
            buffer.append(table.codeColumn);          // Only for making order determinist.
            /*
             * At this point the SQL query is complete. Run it, preserving order.
             * Then sort the result by taking in account the supersession table.
             */
            final var result = new LinkedHashSet<String>();     // We need to preserve order in this set.
            try (ResultSet r = stmt.executeQuery(dao.translator.apply(buffer.toString()))) {
                while (r.next()) {
                    result.add(r.getString(1));
                }
            }
            result.remove(null);                    // Should not have null element, but let be safe.
            if (result.size() > 1) {
                final Object[] id = result.toArray();
                if (dao.sort(table.unquoted(), id)) {
                    result.clear();
                    for (final Object c : id) {
                        result.add((String) c);
                    }
                }
            }
            return result;
        } catch (SQLException exception) {
            throw dao.databaseFailure(Identifier.class, String.valueOf(CollectionsExt.first(filters[0].values)), exception);
        }
    }

    /**
     * Returns a SQL pattern for the given datum name. The name is returned in all lower cases for allowing
     * case-insensitive searches. Punctuations are replaced by any sequence of characters ({@code '%'}) and
     * non-ASCII letters are replaced by any single character ({@code '_'}). The returned pattern should be
     * flexible enough for accepting all names considered equal in {@code DefaultGeodeticDatum} comparisons.
     * In case of doubt, it is okay to return a pattern accepting more names.
     *
     * @param  name    the datum name for which to return a SQL pattern.
     * @param  buffer  temporary buffer to use for creating the pattern.
     * @return the SQL pattern for the given name.
     *
     * @see org.apache.sis.referencing.datum.DefaultGeodeticDatum#isHeuristicMatchForName(String)
     */
    private static String toDatumPattern(final String name, final StringBuilder buffer) {
        int start = 0;
        if (name.startsWith(ESRI_DATUM_PREFIX)) {
            start = ESRI_DATUM_PREFIX.length();
        }
        int end = name.indexOf('(');        // Ignore "Paris" in "Nouvelle Triangulation Française (Paris)".
        if (end < 0) end = name.length();
        end = CharSequences.skipTrailingWhitespaces(name, start, end);
        buffer.setLength(0);
        SQLUtilities.toLikePattern(name, start, end, true, true, buffer);
        return buffer.toString();
    }

    /**
     * Appends to the given buffer the SQL statement for filtering datum names using a pattern created by
     * {@link #toDatumPattern(String, StringBuilder)}. This method append a SQL fragment like below:
     *
     * {@snippet lang="sql" :
     *     (LOWER(<column>) LIKE '<pattern>' OR …)
     *     }
     *
     * This method assumes that {@code namePatterns} contains at least one element.
     *
     * @param  namePatterns  the patterns created by {@link #toDatumPattern(String, StringBuilder)}.
     * @param  column        column where the search for the names.
     * @param  buffer        buffer where to add the SQL fragment.
     */
    private static void appendFilterByName(final Set<String> namePatterns, final String column, final StringBuilder buffer) {
        String separator = "(";
        for (final String pattern : namePatterns) {
            buffer.append(separator).append("LOWER(").append(column)
                  .append(") LIKE '").append(pattern).append('\'');
            separator = " OR ";
        }
        buffer.append(')');
    }
}
