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

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.metadata.sql.privy.Reflection;
import org.apache.sis.metadata.sql.privy.SQLUtilities;
import org.apache.sis.referencing.internal.Resources;


/**
 * Translator of <abbr>SQL</abbr> statements for variations in schema, table and column names.
 * The {@link #apply(String)} method is invoked when a new {@link PreparedStatement}
 * is about to be created from a <abbr>SQL</abbr> string.
 * That method can modify the string before execution.
 * For example, the following <abbr>SQL</abbr> query:
 *
 * <ul>
 *   <li>{@code SELECT * FROM "Coordinate Reference System"}</li>
 * </ul>
 *
 * can be translated to one of the following possibilities:
 *
 * <ul>
 *   <li>{@code SELECT * FROM "Coordinate Reference System"} (no change)</li>
 *   <li>{@code SELECT * FROM CoordinateReferenceSystem} (without quote, so the case will be database-dependent)</li>
 *   <li>{@code SELECT * FROM epsg_CoordinateReferenceSystem} (same as above with {@code "epsg_"} prefix added)</li>
 * </ul>
 *
 * Some differences are the lower or camel case, the spaces between words and the {@code "epsg_"} prefix.
 * Those differences exist because the <abbr>EPSG</abbr> database is distributed in two family of formats:
 * as an MS-Access file and as <abbr>SQL</abbr> scripts, and those two families use different table names.
 * In addition to format-dependent differences, there is also changes in the database schema between some
 * versions of the <abbr>EPSG</abbr> dataset.
 *
 * <h2>Table naming convention</h2>
 * For readability reasons,
 * Apache <abbr>SIS</abbr> generally uses the naming convention found in the MS-Access database,
 * except for the cases shown in the "Name in Apache <abbr>SIS</abbr>" column of the table below.
 * The <abbr>SQL</abbr> statements given to the {@link #apply(String)} method use the latter.
 * The following table gives the mapping between the naming conventions:
 *
 * <table class="sis">
 *   <caption>Mapping of table names</caption>
 *   <tr><th>Name in <abbr>SQL</abbr> scripts</th>      <th>Name in MS-Access database</th>                  <th>Name in Apache <abbr>SIS</abbr></th></tr>
 *   <tr><td>{@code epsg_alias}</td>                    <td>{@code Alias}</td>                               <td>″</td></tr>
 *   <tr><td>{@code epsg_area}</td>                     <td>{@code Area}</td>                                <td>″</td></tr>
 *   <tr><td>{@code epsg_change}</td>                   <td>{@code Change}</td>                              <td>″</td></tr>
 *   <tr><td>{@code epsg_conventionalrs}</td>           <td>{@code ConventionalRS}</td>                      <td>{@code Conventional RS}</td></tr>
 *   <tr><td>{@code epsg_coordinateaxis}</td>           <td>{@code Coordinate Axis}</td>                     <td>″</td></tr>
 *   <tr><td>{@code epsg_coordinateaxisname}</td>       <td>{@code Coordinate Axis Name}</td>                <td>″</td></tr>
 *   <tr><td>{@code epsg_coordoperation}</td>           <td>{@code Coordinate_Operation}</td>                <td>″</td></tr>
 *   <tr><td>{@code epsg_coordoperationmethod}</td>     <td>{@code Coordinate_Operation Method}</td>         <td>″</td></tr>
 *   <tr><td>{@code epsg_coordoperationparam}</td>      <td>{@code Coordinate_Operation Parameter}</td>      <td>″</td></tr>
 *   <tr><td>{@code epsg_coordoperationparamusage}</td> <td>{@code Coordinate_Operation Parameter Usage}</td><td>″</td></tr>
 *   <tr><td>{@code epsg_coordoperationparamvalue}</td> <td>{@code Coordinate_Operation Parameter Value}</td><td>″</td></tr>
 *   <tr><td>{@code epsg_coordoperationpath}</td>       <td>{@code Coordinate_Operation Path}</td>           <td>″</td></tr>
 *   <tr><td>{@code epsg_coordinatereferencesystem}</td><td>{@code Coordinate Reference System}</td>         <td>″</td></tr>
 *   <tr><td>{@code epsg_coordinatesystem}</td>         <td>{@code Coordinate System}</td>                   <td>″</td></tr>
 *   <tr><td>{@code epsg_datum}</td>                    <td>{@code Datum}</td>                               <td>″</td></tr>
 *   <tr><td>{@code epsg_datumensemble}</td>            <td>{@code DatumEnsemble}</td>                       <td>{@code Datum Ensemble}</td></tr>
 *   <tr><td>{@code epsg_datumensemblemember}</td>      <td>{@code DatumEnsembleMember}</td>                 <td>{@code Datum Ensemble Member}</td></tr>
 *   <tr><td>{@code epsg_datumrealizationmethod}</td>   <td>{@code DatumRealizationMethod}</td>              <td>{@code Datum Realization Method}</td></tr>
 *   <tr><td>{@code epsg_definingoperation}</td>        <td>{@code DefiningOperation}</td>                   <td>{@code Defining Operation}</td></tr>
 *   <tr><td>{@code epsg_deprecation}</td>              <td>{@code Deprecation}</td>                         <td>″</td></tr>
 *   <tr><td>{@code epsg_ellipsoid}</td>                <td>{@code Ellipsoid}</td>                           <td>″</td></tr>
 *   <tr><td>{@code epsg_extent}</td>                   <td>{@code Extent}</td>                              <td>″</td></tr>
 *   <tr><td>{@code epsg_namingsystem}</td>             <td>{@code Naming System}</td>                       <td>″</td></tr>
 *   <tr><td>{@code epsg_primemeridian}</td>            <td>{@code Prime Meridian}</td>                      <td>″</td></tr>
 *   <tr><td>{@code epsg_scope}</td>                    <td>{@code Scope}</td>                               <td>″</td></tr>
 *   <tr><td>{@code epsg_supersession}</td>             <td>{@code Supersession}</td>                        <td>″</td></tr>
 *   <tr><td>{@code epsg_unitofmeasure}</td>            <td>{@code Unit of Measure}</td>                     <td>″</td></tr>
 *   <tr><td>{@code epsg_versionhistory}</td>           <td>{@code Version History}</td>                     <td>″</td></tr>
 *   <tr><td>{@code epsg_usage}</td>                    <td>{@code Usage}</td>                               <td>″</td></tr>
 * </table>
 *
 * Columns have the same name in all formats, with only one exception:
 * for avoiding confusion with the <abbr>SQL</abbr> keyword of the same name,
 * the {@code ORDER} column in MS-Access has been renamed {@code coord_axis_order} in <abbr>SQL</abbr> scripts.
 * Apache <abbr>SIS</abbr> uses the latter.
 *
 * <p>Apache <abbr>SIS</abbr> automatically detects which name convention is used, regardless the database engine.
 * For example, it is legal to use the mixed-case variant in a PostgreSQL database
 * even if <abbr>EPSG</abbr> distributes the PostgreSQL scripts in lower-case.
 * The {@code "epsg_"} prefix is redundant with database schema and can be omitted.
 * {@code SQLTranslator} automatically detects which database schema contains the <abbr>EPSG</abbr> tables.</p>
 *
 * <h2>Versions of <abbr>EPSG</abbr> schema</h2>
 * Apache <abbr>SIS</abbr> assumes an <abbr>EPSG</abbr> database schema version 10 or latter.
 * If {@link EPSGFactory} is connected to the <abbr>EPSG</abbr> version 9 database,
 * then this {@code SQLTranslator} class will modify the <abbr>SQL</abbr> statements on-the-fly.
 *
 * <h2>Thread safety</h2>
 * All {@code SQLTranslator} instances given to the {@link EPSGFactory} constructor
 * <strong>shall</strong> be immutable and thread-safe.
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Didier Richard (IGN)
 * @author  John Grange
 * @version 1.5
 * @since   0.7
 */
