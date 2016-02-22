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
import java.util.Locale;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Constants;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Converts the SQL statements from MS-Access dialect to standard SQL. The {@link #apply(String)} method
 * is invoked when a new {@link java.sql.PreparedStatement} is about to be created from a SQL string.
 * Since the <a href="http://www.epsg.org">EPSG dataset</a> is available primarily in MS-Access format,
 * the original SQL statements are formatted using a dialect specific to that particular database software.
 * If the actual EPSG dataset to query is hosted on another database product, then the SQL query needs to be
 * adapted to the target database dialect before to be executed.
 *
 * <div class="note"><b>Example</b>
 * SQL statements for an EPSG dataset hosted on the <cite>PostgreSQL</cite> database need to have their brackets
 * ({@code '['} and {@code ']'}) replaced by the quote character ({@code '"'}) before to be sent to the database
 * driver. Furthermore table names may be different. So the following MS-Access query:
 *
 * <ul>
 *   <li>{@code SELECT * FROM [Coordinate Reference System]}</li>
 * </ul>
 *
 * needs to be converted to one of the following possibilities for a PostgreSQL database
 * (the reason for those multiple choices will be discussed later):
 *
 * <ul>
 *   <li>{@code SELECT * FROM "Coordinate Reference System"}</li>
 *   <li>{@code SELECT * FROM epsg_coordinatereferencesystem} (in the default schema)</li>
 *   <li>{@code SELECT * FROM epsg.coordinatereferencesystem} (in the {@code "epsg"} schema)</li>
 *   <li>{@code SELECT * FROM epsg."Coordinate Reference System"}</li>
 * </ul></div>
 *
 * In addition to the file in MS-Access format, EPSG also provides the dataset as SQL files for PostgreSQL,
 * MySQL and Oracle databases. Those SQL files are used as both <cite>Data Description Language</cite> (DDL)
 * and <cite>Data Manipulation Language</cite> (DML).
 * But the table names and some column names in those scripts differ from the ones used in the MS-Access database.
 * The following table summarizes the name changes:
 *
 * <table class="sis">
 *   <caption>Table and column names</caption>
 *   <tr><th>Element</th><th>Name in MS-Access database</th>                    <th>Name in SQL scripts</th></tr>
 *   <tr><td>Table</td>  <td>{@code Alias}</td>                                 <td>{@code epsg_alias}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Area}</td>                                  <td>{@code epsg_area}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate Axis}</td>                       <td>{@code epsg_coordinateaxis}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate Axis Name}</td>                  <td>{@code epsg_coordinateaxisname}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate_Operation}</td>                  <td>{@code epsg_coordoperation}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate_Operation Method}</td>           <td>{@code epsg_coordoperationmethod}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate_Operation Parameter}</td>        <td>{@code epsg_coordoperationparam}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate_Operation Parameter Usage}</td>  <td>{@code epsg_coordoperationparamusage}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate_Operation Parameter Value}</td>  <td>{@code epsg_coordoperationparamvalue}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate_Operation Path}</td>             <td>{@code epsg_coordoperationpath}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate Reference System}</td>           <td>{@code epsg_coordinatereferencesystem}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Coordinate System}</td>                     <td>{@code epsg_coordinatesystem}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Datum}</td>                                 <td>{@code epsg_datum}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Ellipsoid}</td>                             <td>{@code epsg_ellipsoid}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Naming System}</td>                         <td>{@code epsg_namingsystem}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Prime Meridian}</td>                        <td>{@code epsg_primemeridian}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Supersession}</td>                          <td>{@code epsg_supersession}</td></tr>
 *   <tr><td>Table</td>  <td>{@code Unit of Measure}</td>                       <td>{@code epsg_unitofmeasure}</td></tr>
 *   <tr><td>Column</td> <td>{@code ORDER}</td>                                 <td>{@code coord_axis_order}</td></tr>
 * </table>
 *
 * By default this class auto-detects the schema that contains the EPSG tables and whether the table names are
 * the ones used by EPSG in the MS-Access version or the PostgreSQL, MySQL or Oracle version of the database.
 * Consequently it is legal to use the MS-Access table names, which are more readable, in a PostgreSQL database.
 *
 * <div class="section">Thread safety</div>
 * All {@code SQLTranslator} instances given to the {@link EPSGFactory} constructor
 * <strong>shall</strong> be immutable and thread-safe.
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (IRD)
 * @author  Didier Richard (IGN)
 * @author  John Grange
 * @since   0.7
 * @version 0.7
 * @module
 */
