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

import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.temporal.Temporal;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.format.MeasurementParseException;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.CoordinateOperations;
import org.apache.sis.referencing.privy.ReferencingFactoryContainer;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.internal.DeferredCoordinateOperation;
import org.apache.sis.referencing.internal.DeprecatedCode;
import org.apache.sis.referencing.internal.EPSGParameterDomain;
import org.apache.sis.referencing.internal.ParameterizedTransformBuilder;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.referencing.internal.SignReversalComment;
import org.apache.sis.referencing.internal.VerticalDatumTypes;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.ServicesForMetadata;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.system.Loggers;
import org.apache.sis.system.Semaphores;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Version;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.privy.URLs;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.sql.privy.SQLUtilities;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.pending.jdk.JDK16;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;
import org.opengis.referencing.ObjectDomain;


/**
 * <i>Data Access Object</i> (DAO) creating geodetic objects from a JDBC connection to an EPSG database.
 * The EPSG database is freely available at <a href="https://epsg.org/">https://epsg.org/</a>.
 * Current version of this class requires EPSG database version 6.6 or above.
 *
 * <h2>Object identifier (code or name)</h2>
 * EPSG codes are numerical identifiers. For example, code 3395 stands for <q>WGS 84 / World Mercator</q>.
 * Coordinate Reference Objects are normally created from their numerical codes, but this factory accepts also names.
 * For example, {@code createProjectedCRS("3395")} and {@code createProjectedCRS("WGS 84 / World Mercator")} both fetch
 * the same object.
 * However, names may be ambiguous since the same name may be used for more than one object.
 * This is the case of <q>WGS 84</q> for instance.
 * If such an ambiguity is found, an exception will be thrown.
 *
 * <h2>Life cycle and caching</h2>
 * {@code EPSGDataAccess} instances should be short-lived since they may hold a significant amount of JDBC resource.
 * {@code EPSGDataAccess} instances are created on the fly by {@link EPSGFactory} and closed after a relatively short
 * {@linkplain EPSGFactory#getTimeout timeout}.
 * In addition {@code EPSGFactory} caches the most recently created objects, which reduce greatly
 * the number of {@code EPSGDataAccess} instantiations (and consequently the number of database accesses)
 * in the common case where only a few EPSG codes are used by an application.
 * {@code EPSGDataAccess.createFoo(String)} methods do not cache by themselves and query the database on every invocation.
 *
 * @author  Yann Cézard (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @author  Matthias Basler
 * @author  Andrea Aime (TOPP)
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 *
 * @see <a href="https://sis.apache.org/tables/CoordinateReferenceSystems.html">List of authority codes</a>
 *
 * @since 0.7
 */
public class EPSGDataAccess extends GeodeticAuthorityFactory implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory, Localized, AutoCloseable
{
    /**
     * The logger for factory operation.
     */
    static final Logger LOGGER = Logger.getLogger(Loggers.CRS_FACTORY);

    /**
     * EPSG codes of parameters containing the EPSG code of another object.
     * Those parameters are integers (stored as {@code double} in the database)
     * without unit (associated to {@link Units#UNITY} in the database).
     */
    private static final int[] EPSG_CODE_PARAMETERS = {
        1048,       // EPSG code for Interpolation CRS
        1062        // EPSG code for "standard" CT
    };

    /**
     * The deprecated ellipsoidal coordinate systems and their replacements. Those coordinate systems are deprecated
     * because they use a unit of measurement which is no longer supported by OGC (for example degree-minute-second).
     * Those replacements can be used only if the ellipsoidal CS is used for the base geographic CRS of a derived or
     * projected CRS, because the units of measurement of the base CRS do not impact the units of measurements of the
     * derived CRS.
     *
     * <p>We perform those replacements for avoiding a "Unit conversion from “DMS” to “°” is non-linear" exception
     * at projected CRS creation time.</p>
     *
     * @see #replaceDeprecatedCS
     */
    @Workaround(library = "EPSG:6401-6420", version = "8.9")        // Deprecated in 2002 but still present in 2016.
    private static final Map<Integer,Integer> DEPRECATED_CS = deprecatedCS();
    static Map<Integer,Integer> deprecatedCS() {
        final var m = new HashMap<Integer,Integer>(24);

        // Ellipsoidal 2D CS. Axes: latitude, longitude. Orientations: north, east. UoM: degree
        Integer replacement = 6422;
        m.put(6402, replacement);
        for (int code = 6405; code <= 6412; code++) {
            m.put(code, replacement);
        }

        // Ellipsoidal 3D CS. Axes: latitude, longitude, ellipsoidal height. Orientations: north, east, up. UoM: degree, degree, metre.
        replacement = 6423;
        m.put(6401, replacement);
        for (int code = 6413; code <= 6420; code++) {
            m.put(code, replacement);
        }
        return m;
    }

    /**
     * The namespace of EPSG names and codes. This namespace is needed by all {@code createFoo(String)} methods.
     * The {@code EPSGDataAccess} constructor relies on the {@link EPSGFactory#nameFactory} caching mechanism
     * for giving us the same {@code NameSpace} instance than the one used by previous {@code EPSGDataAccess}
     * instances, if any.
     */
    private final NameSpace namespace;

    /**
     * The last table in which object name were looked for.
     * This is for internal use by {@link #toPrimaryKeys} only.
     */
    private String lastTableForName;

    /**
     * The calendar instance for creating {@link Date} objects from a year (the "epoch" in datum definition).
     * We use the UTC timezone, which may not be quite accurate. But there is no obvious timezone for "epoch",
     * and the "epoch" is an approximation anyway.
     *
     * @see #getCalendar()
     */
    private Calendar calendar;

    /**
     * A pool of prepared statements. Keys are {@link String} objects related to their originating method
     * (for example "Ellipsoid" for {@link #createEllipsoid(String)}).
     */
    private final Map<String, PreparedStatement> statements = new HashMap<>();

    /**
     * The set of authority codes for different types. This map is used by the {@link #getAuthorityCodes(Class)}
     * method as a cache for returning the set created in a previous call. We do not want this map to exist for
     * a long time anyway.
     *
     * <p>Note that this {@code EPSGDataAccess} instance cannot be closed as long as this map is not empty, since
     * {@link AuthorityCodes} caches some SQL statements and consequently require the {@linkplain #connection}
     * to be open. This is why we use weak references rather than hard ones, in order to know when no
     * {@link AuthorityCodes} are still in use.</p>
     *
     * <p>The {@link CloseableReference#dispose()} method takes care of closing the statements used by the map.
     * The {@link AuthorityCodes} reference in this map is then cleared by the garbage collector.
     * The {@link #canClose()} method checks if there is any remaining live reference in this map,
     * and returns {@code false} if some are found (thus blocking the call to {@link #close()}
     * by the {@link org.apache.sis.referencing.factory.ConcurrentAuthorityFactory} timer).</p>
     */
    private final Map<Class<?>, CloseableReference> authorityCodes = new HashMap<>();

    /**
     * Cache for axis names. This service is not provided by {@code ConcurrentAuthorityFactory}
     * because {@link AxisName} objects are specific to the <abbr>EPSG</abbr> authority factory.
     *
     * @see #getAxisName(Integer)
     */
    private final Map<Integer, AxisName> axisNames = new HashMap<>();

    /**
     * Cache for realization methods. This service is not provided by {@code ConcurrentAuthorityFactory}
     * because the realization method table is specific to the <abbr>EPSG</abbr> authority factory.
     *
     * @see #getRealizationMethod(Integer)
     */
    private final Map<Integer, RealizationMethod> realizationMethods = new HashMap<>();

    /**
     * Cache of naming systems other than EPSG. There is usually few of them (at most 15).
     * This is used for aliases.
     *
     * @see #createProperties(String, Integer, String, CharSequence, String, String, CharSequence, boolean)
     */
    private final Map<String, NameSpace> namingSystems = new HashMap<>();

    /**
     * The properties to be given the objects to construct.
     * Reused every time {@code createProperties(…)} is invoked.
     */
    private final Map<String, Object> properties = new HashMap<>();

    /**
     * A safety guard for preventing never-ending loops in recursive calls to some {@code createFoo(String)} methods.
     * Recursion may happen while creating Bursa-Wolf parameters, projected CRS if the database has erroneous data,
     * compound CRS if there is cycles, or coordinate operations.
     *
     * <h4>Example</h4>
     * {@link #createDatum(String)} invokes {@link #createBursaWolfParameters(PrimeMeridian, Integer)}, which creates
     * a target datum. The target datum could have its own Bursa-Wolf parameters, with one of them pointing again to
     * the source datum.
     *
     * Keys are EPSG codes and values are the type of object being constructed (but those values are not yet used).
     */
    private final Map<Integer, Class<?>> safetyGuard = new HashMap<>();

    /**
     * {@code true} for disabling the logging of warnings when this factory creates deprecated objects.
     * This flag should be always {@code false}, except during {@link EPSGCodeFinder#find(IdentifiedObject)}
     * execution since that method may temporarily creates deprecated objects which are later discarded.
     * May also be {@code false} when creating base CRS of deprecated projected or derived CRS.
     */
    transient boolean quiet;

    /**
     * {@code true} if {@link #createCoordinateReferenceSystem(String)} is allowed to replace deprecated
     * coordinate system at CRS creation time. This flag should be set to {@code true} only when creating
     * the base CRS of a projected or derived CRS.
     *
     * @see #DEPRECATED_CS
     */
    private transient boolean replaceDeprecatedCS;

    /**
     * The {@code ConcurrentAuthorityFactory} that created this Data Access Object (DAO).
     * The owner supplies caching for all {@code createFoo(String)} methods.
     */
    protected final EPSGFactory owner;

    /**
     * The connection to the EPSG database. This connection is specified at {@linkplain #EPSGDataAccess construction time}
     * and closed by the {@link #close()} method.
     *
     * @see #close()
     */
    protected final Connection connection;

    /**
     * The translator from the <abbr>SQL</abbr> statements hard-coded in this class to
     * <abbr>SQL</abbr> statements compatible with the actual <abbr>EPSG</abbr> database.
     * This translator may also change the schema and table names used in the queries
     * in the actual database uses different names.
     */
    protected final SQLTranslator translator;

    /**
     * Creates a factory using the given connection. The connection will be {@linkplain Connection#close() closed}
     * when this factory will be {@linkplain #close() closed}.
     *
     * <h4>API design note</h4>
     * This constructor is protected because {@code EPSGDataAccess} instances should not be created as standalone factories.
     * This constructor is for allowing definition of custom {@code EPSGDataAccess} subclasses, which are then instantiated
     * by the {@link EPSGFactory#newDataAccess(Connection, SQLTranslator)} method of a corresponding custom
     * {@code EPSGFactory} subclass.
     *
     * @param owner       the {@code EPSGFactory} which is creating this Data Access Object (DAO).
     * @param connection  the connection to the underlying EPSG database.
     * @param translator  translator from the <abbr>SQL</abbr> statements hard-coded in this class to
     *                    <abbr>SQL</abbr> statements compatible with the actual <abbr>EPSG</abbr> database.
     * @throws SQLException if an error occurred with the database connection.
     *
     * @see EPSGFactory#newDataAccess(Connection, SQLTranslator)
     */
    protected EPSGDataAccess(final EPSGFactory owner, final Connection connection, final SQLTranslator translator)
            throws SQLException
    {
        this.owner      = owner;
        this.connection = Objects.requireNonNull(connection);
        this.translator = Objects.requireNonNull(translator);
        this.namespace  = owner.nameFactory.createNameSpace(
                          owner.nameFactory.createLocalName(null, Constants.IOGP), null);
        connection.setCatalog(translator.getCatalog());
        connection.setSchema (translator.getSchema());
    }

    /**
     * Returns the locale used by this factory for producing error messages.
     * This locale does not change the way data are read from the EPSG database.
     *
     * @return the locale for error messages.
     */
    @Override
    public Locale getLocale() {
        return owner.getLocale();
    }

    /**
     * Returns the calendar to use for reading dates in the database.
     */
    @SuppressWarnings("ReturnOfDateField")
    private Calendar getCalendar() {
        if (calendar == null) {
            calendar = Calendar.getInstance(TimeZone.getTimeZone(Constants.UTC), Locale.CANADA);
            // Canada locale is closer to ISO than US.
        }
        calendar.clear();
        return calendar;
    }

    /**
     * Returns the authority for this EPSG dataset. The returned citation contains the database version
     * in the {@linkplain Citation#getEdition() edition} attribute, together with date of last update in
     * the {@linkplain Citation#getEditionDate() edition date}.
     * Example (the exact content will vary with Apache SIS versions, JDBC driver and EPSG dataset versions):
     *
     * <pre class="text">
     *   Citation
     *   ├─ Title ……………………………………………………… EPSG Geodetic Parameter Dataset
     *   ├─ Identifier ………………………………………… EPSG
     *   ├─ Online resource (1 of 2)
     *   │  ├─ Linkage ………………………………………… https://epsg.org/
     *   │  └─ Function ……………………………………… Browse
     *   └─ Online resource (2 of 2)
     *      ├─ Linkage ………………………………………… jdbc:derby:/my/path/to/SIS_DATA/Databases/SpatialMetadata
     *      ├─ Description ……………………………… EPSG dataset version 9.1 on “Apache Derby Embedded JDBC Driver” version 10.14.
     *      └─ Function ……………………………………… Connection</pre>
     */
    @Override
    public synchronized Citation getAuthority() {
        /*
         * We do not cache this citation because the caching service is already provided by ConcurrentAuthorityFactory.
         */
        final var c = new DefaultCitation("EPSG Geodetic Parameter Dataset");
        c.setIdentifiers(Set.of(new ImmutableIdentifier(null, null, Constants.EPSG)));
        try {
            /*
             * Get the most recent version number from the history table. We get the date in local timezone
             * instead of UTC because the date is for information purpose only, and the local timezone is
             * more likely to be shown nicely (without artificial hours) to the user.
             */
            final String query = translator.apply("SELECT VERSION_NUMBER, VERSION_DATE FROM \"Version History\"" +
                                                  " ORDER BY VERSION_DATE DESC, VERSION_HISTORY_CODE DESC");
            String version = null;
            try (Statement stmt = connection.createStatement();
                 ResultSet result = stmt.executeQuery(query))
            {
                while (result.next()) {
                    version = getOptionalString(result, 1);
                    final Date date = result.getDate(2);                            // Local timezone.
                    if (version != null && date != null) {                          // Paranoiac check.
                        c.setEdition(new SimpleInternationalString(version));
                        c.setEditionDate(LocalDate.ofEpochDay(date.getTime() / Constants.MILLISECONDS_PER_DAY));
                        break;
                    }
                }
            }
            /*
             * Add some hard-coded links to EPSG resources, and finally add the JDBC driver name and version number.
             * The last OnlineResource looks like:
             *
             *    Linkage:      jdbc:derby:/my/path/to/SIS_DATA/Databases/SpatialMetadata
             *    Function:     Connection
             *    Description:  EPSG dataset version 9.1 on “Apache Derby Embedded JDBC Driver” version 10.14.
             *
             * TODO: A future version should use Citations.EPSG as a template.
             */
            final DatabaseMetaData metadata  = connection.getMetaData();
addURIs:    for (int i=0; ; i++) {
                String url;
                OnLineFunction function;
                InternationalString description = null;
                switch (i) {
                    case 0: url = URLs.EPSG; function = OnLineFunction.SEARCH; break;
                    case 1: url = URLs.EPSG; function = OnLineFunction.DOWNLOAD; break;
                    case 2: {
                        url = SQLUtilities.getSimplifiedURL(metadata);
                        function = OnLineFunction.valueOf(ServicesForMetadata.CONNECTION);
                        description = Resources.formatInternational(Resources.Keys.GeodeticDataBase_4,
                                Constants.EPSG, version, metadata.getDatabaseProductName(),
                                Version.valueOf(metadata.getDatabaseMajorVersion(),
                                                metadata.getDatabaseMinorVersion()));
                        break;
                    }
                    default: break addURIs;     // Finished adding all URIs.
                }
                final var r = new DefaultOnlineResource();
                try {
                    r.setLinkage(new URI(url));
                } catch (URISyntaxException exception) {
                    // May happen if there is spaces in the URI.
                    Logging.recoverableException(LOGGER, EPSGDataAccess.class, "getAuthority", exception);
                }
                r.setFunction(function);
                r.setDescription(description);
                c.getOnlineResources().add(r);
            }
        } catch (SQLException exception) {
            unexpectedException("getAuthority", exception);
        } finally {
            c.transitionTo(DefaultCitation.State.FINAL);
        }
        return c;
    }