public class SQLTranslator implements UnaryOperator<String> {
    /**
     * The prefix in table names. The SQL scripts are provided by EPSG with this prefix in front of all table names.
     * SIS rather uses a modified version of those SQL scripts which creates the tables in an "EPSG" database schema.
     * But we still need to check for existence of this prefix in case someone used the original SQL scripts.
     *
     * @see #usePrefixedTableNames
     */
    static final String TABLE_PREFIX = "epsg_";

    /**
     * The columns that may be of {@code BOOLEAN} type instead of {@code SMALLINT}.
     */
    private static final String[] BOOLEAN_COLUMNS = {
        "SHOW_CRS",
        "SHOW_OPERATION",
        "DEPRECATED"
    };

    /**
     * The column where {@code VARCHAR} value may need to be cast to an enumeration.
     * With PostgreSQL, only columns in the {@code WHERE} part of the <abbr>SQL</abbr> statement
     * needs an explicit cast, as the columns in the {@code SELECT} part are implicitly cast.
     */
    private static final String ENUMERATION_COLUMN = "OBJECT_TABLE_NAME";           // In "Alias" table.

    /**
     * The name of the catalog that contains the EPSG tables, or {@code null} or an empty string.
     * <ul>
     *   <li>The {@code ""} value retrieves the <abbr>EPSG</abbr> schema without a catalog.</li>
     *   <li>The {@code null} value means that the catalog name should not be used to narrow the search.</li>
     * </ul>
     *
     * @see #getCatalog()
     */
    private String catalog;

