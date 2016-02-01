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
import org.apache.sis.util.Localized;
import org.apache.sis.util.ObjectConverters;

// Branch-dependent imports
import java.nio.file.Path;


/**
 * A geodetic object factory backed by the EPSG database. This class creates JDBC connections to the EPSG database
 * when first needed using the {@link DataSource} specified at construction time. The geodetic objects are cached
 * for reuse and the idle connections are closed after a timeout.
 *
 * <div class="section">Note for subclasses</div>
 * If there is no cached object for a given code, then {@code EPSGFactory} creates an {@link EPSGDataAccess} instance
 * for performing the actual creation work. Developers who need to customize the geodetic object creation can override
 * the {@link #newDataAccess(Connection, SQLTranslator)} method in order to return their own {@link EPSGDataAccess}
 * subclass.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
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
     * The translator from the SQL statements using MS-Access dialect to SQL statements using the dialect
     * of the actual database. If null, will be created when first needed.
     */
    private volatile SQLTranslator translator;

    /**
     * The locale for producing error messages. This is usually the default locale.
     *
     * @see #getLocale()
     */
    private volatile Locale locale;

    /**
     * Creates a factory using the default data source and object factories.
     * Invoking this constructor is equivalent to invoking the constructor below with a all arguments set to null.
     *
     * @throws FactoryException if the data source can not be obtained.
     */
    public EPSGFactory() throws FactoryException {
        this(null, null, null, null, null, null, null);
    }

    /**
     * Creates a factory using the given data source and object factories.
     *
     * <div class="section">Default argument values</div>
     * Any or all arguments given to this constructor can be {@code null}, in which case default values are used.
     * Those default values are implementation-specific and may change in any future SIS version.
     *
     * @param dataSource    The factory to use for creating {@link Connection}s to the EPSG database.
     * @param nameFactory   The factory to use for creating {@link org.opengis.util.GenericName} instances.
     * @param datumFactory  The factory to use for creating {@link Datum} instances.
     * @param csFactory     The factory to use for creating {@link CoordinateSystem} instances.
     * @param crsFactory    The factory to use for creating {@link CoordinateReferenceSystem} instances.
     * @param copFactory    The factory to use for creating {@link CoordinateOperation} instances.
     * @param mtFactory     The factory to use for creating {@link MathTransform} instances.
     * @throws FactoryException if an error occurred while creating the EPSG factory.
     */
    public EPSGFactory(final DataSource                 dataSource,
                       final NameFactory                nameFactory,
                       final DatumFactory               datumFactory,
                       final CSFactory                  csFactory,
                       final CRSFactory                 crsFactory,
                       final CoordinateOperationFactory copFactory,
                       final MathTransformFactory       mtFactory)
            throws FactoryException
    {
        super(EPSGDataAccess.class);
        if (dataSource != null) {
            this.dataSource = dataSource;
        } else try {
            this.dataSource = Initializer.getDataSource();
            if (this.dataSource == null) {
                throw new UnavailableFactoryException(Initializer.unspecified(null));
            }
        } catch (Exception e) {
            throw new UnavailableFactoryException(e.getLocalizedMessage(), e);
        }
        this.nameFactory  = factory(NameFactory.class, nameFactory);
        this.datumFactory = factory(DatumFactory.class, datumFactory);
        this.csFactory    = factory(CSFactory.class, csFactory);
        this.crsFactory   = factory(CRSFactory.class, crsFactory);
        this.copFactory   = factory(CoordinateOperationFactory.class, copFactory);
        this.mtFactory    = factory(MathTransformFactory.class, mtFactory);
        this.locale       = Locale.getDefault(Locale.Category.DISPLAY);
        super.setTimeout(10, TimeUnit.SECONDS);
    }

    /**
     * Returns the given factory if non-null, or the default factory instance otherwise.
     */
    private static <F> F factory(final Class<F> type, F factory) {
        if (factory == null) {
            factory = DefaultFactories.forBuildin(type);
        }
        return factory;
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
     * Sets the locale to use for producing error messages.
     * The given locale will be honored on a <cite>best effort</cite> basis.
     * It does not change the way data are read from the EPSG database.
     *
     * @param locale The new locale to use for error message.
     */
    public void setLocale(final Locale locale) {
        ArgumentChecks.ensureNonNull("locale", locale);
        this.locale = locale;
    }

    /**
     * Creates the EPSG schema in the database and populates the tables with geodetic definitions.
     * This method is invoked automatically when {@link #newDataAccess()} detects that the EPSG dataset is not installed.
     * Users can also invoke this method explicitely if they wish to install the dataset with custom properties.
     *
     * <p>The {@code properties} map is optional.
     * If non-null, the following properties are recognized (all other properties are ignored):</p>
     *
     * <ul class="verbose">
     *   <li><b>{@code schema}:</b>
     *     a {@link String} giving the name of the database schema where to create the tables.
     *     That schema shall not exist prior this method call as it will be created by this {@code install(…)} method.
     *     If no schema is specified or if the schema is null, then the tables will be created without schema.
     *     If the database does not {@linkplain DatabaseMetaData#supportsSchemasInTableDefinitions() support
     *     schema in table definitions} or in {@linkplain DatabaseMetaData#supportsSchemasInDataManipulation()
     *     data manipulation}, then this property is ignored.</li>
     *
     *   <li><b>{@code scriptDirectory}:</b>
     *     a {@link java.nio.file.Path}, {@link java.io.File} or {@link java.net.URL} to a directory containing
     *     the SQL scripts to execute. If non-null, that directory shall contain at least files matching the
     *     {@code *Tables*.sql}, {@code *Data*.sql} and {@code *FKeys*.sql} patterns (those files are provided by EPSG).
     *     Files matching the {@code *Patches*.sql}, {@code *Indexes*.sql} and {@code *Grant*.sql} patterns
     *     (provided by Apache SIS) are optional but recommended.
     *     If no directory is specified, then this method will search for resources provided by the
     *     {@code geotk-epsg.jar} bundle.</li>
     * </ul>
     *
     * <p><b>Legal constraint:</b>
     * the EPSG dataset can not be distributed with Apache SIS at this time for licensing reasons.
     * Users need to either install the dataset manually (for example with the help of this method),
     * or add on the classpath a non-Apache bundle like {@code geotk-epsg.jar}.
     * See <a href="https://issues.apache.org/jira/browse/LEGAL-183">LEGAL-183</a> for more information.</p>
     *
     * @param  connection Connection to the database where to create the EPSG schema.
     * @param  properties Properties controlling the schema name and location of SQL scripts, or {@code null} if none.
     * @throws IOException if the SQL script can not be found or an I/O error occurred while reading them.
     * @throws SQLException if an error occurred while writing to the database.
     */
    public synchronized void install(final Connection connection, Map<?,?> properties) throws IOException, SQLException {
        ArgumentChecks.ensureNonNull("connection", connection);
        if (properties == null) {
            properties = Collections.emptyMap();
        }
        final String schema = ObjectConverters.convert(properties.get("schema"), String.class);
        final Path scriptDirectory = ObjectConverters.convert(properties.get("scriptDirectory"), Path.class);
        try (EPSGInstaller installer = new EPSGInstaller(connection)) {
            final boolean ac = connection.getAutoCommit();
            if (ac) {
                connection.setAutoCommit(false);
            }
            boolean success = false;
            try {
                if (schema != null) {
                    installer.setSchema(schema);
                }
                installer.run(scriptDirectory);
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
                if (!success) {
                    installer.logFailure(locale);
                }
            }
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
     *       If the tables are not found, invokes {@link #install(Connection, Map)}.</li>
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
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            SQLTranslator tr = translator;
            if (tr == null) {
                synchronized (this) {
                    tr = translator;
                    if (tr == null) {
                        tr = new SQLTranslator(connection.getMetaData());
                        try {
                            if (!tr.isSchemaFound()) {
                                install(connection, Collections.singletonMap("schema", Constants.EPSG));
                                tr.setSchemaFound(connection.getMetaData());   // Set only on success.
                            }
                        } finally {
                            translator = tr;        // Set only after installation in order to block other threads.
                        }
                    }
                }
            }
            if (tr.isSchemaFound()) {
                return newDataAccess(connection, tr);
            }
            connection.close();
        } catch (Exception e) {                     // Really want to catch all exceptions here.
            if (connection != null) try {
                connection.close();
            } catch (SQLException e2) {
                e.addSuppressed(e2);
            }
            throw new UnavailableFactoryException(e.getLocalizedMessage(), e);
        }
        throw new UnavailableFactoryException(SQLTranslator.schemaNotFound(locale));
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
