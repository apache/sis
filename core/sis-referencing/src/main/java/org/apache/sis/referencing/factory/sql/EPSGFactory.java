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
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.net.URI;
import java.net.URISyntaxException;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.converter.ConversionException;

import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.internal.referencing.DeprecatedCode;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.ConcurrentAuthorityFactory;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Version;
import org.apache.sis.measure.Units;


/**
 * A geodetic object factory backed by the EPSG database tables.
 * The EPSG database is freely available at <a href="http://www.epsg.org">http://www.epsg.org</a>.
 * Current version of this class requires EPSG database version 6.6 or above.
 *
 * <p>EPSG codes are numerical identifiers. For example code 3395 stands for <cite>"WGS 84 / World Mercator"</cite>.
 * Coordinate Reference Objects are normally created from their numerical codes, but this factory accepts also names.
 * For example {@code createProjectedCRS("3395")} and {@code createProjectedCRS("WGS 84 / World Mercator")} both fetch
 * the same object.
 * However, names may be ambiguous since the same name may be used for more than one object.
 * This is the case of <cite>"WGS 84"</cite> for instance.
 * If such an ambiguity is found, an exception will be thrown.
 * This behavior can be changed by overriding the {@link #isPrimaryKey(String)} method.</p>
 *
 * <div class="section">Life cycle and caching</div>
 * {@code EPSGFactory} instances should be short-lived since they may hold a significant amount of JDBC resources.
 * It is recommended to have those instances created on the fly by {@link ConcurrentAuthorityFactory} and closed
 * after a relatively short {@linkplain ConcurrentAuthorityFactory#getTimeout timeout}.
 * In addition {@code ConcurrentAuthorityFactory} caches the most recently created objects, which reduce greatly
 * the amount of {@code EPSGFactory} instantiations (and consequently the amount of database accesses)
 * in the common case where only a few EPSG codes are used by an application.
 * {@code EPSGFactory.createFoo(String)} methods do not cache by themselves and query the database on every invocation.
 *
 * <div class="section">SQL dialects</div>
 * Because the primary distribution format for the EPSG database is MS-Access, this class uses
 * SQL statements formatted for the MS-Access syntax. For usage with an other database software,
 * a dialect-specific subclass must be used.
 *
 * @author  Yann Cézard (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @author  Matthias Basler
 * @author  Andrea Aime (TOPP)
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see <a href="http://sis.apache.org/book/tables/CoordinateReferenceSystems.html">List of authority codes</a>
 */
public abstract class EPSGFactory extends GeodeticAuthorityFactory implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory, Localized, AutoCloseable
{
    /**
     * The prefix in table names. The SQL scripts are provided by EPSG with this prefix in front of all table names.
     * SIS rather uses a modified version of those SQL scripts which creates the tables in an "EPSG" database schema.
     * But we still need to check for existence of this prefix in case someone used the original SQL scripts.
     */
    private static final String TABLE_PREFIX = "epsg_";

    // See org.apache.sis.measure.Units.valueOfEPSG(int) for hard-coded units from EPSG codes.
    // See TableInfo.EPSG for hard-coded table names, column names and GeoAPI types.

    // Hard-coded values for datum shift operation methods
    /** First Bursa-Wolf method. */ private static final int BURSA_WOLF_MIN_CODE = 9603;
    /** Last Bursa-Wolf method.  */ private static final int BURSA_WOLF_MAX_CODE = 9607;
    /** Rotation frame method.   */ private static final int ROTATION_FRAME_CODE = 9607;

    /**
     * Sets a Bursa-Wolf parameter from an EPSG parameter.
     *
     * @param  parameters The Bursa-Wolf parameters to modify.
     * @param  code       The EPSG code for a parameter from the [PARAMETER_CODE] column.
     * @param  value      The value of the parameter from the [PARAMETER_VALUE] column.
     * @param  unit       The unit of the parameter value from the [UOM_CODE] column.
     * @throws FactoryException if the code is unrecognized.
     */
    private void setBursaWolfParameter(final BursaWolfParameters parameters,
            final int code, double value, final Unit<?> unit) throws FactoryException
    {
        Unit<?> target = unit;
        if (code >= 8605) {
            if      (code <= 8607) target = SI   .METRE;
            else if (code <= 8710) target = NonSI.SECOND_ANGLE;
            else if (code == 8611) target = Units.PPM;
        }
        if (target != unit) try {
            value = unit.getConverterToAny(target).convert(value);
        } catch (ConversionException e) {
            throw new FactoryDataException(error().getString(Errors.Keys.IncompatibleUnit_1, unit), e);
        }
        switch (code) {
            case 8605: parameters.tX = value; break;
            case 8606: parameters.tY = value; break;
            case 8607: parameters.tZ = value; break;
            case 8608: parameters.rX = value; break;
            case 8609: parameters.rY = value; break;
            case 8610: parameters.rZ = value; break;
            case 8611: parameters.dS = value; break;
            default: throw new FactoryDataException(error().getString(Errors.Keys.UnexpectedParameter_1, code));
        }
    }

    /**
     * The name for the transformation accuracy metadata.
     */
    private static final InternationalString TRANSFORMATION_ACCURACY =
            Vocabulary.formatInternational(Vocabulary.Keys.TransformationAccuracy);

    /**
     * The namespace of EPSG names and codes. This namespace is needed by all {@code createFoo(String)} methods.
     * The {@code EPSGFactory} constructor relies on the {@link #nameFactory} caching mechanism for giving us
     * the same {@code NameSpace} instance than the one used by previous {@code EPSGFactory} instances, if any.
     */
    private final NameSpace namespace;

    /**
     * Last object type returned by {@link #createObject(String)}, or -1 if none.
     * This type is an index in the {@link TableInfo#EPSG} array and is strictly for {@link #createObject} internal use.
     */
    private int lastObjectType = -1;

    /**
     * The last table in which object name were looked for.
     * This is for internal use by {@link #toPrimaryKey} only.
     */
    private String lastTableForName;

    /**
     * The calendar instance for creating {@link Date} objects from a year (the "epoch" in datum definition).
     * We use the UTC timezone, which may not be quite accurate. But there is no obvious timezone for "epoch",
     * and the "epoch" is approximative anyway.
     *
     * @see #getCalendar()
     */
    private Calendar calendar;

    /**
     * The object to use for parsing dates, created when first needed. This is used for
     * parsing the origin of temporal datum. This is an Apache SIS specific extension.
     */
    private DateFormat dateFormat;

    /**
     * A pool of prepared statements. Keys are {@link String} objects related to their originating method
     * (for example "Ellipsoid" for {@link #createEllipsoid(String)}).
     */
    private final Map<String,PreparedStatement> statements = new HashMap<>();

    /**
     * The set of authority codes for different types. This map is used by the {@link #getAuthorityCodes(Class)}
     * method as a cache for returning the set created in a previous call. We do not want this map to exist for
     * a long time anyway.
     *
     * <p>Note that this {@code EPSGFactory} instance can not be closed as long as this map is not empty, since
     * {@link AuthorityCodes} caches some SQL statements and consequently require the {@linkplain #connection}
     * to be open. This is why we use weak references rather than hard ones, in order to know when no
     * {@link AuthorityCodes} are still in use.</p>
     *
     * <p>The {@link CloseableReference#dispose()} method takes care of closing the statements used by the map.
     * The {@link AuthorityCodes} reference in this map is then cleared by the garbage collector.
     * The {@link #canClose()} method checks if there is any remaining live reference in this map,
     * and returns {@code false} if some are found (thus blocking the call to {@link #close()}
     * by the {@link ConcurrentAuthorityFactory} timer).</p>
     */
    private final Map<Class<?>, CloseableReference<AuthorityCodes>> authorityCodes = new HashMap<>();