    /**
     * The name of the schema that contains the EPSG tables, or {@code null} or an empty string.
     * <ul>
     *   <li>The {@code ""} value retrieves the EPSG tables without a schema.
     *       In such case, table names are typically prefixed by {@value #TABLE_PREFIX}.</li>
     *   <li>The {@code null} value means that the schema name should not be used to narrow the search.
     *       In such case, {@code SQLTranslator} will try to automatically detect the schema.</li>
     * </ul>
     *
     * Note that the <abbr>SQL</abbr> scripts distributed by <abbr>EPSG</abbr> do not use schema.
     * Instead, <abbr>EPSG</abbr> puts an {@value #TABLE_PREFIX} prefix in front of every table names.
     * The {@link #usePrefixedTableNames} flag tells whether it is the case for the current database.
     * The two approaches (schema or prefix) are redundant, but it is okay to use both of them anyway.
     *
     * @see #getSchema()
     * @see #usePrefixedTableNames
     */
    private String schema;

    /**
     * Whether the table names are prefixed by {@value #TABLE_PREFIX}. The <abbr>EPSG</abbr> geodetic dataset
     * in the {@code org.apache.sis.referencing.epsg} module replaces that prefix by the {@code "epsg"} schema.
     * However, if the dataset has been installed manually by the user, then the table name prefix is present.
     *
     * @see #TABLE_PREFIX
     * @see #schema
     */
    private boolean usePrefixedTableNames;

    /**
     * {@code true} if this class uses mixed-case convention for table names.
     * In such case, the table names will need to be quoted using the {@link #quote} character.
     */
    private boolean useMixedCaseTableNames;

    /**
     * Mapping from mixed-case "word" used by {@link EPSGDataAccess} to the words actually used by the database.
     * A word may be a table name or a part of it. A table name may consist of many words separated by spaces.
     * This map does not list all tables used in <abbr>EPSG</abbr> schema, but only the ones that cannot be mapped
     * by more generic code (e.g. by replacing spaces by '_').
     *
     * <p>The keys are names that are hard-coded in the <abbr>SQL</abbr> statements of {@link EPSGDataAccess} class,
     * and values are the names used in the <abbr>EPSG</abbr> database on which {@link EPSGDataAccess} is connected.
     * By convention, all column names are in upper-case while table names are in mixed-case characters.</p>
     */
    private Map<String,String> tableRewording;

    /**
     * Mapping from column names used by {@link EPSGDataAccess} to the names actually used by the database.
     * The {@code COORD_AXIS_ORDER} column may be {@code ORDER} in the MS-Access database.
     * This map is rarely non-empty.
     */
    private Map<String,String> columnRenaming;

    /**
     * The characters used for quoting identifiers, or a whitespace if none.
     * This information is provided by {@link DatabaseMetaData#getIdentifierQuoteString()}.
     */
    private final String identifierQuote;

    /**
     * Non-null if the {@value #ENUMERATION_COLUMN} column in {@code "Alias"} table uses enumeration instead
     * than character varying. In such case, this field contains the enumeration type. If {@code null}, then
     * then column type is {@code VARCHAR} and the cast can be omitted.
     *
     * @see #useEnumerations()
     */
    private String tableNameEnum;

