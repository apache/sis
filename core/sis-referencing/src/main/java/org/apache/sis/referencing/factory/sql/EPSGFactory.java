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
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.ConcurrentAuthorityFactory;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.Version;
import org.apache.sis.measure.Units;


/**
 * A geodetic object factory backed by the EPSG database tables.
 * The EPSG database is freely available at <a href="http://www.epsg.org">http://www.epsg.org</a>.
 * Current version of this class requires EPSG database version 6.6 or above.
 *
 * <p>This factory accepts names as well as numerical identifiers.
 * For example <cite>"NTF (Paris) / France I"</cite> and {@code "27581"} both fetch the same object.
 * However, names may be ambiguous since the same name may be used for more than one object.
 * This is the case of <cite>"WGS 84"</cite> for instance.
 * If such an ambiguity is found, an exception will be thrown.
 * This behavior can be changed by overriding the {@link #isPrimaryKey(String)} method.</p>
 *
 * <p>This factory does not cache the result of {@code createFoo(String)} methods.
 * Asking for the same object twice will cause the EPSG database to be queried again.
 * For caching, this factory should be wrapped in {@link ConcurrentAuthorityFactory}.</p>
 *
 * <p>Because the primary distribution format for the EPSG database is MS-Access, this class uses
 * SQL statements formatted for the MS-Access syntax. For usage with an other database software,
 * a dialect-specific subclass must be used.</p>
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
            throw new FactoryException(Errors.format(Errors.Keys.IncompatibleUnit_1, unit), e);
        }
        switch (code) {
            case 8605: parameters.tX = value; break;
            case 8606: parameters.tY = value; break;
            case 8607: parameters.tZ = value; break;
            case 8608: parameters.rX = value; break;
            case 8609: parameters.rY = value; break;
            case 8610: parameters.rZ = value; break;
            case 8611: parameters.dS = value; break;
            default: throw new FactoryException(Errors.format(Errors.Keys.UnexpectedParameter_1, code));
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
     *
     * <div class="note"><b>Note:</b>
     * it is okay to use {@link IdentityHashMap} instead of {@link HashMap} because the keys will always be
     * the exact same object, namely the hard-coded argument given to calls to {@link #prepareStatement} in
     * this class.</div>
     */
    private final Map<String,PreparedStatement> statements = new IdentityHashMap<>();

    /**
     * The set of authority codes for different types. This map is used by the {@link #getAuthorityCodes(Class)}
     * method as a cache for returning the set created in a previous call.
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
    private final Map<Integer,NameSpace> scopes = new HashMap<>();

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
     * @param connection The connection to the underlying EPSG database.
     */
    public EPSGFactory(final Connection connection, final NameFactory nameFactory) {
        super(nameFactory);
        this.connection = connection;
        ArgumentChecks.ensureNonNull("connection", connection);
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
            c.setIdentifiers(Collections.singleton(new DefaultIdentifier("EPSG")));
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
                            description = Messages.formatInternational(Messages.Keys.DataBase_4, "EPSG", version,
                                    metadata.getDatabaseProductName(),
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
                    try {
                        codes.close();
                    } catch (SQLException e) {  // Not a fatal exception for this method since we got the data.
                        unexpectedException("getAuthorityCodes", e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Gets a description of the object corresponding to a code.
     *
     * @param  code Value allocated by authority.
     * @return A description of the object, or {@code null} if the object corresponding to the specified {@code code}
     *         has no description.
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
     * Logs a warning about an unexpected but non-fatal exception.
     *
     * @param method    The source method.
     * @param exception The exception to log.
     */
    private static void unexpectedException(final String method, final Exception exception) {
        Logging.unexpectedException(Logging.getLogger(Loggers.CRS_FACTORY), EPSGFactory.class, method, exception);
    }
}