    /**
     * Cache for axis names. This service is not provided by {@link CachingAuthorityFactory}
     * since {@link AxisName} objects are particular to the EPSG database.
     *
     * @see #getAxisName(int)
     */
    private final Map<Integer,AxisName> axisNames = new HashMap<>();

    /**
     * Cache for the number of dimensions of coordinate systems. This service is not provided by
     * {@link CachingAuthorityFactory} since the number of dimension is used internally in this class.
     *
     * @see #getDimensionForCS(int)
     */
    private final Map<Integer,Integer> csDimensions = new HashMap<>();

    /**
     * Cache for whether conversions are projections. This service is not provided by {@link CachingAuthorityFactory}
     * since the check for conversion type is used internally in this class.
     *
     * @see #isProjection(int)
     */
    private final Map<Integer,Boolean> isProjection = new HashMap<>();

    /**
     * Cache the positional accuracies. Most coordinate operation use a small set of accuracy values.
     *
     * @see #getAccuracy(double)
     */
    private final Map<Double,PositionalAccuracy> accuracies = new HashMap<>();

    /**
     * Cache of naming systems other than EPSG. There is usually few of them (at most 15).
     * This is used for aliases.
     *
     * @see #createProperties(String, String, String, String, boolean)
     */
    private final Map<String,NameSpace> namingSystems = new HashMap<>();

    /**
     * The properties to be given the objects to construct.
     * Reused every time {@code createProperties(…)} is invoked.
     */
    private final Map<String,Object> properties = new HashMap<>();

    /**
     * A safety guard for preventing never-ending loops in recursive calls to {@link #createDatum(String)}.
     * This is used by {@link #createBursaWolfParameters(String, ResultSet)}, which need to create a target datum.
     * The target datum could have its own Bursa-Wolf parameters, with one of them pointing again to the source datum.
     */
    private final Set<Integer> safetyGuard = new HashSet<>();

    /**
     * The {@link ConcurrentAuthorityFactory} that supply caching for all {@code createFoo(String)} methods,
     * or {@code this} if none.
     */
    GeodeticAuthorityFactory buffered = this;

    /**
     * The connection to the EPSG database. This connection is specified at {@linkplain #EPSGFactory construction time}
     * and closed by the {@link #close()} method.
     *
     * @see #close()
     */
    protected final Connection connection;

    /**
     * The factory to use for creating {@link CoordinateReferenceSystem} instances from the properties read in the database.
     */
    protected final CRSFactory crsFactory;

    /**
     * The factory to use for creating {@link CoordinateSystem} instances from the properties read in the database.
     */
    protected final CSFactory csFactory;

    /**
     * The factory to use for creating {@link Datum} instances from the properties read in the database.
     */
    protected final DatumFactory datumFactory;

    /**
     * The locale for producing error messages. This is usually the default locale.
     *
     * @see #getLocale()
     */
    private Locale locale;

    /**
     * Creates a factory using the given connection. The connection will be {@linkplain Connection#close() closed}
     * when this factory will be {@linkplain #close() closed}.
     *
     * @param connection    The connection to the underlying EPSG database.
     * @param crsFactory    The factory to use for creating {@link CoordinateReferenceSystem} instances.
     * @param csFactory     The factory to use for creating {@link CoordinateSystem} instances.
     * @param datumFactory  The factory to use for creating {@link Datum} instances.
     * @param nameFactory   The factory to use for creating authority codes as {@link GenericName} instances.
     */
    public EPSGFactory(final Connection   connection,
                       final CRSFactory   crsFactory,
                       final CSFactory    csFactory,
                       final DatumFactory datumFactory,
                       final NameFactory  nameFactory)
    {
        super(nameFactory);
        ArgumentChecks.ensureNonNull("connection",   connection);
        ArgumentChecks.ensureNonNull("crsFactory",   crsFactory);
        ArgumentChecks.ensureNonNull("csFactory",    csFactory);
        ArgumentChecks.ensureNonNull("datumFactory", datumFactory);
        this.connection   = connection;
        this.crsFactory   = crsFactory;
        this.csFactory    = csFactory;
        this.datumFactory = datumFactory;
        this.namespace    = nameFactory.createNameSpace(nameFactory.createLocalName(null, Constants.EPSG), null);
        this.locale       = Locale.getDefault(Locale.Category.DISPLAY);
    }

    /**
     * Returns the locale used by this factory for producing error messages.
     * This locale does not change the way data are read from the EPSG database.
     *
     * @return The locale for error messages.
     */
    @Override
    public synchronized Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale to use for producing error messages.
     * The given locale will be honored on a <cite>best effort</cite> basis.
     * It does not change the way data are read from the EPSG database.
     *
     * @param locale The new locale to use for error message.
     */
    public synchronized void setLocale(final Locale locale) {
        ArgumentChecks.ensureNonNull("locale", locale);
        this.locale = locale;
    }