    /**
     * {@code true} if the database uses the {@code BOOLEAN} type instead of {@code SMALLINT}
     * for the {@code show_crs}, {@code show_operation} and all {@code deprecated} fields.
     *
     * @see #useBoolean()
     */
    private boolean useBoolean;

    /**
     * Whether the sentinel table {@code "Coordinate_Operation"} or a variant has been found.
     * If {@code false}, then {@link EPSGInstaller} needs to be run.
     * Note that the {@linkplain #schema} may be {@code null}.
     *
     * @see #isSchemaFound()
     */
    private boolean isSchemaFound;

    /**
     * Whether the "Usage" table has been found.
     * That table has been added in version 10 of <abbr>EPSG</abbr> database.
     *
     * @see #isUsageTableFound()
     */
    private boolean isUsageTableFound;

    /**
     * Whether the table names in the hard-coded <abbr>SQL</abbr> queries and in the actual database are the same.
     * If {@code true}, then the renaming of tables can be skipped.
     */
    private boolean sameTableNames;

    /**
     * Whether the <abbr>SQL</abbr> queries hard-coded in {@link EPSGDataAccess} can be used verbatim
     * with the actual database. This is {@code true} if this translator applies no change at all.
     */
    private boolean sameQueries;

    /**
     * Creates a new <abbr>SQL</abbr> translator for the database described by the given metadata.
     * This constructor detects automatically the dialect: the characters to use for quoting identifiers,
     * and whether the <abbr>EPSG</abbr> table names uses the mixed-case or lower-case convention.
     *
     * <p>If the given catalog or schema name is non-null, then the {@linkplain DatabaseMetaData#getTables
     * search for EPSG tables} will be restricted to the catalog or schema of that name.
     * An empty string ({@code ""}) means to search for tables without catalog or schema.
     * A {@code null} value means that the catalog or schema should not be used to narrow the search.</p>
     *
     * @param  md       information about the database.
     * @param  catalog  the catalog where to look for EPSG schema, or {@code null} if any.
     * @param  schema   the schema where to look for EPSG tables, or {@code null} if any.
     * @throws SQLException if an error occurred while querying the database metadata.
     */
    public SQLTranslator(final DatabaseMetaData md, final String catalog, final String schema) throws SQLException {
        identifierQuote = md.getIdentifierQuoteString().trim();
        this.catalog = catalog;
        this.schema  = schema;
        setup(md);
    }

