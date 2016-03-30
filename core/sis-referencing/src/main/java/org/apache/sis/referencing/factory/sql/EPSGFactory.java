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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.io.IOException;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.factory.ConcurrentAuthorityFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Localized;
import org.apache.sis.util.resources.Errors;


/**
 * A geodetic object factory backed by the EPSG database. This class creates JDBC connections to the EPSG database
 * when first needed using the {@link DataSource} specified at construction time. The geodetic objects are cached
 * for reuse and the idle connections are closed after a timeout.
 *
 * <p>If no data source has been specified to the constructor, then {@code EPSGFactory} searches for a
 * default data source in JNDI, or in the directory given by the {@code SIS_DATA} environment variable,
 * or in the directory given by the {@code "derby​.system​.home"} property, in that order.
 * See the {@linkplain org.apache.sis.referencing.factory.sql package documentation} for more information.</p>
 *
 * <div class="section">EPSG dataset installation</div>
 * This class tries to automatically detect the schema that contains the EPSG tables
 * (see {@link SQLTranslator} for examples of tables to look for). If the tables are not found,
 * then the {@link #install(Connection)} method will be invoked for creating the EPSG schema.
 * The {@code install(…)} method can perform its work only if the definition files are reachable
 * on the classpath, or if the directory containing the files have been specified.
 *
 * <div class="section">Data Access Object (DAO)</div>
 * If there is no cached object for a given code, then {@code EPSGFactory} creates an {@link EPSGDataAccess} instance
 * for performing the actual creation work. Developers who need to customize the geodetic object creation can override
 * the {@link #newDataAccess(Connection, SQLTranslator)} method in order to return their own {@link EPSGDataAccess}
 * subclass.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see EPSGDataAccess
 * @see SQLTranslator
 * @see <a href="http://sis.apache.org/book/tables/CoordinateReferenceSystems.html">List of authority codes</a>
 */
public class EPSGFactory extends ConcurrentAuthorityFactory<EPSGDataAccess> implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory, Localized
{
    /**
     * The namespace of EPSG codes.
     *
     * @see #getCodeSpaces()
     */
    private static final Set<String> CODESPACES = Collections.singleton(Constants.EPSG);

    /**
     * The factory to use for creating {@link Connection}s to the EPSG database.
     */
    protected final DataSource dataSource;

    /**
     * The factory to use for creating {@link org.opengis.util.GenericName} instances.
     */
    protected final NameFactory nameFactory;

    /**
     * The factory to use for creating {@link org.opengis.referencing.datum.Datum} instances
     * from the properties read in the database.
     */
    protected final DatumFactory datumFactory;

    /**
     * The factory to use for creating {@link org.opengis.referencing.cs.CoordinateSystem} instances
     * from the properties read in the database.
     */
    protected final CSFactory csFactory;

    /**
     * The factory to use for creating {@link org.opengis.referencing.crs.CoordinateReferenceSystem} instances
     * from the properties read in the database.
     */
    protected final CRSFactory crsFactory;

    /**
     * The factory to use for creating {@link CoordinateOperation} instances from the properties read in the database.
     */
    protected final CoordinateOperationFactory copFactory;

    /**
     * The factory to use for creating {@link org.opengis.referencing.operation.MathTransform} instances.
     * The math transforms are created as part of {@link org.opengis.referencing.operation.CoordinateOperation}
     * creation process.
     */
    protected final MathTransformFactory mtFactory;

    /**
     * The name of the catalog that contains the EPSG tables, or {@code null} or an empty string.
     * <ul>
     *   <li>The {@code ""} value retrieves the EPSG schema without a catalog.</li>
     *   <li>The {@code null} value means that the catalog name should not be used to narrow the search.</li>
     * </ul>
     */
    private final String catalog;