public class SQLTranslator {
    /**
     * Table names used as "sentinel value" for detecting the presence of an EPSG database.
     * This array lists different possible names for the same table. The first entry must be
     * the MS-Access name. Other names may be in any order. They will be tried in reverse order.
     */
    private static final String[] SENTINEL = {
        "Coordinate Reference System",
        "coordinatereferencesystem",
        "epsg_coordinatereferencesystem"
    };

    /**
     * Index of the {@link #SENTINEL} element which is in mixed case. No other element should be in mixed case.
     */
    private static final int MIXED_CASE = 0;

    /**
     * The prefix in table names. The SQL scripts are provided by EPSG with this prefix in front of all table names.
     * SIS rather uses a modified version of those SQL scripts which creates the tables in an "EPSG" database schema.
     * But we still need to check for existence of this prefix in case someone used the original SQL scripts.
     */
    static final String TABLE_PREFIX = "epsg_";

    /**
     * The columns that may be of {@code BOOLEAN} type instead of {@code SMALLINT}.
     */
    private static final String[] BOOLEAN_FIELDS = {
        "SHOW_CRS",
        "SHOW_OPERATION",
        "DEPRECATED"
    };

    /**
     * The name of the catalog that contains the EPSG tables, or {@code null} or an empty string.
     * <ul>
     *   <li>The {@code ""} value retrieves the EPSG schema without a catalog.</li>
     *   <li>The {@code null} value means that the catalog name should not be used to narrow the search.</li>
     * </ul>
     *
     * <p><b>Consider this field as final.</b> This field is non-final only for construction convenience,
     * or for updating after the {@link EPSGInstaller} class created the database.</p>
     *
     * @see #getCatalog()
     */
    private String catalog;

    /**
     * The name of the schema that contains the EPSG tables, or {@code null} or an empty string.
     * <ul>
     *   <li>The {@code ""} value retrieves the EPSG tables without a schema.
     *       In such case, table names are prefixed by {@value #TABLE_PREFIX}.</li>
     *   <li>The {@code null} value means that the schema name should not be used to narrow the search.
     *       In such case, {@code SQLTranslator} will tries to automatically detect the schema.</li>
     * </ul>
     *
     * <p><b>Consider this field as final.</b> This field is non-final only for construction convenience,
     * or for updating after the {@link EPSGInstaller} class created the database.</p>
     *
     * @see #getSchema()
     */
    private String schema;

    /**
     * Mapping from words used in the MS-Access database to words used in the ANSI versions of EPSG databases.
     * A word may be a table or a column name, or a part of it. A table name may consist in many words separated
     * by spaces.
     *
     * <p>The keys are the names in the MS-Access database, and the values are the names in the SQL scripts.
     * By convention, all column names in keys are in upper-case while table names are in mixed-case characters.</p>
     */
    private final Map<String,String> accessToAnsi;

    /**
     * {@code true} if this class needs to quote table names. This quoting should be done only if the database
     * uses the MS-Access table names, even if we are targeting a PostgreSQL, MySQL or Oracle database.
     *
     * <p><b>Consider this field as final.</b> This field is non-final only for construction convenience,
     * or for updating after the {@link EPSGInstaller} class created the database.</p>
     */
    private boolean quoteTableNames;

    /**
     * The characters used for quoting identifiers, or a whitespace if none.
     * This information is provided by {@link DatabaseMetaData#getIdentifierQuoteString()}.
     */
    private final String quote;

    /**
     * {@code true} if the database uses the {@code BOOLEAN} type instead than {@code SMALLINT}
     * for the {@code show_crs}, {@code show_operation} and all {@code deprecated} fields.
     *
     * @see #useBoolean()
     */
    private boolean useBoolean;

    /**
     * {@code true} if one of the {@link #SENTINEL} tables exist.
     * If {@code false}, then {@link EPSGInstaller} needs to be run.
     *
     * @see #isTableFound()
     */
    private boolean isTableFound;

    /**
     * Creates a new SQL translator for the database described by the given metadata.
     * This constructor detects automatically the dialect: the characters to use for quoting identifiers,
     * and whether the table names are the ones used in the MS-Access database or in the SQL scripts.
     *
     * <p>If the given catalog or schema name is non-null, then the {@linkplain DatabaseMetaData#getTables
     * search for EPSG tables} will be restricted to the catalog or schema of that name.
     * An empty string ({@code ""}) means to search for tables without catalog or schema.
     * A {@code null} value means that the catalog or schema should not be used to narrow the search.</p>
     *
     * @param  md      Information about the database.
     * @param  catalog The catalog where to look for EPSG schema, or {@code null} if any.
     * @param  schema  The schema where to look for EPSG tables, or {@code null} if any.
     * @throws SQLException if an error occurred while querying the database metadata.
     */
    public SQLTranslator(final DatabaseMetaData md, final String catalog, final String schema) throws SQLException {
        ArgumentChecks.ensureNonNull("md", md);
        quote = md.getIdentifierQuoteString().trim();
        accessToAnsi = new HashMap<String,String>(4);
        this.catalog = catalog;
        this.schema  = schema;
        setup(md);
    }