    /**
     * Sets the value of all non-final fields. This method performs the following steps:
     *
     * <ol class="verbose">
     *   <li>Find the schema that seems to contain the <abbr>EPSG</abbr> tables.
     *       If there is more than one schema containing the tables, give precedence to the schema named
     *       {@code "EPSG"} (ignoring case), or an arbitrary schema if none has been found with that name.
     *       The schema name may be the empty string if the tables are not contained in a schema.</li>
     *
     *   <li>Determine whether the table names are prefixed by {@value #TABLE_PREFIX}
     *       and whether table names are in lower-case or mixed-case.</li>
     *
     *   <li>Fill the {@link #tableRewording} and {@link #columnRenaming} maps. These maps translate table
     *       and column names used in the <abbr>SQL</abbr> statements into the names used by the database.
     *       Two conventions are understood: the names used in the MS-Access database or the names used
     *       in the <abbr>SQL</abbr> scripts, potentially with {@linkplain #TABLE_PREFIX prefix} removed.</li>
     * </ol>
     */
    @SuppressWarnings("fallthrough")
    final void setup(final DatabaseMetaData md) throws SQLException {
        final String escape  = md.getSearchStringEscape();
        String schemaPattern = SQLUtilities.escape(schema, escape);
        int tableIndex = 0;
        do {
            usePrefixedTableNames  = false;
            useMixedCaseTableNames = false;
            String table = "";
            switch (tableIndex++) {
                case 0: {   // Test EPSG standard table name first.
                    usePrefixedTableNames = true;
                    table = SQLUtilities.escape(TABLE_PREFIX, escape);
                    // Fallthrough for testing "epsg_coordoperation".
                }
                case 2: {
                    table += "coordoperation";      // Prefixed by "epsg_" in case 0.
                    if (md.storesUpperCaseIdentifiers()) {
                        table = table.toUpperCase(Locale.US);
                    }
                    break;
                }
                case 1: {   // Variant used by the Apache SIS installer.
                    useMixedCaseTableNames = true;
                    table = "Coordinate_Operation";
                    break;
                }
                default: return;    // EPSG table not found.
            }
            try (ResultSet result = md.getTables(catalog, schemaPattern, table, null)) {
                while (result.next()) {
                    isSchemaFound = true;
                    catalog = result.getString(Reflection.TABLE_CAT);
                    schema  = result.getString(Reflection.TABLE_SCHEM);
                    if (result.wasNull()) schema = "";
                    if (Constants.EPSG.equalsIgnoreCase(schema)) {
                        break;  // Give precedence to "epsg" schema.
                    }
                }
            }
        } while (!isSchemaFound);
        /*
         * At this point, we found the EPSG sentinel table and we identified the
         * naming convention (unquoted or mixed-case, prefixed by "epsg_" or not).
         */
        UnaryOperator<String> toNativeCase = UnaryOperator.identity();
        schemaPattern  = SQLUtilities.escape(schema, escape);
        columnRenaming = new HashMap<>();
        tableRewording = new HashMap<>();
        /*
         * Special cases not covered by the generic algorithm implemented in `toActualTableName(…)`.
         * The entries are actually not full table names, but words separated by space. For example,
         * "Coordinate_Operation Parameter Value" will be renamed "epsg_CoordOperationParamValue"
         * (note that this renaming combines the two entries of the map).
         */
        if (!useMixedCaseTableNames) {
            tableRewording.put("Coordinate_Operation", "coordoperation");
            tableRewording.put("Parameter",            "param");
            if (md.storesLowerCaseIdentifiers()) {
                toNativeCase = (table) -> table.toLowerCase(Locale.US);
            } else if (md.storesUpperCaseIdentifiers()) {
                toNativeCase = (table) -> table.toUpperCase(Locale.US);
            }
        }
        /*
         * Check if some columns need to be renamed in SQL queries. For example, MS-Access database
         * uses a column named "ORDER" instead of "COORD_AXIS_ORDER" in the "Coordinate Axis" table.
         * The other changes are differences between EPSG version 9 and version 10. For example,
         * column "BASE_CRS_CODE" in EPSG version 10 was named "SOURCE_GEOGCRS_CODE" in version 9.
         *
         * Furthermore, some columns in EPSG version 10 did not existed in EPSG version 9.
         * This loop also checks for columns that do not exist, and replaces them by NULL constant.
         */
        final var missingColumns   = new HashMap<String, String>();
        final var mayRenameColumns = new HashMap<String, String>();
        final var brokenTargetCols = new HashSet<String>();
        tableIndex = 0;
check:  for (;;) {
            String table;
            boolean isUsage  = false;   // "Usage"  is a new table in EPSG version 19.
            boolean isArea   = false;   // "Area"   was a table name used in EPSG version 9.
            boolean mayReuse = false;   // If true, do not clear the maps if the table was not found.
            switch (tableIndex++) {
                case 0: {
                    table = "Coordinate Axis";
                    mayRenameColumns.put("COORD_AXIS_ORDER", "ORDER");               // SQL script → MS-Access.
                    break;
                }
                case 1: {
                    table = "Coordinate_Operation";
                    missingColumns.put("AREA_OF_USE_CODE", "INTEGER");               // Defined in version 9, deprecated in 10+.
                    missingColumns.put("COORD_OP_SCOPE",   "CHAR(1)");               // Idem.
                    break;
                }
                case 2: {
                    table = "Coordinate Reference System";
                    mayRenameColumns.put("BASE_CRS_CODE", "SOURCE_GEOGCRS_CODE");    // EPSG version 10 → version 9.
                    missingColumns  .put("AREA_OF_USE_CODE", "INTEGER");             // Defined in version 9, deprecated in 10+.
                    missingColumns  .put("CRS_SCOPE",        "CHAR(1)");             // Idem.
                    break;
                }
                case 3: {
                    table = "Datum";
                    mayRenameColumns.put("PUBLICATION_DATE",        "REALIZATION_EPOCH"); // EPSG version 10 → version 9.
                    missingColumns  .put("ANCHOR_EPOCH",            "DOUBLE PRECISION");
                    missingColumns  .put("FRAME_REFERENCE_EPOCH",   "DOUBLE PRECISION");
                    missingColumns  .put("REALIZATION_METHOD_CODE", "INTEGER");
                    missingColumns  .put("CONVENTIONAL_RS_CODE",    "INTEGER");
                    missingColumns  .put("AREA_OF_USE_CODE",        "INTEGER");           // Defined in version 9, deprecated in 10+.
                    missingColumns  .put("DATUM_SCOPE",             "CHAR(1)");           // Idem.
                    break;
                }
                case 4: {
                    table = "Extent";                                                // "Area" in 9, "Extent" in 10.
                    mayRenameColumns.put("EXTENT_CODE",              "AREA_CODE");   // EPSG version 10 → version 9.
                    mayRenameColumns.put("EXTENT_NAME",              "AREA_NAME");
                    mayRenameColumns.put("EXTENT_DESCRIPTION",       "AREA_OF_USE");
                    mayRenameColumns.put("BBOX_SOUTH_BOUND_LAT",     "AREA_SOUTH_BOUND_LAT");
                    mayRenameColumns.put("BBOX_NORTH_BOUND_LAT",     "AREA_NORTH_BOUND_LAT");
                    mayRenameColumns.put("BBOX_WEST_BOUND_LON",      "AREA_WEST_BOUND_LON");
                    mayRenameColumns.put("BBOX_EAST_BOUND_LON",      "AREA_EAST_BOUND_LON");
                    missingColumns  .put("VERTICAL_EXTENT_MIN",      "DOUBLE PRECISION");
                    missingColumns  .put("VERTICAL_EXTENT_MAX",      "DOUBLE PRECISION");
                    missingColumns  .put("VERTICAL_EXTENT_CRS_CODE", "INTEGER");
                    missingColumns  .put("TEMPORAL_EXTENT_BEGIN",    "CHAR(1)");
                    missingColumns  .put("TEMPORAL_EXTENT_END",      "CHAR(1)");
                    mayReuse = true;
                    break;
                }
                case 5: {
                    if (mayRenameColumns.isEmpty()) continue;   // "Extent" table has been found.
                    isArea = true;
                    table = "Area";
                    break;
                }
                case 6: {
                    isUsage = true;
                    table = "Usage";
                    break;
                }
                case 7: {
                    if (isUsageTableFound) break check; // The check of `ENUMERATION_COLUMN` is already done.
                    table = "Alias";                    // For checking the type of the `ENUMERATION_COLUMN`.
                    break;
                }
                default: break check;
            }
            boolean isTableFound = false;
            brokenTargetCols.addAll(mayRenameColumns.values());
            table = toNativeCase.apply(toActualTableName(table));
            try (ResultSet result = md.getColumns(catalog, schemaPattern, SQLUtilities.escape(table, escape), "%")) {
                while (result.next()) {
                    isTableFound = true;          // Assuming that all tables contain at least one column.
                    final String column = result.getString(Reflection.COLUMN_NAME).toUpperCase(Locale.US);
                    missingColumns.remove(column);
                    if (mayRenameColumns.remove(column) == null) {  // Do not rename if the new column exists.
                        brokenTargetCols.remove(column);            // Remember which old names were found.
                    }
                    /*
                     * Detect if the database uses boolean types where applicable.
                     * We arbitrarily use the `deprecated` column as a representative value.
                     */
                    if ("DEPRECATED".equals(column)) {
                        final int type = result.getInt(Reflection.DATA_TYPE);
                        useBoolean |= (type == Types.BOOLEAN) || (type == Types.BIT);
                    }
                    /*
                     * Detect if the tables use enumeration (on PostgreSQL database) instead of VARCHAR.
                     * Enumerations appear in various tables, including in a WHERE clause for the Alias table.
                     */
                    if (ENUMERATION_COLUMN.equals(column)) {
                        final String type = result.getString(Reflection.TYPE_NAME);
                        if (!CharSequences.startsWith(type, "VARCHAR", true)) {
                            tableNameEnum = type;
                        }
                    }
                }
            }
            if (isTableFound) {
                isUsageTableFound  |= isUsage;
                if (isArea) {
                    tableRewording.put("Extent", "Area");
                }
                missingColumns.forEach((column, type) -> {
                    columnRenaming.put(column, "CAST(NULL AS " + type + ") AS " + column);
                });
                mayRenameColumns.values().removeAll(brokenTargetCols);  // For renaming only when the old name has been found.
                columnRenaming.putAll(mayRenameColumns);
                mayReuse = false;
            }
            if (!mayReuse) {
                mayRenameColumns.clear();
                brokenTargetCols.clear();
                missingColumns.clear();
            }
        }
        tableRewording = Map.copyOf(tableRewording);
        columnRenaming = Map.copyOf(columnRenaming);
        sameTableNames = useMixedCaseTableNames && "\"".equals(identifierQuote) && tableRewording.isEmpty();
        sameQueries    = sameTableNames && (tableNameEnum == null) && columnRenaming.isEmpty() && !useBoolean;
    }