    /**
     * The name of the schema that contains the EPSG tables, or {@code null} or an empty string.
     * <ul>
     *   <li>The {@code ""} value retrieves the EPSG tables without a schema.
     *       In such case, table names are prefixed by {@value SQLTranslator#TABLE_PREFIX}.</li>
     *   <li>The {@code null} value means that the schema name should not be used to narrow the search.
     *       In such case, {@link SQLTranslator} will tries to automatically detect the schema.</li>
     * </ul>
     */
    private final String schema;

    /**
     * A provider of SQL scripts to use if {@code EPSGFactory} needs to create the database,
     * or {@code null} for the default mechanism.
     */
    private final InstallationScriptProvider scriptProvider;

    /**
     * The translator from the SQL statements using MS-Access dialect to SQL statements using the dialect
     * of the actual database. If null, will be created when first needed.
     */
    private volatile SQLTranslator translator;

    /**
     * The locale for producing error messages. This is usually the default locale.
     *
     * @see #getLocale()
     */
    private final Locale locale;

    /**
     * Creates a factory using the given configuration. The properties recognized by this constructor
     * are listed in the table below. Any property not listed below will be ignored by this constructor.
     * All properties are optional and can {@code null} or omitted, in which case default values are used.
     * Those default values are implementation-specific and may change in any future SIS version.
     *
     * <table class="sis">
     *  <caption>Recognized properties</caption>
     *  <tr>
     *   <th>Key</th>
     *   <th>Value class</th>
     *   <th>Description</th>
     *  </tr><tr>
     *   <td>{@code dataSource}</td>
     *   <td>{@link DataSource}</td>
     *   <td>The factory to use for creating {@link Connection}s to the EPSG database.</td>
     *  </tr><tr>
     *   <td>{@code nameFactory}</td>
     *   <td>{@link NameFactory}</td>
     *   <td>The factory to use for creating {@link org.opengis.util.GenericName} instances.</td>
     *  </tr><tr>
     *   <td>{@code datumFactory}</td>
     *   <td>{@link DatumAuthorityFactory}</td>
     *   <td>The factory to use for creating {@link org.opengis.referencing.datum.Datum} instances.</td>
     *  </tr><tr>
     *   <td>{@code csFactory}</td>
     *   <td>{@link CSAuthorityFactory}</td>
     *   <td>The factory to use for creating {@link org.opengis.referencing.cs.CoordinateSystem} instances.</td>
     *  </tr><tr>
     *   <td>{@code crsFactory}</td>
     *   <td>{@link CRSAuthorityFactory}</td>
     *   <td>The factory to use for creating {@link org.opengis.referencing.crs.CoordinateReferenceSystem} instances.</td>
     *  </tr><tr>
     *   <td>{@code copFactory}</td>
     *   <td>{@link CoordinateOperationAuthorityFactory}</td>
     *   <td>The factory to use for creating {@link org.opengis.referencing.operation.CoordinateOperation} instances.</td>
     *  </tr><tr>
     *   <td>{@code mtFactory}</td>
     *   <td>{@link MathTransformFactory}</td>
     *   <td>The factory to use for creating {@link org.opengis.referencing.operation.MathTransform} instances.</td>
     *  </tr><tr>
     *   <td>{@code catalog}</td>
     *   <td>{@link String}</td>
     *   <td>The database catalog that contains the EPSG schema (see {@linkplain #install install}).</td>
     *  </tr><tr>
     *   <td>{@code schema}</td>
     *   <td>{@link String}</td>
     *   <td>The database schema that contains the EPSG tables (see {@linkplain #install install}).</td>
     *  </tr><tr>
     *   <td>{@code scriptProvider}</td>
     *   <td>{@link InstallationScriptProvider}</td>
     *   <td>A provider of SQL scripts to use if {@code EPSGFactory} needs to create the database.</td>
     *  </tr><tr>
     *   <td>{@code locale}</td>
     *   <td>{@link Locale}</td>
     *   <td>The locale for producing error messages on a <cite>best effort</cite> basis.</td>
     *  </tr>
     * </table>
     *
     * <p>Default values</p>
     * <ul>
     *   <li>If no {@code dataSource} is specified, this constructor defaults to the search algorithm described
     *       in the {@linkplain org.apache.sis.referencing.factory.sql package documentation}.</li>
     *   <li>If no {@code catalog} or {@code schema} is specified, {@link SQLTranslator} will try to auto-detect
     *       the schema that contains the EPSG tables.</li>
     *   <li>If no {@code locale} is specified, this constructor defaults to the
     *       {@linkplain Locale#getDefault(Locale.Category) display locale}.</li>
     * </ul>
     *
     * @param  properties The data source, authority factories and other configuration properties,
     *                    or {@code null} for the default values.
     * @throws ClassCastException if a property value is not of the expected class.
     * @throws IllegalArgumentException if a property value is invalid.
     * @throws FactoryException if an error occurred while creating the EPSG factory.
     */
    public EPSGFactory(Map<String,?> properties) throws FactoryException {
        super(EPSGDataAccess.class);
        if (properties == null) {
            properties = Collections.emptyMap();
        }
        DataSource ds  = (DataSource)                 properties.get("dataSource");
        Locale locale  = (Locale)                     properties.get("locale");
        schema         = (String)                     properties.get("schema");
        catalog        = (String)                     properties.get("catalog");
        scriptProvider = (InstallationScriptProvider) properties.get("scriptProvider");
        if (locale == null) {
            locale = Locale.getDefault();
        }
        this.locale = locale;
        if (ds == null) try {
            ds = Initializer.getDataSource();
            if (ds == null) {
                throw new UnavailableFactoryException(Initializer.unspecified(locale));
            }
        } catch (Exception e) {
            throw new UnavailableFactoryException(message(e), e);
        }
        dataSource   = ds;
        nameFactory  = factory(NameFactory.class,                "nameFactory",  properties);
        datumFactory = factory(DatumFactory.class,               "datumFactory", properties);
        csFactory    = factory(CSFactory.class,                  "csFactory",    properties);
        crsFactory   = factory(CRSFactory.class,                 "crsFactory",   properties);
        copFactory   = factory(CoordinateOperationFactory.class, "copFactory",   properties);
        mtFactory    = factory(MathTransformFactory.class,       "mtFactory",    properties);
        super.setTimeout(10, TimeUnit.SECONDS);
    }