    /**
     * Returns the set of authority codes of the given type.
     * This returned set may keep a connection to the EPSG database,
     * so the set can execute efficiently idioms like the following one:
     *
     * {@snippet lang="java" :
     *     getAuthorityCodes(type).containsAll(others);
     *     }
     *
     * The returned set should not be referenced for a long time, as it may prevent this factory to release
     * JDBC resources. If the set of codes is needed for a long time, their values should be copied in another
     * collection object.
     *
     * <h4>Handling of deprecated objects</h4>
     * The collection returned by this method gives an enumeration of EPSG codes for valid objects only.
     * The EPSG codes of deprecated objects are not included in iterations, computation of {@code Set.size()} value,
     * {@code Set.toString()} result, <i>etc.</i> with one exception:
     * a call to {@code Set.contains(…)} will return {@code true} if the given identifier exists
     * for a deprecated object, even if that identifier does not show up in iterations.
     * In other words, the returned collection behaves as if deprecated codes were included in the set but invisible.
     *
     * @param  type  the spatial reference objects type (may be {@code Object.class}).
     * @return the set of authority codes for spatial reference objects of the given type (may be an empty set).
     * @throws FactoryException if access to the underlying database failed.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        try {
            if (connection.isClosed()) {
                throw new FactoryException(error().getString(Errors.Keys.ConnectionClosed));
            }
            return getCodeMap(type).keySet();
        } catch (SQLException exception) {
            throw new FactoryException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Returns a map of EPSG authority codes as keys and object names as values.
     * The cautions documented in {@link #getAuthorityCodes(Class)} apply also to this map.
     *
     * @param  type  the spatial reference objects type (may be {@code Object.class}).
     * @return the map of authority codes associated to their names. May be an empty map.
     * @throws FactoryException if access to the underlying database failed.
     *
     * @see #getAuthorityCodes(Class)
     * @see #getDescriptionText(Class, String)
     */
    private synchronized Map<String,String> getCodeMap(final Class<?> type) throws SQLException {
        CloseableReference reference = authorityCodes.get(type);
        if (reference != null) {
            AuthorityCodes existing = reference.get();
            if (existing != null) {
                return existing;
            }
        }
        Map<String,String> result = Map.of();
        for (final TableInfo table : TableInfo.EPSG) {
            /*
             * We test `isAssignableFrom` in the two ways for catching the following use cases:
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
                        reference = null;           // The weak reference is no longer valid.
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
     * Returns an empty set since this data access class expects no namespace.
     * Code shall be given to {@code createFoo(String)} methods directly, without {@code "EPSG:"} prefix.
     *
     * @return empty set.
     */
    @Override
    public Set<String> getCodeSpaces() {
        return Set.of();
    }

    /**
     * Gets a description of the object corresponding to a code.
     * This method returns the object name in a lightweight manner, without creating the full {@link IdentifiedObject}.
     *
     * @param  type  the type of object for which to get a description.
     * @param  code  value allocated by authority.
     * @return the object name, or empty if none.
     * @throws FactoryException if the query failed for some other reason.
     *
     * @since 1.5
     */
    @Override
    public Optional<InternationalString> getDescriptionText(final Class<? extends IdentifiedObject> type, final String code)
            throws FactoryException
    {
        try {
            for (final TableInfo table : TableInfo.EPSG) {
                if (table.nameColumn != null && type.isAssignableFrom(table.type)) {
                    final String text = getCodeMap(table.type).get(code);
                    if (text != null) {
                        return Optional.of(new SimpleInternationalString(text));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new FactoryException(exception.getLocalizedMessage(), exception);
        } catch (BackingStoreException exception) {       // Cause is SQLException.
            throw new FactoryException(exception.getLocalizedMessage(), exception.getCause());
        }
        return Optional.empty();
    }

    /**
     * Returns {@code true} if the specified code may be a primary key in some tables.
     * This method does not need to check any entry in the database.
     * It should just check from the syntax if the code looks like a valid EPSG identifier.
     *
     * <p>When this method returns {@code false}, {@code createFoo(String)} methods
     * may look for the code in the name column instead of the primary key column.
     * This allows to accept the <q>WGS 84 / World Mercator</q> string (for example)
     * in addition to the {@code "3395"} primary key. Both string values should fetch the same object.</p>
     *
     * <p>If this method returns {@code true}, then this factory does not search for matching names.
     * In such case, an appropriate exception will be thrown in {@code createFoo(String)} methods
     * if the code is not found in the primary key column.</p>
     *
     * <h4>Default implementation</h4>
     * The default implementation returns {@code true} if all characters are decimal digits 0 to 9.
     * Currently, this default implementation cannot be overridden. But we may allow that in a future
     * version if it appears to be useful.
     *
     * @param  code  the code the inspect.
     * @return {@code true} if the code is probably a primary key.
     * @throws FactoryException if an unexpected error occurred while inspecting the code.
     */
    private static boolean isPrimaryKey(final String code) throws FactoryException {
        int i = code.length();
        if (i == 0) {
            return false;
        }
        do {
            final char c = code.charAt(--i);
            if (c < '0' || c > '9') {
                return false;
            }
        } while (i != 0);
        return true;
    }

    /**
     * Converts EPSG codes or EPSG names to the numerical identifiers (the primary keys).
     * This method can be seen as the converse of above {@link #getDescriptionText(Class, String)} method.
     *
     * @param  table       the table where the code should appears, or {@code null} if {@code codeColumn} is null.
     * @param  codeColumn  the column name for the codes, or {@code null} if none.
     * @param  nameColumn  the column name for the names, or {@code null} if none.
     * @param  codes       the codes or names to convert to primary keys, as an array of length 1 or 2.
     * @return the numerical identifiers (i.e. the table primary key values).
     * @throws SQLException if an error occurred while querying the database.
     */
    private int[] toPrimaryKeys(final String table, final String codeColumn, final String nameColumn, final String... codes)
            throws SQLException, FactoryException
    {
        String actualTable = null;
        final int[] primaryKeys = new int[codes.length];
codes:  for (int i=0; i<codes.length; i++) {
            final String code = codes[i];
            if (codeColumn != null && nameColumn != null && !isPrimaryKey(code)) {
                /*
                 * The given string is not a numerical code. Search the value in the database.
                 * We search first in the primary table. If no name is not found there, then we
                 * will search in the aliases table as a fallback.
                 */
                if (actualTable == null) {
                    actualTable = translator.toActualTableName(table);
                }
                final String pattern = SQLUtilities.toLikePattern(code, false);
                Integer resolved = null;
                boolean alias = false;
                do {
                    PreparedStatement stmt;
                    if (alias) {
                        stmt = prepareStatement("AliasKey",
                                "SELECT OBJECT_CODE, ALIAS FROM \"Alias\"" +
                                " WHERE OBJECT_TABLE_NAME=? AND ALIAS LIKE ?");
                        stmt.setString(1, actualTable);
                        stmt.setString(2, pattern);
                    } else {
                        /*
                         * The SQL query for searching in the primary table is a little bit more complicated than the query for
                         * searching in the aliass table. If a prepared statement is already available, reuse it providing that
                         * it was created for the current table. Otherwise we will create a new statement here.
                         */
                        final String KEY = "PrimaryKey";
                        stmt = statements.get(KEY);
                        if (stmt != null) {
                            if (!table.equals(lastTableForName)) {
                                statements.remove(KEY);
                                stmt.close();
                                stmt = null;
                                lastTableForName = null;
                            }
                        }
                        if (stmt == null) {
                            stmt = connection.prepareStatement(translator.apply(
                                    "SELECT " + codeColumn + ", " + nameColumn +
                                    " FROM \"" + actualTable + "\" WHERE " + nameColumn + " LIKE ?"));
                            statements.put(KEY, stmt);
                            lastTableForName = table;
                        }
                        stmt.setString(1, pattern);
                    }
                    try (ResultSet result = stmt.executeQuery()) {
                        while (result.next()) {
                            if (SQLUtilities.filterFalsePositive(code, result.getString(2))) {
                                resolved = ensureSingleton(getOptionalInteger(result, 1), resolved, code);
                            }
                        }
                    }
                    if (resolved != null) {
                        primaryKeys[i] = resolved;
                        continue codes;
                    }
                } while ((alias = !alias) == true);
            }
            /*
             * At this point, `identifier` should be the primary key. It may still be a non-numerical string
             * if the above code did not found a match in the name column or in the alias table.
             */
            try {
                primaryKeys[i] = Integer.parseInt(code);
            } catch (NumberFormatException e) {
                throw (NoSuchAuthorityCodeException) new NoSuchAuthorityCodeException(error().getString(
                        Errors.Keys.IllegalIdentifierForCodespace_2, Constants.EPSG, code), Constants.EPSG, code).initCause(e);
            }
        }
        return primaryKeys;
    }

    /**
     * Creates a statement and executes for the given codes. The first code value is assigned to parameter #1,
     * the second code value (if any) is assigned to parameter #2, <i>etc</i>. If a given code is not a
     * {@linkplain #isPrimaryKey primary key}, then this method assumes that the code is the object name
     * and will search for its primary key value.
     *
     * @param  table       the table where the code should appears.
     * @param  codeColumn  the column name for the codes, or {@code null} if none.
     * @param  nameColumn  the column name for the names, or {@code null} if none.
     * @param  sql         the SQL statement to use for creating the {@link PreparedStatement} object.
     *                     Will be used only if no prepared statement was already created for the given code.
     * @param  codes       the codes of the object to create, as an array of length 1 or 2.
     * @return the result of the query.
     * @throws SQLException if an error occurred while querying the database.
     */
    private ResultSet executeQuery(final String table, final String codeColumn, final String nameColumn,
            final String sql, final String... codes) throws SQLException, FactoryException
    {
        assert Thread.holdsLock(this);
        assert sql.contains('"' + table + '"') : table;
        assert (codeColumn == null) || sql.contains(codeColumn) || table.equals("Area") : codeColumn;
        assert (nameColumn == null) || sql.contains(nameColumn) || table.equals("Area") : nameColumn;
        return executeQuery(table, sql, toPrimaryKeys(table, codeColumn, nameColumn, codes));
    }

    /**
     * Creates a statement and executes for the given codes. The first code value is assigned to parameter #1,
     * the second code value (if any) is assigned to parameter #2, <i>etc</i>.
     *
     * @param  table  a key uniquely identifying the caller (e.g. {@code "Ellipsoid"} for {@link #createEllipsoid(String)}).
     * @param  sql    the SQL statement to use for creating the {@link PreparedStatement} object.
     *                Will be used only if no prepared statement was already created for the specified key.
     * @param  codes  the codes of the object to create, as an array of length 1 or 2.
     * @return the result of the query.
     * @throws SQLException if an error occurred while querying the database.
     */
    private ResultSet executeQuery(final String table, final String sql, final int... codes) throws SQLException {
        assert Thread.holdsLock(this);
        assert CharSequences.count(sql, '?') == codes.length;
        PreparedStatement stmt = prepareStatement(table, sql);
        // Partial check that the statement is for the right SQL query.
        assert stmt.getParameterMetaData().getParameterCount() == codes.length;
        for (int i=0; i<codes.length; i++) {
            stmt.setInt(i+1, codes[i]);
        }
        return stmt.executeQuery();
    }

    /**
     * Executes a query of the form {@code "SELECT … FROM Alias WHERE OBJECT_TABLE_NAME=? AND OBJECT_CODE=?"}.
     * The first argument shall be the name of a database table.
     *
     * @param  key    a key uniquely identifying the caller.
     * @param  sql    the SQL statement to use for creating the {@link PreparedStatement} object.
     * @param  table  the table to set in the first parameter.
     * @param  code   the object code to set in the second parameter.
     * @return the result of the query.
     * @throws SQLException if an error occurred while querying the database.
     */
    private ResultSet executeMetadataQuery(final String key, final String sql, final String table, final int code) throws SQLException {
        assert Thread.holdsLock(this);
        assert CharSequences.count(sql, '?') == 2;
        PreparedStatement stmt = prepareStatement(key, sql);
        assert stmt.getParameterMetaData().getParameterCount() == 2;
        stmt.setString(1, table);
        stmt.setInt(2, code);
        return stmt.executeQuery();
    }

    /**
     * Returns the cached statement or create a new one for the given table.
     * The {@code table} argument shall be a key uniquely identifying the caller.
     * The {@code sql} argument is used for preparing a new statement if no cached instance exists.
     */
    private PreparedStatement prepareStatement(final String table, final String sql) throws SQLException {
        PreparedStatement stmt = statements.get(table);
        if (stmt == null) {
            stmt = connection.prepareStatement(translator.apply(sql));
            statements.put(table, stmt);
        }
        return stmt;
    }

    /**
     * Gets the value from the specified {@link ResultSet}, or {@code null} if none.
     *
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @return the string at the specified column, or {@code null}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private static String getOptionalString(final ResultSet result, final int columnIndex) throws SQLException {
        final String value = Strings.trimOrNull(result.getString(columnIndex));
        return (value == null) || result.wasNull() ? null : value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}, or {@code NaN} if none.
     *
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @return the number at the specified column, or {@code NaN}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private static double getOptionalDouble(final ResultSet result, final int columnIndex) throws SQLException {
        final double value = result.getDouble(columnIndex);
        return result.wasNull() ? Double.NaN : value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}, or {@code null} if none.
     *
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @return the integer at the specified column, or {@code null}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private static Integer getOptionalInteger(final ResultSet result, final int columnIndex) throws SQLException {
        final int value = result.getInt(columnIndex);
        return result.wasNull() ? null : value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}, or {@code false} if none.
     * The EPSG database stores boolean values as integers instead of using the SQL type.
     *
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @return the boolean at the specified column, or {@code null}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private boolean getOptionalBoolean(final ResultSet result, final int columnIndex) throws SQLException {
        return translator.useBoolean() ? result.getBoolean(columnIndex) : (result.getInt(columnIndex) != 0);
    }

    /**
     * Formats an error message for an unexpected null value.
     */
    @SuppressWarnings("ConvertToTryWithResources")
    private String nullValue(final ResultSet result, final int columnIndex, final Comparable<?> code) throws SQLException {
        final ResultSetMetaData metadata = result.getMetaData();
        final String column = metadata.getColumnName(columnIndex);
        final String table  = metadata.getTableName (columnIndex);
        result.close();     // Only an optimization. The actual try-with-resource is done by the caller.
        return error().getString(Errors.Keys.NullValueInTable_3, table, column, code);
    }

    /**
     * Same as {@link #getString(Comparable, ResultSet, int)},
     * but reports the fault on an alternative column if the value is null.
     */
    private String getString(final String code, final ResultSet result, final int columnIndex, final int columnFault)
            throws SQLException, FactoryDataException
    {
        final String value = Strings.trimOrNull(result.getString(columnIndex));
        if (value == null || result.wasNull()) {
            throw new FactoryDataException(nullValue(result, columnFault, code));
        }
        return value;
    }

    /**
     * Gets the string from the specified {@link ResultSet}.
     * The string is required to be non-null. A null string will throw an exception.
     *
     * @param  code         the identifier of the record where the string was found.
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @return the string at the specified column.
     * @throws SQLException if an error occurred while querying the database.
     * @throws FactoryDataException if a null value was found.
     */
    private String getString(final Comparable<?> code, final ResultSet result, final int columnIndex)
            throws SQLException, FactoryDataException
    {
        final String value = Strings.trimOrNull(result.getString(columnIndex));
        if (value == null || result.wasNull()) {
            throw new FactoryDataException(nullValue(result, columnIndex, code));
        }
        return value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}.
     * The value is required to be non-null. A null value (i.e. blank) will throw an exception.
     *
     * @param  code         the identifier of the record where the double was found.
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @return the double at the specified column.
     * @throws SQLException if an error occurred while querying the database.
     * @throws FactoryDataException if a null value was found.
     */
    private double getDouble(final Comparable<?> code, final ResultSet result, final int columnIndex)
            throws SQLException, FactoryDataException
    {
        final double value = result.getDouble(columnIndex);
        if (Double.isNaN(value) || result.wasNull()) {
            throw new FactoryDataException(nullValue(result, columnIndex, code));
        }
        return value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}.
     * The value is required to be non-null. A null value (i.e. blank) will throw an exception.
     *
     * <p>We return the value as the {@code Integer} wrapper instead of the {@code int} primitive type
     * because the caller will often need that value as an object (for use as key in {@link HashMap}, etc.).</p>
     *
     * @param  code         the identifier of the record where the integer was found.
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @return the integer at the specified column.
     * @throws SQLException if an error occurred while querying the database.
     * @throws FactoryDataException if a null value was found.
     */
    private Integer getInteger(final Comparable<?> code, final ResultSet result, final int columnIndex)
            throws SQLException, FactoryDataException
    {
        final int value = result.getInt(columnIndex);
        if (result.wasNull()) {
            throw new FactoryDataException(nullValue(result, columnIndex, code));
        }
        return value;
    }

    /**
     * Makes sure that an object constructed from the database is not incoherent.
     * If the code supplied to a {@code createFoo(String)} method exists in the database,
     * then we should find only one record. However, we will do a paranoiac check and verify if there is
     * more records, using a {@code while (results.next())} loop instead of {@code if (results.next())}.
     * This method is invoked in the loop for making sure that, if there is more than one record
     * (which should never happen), at least they have identical content.
     *
     * @param  newValue  the newly constructed object.
     * @param  oldValue  the object previously constructed, or {@code null} if none.
     * @param  code      the EPSG code (for formatting error message).
     * @throws FactoryDataException if a duplication has been detected.
     */
    private <T> T ensureSingleton(final T newValue, final T oldValue, final Comparable<?> code) throws FactoryDataException {
        if (oldValue == null) {
            return newValue;
        }
        if (oldValue.equals(newValue)) {
            return oldValue;
        }
        throw new FactoryDataException(error().getString(Errors.Keys.DuplicatedIdentifier_1, code));
    }

    /**
     * Ensures that this factory is not already building an object of the given code.
     * This method shall be followed by a {@code try ... finally} block like below:
     *
     * {@snippet lang="java" :
     *     ensureNoCycle(type, code);
     *     try {
     *         ...
     *     } finally {
     *         endOfRecursive(type, code);
     *     }
     *     }
     */
    private void ensureNoCycle(final Class<?> type, final Integer code) throws FactoryException {
        if (safetyGuard.putIfAbsent(code, type) != null) {
            throw new FactoryException(resources().getString(Resources.Keys.RecursiveCreateCallForCode_2, type, code));
        }
    }

    /**
     * Invoked after the block protected against infinite recursion.
     */
    private void endOfRecursion(final Class<?> type, final Integer code) throws FactoryException {
        if (safetyGuard.remove(code) != type) {
            throw new FactoryException(String.valueOf(code));   // Would be an EPSGDataAccess bug if it happen.
        }
    }

    /**
     * Logs a warning saying that the given code is deprecated and returns the code of the proposed replacement.
     *
     * @param  table   the table of the deprecated code.
     * @param  code    the deprecated code.
     * @return the proposed replacement (may be the "(none)" text).
     */
    private String getSupersession(final String table, final Integer code, final Locale locale) throws SQLException {
        String reason = null;
        Object replacedBy = null;
        try (ResultSet result = executeMetadataQuery("Deprecation",
                "SELECT DEPRECATION_REASON, REPLACED_BY FROM \"Deprecation\"" +
                " WHERE OBJECT_TABLE_NAME=? AND OBJECT_CODE=?",
                translator.toActualTableName(table), code))
        {
            while (result.next()) {
                reason     = getOptionalString (result, 1);
                replacedBy = getOptionalInteger(result, 2);
                if (replacedBy != null) break;                  // Prefer the first record providing a replacement.
            }
        }
        if (replacedBy == null) {
            replacedBy = '(' + Vocabulary.forLocale(locale).getString(Vocabulary.Keys.None).toLowerCase(locale) + ')';
        } else {
            replacedBy = replacedBy.toString();
        }
        /*
         * Try to infer the method name from the table name. For example, if the deprecated code was found in
         * the "Coordinate Reference System" table, then we declare `createCoordinateReferenceSystem(String)`
         * as the source of the log message.
         */
        String method = "create";
        for (final TableInfo info : TableInfo.EPSG) {
            if (info.tableMatches(table)) {
                method += info.type.getSimpleName();
                break;
            }
        }
        if (!quiet) {
            LogRecord record = Resources.forLocale(locale).createLogRecord(
                    Level.WARNING,
                    Resources.Keys.DeprecatedCode_3,
                    Constants.EPSG + Constants.DEFAULT_SEPARATOR + code,
                    replacedBy,
                    reason);
            Logging.completeAndLog(LOGGER, EPSGDataAccess.class, method, record);
        }
        return (String) replacedBy;
    }

    /**
     * Returns the name and aliases for the {@link IdentifiedObject} to construct.
     *
     * @param  table       the table on which a query has been executed.
     * @param  code        the EPSG code of the object to construct.
     * @param  name        the name for the {@link IdentifiedObject} to construct.
     * @param  description a description associated with the name, or {@code null} if none.
     * @param  domainCode  the code for the domain of validity, or {@code null} if none.
     * @param  scope       the scope, or {@code null} if none.
     * @param  remarks     remarks as a {@link String} or {@link InternationalString}, or {@code null} if none.
     * @param  deprecated  {@code true} if the object to create is deprecated.
     * @return the name together with a set of properties.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<String,Object> createProperties(final String       table,
                                                final Integer      code,
                                                      String       name,        // May be replaced by an alias.
                                                final CharSequence description,
                                                final String       domainCode,
                                                      String       scope,       // May replace "?" by text.
                                                final CharSequence remarks,
                                                final boolean      deprecated)
            throws SQLException, FactoryException
    {
        /*
         * Search for aliases. Note that searching for the object code is not sufficient. We also need to check if the
         * record is really from the table we are looking for since different tables may have objects with the same ID.
         *
         * Some aliases are identical to the name except that some letters are replaced by their accented letters.
         * For example, "Reseau Geodesique Francais" → "Réseau Géodésique Français". If we find such alias, replace
         * the name by the alias so we have proper display in user interface. Notes:
         *
         *   - WKT formatting will still be compliant with ISO 19162 because the WKT formatter replaces accented
         *     letters by ASCII ones.
         *   - We do not perform this replacement directly in our EPSG database because ASCII letters are more
         *     convenient for implementing accent-insensitive searches.
         */
        final var aliases = new ArrayList<GenericName>();
        try (ResultSet result = executeMetadataQuery("Alias",
                "SELECT NAMING_SYSTEM_NAME, ALIAS" +
                " FROM \"Alias\" INNER JOIN \"Naming System\"" +
                  " ON \"Alias\".NAMING_SYSTEM_CODE =" +
                " \"Naming System\".NAMING_SYSTEM_CODE" +
                " WHERE OBJECT_TABLE_NAME=? AND OBJECT_CODE=?",
                translator.toActualTableName(table), code))
        {
            while (result.next()) {
                final String naming = getOptionalString(result, 1);
                final String alias  = getString(code,   result, 2);
                NameSpace ns = null;
                if (naming != null) {
                    ns = namingSystems.get(naming);
                    if (ns == null) {
                        ns = owner.nameFactory.createNameSpace(owner.nameFactory.createLocalName(null, naming), null);
                        namingSystems.put(naming, ns);
                    }
                }
                if (CharSequences.toASCII(alias).toString().equals(name)) {
                    name = alias;   // Same name but with accented letters.
                } else {
                    aliases.add(owner.nameFactory.createLocalName(ns, alias));
                }
            }
        }
        /*
         * At this point we can fill the properties map.
         */
        properties.clear();
        GenericName gn = null;
        final Locale locale = getLocale();
        final Citation authority = owner.getAuthority();
        final InternationalString edition = authority.getEdition();
        final String version = (edition != null) ? edition.toString() : null;
        if (name != null) {
            gn = owner.nameFactory.createGenericName(namespace, Constants.EPSG, name);
            properties.put("name", gn);
            properties.put(NamedIdentifier.CODE_KEY,        name);
            properties.put(NamedIdentifier.VERSION_KEY,     version);
            properties.put(NamedIdentifier.AUTHORITY_KEY,   authority);
            properties.put(NamedIdentifier.DESCRIPTION_KEY, description);
            properties.put(AbstractIdentifiedObject.LOCALE_KEY, locale);
            final var id = new NamedIdentifier(properties);
            properties.clear();
            properties.put(IdentifiedObject.NAME_KEY, id);
        }
        if (!aliases.isEmpty()) {
            properties.put(IdentifiedObject.ALIAS_KEY, aliases.toArray(GenericName[]::new));
        }
        if (code != null) {
            final String codeString = code.toString();
            final ImmutableIdentifier identifier;
            if (deprecated) {
                final String replacedBy = getSupersession(table, code, locale);
                identifier = new DeprecatedCode(authority, Constants.EPSG, codeString, version,
                        Character.isDigit(replacedBy.charAt(0)) ? replacedBy : null,
                        Vocabulary.formatInternational(Vocabulary.Keys.SupersededBy_1, replacedBy));
                properties.put(AbstractIdentifiedObject.DEPRECATED_KEY, Boolean.TRUE);
            } else {
                identifier = new ImmutableIdentifier(authority, Constants.EPSG, codeString, version,
                                    (gn != null) ? gn.toInternationalString() : null);
            }
            properties.put(IdentifiedObject.IDENTIFIERS_KEY, identifier);
        }
        properties.put(IdentifiedObject.REMARKS_KEY, remarks);
        properties.put(AbstractIdentifiedObject.LOCALE_KEY, locale);
        properties.put(ReferencingFactoryContainer.MT_FACTORY, owner.mtFactory);
        if ("?".equals(scope)) {                // EPSG sometimes uses this value for unspecified scope.
            scope = null;
        }
        if (domainCode != null) {
            properties.put(ObjectDomain.DOMAIN_OF_VALIDITY_KEY, owner.createExtent(domainCode));
        }
        properties.put(ObjectDomain.SCOPE_KEY, scope);
        return properties;
    }

    /**
     * Returns an arbitrary object from a code. The default implementation delegates to more specific methods,
     * for example {@link #createCoordinateReferenceSystem(String)}, {@link #createDatum(String)}, <i>etc.</i>
     * until a successful one is found.
     *
     * <p><strong>Note that this method may be ambiguous</strong> because the same EPSG code can be used for different
     * kinds of objects. This method throws an exception on a <em>best-effort</em> basis if it detects an ambiguity.
     * It is recommended to invoke the most specific {@code createFoo(String)} method when the desired type is known,
     * both for performance reason and for avoiding ambiguity.</p>
     *
     * @param  code  value allocated by EPSG.
     * @return the object for the given code.
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
        final boolean isPrimaryKey = isPrimaryKey(code);
        final StringBuilder query  = new StringBuilder("SELECT ");
        final int queryStart       = query.length();
        int found = -1;
        try {
            final int pk = isPrimaryKey ? toPrimaryKeys(null, null, null, code)[0] : 0;
            for (int i = 0; i < TableInfo.EPSG.length; i++) {
                final TableInfo table = TableInfo.EPSG[i];
                final String column = isPrimaryKey ? table.codeColumn : table.nameColumn;
                if (column == null) {
                    continue;
                }
                query.setLength(queryStart);
                query.append(table.codeColumn);
                if (!isPrimaryKey) {
                    query.append(", ").append(column);      // Only for filterFalsePositive(…).
                }
                query.append(" FROM ").append(table.table)
                     .append(" WHERE ").append(column).append(isPrimaryKey ? " = ?" : " LIKE ?");
                try (PreparedStatement stmt = connection.prepareStatement(translator.apply(query.toString()))) {
                    /*
                     * Check if at least one record is found for the code or the name.
                     * Ensure that there is not two values for the same code or name.
                     */
                    if (isPrimaryKey) {
                        stmt.setInt(1, pk);
                    } else {
                        stmt.setString(1, SQLUtilities.toLikePattern(code, false));
                    }
                    Integer present = null;
                    try (ResultSet result = stmt.executeQuery()) {
                        while (result.next()) {
                            if (isPrimaryKey || SQLUtilities.filterFalsePositive(code, result.getString(2))) {
                                present = ensureSingleton(getOptionalInteger(result, 1), present, code);
                            }
                        }
                    }
                    if (present != null) {
                        if (found >= 0) {
                            throw new FactoryDataException(error().getString(Errors.Keys.DuplicatedIdentifier_1, code));
                        }
                        found = i;
                    }
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(IdentifiedObject.class, code, exception);
        }
        /*
         * If a record has been found in one table, then delegates to the appropriate method.
         */
        if (found >= 0) {
            switch (found) {
                case 0:  return createCoordinateReferenceSystem(code);
                case 1:  return createCoordinateSystem         (code);
                case 2:  return createCoordinateSystemAxis     (code);
                case 3:  return createDatum                    (code);
                case 4:  return createEllipsoid                (code);
                case 5:  return createPrimeMeridian            (code);
                case 6:  return createCoordinateOperation      (code);
                case 7:  return createOperationMethod          (code);
                case 8:  return createParameterDescriptor      (code);
                case 9:  break; // Cannot cast Unit to IdentifiedObject
                default: throw new AssertionError(found);                   // Should not happen
            }
        }
        throw noSuchAuthorityCode(IdentifiedObject.class, code);
    }

    /**
     * Creates an arbitrary coordinate reference system from a code.
     * The returned object will typically be an instance of {@link GeographicCRS}, {@link ProjectedCRS},
     * {@link VerticalCRS} or {@link CompoundCRS}.
     *
     * <h4>Examples</h4>
     * Some EPSG codes for coordinate reference systems are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Type</th>          <th>Description</th></tr>
     *   <tr><td>4326</td> <td>Geographic</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>4979</td> <td>Geographic 3D</td> <td>World Geodetic System 1984</td></tr>
     *   <tr><td>4978</td> <td>Geocentric</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>3395</td> <td>Projected</td>     <td>WGS 84 / World Mercator</td></tr>
     *   <tr><td>5714</td> <td>Vertical</td>      <td>Mean Sea Level height</td></tr>
     *   <tr><td>6349</td> <td>Compound</td>      <td>NAD83(2011) + NAVD88 height</td></tr>
     *   <tr><td>5800</td> <td>Engineering</td>   <td>Astra Minas Grid</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    @SuppressWarnings("try")    // Explicit call to close() on an auto-closeable resource.
    public synchronized CoordinateReferenceSystem createCoordinateReferenceSystem(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        CoordinateReferenceSystem returnValue = null;
        try (ResultSet result = executeQuery("Coordinate Reference System", "COORD_REF_SYS_CODE", "COORD_REF_SYS_NAME",
                "SELECT COORD_REF_SYS_CODE,"          +     // [ 1]
                      " COORD_REF_SYS_NAME,"          +     // [ 2]
                      " AREA_OF_USE_CODE,"            +     // [ 3] Deprecated since EPSG version 10 (always null)
                      " CRS_SCOPE,"                   +     // [ 4]
                      " REMARKS,"                     +     // [ 5]
                      " DEPRECATED,"                  +     // [ 6]
                      " COORD_REF_SYS_KIND,"          +     // [ 7]
                      " COORD_SYS_CODE,"              +     // [ 8] Null for CompoundCRS
                      " DATUM_CODE,"                  +     // [ 9] Null for ProjectedCRS
                      " BASE_CRS_CODE,"               +     // [10] For ProjectedCRS
                      " PROJECTION_CONV_CODE,"        +     // [11] For ProjectedCRS
                      " CMPD_HORIZCRS_CODE,"          +     // [12] For CompoundCRS only
                      " CMPD_VERTCRS_CODE"            +     // [13] For CompoundCRS only
                " FROM \"Coordinate Reference System\"" +
                " WHERE COORD_REF_SYS_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final String  area       = getOptionalString (result, 3);
                final String  scope      = getOptionalString (result, 4);
                final String  remarks    = getOptionalString (result, 5);
                final boolean deprecated = getOptionalBoolean(result, 6);
                final String  type       = getString   (code, result, 7);
                /*
                 * Note: Do not invoke `createProperties` now, even if we have all required information,
                 *       because the `properties` map is going to overwritten by calls to `createDatum`, etc.
                 *
                 * The following switch statement should have a case for all "epsg_crs_kind" values enumerated
                 * in the "EPSG_Prepare.sql" file, except that the values in this Java code are in lower cases.
                 */
                final CRSFactory crsFactory = owner.crsFactory;
                final CoordinateReferenceSystem crs;
                switch (type.toLowerCase(Locale.US)) {
                    /* ----------------------------------------------------------------------
                     *   GEOGRAPHIC CRS
                     *
                     *   NOTE: `createProperties` MUST be invoked after any call to another
                     *         `createFoo` method. Consequently, do not factor out.
                     * ---------------------------------------------------------------------- */
                    case "geographic 2d":
                    case "geographic 3d": {
                        Integer csCode = getInteger(code, result, 8);
                        if (replaceDeprecatedCS) {
                            csCode = DEPRECATED_CS.getOrDefault(csCode, csCode);
                        }
                        final EllipsoidalCS cs = owner.createEllipsoidalCS(csCode.toString());
                        final String datumCode = getOptionalString(result, 9);
                        GeodeticDatum datum;
                        if (datumCode != null) {
                            datum = owner.createGeodeticDatum(datumCode);
                        } else {
                            final String geoCode = getString(code, result, 10, 9);
                            result.close();     // Must be closed before call to createGeographicCRS(String)
                            ensureNoCycle(GeographicCRS.class, epsg);
                            try {
                                datum = owner.createGeographicCRS(geoCode).getDatum();
                            } finally {
                                endOfRecursion(GeographicCRS.class, epsg);
                            }
                        }
                        DatumEnsemble<GeodeticDatum> ensemble = wasDatumEnsemble(datum, GeodeticDatum.class);
                        if (ensemble != null) datum = null;
                        crs = crsFactory.createGeographicCRS(createProperties("Coordinate Reference System",
                                epsg, name, null, area, scope, remarks, deprecated), datum, ensemble, cs);
                        break;
                    }
                    /* ----------------------------------------------------------------------
                     *   PROJECTED CRS
                     *
                     *   NOTE: This method invokes itself indirectly, through createGeographicCRS.
                     *         Consequently, we cannot use `result` anymore after this block.
                     * ---------------------------------------------------------------------- */
                    case "projected": {
                        final String csCode  = getString(code, result,  8);
                        final String geoCode = getString(code, result, 10);
                        final String opCode  = getString(code, result, 11);
                        result.close();      // Must be closed before call to createFoo(String)
                        ensureNoCycle(ProjectedCRS.class, epsg);
                        try {
                            final CartesianCS cs = owner.createCartesianCS(csCode);
                            final Conversion op;
                            try {
                                op = (Conversion) owner.createCoordinateOperation(opCode);
                            } catch (ClassCastException e) {
                                // Should never happen in a well-formed EPSG database.
                                // If happen anyway, the ClassCastException cause will give more hints than just the message.
                                throw (NoSuchAuthorityCodeException) noSuchAuthorityCode(Conversion.class, opCode).initCause(e);
                            }
                            final CoordinateReferenceSystem baseCRS;
                            final boolean suspendParamChecks;
                            if (!deprecated) {
                                baseCRS = owner.createCoordinateReferenceSystem(geoCode);
                                suspendParamChecks = true;
                            } else {
                                /*
                                 * If the ProjectedCRS is deprecated, one reason among others may be that it uses one of
                                 * the deprecated coordinate systems. Those deprecated CS used non-linear units like DMS.
                                 * Apache SIS cannot instantiate a ProjectedCRS when the baseCRS uses such units, so we
                                 * set a flag asking to replace the deprecated CS by a supported one. Since that baseCRS
                                 * would not be exactly as defined by EPSG, we must not cache it because we do not want
                                 * `owner.createGeographicCRS(geoCode)` to return that modified CRS. Since the same CRS
                                 * may be recreated every time a deprecated ProjectedCRS is created, we temporarily
                                 * shutdown the loggings in order to avoid the same warning to be logged many time.
                                 */
                                final boolean old = quiet;
                                try {
                                    quiet = true;
                                    replaceDeprecatedCS = true;
                                    baseCRS = createCoordinateReferenceSystem(geoCode);         // Do not cache that CRS.
                                } finally {
                                    replaceDeprecatedCS = false;
                                    quiet = old;
                                }
                                /*
                                 * The crsFactory method calls will indirectly create a parameterized MathTransform.
                                 * Their constructor will try to verify the parameter validity. But some deprecated
                                 * CRS had invalid parameter values (they were deprecated precisely for that reason).
                                 * If and only if we are creating a deprecated CRS, temporarily suspend the parameter
                                 * checks.
                                 */
                                suspendParamChecks = Semaphores.queryAndSet(Semaphores.SUSPEND_PARAMETER_CHECK);
                                // Try block must be immediately after above line (do not insert any code between).
                            }
                            try {
                                /*
                                 * For a ProjectedCRS, the baseCRS is always geodetic. So in theory we would not
                                 * need the `instanceof` check. However, the EPSG dataset version 8.9 also uses the
                                 * "projected" type for CRS that are actually derived CRS. See EPSG:5820 and 5821.
                                 *
                                 * TODO: there is an ambiguity when the source CRS is geographic but the operation
                                 * is nevertheless considered as not a map projection. It is the case of EPSG:5819.
                                 * The problem is that the "COORD_REF_SYS_KIND" column still contains "Projected".
                                 * We need to check if EPSG database 10+ has more specific information.
                                 * See https://issues.apache.org/jira/browse/SIS-518
                                 */
                                @SuppressWarnings("LocalVariableHidesMemberVariable")
                                final Map<String,Object> properties = createProperties("Coordinate Reference System",
                                        epsg, name, null, area, scope, remarks, deprecated);
                                if (baseCRS instanceof GeodeticCRS) {
                                    crs = crsFactory.createProjectedCRS(properties, (GeodeticCRS) baseCRS, op, cs);
                                } else {
                                    crs = crsFactory.createDerivedCRS(properties, baseCRS, op, cs);
                                }
                            } finally {
                                Semaphores.clear(Semaphores.SUSPEND_PARAMETER_CHECK, suspendParamChecks);
                            }
                        } finally {
                            endOfRecursion(ProjectedCRS.class, epsg);
                        }
                        break;
                    }
                    /* ----------------------------------------------------------------------
                     *   VERTICAL CRS
                     * ---------------------------------------------------------------------- */
                    case "vertical": {
                        VerticalCS    cs    = owner.createVerticalCS   (getString(code, result, 8));
                        VerticalDatum datum = owner.createVerticalDatum(getString(code, result, 9));
                        DatumEnsemble<VerticalDatum> ensemble = wasDatumEnsemble(datum, VerticalDatum.class);
                        if (ensemble != null) datum = null;
                        crs = crsFactory.createVerticalCRS(createProperties("Coordinate Reference System",
                                epsg, name, null, area, scope, remarks, deprecated), datum, ensemble, cs);
                        break;
                    }
                    /* ----------------------------------------------------------------------
                     *   TEMPORAL CRS
                     *
                     *   NOTE : The original EPSG database does not define any temporal CRS.
                     *          This block is a SIS-specific extension.
                     * ---------------------------------------------------------------------- */
                    case "time":
                    case "temporal": {
                        TimeCS        cs    = owner.createTimeCS       (getString(code, result, 8));
                        TemporalDatum datum = owner.createTemporalDatum(getString(code, result, 9));
                        DatumEnsemble<TemporalDatum> ensemble = wasDatumEnsemble(datum, TemporalDatum.class);
                        if (ensemble != null) datum = null;
                        crs = crsFactory.createTemporalCRS(createProperties("Coordinate Reference System",
                                epsg, name, null, area, scope, remarks, deprecated), datum, ensemble, cs);
                        break;
                    }
                    /* ----------------------------------------------------------------------
                     *   COMPOUND CRS
                     *
                     *   NOTE: This method invokes itself recursively.
                     *         Consequently, we cannot use `result` anymore.
                     * ---------------------------------------------------------------------- */
                    case "compound": {
                        final String code1 = getString(code, result, 12);
                        final String code2 = getString(code, result, 13);
                        result.close();
                        final CoordinateReferenceSystem crs1, crs2;
                        ensureNoCycle(CompoundCRS.class, epsg);
                        try {
                            crs1 = owner.createCoordinateReferenceSystem(code1);
                            crs2 = owner.createCoordinateReferenceSystem(code2);
                        } finally {
                            endOfRecursion(CompoundCRS.class, epsg);
                        }
                        // Note: Do not invoke `createProperties` sooner.
                        crs  = crsFactory.createCompoundCRS(createProperties("Coordinate Reference System",
                                epsg, name, null, area, scope, remarks, deprecated), crs1, crs2);
                        break;
                    }
                    /* ----------------------------------------------------------------------
                     *   GEOCENTRIC CRS
                     * ---------------------------------------------------------------------- */
                    case "geocentric": {
                        CoordinateSystem cs = owner.createCoordinateSystem(getString(code, result, 8));
                        GeodeticDatum datum = owner.createGeodeticDatum   (getString(code, result, 9));
                        DatumEnsemble<GeodeticDatum> ensemble = wasDatumEnsemble(datum, GeodeticDatum.class);
                        if (ensemble != null) datum = null;
                        @SuppressWarnings("LocalVariableHidesMemberVariable")
                        final Map<String,Object> properties = createProperties("Coordinate Reference System",
                                epsg, name, null, area, scope, remarks, deprecated);
                        if (cs instanceof CartesianCS) {
                            crs = crsFactory.createGeodeticCRS(properties, datum, ensemble, (CartesianCS) cs);
                        } else if (cs instanceof SphericalCS) {
                            crs = crsFactory.createGeodeticCRS(properties, datum, ensemble, (SphericalCS) cs);
                        } else {
                            throw new FactoryDataException(error().getString(
                                    Errors.Keys.IllegalCoordinateSystem_1, cs.getName()));
                        }
                        break;
                    }
                    /* ----------------------------------------------------------------------
                     *   ENGINEERING CRS
                     * ---------------------------------------------------------------------- */
                    case "engineering": {
                        CoordinateSystem cs    = owner.createCoordinateSystem(getString(code, result, 8));
                        EngineeringDatum datum = owner.createEngineeringDatum(getString(code, result, 9));
                        DatumEnsemble<EngineeringDatum> ensemble = wasDatumEnsemble(datum, EngineeringDatum.class);
                        if (ensemble != null) datum = null;
                        crs = crsFactory.createEngineeringCRS(createProperties("Coordinate Reference System",
                                epsg, name, null, area, scope, remarks, deprecated), datum, ensemble, cs);
                        break;
                    }
                    /* ----------------------------------------------------------------------
                     *   PARAMETRIC CRS
                     * ---------------------------------------------------------------------- */
                    case "parametric": {
                        ParametricCS    cs    = owner.createParametricCS   (getString(code, result, 8));
                        ParametricDatum datum = owner.createParametricDatum(getString(code, result, 9));
                        DatumEnsemble<ParametricDatum> ensemble = wasDatumEnsemble(datum, ParametricDatum.class);
                        if (ensemble != null) datum = null;
                        crs = crsFactory.createParametricCRS(createProperties("Coordinate Reference System",
                                epsg, name, null, area, scope, remarks, deprecated), datum, ensemble, cs);
                        break;
                    }
                    /* ----------------------------------------------------------------------
                     *   UNKNOWN CRS
                     * ---------------------------------------------------------------------- */
                    default: {
                        throw new FactoryDataException(error().getString(Errors.Keys.UnknownType_1, type));
                    }
                }
                returnValue = ensureSingleton(crs, returnValue, code);
                if (result.isClosed()) {
                    return returnValue;
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateReferenceSystem.class, code, exception);
        } catch (ClassCastException exception) {
            throw new FactoryDataException(error().getString(exception.getLocalizedMessage()), exception);
        }
        if (returnValue == null) {
             throw noSuchAuthorityCode(CoordinateReferenceSystem.class, code);
        }
        return returnValue;
    }