    /**
     * Returns the catalog that contains the EPSG schema. This is the catalog specified at construction time
     * if it was non-null, or the catalog discovered by the constructor otherwise.
     * Note that this method may still return {@code null} if the EPSG tables were not found or if the database
     * does not {@linkplain DatabaseMetaData#supportsCatalogsInDataManipulation() supports catalogs}.
     *
     * @return the catalog that contains the EPSG schema, or {@code null}.
     */
    @OptionalCandidate
    public String getCatalog() {
        return catalog;
    }

    /**
     * Returns the schema that contains the EPSG tables. This is the schema specified at construction time
     * if it was non-null, or the schema discovered by the constructor otherwise.
     * Note that this method may still return {@code null} if the EPSG tables were not found or if the database
     * does not {@linkplain DatabaseMetaData#supportsSchemasInDataManipulation() supports schemas}.
     *
     * @return the schema that contains the EPSG tables, or {@code null}.
     */
    @OptionalCandidate
    public String getSchema() {
        return schema;
    }

    /**
     * Returns whether the <abbr>EPSG</abbr> tables have been found.
     * If {@code false}, then {@link EPSGInstaller} needs to be run.
     * Note that the {@linkplain #getSchema() schema} may be {@code null}.
     */
    final boolean isSchemaFound() {
        return isSchemaFound;
    }