    /**
     * Returns the factory for the given key if it exists, or the default factory instance otherwise.
     */
    private static <F> F factory(final Class<F> type, final String key, final Map<String,?> properties) {
        final F factory = type.cast(properties.get(key));
        return (factory != null) ? factory : DefaultFactories.forBuildin(type);
    }

    /**
     * Returns the message to put in an {@link UnavailableFactoryException} having the given exception as its cause.
     */
    private String message(final Exception e) {
        String message = Exceptions.getLocalizedMessage(e, locale);
        if (message == null) {
            message = Classes.getShortClassName(e);
        }
        return Errors.getResources(locale).getString(Errors.Keys.CanNotUseGeodeticParameters_2, Constants.EPSG, message);
    }

    /**
     * Returns the namespace of EPSG codes.
     *
     * @return The {@code "EPSG"} string in a singleton map.
     */
    @Override
    public Set<String> getCodeSpaces() {
        return CODESPACES;
    }

    /**
     * Returns the locale used by this factory for producing error messages.
     * This locale does not change the way data are read from the EPSG database.
     *
     * @return The locale for error messages.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Creates the EPSG schema in the database and populates the tables with geodetic definitions.
     * This method is invoked automatically when {@link #newDataAccess()} detects that the EPSG dataset is not installed.
     * Users can also invoke this method explicitely if they wish to force the dataset installation.
     *
     * <p>This method uses the following properties from the map specified at
     * {@linkplain #EPSGFactory(Map) construction time}:</p>
     *
     * <ul class="verbose">
     *   <li><b>{@code catalog}:</b><br>
     *     a {@link String} giving the name of the database catalog where to create the EPSG schema.
     *     If non-null, that catalog shall exist prior this method call (this method does not create any catalog).
     *     If no catalog is specified or if the catalog is an empty string,
     *     then the EPSG schema will be created without catalog.
     *     If the database does not {@linkplain DatabaseMetaData#supportsCatalogsInTableDefinitions() support
     *     catalogs in table definitions} or in {@linkplain DatabaseMetaData#supportsCatalogsInDataManipulation()
     *     data manipulation}, then this property is ignored.</li>
     *
     *   <li><b>{@code schema}:</b><br>
     *     a {@link String} giving the name of the database schema where to create the EPSG tables.
     *     That schema shall <strong>not</strong> exist prior this method call;
     *     the schema will be created by this {@code install(…)} method.
     *     If the schema is an empty string, then the tables will be created without schema.
     *     If no schema is specified, then the default schema is {@code "EPSG"}.
     *     If the database does not {@linkplain DatabaseMetaData#supportsSchemasInTableDefinitions() support
     *     schemas in table definitions} or in {@linkplain DatabaseMetaData#supportsSchemasInDataManipulation()
     *     data manipulation}, then this property is ignored.</li>
     *
     *   <li><b>{@code scriptProvider}:</b><br>
     *     an {@link InstallationScriptProvider} giving the SQL scripts to execute for creating the EPSG database.
     *     If no provider is specified, then this method will search on the classpath (with {@link java.util.ServiceLoader})
     *     for user-provided implementations of {@code InstallationScriptProvider}.
     *     If no user-specified provider is found, then this method will search for
     *     {@code "EPSG_*Tables.sql"}, {@code "EPSG_*Data.sql"} and {@code "EPSG_*FKeys.sql"} files in the
     *     {@code $SIS_DATA/Databases/ExternalSources} directory where {@code *} stands for any characters
     *     provided that there is no ambiguity.</li>
     * </ul>
     *
     * <p><b>Legal constraint:</b>
     * the EPSG dataset can not be distributed with Apache SIS at this time for licensing reasons.
     * Users need to either install the dataset manually (for example with the help of this method),
     * or add on the classpath a non-Apache bundle like {@code geotk-epsg.jar}.
     * See <a href="https://issues.apache.org/jira/browse/LEGAL-183">LEGAL-183</a> for more information.</p>
     *
     * @param  connection Connection to the database where to create the EPSG schema.
     * @throws IOException if the SQL script can not be found or an I/O error occurred while reading them.
     * @throws SQLException if an error occurred while writing to the database.
     *
     * @see InstallationScriptProvider
     */
    public synchronized void install(final Connection connection) throws IOException, SQLException {
        ArgumentChecks.ensureNonNull("connection", connection);
        final EPSGInstaller installer = new EPSGInstaller(connection);
        try {
            final boolean ac = connection.getAutoCommit();
            if (ac) {
                connection.setAutoCommit(false);
            }
            try {
                boolean success = false;
                try {
                    if (!"".equals(schema)) {                                           // Schema may be null.
                        installer.setSchema(schema != null ? schema : Constants.EPSG);
                        if (catalog != null && !catalog.isEmpty()) {
                            installer.prependNamespace(catalog);
                        }
                    }
                    installer.run(scriptProvider, locale);
                    success = true;
                } finally {
                    if (ac) {
                        if (success) {
                            connection.commit();
                        } else {
                            connection.rollback();
                        }
                        connection.setAutoCommit(true);
                    }
                }
            } catch (IOException e) {
                installer.logFailure(locale, e);
                throw e;
            } catch (SQLException e) {
                installer.logFailure(locale, e);
                throw e;
            }
        } finally {
            installer.close();
        }
    }

