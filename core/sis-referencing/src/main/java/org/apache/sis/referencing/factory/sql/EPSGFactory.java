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
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
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
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.ConcurrentAuthorityFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Localized;


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
public class EPSGFactory extends ConcurrentAuthorityFactory implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory, Localized
{
    /**
     * The factory to use for creating {@link Connection}s to the EPSG database.
     */
    protected final DataSource dataSource;

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
        this(null, null, null, null, null, null, null, null);
    }

    /**
     * Creates a factory using the given data source and object factories.
     *
     * <div class="section">Default argument values</div>
     * Any or all arguments given to this constructor can be {@code null}, in which case default values are used.
     * Those default values are implementation-specific and may change in any future SIS version.
     *
     * @param dataSource    The factory to use for creating {@link Connection}s to the EPSG database.
     * @param nameFactory   The factory to use for creating authority codes as {@link GenericName} instances.
     * @param datumFactory  The factory to use for creating {@link Datum} instances.
     * @param csFactory     The factory to use for creating {@link CoordinateSystem} instances.
     * @param crsFactory    The factory to use for creating {@link CoordinateReferenceSystem} instances.
     * @param copFactory    The factory to use for creating {@link CoordinateOperation} instances.
     * @param mtFactory     The factory to use for creating {@link MathTransform} instances.
     * @param translator    The translator from the SQL statements using MS-Access dialect to SQL statements
     *                      using the dialect of the actual database.
     * @throws FactoryException if an error occurred while creating the EPSG factory.
     */
    public EPSGFactory(final DataSource                 dataSource,
                       final NameFactory                nameFactory,
                       final DatumFactory               datumFactory,
                       final CSFactory                  csFactory,
                       final CRSFactory                 crsFactory,
                       final CoordinateOperationFactory copFactory,
                       final MathTransformFactory       mtFactory,
                       final SQLTranslator              translator)
            throws FactoryException
    {
        super(EPSGDataAccess.class, factory(NameFactory.class, nameFactory));
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
        this.datumFactory = factory(DatumFactory.class, datumFactory);
        this.csFactory    = factory(CSFactory.class, csFactory);
        this.crsFactory   = factory(CRSFactory.class, crsFactory);
        this.copFactory   = factory(CoordinateOperationFactory.class, copFactory);
        this.mtFactory    = factory(MathTransformFactory.class, mtFactory);
        this.translator   = translator;
        this.locale       = Locale.getDefault(Locale.Category.DISPLAY);
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
     * Creates the factory which will perform the actual geodetic object creation work.
     * This method is invoked automatically when a new worker is required, either because the previous
     * one has been disposed after its timeout or because a new one is required for concurrency.
     *
     * <p>The default implementation gets a new connection from the {@link #dataSource} and delegates to
     * {@link #newDataAccess(Connection, SQLTranslator)}, which provides an easier overriding point
     * for subclasses wanting to return a custom {@link EPSGDataAccess} instance.</p>
     *
     * @return Data Access Object (DAO) to use in {@code createFoo(String)} methods.
     * @throws FactoryException if the constructor failed to connect to the EPSG database.
     *         This exception usually has a {@link SQLException} as its cause.
     */
    @Override
    protected GeodeticAuthorityFactory newDataAccess() throws FactoryException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            SQLTranslator tr = translator;
            if (tr == null) {
                synchronized (this) {
                    tr = translator;
                    if (tr == null) {
                        translator = tr = new SQLTranslator(connection.getMetaData());
                    }
                }
            }
            return newDataAccess(connection, tr);
        } catch (Exception e) {
            if (connection != null) try {
                connection.close();
            } catch (SQLException e2) {
                e.addSuppressed(e2);
            }
            throw new UnavailableFactoryException(e.getLocalizedMessage(), e);
        }
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
}
