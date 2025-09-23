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

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.FileNotFoundException;
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
import org.apache.sis.metadata.sql.internal.shared.Initializer;
import org.apache.sis.referencing.internal.DeferredCoordinateOperation;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.ReferencingFactoryContainer;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Localized;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.referencing.factory.ConcurrentAuthorityFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.util.resources.Messages;


/**
 * A geodetic object factory backed by the EPSG database. This class creates JDBC connections to the EPSG database
 * when first needed using the {@link DataSource} specified at construction time. The geodetic objects are cached
 * for reuse and the idle connections are closed after a timeout.
 *
 * <p>If no data source has been specified to the constructor, then {@code EPSGFactory} searches for a
 * default data source in JNDI, or in the directory given by the {@code SIS_DATA} environment variable,
 * or in the directory given by the {@code "derby.system.home"} property, in that order.
 * See the {@linkplain org.apache.sis.referencing.factory.sql package documentation} for more information.</p>
 *
 * <h2>EPSG dataset installation</h2>
 * This class tries to automatically detect the schema that contains the EPSG tables
 * (see {@link SQLTranslator} for examples of tables to look for). If the tables are not found,
 * then the {@link #install(Connection)} method will be invoked for creating the EPSG schema.
 * The {@code install(…)} method can perform its work only if the definition files are reachable
 * on the module path, or if the directory containing the files have been specified.
 *
 * <h2>Data Access Object (DAO)</h2>
 * If there is no cached object for a given code, then {@code EPSGFactory} creates an {@link EPSGDataAccess} instance
 * for performing the actual creation work. Developers who need to customize the geodetic object creation can override
 * the {@link #newDataAccess(Connection, SQLTranslator)} method in order to return their own {@link EPSGDataAccess}
 * subclass.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see EPSGDataAccess
 * @see SQLTranslator
 * @see <a href="https://sis.apache.org/tables/CoordinateReferenceSystems.html">List of authority codes</a>
 *
 * @since 0.7
 */
public class EPSGFactory extends ConcurrentAuthorityFactory<EPSGDataAccess> implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory, Localized
{
    /**
     * The namespace of EPSG codes.
     *
     * @see #getCodeSpaces()
     */
    private static final Set<String> CODESPACES = Set.of(Constants.EPSG);

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
     * The factory to use for creating {@link org.opengis.referencing.operation.CoordinateOperation} instances
     * from the properties read in the database.
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
     *   <li>The {@code ""} value retrieves the EPSG tables without a schema.</li>
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
     * The translator from the <abbr>SQL</abbr> statements hard-coded in {@link EPSGDataAccess}
     * to <abbr>SQL</abbr> statements compatible with the actual <abbr>EPSG</abbr> database.
     * This translator may also change the schema and table names used in the queries in the
     * actual database uses different names.
     *
     * <p>If {@code null}, a default translator will be created when first needed.</p>
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
     *   <td>The locale for producing error messages on a <em>best effort</em> basis.</td>
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
     * @param  properties  the data source, authority factories and other configuration properties,
     *                     or {@code null} for the default values.
     * @throws ClassCastException if a property value is not of the expected class.
     * @throws IllegalArgumentException if a property value is invalid.
     * @throws UnavailableFactoryException if an error occurred while creating the EPSG factory.
     */
    @SuppressWarnings("this-escape")    // The invoked method does not store `this` and is not overrideable.
    public EPSGFactory(Map<String,?> properties) throws UnavailableFactoryException {
        super(EPSGDataAccess.class);
        if (properties == null) {
            properties = Map.of();
        }
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        Locale locale  = (Locale)                     properties.get("locale");
        DataSource ds  = (DataSource)                 properties.get("dataSource");
        schema         = (String)                     properties.get("schema");
        catalog        = (String)                     properties.get("catalog");
        scriptProvider = (InstallationScriptProvider) properties.get("scriptProvider");
        if (locale == null) {
            locale = Locale.getDefault(Locale.Category.DISPLAY);
        }
        this.locale = locale;
        if (ds == null) try {
            ds = Initializer.getDataSource();
        } catch (Exception e) {
            throw new UnavailableFactoryException(canNotUse(e), e);
        }
        if (ds == null) {
            // Must be outside the above `try` block.
            throw new UnavailableFactoryException(String.valueOf(Initializer.unspecified(locale, false)));
        }
        final var c = new ReferencingFactoryContainer(properties);
        dataSource   = ds;
        nameFactory  = c.getNameFactory();
        datumFactory = c.getDatumFactory();
        csFactory    = c.getCSFactory();
        crsFactory   = c.getCRSFactory();
        copFactory   = c.getCoordinateOperationFactory();
        mtFactory    = c.getMathTransformFactory();
        super.setTimeout(10, TimeUnit.SECONDS);
    }