    /**
     * Creates the factory which will perform the actual geodetic object creation work.
     * This method is invoked automatically when a new worker is required, either because the previous
     * one has been disposed after its timeout or because a new one is required for concurrency.
     *
     * <p>The default implementation performs the following steps:</p>
     * <ol>
     *   <li>Gets a new connection from the {@link #dataSource}.</li>
     *   <li>If this method is invoked for the first time, verifies if the EPSG tables exists.
     *       If the tables are not found, invokes {@link #install(Connection)}.</li>
     *   <li>Delegates to {@link #newDataAccess(Connection, SQLTranslator)}, which provides an easier
     *       overriding point for subclasses wanting to return a custom {@link EPSGDataAccess} instance.</li>
     * </ol>
     *
     * @return Data Access Object (DAO) to use in {@code createFoo(String)} methods.
     * @throws FactoryException if the constructor failed to connect to the EPSG database.
     *         This exception usually has a {@link SQLException} as its cause.
     */
    @Override
    protected EPSGDataAccess newDataAccess() throws FactoryException {
        UnavailableFactoryException exception;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            SQLTranslator tr = translator;
            if (tr == null) {
                synchronized (this) {
                    tr = translator;
                    if (tr == null) {
                        tr = new SQLTranslator(connection.getMetaData(), catalog, schema);
                        try {
                            if (!tr.isTableFound()) {
                                install(connection);
                                tr.setup(connection.getMetaData());         // Set only on success.
                            }
                        } finally {
                            translator = tr;        // Set only after installation in order to block other threads.
                        }
                    }
                }
            }
            if (tr.isTableFound()) {
                return newDataAccess(connection, tr);
            } else {
                connection.close();
                exception = new UnavailableFactoryException(SQLTranslator.tableNotFound(locale));
            }
        } catch (Exception e) {                     // Really want to catch all exceptions here.
            if (connection != null) try {
                connection.close();
            } catch (SQLException e2) {
                // e.addSuppressed(e2) on the JDK7 branch.
            }
            exception = new UnavailableFactoryException(message(e), e);
        }
        exception.setUnavailableFactory(this);
        throw exception;
    }

    /**
     * Creates the factory which will perform the actual geodetic object creation from a given connection.
     * This method is a convenience hook easier to override than {@link #newDataAccess()} for subclasses
     * wanting to return instances of their own {@link EPSGDataAccess} subclass.
     * The default implementation is simply:
     *
     * {@preformat java
     *     return new EPSGDataAccess(this, connection, translator);
     * }
     *
     * Subclasses can override this method with a similar code but with {@code new EPSGDataAccess(…)} replaced
     * by {@code new MyDataAccessSubclass(…)}.
     *
     * @param  connection A connection to the EPSG database.
     * @param  translator The translator from the SQL statements using MS-Access dialect to SQL statements
     *                    using the dialect of the actual database.
     * @return Data Access Object (DAO) to use in {@code createFoo(String)} methods.
     * @throws SQLException if a problem with the database has been detected.
     *
     * @see EPSGDataAccess#EPSGDataAccess(EPSGFactory, Connection, SQLTranslator)
     */
    protected EPSGDataAccess newDataAccess(Connection connection, SQLTranslator translator) throws SQLException {
        return new EPSGDataAccess(this, connection, translator);
    }

    /**
     * Returns {@code true} if the given Data Access Object (DAO) can be closed. This method is invoked automatically
     * after the {@linkplain #getTimeout timeout} if the given DAO has been idle during all that time. The default
     * implementation always returns {@code false} if a set returned by {@link EPSGDataAccess#getAuthorityCodes(Class)}
     * is still in use.
     *
     * @param factory The Data Access Object which is about to be closed.
     * @return {@code true} if the given Data Access Object can be closed.
     */
    @Override
    protected boolean canClose(final EPSGDataAccess factory) {
        return factory.canClose();
    }
}