    /**
     * Returns whether the "Usage" table has been found.
     * That table has been added in version 10 of <abbr>EPSG</abbr> database.
     */
    final boolean isUsageTableFound() {
        return isUsageTableFound;
    }

    /**
     * Returns the error message for the exception to throw if the EPSG tables are not found and we cannot create them.
     */
    static String tableNotFound(final DatabaseMetaData md, final Locale locale) throws SQLException {
        String db = md.getURL();
        if (db == null) {
            db = "?";
        } else {
            int s = db.indexOf('?');
            if (s >= 0 || (s = db.indexOf('#')) >= 0) {
                db = db.substring(9, s);
            }
        }
        return Resources.forLocale(locale).getString(Resources.Keys.TableNotFound_3,
                Constants.EPSG, db, "Coordinate_Operation");
    }

    /**
     * Returns {@code true} if the database uses the {@code BOOLEAN} type instead of {@code SMALLINT}
     * for the {@code show_crs}, {@code show_operation} and all {@code deprecated} fields.
     */
    final boolean useBoolean() {
        return useBoolean;
    }

    /**
     * Returns {@code true} if the database uses enumeration values where applicable.
     * This method use the {@value #ENUMERATION_COLUMN} column as a sentinel value for
     * detecting whether enumerations are used for the whole <abbr>EPSG</abbr> database.
     */
    final boolean useEnumerations() {
        return tableNameEnum != null;
    }

    /**
     * Converts a mixed-case table name to the convention used in the database.
     * The names of the tables for the two conventions are listed in a table in the Javadoc of this class.
     * The returned string does not include the identifier quotes.
     *
     * @param  name  the mixed-case table name.
     * @return the name converted to the convention used by the database.
     */
    final String toActualTableName(String name) {
        if (useMixedCaseTableNames) {
            return name;
        }
        final var buffer = new StringBuilder(name.length() + 5);
        toActualTableName(name, buffer);
        return buffer.toString().toLowerCase(Locale.US);
    }