    /**
     * Sets the value of all non-final fields. This method performs two steps:
     *
     * <ol class="verbose">
     *   <li>Finds the schema that seems to contain the EPSG tables. If there is more than one schema containing the
     *       tables, gives precedence to the schema named "EPSG" if one is found. If there is no schema named "EPSG",
     *       takes an arbitrary schema. It may be the empty string if the tables are not contained in a schema.</li>
     *
     *   <li>Fills the {@link #accessToAnsi} map. That map translates the table and column names used in the SQL
     *       statements into the names used by the database. Two conventions are understood: the names used in
     *       the MS-Access database or the names used in the SQL scripts. Both of them are distributed by EPSG.</li>
     * </ol>
     */
    final void setup(final DatabaseMetaData md) throws SQLException {
        final boolean toUpperCase = md.storesUpperCaseIdentifiers();
        for (int i = SENTINEL.length; --i >= 0;) {
            String table = SENTINEL[i];
            if (toUpperCase && i != MIXED_CASE) {
                table = table.toUpperCase(Locale.US);
            }
            final ResultSet result = md.getTables(catalog, schema, table, null);
            try {
                if (result.next()) {
                    isTableFound    = true;
                    quoteTableNames = (i == MIXED_CASE);
                    do {
                        catalog = result.getString("TABLE_CAT");
                        schema  = result.getString("TABLE_SCHEM");
                    } while (!Constants.EPSG.equalsIgnoreCase(schema) && result.next());
                    if (schema == null) schema = "";
                    break;
                }
            } finally {
                result.close();
            }
        }
        /*
         * At this point the catalog and schema have been found or confirmed, or are still null
         * if we did not found them. Now fill the 'accessToAnsi' map.
         */
        boolean translateColumns = true;
        accessToAnsi.clear();
        if (quoteTableNames) {
            /*
             * MS-Access database uses a column named "ORDER" in the "Coordinate Axis" table.
             * This column has been renamed "coord_axis_order" in DLL scripts.
             * We need to check which name our current database uses.
             */
            final ResultSet result = md.getColumns(catalog, schema, "Coordinate Axis", "ORDER");
            try {
                translateColumns = !result.next();
            } finally {
                result.close();
            }
        } else {
            accessToAnsi.put("Coordinate_Operation", "coordoperation");
            accessToAnsi.put("Parameter", "param");
        }
        if (translateColumns) {
            accessToAnsi.put("ORDER", "coord_axis_order");
        }
        /*
         * Detect if the database uses boolean types where applicable.
         * We arbitrarily use the Datum table as a representative value.
         */
        String deprecated = "DEPRECATED";
        if (md.storesLowerCaseIdentifiers()) {
            deprecated = deprecated.toLowerCase(Locale.US);
        }
        final ResultSet result = md.getColumns(catalog, schema, null, deprecated);
        try {
            while (result.next()) {
                if (CharSequences.endsWith(result.getString("TABLE_NAME"), "Datum", true)) {
                    final int type = result.getInt("DATA_TYPE");
                    useBoolean = (type == Types.BOOLEAN) || (type == Types.BIT);
                    break;
                }
            }
        } finally {
            result.close();
        }
    };

    /**
     * Returns the catalog that contains the EPSG schema. This is the catalog specified at construction time
     * if it was non-null, or the catalog discovered by the constructor otherwise.
     * Note that this method may still return {@code null} if the EPSG tables were not found or if the database
     * does not {@linkplain DatabaseMetaData#supportsCatalogsInDataManipulation() supports catalogs}.
     *
     * @return The catalog that contains the EPSG schema, or {@code null}.
     */
    public String getCatalog() {
        return catalog;
    }

    /**
     * Returns the schema that contains the EPSG tables. This is the schema specified at construction time
     * if it was non-null, or the schema discovered by the constructor otherwise.
     * Note that this method may still return {@code null} if the EPSG tables were not found or if the database
     * does not {@linkplain DatabaseMetaData#supportsSchemasInDataManipulation() supports schemas}.
     *
     * @return The schema that contains the EPSG tables, or {@code null}.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Returns whether the EPSG tables have been found.
     * If {@code false}, then {@link EPSGInstaller} needs to be run.
     */
    final boolean isTableFound() {
        return isTableFound;
    }

