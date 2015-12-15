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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;
import java.lang.ref.Reference;
import java.text.DateFormat;
import java.net.URI;
import java.net.URISyntaxException;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.converter.ConversionException;

import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.internal.referencing.DeprecatedCode;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.ConcurrentAuthorityFactory;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Disposable;
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
 * It is recommended to have those instances created on the fly by {@link ConcurrentAuthorityFactory} and disposed
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
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory, Disposable
{
    /**
     * The prefix in table names. The SQL scripts are provided by EPSG with this prefix in front of all table names.
     * SIS rather uses a modified version of those SQL scripts which creates the tables in an "EPSG" database schema.
     * But we still need to check for existence of this prefix in case someone used the original SQL scripts.
     */
    private static final String TABLE_PREFIX = "epsg_";



    //////////////////////////////////////////////////////////////////////////////////////////////
    //////                                                                                 ///////
    //////   HARD CODED VALUES (other than SQL statements) RELATIVE TO THE EPSG DATABASE   ///////
    //////                                                                                 ///////
    //////////////////////////////////////////////////////////////////////////////////////////////

    // See org.apache.sis.measure.Units.valueOfEPSG(int) for hard-code units from EPSG codes.

    /**
     * Sets a Bursa-Wolf parameter from an EPSG parameter.
     *
     * @param  parameters The Bursa-Wolf parameters to modify.
     * @param  code       The EPSG code for a parameter from the [PARAMETER_CODE] column.
     * @param  value      The value of the parameter from the [PARAMETER_VALUE] column.
     * @param  unit       The unit of the parameter value from the [UOM_CODE] column.
     * @throws FactoryException if the code is unrecognized.
     */
    private static void setBursaWolfParameter(final BursaWolfParameters parameters,
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
            throw new FactoryDataException(Errors.format(Errors.Keys.IncompatibleUnit_1, unit), e);
        }
        switch (code) {
            case 8605: parameters.tX = value; break;
            case 8606: parameters.tY = value; break;
            case 8607: parameters.tZ = value; break;
            case 8608: parameters.rX = value; break;
            case 8609: parameters.rY = value; break;
            case 8610: parameters.rZ = value; break;
            case 8611: parameters.dS = value; break;
            default: throw new FactoryDataException(Errors.format(Errors.Keys.UnexpectedParameter_1, code));
        }
    }
    // Datum shift operation methods
    /** First Bursa-Wolf method. */ private static final int BURSA_WOLF_MIN_CODE = 9603;
    /** Last Bursa-Wolf method.  */ private static final int BURSA_WOLF_MAX_CODE = 9607;
    /** Rotation frame method.   */ private static final int ROTATION_FRAME_CODE = 9607;

    /**
     * List of tables and columns to test for codes values. Those tables are used by the {@link #createObject(String)}
     * method in order to detect which of the following methods should be invoked for a given code:
     *
     * {@link #createCoordinateReferenceSystem(String)}
     * {@link #createCoordinateSystem(String)}
     * {@link #createDatum(String)}
     * {@link #createEllipsoid(String)}
     * {@link #createUnit(String)}
     *
     * The order is significant: it is the key for a {@code switch} statement.
     *
     * @see #createObject(String)
     * @see #lastObjectType
     */
    private static final TableInfo[] TABLES_INFO = {
        new TableInfo(CoordinateReferenceSystem.class,
                "[Coordinate Reference System]",
                "COORD_REF_SYS_CODE",
                "COORD_REF_SYS_NAME",
                "COORD_REF_SYS_KIND",
                new Class<?>[] { ProjectedCRS.class, GeographicCRS.class, GeocentricCRS.class,
                                 VerticalCRS.class,  CompoundCRS.class,   EngineeringCRS.class},
                new String[]   {"projected",        "geographic",        "geocentric",
                                "vertical",         "compound",          "engineering"}),

        new TableInfo(CoordinateSystem.class,
                "[Coordinate System]",
                "COORD_SYS_CODE",
                "COORD_SYS_NAME",
                "COORD_SYS_TYPE",
                new Class<?>[] { CartesianCS.class, EllipsoidalCS.class, SphericalCS.class, VerticalCS.class},
                new String[]   {"Cartesian",       "ellipsoidal",       "spherical",       "vertical"}),
                               //Really upper-case C.
        new TableInfo(CoordinateSystemAxis.class,
                "[Coordinate Axis] AS CA INNER JOIN [Coordinate Axis Name] AS CAN" +
                                 " ON CA.COORD_AXIS_NAME_CODE=CAN.COORD_AXIS_NAME_CODE",
                "COORD_AXIS_CODE",
                "COORD_AXIS_NAME"),

        new TableInfo(Datum.class,
                "[Datum]",
                "DATUM_CODE",
                "DATUM_NAME",
                "DATUM_TYPE",
                new Class<?>[] { GeodeticDatum.class, VerticalDatum.class, EngineeringDatum.class},
                new String[]   {"geodetic",          "vertical",          "engineering"}),

        new TableInfo(Ellipsoid.class,
                "[Ellipsoid]",
                "ELLIPSOID_CODE",
                "ELLIPSOID_NAME"),

        new TableInfo(PrimeMeridian.class,
                "[Prime Meridian]",
                "PRIME_MERIDIAN_CODE",
                "PRIME_MERIDIAN_NAME"),

        new TableInfo(CoordinateOperation.class,
                "[Coordinate_Operation]",
                "COORD_OP_CODE",
                "COORD_OP_NAME",
                "COORD_OP_TYPE",
                new Class<?>[] { Projection.class, Conversion.class, Transformation.class},
                new String[]   {"conversion",     "conversion",     "transformation"}),
                // Note: Projection is handle in a special way.

        new TableInfo(OperationMethod.class,
                "[Coordinate_Operation Method]",
                "COORD_OP_METHOD_CODE",
                "COORD_OP_METHOD_NAME"),

        new TableInfo(ParameterDescriptor.class,
                "[Coordinate_Operation Parameter]",
                "PARAMETER_CODE",
                "PARAMETER_NAME"),

        new TableInfo(Unit.class,
                "[Unit of Measure]",
                "UOM_CODE",
                "UNIT_OF_MEAS_NAME")
    };

    ///////////////////////////////////////////////////////////////////////////////
    ////////                                                               ////////
    ////////                    END OF HARD CODED VALUES                   ////////
    ////////                                                               ////////
    ////////    NOTE: 'createFoo(...)' methods may still have hard-coded   ////////
    ////////    values (others than SQL statements) in 'equalsIgnoreCase'  ////////
    ////////    expressions.                                               ////////
    ///////////////////////////////////////////////////////////////////////////////




    /**
     * The name for the transformation accuracy metadata.
     */
    private static final InternationalString TRANSFORMATION_ACCURACY =
            Vocabulary.formatInternational(Vocabulary.Keys.TransformationAccuracy);

    /**
     * The authority for this database. Will be created only when first needed. This authority will contain
     * the database version in the {@linkplain Citation#getEdition() edition} attribute, together with the
     * {@linkplain Citation#getEditionDate() edition date}.
     */
    private Citation authority;

    /**
     * The namespace of EPSG names and codes. This namespace is needed by all {@code createFoo(String)} methods.
     * The {@code EPSGFactory} constructor relies on the {@link #nameFactory} caching mechanism for giving us
     * the same {@code NameSpace} instance than the one used by previous {@code EPSGFactory} instances, if any.
     */
    private final NameSpace namespace;

    /**
     * Last object type returned by {@link #createObject(String)}, or -1 if none.
     * This type is an index in the {@link #TABLES_INFO} array and is strictly for {@link #createObject} internal use.
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
     * <p>Note that this {@code EPSGFactory} instance can not be disposed as long as this map is not empty, since
     * {@link AuthorityCodes} caches some SQL statements and consequently require the {@linkplain #connection} to
     * be open. This is why we use weak references rather than hard ones, in order to know when no
     * {@link AuthorityCodes} are still in use.</p>
     *
     * <p>The {@link CloseableReference#dispose()} method takes care of closing the statements used by the map.
     * The {@link AuthorityCodes} reference in this map is then cleared by the garbage collector.
     * The {@link #canDispose()} method checks if there is any remaining live reference in this map,
     * and returns {@code false} if some are found (thus blocking the call to {@link #dispose()}
     * by the {@link ConcurrentAuthorityFactory} timer).</p>
     */
    private final Map<Class<?>, Reference<AuthorityCodes>> authorityCodes = new HashMap<>();

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
     * Pool of naming systems, used for caching. There is usually few of them (about 15).
     *
     * @see #createProperties(String, String, String, String, boolean)
     */
    private final Map<String,NameSpace> scopes = new HashMap<>();

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
     * and closed by the {@link #dispose()} method, or when this {@code EPSGFactory} instance is garbage collected.
     */
    protected final Connection connection;

    /**
     * Creates a factory using the given connection. The connection will be {@linkplain Connection#close() closed}
     * when this factory will be {@linkplain #dispose() disposed}.
     *
     * @param connection   The connection to the underlying EPSG database.
     * @param nameFactory  The factory to use for creating authority codes as {@link GenericName} instances.
     */
    public EPSGFactory(final Connection connection, final NameFactory nameFactory) {
        super(nameFactory);
        ArgumentChecks.ensureNonNull("connection", connection);
        this.connection = connection;
        this.namespace  = nameFactory.createNameSpace(nameFactory.createLocalName(null, Constants.EPSG), null);
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
        if (authority == null) {
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
addURIs:        for (int i=0; ; i++) {
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
                authority = c;
            }
        }
        return authority;
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
        Reference<AuthorityCodes> reference = authorityCodes.get(type);
        if (reference != null) {
            AuthorityCodes existing = reference.get();
            if (existing != null) {
                return existing;
            }
        }
        Map<String,String> result = Collections.emptyMap();
        for (final TableInfo table : TABLES_INFO) {
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
        for (final TableInfo table : TABLES_INFO) {
            final String text = getCodeMap(table.type).get(primaryKey);
            if (text != null) {
                return (table.nameColumn != null) ? new SimpleInternationalString(text) : null;
            }
        }
        throw noSuchAuthorityCode(IdentifiedObject.class, code);
    }

    /**
     * Returns a prepared statement for the specified name. Most {@link PreparedStatement} creations are performed
     * through this method, except {@link #toPrimaryKey} and {@link #createObject(String)}.
     *
     * @param  key  A key uniquely identifying the caller (e.g. {@code "Ellipsoid"} for {@link #createEllipsoid(String)}).
     * @param  sql  The SQL statement to use if for creating the {@link PreparedStatement} object.
     *              Will be used only if no prepared statement was already created for the specified key.
     * @return The prepared statement.
     * @throws SQLException if the prepared statement can not be created.
     */
    private PreparedStatement prepareStatement(final String key, final String sql) throws SQLException {
        assert Thread.holdsLock(this);
        PreparedStatement stmt = statements.get(key);
        if (stmt == null) {
            stmt = connection.prepareStatement(adaptSQL(sql));
            statements.put(key, stmt);
        }
        // Partial check that the statement is for the right SQL query.
        assert stmt.getParameterMetaData().getParameterCount() == CharSequences.count(sql, '?');
        return stmt;
    }

    /**
     * Makes sure that the last result was non-null.
     * Used for {@code getString(…)}, {@code getDouble(…)} and {@code getInt(…)} methods only.
     */
    private static void ensureNonNull(final ResultSet result, final int columnIndex, final Object code)
            throws SQLException, FactoryDataException
    {
        if (result.wasNull()) {
            final ResultSetMetaData metadata = result.getMetaData();
            final String column = metadata.getColumnName(columnIndex);
            final String table  = metadata.getTableName (columnIndex);
            result.close();
            throw new FactoryDataException(Errors.format(Errors.Keys.NullValueInTable_3, table, column, code));
        }
    }

    /**
     * Same as {@link #getString(ResultSet, int, Object)},
     * but reports the fault on an alternative column if the value is null.
     */
    private static String getString(final ResultSet result, final int columnIndex,
                                    final String    code,   final int columnFault)
            throws SQLException, FactoryDataException
    {
        final String str = result.getString(columnIndex);
        if (result.wasNull()) {
            final ResultSetMetaData metadata = result.getMetaData();
            final String column = metadata.getColumnName(columnFault);
            final String table  = metadata.getTableName (columnFault);
            result.close();
            throw new FactoryDataException(Errors.format(Errors.Keys.NullValueInTable_3, table, column, code));
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
    private static String getString(final ResultSet result, final int columnIndex, final Object code)
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
    private static double getDouble(final ResultSet result, final int columnIndex, final Object code)
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
    private static int getInt(final ResultSet result, final int columnIndex, final Object code)
            throws SQLException, FactoryDataException
    {
        final int value = result.getInt(columnIndex);
        ensureNonNull(result, columnIndex, code);
        return value;
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
    private static ResultSet executeQuery(final PreparedStatement stmt, final String primaryKey)
            throws NoSuchIdentifierException, SQLException
    {
        final int n;
        try {
            n = Integer.parseInt(primaryKey);
        } catch (NumberFormatException e) {
            final NoSuchIdentifierException ne = new NoSuchIdentifierException(Errors.format(
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
    private static ResultSet executeQuery(final PreparedStatement stmt, final String pk1, final String pk2)
            throws NoSuchIdentifierException, SQLException
    {
        final int n1, n2;
        String key = pk1;
        try {
            n1 = Integer.parseInt(      pk1);
            n2 = Integer.parseInt(key = pk2);
        } catch (NumberFormatException e) {
            final NoSuchIdentifierException ne = new NoSuchIdentifierException(Errors.format(
                    Errors.Keys.IllegalIdentifierForCodespace_2, Constants.EPSG, key), key);
            ne.initCause(e);
            throw ne;
        }
        stmt.setInt(1, n1);
        stmt.setInt(2, n2);
        return stmt.executeQuery();
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
    private static <T> T ensureSingleton(final T newValue, final T oldValue, final String code) throws FactoryDataException {
        if (oldValue == null) {
            return newValue;
        }
        if (oldValue.equals(newValue)) {
            return oldValue;
        }
        throw new FactoryDataException(Errors.format(Errors.Keys.DuplicatedIdentifier_1, code));
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
    private Map<String,Object> createProperties(final String table, final String name, String code,
            String remarks, final boolean deprecated) throws SQLException, FactoryException
    {
        properties.clear();
        GenericName gn = null;
        final Citation authority = getAuthority();
        final InternationalString edition = authority.getEdition();
        final String version = (edition != null) ? edition.toString() : null;
        if (name != null) {
            properties.put("name", gn = nameFactory.createLocalName(namespace, name.trim()));
            properties.put(NamedIdentifier.AUTHORITY_KEY, authority);
            properties.put(NamedIdentifier.CODE_KEY,      name.trim());
            properties.put(NamedIdentifier.VERSION_KEY,   version);
            final NamedIdentifier id = new NamedIdentifier(properties);
            properties.clear();
            properties.put(IdentifiedObject.NAME_KEY, id);
        }
        if (code != null) {
            code = code.trim();
            final ImmutableIdentifier identifier;
            if (deprecated) {
                identifier = new DeprecatedCode(authority, Constants.EPSG, code, version, null);
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
         * Search for aliases.
         */
        List<GenericName> alias = null;
        final PreparedStatement stmt;
        stmt = prepareStatement(
                "[Alias]", "SELECT NAMING_SYSTEM_NAME, ALIAS, OBJECT_TABLE_NAME" +
                " FROM [Alias] INNER JOIN [Naming System]" +
                  " ON [Alias].NAMING_SYSTEM_CODE =" +
                " [Naming System].NAMING_SYSTEM_CODE" +
                " WHERE OBJECT_CODE = ?");
        try (ResultSet result = executeQuery(stmt, code)) {
            while (result.next()) {
                String owner = result.getString(3);
                if (owner != null) {
                    /*
                     * We have found an alias for a object having the ID we are looking for, but we need to check if
                     * it is really from the same table since a few different tables have objects with the same ID.
                     */
                    if (owner.startsWith(TABLE_PREFIX)) {
                        owner = owner.substring(TABLE_PREFIX.length());
                    }
                    if (!CharSequences.isAcronymForWords(owner, table)) {
                        continue;
                    }
                }
                final String scope = result.getString(1);
                final String local = getString(result, 2, code);
                final GenericName generic;
                if (scope == null) {
                    generic = nameFactory.createLocalName(null, local);
                } else {
                    NameSpace cached = scopes.get(scope);
                    if (cached == null) {
                        cached = nameFactory.createNameSpace(
                                 nameFactory.createLocalName(null, scope),
                                 Collections.singletonMap("separator", ":"));
                        scopes.put(scope, cached);
                    }
                    generic = nameFactory.createLocalName(cached, local);
                }
                if (alias == null) {
                    alias = new ArrayList<>();
                }
                alias.add(generic);
            }
        }
        if (alias != null) {
            properties.put(IdentifiedObject.ALIAS_KEY, alias.toArray(new GenericName[alias.size()]));
        }
        return properties;
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
     * Logs a warning about an unexpected but non-fatal exception.
     *
     * @param method    The source method.
     * @param exception The exception to log.
     */
    private static void unexpectedException(final String method, final Exception exception) {
        Logging.unexpectedException(Logging.getLogger(Loggers.CRS_FACTORY), EPSGFactory.class, method, exception);
    }
}