    /**
     * Returns the given datum as a datum ensemble if it should be considered as such.
     * This method exists because the datum and datum ensemble are stored in the same table,
     * and Apache <abbr>SIS</abbr> creates those two kinds of objects with the same method.
     * The real type is resolved by inspection of the {@link #createDatum(String)} return value.
     *
     * <h4>Design restriction</h4>
     * We cannot resolve the type with a private field which would be set by {@code #createDatumEnsemble(…)}
     * because that method will not be invoked if the datum is fetched from the cache.
     *
     * @param  <D>         compile-time value of {@code memberType}.
     * @param  datum       the datum to check if it is a datum ensemble.
     * @param  memberType  the expected type of datum members.
     * @return the given datum as an ensemble if it should be considered as such, or {@code null} otherwise.
     * @throws ClassCastException if at least one member is not an instance of the specified type.
     */
    private static <D extends Datum> DatumEnsemble<D> wasDatumEnsemble(final D datum, final Class<D> memberType) {
        if (datum instanceof DatumEnsemble<?>) {
            return DefaultDatumEnsemble.castOrCopy((DatumEnsemble<?>) datum).cast(memberType);
        }
        return null;
    }

    /**
     * Creates an arbitrary datum from a code. The returned object will typically be an
     * instance of {@link GeodeticDatum}, {@link VerticalDatum} or {@link TemporalDatum}.
     *
     * <h4>Examples</h4>
     * Some EPSG codes for datums are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Type</th>            <th>Description</th></tr>
     *   <tr><td>6326</td> <td>Datum ensemble</td>  <td>World Geodetic System 1984</td></tr>
     *   <tr><td>6322</td> <td>Dynamic geodetic</td><td>World Geodetic System 1972</td></tr>
     *   <tr><td>1027</td> <td>Vertical</td>        <td>EGM2008 geoid</td></tr>
     *   <tr><td>5100</td> <td>Vertical</td>        <td>Mean Sea Level</td></tr>
     *   <tr><td>9315</td> <td>Engineering</td>     <td>Seismic bin grid datum</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized Datum createDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        Datum returnValue = null;
        try (ResultSet result = executeQuery("Datum", "DATUM_CODE", "DATUM_NAME",
                "SELECT DATUM_CODE,"             +  // [ 1]
                      " DATUM_NAME,"             +  // [ 2]
                      " DATUM_TYPE,"             +  // [ 3]
                      " ORIGIN_DESCRIPTION,"     +  // [ 4]
                      " REALIZATION_EPOCH,"      +  // [ 5]
                      " AREA_OF_USE_CODE,"       +  // [ 6] — Deprecated since EPSG version 10 (always null)
                      " DATUM_SCOPE,"            +  // [ 7]
                      " REMARKS,"                +  // [ 8]
                      " DEPRECATED,"             +  // [ 9]
                      " ELLIPSOID_CODE,"         +  // [10] — Only for geodetic type
                      " PRIME_MERIDIAN_CODE,"    +  // [11] — Only for geodetic type
                      " REALIZATION_METHOD_CODE" +  // [12] — Only for vertical type
                " FROM \"Datum\"" +
                " WHERE DATUM_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final String  type       = getString   (code, result, 3);
                final String  anchor     = getOptionalString (result, 4);
                final String  epoch      = getOptionalString (result, 5);
                final String  area       = getOptionalString (result, 6);
                final String  scope      = getOptionalString (result, 7);
                final String  remarks    = getOptionalString (result, 8);
                final boolean deprecated = getOptionalBoolean(result, 9);
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                Map<String,Object> properties = createProperties("Datum",
                        epsg, name, null, area, scope, remarks, deprecated);
                properties.put(Datum.ANCHOR_DEFINITION_KEY, anchor);
                if (epoch != null) try {
                    /*
                     * Parse the date manually because it is declared as a VARCHAR instead of DATE in original
                     * SQL scripts. Apache SIS installer replaces VARCHAR by DATE, but we have no guarantee that
                     * we are reading an EPSG database created by our installer. Furthermore, an older version of
                     * EPSG installer was using SMALLINT instead of DATE, because scripts before EPSG 9.0 were
                     * reporting only the epoch year.
                     */
                    final CharSequence[] fields = CharSequences.split(epoch, '-');
                    int year = 0, month = 0, day = 1;
                    for (int i = Math.min(fields.length, 3); --i >= 0;) {
                        final int f = Integer.parseInt(fields[i].toString());
                        switch (i) {
                            case 0: year  = f;   break;
                            case 1: month = f-1; break;
                            case 2: day   = f;   break;
                        }
                    }
                    if (year != 0) {
                        @SuppressWarnings("LocalVariableHidesMemberVariable")
                        final Calendar calendar = getCalendar();
                        calendar.set(year, month, day);
                        properties.put(Datum.ANCHOR_EPOCH_KEY, calendar.getTime().toInstant());
                    }
                } catch (NumberFormatException exception) {
                    unexpectedException("createDatum", exception);          // Not a fatal error.
                }
                /*
                 * The following switch statement should have a case for all "epsg_datum_kind" values enumerated
                 * in the "EPSG_Prepare.sql" file, except that the values in this Java code are in lower cases.
                 */
                final DatumFactory datumFactory = owner.datumFactory;
                final Datum datum;
                switch (type.toLowerCase(Locale.US)) {
                    /*
                     * The "geodetic" case invokes createProperties(…) indirectly through calls to
                     * createEllipsoid(String) and createPrimeMeridian(String), so we must protect
                     * the properties map from changes.
                     */
                    case "dynamic geodetic":
                    case "geodetic": {
                        properties = new HashMap<>(properties);         // Protect from changes
                        final Ellipsoid ellipsoid    = owner.createEllipsoid    (getString(code, result, 10));
                        final PrimeMeridian meridian = owner.createPrimeMeridian(getString(code, result, 11));
                        final BursaWolfParameters[] param = createBursaWolfParameters(meridian, epsg);
                        if (param != null) {
                            properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, param);
                        }
                        datum = datumFactory.createGeodeticDatum(properties, ellipsoid, meridian);
                        break;
                    }
                    case "vertical": {
                        final RealizationMethod method = getRealizationMethod(getOptionalInteger(result, 12));
                        datum = datumFactory.createVerticalDatum(properties, method);
                        break;
                    }
                    /*
                     * Origin date is stored in ORIGIN_DESCRIPTION field. A column of SQL type
                     * "date" type would have been better, but we do not modify the EPSG model.
                     */
                    case "temporal": {
                        final Temporal originDate;
                        if (Strings.isNullOrEmpty(anchor)) {
                            throw new FactoryDataException(resources().getString(Resources.Keys.DatumOriginShallBeDate));
                        }
                        try {
                            originDate = LenientDateFormat.parseBest(anchor);
                        } catch (RuntimeException e) {
                            throw new FactoryDataException(resources().getString(Resources.Keys.DatumOriginShallBeDate), e);
                        }
                        datum = datumFactory.createTemporalDatum(properties, originDate);
                        break;
                    }
                    /*
                     * Straightforward case.
                     */
                    case "engineering": {
                        datum = datumFactory.createEngineeringDatum(properties);
                        break;
                    }
                    case "parametric": {
                        datum = datumFactory.createParametricDatum(properties);
                        break;
                    }
                    case "ensemble": {
                        properties = new HashMap<>(properties);         // Protect from changes
                        datum = DefaultDatumEnsemble.castOrCopy(createDatumEnsemble(epsg, properties));
                        break;
                    }
                    default: {
                        throw new FactoryDataException(error().getString(Errors.Keys.UnknownType_1, type));
                    }
                }
                returnValue = ensureSingleton(datum, returnValue, code);
                if (result.isClosed()) {
                    break;                  // Because of the recursive call done by createBursaWolfParameters(…).
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(Datum.class, code, exception);
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(Datum.class, code);
        }
        return returnValue;
    }

    /**
     * Creates an arbitrary datum ensemble from a code.
     *
     * @param  code        value allocated by EPSG.
     * @param  properties  properties to assign to the datum ensemble.
     * @return the datum ensemble for the given code, or {@code null} if not found.
     */
    private DatumEnsemble<?> createDatumEnsemble(final Integer code, final Map<String,Object> properties)
            throws SQLException, FactoryException
    {
        double accuracy = Double.NaN;
        try (ResultSet result = executeQuery("DatumEnsemble",
                "SELECT ENSEMBLE_ACCURACY" +
                " FROM \"DatumEnsemble\"" +
                " WHERE DATUM_ENSEMBLE_CODE = ?", code))
        {
            // Should have exactly one value. The loop is a paranoiac safety.
            while (result.next()) {
                final double value = getDouble(code, result, 1);
                if (Double.isNaN(accuracy) || value > accuracy) {
                    accuracy = value;
                }
            }
        }
        final var members = new ArrayList<Datum>();
        try (ResultSet result = executeQuery("DatumEnsembleMember",
                "SELECT DATUM_CODE" +
                " FROM \"DatumEnsembleMember\"" +
                " WHERE DATUM_ENSEMBLE_CODE = ?" +
                " ORDER BY DATUM_SEQUENCE", code))
        {
            while (result.next()) {
                members.add(owner.createDatum(getInteger(code, result, 1).toString()));
            }
        }
        return owner.datumFactory.createDatumEnsemble(properties, members, PositionalAccuracyConstant.ensemble(accuracy));
    }

    /**
     * Returns Bursa-Wolf parameters for a geodetic reference frame.
     * If the specified datum has no conversion information, then this method returns {@code null}.
     *
     * <p>This method is for compatibility with <i>Well Known Text</i> (WKT) version 1 formatting.
     * That legacy format had a {@code TOWGS84} element which needs the information provided by this method.
     * Note that {@code TOWGS84} is a deprecated element as of WKT 2 (ISO 19162).</p>
     *
     * @param  meridian  the source datum prime meridian, used for discarding any target datum using a different meridian.
     * @param  code      the EPSG code of the source {@link GeodeticDatum}.
     * @return an array of Bursa-Wolf parameters, or {@code null}.
     */
    private BursaWolfParameters[] createBursaWolfParameters(final PrimeMeridian meridian, final Integer code)
            throws SQLException, FactoryException
    {
        /*
         * We do not provide TOWGS84 information for WGS84 itself or for any other datum on our list of target datum,
         * in order to avoid infinite recursion. The `ensureNonRecursive` call is an extra safety check which should
         * never fail, unless TARGET_CRS and TARGET_DATUM values do not agree with database content.
         */
        if (code == BursaWolfInfo.TARGET_DATUM) {
            return null;
        }
        final var bwInfos = new ArrayList<BursaWolfInfo>();
        try (ResultSet result = executeQuery("BursaWolfParametersSet",
                "SELECT COORD_OP_CODE," +
                      " COORD_OP_METHOD_CODE," +
                      " TARGET_CRS_CODE," +
                      " AREA_OF_USE_CODE" +      // Deprecated since EPSG version 10 (always null).
                " FROM \"Coordinate_Operation\"" +
               " WHERE DEPRECATED=0" +           // Do not put spaces around "=" - SQLTranslator searches for this exact match.
                 " AND TARGET_CRS_CODE = "       + BursaWolfInfo.TARGET_CRS +
                 " AND COORD_OP_METHOD_CODE >= " + BursaWolfInfo.MIN_METHOD_CODE +
                 " AND COORD_OP_METHOD_CODE <= " + BursaWolfInfo.MAX_METHOD_CODE +
                 " AND SOURCE_CRS_CODE IN " +
               "(SELECT COORD_REF_SYS_CODE FROM \"Coordinate Reference System\" WHERE DATUM_CODE = ?)" +
            " ORDER BY TARGET_CRS_CODE, COORD_OP_ACCURACY, COORD_OP_CODE DESC", code))
        {
            while (result.next()) {
                final var info = new BursaWolfInfo(
                        getInteger(code, result, 1),                // Operation
                        getInteger(code, result, 2),                // Method
                        getInteger(code, result, 3),                // Target datum
                        getInteger(code, result, 4));               // Domain of validity
                if (info.target != code) {                          // Paranoiac check.
                    bwInfos.add(info);
                }
            }
        }
        int size = bwInfos.size();
        if (size == 0) {
            return null;
        }
        /*
         * Sort the infos in preference order. The "ORDER BY" clause above was not enough;
         * we also need to take the "Supersession" table in account. Once the sorting is done,
         * keep only one Bursa-Wolf parameters for each datum.
         */
        if (size > 1) {
            final BursaWolfInfo[] codes = bwInfos.toArray(new BursaWolfInfo[size]);
            sort("Coordinate_Operation", codes);
            bwInfos.clear();
            BursaWolfInfo.filter(owner, codes, bwInfos);
            size = bwInfos.size();
        }
        /*
         * Now, iterate over the results and fetch the parameter values for each BursaWolfParameters object.
         */
        final var parameters = new BursaWolfParameters[size];
        final Locale locale = getLocale();
        int count = 0;
        for (int i=0; i<size; i++) {
            final BursaWolfInfo info = bwInfos.get(i);
            final GeodeticDatum datum;
            ensureNoCycle(BursaWolfParameters.class, code);    // See comment at the begining of this method.
            try {
                datum = owner.createGeodeticDatum(String.valueOf(info.target));
            } finally {
                endOfRecursion(BursaWolfParameters.class, code);
            }
            /*
             * Accept only Bursa-Wolf parameters between datum that use the same prime meridian.
             * This is for avoiding ambiguity about whether longitude rotation should be applied
             * before or after the datum change. This check is useless for EPSG dataset 8.9 since
             * all datum seen by this method use Greenwich. But we nevertheless perform this check
             * as a safety for future evolution or customized EPSG dataset.
             */
            if (!Utilities.equalsIgnoreMetadata(meridian, datum.getPrimeMeridian())) {
                continue;
            }
            final var bwp = new BursaWolfParameters(datum, info.getDomainOfValidity(owner));
            try (ResultSet result = executeQuery("BursaWolfParameters",
                "SELECT PARAMETER_CODE," +
                      " PARAMETER_VALUE," +
                      " UOM_CODE" +
                " FROM \"Coordinate_Operation Parameter Value\"" +
                " WHERE COORD_OP_CODE = ?" +
                  " AND COORD_OP_METHOD_CODE = ?", info.operation, info.method))
            {
                while (result.next()) {
                    BursaWolfInfo.setBursaWolfParameter(bwp,
                            getInteger(info.operation, result, 1),
                            getDouble (info.operation, result, 2),
                            owner.createUnit(getString(info.operation, result, 3)), locale);
                }
            }
            if (info.isFrameRotation()) {
                // Coordinate frame rotation (9607): same as 9606,
                // except for the sign of rotation parameters.
                bwp.reverseRotation();
            }
            parameters[count++] = bwp;
        }
        return ArraysExt.resize(parameters, count);
    }

    /**
     * Creates a geometric figure that can be used to describe the approximate shape of the earth.
     * In mathematical terms, it is a surface formed by the rotation of an ellipse about its minor axis.
     *
     * <h4>Examples</h4>
     * Some EPSG codes for ellipsoids are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>7030</td> <td>WGS 84</td></tr>
     *   <tr><td>7034</td> <td>Clarke 1880</td></tr>
     *   <tr><td>7048</td> <td>GRS 1980 Authalic Sphere</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the ellipsoid for the given code.
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
        try (ResultSet result = executeQuery("Ellipsoid", "ELLIPSOID_CODE", "ELLIPSOID_NAME",
                "SELECT ELLIPSOID_CODE," +
                      " ELLIPSOID_NAME," +
                      " SEMI_MAJOR_AXIS," +
                      " INV_FLATTENING," +
                      " SEMI_MINOR_AXIS," +
                      " UOM_CODE," +
                      " REMARKS," +
                      " DEPRECATED" +
                " FROM \"Ellipsoid\"" +
                " WHERE ELLIPSOID_CODE = ?", code))
        {
            while (result.next()) {
                /*
                 * One of `semiMinorAxis` and `inverseFlattening` values can be NULL in the database.
                 * Consequently, we don't use `getString(ResultSet, int)` for those parameters because
                 * we do not want to throw an exception if a NULL value is found.
                 */
                final Integer epsg              = getInteger  (code, result, 1);
                final String  name              = getString   (code, result, 2);
                final double  semiMajorAxis     = getDouble   (code, result, 3);
                final double  inverseFlattening = getOptionalDouble (result, 4);
                final double  semiMinorAxis     = getOptionalDouble (result, 5);
                final String  unitCode          = getString   (code, result, 6);
                final String  remarks           = getOptionalString (result, 7);
                final boolean deprecated        = getOptionalBoolean(result, 8);
                final Unit<Length> unit         = owner.createUnit(unitCode).asType(Length.class);
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties("Ellipsoid",
                        epsg, name, null, null, null, remarks, deprecated);
                final Ellipsoid ellipsoid;
                if (Double.isNaN(inverseFlattening)) {
                    if (Double.isNaN(semiMinorAxis)) {
                        // Both are null, which is not allowed.
                        final String column = result.getMetaData().getColumnName(3);
                        throw new FactoryDataException(error().getString(Errors.Keys.NullValueInTable_3, code, column));
                    }
                    // We only have semiMinorAxis defined. It is OK
                    ellipsoid = owner.datumFactory.createEllipsoid(properties, semiMajorAxis, semiMinorAxis, unit);
                } else {
                    if (!Double.isNaN(semiMinorAxis)) {
                        // Both `inverseFlattening` and `semiMinorAxis` are defined.
                        // Log a warning and create the ellipsoid using the inverse flattening.
                        final LogRecord record = resources().createLogRecord(
                                Level.WARNING,
                                Resources.Keys.AmbiguousEllipsoid_1,
                                Constants.EPSG + Constants.DEFAULT_SEPARATOR + code);
                        Logging.completeAndLog(LOGGER, EPSGDataAccess.class, "createEllipsoid", record);
                    }
                    ellipsoid = owner.datumFactory.createFlattenedSphere(properties, semiMajorAxis, inverseFlattening, unit);
                }
                returnValue = ensureSingleton(ellipsoid, returnValue, code);
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
     * <h4>Examples</h4>
     * Some EPSG codes for prime meridians are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>8901</td> <td>Greenwich</td></tr>
     *   <tr><td>8903</td> <td>Paris</td></tr>
     *   <tr><td>8904</td> <td>Bogota</td></tr>
     *   <tr><td>8905</td> <td>Madrid</td></tr>
     *   <tr><td>8906</td> <td>Rome</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the prime meridian for the given code.
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
        try (ResultSet result = executeQuery("Prime Meridian", "PRIME_MERIDIAN_CODE", "PRIME_MERIDIAN_NAME",
                "SELECT PRIME_MERIDIAN_CODE," +
                      " PRIME_MERIDIAN_NAME," +
                      " GREENWICH_LONGITUDE," +
                      " UOM_CODE," +
                      " REMARKS," +
                      " DEPRECATED" +
                " FROM \"Prime Meridian\"" +
                " WHERE PRIME_MERIDIAN_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final double  longitude  = getDouble   (code, result, 3);
                final String  unitCode   = getString   (code, result, 4);
                final String  remarks    = getOptionalString (result, 5);
                final boolean deprecated = getOptionalBoolean(result, 6);
                final Unit<Angle> unit = owner.createUnit(unitCode).asType(Angle.class);
                final PrimeMeridian primeMeridian = owner.datumFactory.createPrimeMeridian(
                        createProperties("Prime Meridian", epsg, name, null, null, null, remarks, deprecated),
                        longitude, unit);
                returnValue = ensureSingleton(primeMeridian, returnValue, code);
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
     * <h4>Examples</h4>
     * Some EPSG codes for extents are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>1262</td> <td>World</td></tr>
     *   <tr><td>3391</td> <td>World - between 80°S and 84°N</td></tr>
     * </table>
     *
     * <h4>History</h4>
     * The table name was {@code "Area"} before version 10 of the <abbr>EPSG</abbr> geodetic dataset.
     * Starting from <abbr>EPSG</abbr> version 10, the table name is {@code "Extent"} but the first 7
     * columns are the same with different names and order. The last columns are news.
     *
     * <p>Before <abbr>EPSG</abbr> version 10, extents were referenced in columns named {@code AREA_OF_USE_CODE}.
     * Starting with version 10, that column still exists but is deprecated and contains only {@code null} values.
     * An {@code "Usage"} intersection table is used instead.</p>
     *
     * @param  code  value allocated by EPSG.
     * @return the extent for the given code.
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
        try (ResultSet result = executeQuery("Area", "AREA_CODE", "AREA_NAME",
                "SELECT AREA_OF_USE," +
                      " AREA_SOUTH_BOUND_LAT," +
                      " AREA_NORTH_BOUND_LAT," +
                      " AREA_WEST_BOUND_LON," +
                      " AREA_EAST_BOUND_LON" +
                " FROM \"Area\"" +
                " WHERE AREA_CODE = ?", code))
        {
            while (result.next()) {
                final String description = getOptionalString(result, 1);
                double ymin = getOptionalDouble(result, 2);
                double ymax = getOptionalDouble(result, 3);
                double xmin = getOptionalDouble(result, 4);
                double xmax = getOptionalDouble(result, 5);
                DefaultGeographicBoundingBox bbox = null;
                if (!Double.isNaN(ymin) || !Double.isNaN(ymax) || !Double.isNaN(xmin) || !Double.isNaN(xmax)) {
                    /*
                     * Fix an error found in EPSG:3790 New Zealand - South Island - Mount Pleasant mc
                     * for older database (this error is fixed in EPSG database 8.2).
                     *
                     * Do NOT apply anything similar for the x axis, because xmin > xmax is not error:
                     * it describes a bounding box crossing the anti-meridian (±180° of longitude).
                     */
                    if (ymin > ymax) {
                        final double t = ymin;
                        ymin = ymax;
                        ymax = t;
                    }
                    bbox = new DefaultGeographicBoundingBox(xmin, xmax, ymin, ymax);
                }
                if (description != null || bbox != null) {
                    var extent = new DefaultExtent(description, bbox, null, null);
                    extent.transitionTo(DefaultExtent.State.FINAL);
                    returnValue = ensureSingleton(extent, returnValue, code);
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
     * Creates an arbitrary coordinate system from a code. The returned object will typically be an
     * instance of {@link EllipsoidalCS}, {@link CartesianCS} or {@link VerticalCS}.
     *
     * <h4>Examples</h4>
     * Some EPSG codes for coordinate systems are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Type</th>              <th>Axes</th>                                    <th>Orientations</th> <th>Unit</th></tr>
     *   <tr><td>4406</td> <td>Cartesian 2D CS</td>   <td>easting, northing (E,N)</td>                 <td>east, north</td>     <td>kilometre</td></tr>
     *   <tr><td>4496</td> <td>Cartesian 2D CS</td>   <td>easting, northing (E,N)</td>                 <td>east, north</td>     <td>metre</td></tr>
     *   <tr><td>4500</td> <td>Cartesian 2D CS</td>   <td>northing, easting (N,E)</td>                 <td>north, east</td>     <td>metre</td></tr>
     *   <tr><td>4491</td> <td>Cartesian 2D CS</td>   <td>westing, northing (W,N)</td>                 <td>west, north</td>     <td>metre</td></tr>
     *   <tr><td>6422</td> <td>Ellipsoidal 2D CS</td> <td>latitude, longitude</td>                     <td>north, east</td>     <td>degree</td></tr>
     *   <tr><td>6424</td> <td>Ellipsoidal 2D CS</td> <td>longitude, latitude</td>                     <td>east, north</td>     <td>degree</td></tr>
     *   <tr><td>6429</td> <td>Ellipsoidal 2D CS</td> <td>longitude, latitude</td>                     <td>east, north</td>     <td>radian</td></tr>
     *   <tr><td>6423</td> <td>Ellipsoidal 3D CS</td> <td>latitude, longitude, ellipsoidal height</td> <td>north, east, up</td> <td>degree, degree, metre</td></tr>
     *   <tr><td>6404</td> <td>Spherical 3D CS</td>   <td>latitude, longitude, radius</td>             <td>north, east, up</td> <td>degree, degree, metre</td></tr>
     *   <tr><td>6498</td> <td>Vertical CS</td>       <td>depth (D)</td>                               <td>down</td>            <td>metre</td></tr>
     *   <tr><td>6499</td> <td>Vertical CS</td>       <td>height (H)</td>                              <td>up</td>              <td>metre</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized CoordinateSystem createCoordinateSystem(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        CoordinateSystem returnValue = null;
        try (ResultSet result = executeQuery("Coordinate System", "COORD_SYS_CODE", "COORD_SYS_NAME",
                "SELECT COORD_SYS_CODE," +
                      " COORD_SYS_NAME," +
                      " COORD_SYS_TYPE," +
                      " DIMENSION," +
                      " REMARKS," +
                      " DEPRECATED" +
                " FROM \"Coordinate System\"" +
                " WHERE COORD_SYS_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final String  type       = getString   (code, result, 3);
                final int     dimension  = getInteger  (code, result, 4);
                final String  remarks    = getOptionalString (result, 5);
                final boolean deprecated = getOptionalBoolean(result, 6);
                final CoordinateSystemAxis[] axes = createCoordinateSystemAxes(epsg, dimension);
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties("Coordinate System",
                        epsg, name, null, null, null, remarks, deprecated);   // Must be after axes.
                /*
                 * The following switch statement should have a case for all "epsg_cs_kind" values enumerated
                 * in the "EPSG_Prepare.sql" file, except that the values in this Java code are in lower cases.
                 */
                final CSFactory csFactory = owner.csFactory;
                CoordinateSystem cs = null;
                switch (type.toLowerCase(Locale.US)) {
                    case WKTKeywords.ellipsoidal: {
                        switch (dimension) {
                            case 2: cs = csFactory.createEllipsoidalCS(properties, axes[0], axes[1]); break;
                            case 3: cs = csFactory.createEllipsoidalCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                        break;
                    }
                    case "cartesian": {         // Need lower-case "c"
                        switch (dimension) {
                            case 2: cs = csFactory.createCartesianCS(properties, axes[0], axes[1]); break;
                            case 3: cs = csFactory.createCartesianCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                        break;
                    }
                    case WKTKeywords.spherical: {
                        switch (dimension) {
                            case 2: cs = csFactory.createSphericalCS(properties, axes[0], axes[1]); break;
                            case 3: cs = csFactory.createSphericalCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                        break;
                    }
                    case WKTKeywords.vertical:
                    case "gravity-related": {
                        switch (dimension) {
                            case 1: cs = csFactory.createVerticalCS(properties, axes[0]); break;
                        }
                        break;
                    }
                    case "time":
                    case WKTKeywords.temporal: {
                        switch (dimension) {
                            case 1: cs = csFactory.createTimeCS(properties, axes[0]); break;
                        }
                        break;
                    }
                    case WKTKeywords.parametric: {
                        switch (dimension) {
                            case 1: cs = csFactory.createParametricCS(properties, axes[0]); break;
                        }
                        break;
                    }
                    case WKTKeywords.linear: {
                        switch (dimension) {
                            case 1: cs = csFactory.createLinearCS(properties, axes[0]); break;
                        }
                        break;
                    }
                    case WKTKeywords.polar: {
                        switch (dimension) {
                            case 2: cs = csFactory.createPolarCS(properties, axes[0], axes[1]); break;
                        }
                        break;
                    }
                    case WKTKeywords.cylindrical: {
                        switch (dimension) {
                            case 3: cs = csFactory.createCylindricalCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                        break;
                    }
                    case WKTKeywords.affine: {
                        switch (dimension) {
                            case 2: cs = csFactory.createAffineCS(properties, axes[0], axes[1]); break;
                            case 3: cs = csFactory.createAffineCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                        break;
                    }
                    default: {
                        throw new FactoryDataException(error().getString(Errors.Keys.UnknownType_1, type));
                    }
                }
                if (cs == null) {
                    throw new FactoryDataException(resources().getString(Resources.Keys.UnexpectedDimensionForCS_1, type));
                }
                returnValue = ensureSingleton(cs, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateSystem.class, code, exception);
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(CoordinateSystem.class, code);
        }
        return returnValue;
    }

    /**
     * Returns the coordinate system axis from an EPSG code for a {@link CoordinateSystem}.
     *
     * @param  cs         the EPSG code for the coordinate system.
     * @param  dimension  of the coordinate system, which is also the size of the returned array.
     * @return an array of coordinate system axis.
     * @throws SQLException if an error occurred during database access.
     * @throws FactoryException if the code has not been found.
     */
    private CoordinateSystemAxis[] createCoordinateSystemAxes(final Integer cs, final int dimension)
            throws SQLException, FactoryException
    {
        int i = 0;
        final var axes = new CoordinateSystemAxis[dimension];
        try (ResultSet result = executeQuery("AxisOrder",
                "SELECT COORD_AXIS_CODE" +
                " FROM \"Coordinate Axis\"" +
                " WHERE COORD_SYS_CODE = ?" +
                " ORDER BY COORD_AXIS_ORDER", cs))
        {
            while (result.next()) {
                final String axis = getString(cs, result, 1);
                if (i < axes.length) {
                    /*
                     * If `i` is out of bounds, an exception will be thrown after the loop.
                     * We do not want to throw an ArrayIndexOutOfBoundsException here.
                     */
                    axes[i] = owner.createCoordinateSystemAxis(axis);
                }
                ++i;
            }
        }
        if (i != axes.length) {
            throw new FactoryDataException(error().getString(Errors.Keys.MismatchedDimension_2, axes.length, i));
        }
        return axes;
    }

    /**
     * Creates a coordinate system axis with name, direction, unit and range of values.
     *
     * <h4>Examples</h4>
     * Some EPSG codes for axes are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Description</th>   <th>Unit</th></tr>
     *   <tr><td>106</td>  <td>Latitude (φ)</td>  <td>degree</td></tr>
     *   <tr><td>107</td>  <td>Longitude (λ)</td> <td>degree</td></tr>
     *   <tr><td>1</td>    <td>Easting (E)</td>   <td>metre</td></tr>
     *   <tr><td>2</td>    <td>Northing (N)</td>  <td>metre</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the axis for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see #createCoordinateSystem(String)
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis
     */
    @Override
    public synchronized CoordinateSystemAxis createCoordinateSystemAxis(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        CoordinateSystemAxis returnValue = null;
        try (ResultSet result = executeQuery("Coordinate Axis", "COORD_AXIS_CODE", null,
                "SELECT COORD_AXIS_CODE," +
                      " COORD_AXIS_NAME_CODE," +
                      " COORD_AXIS_ORIENTATION," +
                      " COORD_AXIS_ABBREVIATION," +
                      " UOM_CODE" +
                " FROM \"Coordinate Axis\"" +
               " WHERE COORD_AXIS_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg         = getInteger(code, result, 1);
                final Integer nameCode     = getInteger(code, result, 2);
                final String  orientation  = getString (code, result, 3);
                final String  abbreviation = getString (code, result, 4);
                final String  unit         = getString (code, result, 5);
                final AxisDirection direction;
                try {
                    direction = CoordinateSystems.parseAxisDirection(orientation);
                } catch (IllegalArgumentException exception) {
                    throw new FactoryDataException(exception.getLocalizedMessage(), exception);
                }
                final AxisName an = getAxisName(nameCode);
                final CoordinateSystemAxis axis = owner.csFactory.createCoordinateSystemAxis(
                        createProperties("Coordinate Axis", epsg, an.name, an.description, null, null, an.remarks, false),
                        abbreviation, direction, owner.createUnit(unit));
                returnValue = ensureSingleton(axis, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateSystemAxis.class, code, exception);
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(CoordinateSystemAxis.class, code);
        }
        return returnValue;
    }

    /**
     * Returns the name and description for the specified {@link CoordinateSystemAxis} code.
     * Many axes share the same name and description, so it is worth to cache them.
     */
    private AxisName getAxisName(final Integer code) throws FactoryException, SQLException {
        assert Thread.holdsLock(this);
        AxisName returnValue = axisNames.get(code);
        if (returnValue == null) {
            try (ResultSet result = executeQuery("Coordinate Axis Name",
                    "SELECT COORD_AXIS_NAME, DESCRIPTION, REMARKS" +
                    " FROM \"Coordinate Axis Name\"" +
                    " WHERE COORD_AXIS_NAME_CODE = ?", code))
            {
                while (result.next()) {
                    final String name  = getString(code,   result, 1);
                    String description = getOptionalString(result, 2);
                    String remarks     = getOptionalString(result, 3);
                    final var axis = new AxisName(name, description, remarks);
                    returnValue = ensureSingleton(axis, returnValue, code);
                }
            }
            if (returnValue == null) {
                throw noSuchAuthorityCode(AxisName.class, String.valueOf(code));
            }
            axisNames.put(code, returnValue);
        }
        return returnValue;
    }

    /**
     * Returns the realization method for the specified code.
     *
     * @param  code  code of the realization method, or {@code null} if none.
     * @return realization method, or {@code null} if the given code was null.
     */
    private RealizationMethod getRealizationMethod(final Integer code) throws FactoryException, SQLException {
        assert Thread.holdsLock(this);
        RealizationMethod returnValue = realizationMethods.get(code);
        if (returnValue == null && code != null) {
            try (ResultSet result = executeQuery("DatumRealizationMethod",
                    "SELECT REALIZATION_METHOD_NAME" +
                    " FROM \"DatumRealizationMethod\"" +
                    " WHERE REALIZATION_METHOD_CODE = ?", code))
            {
                while (result.next()) {
                    final String name = getString(code, result, 1);
                    returnValue = ensureSingleton(VerticalDatumTypes.fromMethod(name), returnValue, code);
                }
            }
            if (returnValue == null) {
                throw noSuchAuthorityCode(RealizationMethod.class, String.valueOf(code));
            }
            realizationMethods.put(code, returnValue);
        }
        return returnValue;
    }

    /**
     * Creates an unit of measurement from a code.
     * Current implementation first checks if {@link Units#valueOfEPSG(int)} can provide a hard-coded unit
     * for the given code before to try to parse the information found in the database. This is done that
     * way for better support of non-straightforward units like <i>sexagesimal degrees</i>
     * (EPSG:9110 and 9111).
     *
     * <h4>Examples</h4>
     * Some EPSG codes for units are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>9002</td> <td>decimal degree</td></tr>
     *   <tr><td>9001</td> <td>metre</td></tr>
     *   <tr><td>9030</td> <td>kilometre</td></tr>
     *   <tr><td>1040</td> <td>second</td></tr>
     *   <tr><td>1029</td> <td>year</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the unit of measurement for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized Unit<?> createUnit(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        Unit<?> returnValue = null;
        try (ResultSet result = executeQuery("Unit of Measure", "UOM_CODE", "UNIT_OF_MEAS_NAME",
                "SELECT UOM_CODE," +
                      " FACTOR_B," +
                      " FACTOR_C," +
                      " TARGET_UOM_CODE," +
                      " UNIT_OF_MEAS_NAME" +
                " FROM \"Unit of Measure\"" +
                " WHERE UOM_CODE = ?", code))
        {
            while (result.next()) {
                final int source = getInteger(code,  result, 1);
                final double   b = getOptionalDouble(result, 2);
                final double   c = getOptionalDouble(result, 3);
                final int target = getInteger(code,  result, 4);
                if (source == target) {
                    /*
                     * The unit is a base unit. Verify its consistency:
                     * conversion from `source` to itself shall be the identity function.
                     */
                    final boolean pb = (b != 1);
                    if (pb || c != 1) {
                        throw new FactoryDataException(error().getString(
                                Errors.Keys.InconsistentAttribute_2,
                                pb ? "FACTOR_B" : "FACTOR_C",
                                pb ? b : c));
                    }
                }
                Unit<?> unit = Units.valueOfEPSG(source);                           // Check in our list of hard-coded unit codes.
                if (unit == null) {
                    final Unit<?> base = Units.valueOfEPSG(target);
                    if (base != null && !Double.isNaN(b) && !Double.isNaN(c)) {     // May be NaN if the conversion is non-linear.
                        unit = Units.multiply(base, b, c);
                    } else try {
                        unit = Units.valueOf(getString(code, result, 5));           // Try parsing the unit symbol as a fallback.
                    } catch (MeasurementParseException e) {
                        throw new FactoryDataException(error().getString(Errors.Keys.UnknownUnit_1, code), e);
                    }
                }
                returnValue = ensureSingleton(unit, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(Unit.class, code, exception);
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(Unit.class, code);
        }
        return returnValue;
    }

    /**
     * Creates a definition of a single parameter used by an operation method.
     *
     * <h4>Examples</h4>
     * Some EPSG codes for parameters are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>8801</td> <td>Latitude of natural origin</td></tr>
     *   <tr><td>8802</td> <td>Longitude of natural origin</td></tr>
     *   <tr><td>8805</td> <td>Scale factor at natural origin</td></tr>
     *   <tr><td>8806</td> <td>False easting</td></tr>
     *   <tr><td>8807</td> <td>False northing</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the parameter descriptor for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.parameter.DefaultParameterDescriptor
     */
    @Override
    public synchronized ParameterDescriptor<?> createParameterDescriptor(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        ParameterDescriptor<?> returnValue = null;
        try (ResultSet result = executeQuery("Coordinate_Operation Parameter", "PARAMETER_CODE", "PARAMETER_NAME",
                "SELECT PARAMETER_CODE," +
                      " PARAMETER_NAME," +
                      " DESCRIPTION," +
                      " DEPRECATED" +
                " FROM \"Coordinate_Operation Parameter\"" +
                " WHERE PARAMETER_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg        = getInteger  (code, result, 1);
                final String  name        = getString   (code, result, 2);
                final String  description = getOptionalString (result, 3);
                final boolean deprecated  = getOptionalBoolean(result, 4);
                /*
                 * If the parameter is an integer code, the type is integer and there is no unit.
                 */
                Class<?> type;
                final Set<Unit<?>> units;
                if (epsg != null && Arrays.binarySearch(EPSG_CODE_PARAMETERS, epsg) >= 0) {
                    type  = Integer.class;
                    units = Set.of();
                } else {
                    /*
                     * If the parameter appears to have at least one non-null value in the "Parameter File Name" column,
                     * then the type is assumed to be URI as a string. Otherwise, the type is a floating point number.
                     */
                    type = Double.class;
                    try (ResultSet r = executeQuery("ParameterType",
                            "SELECT PARAM_VALUE_FILE_REF FROM \"Coordinate_Operation Parameter Value\"" +
                            " WHERE (PARAMETER_CODE = ?) AND PARAM_VALUE_FILE_REF IS NOT NULL", epsg))
                    {
                        while (r.next()) {
                            String element = getOptionalString(r, 1);
                            if (element != null && !element.isEmpty()) {
                                type = String.class;
                                break;
                            }
                        }
                    }
                    /*
                     * Search for units.   We typically have many different units but all of the same dimension
                     * (for example metres, kilometres, feet, etc.). In such case, the units Set will have only
                     * one element and that element will be the most frequently used unit.  But some parameters
                     * accept units of different dimensions. For example, the "Coordinate 1 of evaluation point"
                     * (EPSG:8617) parameter value may be in metres or in degrees.   In such case the units Set
                     * will have two elements.
                     */
                    units = new LinkedHashSet<>();
                    try (ResultSet r = executeQuery("ParameterUnit",
                            "SELECT UOM_CODE FROM \"Coordinate_Operation Parameter Value\"" +
                            " WHERE (PARAMETER_CODE = ?)" +
                            " GROUP BY UOM_CODE" +
                            " ORDER BY COUNT(UOM_CODE) DESC", epsg))
                    {
next:                   while (r.next()) {
                            final String c = getOptionalString(r, 1);
                            if (c != null) {
                                final Unit<?> candidate = owner.createUnit(c);
                                for (final Unit<?> e : units) {
                                    if (candidate.isCompatible(e)) {
                                        continue next;
                                    }
                                }
                                units.add(candidate);
                            }
                        }
                    }
                }
                /*
                 * Determines if the inverse operation can be performed by reversing the parameter sign.
                 * The EPSG dataset uses "Yes" or "No" value, but SIS scripts use boolean type. We have
                 * to accept both. Note that if we do not recognize the string as a boolean value, then
                 * we need a SQLException, not a null value.  If the value is wrongly null, this method
                 * will succeed anyway and EPSGDataAccess will finish its work without apparent problem,
                 * but Apache SIS will fail later when it will try to compute the inverse operation, for
                 * example in a call to CRS.findOperation(…). The exception thrown at such later time is
                 * much more difficult to relate to the root cause than if we throw the exception here.
                 */
                InternationalString isReversible = null;
                try (ResultSet r = executeQuery("ParameterSign",
                        "SELECT DISTINCT PARAM_SIGN_REVERSAL" +
                        " FROM \"Coordinate_Operation Parameter Usage\"" +
                        " WHERE (PARAMETER_CODE = ?)", epsg))
                {
                    if (r.next()) {
                        Boolean b;
                        if (translator.useBoolean()) {
                            b = r.getBoolean(1);
                            if (r.wasNull()) b = null;
                        } else {
                            b = SQLUtilities.parseBoolean(r.getString(1));  // May throw SQLException - see above comment.
                        }
                        if (b != null) {
                            isReversible = b ? SignReversalComment.OPPOSITE : SignReversalComment.SAME;
                        }
                    }
                }
                /*
                 * Now creates the parameter descriptor.
                 */
                final NumberRange<?> valueDomain;
                switch (units.size()) {
                    case 0:  valueDomain = null; break;
                    default: valueDomain = new EPSGParameterDomain(units); break;
                    case 1:  valueDomain = MeasurementRange.create(Double.NEGATIVE_INFINITY, false,
                                    Double.POSITIVE_INFINITY, false, CollectionsExt.first(units)); break;
                }
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties("Coordinate_Operation Parameter",
                        epsg, name, null, null, null, isReversible, deprecated);
                properties.put(Identifier.DESCRIPTION_KEY, description);
                final var descriptor = new DefaultParameterDescriptor<>(properties, 1, 1, type, valueDomain, null, null);
                returnValue = ensureSingleton(descriptor, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(OperationMethod.class, code, exception);
        }
        if (returnValue == null) {
             throw noSuchAuthorityCode(OperationMethod.class, code);
        }
        return returnValue;
    }

    /**
     * Returns all parameter descriptors for the specified method.
     *
     * @param  method  the operation method code.
     * @return the parameter descriptors.
     * @throws SQLException if a SQL statement failed.
     */
    private ParameterDescriptor<?>[] createParameterDescriptors(final Integer method) throws FactoryException, SQLException {
        final var descriptors = new ArrayList<ParameterDescriptor<?>>();
        try (ResultSet result = executeQuery("Coordinate_Operation Parameter Usage",
                "SELECT PARAMETER_CODE" +
                " FROM \"Coordinate_Operation Parameter Usage\"" +
                " WHERE COORD_OP_METHOD_CODE = ?" +
                " ORDER BY SORT_ORDER", method))
        {
            while (result.next()) {
                descriptors.add(owner.createParameterDescriptor(getString(method, result, 1)));
            }
        }
        return descriptors.toArray(ParameterDescriptor<?>[]::new);
    }

    /**
     * Sets the values of all parameters in the given group.
     *
     * @param  method      the EPSG code for the operation method.
     * @param  operation   the EPSG code for the operation (conversion or transformation).
     * @param  parameters  the parameter values to fill.
     * @throws SQLException if a SQL statement failed.
     */
    private void fillParameterValues(final Integer method, final Integer operation, final ParameterValueGroup parameters)
            throws FactoryException, SQLException
    {
        try (ResultSet result = executeQuery("Coordinate_Operation Parameter Value",
                "SELECT CP.PARAMETER_NAME," +
                      " CV.PARAMETER_VALUE," +
                      " CV.PARAM_VALUE_FILE_REF," +
                      " CV.UOM_CODE" +
               " FROM (\"Coordinate_Operation Parameter Value\" AS CV" +
          " INNER JOIN \"Coordinate_Operation Parameter\" AS CP" +
                   " ON CV.PARAMETER_CODE = CP.PARAMETER_CODE)" +
          " INNER JOIN \"Coordinate_Operation Parameter Usage\" AS CU" +
                  " ON (CP.PARAMETER_CODE = CU.PARAMETER_CODE)" +
                 " AND (CV.COORD_OP_METHOD_CODE = CU.COORD_OP_METHOD_CODE)" +
                " WHERE CV.COORD_OP_METHOD_CODE = ?" +
                  " AND CV.COORD_OP_CODE = ?" +
             " ORDER BY CU.SORT_ORDER", method, operation))
        {
            while (result.next()) {
                final String name = getString(operation, result, 1);
                final ParameterValue<?> param;
                try {
                    param = parameters.parameter(name);
                } catch (ParameterNotFoundException exception) {
                    /*
                     * Wrap the unchecked ParameterNotFoundException into the checked NoSuchIdentifierException,
                     * which is a FactoryException subclass.  Note that in principle, NoSuchIdentifierException is for
                     * MathTransforms rather than parameters. However, we are close in spirit here since we are setting
                     * up MathTransform's parameters. Using NoSuchIdentifierException allows CoordinateOperationSet to
                     * know that the failure is probably caused by a MathTransform not yet supported in Apache SIS
                     * (or only partially supported) rather than some more serious failure in the database side.
                     * Callers can use this information in order to determine if they should try the next coordinate
                     * operation or propagate the exception.
                     */
                    throw (NoSuchIdentifierException) new NoSuchIdentifierException(error().getString(
                            Errors.Keys.CanNotSetParameterValue_1, name), name).initCause(exception);
                }
                final double value = getOptionalDouble(result, 2);
                Unit<?> unit = null;
                String reference;
                if (Double.isNaN(value)) {
                    /*
                     * If no numeric value was provided in the database, then the values should be in
                     * an external file. It may be a file in the "$SIS_DATA/DatumChanges" directory.
                     * The reference file should be relative and _not_ encoded for valid URI syntax.
                     * The encoding will be applied by invoking an `URI` multi-argument constructor.
                     * Note that we must use a multi-arguments constructor, not URI(String), because
                     * the latter assumes an encoded string (which is not the case in EPSG database).
                     */
                    reference = getString(operation, result, 3);
                } else {
                    reference = null;
                    final String unitCode = getOptionalString(result, 4);
                    if (unitCode != null) {
                        unit = owner.createUnit(unitCode);
                        if (Units.UNITY.equals(unit) && param.getUnit() == null) {
                            unit = null;
                        }
                    }
                }
                try {
                    if (reference != null) {
                        param.setValue(new URI(null, reference, null));     // See above comment.
                    } else if (unit != null) {
                        param.setValue(value, unit);
                    } else {
                        param.setValue(value);
                    }
                } catch (RuntimeException | URISyntaxException exception) {
                    // Catch InvalidParameterValueException, ArithmeticException and others.
                    throw new FactoryDataException(error().getString(Errors.Keys.CanNotSetParameterValue_1, name), exception);
                }
            }
        }
    }

    /**
     * Creates description of the algorithm and parameters used to perform a coordinate operation.
     * An {@code OperationMethod} is a kind of metadata: it does not perform any coordinate operation
     * (e.g. map projection) by itself, but tells us what is needed in order to perform such operation.
     *
     * <h4>Examples</h4>
     * Some EPSG codes for operation methods are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>9804</td> <td>Mercator (variant A)</td></tr>
     *   <tr><td>9802</td> <td>Lambert Conic Conformal (2SP)</td></tr>
     *   <tr><td>9810</td> <td>Polar Stereographic (variant A)</td></tr>
     *   <tr><td>9624</td> <td>Affine parametric transformation</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the operation method for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized OperationMethod createOperationMethod(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        OperationMethod returnValue = null;
        try (ResultSet result = executeQuery("Coordinate_Operation Method", "COORD_OP_METHOD_CODE", "COORD_OP_METHOD_NAME",
                "SELECT COORD_OP_METHOD_CODE," +
                      " COORD_OP_METHOD_NAME," +
                      " REMARKS," +
                      " DEPRECATED" +
                 " FROM \"Coordinate_Operation Method\"" +
                " WHERE COORD_OP_METHOD_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final String  remarks    = getOptionalString (result, 3);
                final boolean deprecated = getOptionalBoolean(result, 4);
                final ParameterDescriptor<?>[] descriptors = createParameterDescriptors(epsg);
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties("Coordinate_Operation Method",
                        epsg, name, null, null, null, remarks, deprecated);
                /*
                 * Note: we do not store the formula at this time, because the text is very verbose and rarely used.
                 */
                final var params = new DefaultParameterDescriptorGroup(properties, 1, 1, descriptors);
                final var method = new DefaultOperationMethod(properties, params);
                returnValue = ensureSingleton(method, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(OperationMethod.class, code, exception);
        }
        if (returnValue == null) {
             throw noSuchAuthorityCode(OperationMethod.class, code);
        }
        return returnValue;
    }

    /**
     * Creates an operation for transforming coordinates in the source CRS to coordinates in the target CRS.
     * The returned object will either be a {@link Conversion} or a {@link Transformation}, depending on the code.
     *
     * <h4>Examples</h4>
     * Some EPSG codes for coordinate transformations are:
     *
     * <table class="sis">
     * <caption>EPSG codes examples</caption>
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>1133</td> <td>ED50 to WGS 84 (1)</td></tr>
     *   <tr><td>1241</td> <td>NAD27 to NAD83 (1)</td></tr>
     *   <tr><td>1173</td> <td>NAD27 to WGS 84 (4)</td></tr>
     *   <tr><td>6326</td> <td>NAD83(2011) to NAVD88 height (1)</td></tr>
     * </table>
     *
     * @param  code  value allocated by EPSG.
     * @return the operation for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    @SuppressWarnings("try")    // Explicit call to close() on an auto-closeable resource.
    public synchronized CoordinateOperation createCoordinateOperation(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        CoordinateOperation returnValue = null;
        try {
            try (ResultSet result = executeQuery("Coordinate_Operation", "COORD_OP_CODE", "COORD_OP_NAME",
                    "SELECT COORD_OP_CODE," +
                          " COORD_OP_NAME," +
                          " COORD_OP_TYPE," +
                          " SOURCE_CRS_CODE," +
                          " TARGET_CRS_CODE," +
                          " COORD_OP_METHOD_CODE," +
                          " COORD_TFM_VERSION," +
                          " COORD_OP_ACCURACY," +
                          " AREA_OF_USE_CODE," +    // Deprecated since EPSG version 10 (always null)
                          " COORD_OP_SCOPE," +
                          " REMARKS," +
                          " DEPRECATED" +
                    " FROM \"Coordinate_Operation\"" +
                    " WHERE COORD_OP_CODE = ?", code))
            {
                while (result.next()) {
                    final Integer epsg = getInteger(code, result, 1);
                    final String  name = getString (code, result, 2);
                    final String  type = getString (code, result, 3).toLowerCase(Locale.US);
                    final boolean isTransformation = type.equals("transformation");
                    final boolean isConversion     = type.equals("conversion");
                    final boolean isConcatenated   = type.equals("concatenated operation");
                    final String sourceCode, targetCode;
                    final Integer methodCode;
                    if (isConversion) {
                        sourceCode = getOptionalString(result, 4);      // Optional for conversions, mandatory for all others.
                        targetCode = getOptionalString(result, 5);
                    } else {
                        sourceCode = getString(code, result, 4);
                        targetCode = getString(code, result, 5);
                    }
                    if (isConcatenated) {
                        methodCode = getOptionalInteger(result, 6);     // Not applicable to concatenated operation, mandatory for all others.
                    } else {
                        methodCode = getInteger(code, result, 6);
                    }
                    final String  version    = getOptionalString (result,  7);
                    final double  accuracy   = getOptionalDouble (result,  8);
                    final String  area       = getOptionalString (result,  9);
                    final String  scope      = getOptionalString (result, 10);
                    final String  remarks    = getOptionalString (result, 11);
                    final boolean deprecated = getOptionalBoolean(result, 12);
                    /*
                     * Create the source and target CRS for the codes fetched above.  Those CRS are optional only for
                     * conversions (the above calls to getString(code, result, …) verified that those CRS are defined
                     * for other kinds of operation). Conversions in EPSG database are usually "defining conversions"
                     * without source and target CRS.
                     *
                     * In EPSG database 6.7, all defining conversions are projections and their dimensions are always 2.
                     * However, this default number of dimensions is not generalizable to other kind of operation methods.
                     * For example, the "Geocentric translation" operation method has 3-dimensional source and target CRS.
                     */
                    final CoordinateReferenceSystem sourceCRS, targetCRS;
                    if (sourceCode != null) {
                        sourceCRS = owner.createCoordinateReferenceSystem(sourceCode);
                    } else {
                        sourceCRS = null;
                    }
                    if (targetCode != null) {
                        targetCRS = owner.createCoordinateReferenceSystem(targetCode);
                    } else {
                        targetCRS = null;
                    }
                    /*
                     * Get the operation method. This is mandatory for conversions and transformations
                     * (it was checked by getInteger(code, result, …) above in this method) but optional
                     * for concatenated operations. Fetching parameter values is part of this block.
                     */
                    final boolean       isDeferred = Semaphores.query(Semaphores.METADATA_ONLY);
                    ParameterValueGroup parameters = null;
                    OperationMethod     method     = null;
                    if (methodCode != null && !isDeferred) {
                        method = owner.createOperationMethod(methodCode.toString());
                        parameters = method.getParameters().createValue();
                        fillParameterValues(methodCode, epsg, parameters);
                    }
                    /*
                     * Creates common properties. The `version` and `accuracy` are usually defined
                     * for transformations only. However, we check them for all kind of operations
                     * (including conversions) and copy the information unconditionally if present.
                     *
                     * NOTE: This block must be executed last before object creations below, because
                     *       methods like createCoordinateReferenceSystem and createOperationMethod
                     *       overwrite the properties map.
                     */
                    Map<String,Object> opProperties = createProperties("Coordinate_Operation",
                            epsg, name, null, area, scope, remarks, deprecated);
                    opProperties.put(CoordinateOperation.OPERATION_VERSION_KEY, version);
                    opProperties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY,
                                     PositionalAccuracyConstant.transformation(accuracy));
                    /*
                     * Creates the operation. Conversions should be the only operations allowed to have
                     * null source and target CRS. In such case, the operation is a defining conversion
                     * (usually to be used later as part of a ProjectedCRS creation).
                     */
                    final CoordinateOperation operation;
                    final CoordinateOperationFactory copFactory = owner.copFactory;
                    if (isDeferred) {
                        operation = new DeferredCoordinateOperation(opProperties, sourceCRS, targetCRS, owner);
                    } else if (isConversion && (sourceCRS == null || targetCRS == null)) {
                        operation = copFactory.createDefiningConversion(opProperties, method, parameters);
                    } else if (isConcatenated) {
                        /*
                         * Concatenated operation: we need to close the current result set, because
                         * we are going to invoke this method recursively in the following lines.
                         */
                        result.close();
                        opProperties = new HashMap<>(opProperties);         // Because this class uses a shared map.
                        final var codes = new ArrayList<String>();
                        try (ResultSet cr = executeQuery("Coordinate_Operation Path",
                                "SELECT SINGLE_OPERATION_CODE" +
                                 " FROM \"Coordinate_Operation Path\"" +
                                " WHERE (CONCAT_OPERATION_CODE = ?)" +
                                " ORDER BY OP_PATH_STEP", epsg))
                        {
                            while (cr.next()) {
                                codes.add(getString(code, cr, 1));
                            }
                        }
                        final var operations = new CoordinateOperation[codes.size()];
                        ensureNoCycle(CoordinateOperation.class, epsg);
                        try {
                            for (int i=0; i<operations.length; i++) {
                                operations[i] = owner.createCoordinateOperation(codes.get(i));
                            }
                        } finally {
                            endOfRecursion(CoordinateOperation.class, epsg);
                        }
                        return copFactory.createConcatenatedOperation(opProperties, operations);
                    } else {
                        /*
                         * At this stage, the parameters are ready for use. Create the math transform and wrap it in the
                         * final operation (a Conversion or a Transformation). We need to give to MathTransformFactory
                         * some information about the context (source and target CRS) for allowing the factory to set
                         * the values of above-mentioned implicit parameters (semi-major and semi-minor axis lengths).
                         *
                         * The first special case may be removed in a future SIS version if the missing method is added
                         * to GeoAPI. Actually GeoAPI has a method doing part of the job, but incomplete (e.g. the pure
                         * GeoAPI method cannot handle Molodensky transform because it does not give the target datum).
                         */
                        opProperties = new HashMap<>(opProperties);             // Because this class uses a shared map.
                        final var builder = new ParameterizedTransformBuilder(owner.mtFactory, null);
                        builder.setParameters(parameters, true);
                        builder.setSourceAxes(sourceCRS);
                        builder.setTargetAxes(targetCRS);
                        final MathTransform mt = builder.create();
                        /*
                         * Give a hint to the factory about the type of the coordinate operation. ISO 19111 defines
                         * Conversion and Transformation, but SIS also have more specific sub-types.  We begin with
                         * what we can infer from the EPSG database.  Next, if the SIS MathTransform providers give
                         * more information, then we refine the type.
                         */
                        Class<? extends SingleOperation> opType;
                        if (isTransformation) {
                            opType = Transformation.class;
                        } else if (isConversion) {
                            opType = Conversion.class;
                        } else {
                            opType = SingleOperation.class;
                        }
                        final OperationMethod provider = builder.getMethod().orElse(null);
                        if (provider instanceof DefaultOperationMethod) {                 // SIS-specific
                            final Class<?> s = ((DefaultOperationMethod) provider).getOperationType();
                            if (s != null && opType.isAssignableFrom(s)) {
                                opType = s.asSubclass(SingleOperation.class);
                            }
                        }
                        opProperties.put(CoordinateOperations.OPERATION_TYPE_KEY, opType);
                        opProperties.put(CoordinateOperations.PARAMETERS_KEY, parameters);
                        /*
                         * Following restriction will be removed in a future SIS version if the method is added to GeoAPI.
                         */
                        if (!(copFactory instanceof DefaultCoordinateOperationFactory)) {
                            throw new UnsupportedOperationException(error().getString(
                                    Errors.Keys.UnsupportedImplementation_1, copFactory.getClass()));
                        }
                        operation = ((DefaultCoordinateOperationFactory) copFactory)
                                .createSingleOperation(opProperties, sourceCRS, targetCRS, null, method, mt);
                    }
                    returnValue = ensureSingleton(operation, returnValue, code);
                    if (result.isClosed()) {
                        return returnValue;
                    }
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateOperation.class, code, exception);
        }
        if (returnValue == null) {
             throw noSuchAuthorityCode(CoordinateOperation.class, code);
        }
        return returnValue;
    }

    /**
     * Creates operations from source and target coordinate reference system codes.
     * This method only extract the information explicitly declared in the EPSG database;
     * it does not attempt to infer by itself operations that are not explicitly recorded in the database.
     *
     * <p>The returned set is ordered with the most accurate operations first.
     * Deprecated operations are not included in the set; if a deprecated operation is really wanted,
     * it can be fetched by an explicit call to {@link #createCoordinateOperation(String)}.</p>
     *
     * @param  sourceCRS  coded value of source coordinate reference system.
     * @param  targetCRS  coded value of target coordinate reference system.
     * @return the operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws NoSuchAuthorityCodeException if a specified code was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized Set<CoordinateOperation> createFromCoordinateReferenceSystemCodes(
            final String sourceCRS, final String targetCRS) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        final String label = sourceCRS + " ⇨ " + targetCRS;
        final var set = new CoordinateOperationSet(owner);
        try {
            final int[] pair = toPrimaryKeys(null, null, null, sourceCRS, targetCRS);
            boolean searchTransformations = false;
            do {
                /*
                 * This `do` loop is executed twice: the first time for searching defining conversions, and the second
                 * time for searching all other kinds of operations. Defining conversions are searched first because
                 * they are, by definition, the most accurate operations.
                 */
                final String key, sql;
                if (searchTransformations) {
                    key = "TransformationFromCRS";
                    sql = "SELECT COORD_OP_CODE" +
                          " FROM \"Coordinate_Operation\" AS CO" +
                          " JOIN \"Area\" ON AREA_OF_USE_CODE = AREA_CODE" +
                          " WHERE CO.DEPRECATED=0" +   // Do not put spaces around "=" - SQLTranslator searches for this exact match.
                            " AND SOURCE_CRS_CODE = ?" +
                            " AND TARGET_CRS_CODE = ?" +
                          " ORDER BY COORD_OP_ACCURACY ASC NULLS LAST, " +
                            " (AREA_EAST_BOUND_LON - AREA_WEST_BOUND_LON + CASE WHEN AREA_EAST_BOUND_LON < AREA_WEST_BOUND_LON THEN 360 ELSE 0 END)" +
                          " * (AREA_NORTH_BOUND_LAT - AREA_SOUTH_BOUND_LAT)" +
                          " * COS(RADIANS(AREA_NORTH_BOUND_LAT + AREA_SOUTH_BOUND_LAT)/2) DESC";
                } else {
                    key = "ConversionFromCRS";
                    sql = "SELECT PROJECTION_CONV_CODE" +
                          " FROM \"Coordinate Reference System\"" +
                          " WHERE BASE_CRS_CODE = ?" +
                            " AND COORD_REF_SYS_CODE = ?";
                }
                final Integer targetKey = searchTransformations ? null : pair[1];
                try (ResultSet result = executeQuery(key, sql, pair)) {
                    while (result.next()) {
                        set.addAuthorityCode(getString(label, result, 1), targetKey);
                    }
                }
            } while ((searchTransformations = !searchTransformations) == true);
            /*
             * Search finished. We may have a lot of coordinate operations
             * (e.g. about 40 for "ED50" (EPSG:4230) to "WGS 84" (EPSG:4326)).
             * Alter the ordering using the information supplied in the supersession table.
             */
            final String[] codes = set.getAuthorityCodes();
            if (codes.length > 1 && sort("Coordinate_Operation", codes)) {
                set.setAuthorityCodes(codes);
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateOperation.class, label, exception);
        }
        /*
         * Before to return the set, tests the creation of 1 object in order to report early (i.e. now)
         * any problems with SQL statements. Remaining operations will be created only when first needed.
         */
        if (!Semaphores.query(Semaphores.METADATA_ONLY)) {
            set.resolve(1);
        }
        return set;
    }

    /**
     * Returns a finder which can be used for looking up unidentified objects.
     * The finder tries to fetch a fully {@linkplain AbstractIdentifiedObject identified object} from an incomplete one,
     * for example from an object without "{@code ID[…]}" or "{@code AUTHORITY[…]}" element in <i>Well Known Text</i>.
     *
     * @return a finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder cannot be created.
     */
    @Override
    public IdentifiedObjectFinder newIdentifiedObjectFinder() throws FactoryException {
        try {
            if (connection.isClosed()) {
                throw new FactoryException(error().getString(Errors.Keys.ConnectionClosed));
            }
            return new EPSGCodeFinder(this);
        } catch (SQLException exception) {
            throw new FactoryException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Sorts an array of codes in preference order. This method orders pairwise the codes according the information
     * provided in the supersession table. If the same object is superseded by more than one object, then the most
     * recent one is inserted first. Except for the codes moved as a result of pairwise ordering, this method tries
     * to preserve the old ordering of the supplied codes (since deprecated operations should already be last).
     * The ordering is performed in place.
     *
     * @param table  the table of the objects for which to check for supersession.
     * @param codes  the codes, usually as an array of {@link String}. If the array do not contains string objects,
     *               then the {@link Object#toString()} method must return the code for each element.
     * @return {@code true} if the array changed as a result of this method call.
     */
    final synchronized boolean sort(final String table, final Object[] codes) throws SQLException, FactoryException {
        int iteration = 0;
        do {
            boolean changed = false;
            for (int i=0; i<codes.length; i++) {
                final int code;
                try {
                    code = Integer.parseInt(codes[i].toString());
                } catch (NumberFormatException e) {
                    unexpectedException("sort", e);
                    continue;
                }
                try (ResultSet result = executeMetadataQuery("Supersession",
                        "SELECT SUPERSEDED_BY FROM \"Supersession\"" +
                        " WHERE OBJECT_TABLE_NAME=? AND OBJECT_CODE=?" +
                        " ORDER BY SUPERSESSION_YEAR DESC",
                        translator.toActualTableName(table), code))
                {
                    while (result.next()) {
                        final String replacement = result.getString(1);
                        if (replacement != null) {
                            for (int j=i+1; j<codes.length; j++) {
                                final Object candidate = codes[j];
                                if (replacement.equals(candidate.toString())) {
                                    /*
                                     * Found a code to move in front of the superceded one.
                                     */
                                    System.arraycopy(codes, i, codes, i+1, j-i);
                                    codes[i++] = candidate;
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
            if (!changed) {
                return iteration != 0;
            }
        }
        while (++iteration < Formulas.MAXIMUM_ITERATIONS);      // Arbitrary limit for avoiding never-ending loop.
        return true;
    }

    /**
     * Creates an exception for an unknown authority code.
     * This convenience method is provided for implementation of {@code createFoo(String)} methods.
     *
     * @param  type  the GeoAPI interface that was to be created (e.g. {@code CoordinateReferenceSystem.class}).
     * @param  code  the unknown authority code.
     * @return an exception initialized with an error message built from the specified information.
     */
    private NoSuchAuthorityCodeException noSuchAuthorityCode(final Class<?> type, final String code) {
        return new NoSuchAuthorityCodeException(resources().getString(Resources.Keys.NoSuchAuthorityCode_3,
                Constants.EPSG, type, code), Constants.EPSG, code, code);
    }

    /**
     * Constructs an exception for a database failure.
     */
    final FactoryException databaseFailure(Class<?> type, Comparable<?> code, SQLException cause) {
        return new FactoryException(error().getString(Errors.Keys.DatabaseError_2, type, code), cause);
    }

    /**
     * Minor shortcut for fetching the error resources.
     */
    private Errors error() {
        return Errors.forLocale(getLocale());
    }

    /**
     * Minor shortcut for fetching the resources specific to the {@code org.apache.sis.referencing} module.
     */
    private Resources resources() {
        return Resources.forLocale(getLocale());
    }

    /**
     * Logs a warning about an unexpected but non-fatal exception.
     *
     * @param method     the source method.
     * @param exception  the exception to log.
     */
    private static void unexpectedException(final String method, final Exception exception) {
        Logging.unexpectedException(LOGGER, EPSGDataAccess.class, method, exception);
    }

    /**
     * Returns {@code true} if it is safe to close this factory. This method is invoked indirectly
     * by {@link EPSGFactory} after some timeout in order to release resources.
     * This method will block the disposal if some {@link AuthorityCodes} are still in use.
     *
     * @return {@code true} if this Data Access Object can be closed.
     *
     * @see EPSGFactory#canClose(EPSGDataAccess)
     */
    final synchronized boolean canClose() {
        boolean can = true;
        if (!authorityCodes.isEmpty()) {
            System.gc();                // For cleaning as much weak references as we can before we check them.
            final Iterator<CloseableReference> it = authorityCodes.values().iterator();
            while (it.hasNext()) {
                if (JDK16.refersTo(it.next(), null)) {
                    it.remove();
                } else {
                    /*
                     * A set of authority codes is still in use. We cannot close this factory.
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
     * If this {@code EPSGDataAccess} is used by an {@link EPSGFactory}, then this method
     * will be automatically invoked after some {@linkplain EPSGFactory#getTimeout timeout}.
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
        final Iterator<CloseableReference> it = authorityCodes.values().iterator();
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
                e.addSuppressed(exception);     // Keep the connection thrown by Connection as the main one to report.
            }
        }
        if (exception != null) {
            throw new FactoryException(exception);
        }
    }
}