    /**
     * Returns the error message for the exception to throw if the EPSG tables are not found and we can not create them.
     */
    static String tableNotFound(final Locale locale) {
        return Errors.getResources(locale).getString(Errors.Keys.TableNotFound_1, SENTINEL[MIXED_CASE]);
    }

    /**
     * Returns {@code true} if the database uses the {@code BOOLEAN} type instead than {@code SMALLINT}
     * for the {@code show_crs}, {@code show_operation} and all {@code deprecated} fields.
     */
    final boolean useBoolean() {
        return useBoolean;
    }

    /**
     * If the given string is empty, returns {@code null} instead.
     */
    private static String nonEmpty(final String s) {
        return (s != null && !s.isEmpty()) ? s : null;
    }

    /**
     * Adapts the given SQL statement from the original MS-Access dialect to the dialect of the target database.
     * Table and column names may also be replaced.
     *
     * @param  sql The statement in MS-Access dialect.
     * @return The SQL statement adapted to the dialect of the target database.
     */
    public String apply(final String sql) {
        final String catalog = nonEmpty(this.catalog);
        final String schema  = nonEmpty(this.schema);
        if (quote.isEmpty() && accessToAnsi.isEmpty() && schema == null && catalog == null) {
            return sql;
        }
        final StringBuilder ansi = new StringBuilder(sql.length() + 16);
        int start, end = 0;
        while ((start = sql.indexOf('[', end)) >= 0) {
            /*
             * Append every characters since the end of the last processed table/column name,
             * or since the beginning of the SQL statement if we are in the first iteration.
             * Then find the end of the new table/column name to process in this iteration.
             */
            ansi.append(sql, end, start);
            if ((end = sql.indexOf(']', ++start)) < 0) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.MissingCharacterInElement_2, sql.substring(start), ']'));
            }
            /*
             * The name can be a table name or a column name, but only table names will be quoted.
             * EPSG seems to write all column names in upper-case (MS-Access) or lower-case (ANSI),
             * so we will let the database driver selects the case of its choice for column names.
             */
            final String name = sql.substring(start, end++);
            if (CharSequences.isUpperCase(name)) {
                ansi.append(JDK8.getOrDefault(accessToAnsi, name, name));
            } else {
                if (catalog != null) {
                    ansi.append(quote).append(catalog).append(quote).append('.');
                }
                if (schema != null) {
                    ansi.append(quote).append(schema).append(quote).append('.');
                }
                if (quoteTableNames) {
                    ansi.append(quote);
                }
                if (schema == null) {
                    ansi.append(TABLE_PREFIX);
                }
                if (quoteTableNames) {
                    ansi.append(JDK8.getOrDefault(accessToAnsi, name, name)).append(quote);
                } else {
                    for (final String word : name.split("\\s")) {
                        ansi.append(JDK8.getOrDefault(accessToAnsi, word, word));
                    }
                }
            }
        }
        ansi.append(sql, end, sql.length());
        /*
         * If the database use the BOOLEAN type instead of SMALLINT, replaces "deprecated=0' by "deprecated=false".
         */
        if (useBoolean) {
            int w = ansi.indexOf("WHERE");
            if (w >= 0) {
                w += 5;
                for (final String field : BOOLEAN_FIELDS) {
                    int p = ansi.indexOf(field, w);
                    if (p >= 0) {
                        p += field.length();
                        if (!replaceIfEquals(ansi, p, "=0", "=FALSE") &&
                            !replaceIfEquals(ansi, p, "<>0", "=TRUE"))
                        {
                            // Remove "ABS" in "ABS(DEPRECATED)" or "ABS(CO.DEPRECATED)".
                            if ((p = ansi.lastIndexOf("(", p)) > w) {
                                replaceIfEquals(ansi, p-3, "ABS", "");
                            }
                        }
                    }
                }
            }
        }
        return ansi.toString();
    }

    /**
     * Replaces the text at the given position in the buffer if it is equals to the {@code expected} text.
     */
    private static boolean replaceIfEquals(final StringBuilder ansi, final int pos,
            final String expected, final String replacement)
    {
        if (CharSequences.regionMatches(ansi, pos, expected)) {
            ansi.replace(pos, pos + expected.length(), replacement);
            return true;
        }
        return false;
    }
}