    /**
     * Returns the message to put in an {@link UnavailableFactoryException} having the given exception as its cause.
     */
    private String canNotUse(final Exception e) {
        String message = Exceptions.getLocalizedMessage(e, locale);
        if (message == null) {
            message = Classes.getShortClassName(e);
        }
        return Resources.forLocale(locale).getString(Resources.Keys.CanNotUseGeodeticParameters_2, Constants.EPSG, message);
    }

    /**
     * Returns the namespace of EPSG codes.
     *
     * @return the {@code "EPSG"} string in a singleton map.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<String> getCodeSpaces() {
        return CODESPACES;
    }

    /**
     * Returns the locale used by this factory for producing error messages.
     * This locale does not change the way data are read from the EPSG database.
     *
     * @return the locale for error messages.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Creates the EPSG schema in the database and populates the tables with geodetic definitions.
     * This method is invoked automatically when {@link #newDataAccess()} detects that the <abbr>EPSG</abbr> geodetic dataset is not installed.
     * Users can also invoke this method explicitly if they wish to force the dataset installation.
     *
     * <p>This method uses the following properties from the map specified at
     * {@linkplain #EPSGFactory(Map) construction time}:</p>
     *
     * <ul class="verbose">
     *   <li><b>{@code catalog}:</b><br>
     *     a {@link String} giving the name of the database catalog where to create the EPSG schema.
     *     If non-null, that catalog shall exist prior this method call (this method does not create any catalog).
     *     If no catalog is specified or if the catalog is an empty string,
     *     then the EPSG schema will be created without catalog. If the database does not
     *     {@linkplain DatabaseMetaData#supportsCatalogsInTableDefinitions() support catalogs in table definitions} or in
     *     {@linkplain DatabaseMetaData#supportsCatalogsInDataManipulation() data manipulation}, then this property is ignored.</li>
     *
     *   <li><b>{@code schema}:</b><br>
     *     a {@link String} giving the name of the database schema where to create the <abbr>EPSG</abbr> tables.
     *     That schema shall <strong>not</strong> exist prior this method call.
     *     The schema will be created by this {@code install(…)} method.
     *     If the schema is an empty string, then the tables will be created without schema.
     *     If no schema is specified, then the default schema is {@code "EPSG"}. If the database does not
     *     {@linkplain DatabaseMetaData#supportsSchemasInTableDefinitions() support schemas in table definitions} or in
     *     {@linkplain DatabaseMetaData#supportsSchemasInDataManipulation() data manipulation}, then this property is ignored.</li>
     *
     *   <li><b>{@code scriptProvider}:</b><br>
     *     an {@link InstallationScriptProvider} giving the <abbr>SQL</abbr> scripts to execute for creating the EPSG schema.
     *     If no provider is specified, then this method searches on the module path (with {@link java.util.ServiceLoader})
     *     for user-provided implementations of {@code InstallationScriptProvider}.</li>
     * </ul>
     *
     * <h4>Legal constraint</h4>
     * The <abbr>EPSG</abbr> dataset cannot be distributed with Apache SIS for licensing reasons.
     * Users need to either install the dataset manually (for example with the help of this method),
     * or add on the module path a separated bundle such as the {@code org.apache.sis.referencing.epsg} module.
     * See <a href="https://sis.apache.org/epsg.html">How to use EPSG geodetic dataset</a> for more information.
     *
     * @param  connection  connection to the database where to create the EPSG schema.
     * @throws UnavailableFactoryException if installation failed. The exception will have a
     *         {@link FileNotFoundException} cause if a SQL script has not been found
     *         (typically because a required resource is not on the module path), an
     *         {@link IOException} if an I/O error occurred while reading a SQL script, or a
     *         {@link SQLException} if an error occurred while writing to the database.
     *
     * @see InstallationScriptProvider
     */
    public synchronized void install(final Connection connection) throws UnavailableFactoryException {
        String    message = null;
        Exception failure = null;
        boolean   success = false;
        try {
            if (catalog != null) {
                connection.setCatalog(catalog);
            }
            final boolean autoCommit = connection.getAutoCommit();
            if (autoCommit) {
                connection.setAutoCommit(false);
            }
            try (EPSGInstaller installer = new EPSGInstaller(connection, schema)) {
                try {
                    success = installer.run(scriptProvider, locale);
                } catch (IOException | SQLException e) {
                    message = installer.failure(locale);
                    failure = e;
                }
            } finally {
                if (autoCommit) {
                    if (success) {
                        connection.commit();
                    } else {
                        connection.rollback();
                    }
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            if (failure != null) {
                failure.addSuppressed(e);
            } else {
                failure = e;
            }
            success = false;
        }
        if (!success) {
            if (message == null) {
                message = Messages.forLocale(locale).getString(
                        (failure != null) ? Messages.Keys.CanNotCreateSchema_1
                                          : Messages.Keys.NoDataSourceFound_1, Constants.EPSG);
            }
            /*
             * Derby sometimes wraps SQLException into another SQLException.  For making the stack strace a
             * little bit simpler, keep only the root cause provided that the exception type is compatible.
             */
            var exception = new UnavailableFactoryException(message, Exceptions.unwrap(failure));
            exception.setUnavailableFactory(this);
            throw exception;
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
            Initializer.connected(connection.getMetaData(), EPSGFactory.class, "newDataAccess");
            SQLTranslator tr = translator;
            if (tr == null) {
                synchronized (this) {
                    tr = translator;
                    if (tr == null) {
                        tr = new SQLTranslator(connection.getMetaData(), catalog, schema);
                        try {
                            if (!tr.isSchemaFound()) {
                                install(connection);
                                tr.setup(connection.getMetaData());         // Set only on success.
                            }
                        } finally {
                            translator = tr;        // Set only after installation in order to block other threads.
                        }
                    }
                }
            }
            if (tr.isSchemaFound()) {
                return newDataAccess(connection, tr);
            } else {
                String cause;
                try {
                    cause = SQLTranslator.tableNotFound(connection.getMetaData(), locale);
                } finally {
                    connection.close();
                }
                exception = new UnavailableFactoryException(cause);
            }
        } catch (Exception e) {                     // Really want to catch all exceptions here.
            if (connection != null) try {
                connection.close();
            } catch (SQLException e2) {
                e.addSuppressed(e2);
            }
            if (e instanceof FactoryException) {
                throw (FactoryException) e;
            }
            /*
             * Derby sometimes wraps SQLException into another SQLException.  For making the stack strace a
             * little bit simpler, keep only the root cause provided that the exception type is compatible.
             */
            exception = new UnavailableFactoryException(canNotUse(e), Exceptions.unwrap(e));
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
     * {@snippet lang="java" :
     *     return new EPSGDataAccess(this, connection, translator);
     *     }
     *
     * Subclasses can override this method with a similar code but with {@code new EPSGDataAccess(…)} replaced
     * by {@code new MyDataAccessSubclass(…)}.
     *
     * @param  connection  a connection to the EPSG database.
     * @param  translator  translator from the <abbr>SQL</abbr> statements hard-coded in {@link EPSGDataAccess}
     *                     to <abbr>SQL</abbr> statements compatible with the actual <abbr>EPSG</abbr> database.
     * @return Data Access Object (DAO) to use in {@code createFoo(String)} methods.
     * @throws SQLException if an error occurred with the database connection.
     *
     * @see EPSGDataAccess#EPSGDataAccess(EPSGFactory, Connection, SQLTranslator)
     */
    protected EPSGDataAccess newDataAccess(Connection connection, SQLTranslator translator) throws SQLException {
        return new EPSGDataAccess(this, connection, translator);
    }

    /**
     * Returns {@code true} if the given Data Access Object (DAO) can be closed. This method is invoked automatically
     * after the {@linkplain #getTimeout timeout} if the given <abbr>DAO</abbr> has been idle during all that time.
     * Returns {@code false} if a set returned by {@link EPSGDataAccess#getAuthorityCodes(Class)} is still in use.
     *
     * @param  factory  the Data Access Object which is about to be closed.
     * @return {@code true} if the given Data Access Object can be closed.
     */
    @Override
    protected boolean canClose(final EPSGDataAccess factory) {
        return factory.canClose();
    }

    /**
     * Returns whether the given object can be cached.
     * This method is invoked after {@link EPSGDataAccess} created a new object not previously in the cache.
     *
     * @hidden
     * @since 0.8
     */
    @Override
    protected boolean isCacheable(String code, Object object) {
        return !(object instanceof DeferredCoordinateOperation);
    }
}