    /**
     * Appends the table name in the given buffer, converted to the convention used in the database.
     * Identifier quotes are added when mixed-case is used. Otherwise, the identifier may not be in
     * the right lower/upper case. But when there is no identifier quotes, the database should convert
     * to* whatever convention it uses.
     */
    private void toActualTableName(final String name, final StringBuilder buffer) {
        if (useMixedCaseTableNames) {
            buffer.append(identifierQuote)
                  .append(tableRewording.getOrDefault(name, name))
                  .append(identifierQuote);
        } else {
            if (usePrefixedTableNames) {
                buffer.append(TABLE_PREFIX);
            }
            for (final String word : name.split("\\s")) {
                buffer.append(tableRewording.getOrDefault(word, word));
            }
            // Ignore lower/upper case.
        }
    }

    /**
     * Adapts the given <abbr>SQL</abbr> statement from the mixed-case convention to the convention used by
     * the target database. The mixed-case convention is used by {@link EPSGDataAccess} hard-coded queries.
     * This method can replace the schema and table names, and sometime some <abbr>SQL</abbr> keywords,
     * for complying with the expectation of the target database.
     *
     * @param  sql  a <abbr>SQL</abbr> statement with table names in mixed-case convention.
     * @return the given statement adapted to the expectation of the target database.
     */
    @Override
    public String apply(final String sql) {
        if (sameQueries) {
            return sql;     // Shortcut when there is nothing to change.
        }
        final var buffer = new StringBuilder(sql.length() + 16);
        int end = 0;
        if (!sameTableNames) {
            int start;
            while ((start = sql.indexOf('"', end)) >= 0) {
                /*
                 * Append every characters since the end of the last processed table/column name,
                 * or since the beginning of the SQL statement if we are in the first iteration.
                 * Then find the end of the new table/column name to process in this iteration.
                 */
                buffer.append(sql, end, start);
                if ((end = sql.indexOf('"', ++start)) < 0) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.MissingCharacterInElement_2, sql.substring(start), '"'));
                }
                toActualTableName(sql.substring(start, end++), buffer);
            }
        }
        buffer.append(sql, end, sql.length());
        columnRenaming.forEach((toSearch, replaceBy) -> StringBuilders.replace(buffer, toSearch, replaceBy));
        /*
         * If the database use the BOOLEAN type instead of SMALLINT, replaces "deprecated=0' by "deprecated=false".
         */
        if (useBoolean) {
            int w = buffer.indexOf("WHERE");
            if (w >= 0) {
                w += 5;
                for (final String field : BOOLEAN_COLUMNS) {
                    int p = buffer.indexOf(field, w);
                    if (p >= 0) {
                        p += field.length();
                        if (!replaceIfEquals(buffer, p, "=0", "=FALSE") &&
                            !replaceIfEquals(buffer, p, "<>0", "=TRUE"))
                        {
                            // Remove "ABS" in "ABS(DEPRECATED)" or "ABS(CO.DEPRECATED)".
                            if ((p = buffer.lastIndexOf("(", p)) > w) {
                                replaceIfEquals(buffer, p-3, "ABS", "");
                            }
                        }
                    }
                }
            }
        }
        /*
         * If the database uses enumeration, we need an explicit cast with PostgreSQL.
         * The enumeration type is typically "EPSG"."Table Name".
         */
        if (tableNameEnum != null) {
            int w = buffer.lastIndexOf(ENUMERATION_COLUMN + "=?");
            if (w >= 0) {
                w += ENUMERATION_COLUMN.length() + 1;
                buffer.replace(w, w+1, "CAST(? AS \"" + tableNameEnum + "\")");
            }
        }
        return buffer.toString();
    }

    /**
     * Replaces the text at the given position in the buffer if it is equal to the {@code expected} text.
     */
    private static boolean replaceIfEquals(final StringBuilder buffer, final int pos,
            final String expected, final String replacement)
    {
        if (CharSequences.regionMatches(buffer, pos, expected)) {
            buffer.replace(pos, pos + expected.length(), replacement);
            return true;
        }
        return false;
    }
}