    /**
     * Returns the calendar to use for reading dates in the database.
     */
    @SuppressWarnings("ReturnOfDateField")
    private Calendar getCalendar() {
        if (calendar == null) {
            calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.CANADA);
            // Canada locale is closer to ISO than US.
        }
        return calendar;
    }

    /**
     * Returns the authority for this EPSG database. The returned citation contains the database version
     * in the {@linkplain Citation#getEdition() edition} attribute, together with date of last update in
     * the {@linkplain Citation#getEditionDate() edition date}.
     */
    @Override
    public synchronized Citation getAuthority() {
        /*
         * We do not cache this citation because the caching service is already provided by ConcurrentAuthorityFactory
         * and we overridden the trimAuthority(…) and noSuchAuthorityCode(…) methods that invoked this getAuthority().
         */
        final DefaultCitation c = new DefaultCitation("EPSG Geodetic Parameter Dataset");
        c.setIdentifiers(Collections.singleton(new ImmutableIdentifier(null, null, Constants.EPSG)));
        try {
            /*
             * Get the most recent version number from the history table. We get the date in local timezone
             * instead then UTC because the date is for information purpose only, and the local timezone is
             * more likely to be shown nicely (without artificial hours) to the user.
             */
            final String query = adaptSQL("SELECT VERSION_NUMBER, VERSION_DATE FROM [Version History]" +
                                          " ORDER BY VERSION_DATE DESC, VERSION_HISTORY_CODE DESC");
            String version = null;
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(query))
            {
                while (result.next()) {
                    version = result.getString(1);
                    final Date date = result.getDate(2);                            // Local timezone.
                    if (version != null && date != null) {                          // Paranoiac check.
                        c.setEdition(new SimpleInternationalString(version));
                        c.setEditionDate(date);
                        break;
                    }
                }
            }
            /*
             * Add some hard-coded links to EPSG resources, and finally add the JDBC driver name and version number.
             * The list last OnlineResource looks like:
             *
             *    Linkage:      jdbc:derby:/my/path/to/SIS_DATA/Metadata
             *    Function:     Connection
             *    Description:  EPSG dataset version 8.8 on “Apache Derby Embedded JDBC Driver” version 10.12.
             */
            final DatabaseMetaData metadata  = connection.getMetaData();
addURIs:    for (int i=0; ; i++) {
                String url;
                OnLineFunction function;
                InternationalString description = null;
                switch (i) {
                    case 0: url = "http://epsg-registry.org/"; function = OnLineFunction.SEARCH; break;
                    case 1: url = "http://www.epsg.org/"; function = OnLineFunction.DOWNLOAD; break;
                    case 2: {
                        url = metadata.getURL();
                        function = OnLineFunction.valueOf("CONNECTION");
                        description = Messages.formatInternational(Messages.Keys.DataBase_4,
                                Constants.EPSG, version, metadata.getDatabaseProductName(),
                                Version.valueOf(metadata.getDatabaseMajorVersion(),
                                                metadata.getDatabaseMinorVersion()));
                        break;
                    }
                    default: break addURIs;     // Finished adding all URIs.
                }
                final DefaultOnlineResource r = new DefaultOnlineResource();
                try {
                    r.setLinkage(new URI(url));
                } catch (URISyntaxException exception) {
                    unexpectedException("getAuthority", exception);
                }
                r.setFunction(function);
                r.setDescription(description);
                c.getOnlineResources().add(r);
            }
        } catch (SQLException exception) {
            unexpectedException("getAuthority", exception);
        } finally {
            c.freeze();
        }
        return c;
    }

    /**
     * Returns the set of authority codes of the given type.
     * This returned set may keep a connection to the EPSG database,
     * so the set can execute efficiently idioms like the following one:
     *
     * {@preformat java
     *     getAuthorityCodes(type).containsAll(others)
     * }
     *
     * The returned set should not be referenced for a long time, as it may prevent this factory to release
     * JDBC resources. If the set of codes is needed for a long time, their values should be copied in another
     * collection object.
     *
     * @param  type The spatial reference objects type (may be {@code Object.class}).
     * @return The set of authority codes for spatial reference objects of the given type (may be an empty set).
     * @throws FactoryException if access to the underlying database failed.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        return getCodeMap(type).keySet();
    }

    /**
     * Returns a map of EPSG authority codes as keys and object names as values.
     */
    private synchronized Map<String,String> getCodeMap(final Class<?> type) throws FactoryException {
        CloseableReference<AuthorityCodes> reference = authorityCodes.get(type);
        if (reference != null) {
            AuthorityCodes existing = reference.get();
            if (existing != null) {
                return existing;
            }
        }
        Map<String,String> result = Collections.emptyMap();
        for (final TableInfo table : TableInfo.EPSG) {
            /*
             * We test 'isAssignableFrom' in the two ways for catching the following use cases:
             *
             *  - table.type.isAssignableFrom(type)
             *    is for the case where a table is for CoordinateReferenceSystem while the user type is some subtype
             *    like GeographicCRS. The GeographicCRS need to be queried into the CoordinateReferenceSystem table.
             *    An additional filter will be applied inside the AuthorityCodes class implementation.
             *
             *  - type.isAssignableFrom(table.type)
             *    is for the case where the user type is IdentifiedObject or Object, in which case we basically want
             *    to iterate through every tables.
             */
            if (table.type.isAssignableFrom(type) || type.isAssignableFrom(table.type)) {
                /*
                 * Maybe an instance already existed but was not found above because the user specified some
                 * implementation class instead of an interface class. Before to return a newly created map,
                 * check again in the cached maps using the type computed by AuthorityCodes itself.
                 */
                AuthorityCodes codes = new AuthorityCodes(connection, table, type, this);
                reference = authorityCodes.get(codes.type);
                if (reference != null) {
                    AuthorityCodes existing = reference.get();
                    if (existing != null) {
                        codes = existing;
                    } else {
                        reference = null;   // The weak reference is no longer valid.
                    }
                }
                if (reference == null) {
                    reference = codes.createReference();
                    authorityCodes.put(codes.type, reference);
                }
                if (type != codes.type) {
                    authorityCodes.put(type, reference);
                }
                /*
                 * We now have the codes for a single type. Append with the codes of previous types, if any.
                 * This usually happen only if the user asked for the IdentifiedObject type. Of course this
                 * break all our effort to query the data only when first needed, but the user should ask
                 * for more specific types.
                 */
                if (result.isEmpty()) {
                    result = codes;
                } else {
                    if (result instanceof AuthorityCodes) {
                        result = new LinkedHashMap<>(result);
                    }
                    result.putAll(codes);
                }
            }
        }
        return result;
    }

    /**
     * Gets a description of the object corresponding to a code.
     * This method returns the object name in a lightweight manner, without creating the full {@link IdentifiedObject}.
     *
     * @param  code Value allocated by authority.
     * @return The object name, or {@code null} if the object corresponding to the specified {@code code} has no name.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the query failed for some other reason.
     */
    @Override
    public InternationalString getDescriptionText(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        final String primaryKey = trimAuthority(code);
        for (final TableInfo table : TableInfo.EPSG) {
            final String text = getCodeMap(table.type).get(primaryKey);
            if (text != null) {
                return (table.nameColumn != null) ? new SimpleInternationalString(text) : null;
            }
        }
        throw noSuchAuthorityCode(IdentifiedObject.class, code);
    }

    /**
     * Converts a code from an arbitrary name to the numerical identifier (the primary key).
     * If the supplied code is already a numerical value, then it is returned unchanged.
     * If the code is not found in the name column, it is returned unchanged as well
     * so that the caller will produce an appropriate "Code not found" error message.
     * If the code is found more than once, then an exception is thrown.
     *
     * <p>Note that this method includes a call to {@link #trimAuthority(String)},
     * so there is no need to call it before or after this method.</p>
     *
     * <div class="note"><b>Note:</b>
     * this method could be seen as the converse of above {@link #getDescriptionText(String)} method.</div>
     *
     * @param  type        The type of object to create.
     * @param  code        The code to check.
     * @param  table       The table where the code should appears.
     * @param  codeColumn  The column name for the code.
     * @param  nameColumn  The column name for the name.
     * @return The numerical identifier (i.e. the table primary key value).
     * @throws SQLException if an error occurred while reading the database.
     */
    private String toPrimaryKey(final Class<?> type, final String code, final String table,
            final String codeColumn, final String nameColumn) throws SQLException, FactoryException
    {
        assert Thread.holdsLock(this);
        String identifier = trimAuthority(code);
        if (!isPrimaryKey(identifier)) {
            /*
             * The given string is not a numerical code. Search the value in the database.
             * If a prepared statement is already available, reuse it providing that it was
             * created for the current table. Otherwise we will create a new statement.
             */
            final String KEY = "NumericalIdentifier";
            PreparedStatement statement = statements.get(KEY);
            if (statement != null) {
                if (!table.equals(lastTableForName)) {
                    statements.remove(KEY);
                    statement.close();
                    statement        = null;
                    lastTableForName = null;
                }
            }
            if (statement == null) {
                final String query = "SELECT " + codeColumn + " FROM " + table +
                                     " WHERE " + nameColumn + " = ?";
                statement = connection.prepareStatement(adaptSQL(query));
                statements.put(KEY, statement);
            }
            // Do not use executeQuery(statement, primaryKey) because "identifier" is a name here.
            statement.setString(1, identifier);
            identifier = null;
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    identifier = ensureSingleton(result.getString(1), identifier, code);
                }
            }
            if (identifier == null) {
                throw noSuchAuthorityCode(type, code);
            }
        }
        return identifier;
    }

    /**
     * Returns a prepared statement for the specified name. Most {@link PreparedStatement} creations are performed
     * through this method, except {@link #toPrimaryKey} and {@link #createObject(String)}.
     *
     * @param  table A key uniquely identifying the caller (e.g. {@code "Ellipsoid"} for {@link #createEllipsoid(String)}).
     * @param  sql   The SQL statement to use if for creating the {@link PreparedStatement} object.
     *               Will be used only if no prepared statement was already created for the specified key.
     * @return The prepared statement.
     * @throws SQLException if the prepared statement can not be created.
     */
    private PreparedStatement prepareStatement(final String table, final String sql) throws SQLException {
        assert Thread.holdsLock(this);
        PreparedStatement stmt = statements.get(table);
        if (stmt == null) {
            stmt = connection.prepareStatement(adaptSQL(sql));
            statements.put(table, stmt);
        }
        // Partial check that the statement is for the right SQL query.
        assert stmt.getParameterMetaData().getParameterCount() == CharSequences.count(sql, '?');
        return stmt;
    }

    /**
     * Sets the value of the primary key to search for, and executes the given prepared statement.
     * The primary key should be the value returned by {@link #toPrimaryKey}.
     * Its values is assigned to the parameter #1.
     *
     * @param  stmt  The prepared statement in which to set the primary key.
     * @param  primaryKey  The primary key.
     * @throws NoSuchIdentifierException if the primary key has not been found.
     * @throws SQLException if an error occurred while querying the database.
     */
    private ResultSet executeQuery(final PreparedStatement stmt, final String primaryKey)
            throws NoSuchIdentifierException, SQLException
    {
        final int n;
        try {
            n = Integer.parseInt(primaryKey);
        } catch (NumberFormatException e) {
            final NoSuchIdentifierException ne = new NoSuchIdentifierException(error().getString(
                    Errors.Keys.IllegalIdentifierForCodespace_2, Constants.EPSG, primaryKey), primaryKey);
            ne.initCause(e);
            throw ne;
        }
        stmt.setInt(1, n);
        return stmt.executeQuery();
    }

    /**
     * Sets the value of the primary keys to search for, and executes the given prepared statement.
     * The primary keys should be the values returned by {@link #toPrimaryKey}.
     * Their values are assigned to parameters #1 and 2.
     *
     * @param  stmt The prepared statement in which to set the primary key.
     * @param  primaryKey The primary key.
     * @throws SQLException If an error occurred.
     */
    private ResultSet executeQuery(final PreparedStatement stmt, final String pk1, final String pk2)
            throws NoSuchIdentifierException, SQLException
    {
        final int n1, n2;
        String key = pk1;
        try {
            n1 = Integer.parseInt(      pk1);
            n2 = Integer.parseInt(key = pk2);
        } catch (NumberFormatException e) {
            final NoSuchIdentifierException ne = new NoSuchIdentifierException(error().getString(
                    Errors.Keys.IllegalIdentifierForCodespace_2, Constants.EPSG, key), key);
            ne.initCause(e);
            throw ne;
        }
        stmt.setInt(1, n1);
        stmt.setInt(2, n2);
        return stmt.executeQuery();
    }

    /**
     * Same as {@link #getString(ResultSet, int, Object)},
     * but reports the fault on an alternative column if the value is null.
     */
    private String getString(final ResultSet result, final int columnIndex,
                             final String    code,   final int columnFault)
            throws SQLException, FactoryDataException
    {
        final String str = result.getString(columnIndex);
        if (result.wasNull()) {
            final ResultSetMetaData metadata = result.getMetaData();
            final String column = metadata.getColumnName(columnFault);
            final String table  = metadata.getTableName (columnFault);
            result.close();
            throw new FactoryDataException(error().getString(Errors.Keys.NullValueInTable_3, table, column, code));
        }
        return str.trim();
    }

    /**
     * Gets the string from the specified {@link ResultSet}.
     * The string is required to be non-null. A null string will throw an exception.
     *
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @param  code         The identifier of the record where the string was found.
     * @return The string at the specified column.
     * @throws SQLException if an error occurred while querying the database.
     * @throws FactoryDataException if a null value was found.
     */
    private String getString(final ResultSet result, final int columnIndex, final Object code)
            throws SQLException, FactoryDataException
    {
        final String value = result.getString(columnIndex);
        ensureNonNull(result, columnIndex, code);
        return value.trim();
    }

    /**
     * Gets the value from the specified {@link ResultSet}.
     * The value is required to be non-null. A null value (i.e. blank) will throw an exception.
     *
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @param  code         The identifier of the record where the string was found.
     * @return The double at the specified column.
     * @throws SQLException if an error occurred while querying the database.
     * @throws FactoryDataException if a null value was found.
     */
    private double getDouble(final ResultSet result, final int columnIndex, final Object code)
            throws SQLException, FactoryDataException
    {
        final double value = result.getDouble(columnIndex);
        ensureNonNull(result, columnIndex, code);
        return value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}.
     * The value is required to be non-null. A null value (i.e. blank) will throw an exception.
     *
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @param  code         The identifier of the record where the string was found.
     * @return The integer at the specified column.
     * @throws SQLException if an error occurred while querying the database.
     * @throws FactoryDataException if a null value was found.
     */
    private int getInt(final ResultSet result, final int columnIndex, final Object code)
            throws SQLException, FactoryDataException
    {
        final int value = result.getInt(columnIndex);
        ensureNonNull(result, columnIndex, code);
        return value;
    }

    /**
     * Makes sure that the last result was non-null.
     * Used for {@code getString(…)}, {@code getDouble(…)} and {@code getInt(…)} methods only.
     */
    private void ensureNonNull(final ResultSet result, final int columnIndex, final Object code)
            throws SQLException, FactoryDataException
    {
        if (result.wasNull()) {
            final ResultSetMetaData metadata = result.getMetaData();
            final String column = metadata.getColumnName(columnIndex);
            final String table  = metadata.getTableName (columnIndex);
            result.close();
            throw new FactoryDataException(error().getString(Errors.Keys.NullValueInTable_3, table, column, code));
        }
    }

    /**
     * Makes sure that an object constructed from the database is not incoherent.
     * If the code supplied to a {@code createFoo(String)} method exists in the database,
     * then we should find only one record. However we will do a paranoiac check and verify if there is
     * more records, using a {@code while (results.next())} loop instead of {@code if (results.next())}.
     * This method is invoked in the loop for making sure that, if there is more than one record
     * (which should never happen), at least they have identical content.
     *
     * @param  newValue  The newly constructed object.
     * @param  oldValue  The object previously constructed, or {@code null} if none.
     * @param  code The EPSG code (for formatting error message).
     * @throws FactoryDataException if a duplication has been detected.
     */
    private <T> T ensureSingleton(final T newValue, final T oldValue, final String code) throws FactoryDataException {
        if (oldValue == null) {
            return newValue;
        }
        if (oldValue.equals(newValue)) {
            return oldValue;
        }
        throw new FactoryDataException(error().getString(Errors.Keys.DuplicatedIdentifier_1, code));
    }

    /**
     * Returns {@code true} if the given table {@code name} matches the {@code expected} name.
     * The given {@code name} may be prefixed by {@code "epsg_"} and may contain abbreviations of the full name.
     * For example {@code "epsg_coordoperation"} is considered as a match for {@code "Coordinate_Operation"}.
     *
     * @param  expected  The expected table name (e.g. {@code "Coordinate_Operation"}).
     * @param  name      The actual table name.
     * @return Whether the given {@code name} is considered to match the expected name.
     */
    static boolean tableMatches(final String expected, String name) {
        if (name == null) {
            return false;
        }
        if (name.startsWith(TABLE_PREFIX)) {
            name = name.substring(TABLE_PREFIX.length());
        }
        return CharSequences.isAcronymForWords(name, expected);
    }

    /**
     * Logs a warning saying that the given code is deprecated and returns a message proposing a replacement.
     *
     * @param  table  The table of the deprecated code.
     * @param  code   The deprecated code.
     * @return A message proposing a replacement, or {@code null} if none.
     */
    private InternationalString getDeprecation(final String table, final String code)
            throws SQLException, NoSuchIdentifierException
    {
        final PreparedStatement stmt = prepareStatement("[Deprecation]",
                "SELECT OBJECT_TABLE_NAME, DEPRECATION_REASON, REPLACED_BY" +
                " FROM [Deprecation] WHERE OBJECT_CODE = ?");
        String reason = null;
        Object replacedBy = null;
        try (ResultSet result = executeQuery(stmt, code)) {
            while (result.next()) {
                if (tableMatches(table, result.getString(1))) {
                    reason = result.getString(2);
                    replacedBy = result.getInt(3);
                    if (result.wasNull()) {
                        replacedBy = null;
                    }
                    break;
                }
            }
        }
        if (replacedBy == null) {
            replacedBy = '(' + Vocabulary.getResources(locale).getString(Vocabulary.Keys.None).toLowerCase(locale) + ')';
        }
        /*
         * Try to infer the method name from the table name. For example if the deprecated code was found in
         * the [Coordinate Reference System] table, then we declare createCoordinateReferenceSystem(String)
         * as the source of the log message.
         */
        String method = "create";
        for (final TableInfo info : TableInfo.EPSG) {
            if (tableMatches(info.table, table)) {
                method += info.type.getSimpleName();
                break;
            }
        }
        LogRecord record = Messages.getResources(locale).getLogRecord(Level.WARNING, Messages.Keys.DeprecatedCode_3,
                Constants.EPSG + DefaultNameSpace.DEFAULT_SEPARATOR + code, replacedBy, reason);
        record.setLoggerName(Loggers.CRS_FACTORY);
        Logging.log(EPSGFactory.class, method, record);
        return Vocabulary.formatInternational(Vocabulary.Keys.SupersededBy_1, replacedBy);
    }

    /**
     * Returns the name and aliases for the {@link IdentifiedObject} to construct.
     *
     * @param  table       The table on which a query has been executed.
     * @param  name        The name for the {@link IndentifiedObject} to construct.
     * @param  code        The EPSG code of the object to construct.
     * @param  remarks     Remarks, or {@code null} if none.
     * @param  deprecated  {@code true} if the object to create is deprecated.
     * @return The name together with a set of properties.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<String,Object> createProperties(final String table, String name, String code,
            String remarks, final boolean deprecated) throws SQLException, FactoryException
    {
        properties.clear();
        GenericName gn = null;
        final Citation authority = buffered.getAuthority();
        final InternationalString edition = authority.getEdition();
        final String version = (edition != null) ? edition.toString() : null;
        if (name != null) {
            name = name.trim();
            gn = nameFactory.createLocalName(namespace, name);
            properties.put("name", gn);
            properties.put(NamedIdentifier.CODE_KEY,      name);
            properties.put(NamedIdentifier.VERSION_KEY,   version);
            properties.put(NamedIdentifier.AUTHORITY_KEY, authority);
            properties.put(AbstractIdentifiedObject.LOCALE_KEY, locale);
            final NamedIdentifier id = new NamedIdentifier(properties);
            properties.clear();
            properties.put(IdentifiedObject.NAME_KEY, id);
        }
        if (code != null) {
            code = code.trim();
            final ImmutableIdentifier identifier;
            if (deprecated) {
                identifier = new DeprecatedCode(authority, Constants.EPSG, code, version, getDeprecation(table, code));
            } else {
                identifier = new ImmutableIdentifier(authority, Constants.EPSG, code, version,
                                    (gn != null) ? gn.toInternationalString() : null);
            }
            properties.put(IdentifiedObject.IDENTIFIERS_KEY, identifier);
        }
        if (remarks != null && !(remarks = remarks.trim()).isEmpty()) {
            properties.put(IdentifiedObject.REMARKS_KEY, remarks);
        }
        /*
         * Search for aliases. Note that searching for the object code is not sufficient. We also need to check if the
         * record is really from the table we are looking for since different tables may have objects with the same ID.
         */
        final List<GenericName> aliases = new ArrayList<>();
        final PreparedStatement stmt = prepareStatement("[Alias]",
                "SELECT OBJECT_TABLE_NAME, NAMING_SYSTEM_NAME, ALIAS" +
                " FROM [Alias] INNER JOIN [Naming System]" +
                  " ON [Alias].NAMING_SYSTEM_CODE =" +
                " [Naming System].NAMING_SYSTEM_CODE" +
                " WHERE OBJECT_CODE = ?");
        try (ResultSet result = executeQuery(stmt, code)) {
            while (result.next()) {
                if (tableMatches(table, result.getString(1))) {
                    String naming = result.getString(2);
                    String alias = getString(result, 3, code);
                    NameSpace ns = null;
                    if (naming != null) {
                        naming = naming.trim();
                        ns = namingSystems.get(naming);
                        if (ns == null) {
                            ns = nameFactory.createNameSpace(nameFactory.createLocalName(null, naming), null);
                            namingSystems.put(naming, ns);
                        }
                    }
                    aliases.add(nameFactory.createLocalName(ns, alias));
                }
            }
        }
        if (!aliases.isEmpty()) {
            properties.put(IdentifiedObject.ALIAS_KEY, aliases.toArray(new GenericName[aliases.size()]));
        }
        properties.put(AbstractIdentifiedObject.LOCALE_KEY, locale);
        return properties;
    }

    /**
     * Returns the name, aliases and domain of validity for the {@link IdentifiedObject} to construct.
     *
     * @param  table      The table on which a query has been executed.
     * @param  name       The name for the {@link IndentifiedObject} to construct.
     * @param  code       The EPSG code of the object to construct.
     * @param  domainCode The code for the domain of validity, or {@code null} if none.
     * @param  scope      The scope, or {@code null} if none.
     * @param  remarks    Remarks, or {@code null} if none.
     * @param  deprecated {@code true} if the object to create is deprecated.
     * @return The name together with a set of properties.
     */
    private Map<String,Object> createProperties(final String table, final String name, final String code,
            String domainCode, String scope, String remarks, final boolean deprecated) throws SQLException, FactoryException
    {
        final Map<String,Object> properties = createProperties(table, name, code, remarks, deprecated);
        if (domainCode != null  &&  !(domainCode = domainCode.trim()).isEmpty()) {
            final Extent extent = buffered.createExtent(domainCode);
            properties.put(Datum.DOMAIN_OF_VALIDITY_KEY, extent);
        }
        if (scope != null &&  !(scope = scope.trim()).isEmpty()) {
            properties.put(Datum.SCOPE_KEY, scope);
        }
        return properties;
    }

    /**
     * Returns an arbitrary object from a code. The default implementation delegates to more specific methods,
     * for example {@link #createCoordinateReferenceSystem(String)}, {@link #createDatum(String)}, <i>etc.</i>
     * until a successful one is found.
     *
     * <p><strong>Note that this method may be ambiguous</strong> since the same EPSG code can be used for different
     * kind of objects. This method may throw an exception if it detects an ambiguity, but this is not guaranteed.
     * It is recommended to invoke the most specific {@code createFoo(String)} method when the desired type is known,
     * both for performance reason and for avoiding ambiguity.</p>
     *
     * @param  code Value allocated by EPSG.
     * @return The object for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createCoordinateReferenceSystem(String)
     * @see #createDatum(String)
     * @see #createCoordinateSystem(String)
     */
    @Override
    public synchronized IdentifiedObject createObject(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        final String      KEY   = "IdentifiedObject";
        PreparedStatement stmt  = statements.get(KEY);  // Null allowed.
        StringBuilder     query = null;                 // Will be created only if the last statement does not suit.
        /*
         * Iterates through all tables listed in TableInfo.EPSG, starting with the table used during the last call to
         * this method. This approach assumes that two consecutive calls will often return the same type of object.
         * If the object type changed, then this method will have to discard the old prepared statement and prepare
         * a new one, which may be a costly operation. Only the last successful prepared statement is cached,
         * in order to keep the amount of statements low. Unsuccessful statements are immediately closed.
         */
        final String  epsg         = trimAuthority(code);
        final boolean isPrimaryKey = isPrimaryKey(epsg);
        final int     tupleToSkip  = isPrimaryKey ? lastObjectType : -1;
        int index = -1;
        for (int i = -1; i < TableInfo.EPSG.length; i++) {
            if (i == tupleToSkip) {
                // Avoid to test the same table twice. Avoid also a NullPointerException
                // if 'stmt' is null, since 'lastObjectType' should be -1 in this case.
                continue;
            }
            try {
                if (i >= 0) {
                    final TableInfo table = TableInfo.EPSG[i];
                    final String column = isPrimaryKey ? table.codeColumn : table.nameColumn;
                    if (column == null) {
                        continue;
                    }
                    if (query == null) {
                        query = new StringBuilder("SELECT ");
                    }
                    query.setLength(7);   // 7 is the length of "SELECT " in the above line.
                    query.append(table.codeColumn).append(" FROM ").append(table.table)
                         .append(" WHERE ").append(column).append(" = ?");
                    if (isPrimaryKey) {
                        assert !statements.containsKey(KEY) : table;
                        stmt = prepareStatement(KEY, query.toString());
                    } else {
                        // Do not cache the statement for names.
                        stmt = connection.prepareStatement(adaptSQL(query.toString()));
                    }
                }
                /*
                 * Checks if at least one record is found for the code. If the code is the primary key, then we
                 * will stop at the first table found since the EPSG database contains few duplicate identifiers
                 * (actually it still have some, but we assume that the risk of collision is low).
                 * If the code is a name, then we need to search in all tables since duplicate names exist.
                 */
                final ResultSet result;
                if (isPrimaryKey) {
                    result = executeQuery(stmt, epsg);
                } else {
                    stmt.setString(1, epsg);
                    result = stmt.executeQuery();
                }
                final boolean present;
                try {
                    present = result.next();
                } finally {
                    result.close();
                }
                if (present) {
                    if (index >= 0) {
                        throw new FactoryDataException(error().getString(Errors.Keys.DuplicatedIdentifier_1, code));
                    }
                    index = (i < 0) ? lastObjectType : i;
                    if (isPrimaryKey) {
                        // Do not scan other tables, since EPSG code are more likely to be unique.
                        // Note that names are more at risk to be duplicated, so we do not stop for names.
                        break;
                    }
                }
                if (isPrimaryKey) {
                    if (statements.remove(KEY) == null) {
                        throw new AssertionError(code);         // Should never happen.
                    }
                }
                stmt.close();
            } catch (SQLException exception) {
                throw databaseFailure(IdentifiedObject.class, code, exception);
            }
        }
        /*
         * If a record has been found in one table, then delegates to the appropriate method.
         */
        if (isPrimaryKey) {
            lastObjectType = index;
        }
        if (index >= 0) {
            switch (index) {
                case 0:  return createCoordinateReferenceSystem(code);
                case 1:  return createCoordinateSystem         (code);
                case 2:  return createCoordinateSystemAxis     (code);
                case 3:  return createDatum                    (code);
                case 4:  return createEllipsoid                (code);
                case 5:  return createPrimeMeridian            (code);
                case 6:  return createCoordinateOperation      (code);
                case 7:  return createOperationMethod          (code);
                case 8:  return createParameterDescriptor      (code);
                case 9:  break; // Can not cast Unit to IdentifiedObject
                default: throw new AssertionError(index);                   // Should not happen
            }
        }
        throw noSuchAuthorityCode(IdentifiedObject.class, code);
    }

    /**
     * Creates a geometric figure that can be used to describe the approximate shape of the earth.
     * In mathematical terms, it is a surface formed by the rotation of an ellipse about its minor axis.
     *
     * @param  code Value allocated by EPSG.
     * @return The ellipsoid for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createGeodeticDatum(String)
     * @see #createEllipsoidalCS(String)
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid
     */
    @Override
    public synchronized Ellipsoid createEllipsoid(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        Ellipsoid returnValue = null;
        try {
            final String primaryKey = toPrimaryKey(Ellipsoid.class, code,
                    "[Ellipsoid]", "ELLIPSOID_CODE", "ELLIPSOID_NAME");
            final PreparedStatement stmt = prepareStatement("[Ellipsoid]",
                    "SELECT ELLIPSOID_CODE," +
                          " ELLIPSOID_NAME," +
                          " SEMI_MAJOR_AXIS," +
                          " INV_FLATTENING," +
                          " SEMI_MINOR_AXIS," +
                          " UOM_CODE," +
                          " REMARKS," +
                          " DEPRECATED" +
                    " FROM [Ellipsoid]" +
                    " WHERE ELLIPSOID_CODE = ?");
            try (ResultSet result = executeQuery(stmt, primaryKey)) {
                while (result.next()) {
                    /*
                     * One of 'semiMinorAxis' and 'inverseFlattening' values can be NULL in the database.
                     * Consequently, we don't use 'getString(ResultSet, int)' for those parameters because
                     * we do not want to thrown an exception if a NULL value is found.
                     */
                    final String  epsg              = getString(result, 1, code);
                    final String  name              = getString(result, 2, code);
                    final double  semiMajorAxis     = getDouble(result, 3, code);
                    final double  inverseFlattening = result.getDouble( 4);
                    final double  semiMinorAxis     = result.getDouble( 5);
                    final String  unitCode          = getString(result, 6, code);
                    final String  remarks           = result.getString( 7);
                    final boolean deprecated        = result.getInt   ( 8) != 0;
                    final Unit<Length> unit         = buffered.createUnit(unitCode).asType(Length.class);
                    final Map<String,Object> properties = createProperties("[Ellipsoid]", name, epsg, remarks, deprecated);
                    final Ellipsoid ellipsoid;
                    if (inverseFlattening == 0) {
                        if (semiMinorAxis == 0) {
                            // Both are null, which is not allowed.
                            final String column = result.getMetaData().getColumnName(3);
                            throw new FactoryDataException(error().getString(Errors.Keys.NullValueInTable_3, code, column));
                        } else {
                            // We only have semiMinorAxis defined. It is OK
                            ellipsoid = datumFactory.createEllipsoid(properties, semiMajorAxis, semiMinorAxis, unit);
                        }
                    } else {
                        if (semiMinorAxis != 0) {
                            // Both 'inverseFlattening' and 'semiMinorAxis' are defined.
                            // Log a warning and create the ellipsoid using the inverse flattening.
                            final LogRecord record = Messages.getResources(locale).getLogRecord(Level.WARNING,
                                    Messages.Keys.AmbiguousEllipsoid_1, Constants.EPSG + DefaultNameSpace.DEFAULT_SEPARATOR + code);
                            record.setLoggerName(Loggers.CRS_FACTORY);
                            Logging.log(EPSGFactory.class, "createEllipsoid", record);
                        }
                        ellipsoid = datumFactory.createFlattenedSphere(properties, semiMajorAxis, inverseFlattening, unit);
                    }
                    returnValue = ensureSingleton(ellipsoid, returnValue, code);
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(Ellipsoid.class, code, exception);
        }
        if (returnValue == null) {
             throw noSuchAuthorityCode(Ellipsoid.class, code);
        }
        return returnValue;
    }

    /**
     * Creates a prime meridian defining the origin from which longitude values are determined.
     *
     * @param  code Value allocated by EPSG.
     * @return The prime meridian for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createGeodeticDatum(String)
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian
     */
    @Override
    public synchronized PrimeMeridian createPrimeMeridian(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        PrimeMeridian returnValue = null;
        try {
            final String primaryKey = toPrimaryKey(PrimeMeridian.class, code,
                    "[Prime Meridian]", "PRIME_MERIDIAN_CODE", "PRIME_MERIDIAN_NAME");
            final PreparedStatement stmt = prepareStatement("[Prime Meridian]",
                    "SELECT PRIME_MERIDIAN_CODE," +
                          " PRIME_MERIDIAN_NAME," +
                          " GREENWICH_LONGITUDE," +
                          " UOM_CODE," +
                          " REMARKS," +
                          " DEPRECATED" +
                    " FROM [Prime Meridian]" +
                    " WHERE PRIME_MERIDIAN_CODE = ?");
            try (ResultSet result = executeQuery(stmt, primaryKey)) {
                while (result.next()) {
                    final String  epsg       = getString(result, 1, code);
                    final String  name       = getString(result, 2, code);
                    final double  longitude  = getDouble(result, 3, code);
                    final String  unitCode   = getString(result, 4, code);
                    final String  remarks    = result.getString( 5);
                    final boolean deprecated = result.getInt   ( 6) != 0;
                    final Unit<Angle> unit = buffered.createUnit(unitCode).asType(Angle.class);
                    Map<String,Object> properties = createProperties("[Prime Meridian]", name, epsg, remarks, deprecated);
                    PrimeMeridian primeMeridian = datumFactory.createPrimeMeridian(properties, longitude, unit);
                    returnValue = ensureSingleton(primeMeridian, returnValue, code);
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(PrimeMeridian.class, code, exception);
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(PrimeMeridian.class, code);
        }
        return returnValue;
    }

    /**
     * Creates information about spatial, vertical, and temporal extent (usually a domain of validity) from a code.
     *
     * @param  code Value allocated by EPSG.
     * @return The extent for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createCoordinateReferenceSystem(String)
     * @see #createDatum(String)
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    @Override
    public synchronized Extent createExtent(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        Extent returnValue = null;
        try {
            final String primaryKey = toPrimaryKey(Extent.class, code,
                    "[Area]", "AREA_CODE", "AREA_NAME");
            final PreparedStatement stmt = prepareStatement("[Area]",
                    "SELECT AREA_OF_USE," +
                          " AREA_SOUTH_BOUND_LAT," +
                          " AREA_NORTH_BOUND_LAT," +
                          " AREA_WEST_BOUND_LON," +
                          " AREA_EAST_BOUND_LON" +
                    " FROM [Area]" +
                    " WHERE AREA_CODE = ?");
            try (ResultSet result = executeQuery(stmt, primaryKey)) {
                while (result.next()) {
                    String description = result.getString(1);
                    if (description != null && (description = description.trim()).isEmpty()) {
                        description = null;
                    }
                    DefaultGeographicBoundingBox bbox = null;
                    double ymin = result.getDouble(2); if (result.wasNull()) ymin = Double.NaN;
                    double ymax = result.getDouble(3); if (result.wasNull()) ymax = Double.NaN;
                    double xmin = result.getDouble(4); if (result.wasNull()) xmin = Double.NaN;
                    double xmax = result.getDouble(5); if (result.wasNull()) xmax = Double.NaN;
                    if (!Double.isNaN(ymin) || !Double.isNaN(ymax) || !Double.isNaN(xmin) || !Double.isNaN(xmax)) {
                        /*
                         * Fix an error found in EPSG:3790 New Zealand - South Island - Mount Pleasant mc
                         * for older database (this error is fixed in EPSG database 8.2).
                         *
                         * Do NOT apply anything similar for the x axis, because xmin > xmax is not error:
                         * it describes a bounding box spanning the anti-meridian (±180° of longitude).
                         */
                        if (ymin > ymax) {
                            final double t = ymin;
                            ymin = ymax;
                            ymax = t;
                        }
                        bbox = new DefaultGeographicBoundingBox(xmin, xmax, ymin, ymax);
                    }
                    if (description != null || bbox != null) {
                        DefaultExtent extent = new DefaultExtent(description, bbox, null, null);
                        extent.freeze();
                        returnValue = ensureSingleton(extent, returnValue, code);
                    }
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(Extent.class, code, exception);
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(Extent.class, code);
        }
        return returnValue;
    }

    /**
     * Creates an unit of measurement from a code.
     *
     * @param  code Value allocated by EPSG.
     * @return The unit of measurement for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized Unit<?> createUnit(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        Unit<?> returnValue = null;
        try {
            final String primaryKey = toPrimaryKey(Unit.class, code,
                    "[Unit of Measure]", "UOM_CODE", "UNIT_OF_MEAS_NAME");
            final PreparedStatement stmt;
            stmt = prepareStatement("[Unit of Measure]",
                    "SELECT UOM_CODE," +
                          " FACTOR_B," +
                          " FACTOR_C," +
                          " TARGET_UOM_CODE," +
                          " UNIT_OF_MEAS_NAME" +
                    " FROM [Unit of Measure]" +
                    " WHERE UOM_CODE = ?");
            try (ResultSet result = executeQuery(stmt, primaryKey)) {
                while (result.next()) {
                    final int source = getInt(result,   1, code);
                    final double   b = result.getDouble(2);
                    final double   c = result.getDouble(3);
                    final int target = getInt(result,   4, code);
                    if (source == target) {
                        /*
                         * The unit is a base unit. Verify its consistency:
                         * conversion from 'source' to itself shall be the identity function.
                         */
                        final boolean pb = (b != 1);
                        if (pb || c != 1) {
                            throw new FactoryDataException(error().getString(Errors.Keys.InconsistentAttribute_2,
                                        pb ? "FACTOR_B" : "FACTOR_C", pb ? b : c));
                        }
                    }
                    Unit<?> unit = Units.valueOfEPSG(source);           // Check in our list of hard-coded unit codes.
                    if (unit == null) {
                        final Unit<?> base = Units.valueOfEPSG(target);
                        if (base != null && b != 0 && c != 0) {         // May be 0 if the conversion is non-linear.
                            unit = Units.multiply(base, b/c);
                        } else try {
                            unit = Units.valueOf(result.getString(5));  // Try parsing the unit symbol as a fallback.
                        } catch (IllegalArgumentException e) {
                            throw new FactoryDataException(error().getString(Errors.Keys.UnknownUnit_1, code), e);
                        }
                    }
                    returnValue = ensureSingleton(unit, returnValue, code);
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(Unit.class, code, exception);
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(Unit.class, code);
        }
        return returnValue;
    }

    final boolean isProjection(final int code) throws NoSuchIdentifierException, SQLException {
        return false;
    }

    /**
     * Invoked when a new {@link PreparedStatement} is about to be created from a SQL string.
     * Since the <a href="http://www.epsg.org">EPSG database</a> is available primarily in MS-Access format,
     * SQL statements are formatted using a syntax specific to this particular database software
     * (for example "{@code SELECT * FROM [Coordinate Reference System]}").
     * When a subclass targets another database vendor, it must overrides this method in order to adapt the SQL syntax.
     *
     * <div class="note"><b>Example</b>
     * a subclass connecting to a <cite>PostgreSQL</cite> database would replace the watching braces
     * ({@code '['} and {@code ']'}) by the quote character ({@code '"'}).</div>
     *
     * The default implementation returns the given statement unchanged.
     *
     * @param  statement The statement in MS-Access syntax.
     * @return The SQL statement adapted to the syntax of the target database.
     */
    protected String adaptSQL(final String statement) {
        return statement;
    }

    /**
     * Returns {@code true} if the specified code may be a primary key in some table.
     * This method does not need to check any entry in the database.
     * It should just check from the syntax if the code looks like a valid EPSG identifier.
     *
     * <p>When this method returns {@code false}, {@code createFoo(String)} methods
     * may look for the code in the name column instead than the primary key column.
     * This allows to accept the <cite>"WGS 84 / World Mercator"</cite> string (for example)
     * in addition to the {@code "3395"} primary key. Both string values should fetch the same object.</p>
     *
     * <p>If this method returns {@code true}, then this factory does not search for matching names.
     * In such case, an appropriate exception will be thrown in {@code createFoo(String)} methods
     * if the code is not found in the primary key column.</p>
     *
     * <div class="section">Default implementation</div>
     * The default implementation returns {@code true} if all non-space characters
     * are {@linkplain Character#isDigit(int) digits}.
     *
     * @param  code  The code the inspect.
     * @return {@code true} if the code is probably a primary key.
     * @throws FactoryException if an unexpected error occurred while inspecting the code.
     */
    protected boolean isPrimaryKey(final String code) throws FactoryException {
        final int length = code.length();
        for (int i=0; i<length;) {
            final int c = code.codePointAt(i);
            if (!Character.isDigit(c) && !Character.isSpaceChar(c)) {
                return false;
            }
            i += Character.charCount(c);
        }
        return true;
    }

    /**
     * Removes the {@code "EPSG:"} prefix from the given string, if present.
     * This method overrides the more generic implementation provided by the subclasses for efficiency.
     * In particular, this method avoid to call the potentially costly {@link #getAuthority()} method.
     *
     * @param  code The code to trim.
     * @return The code without the {@code "EPSG:"} prefix.
     */
    @Override
    protected String trimAuthority(String code) {
        int s = code.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR);
        if (s >= 0 && Constants.EPSG.equals(code.substring(0, s).trim())) {
            code = code.substring(s+1).trim();
        }
        return code;
    }

    /**
     * Creates an exception for an unknown authority code.
     * This convenience method is provided for implementation of {@code createFoo(String)} methods.
     *
     * @param  type  The GeoAPI interface that was to be created (e.g. {@code CoordinateReferenceSystem.class}).
     * @param  code  The unknown authority code.
     * @return An exception initialized with an error message built from the specified informations.
     */
    @Override
    protected NoSuchAuthorityCodeException noSuchAuthorityCode(final Class<?> type, final String code) {
        return new NoSuchAuthorityCodeException(error().getString(Errors.Keys.NoSuchAuthorityCode_3,
                Constants.EPSG, type, code), Constants.EPSG, trimAuthority(code), code);
    }

    /**
     * Constructs an exception for recursive calls.
     */
    private FactoryException recursiveCall(final Class<?> type, final String code) {
        return new FactoryException(error().getString(Errors.Keys.RecursiveCreateCallForCode_2, type, code));
    }

    /**
     * Constructs an exception for a database failure.
     */
    private FactoryException databaseFailure(Class<?> type, String code, SQLException cause) {
        return new FactoryException(error().getString(Errors.Keys.DatabaseError_2, type, code), cause);
    }

    /**
     * Minor shortcut for fetching the error resources.
     */
    private Errors error() {
        return Errors.getResources(locale);
    }

    /**
     * Logs a warning about an unexpected but non-fatal exception.
     *
     * @param method    The source method.
     * @param exception The exception to log.
     */
    private static void unexpectedException(final String method, final Exception exception) {
        Logging.unexpectedException(Logging.getLogger(Loggers.CRS_FACTORY), EPSGFactory.class, method, exception);
    }

    /**
     * Returns {@code true} if it is safe to close this factory. This method is invoked indirectly
     * by {@link ConcurrentAuthorityFactory} after some timeout in order to release resources.
     * This method will block the disposal if some {@link AuthorityCodes} are still in use.
     */
    final synchronized boolean canClose() {
        boolean can = true;
        if (authorityCodes != null) {
            System.gc();                // For cleaning as much weak references as we can before we check them.
            final Iterator<CloseableReference<AuthorityCodes>> it = authorityCodes.values().iterator();
            while (it.hasNext()) {
                final AuthorityCodes codes = it.next().get();
                if (codes == null) {
                    it.remove();
                } else {
                    /*
                     * A set of authority codes is still in use. We can not close this factory.
                     * But we continue the iteration anyway in order to cleanup weak references.
                     */
                    can = false;
                }
            }
        }
        return can;
    }

    /**
     * Closes the JDBC connection used by this factory.
     * If this {@code EPSGFactory} is used by a {@link ConcurrentAuthorityFactory}, then this method
     * will be automatically invoked after some {@linkplain ConcurrentAuthorityFactory#getTimeout timeout}.
     *
     * @throws FactoryException if an error occurred while closing the connection.
     *
     * @see #connection
     */
    @Override
    public synchronized void close() throws FactoryException {
        SQLException exception = null;
        final Iterator<PreparedStatement> ip = statements.values().iterator();
        while (ip.hasNext()) {
            try {
                ip.next().close();
            } catch (SQLException e) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
            ip.remove();
        }
        final Iterator<CloseableReference<AuthorityCodes>> it = authorityCodes.values().iterator();
        while (it.hasNext()) {
            try {
                it.next().close();
            } catch (SQLException e) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
            it.remove();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            if (exception == null) {
                exception = e;
            } else {
                e.addSuppressed(exception);     // Keep the connection thrown be Connection as the main one to report.
            }
        }
        if (exception != null) {
            throw new FactoryException(exception);
        }
    }
}
