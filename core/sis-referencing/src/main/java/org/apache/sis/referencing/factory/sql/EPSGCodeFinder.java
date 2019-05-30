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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.logging.Logging;

import static org.apache.sis.internal.metadata.NameToIdentifier.Simplifier.ESRI_DATUM_PREFIX;


/**
 * An implementation of {@link IdentifiedObjectFinder} which scans over a smaller set of authority codes.
 * This is used for finding the EPSG code of a given Coordinate Reference System or other geodetic object.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.7
 * @module
 */
final class EPSGCodeFinder extends IdentifiedObjectFinder {
    /**
     * The data access object to use for searching EPSG codes.
     */
    private final EPSGDataAccess dao;

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
     * Returns a set of authority codes that <strong>may</strong> identify the same object than the specified one.
     * This implementation tries to get a smaller set than what {@link EPSGDataAccess#getAuthorityCodes(Class)}
     * would produce. Deprecated objects must be last in iteration order.
     */
    @Override
    protected Set<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
        /*
         * SQL query will be of the form shown below, except that the WHERE clause will be modified
         * to accommodate the cases where <filters> is a floating point value or a list of integers.
         *
         *     SELECT <codeColumn> FROM <table> WHERE <where> = <filters>
         */
        final String      where;                // Column to use for filtering, or null if none.
        final TableInfo   table;                // Contains 'codeColumn' and 'table' names.
        final Set<Number> filters;              // Values to put in the WHERE clause, or null if none.
        boolean isFloat = false;                // Whether 'filters' shall be handled as a floating point value.
        Set<String>   namePatterns = null;      // SQL patterns for filtering by names, or null for no filtering.
        StringBuilder buffer       = null;      // Temporary buffer for building SQL queries, created when first needed.
        if (object instanceof Ellipsoid) {
            where   = "SEMI_MAJOR_AXIS";
            table   = TableInfo.ELLIPSOID;
            filters = Collections.singleton(((Ellipsoid) object).getSemiMajorAxis());
            isFloat = true;
        } else {
            final IdentifiedObject dependency;
            if (object instanceof GeneralDerivedCRS) {
                dependency = ((GeneralDerivedCRS) object).getBaseCRS();
                where      = "SOURCE_GEOGCRS_CODE";
                table      = TableInfo.CRS;
            } else if (object instanceof SingleCRS) {
                dependency = ((SingleCRS) object).getDatum();
                where      = "DATUM_CODE";
                table      = TableInfo.CRS;
            } else if (object instanceof Datum) {
                table      = TableInfo.DATUM;
                if (object instanceof GeodeticDatum) {
                    dependency = ((GeodeticDatum) object).getEllipsoid();
                    where      = "ELLIPSOID_CODE";
                } else {
                    dependency = null;
                    where      = null;
                    if (object instanceof VerticalDatum) {
                        final VerticalDatumType type = ((VerticalDatum) object).getVerticalDatumType();
                        if (type != null && !type.equals(EPSGDataAccess.VERTICAL_DATUM_TYPE)) {
                            return Collections.emptySet();
                        }
                    }
                }
                /*
                 * We currently have no better way to filter datum (or reference frames) than their names.
                 * Filtering must be at least as tolerant as AbstractDatum.isHeuristicMatchForName(String).
                 * We initialize a larger StringBuilder since SQL query using names may be long.
                 */
                buffer = new StringBuilder(350);
                namePatterns = new LinkedHashSet<>();
                namePatterns.add(toDatumPattern(object.getName().getCode(), buffer));
                for (final GenericName id : object.getAlias()) {
                    namePatterns.add(toDatumPattern(id.tip().toString(), buffer));
                }
            } else {
                // Not a supported type. Returns all codes.
                return super.getCodeCandidates(object);
            }
            /*
             * Search for the dependency.  The super.find(…) method performs a check (not documented in public API)
             * for detecting when it is invoked recursively, which is the case here. Consequently the super.find(…)
             * behavior below is slightly different than usual: since invoked recursively, super.find(…) checks the
             * cache of the ConcurrentAuthorityFactory wrapper. If found, the dependency will also be stored in the
             * cache. This is desirable since this method may be invoked (indirectly) in a loop for many CRS objects
             * sharing the same CoordinateSystem or Datum dependencies.
             */
            if (dependency != null) {
                final boolean previous = isIgnoringAxes();
                final Set<IdentifiedObject> find;
                try {
                    setIgnoringAxes(true);
                    find = find(dependency);
                } finally {
                    setIgnoringAxes(previous);
                }
                filters = new LinkedHashSet<>(Containers.hashMapCapacity(find.size()));
                for (final IdentifiedObject dep : find) {
                    Identifier id = IdentifiedObjects.getIdentifier(dep, Citations.EPSG);
                    if (id != null) try {           // Should never be null, but let be safe.
                        filters.add(Integer.parseInt(id.getCode()));
                    } catch (NumberFormatException e) {
                        Logging.recoverableException(Logging.getLogger(Loggers.CRS_FACTORY), EPSGCodeFinder.class, "getCodeCandidates", e);
                    }
                }
                if (filters.isEmpty()) {
                    // Dependency not found.
                    return Collections.emptySet();
                }
            } else {
                filters = null;
            }
        }
        /*
         * At this point we collected the information needed for creating the main SQL query. We need an
         * additional query if we are going to filter by names, since we will need to take aliases in account.
         */
        if (buffer == null) {
            buffer = new StringBuilder(200);
        }
        final String aliasSQL;
        if (namePatterns != null) {
            buffer.setLength(0);
            buffer.append("SELECT OBJECT_CODE FROM [Alias] WHERE OBJECT_TABLE_NAME='Datum'");
            // PostgreSQL does not require explicit cast when the value is a literal instead than "?".
            String separator = " AND (";
            for (final String pattern : namePatterns) {
                appendFilterByName(buffer.append(separator), "ALIAS", pattern);
                separator = " OR ";
            }
            aliasSQL = dao.translator.apply(buffer.append(')').toString());
        } else {
            aliasSQL = null;
        }
        /*
         * Prepare the first part of SQL statement:
         *
         *    SELECT <codeColumn> FROM <table> WHERE <where> = <filters>
         *
         * The filters depend on whether the search criterion is any code in a list of EPSG codes or a numeric value.
         * In the later case, the numeric value is assumed a linear distance in metres and the tolerance threshold is
         * 1 cm for a planet of the size of Earth.
         */
        buffer.setLength(0);
        buffer.append("SELECT ").append(table.codeColumn).append(" FROM ").append(table.table);
        if (filters != null) {
            table.where(object.getClass(), buffer);
            buffer.append(where);
            if (isFloat) {
                final double value = filters.iterator().next().doubleValue();
                final double tolerance = Math.abs(value * (Formulas.LINEAR_TOLERANCE / ReferencingServices.AUTHALIC_RADIUS));
                buffer.append(">=").append(value - tolerance).append(" AND ").append(where)
                      .append("<=").append(value + tolerance);
            } else {
                String separator = " IN (";
                for (Number code : filters) {
                    buffer.append(separator).append(code.intValue());
                    separator = ",";
                }
                buffer.append(')');
            }
        }
        /*
         * We did not finished to build the SQL query, but the remaining part may require a JDBC connection.
         * We do not use PreparedStatement because the number of parameters varies, and we may need to use a
         * Statement two times for completely different queries.
         */
        try (Statement stmt = dao.connection.createStatement()) {
            if (namePatterns != null) {
                String separator = (where == null) ? " WHERE (" : " AND (";
                for (final String pattern : namePatterns) {
                    appendFilterByName(buffer.append(separator), table.nameColumn, pattern);
                    separator = " OR ";
                }
                boolean hasAlias = false;
                try (ResultSet result = stmt.executeQuery(aliasSQL)) {
                    while (result.next()) {
                        final int code = result.getInt(1);
                        if (!result.wasNull()) {            // Should never be null but we are paranoiac.
                            buffer.append(separator);
                            if (!hasAlias) {
                                hasAlias = true;
                                buffer.append(table.codeColumn).append(" IN (");
                            }
                            buffer.append(code);
                            separator = ", ";
                        }
                    }
                }
                if (hasAlias) buffer.append(')');
                buffer.append(')');
            }
            buffer.append(getSearchDomain() == Domain.ALL_DATASET
                          ? " ORDER BY ABS(DEPRECATED), "
                          : " AND DEPRECATED=0 ORDER BY ");     // Do not put spaces around "=" - SQLTranslator searches for this exact match.
            if (isFloat) {
                @SuppressWarnings("null")
                final double value = filters.iterator().next().doubleValue();
                buffer.append("ABS(").append(where).append('-').append(value).append("), ");
            }
            buffer.append(table.codeColumn);          // Only for making order determinist.
            /*
             * At this point the SQL query is complete. Run it, preserving order.
             * Then sort the result by taking in account the supersession table.
             */
            final Set<String> result = new LinkedHashSet<>();       // We need to preserve order in this set.
            try (ResultSet r = stmt.executeQuery(dao.translator.apply(buffer.toString()))) {
                while (r.next()) {
                    result.add(r.getString(1));
                }
            }
            result.remove(null);                    // Should not have null element, but let be safe.
            if (result.size() > 1) {
                final Object[] id = result.toArray();
                if (dao.sort(table.tableName(), id)) {
                    result.clear();
                    for (final Object c : id) {
                        result.add((String) c);
                    }
                }
            }
            return result;
        } catch (SQLException exception) {
            throw dao.databaseFailure(Identifier.class, String.valueOf(CollectionsExt.first(filters)), exception);
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
     * {@link #toDatumPattern(String, StringBuilder)}.
     */
    private static void appendFilterByName(final StringBuilder buffer, final String column, final String pattern) {
        buffer.append("LOWER(").append(column).append(") LIKE '").append(pattern).append('\'');
    }
}
