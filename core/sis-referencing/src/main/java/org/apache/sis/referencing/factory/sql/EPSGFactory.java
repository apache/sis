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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Localized;


/**
 * A geodetic object factory backed by the EPSG database. This class creates JDBC connections to the EPSG database
 * when first needed using the {@link DataSource} specified at construction time. The geodetic objects are cached
 * for reuse and the idle connections are closed after a timeout.
 *
 * <div class="section">Note for subclasses</div>
 * If there is no cached object for a given code, then {@code EPSGFactory} creates an {@link EPSGDataAccess} instance for
 * performing the actual creation work. Developers who need to customize the geodetic object creation can override the
 * {@link #createBackingStore(Connection)} method in order to return their own {@link EPSGDataAccess} subclass.
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
     * The locale for producing error messages. This is usually the default locale.
     *
     * @see #getLocale()
     */
    private volatile Locale locale;

    /**
     * Creates a factory using the default data source.
     *
     * @throws FactoryException if the data source can not be obtained.
     */
    public EPSGFactory() throws FactoryException {
        super(DefaultFactories.forBuildin(NameFactory.class));
        try {
            dataSource = Initializer.getDataSource();
        } catch (Exception e) {
            throw new FactoryException(e.getLocalizedMessage(), e);
        }
        datumFactory = DefaultFactories.forBuildin(DatumFactory.class);
        csFactory    = DefaultFactories.forBuildin(CSFactory.class);
        crsFactory   = DefaultFactories.forBuildin(CRSFactory.class);
        copFactory   = DefaultFactories.forBuildin(CoordinateOperationFactory.class);
        mtFactory    = DefaultFactories.forBuildin(MathTransformFactory.class);
        locale       = Locale.getDefault(Locale.Category.DISPLAY);
    }

    /**
     * Creates a factory using the given data source.
     *
     * @param dataSource    The factory to use for creating {@link Connection}s to the EPSG database.
     * @param nameFactory   The factory to use for creating authority codes as {@link GenericName} instances.
     * @param datumFactory  The factory to use for creating {@link Datum} instances.
     * @param csFactory     The factory to use for creating {@link CoordinateSystem} instances.
     * @param crsFactory    The factory to use for creating {@link CoordinateReferenceSystem} instances.
     * @param copFactory    The factory to use for creating {@link CoordinateOperation} instances.
     * @param mtFactory     The factory to use for creating {@link MathTransform} instances.
     */
    public EPSGFactory(final DataSource                 dataSource,
                       final NameFactory                nameFactory,
                       final DatumFactory               datumFactory,
                       final CSFactory                  csFactory,
                       final CRSFactory                 crsFactory,
                       final CoordinateOperationFactory copFactory,
                       final MathTransformFactory       mtFactory)
    {
        super(nameFactory);
        ArgumentChecks.ensureNonNull("dataSource",   dataSource);
        ArgumentChecks.ensureNonNull("datumFactory", datumFactory);
        ArgumentChecks.ensureNonNull("csFactory",    csFactory);
        ArgumentChecks.ensureNonNull("crsFactory",   crsFactory);
        ArgumentChecks.ensureNonNull("copFactory",   copFactory);
        ArgumentChecks.ensureNonNull("mtFactory",    mtFactory);
        this.dataSource   = dataSource;
        this.datumFactory = datumFactory;
        this.csFactory    = csFactory;
        this.crsFactory   = crsFactory;
        this.copFactory   = copFactory;
        this.mtFactory    = mtFactory;
        this.locale       = Locale.getDefault(Locale.Category.DISPLAY);
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
     * Invoked by {@code ConcurrentAuthorityFactory} when a new worker is required.
     * This method gets a new connection from the {@link #dataSource} and delegates
     * the worker creation to {@link #createBackingStore(Connection)}.
     *
     * @return The backing store to use in {@code createFoo(String)} methods.
     * @throws FactoryException if the constructor failed to connect to the EPSG database.
     *         This exception usually has a {@link SQLException} as its cause.
     */
    @Override
    protected final GeodeticAuthorityFactory createBackingStore() throws FactoryException {
        Connection c = null;
        try {
            c = dataSource.getConnection();
            final EPSGDataAccess factory = createBackingStore(c);
            factory.buffered = this;
            factory.setLocale(locale);
            return factory;
        } catch (Exception e) {
            if (c != null) try {
                c.close();
            } catch (SQLException e2) {
                e.addSuppressed(e2);
            }
            throw new FactoryException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Creates the factory which will perform the actual geodetic object creation work.
     * This method is invoked automatically when a new worker is required, either because the previous
     * one has been disposed after its timeout or because a new one is required for concurrency.
     *
     * <p>The default implementation creates a new {@link EPSGDataAccess} instance.
     * Subclasses can override this method if they want to return a custom instance.</p>
     *
     * @param  connection A connection to the EPSG database.
     * @throws SQLException if {@code EPSGDataAccess} detected a problem with the database.
     * @return The backing store to use in {@code createFoo(String)} methods.
     */
    protected EPSGDataAccess createBackingStore(final Connection connection) throws SQLException {
        return new EPSGDataAccess(connection, nameFactory, datumFactory, csFactory, crsFactory, copFactory, mtFactory);
    }
}
