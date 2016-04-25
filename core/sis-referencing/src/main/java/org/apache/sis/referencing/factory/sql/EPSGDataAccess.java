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
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import java.text.ParseException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.measure.unit.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;

import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.metadata.TransformationAccuracy;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.referencing.DeprecatedCode;
import org.apache.sis.internal.referencing.EPSGParameterDomain;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.SignReversalComment;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.system.Semaphores;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
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
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;

import static org.apache.sis.util.Utilities.equalsIgnoreMetadata;
import static org.apache.sis.internal.referencing.ServicesForMetadata.CONNECTION;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.JDK7;
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.internal.jdk7.AutoCloseable;
import org.apache.sis.internal.util.StandardDateFormat;
import org.apache.sis.referencing.cs.DefaultParametricCS;
import org.apache.sis.referencing.datum.DefaultParametricDatum;


/**
 * <cite>Data Access Object</cite> (DAO) creating geodetic objects from a JDBC connection to an EPSG database.
 * The EPSG database is freely available at <a href="http://www.epsg.org">http://www.epsg.org</a>.
 * Current version of this class requires EPSG database version 6.6 or above.
 *
 * <div class="section">Object identifier (code or name)</div>
 * EPSG codes are numerical identifiers. For example code 3395 stands for <cite>"WGS 84 / World Mercator"</cite>.
 * Coordinate Reference Objects are normally created from their numerical codes, but this factory accepts also names.
 * For example {@code createProjectedCRS("3395")} and {@code createProjectedCRS("WGS 84 / World Mercator")} both fetch
 * the same object.
 * However, names may be ambiguous since the same name may be used for more than one object.
 * This is the case of <cite>"WGS 84"</cite> for instance.
 * If such an ambiguity is found, an exception will be thrown.
 *
 * <div class="section">Life cycle and caching</div>
 * {@code EPSGDataAccess} instances should be short-lived since they may hold a significant amount of JDBC resources.
 * {@code EPSGDataAccess} instances are created on the fly by {@link EPSGFactory} and closed after a relatively short
 * {@linkplain EPSGFactory#getTimeout timeout}.
 * In addition {@code EPSGFactory} caches the most recently created objects, which reduce greatly
 * the amount of {@code EPSGDataAccess} instantiations (and consequently the amount of database accesses)
 * in the common case where only a few EPSG codes are used by an application.
 * {@code EPSGDataAccess.createFoo(String)} methods do not cache by themselves and query the database on every invocation.
 *
 * <div class="section">SQL dialects</div>
 * Because the primary distribution format for the EPSG dataset is MS-Access, this class uses SQL statements formatted
 * for the MS-Access dialect. For usage with other database softwares like PostgreSQL or Derby, a {@link SQLTranslator}
 * instance is provided to the constructor.
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
@AutoCloseable
public class EPSGDataAccess extends GeodeticAuthorityFactory implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory, Localized
{
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
        final Map<Integer,Integer> m = new HashMap<Integer,Integer>(24);

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
    private final Map<String,PreparedStatement> statements = new HashMap<String,PreparedStatement>();

    /**
     * The set of authority codes for different types. This map is used by the {@link #getAuthorityCodes(Class)}
     * method as a cache for returning the set created in a previous call. We do not want this map to exist for
     * a long time anyway.
     *
     * <p>Note that this {@code EPSGDataAccess} instance can not be closed as long as this map is not empty, since
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
    private final Map<Class<?>, CloseableReference<AuthorityCodes>> authorityCodes = new HashMap<Class<?>, CloseableReference<AuthorityCodes>>();

    /**
     * Cache for axis names. This service is not provided by {@code ConcurrentAuthorityFactory}
     * since {@link AxisName} objects are particular to the EPSG database.
     *
     * @see #getAxisName(int)
     */
    private final Map<Integer,AxisName> axisNames = new HashMap<Integer,AxisName>();

    /**
     * Cache for the number of dimensions of coordinate systems. This service is not provided by
     * {@code ConcurrentAuthorityFactory} since the number of dimension is used internally in this class.
     *
     * @see #getDimensionForCS(int)
     */
    private final Map<Integer,Integer> csDimensions = new HashMap<Integer,Integer>();

    /**
     * Cache for whether conversions are projections. This service is not provided by {@code ConcurrentAuthorityFactory}
     * since the check for conversion type is used internally in this class.
     *
     * @see #isProjection(int)
     */
    private final Map<Integer,Boolean> isProjection = new HashMap<Integer,Boolean>();

    /**
     * Cache of naming systems other than EPSG. There is usually few of them (at most 15).
     * This is used for aliases.
     *
     * @see #createProperties(String, String, String, String, boolean)
     */
    private final Map<String,NameSpace> namingSystems = new HashMap<String,NameSpace>();

    /**
     * The properties to be given the objects to construct.
     * Reused every time {@code createProperties(…)} is invoked.
     */
    private final Map<String,Object> properties = new HashMap<String,Object>();

    /**
     * A safety guard for preventing never-ending loops in recursive calls to some {@code createFoo(String)} methods.
     * Recursivity may happen while creating Bursa-Wolf parameters, projected CRS if the database has erroneous data,
     * compound CRS if there is cycles, or coordinate operations.
     *
     * <div class="note"><b>Example:</b>
     * {@link #createDatum(String)} invokes {@link #createBursaWolfParameters(Integer)}, which creates a target datum.
     * The target datum could have its own Bursa-Wolf parameters, with one of them pointing again to the source datum.
     * </div>
     *
     * Keys are EPSG codes and values are the type of object being constructed (but those values are not yet used).
     */
    private final Map<Integer,Class<?>> safetyGuard = new HashMap<Integer,Class<?>>();

    /**
     * {@code true} for disabling the logging of warnings when this factory creates deprecated objects.
     * This flag should be always {@code false}, except during {@link Finder#find(IdentifiedObject)}
     * execution since that method may temporarily creates deprecated objects which are later discarded.
     * May also be {@code false} when creating base CRS of deprecated projected or derived CRS.
     */
    private transient boolean quiet;

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
     * The translator from the SQL statements using MS-Access dialect
     * to SQL statements using the dialect of the actual database.
     */
    protected final SQLTranslator translator;

    /**
     * Creates a factory using the given connection. The connection will be {@linkplain Connection#close() closed}
     * when this factory will be {@linkplain #close() closed}.
     *
     * <div class="note"><b>API design note:</b>
     * this constructor is protected because {@code EPSGDataAccess} instances should not be created as standalone factories.
     * This constructor is for allowing definition of custom {@code EPSGDataAccess} subclasses, which are then instantiated
     * by the {@link EPSGFactory#newDataAccess(Connection, SQLTranslator)} method of a corresponding custom
     * {@code EPSGFactory} subclass.</div>
     *
     * @param owner       The {@code EPSGFactory} which is creating this Data Access Object (DAO).
     * @param connection  The connection to the underlying EPSG database.
     * @param translator  The translator from the SQL statements using MS-Access dialect
     *                    to SQL statements using the dialect of the actual database.
     *
     * @see EPSGFactory#newDataAccess(Connection, SQLTranslator)
     */
    protected EPSGDataAccess(final EPSGFactory owner, final Connection connection, final SQLTranslator translator) {
        ArgumentChecks.ensureNonNull("connection", connection);
        ArgumentChecks.ensureNonNull("translator", translator);
        this.owner      = owner;
        this.connection = connection;
        this.translator = translator;
        this.namespace  = owner.nameFactory.createNameSpace(
                          owner.nameFactory.createLocalName(null, Constants.IOGP), null);
    }

    /**
     * Returns the locale used by this factory for producing error messages.
     * This locale does not change the way data are read from the EPSG database.
     *
     * @return The locale for error messages.
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
            calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.CANADA);
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
     * {@preformat text
     *   Citation
     *   ├─ Title ……………………………………………………… EPSG Geodetic Parameter Dataset
     *   ├─ Identifier ………………………………………… EPSG
     *   ├─ Online resource (1 of 2)
     *   │  ├─ Linkage ………………………………………… http://epsg-registry.org/
     *   │  └─ Function ……………………………………… Browse
     *   └─ Online resource (2 of 2)
     *      ├─ Linkage ………………………………………… jdbc:derby:/my/path/to/SIS_DATA/Databases/SpatialMetadata
     *      ├─ Description ……………………………… EPSG dataset version 8.9 on “Apache Derby Embedded JDBC Driver” version 10.12.
     *      └─ Function ……………………………………… Connection
     * }
     */
    @Override
    public synchronized Citation getAuthority() {
        /*
         * We do not cache this citation because the caching service is already provided by ConcurrentAuthorityFactory.
         */
        final DefaultCitation c = new DefaultCitation("EPSG Geodetic Parameter Dataset");
        c.setIdentifiers(Collections.singleton(new ImmutableIdentifier(null, null, Constants.EPSG)));
        try {
            /*
             * Get the most recent version number from the history table. We get the date in local timezone
             * instead then UTC because the date is for information purpose only, and the local timezone is
             * more likely to be shown nicely (without artificial hours) to the user.
             */
            final String query = translator.apply("SELECT VERSION_NUMBER, VERSION_DATE FROM [Version History]" +
                                                  " ORDER BY VERSION_DATE DESC, VERSION_HISTORY_CODE DESC");
            String version = null;
            final Statement statement = connection.createStatement();
            final ResultSet result = statement.executeQuery(query);
            try {
                while (result.next()) {
                    version = getOptionalString(result, 1);
                    final Date date = result.getDate(2);                            // Local timezone.
                    if (version != null && date != null) {                          // Paranoiac check.
                        c.setEdition(new SimpleInternationalString(version));
                        c.setEditionDate(date);
                        break;
                    }
                }
            } finally {
                result.close();
                statement.close();
            }
            /*
             * Add some hard-coded links to EPSG resources, and finally add the JDBC driver name and version number.
             * The list last OnlineResource looks like:
             *
             *    Linkage:      jdbc:derby:/my/path/to/SIS_DATA/Databases/SpatialMetadata
             *    Function:     Connection
             *    Description:  EPSG dataset version 8.9 on “Apache Derby Embedded JDBC Driver” version 10.12.
             *
             * TODO: A future version should use Citations.EPSG as a template.
             *       See the "EPSG" case in ServiceForUtility.createCitation(String).
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
                        function = OnLineFunction.valueOf(CONNECTION);
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
                    r.setLinkage(new URI(SQLUtilities.getSimplifiedURL(metadata)));
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
     * <div class="section">Handling of deprecated objects</div>
     * The collection returned by this method gives an enumeration of EPSG codes for valid objects only.
     * The EPSG codes of deprecated objects are not included in iterations, computation of {@code Set.size()} value,
     * {@code Set.toString()} result, <i>etc.</i> with one exception:
     * a call to {@code Set.contains(…)} will return {@code true} if the given identifier exists
     * for a deprecated object, even if that identifier does not show up in iterations.
     *
     * <p>An other point of view could be to said that the returned collection behaves as if the deprecated codes
     * were included in the set but invisible.</p>
     *
     * @param  type The spatial reference objects type (may be {@code Object.class}).
     * @return The set of authority codes for spatial reference objects of the given type (may be an empty set).
     * @throws FactoryException if access to the underlying database failed.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        try {
            return getCodeMap(type).keySet();
        } catch (SQLException exception) {
            throw new FactoryException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Returns a map of EPSG authority codes as keys and object names as values.
     */
    private synchronized Map<String,String> getCodeMap(final Class<?> type) throws SQLException {
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
                        result = new LinkedHashMap<String,String>(result);
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
     * @return Empty set.
     */
    @Override
    public Set<String> getCodeSpaces() {
        return Collections.emptySet();
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
        try {
            for (final TableInfo table : TableInfo.EPSG) {
                final String text = getCodeMap(table.type).get(code);
                if (text != null) {
                    return (table.nameColumn != null) ? new SimpleInternationalString(text) : null;
                }
            }
        } catch (SQLException exception) {
            throw new FactoryException(exception.getLocalizedMessage(), exception);
        }
        throw noSuchAuthorityCode(IdentifiedObject.class, code);
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
     * The default implementation returns {@code true} if all characters are decimal digits 0 to 9.
     *
     * @param  code  The code the inspect.
     * @return {@code true} if the code is probably a primary key.
     * @throws FactoryException if an unexpected error occurred while inspecting the code.
     */
    private boolean isPrimaryKey(final String code) throws FactoryException {
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
     *
     * <div class="note"><b>Note:</b>
     * this method could be seen as the converse of above {@link #getDescriptionText(String)} method.</div>
     *
     * @param  table       The table where the code should appears, or {@code null} if none.
     * @param  codeColumn  The column name for the codes, or {@code null} if none.
     * @param  nameColumn  The column name for the names, or {@code null} if none.
     * @param  codes       The codes or names to convert to primary keys, as an array of length 1 or 2.
     * @return The numerical identifiers (i.e. the table primary key values).
     * @throws SQLException if an error occurred while querying the database.
     */
    private int[] toPrimaryKeys(final String table, final String codeColumn, final String nameColumn, final String... codes)
            throws SQLException, FactoryException
    {
        final int[] primaryKeys = new int[codes.length];
        for (int i=0; i<codes.length; i++) {
            final String code = codes[i];
            if (codeColumn != null && nameColumn != null && !isPrimaryKey(code)) {
                /*
                 * The given string is not a numerical code. Search the value in the database.
                 * If a prepared statement is already available, reuse it providing that it was
                 * created for the current table. Otherwise we will create a new statement.
                 */
                final String KEY = "PrimaryKey";
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
                    statement = connection.prepareStatement(translator.apply(
                            "SELECT " + codeColumn + ", " + nameColumn +
                            " FROM [" + table + "] WHERE " + nameColumn + " LIKE ?"));
                    statements.put(KEY, statement);
                    lastTableForName = table;
                }
                statement.setString(1, toLikePattern(code));
                Integer resolved = null;
                final ResultSet result = statement.executeQuery();
                try {
                    while (result.next()) {
                        if (SQLUtilities.filterFalsePositive(code, result.getString(2))) {
                            resolved = ensureSingleton(getOptionalInteger(result, 1), resolved, code);
                        }
                    }
                } finally {
                    result.close();
                }
                if (resolved != null) {
                    primaryKeys[i] = resolved;
                    continue;
                }
            }
            /*
             * At this point, 'identifier' should be the primary key. It may still be a non-numerical string
             * if we the above code did not found a match in the name column.
             */
            try {
                primaryKeys[i] = Integer.parseInt(code);
            } catch (NumberFormatException e) {
                final NoSuchAuthorityCodeException ne = new NoSuchAuthorityCodeException(error().getString(
                        Errors.Keys.IllegalIdentifierForCodespace_2, Constants.EPSG, code), Constants.EPSG, code);
                ne.initCause(e);
                throw ne;
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
     * @param  table       The table where the code should appears.
     * @param  codeColumn  The column name for the codes, or {@code null} if none.
     * @param  nameColumn  The column name for the names, or {@code null} if none.
     * @param  sql         The SQL statement to use for creating the {@link PreparedStatement} object.
     *                     Will be used only if no prepared statement was already created for the given code.
     * @param  codes       The codes of the object to create, as an array of length 1 or 2.
     * @return The result of the query.
     * @throws SQLException if an error occurred while querying the database.
     */
    private ResultSet executeQuery(final String table, final String codeColumn, final String nameColumn,
            final String sql, final String... codes) throws SQLException, FactoryException
    {
        assert Thread.holdsLock(this);
        assert sql.contains('[' + table + ']') : table;
        assert (codeColumn == null) || sql.contains(codeColumn) || table.equals("Area") : codeColumn;
        assert (nameColumn == null) || sql.contains(nameColumn) || table.equals("Area") : nameColumn;
        return executeQuery(table, sql, toPrimaryKeys(table, codeColumn, nameColumn, codes));
    }

    /**
     * Creates a statement and executes for the given codes. The first code value is assigned to parameter #1,
     * the second code value (if any) is assigned to parameter #2, <i>etc</i>.
     *
     * @param  table A key uniquely identifying the caller (e.g. {@code "Ellipsoid"} for {@link #createEllipsoid(String)}).
     * @param  sql   The SQL statement to use for creating the {@link PreparedStatement} object.
     *               Will be used only if no prepared statement was already created for the specified key.
     * @param  codes The codes of the object to create, as an array of length 1 or 2.
     * @return The result of the query.
     * @throws SQLException if an error occurred while querying the database.
     */
    private ResultSet executeQuery(final String table, final String sql, final int... codes) throws SQLException {
        assert Thread.holdsLock(this);
        PreparedStatement stmt = statements.get(table);
        if (stmt == null) {
            stmt = connection.prepareStatement(translator.apply(sql));
            statements.put(table, stmt);
        }
        // Partial check that the statement is for the right SQL query.
        assert stmt.getParameterMetaData().getParameterCount() == CharSequences.count(sql, '?');
        for (int i=0; i<codes.length; i++) {
            stmt.setInt(i+1, codes[i]);
        }
        return stmt.executeQuery();
    }

    /**
     * Gets the value from the specified {@link ResultSet}, or {@code null} if none.
     *
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @return The string at the specified column, or {@code null}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private static String getOptionalString(final ResultSet result, final int columnIndex) throws SQLException {
        String value = result.getString(columnIndex);
        return (value != null) && !(value = value.trim()).isEmpty() && !result.wasNull() ? value : null;
    }

    /**
     * Gets the value from the specified {@link ResultSet}, or {@code NaN} if none.
     *
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @return The number at the specified column, or {@code NaN}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private static double getOptionalDouble(final ResultSet result, final int columnIndex) throws SQLException {
        final double value = result.getDouble(columnIndex);
        return result.wasNull() ? Double.NaN : value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}, or {@code null} if none.
     *
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @return The integer at the specified column, or {@code null}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private static Integer getOptionalInteger(final ResultSet result, final int columnIndex) throws SQLException {
        final int value = result.getInt(columnIndex);
        return result.wasNull() ? null : value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}, or {@code false} if none.
     * The EPSG database stores boolean values as integers instead than using the SQL type.
     *
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @return The boolean at the specified column, or {@code null}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private boolean getOptionalBoolean(final ResultSet result, final int columnIndex) throws SQLException {
        return translator.useBoolean() ? result.getBoolean(columnIndex) : (result.getInt(columnIndex) != 0);
    }

    /**
     * Formats an error message for an unexpected null value.
     */
    private String nullValue(final ResultSet result, final int columnIndex, final Comparable<?> code) throws SQLException {
        final ResultSetMetaData metadata = result.getMetaData();
        final String column = metadata.getColumnName(columnIndex);
        final String table  = metadata.getTableName (columnIndex);
        result.close();
        return error().getString(Errors.Keys.NullValueInTable_3, table, column, code);
    }

    /**
     * Same as {@link #getString(Comparable, ResultSet, int)},
     * but reports the fault on an alternative column if the value is null.
     */
    private String getString(final String code, final ResultSet result, final int columnIndex, final int columnFault)
            throws SQLException, FactoryDataException
    {
        String value = result.getString(columnIndex);
        if (value == null || (value = value.trim()).isEmpty() || result.wasNull()) {
            throw new FactoryDataException(nullValue(result, columnFault, code));
        }
        return value;
    }

    /**
     * Gets the string from the specified {@link ResultSet}.
     * The string is required to be non-null. A null string will throw an exception.
     *
     * @param  code         The identifier of the record where the string was found.
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @return The string at the specified column.
     * @throws SQLException if an error occurred while querying the database.
     * @throws FactoryDataException if a null value was found.
     */
    private String getString(final Comparable<?> code, final ResultSet result, final int columnIndex)
            throws SQLException, FactoryDataException
    {
        String value = result.getString(columnIndex);
        if (value == null || (value = value.trim()).isEmpty() || result.wasNull()) {
            throw new FactoryDataException(nullValue(result, columnIndex, code));
        }
        return value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}.
     * The value is required to be non-null. A null value (i.e. blank) will throw an exception.
     *
     * @param  code         The identifier of the record where the double was found.
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @return The double at the specified column.
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
     * <p>We return the value as the {@code Integer} wrapper instead than the {@code int} primitive type
     * because the caller will often need that value as an object (for use as key in {@link HashMap}, etc.).</p>
     *
     * @param  code         The identifier of the record where the integer was found.
     * @param  result       The result set to fetch value from.
     * @param  columnIndex  The column index (1-based).
     * @return The integer at the specified column.
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
     * {@preformat java
     *     ensureNoCycle(type, code);
     *     try {
     *         ...
     *     } finally {
     *         endOfRecursive(type, code);
     *     }
     * }
     */
    private void ensureNoCycle(final Class<?> type, final Integer code) throws FactoryException {
        if (JDK8.putIfAbsent(safetyGuard, code, type) != null) {
            throw new FactoryException(error().getString(Errors.Keys.RecursiveCreateCallForCode_2, type, code));
        }
    }

    /**
     * Invoked after the block protected against infinite recursivity.
     */
    private void endOfRecursivity(final Class<?> type, final Integer code) throws FactoryException {
        if (safetyGuard.remove(code) != type) {
            throw new FactoryException(String.valueOf(code));   // Would be an EPSGDataAccess bug if it happen.
        }
    }

    /**
     * Returns {@code true} if the given table {@code name} matches the {@code expected} name.
     * The given {@code name} may be prefixed by {@code "epsg_"} and may contain abbreviations of the full name.
     * For example {@code "epsg_coordoperation"} is considered as a match for {@code "Coordinate_Operation"}.
     *
     * <p>The table name should be one of the values enumerated in the {@code epsg_table_name} type of the
     * {@code EPSG_Prepare.sql} file.</p>
     *
     * @param  expected  The expected table name (e.g. {@code "Coordinate_Operation"}).
     * @param  name      The actual table name.
     * @return Whether the given {@code name} is considered to match the expected name.
     */
    static boolean tableMatches(final String expected, String name) {
        if (name == null) {
            return false;
        }
        if (name.startsWith(SQLTranslator.TABLE_PREFIX)) {
            name = name.substring(SQLTranslator.TABLE_PREFIX.length());
        }
        return CharSequences.isAcronymForWords(name, expected);
    }

    /**
     * Logs a warning saying that the given code is deprecated and returns the code of the proposed replacement.
     *
     * @param  table  The table of the deprecated code.
     * @param  code   The deprecated code.
     * @param  locale The locale for logging messages.
     * @return The proposed replacement (may be the "(none)" text).
     */
    private String getSupersession(final String table, final Integer code, final Locale locale) throws SQLException {
        String reason = null;
        Object replacedBy = null;
        final ResultSet result = executeQuery("Deprecation",
                "SELECT OBJECT_TABLE_NAME, DEPRECATION_REASON, REPLACED_BY" +
                " FROM [Deprecation] WHERE OBJECT_CODE = ?", code);
        try {
            while (result.next()) {
                if (tableMatches(table, result.getString(1))) {
                    reason     = getOptionalString (result, 2);
                    replacedBy = getOptionalInteger(result, 3);
                    break;
                }
            }
        } finally {
            result.close();
        }
        if (replacedBy == null) {
            replacedBy = '(' + Vocabulary.getResources(locale).getString(Vocabulary.Keys.None).toLowerCase(locale) + ')';
        } else {
            replacedBy = replacedBy.toString();
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
        if (!quiet) {
            LogRecord record = Messages.getResources(locale).getLogRecord(Level.WARNING, Messages.Keys.DeprecatedCode_3,
                    Constants.EPSG + DefaultNameSpace.DEFAULT_SEPARATOR + code, replacedBy, reason);
            record.setLoggerName(Loggers.CRS_FACTORY);
            Logging.log(EPSGDataAccess.class, method, record);
        }
        return (String) replacedBy;
    }

    /**
     * Returns the name and aliases for the {@link IdentifiedObject} to construct.
     *
     * @param  table       The table on which a query has been executed.
     * @param  name        The name for the {@link IndentifiedObject} to construct.
     * @param  code        The EPSG code of the object to construct.
     * @param  remarks     Remarks as a {@link String} or {@link InternationalString}, or {@code null} if none.
     * @param  deprecated  {@code true} if the object to create is deprecated.
     * @return The name together with a set of properties.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<String,Object> createProperties(final String table, String name, final Integer code,
            CharSequence remarks, final boolean deprecated) throws SQLException, FactoryDataException
    {
        /*
         * Search for aliases. Note that searching for the object code is not sufficient. We also need to check if the
         * record is really from the table we are looking for since different tables may have objects with the same ID.
         *
         * Some aliases are identical to the name except that some letters are replaced by their accented letters.
         * For example "Reseau Geodesique Francais" → "Réseau Géodésique Français". If we find such alias, replace
         * the name by the alias so we have proper display in user interface. Notes:
         *
         *   - WKT formatting will still be compliant with ISO 19162 because the WKT formatter replaces accented
         *     letters by ASCII ones.
         *   - We do not perform this replacement directly in our EPSG database because ASCII letters are more
         *     convenient for implementing accent-insensitive searches.
         */
        final List<GenericName> aliases = new ArrayList<GenericName>();
        final ResultSet result = executeQuery("Alias",
                "SELECT OBJECT_TABLE_NAME, NAMING_SYSTEM_NAME, ALIAS" +
                " FROM [Alias] INNER JOIN [Naming System]" +
                  " ON [Alias].NAMING_SYSTEM_CODE =" +
                " [Naming System].NAMING_SYSTEM_CODE" +
                " WHERE OBJECT_CODE = ?", code);
        try {
            while (result.next()) {
                if (tableMatches(table, result.getString(1))) {
                    final String naming = getOptionalString(result, 2);
                    final String alias  = getString(code,   result, 3);
                    NameSpace ns = null;
                    if (naming != null) {
                        ns = namingSystems.get(naming);
                        if (ns == null) {
                            ns = owner.nameFactory.createNameSpace(owner.nameFactory.createLocalName(null, naming), null);
                            namingSystems.put(naming, ns);
                        }
                    }
                    if (CharSequences.toASCII(alias).toString().equals(name)) {
                        name = alias;
                    } else {
                        aliases.add(owner.nameFactory.createLocalName(ns, alias));
                    }
                }
            }
        } finally {
            result.close();
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
            properties.put(NamedIdentifier.CODE_KEY,      name);
            properties.put(NamedIdentifier.VERSION_KEY,   version);
            properties.put(NamedIdentifier.AUTHORITY_KEY, authority);
            properties.put(AbstractIdentifiedObject.LOCALE_KEY, locale);
            final NamedIdentifier id = new NamedIdentifier(properties);
            properties.clear();
            properties.put(IdentifiedObject.NAME_KEY, id);
        }
        if (!aliases.isEmpty()) {
            properties.put(IdentifiedObject.ALIAS_KEY, aliases.toArray(new GenericName[aliases.size()]));
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
        properties.put(ReferencingServices.MT_FACTORY, owner.mtFactory);
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
    private Map<String,Object> createProperties(final String table, final String name, final Integer code,
            final String domainCode, String scope, final String remarks, final boolean deprecated)
            throws SQLException, FactoryException
    {
        if ("?".equals(scope)) {                // EPSG sometime uses this value for unspecified scope.
            scope = null;
        }
        final Map<String,Object> properties = createProperties(table, name, code, remarks, deprecated);
        if (domainCode != null) {
            properties.put(Datum.DOMAIN_OF_VALIDITY_KEY, owner.createExtent(domainCode));
        }
        properties.put(Datum.SCOPE_KEY, scope);
        return properties;
    }

    /**
     * Returns a string like the given string but with accented letters replaced by ASCII letters
     * and all characters that are not letter or digit replaced by the wildcard % character.
     *
     * @see SQLUtilities#toLikePattern(String)
     */
    private static String toLikePattern(final String name) {
        return SQLUtilities.toLikePattern(CharSequences.toASCII(name).toString());
    }

    /**
     * Returns an arbitrary object from a code. The default implementation delegates to more specific methods,
     * for example {@link #createCoordinateReferenceSystem(String)}, {@link #createDatum(String)}, <i>etc.</i>
     * until a successful one is found.
     *
     * <p><strong>Note that this method may be ambiguous</strong> since the same EPSG code can be used for different
     * kind of objects. This method throws an exception if it detects an ambiguity on a <em>best-effort</em> basis.
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
                final PreparedStatement stmt = connection.prepareStatement(translator.apply(query.toString()));
                try {
                    /*
                     * Check if at least one record is found for the code or the name.
                     * Ensure that there is not two values for the same code or name.
                     */
                    if (isPrimaryKey) {
                        stmt.setInt(1, pk);
                    } else {
                        stmt.setString(1, toLikePattern(code));
                    }
                    Integer present = null;
                    final ResultSet result = stmt.executeQuery();
                    try {
                        while (result.next()) {
                            if (isPrimaryKey || SQLUtilities.filterFalsePositive(code, result.getString(2))) {
                                present = ensureSingleton(getOptionalInteger(result, 1), present, code);
                            }
                        }
                    } finally {
                        result.close();
                    }
                    if (present != null) {
                        if (found >= 0) {
                            throw new FactoryDataException(error().getString(Errors.Keys.DuplicatedIdentifier_1, code));
                        }
                        found = i;
                    }
                } finally {
                    stmt.close();
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
                case 9:  break; // Can not cast Unit to IdentifiedObject
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
     * <div class="note"><b>Example:</b>
     * some EPSG codes for coordinate reference systems are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Type</th>          <th>Description</th></tr>
     *   <tr><td>4326</td> <td>Geographic</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>4979</td> <td>Geographic 3D</td> <td>World Geodetic System 1984</td></tr>
     *   <tr><td>4978</td> <td>Geocentric</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>3395</td> <td>Projected</td>     <td>WGS 84 / World Mercator</td></tr>
     *   <tr><td>5714</td> <td>Vertical</td>      <td>Mean Sea Level height</td></tr>
     *   <tr><td>6349</td> <td>Compound</td>      <td>NAD83(2011) + NAVD88 height</td></tr>
     *   <tr><td>5800</td> <td>Engineering</td>   <td>Astra Minas Grid</td></tr>
     * </table></div>
     *
     * @param  code Value allocated by EPSG.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized CoordinateReferenceSystem createCoordinateReferenceSystem(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        CoordinateReferenceSystem returnValue = null;
        ResultSet result = null;
        try {
            result = executeQuery("Coordinate Reference System", "COORD_REF_SYS_CODE", "COORD_REF_SYS_NAME",
                "SELECT COORD_REF_SYS_CODE,"          +     // [ 1]
                      " COORD_REF_SYS_NAME,"          +     // [ 2]
                      " AREA_OF_USE_CODE,"            +     // [ 3]
                      " CRS_SCOPE,"                   +     // [ 4]
                      " REMARKS,"                     +     // [ 5]
                      " DEPRECATED,"                  +     // [ 6]
                      " COORD_REF_SYS_KIND,"          +     // [ 7]
                      " COORD_SYS_CODE,"              +     // [ 8] Null for CompoundCRS
                      " DATUM_CODE,"                  +     // [ 9] Null for ProjectedCRS
                      " SOURCE_GEOGCRS_CODE,"         +     // [10] For ProjectedCRS
                      " PROJECTION_CONV_CODE,"        +     // [11] For ProjectedCRS
                      " CMPD_HORIZCRS_CODE,"          +     // [12] For CompoundCRS only
                      " CMPD_VERTCRS_CODE"            +     // [13] For CompoundCRS only
                " FROM [Coordinate Reference System]" +
                " WHERE COORD_REF_SYS_CODE = ?", code);

            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final String  area       = getOptionalString (result, 3);
                final String  scope      = getOptionalString (result, 4);
                final String  remarks    = getOptionalString (result, 5);
                final boolean deprecated = getOptionalBoolean(result, 6);
                final String  type       = getString   (code, result, 7);
                /*
                 * Note: Do not invoke 'createProperties' now, even if we have all required informations,
                 *       because the 'properties' map is going to overwritten by calls to 'createDatum', etc.
                 *
                 * The following switch statement should have a case for all "epsg_crs_kind" values enumerated
                 * in the "EPSG_Prepare.sql" file, except that the values in this Java code are in lower cases.
                 */
                final CRSFactory crsFactory = owner.crsFactory;
                final CoordinateReferenceSystem crs;
                {   // On the JDK7 branch, this is a switch on strings.
                    /* ----------------------------------------------------------------------
                     *   GEOGRAPHIC CRS
                     *
                     *   NOTE: 'createProperties' MUST be invoked after any call to an other
                     *         'createFoo' method. Consequently, do not factor out.
                     * ---------------------------------------------------------------------- */
                    if (type.equalsIgnoreCase("geographic 2d") ||
                        type.equalsIgnoreCase("geographic 3d"))
                    {
                        Integer csCode = getInteger(code, result, 8);
                        if (replaceDeprecatedCS) {
                            csCode = JDK8.getOrDefault(DEPRECATED_CS, csCode, csCode);
                        }
                        final EllipsoidalCS cs = owner.createEllipsoidalCS(csCode.toString());
                        final String datumCode = getOptionalString(result, 9);
                        final GeodeticDatum datum;
                        if (datumCode != null) {
                            datum = owner.createGeodeticDatum(datumCode);
                        } else {
                            final String geoCode = getString(code, result, 10, 9);
                            result.close();     // Must be closed before call to createGeographicCRS(String)
                            ensureNoCycle(GeographicCRS.class, epsg);
                            try {
                                datum = owner.createGeographicCRS(geoCode).getDatum();
                            } finally {
                                endOfRecursivity(GeographicCRS.class, epsg);
                            }
                        }
                        crs = crsFactory.createGeographicCRS(createProperties("Coordinate Reference System",
                                name, epsg, area, scope, remarks, deprecated), datum, cs);
                    }
                    /* ----------------------------------------------------------------------
                     *   PROJECTED CRS
                     *
                     *   NOTE: This method invokes itself indirectly, through createGeographicCRS.
                     *         Consequently we can not use 'result' anymore after this block.
                     * ---------------------------------------------------------------------- */
                    else if (type.equalsIgnoreCase("projected")) {
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
                                throw (NoSuchAuthorityCodeException) noSuchAuthorityCode(Projection.class, opCode).initCause(e);
                            }
                            final CoordinateReferenceSystem baseCRS;
                            final boolean resumeParamChecks;
                            if (!deprecated) {
                                baseCRS = owner.createCoordinateReferenceSystem(geoCode);
                                resumeParamChecks = false;
                            } else {
                                /*
                                 * If the ProjectedCRS is deprecated, one reason among others may be that it uses one of
                                 * the deprecated coordinate systems. Those deprecated CS used non-linear units like DMS.
                                 * Apache SIS can not instantiate a ProjectedCRS when the baseCRS uses such units, so we
                                 * set a flag asking to replace the deprecated CS by a supported one. Since that baseCRS
                                 * would not be exactly as defined by EPSG, we must not cache it because we do not want
                                 * 'owner.createGeographicCRS(geoCode)' to return that modified CRS. Since the same CRS
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
                                resumeParamChecks = !Semaphores.queryAndSet(Semaphores.SUSPEND_PARAMETER_CHECK);
                                // Try block must be immediately after above line (do not insert any code between).
                            }
                            try {
                                /*
                                 * For a ProjectedCRS, the baseCRS is always geographic. So in theory we would not
                                 * need the 'instanceof' check. However the EPSG dataset version 8.9 also uses the
                                 * "projected" type for CRS that are actually derived CRS. See EPSG:5820 and 5821.
                                 */
                                final Map<String, Object> properties = createProperties("Coordinate Reference System",
                                                                        name, epsg, area, scope, remarks, deprecated);
                                if (baseCRS instanceof GeographicCRS) {
                                    crs = crsFactory.createProjectedCRS(properties, (GeographicCRS) baseCRS, op, cs);
                                } else {
                                    crs = crsFactory.createDerivedCRS(properties, baseCRS, op, cs);
                                }
                            } finally {
                                if (resumeParamChecks) {
                                    Semaphores.clear(Semaphores.SUSPEND_PARAMETER_CHECK);
                                }
                            }
                        } finally {
                            endOfRecursivity(ProjectedCRS.class, epsg);
                        }
                    }
                    /* ----------------------------------------------------------------------
                     *   VERTICAL CRS
                     * ---------------------------------------------------------------------- */
                    else if (type.equalsIgnoreCase("vertical")) {
                        final VerticalCS    cs    = owner.createVerticalCS   (getString(code, result, 8));
                        final VerticalDatum datum = owner.createVerticalDatum(getString(code, result, 9));
                        crs = crsFactory.createVerticalCRS(createProperties("Coordinate Reference System",
                                name, epsg, area, scope, remarks, deprecated), datum, cs);
                    }
                    /* ----------------------------------------------------------------------
                     *   TEMPORAL CRS
                     *
                     *   NOTE : The original EPSG database does not define any temporal CRS.
                     *          This block is a SIS-specific extension.
                     * ---------------------------------------------------------------------- */
                    else if (type.equalsIgnoreCase("time") || type.equalsIgnoreCase("temporal")) {
                        final TimeCS        cs    = owner.createTimeCS       (getString(code, result, 8));
                        final TemporalDatum datum = owner.createTemporalDatum(getString(code, result, 9));
                        crs = crsFactory.createTemporalCRS(createProperties("Coordinate Reference System",
                                name, epsg, area, scope, remarks, deprecated), datum, cs);
                    }
                    /* ----------------------------------------------------------------------
                     *   COMPOUND CRS
                     *
                     *   NOTE: This method invokes itself recursively.
                     *         Consequently, we can not use 'result' anymore.
                     * ---------------------------------------------------------------------- */
                    else if (type.equalsIgnoreCase("compound")) {
                        final String code1 = getString(code, result, 12);
                        final String code2 = getString(code, result, 13);
                        result.close();
                        final CoordinateReferenceSystem crs1, crs2;
                        ensureNoCycle(CompoundCRS.class, epsg);
                        try {
                            crs1 = owner.createCoordinateReferenceSystem(code1);
                            crs2 = owner.createCoordinateReferenceSystem(code2);
                        } finally {
                            endOfRecursivity(CompoundCRS.class, epsg);
                        }
                        // Note: Do not invoke 'createProperties' sooner.
                        crs  = crsFactory.createCompoundCRS(createProperties("Coordinate Reference System",
                                name, epsg, area, scope, remarks, deprecated), crs1, crs2);
                    }
                    /* ----------------------------------------------------------------------
                     *   GEOCENTRIC CRS
                     * ---------------------------------------------------------------------- */
                    else if (type.equalsIgnoreCase("geocentric")) {
                        final CoordinateSystem cs = owner.createCoordinateSystem(getString(code, result, 8));
                        final GeodeticDatum datum = owner.createGeodeticDatum   (getString(code, result, 9));
                        final Map<String,Object> properties = createProperties("Coordinate Reference System",
                                name, epsg, area, scope, remarks, deprecated);
                        if (cs instanceof CartesianCS) {
                            crs = crsFactory.createGeocentricCRS(properties, datum, (CartesianCS) cs);
                        } else if (cs instanceof SphericalCS) {
                            crs = crsFactory.createGeocentricCRS(properties, datum, (SphericalCS) cs);
                        } else {
                            throw new FactoryDataException(error().getString(
                                    Errors.Keys.IllegalCoordinateSystem_1, cs.getName()));
                        }
                    }
                    /* ----------------------------------------------------------------------
                     *   ENGINEERING CRS
                     * ---------------------------------------------------------------------- */
                    else if (type.equalsIgnoreCase("engineering")) {
                        final CoordinateSystem cs    = owner.createCoordinateSystem(getString(code, result, 8));
                        final EngineeringDatum datum = owner.createEngineeringDatum(getString(code, result, 9));
                        crs = crsFactory.createEngineeringCRS(createProperties("Coordinate Reference System",
                                name, epsg, area, scope, remarks, deprecated), datum, cs);
                    }
                    /* ----------------------------------------------------------------------
                     *   PARAMETRIC CRS
                     * ---------------------------------------------------------------------- */
                    else if (type.equalsIgnoreCase("engineering")) {
                        final DefaultParametricCS    cs    = owner.createParametricCS   (getString(code, result, 8));
                        final DefaultParametricDatum datum = owner.createParametricDatum(getString(code, result, 9));
                        crs = ReferencingServices.getInstance().createParametricCRS(createProperties("Coordinate Reference System",
                                name, epsg, area, scope, remarks, deprecated), datum, cs, crsFactory);
                    }
                    /* ----------------------------------------------------------------------
                     *   UNKNOWN CRS
                     * ---------------------------------------------------------------------- */
                    else {
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
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
        }
        if (returnValue == null) {
             throw noSuchAuthorityCode(CoordinateReferenceSystem.class, code);
        }
        return returnValue;
    }

    /**
     * Creates an arbitrary datum from a code. The returned object will typically be an
     * instance of {@link GeodeticDatum}, {@link VerticalDatum} or {@link TemporalDatum}.
     *
     * <div class="note"><b>Example:</b>
     * some EPSG codes for datums are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Type</th>        <th>Description</th></tr>
     *   <tr><td>6326</td> <td>Geodetic</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>6322</td> <td>Geodetic</td>    <td>World Geodetic System 1972</td></tr>
     *   <tr><td>1027</td> <td>Vertical</td>    <td>EGM2008 geoid</td></tr>
     *   <tr><td>5100</td> <td>Vertical</td>    <td>Mean Sea Level</td></tr>
     *   <tr><td>9315</td> <td>Engineering</td> <td>Seismic bin grid datum</td></tr>
     * </table></div>
     *
     * @param  code Value allocated by EPSG.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized Datum createDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        Datum returnValue = null;
        ResultSet result = null;
        try {
            result = executeQuery("Datum", "DATUM_CODE", "DATUM_NAME",
                "SELECT DATUM_CODE," +
                      " DATUM_NAME," +
                      " DATUM_TYPE," +
                      " ORIGIN_DESCRIPTION," +
                      " REALIZATION_EPOCH," +
                      " AREA_OF_USE_CODE," +
                      " DATUM_SCOPE," +
                      " REMARKS," +
                      " DEPRECATED," +
                      " ELLIPSOID_CODE," +          // Only for geodetic type
                      " PRIME_MERIDIAN_CODE" +      // Only for geodetic type
                " FROM [Datum]" +
                " WHERE DATUM_CODE = ?", code);

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
                Map<String,Object> properties = createProperties("Datum",
                        name, epsg, area, scope, remarks, deprecated);
                if (anchor != null) {
                    properties.put(Datum.ANCHOR_POINT_KEY, anchor);
                }
                if (epoch != null && !epoch.isEmpty()) try {
                    final int year = Integer.parseInt(epoch);
                    final Calendar calendar = getCalendar();
                    calendar.set(year, 0, 1);
                    properties.put(Datum.REALIZATION_EPOCH_KEY, calendar.getTime());
                } catch (NumberFormatException exception) {
                    unexpectedException("createDatum", exception);          // Not a fatal error.
                }
                /*
                 * The following switch statement should have a case for all "epsg_datum_kind" values enumerated
                 * in the "EPSG_Prepare.sql" file, except that the values in this Java code are in lower cases.
                 */
                final DatumFactory datumFactory = owner.datumFactory;
                final Datum datum;
                {   // On the JDK7 branch, this is a switch on strings.
                    /*
                     * The "geodetic" case invokes createProperties(…) indirectly through calls to
                     * createEllipsoid(String) and createPrimeMeridian(String), so we must protect
                     * the properties map from changes.
                     */
                    if (type.equalsIgnoreCase("geodetic")) {
                        properties = new HashMap<String,Object>(properties);         // Protect from changes
                        final Ellipsoid ellipsoid    = owner.createEllipsoid    (getString(code, result, 10));
                        final PrimeMeridian meridian = owner.createPrimeMeridian(getString(code, result, 11));
                        final BursaWolfParameters[] param = createBursaWolfParameters(meridian, epsg);
                        if (param != null) {
                            properties.put(DefaultGeodeticDatum.BURSA_WOLF_KEY, param);
                        }
                        datum = datumFactory.createGeodeticDatum(properties, ellipsoid, meridian);
                    }
                    /*
                     * Vertical datum type is hard-coded to geoidal. It would be possible to infer other
                     * types by looking at the coordinate system, but it could result in different datum
                     * associated to the same EPSG code.  Since vertical datum type is no longer part of
                     * ISO 19111:2007, it is probably not worth to handle such cases.
                     */
                    else if (type.equalsIgnoreCase("vertical")) {
                        datum = datumFactory.createVerticalDatum(properties, VerticalDatumType.GEOIDAL);
                    }
                    /*
                     * Origin date is stored in ORIGIN_DESCRIPTION field. A column of SQL type
                     * "date" type would have been better, but we do not modify the EPSG model.
                     */
                    else if (type.equalsIgnoreCase("temporal")) {
                        final Date originDate;
                        if (anchor == null || anchor.isEmpty()) {
                            throw new FactoryDataException(error().getString(Errors.Keys.DatumOriginShallBeDate));
                        }
                        if (dateFormat == null) {
                            dateFormat = new StandardDateFormat();
                            dateFormat.setCalendar(getCalendar());          // Use UTC timezone.
                        }
                        try {
                            originDate = dateFormat.parse(anchor);
                        } catch (ParseException e) {
                            throw new FactoryDataException(error().getString(Errors.Keys.DatumOriginShallBeDate), e);
                        }
                        datum = datumFactory.createTemporalDatum(properties, originDate);
                    }
                    /*
                     * Straightforward case.
                     */
                    else if (type.equalsIgnoreCase("engineering")) {
                        datum = datumFactory.createEngineeringDatum(properties);
                    }
                    else if (type.equalsIgnoreCase("parametric")) {
                        datum = ReferencingServices.getInstance().createParametricDatum(properties, datumFactory);
                    }
                    else {
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
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(Datum.class, code);
        }
        return returnValue;
    }

    /**
     * Returns Bursa-Wolf parameters for a geodetic datum. If the specified datum has no conversion informations,
     * then this method returns {@code null}.
     *
     * <p>This method is for compatibility with <cite>Well Known Text</cite> (WKT) version 1 formatting.
     * That legacy format had a {@code TOWGS84} element which needs the information provided by this method.
     * Note that {@code TOWGS84} is a deprecated element as of WKT 2 (ISO 19162).</p>
     *
     * @param  meridian The source datum prime meridian, used for discarding any target datum using a different meridian.
     * @param  code The EPSG code of the source {@link GeodeticDatum}.
     * @return an array of Bursa-Wolf parameters, or {@code null}.
     */
    private BursaWolfParameters[] createBursaWolfParameters(final PrimeMeridian meridian, final Integer code)
            throws SQLException, FactoryException
    {
        /*
         * We do not provide TOWGS84 information for WGS84 itself or for any other datum on our list of target datum,
         * in order to avoid infinite recursivity. The 'ensureNonRecursive' call is an extra safety check which should
         * never fail, unless TARGET_CRS and TARGET_DATUM values do not agree with database content.
         */
        if (code == BursaWolfInfo.TARGET_DATUM) {
            return null;
        }
        final List<BursaWolfInfo> bwInfos = new ArrayList<BursaWolfInfo>();
        ResultSet result = executeQuery("BursaWolfParametersSet",
                "SELECT COORD_OP_CODE," +
                      " COORD_OP_METHOD_CODE," +
                      " TARGET_CRS_CODE," +
                      " AREA_OF_USE_CODE"+
                " FROM [Coordinate_Operation]" +
               " WHERE DEPRECATED=0" +           // Do not put spaces around "=" - SQLTranslator searches for this exact match.
                 " AND TARGET_CRS_CODE = "       + BursaWolfInfo.TARGET_CRS +
                 " AND COORD_OP_METHOD_CODE >= " + BursaWolfInfo.MIN_METHOD_CODE +
                 " AND COORD_OP_METHOD_CODE <= " + BursaWolfInfo.MAX_METHOD_CODE +
                 " AND SOURCE_CRS_CODE IN " +
               "(SELECT COORD_REF_SYS_CODE FROM [Coordinate Reference System] WHERE DATUM_CODE = ?)" +
            " ORDER BY TARGET_CRS_CODE, COORD_OP_ACCURACY, COORD_OP_CODE DESC", code);
        try {
            while (result.next()) {
                final BursaWolfInfo info = new BursaWolfInfo(
                        getInteger(code, result, 1),                // Operation
                        getInteger(code, result, 2),                // Method
                        getInteger(code, result, 3),                // Target datum
                        getInteger(code, result, 4));               // Domain of validity
                if (info.target != code) {                          // Paranoiac check.
                    bwInfos.add(info);
                }
            }
        } finally {
            result.close();
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
        final BursaWolfParameters[] parameters = new BursaWolfParameters[size];
        final Locale locale = getLocale();
        int count = 0;
        for (int i=0; i<size; i++) {
            final BursaWolfInfo info = bwInfos.get(i);
            final GeodeticDatum datum;
            ensureNoCycle(BursaWolfParameters.class, code);    // See comment at the begining of this method.
            try {
                datum = owner.createGeodeticDatum(String.valueOf(info.target));
            } finally {
                endOfRecursivity(BursaWolfParameters.class, code);
            }
            /*
             * Accept only Bursa-Wolf parameters between datum that use the same prime meridian.
             * This is for avoiding ambiguity about whether longitude rotation should be applied
             * before or after the datum change. This check is useless for EPSG dataset 8.9 since
             * all datum seen by this method use Greenwich. But we nevertheless perform this check
             * as a safety for future evolution or customized EPSG dataset.
             */
            if (!equalsIgnoreMetadata(meridian, datum.getPrimeMeridian())) {
                continue;
            }
            final BursaWolfParameters bwp = new BursaWolfParameters(datum, info.getDomainOfValidity(owner));
            result = executeQuery("BursaWolfParameters",
                "SELECT PARAMETER_CODE," +
                      " PARAMETER_VALUE," +
                      " UOM_CODE" +
                " FROM [Coordinate_Operation Parameter Value]" +
                " WHERE COORD_OP_CODE = ?" +
                  " AND COORD_OP_METHOD_CODE = ?", info.operation, info.method);
            try {
                while (result.next()) {
                    BursaWolfInfo.setBursaWolfParameter(bwp,
                            getInteger(info.operation, result, 1),
                            getDouble (info.operation, result, 2),
                            owner.createUnit(getString(info.operation, result, 3)), locale);
                }
            } finally {
                result.close();
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
     * <div class="note"><b>Example:</b>
     * some EPSG codes for ellipsoids are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>7030</td> <td>WGS 84</td></tr>
     *   <tr><td>7034</td> <td>Clarke 1880</td></tr>
     *   <tr><td>7048</td> <td>GRS 1980 Authalic Sphere</td></tr>
     * </table></div>
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
        ResultSet result = null;
        try {
            result = executeQuery("Ellipsoid", "ELLIPSOID_CODE", "ELLIPSOID_NAME",
                "SELECT ELLIPSOID_CODE," +
                      " ELLIPSOID_NAME," +
                      " SEMI_MAJOR_AXIS," +
                      " INV_FLATTENING," +
                      " SEMI_MINOR_AXIS," +
                      " UOM_CODE," +
                      " REMARKS," +
                      " DEPRECATED" +
                " FROM [Ellipsoid]" +
                " WHERE ELLIPSOID_CODE = ?", code);

            while (result.next()) {
                /*
                 * One of 'semiMinorAxis' and 'inverseFlattening' values can be NULL in the database.
                 * Consequently, we don't use 'getString(ResultSet, int)' for those parameters because
                 * we do not want to thrown an exception if a NULL value is found.
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
                final Map<String,Object> properties = createProperties("Ellipsoid", name, epsg, remarks, deprecated);
                final Ellipsoid ellipsoid;
                if (Double.isNaN(inverseFlattening)) {
                    if (Double.isNaN(semiMinorAxis)) {
                        // Both are null, which is not allowed.
                        final String column = result.getMetaData().getColumnName(3);
                        throw new FactoryDataException(error().getString(Errors.Keys.NullValueInTable_3, code, column));
                    } else {
                        // We only have semiMinorAxis defined. It is OK
                        ellipsoid = owner.datumFactory.createEllipsoid(properties, semiMajorAxis, semiMinorAxis, unit);
                    }
                } else {
                    if (!Double.isNaN(semiMinorAxis)) {
                        // Both 'inverseFlattening' and 'semiMinorAxis' are defined.
                        // Log a warning and create the ellipsoid using the inverse flattening.
                        final LogRecord record = Messages.getResources(getLocale()).getLogRecord(Level.WARNING,
                                Messages.Keys.AmbiguousEllipsoid_1, Constants.EPSG + DefaultNameSpace.DEFAULT_SEPARATOR + code);
                        record.setLoggerName(Loggers.CRS_FACTORY);
                        Logging.log(EPSGDataAccess.class, "createEllipsoid", record);
                    }
                    ellipsoid = owner.datumFactory.createFlattenedSphere(properties, semiMajorAxis, inverseFlattening, unit);
                }
                returnValue = ensureSingleton(ellipsoid, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(Ellipsoid.class, code, exception);
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
        }
        if (returnValue == null) {
             throw noSuchAuthorityCode(Ellipsoid.class, code);
        }
        return returnValue;
    }

    /**
     * Creates a prime meridian defining the origin from which longitude values are determined.
     *
     * <div class="note"><b>Example:</b>
     * some EPSG codes for prime meridians are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>8901</td> <td>Greenwich</td></tr>
     *   <tr><td>8903</td> <td>Paris</td></tr>
     *   <tr><td>8904</td> <td>Bogota</td></tr>
     *   <tr><td>8905</td> <td>Madrid</td></tr>
     *   <tr><td>8906</td> <td>Rome</td></tr>
     * </table></div>
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
        ResultSet result = null;
        try {
            result = executeQuery("Prime Meridian", "PRIME_MERIDIAN_CODE", "PRIME_MERIDIAN_NAME",
                "SELECT PRIME_MERIDIAN_CODE," +
                      " PRIME_MERIDIAN_NAME," +
                      " GREENWICH_LONGITUDE," +
                      " UOM_CODE," +
                      " REMARKS," +
                      " DEPRECATED" +
                " FROM [Prime Meridian]" +
                " WHERE PRIME_MERIDIAN_CODE = ?", code);

            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final double  longitude  = getDouble   (code, result, 3);
                final String  unitCode   = getString   (code, result, 4);
                final String  remarks    = getOptionalString (result, 5);
                final boolean deprecated = getOptionalBoolean(result, 6);
                final Unit<Angle> unit = owner.createUnit(unitCode).asType(Angle.class);
                final PrimeMeridian primeMeridian = owner.datumFactory.createPrimeMeridian(
                        createProperties("Prime Meridian", name, epsg, remarks, deprecated), longitude, unit);
                returnValue = ensureSingleton(primeMeridian, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(PrimeMeridian.class, code, exception);
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(PrimeMeridian.class, code);
        }
        return returnValue;
    }

    /**
     * Creates information about spatial, vertical, and temporal extent (usually a domain of validity) from a code.
     *
     * <div class="note"><b>Example:</b>
     * some EPSG codes for extents are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>1262</td> <td>World</td></tr>
     *   <tr><td>3391</td> <td>World - between 80°S and 84°N</td></tr>
     * </table></div>
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
        ResultSet result = null;
        try {
            result = executeQuery("Area", "AREA_CODE", "AREA_NAME",
                "SELECT AREA_OF_USE," +
                      " AREA_SOUTH_BOUND_LAT," +
                      " AREA_NORTH_BOUND_LAT," +
                      " AREA_WEST_BOUND_LON," +
                      " AREA_EAST_BOUND_LON" +
                " FROM [Area]" +
                " WHERE AREA_CODE = ?", code);

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
        } catch (SQLException exception) {
            throw databaseFailure(Extent.class, code, exception);
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
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
     * <div class="note"><b>Example:</b>
     * some EPSG codes for coordinate systems are:
     *
     * <table class="sis" summary="EPSG codes examples">
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
     * </table></div>
     *
     * @param  code Value allocated by EPSG.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized CoordinateSystem createCoordinateSystem(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        CoordinateSystem returnValue = null;
        ResultSet result = null;
        try {
            result = executeQuery("Coordinate System", "COORD_SYS_CODE", "COORD_SYS_NAME",
                "SELECT COORD_SYS_CODE," +
                      " COORD_SYS_NAME," +
                      " COORD_SYS_TYPE," +
                      " DIMENSION," +
                      " REMARKS," +
                      " DEPRECATED" +
                " FROM [Coordinate System]" +
                " WHERE COORD_SYS_CODE = ?", code);

            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final String  type       = getString   (code, result, 3);
                final int     dimension  = getInteger  (code, result, 4);
                final String  remarks    = getOptionalString (result, 5);
                final boolean deprecated = getOptionalBoolean(result, 6);
                final CoordinateSystemAxis[] axes = createCoordinateSystemAxes(epsg, dimension);
                final Map<String,Object> properties = createProperties("Coordinate System", name, epsg, remarks, deprecated);   // Must be after axes.
                /*
                 * The following switch statement should have a case for all "epsg_cs_kind" values enumerated
                 * in the "EPSG_Prepare.sql" file, except that the values in this Java code are in lower cases.
                 */
                final CSFactory csFactory = owner.csFactory;
                CoordinateSystem cs = null;
                {   // On the JDK7 branch, this is a switch on strings.
                    if (type.equalsIgnoreCase(WKTKeywords.ellipsoidal)) {
                        switch (dimension) {
                            case 2: cs = csFactory.createEllipsoidalCS(properties, axes[0], axes[1]); break;
                            case 3: cs = csFactory.createEllipsoidalCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                    }
                    else if (type.equalsIgnoreCase("cartesian")) {          // Need lower-case "c"
                        switch (dimension) {
                            case 2: cs = csFactory.createCartesianCS(properties, axes[0], axes[1]); break;
                            case 3: cs = csFactory.createCartesianCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                    }
                    else if (type.equalsIgnoreCase(WKTKeywords.spherical)) {
                        switch (dimension) {
                            case 3: cs = csFactory.createSphericalCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                    }
                    else if (type.equalsIgnoreCase(WKTKeywords.vertical) || type.equalsIgnoreCase("gravity-related")) {
                        switch (dimension) {
                            case 1: cs = csFactory.createVerticalCS(properties, axes[0]); break;
                        }
                    }
                    else if (type.equalsIgnoreCase("time") || type.equalsIgnoreCase(WKTKeywords.temporal)) {
                        switch (dimension) {
                            case 1: cs = csFactory.createTimeCS(properties, axes[0]); break;
                        }
                    }
                    else if (type.equalsIgnoreCase(WKTKeywords.parametric)) {
                        switch (dimension) {
                            case 1: cs = ReferencingServices.getInstance().createParametricCS(properties, axes[0], csFactory); break;
                        }
                    }
                    else if (type.equalsIgnoreCase(WKTKeywords.linear)) {
                        switch (dimension) {
                            case 1: cs = csFactory.createLinearCS(properties, axes[0]); break;
                        }
                    }
                    else if (type.equalsIgnoreCase(WKTKeywords.polar)) {
                        switch (dimension) {
                            case 2: cs = csFactory.createPolarCS(properties, axes[0], axes[1]); break;
                        }
                    }
                    else if (type.equalsIgnoreCase(WKTKeywords.cylindrical)) {
                        switch (dimension) {
                            case 3: cs = csFactory.createCylindricalCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                    }
                    else if (type.equalsIgnoreCase(WKTKeywords.affine)) {
                        switch (dimension) {
                            case 2: cs = csFactory.createAffineCS(properties, axes[0], axes[1]); break;
                            case 3: cs = csFactory.createAffineCS(properties, axes[0], axes[1], axes[2]); break;
                        }
                    }
                    else {
                        throw new FactoryDataException(error().getString(Errors.Keys.UnknownType_1, type));
                    }
                }
                if (cs == null) {
                    throw new FactoryDataException(error().getString(Errors.Keys.UnexpectedDimensionForCS_1, type));
                }
                returnValue = ensureSingleton(cs, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateSystem.class, code, exception);
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(CoordinateSystem.class, code);
        }
        return returnValue;
    }

    /**
     * Returns the number of dimension for the specified Coordinate System, or {@code null} if not found.
     *
     * @param  cs the EPSG code for the coordinate system.
     * @return The number of dimensions, or {@code null} if not found.
     *
     * @see #getDimensionsForMethod(int)
     */
    private Integer getDimensionForCS(final Integer cs) throws SQLException {
        Integer dimension = csDimensions.get(cs);
        if (dimension == null) {
            final ResultSet result = executeQuery("Dimension",
                    " SELECT COUNT(COORD_AXIS_CODE)" +
                     " FROM [Coordinate Axis]" +
                     " WHERE COORD_SYS_CODE = ?", cs);
            try {
                dimension = result.next() ? result.getInt(1) : 0;
                csDimensions.put(cs, dimension);
            } finally {
                result.close();
            }
        }
        return (dimension != 0) ? dimension : null;
    }

    /**
     * Returns the coordinate system axis from an EPSG code for a {@link CoordinateSystem}.
     *
     * <p><strong>WARNING:</strong> The EPSG database uses "{@code ORDER}" as a column name.
     * This is tolerated by Access, but MySQL does not accept that name.</p>
     *
     * @param  cs the EPSG code for the coordinate system.
     * @param  dimension of the coordinate system, which is also the size of the returned array.
     * @return An array of coordinate system axis.
     * @throws SQLException if an error occurred during database access.
     * @throws FactoryException if the code has not been found.
     */
    private CoordinateSystemAxis[] createCoordinateSystemAxes(final Integer cs, final int dimension)
            throws SQLException, FactoryException
    {
        int i = 0;
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[dimension];
        final ResultSet result = executeQuery("AxisOrder",
                "SELECT COORD_AXIS_CODE" +
                " FROM [Coordinate Axis]" +
                " WHERE COORD_SYS_CODE = ?" +
                " ORDER BY [ORDER]", cs);
        try {
            while (result.next()) {
                final String axis = getString(cs, result, 1);
                if (i < axes.length) {
                    /*
                     * If 'i' is out of bounds, an exception will be thrown after the loop.
                     * We do not want to thrown an ArrayIndexOutOfBoundsException here.
                     */
                    axes[i] = owner.createCoordinateSystemAxis(axis);
                }
                ++i;
            }
        } finally {
            result.close();
        }
        if (i != axes.length) {
            throw new FactoryDataException(error().getString(Errors.Keys.MismatchedDimension_2, axes.length, i));
        }
        return axes;
    }

    /**
     * Creates a coordinate system axis with name, direction, unit and range of values.
     *
     * <div class="note"><b>Example:</b>
     * some EPSG codes for axes are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Description</th>   <th>Unit</th></tr>
     *   <tr><td>106</td>  <td>Latitude (φ)</td>  <td>degree</td></tr>
     *   <tr><td>107</td>  <td>Longitude (λ)</td> <td>degree</td></tr>
     *   <tr><td>1</td>    <td>Easting (E)</td>   <td>metre</td></tr>
     *   <tr><td>2</td>    <td>Northing (N)</td>  <td>metre</td></tr>
     * </table></div>
     *
     * @param  code Value allocated by EPSG.
     * @return The axis for the given code.
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
        ResultSet result = null;
        try {
            result = executeQuery("Coordinate Axis", "COORD_AXIS_CODE", null,
                "SELECT COORD_AXIS_CODE," +
                      " COORD_AXIS_NAME_CODE," +
                      " COORD_AXIS_ORIENTATION," +
                      " COORD_AXIS_ABBREVIATION," +
                      " UOM_CODE" +
                " FROM [Coordinate Axis]" +
               " WHERE COORD_AXIS_CODE = ?", code);

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
                        createProperties("Coordinate Axis", an.name, epsg, an.description, false),
                        abbreviation, direction, owner.createUnit(unit));
                returnValue = ensureSingleton(axis, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateSystemAxis.class, code, exception);
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
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
            final ResultSet result = executeQuery("Coordinate Axis Name",
                    "SELECT COORD_AXIS_NAME, DESCRIPTION, REMARKS" +
                    " FROM [Coordinate Axis Name]" +
                    " WHERE COORD_AXIS_NAME_CODE = ?", code);
            try {
                while (result.next()) {
                    final String name  = getString(code,   result, 1);
                    String description = getOptionalString(result, 2);
                    String remarks     = getOptionalString(result, 3);
                    if (description == null) {
                        description = remarks;
                    } else if (remarks != null) {
                        description += JDK7.lineSeparator() + remarks;
                    }
                    final AxisName axis = new AxisName(name, description);
                    returnValue = ensureSingleton(axis, returnValue, code);
                }
            } finally {
                result.close();
            }
            if (returnValue == null) {
                throw noSuchAuthorityCode(AxisName.class, String.valueOf(code));
            }
            axisNames.put(code, returnValue);
        }
        return returnValue;
    }

    /**
     * Creates an unit of measurement from a code.
     * Current implementation first checks if {@link Units#valueOfEPSG(int)} can provide a hard-coded unit
     * for the given code before to try to parse the information found in the database. This is done that
     * way for better support of non-straightforward units like <cite>sexagesimal degrees</cite>
     * (EPSG:9110 and 9111).
     *
     * <div class="note"><b>Example:</b>
     * some EPSG codes for units are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>9002</td> <td>decimal degree</td></tr>
     *   <tr><td>9001</td> <td>metre</td></tr>
     *   <tr><td>9030</td> <td>kilometre</td></tr>
     *   <tr><td>1040</td> <td>second</td></tr>
     *   <tr><td>1029</td> <td>year</td></tr>
     * </table></div>
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
        ResultSet result = null;
        try {
            result = executeQuery("Unit of Measure", "UOM_CODE", "UNIT_OF_MEAS_NAME",
                "SELECT UOM_CODE," +
                      " FACTOR_B," +
                      " FACTOR_C," +
                      " TARGET_UOM_CODE," +
                      " UNIT_OF_MEAS_NAME" +
                " FROM [Unit of Measure]" +
                " WHERE UOM_CODE = ?", code);

            while (result.next()) {
                final int source = getInteger(code,  result, 1);
                final double   b = getOptionalDouble(result, 2);
                final double   c = getOptionalDouble(result, 3);
                final int target = getInteger(code,  result, 4);
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
                Unit<?> unit = Units.valueOfEPSG(source);                           // Check in our list of hard-coded unit codes.
                if (unit == null) {
                    final Unit<?> base = Units.valueOfEPSG(target);
                    if (base != null && !Double.isNaN(b) && !Double.isNaN(c)) {     // May be NaN if the conversion is non-linear.
                        unit = Units.multiply(base, b/c);
                    } else try {
                        unit = Units.valueOf(getString(code, result, 5));           // Try parsing the unit symbol as a fallback.
                    } catch (IllegalArgumentException e) {
                        throw new FactoryDataException(error().getString(Errors.Keys.UnknownUnit_1, code), e);
                    }
                }
                returnValue = ensureSingleton(unit, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(Unit.class, code, exception);
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(Unit.class, code);
        }
        return returnValue;
    }

    /**
     * Creates a definition of a single parameter used by an operation method.
     *
     * <div class="note"><b>Example:</b>
     * some EPSG codes for parameters are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>8801</td> <td>Latitude of natural origin</td></tr>
     *   <tr><td>8802</td> <td>Longitude of natural origin</td></tr>
     *   <tr><td>8805</td> <td>Scale factor at natural origin</td></tr>
     *   <tr><td>8806</td> <td>False easting</td></tr>
     *   <tr><td>8807</td> <td>False northing</td></tr>
     * </table></div>
     *
     * @param  code Value allocated by EPSG.
     * @return The parameter descriptor for the given code.
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
        ResultSet result = null;
        try {
            result = executeQuery("Coordinate_Operation Parameter", "PARAMETER_CODE", "PARAMETER_NAME",
                    "SELECT PARAMETER_CODE," +
                          " PARAMETER_NAME," +
                          " DESCRIPTION," +
                          " DEPRECATED" +
                    " FROM [Coordinate_Operation Parameter]" +
                    " WHERE PARAMETER_CODE = ?", code);

            while (result.next()) {
                final Integer epsg        = getInteger  (code, result, 1);
                final String  name        = getString   (code, result, 2);
                final String  description = getOptionalString (result, 3);
                final boolean deprecated  = getOptionalBoolean(result, 4);
                Class<?> type = Double.class;
                /*
                 * If the parameter appears to have at least one non-null value in the "Parameter File Name" column,
                 * then the type is assumed to be URI as a string. Otherwise, the type is a floating point number.
                 */
                ResultSet r = executeQuery("ParameterType",
                        "SELECT PARAM_VALUE_FILE_REF FROM [Coordinate_Operation Parameter Value]" +
                        " WHERE (PARAMETER_CODE = ?) AND PARAM_VALUE_FILE_REF IS NOT NULL", epsg);
                try {
                    while (r.next()) {
                        String element = getOptionalString(r, 1);
                        if (element != null && !element.isEmpty()) {
                            type = String.class;
                            break;
                        }
                    }
                } finally {
                    r.close();
                }
                /*
                 * Search for units.   We typically have many different units but all of the same dimension
                 * (for example metres, kilometres, feet, etc.). In such case, the units Set will have only
                 * one element and that element will be the most frequently used unit.  But some parameters
                 * accept units of different dimensions.   For example the "Ordinate 1 of evaluation point"
                 * (EPSG:8617) parameter value may be in metres or in degrees.   In such case the units Set
                 * will have two elements.
                 */
                final Set<Unit<?>> units = new LinkedHashSet<Unit<?>>();
                r = executeQuery("ParameterUnit",
                        "SELECT UOM_CODE FROM [Coordinate_Operation Parameter Value]" +
                        " WHERE (PARAMETER_CODE = ?)" +
                        " GROUP BY UOM_CODE" +
                        " ORDER BY COUNT(UOM_CODE) DESC", epsg);
                try {
next:               while (r.next()) {
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
                } finally {
                    r.close();
                }
                /*
                 * Determines if the inverse operation can be performed by reversing the parameter sign.
                 * The EPSG dataset uses "Yes" or "No" value, but SIS scripts use boolean type. We have
                 * to accept both.
                 */
                InternationalString isReversible = null;
                r = executeQuery("ParameterSign",
                        "SELECT DISTINCT PARAM_SIGN_REVERSAL FROM [Coordinate_Operation Parameter Usage]" +
                        " WHERE (PARAMETER_CODE = ?)", epsg);
                try {
                    if (r.next()) {
                        final String v = r.getString(1);
                        if (v != null && !r.next()) {
                            if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equals("1")) {
                                isReversible = SignReversalComment.OPPOSITE;
                            } else if (v.equalsIgnoreCase("false") || v.equalsIgnoreCase("no") || v.equals("0")) {
                                isReversible = SignReversalComment.SAME;
                            }
                        }
                    }
                } finally {
                    r.close();
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
                final Map<String, Object> properties =
                        createProperties("Coordinate_Operation Parameter", name, epsg, isReversible, deprecated);
                properties.put(ImmutableIdentifier.DESCRIPTION_KEY, description);
                @SuppressWarnings({"unchecked", "rawtypes"})
                final ParameterDescriptor<?> descriptor = new DefaultParameterDescriptor(properties,
                        1, 1, type, valueDomain, null, null);
                returnValue = ensureSingleton(descriptor, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(OperationMethod.class, code, exception);
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
        }
        if (returnValue == null) {
             throw noSuchAuthorityCode(OperationMethod.class, code);
        }
        return returnValue;
    }

    /**
     * Returns all parameter descriptors for the specified method.
     *
     * @param  method The operation method code.
     * @return The parameter descriptors.
     * @throws SQLException if a SQL statement failed.
     */
    private ParameterDescriptor<?>[] createParameterDescriptors(final Integer method) throws FactoryException, SQLException {
        final List<ParameterDescriptor<?>> descriptors = new ArrayList<ParameterDescriptor<?>>();
        final ResultSet result = executeQuery("Coordinate_Operation Parameter Usage",
                "SELECT PARAMETER_CODE" +
                " FROM [Coordinate_Operation Parameter Usage]" +
                " WHERE COORD_OP_METHOD_CODE = ?" +
                " ORDER BY SORT_ORDER", method);
        try {
            while (result.next()) {
                descriptors.add(owner.createParameterDescriptor(getString(method, result, 1)));
            }
        } finally {
            result.close();
        }
        return descriptors.toArray(new ParameterDescriptor<?>[descriptors.size()]);
    }

    /**
     * Sets the values of all parameters in the given group.
     *
     * @param  method    The EPSG code for the operation method.
     * @param  operation The EPSG code for the operation (conversion or transformation).
     * @param  value     The parameter values to fill.
     * @throws SQLException if a SQL statement failed.
     */
    private void fillParameterValues(final Integer method, final Integer operation, final ParameterValueGroup parameters)
            throws FactoryException, SQLException
    {
        final ResultSet result = executeQuery("Coordinate_Operation Parameter Value",
                "SELECT CP.PARAMETER_NAME," +
                      " CV.PARAMETER_VALUE," +
                      " CV.PARAM_VALUE_FILE_REF," +
                      " CV.UOM_CODE" +
               " FROM ([Coordinate_Operation Parameter Value] AS CV" +
          " INNER JOIN [Coordinate_Operation Parameter] AS CP" +
                   " ON CV.PARAMETER_CODE = CP.PARAMETER_CODE)" +
          " INNER JOIN [Coordinate_Operation Parameter Usage] AS CU" +
                  " ON (CP.PARAMETER_CODE = CU.PARAMETER_CODE)" +
                 " AND (CV.COORD_OP_METHOD_CODE = CU.COORD_OP_METHOD_CODE)" +
                " WHERE CV.COORD_OP_METHOD_CODE = ?" +
                  " AND CV.COORD_OP_CODE = ?" +
             " ORDER BY CU.SORT_ORDER", method, operation);
        try {
            while (result.next()) {
                final String name  = getString(operation, result, 1);
                final double value = getOptionalDouble(result, 2);
                final Unit<?> unit;
                String reference;
                if (Double.isNaN(value)) {
                    /*
                     * If no numeric values were provided in the database, then the values should be
                     * in some external file. It may be a file in the $SIS_DATA/DatumChanges directory.
                     */
                    reference = getString(operation, result, 3);
                    unit = null;
                } else {
                    reference = null;
                    final String unitCode = getOptionalString(result, 4);
                    unit = (unitCode != null) ? owner.createUnit(unitCode) : null;
                }
                final ParameterValue<?> param;
                try {
                    param = parameters.parameter(name);
                } catch (ParameterNotFoundException exception) {
                    /*
                     * Wrap the unchecked ParameterNotFoundException into the checked NoSuchIdentifierException,
                     * which is a FactoryException subclass.  Note that in principle, NoSuchIdentifierException is for
                     * MathTransforms rather than parameters. However we are close in spirit here since we are setting
                     * up MathTransform's parameters. Using NoSuchIdentifierException allows CoordinateOperationSet to
                     * know that the failure is probably caused by a MathTransform not yet supported in Apache SIS
                     * (or only partially supported) rather than some more serious failure in the database side.
                     * Callers can use this information in order to determine if they should try the next coordinate
                     * operation or propagate the exception.
                     */
                    final NoSuchIdentifierException e = new NoSuchIdentifierException(error()
                            .getString(Errors.Keys.CanNotSetParameterValue_1, name), name);
                    e.initCause(exception);
                    throw e;
                }
                try {
                    if (reference != null) {
                        param.setValue(reference);
                    } else if (unit != null) {
                        param.setValue(value, unit);
                    } else {
                        param.setValue(value);
                    }
                } catch (RuntimeException exception) {  // Catch InvalidParameterValueException, ArithmeticException and others.
                    throw new FactoryDataException(error().getString(Errors.Keys.CanNotSetParameterValue_1, name), exception);
                }
            }
        } finally {
            result.close();
        }
    }

    /**
     * Creates description of the algorithm and parameters used to perform a coordinate operation.
     * An {@code OperationMethod} is a kind of metadata: it does not perform any coordinate operation
     * (e.g. map projection) by itself, but tells us what is needed in order to perform such operation.
     *
     * <div class="note"><b>Example:</b>
     * some EPSG codes for operation methods are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>9804</td> <td>Mercator (variant A)</td></tr>
     *   <tr><td>9802</td> <td>Lambert Conic Conformal (2SP)</td></tr>
     *   <tr><td>9810</td> <td>Polar Stereographic (variant A)</td></tr>
     *   <tr><td>9624</td> <td>Affine parametric transformation</td></tr>
     * </table></div>
     *
     * @param  code Value allocated by EPSG.
     * @return The operation method for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    public synchronized OperationMethod createOperationMethod(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        OperationMethod returnValue = null;
        ResultSet result = null;
        try {
            result = executeQuery("Coordinate_Operation Method", "COORD_OP_METHOD_CODE", "COORD_OP_METHOD_NAME",
                "SELECT COORD_OP_METHOD_CODE," +
                      " COORD_OP_METHOD_NAME," +
                      " REMARKS," +
                      " DEPRECATED" +
                 " FROM [Coordinate_Operation Method]" +
                " WHERE COORD_OP_METHOD_CODE = ?", code);

            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final String  remarks    = getOptionalString (result, 3);
                final boolean deprecated = getOptionalBoolean(result, 4);
                final Integer[] dim = getDimensionsForMethod(epsg);
                final ParameterDescriptor<?>[] descriptors = createParameterDescriptors(epsg);
                Map<String,Object> properties = createProperties("Coordinate_Operation Method", name, epsg, remarks, deprecated);
                // We do not store the formula at this time, because the text is very verbose and rarely used.
                final OperationMethod method = new DefaultOperationMethod(properties, dim[0], dim[1],
                            new DefaultParameterDescriptorGroup(properties, 1, 1, descriptors));
                returnValue = ensureSingleton(method, returnValue, code);
            }
        } catch (SQLException exception) {
            throw databaseFailure(OperationMethod.class, code, exception);
        } finally {
            if (result != null) try {
                result.close();
            } catch (SQLException e) {
                // Suppressed exception on the JDK7 branch.
            }
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
     * <div class="note"><b>Example:</b>
     * some EPSG codes for coordinate transformations are:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th> <th>Description</th></tr>
     *   <tr><td>1133</td> <td>ED50 to WGS 84 (1)</td></tr>
     *   <tr><td>1241</td> <td>NAD27 to NAD83 (1)</td></tr>
     *   <tr><td>1173</td> <td>NAD27 to WGS 84 (4)</td></tr>
     *   <tr><td>6326</td> <td>NAD83(2011) to NAVD88 height (1)</td></tr>
     * </table></div>
     *
     * @param  code Value allocated by EPSG.
     * @return The operation for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    @Override
    @SuppressWarnings("null")
    public synchronized CoordinateOperation createCoordinateOperation(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        CoordinateOperation returnValue = null;
        try {
            final ResultSet result = executeQuery("Coordinate_Operation", "COORD_OP_CODE", "COORD_OP_NAME",
                    "SELECT COORD_OP_CODE," +
                          " COORD_OP_NAME," +
                          " COORD_OP_TYPE," +
                          " SOURCE_CRS_CODE," +
                          " TARGET_CRS_CODE," +
                          " COORD_OP_METHOD_CODE," +
                          " COORD_TFM_VERSION," +
                          " COORD_OP_ACCURACY," +
                          " AREA_OF_USE_CODE," +
                          " COORD_OP_SCOPE," +
                          " REMARKS," +
                          " DEPRECATED" +
                    " FROM [Coordinate_Operation]" +
                    " WHERE COORD_OP_CODE = ?", code);

            try {
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
                     * For example the "Geocentric translation" operation method has 3-dimensional source and target CRS.
                     */
                    boolean isDimensionKnown = true;
                    final int sourceDimensions, targetDimensions;
                    final CoordinateReferenceSystem sourceCRS, targetCRS;
                    if (sourceCode != null) {
                        sourceCRS = owner.createCoordinateReferenceSystem(sourceCode);
                        sourceDimensions = sourceCRS.getCoordinateSystem().getDimension();
                    } else {
                        sourceCRS = null;
                        sourceDimensions = 2;           // Acceptable default for projections only.
                        isDimensionKnown = false;
                    }
                    if (targetCode != null) {
                        targetCRS = owner.createCoordinateReferenceSystem(targetCode);
                        targetDimensions = targetCRS.getCoordinateSystem().getDimension();
                    } else {
                        targetCRS = null;
                        targetDimensions = 2;           // Acceptable default for projections only.
                        isDimensionKnown = false;
                    }
                    /*
                     * Get the operation method. This is mandatory for conversions and transformations
                     * (it was checked by getInteger(code, result, …) above in this method) but optional
                     * for concatenated operations. Fetching parameter values is part of this block.
                     */
                    OperationMethod method;
                    ParameterValueGroup parameters;
                    if (methodCode == null) {
                        method      = null;
                        parameters  = null;
                    } else {
                        method = owner.createOperationMethod(methodCode.toString());
                        if (isDimensionKnown) {
                            method = DefaultOperationMethod.redimension(method, sourceDimensions, targetDimensions);
                        }
                        parameters = method.getParameters().createValue();
                        fillParameterValues(methodCode, epsg, parameters);
                    }
                    /*
                     * Creates common properties. The 'version' and 'accuracy' are usually defined
                     * for transformations only. However, we check them for all kind of operations
                     * (including conversions) and copy the information unconditionally if present.
                     *
                     * NOTE: This block must be executed last before object creations below, because
                     *       methods like createCoordinateReferenceSystem and createOperationMethod
                     *       overwrite the properties map.
                     */
                    Map<String,Object> opProperties = createProperties("Coordinate_Operation",
                            name, epsg, area, scope, remarks, deprecated);
                    opProperties.put(CoordinateOperation.OPERATION_VERSION_KEY, version);
                    if (!Double.isNaN(accuracy)) {
                        opProperties.put(CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY,
                                TransformationAccuracy.create(accuracy));
                    }
                    /*
                     * Creates the operation. Conversions should be the only operations allowed to have
                     * null source and target CRS. In such case, the operation is a defining conversion
                     * (usually to be used later as part of a ProjectedCRS creation).
                     */
                    final CoordinateOperation operation;
                    final CoordinateOperationFactory copFactory = owner.copFactory;
                    if (isConversion && (sourceCRS == null || targetCRS == null)) {
                        operation = copFactory.createDefiningConversion(opProperties, method, parameters);
                    } else if (isConcatenated) {
                        /*
                         * Concatenated operation: we need to close the current result set, because
                         * we are going to invoke this method recursively in the following lines.
                         */
                        result.close();
                        opProperties = new HashMap<String,Object>(opProperties);     // Because this class uses a shared map.
                        final List<String> codes = new ArrayList<String>();
                        final ResultSet cr = executeQuery("Coordinate_Operation Path",
                                "SELECT SINGLE_OPERATION_CODE" +
                                 " FROM [Coordinate_Operation Path]" +
                                " WHERE (CONCAT_OPERATION_CODE = ?)" +
                                " ORDER BY OP_PATH_STEP", epsg);
                        try {
                            while (cr.next()) {
                                codes.add(getString(code, cr, 1));
                            }
                        } finally {
                            cr.close();
                        }
                        final CoordinateOperation[] operations = new CoordinateOperation[codes.size()];
                        ensureNoCycle(CoordinateOperation.class, epsg);
                        try {
                            for (int i=0; i<operations.length; i++) {
                                operations[i] = owner.createCoordinateOperation(codes.get(i));
                            }
                        } finally {
                            endOfRecursivity(CoordinateOperation.class, epsg);
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
                         * GeoAPI method can not handle Molodensky transform because it does not give the target datum).
                         */
                        final MathTransform mt;
                        final MathTransformFactory mtFactory = owner.mtFactory;
                        if (mtFactory instanceof DefaultMathTransformFactory) {
                            mt = ((DefaultMathTransformFactory) mtFactory).createParameterizedTransform(parameters,
                                    ReferencingUtilities.createTransformContext(sourceCRS, targetCRS, null));
                        } else {
                            // Fallback for non-SIS implementations. Work for map projections but not for Molodensky.
                            mt = mtFactory.createBaseToDerived(sourceCRS, parameters, targetCRS.getCoordinateSystem());
                        }
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
                        final OperationMethod provider = mtFactory.getLastMethodUsed();
                        if (provider instanceof DefaultOperationMethod) {                 // SIS-specific
                            final Class<?> s = ((DefaultOperationMethod) provider).getOperationType();
                            if (s != null && opType.isAssignableFrom(s)) {
                                opType = s.asSubclass(SingleOperation.class);
                            }
                        }
                        opProperties.put(ReferencingServices.OPERATION_TYPE_KEY, opType);
                        opProperties.put(ReferencingServices.PARAMETERS_KEY, parameters);
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
            } finally {
                result.close();
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
     * This method only extract the information explicitely declared in the EPSG database;
     * it does not attempt to infer by itself operations that are not explicitely recorded in the database.
     *
     * <p>The returned set is ordered with the most accurate operations first.</p>
     *
     * @param  sourceCRS  Coded value of source coordinate reference system.
     * @param  targetCRS  Coded value of target coordinate reference system.
     * @return The operations from {@code sourceCRS} to {@code targetCRS}.
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
        final CoordinateOperationSet set = new CoordinateOperationSet(owner);
        try {
            final int[] pair = toPrimaryKeys(null, null, null, sourceCRS, targetCRS);
            boolean searchTransformations = false;
            do {
                /*
                 * This 'do' loop is executed twice: the first time for searching defining conversions, and the second
                 * time for searching all other kind of operations. Defining conversions are searched first because
                 * they are, by definition, the most accurate operations.
                 */
                final String key, sql;
                if (searchTransformations) {
                    key = "TransformationFromCRS";
                    sql = "SELECT COORD_OP_CODE" +
                          " FROM [Coordinate_Operation] AS CO" +
                          " JOIN [Area] ON AREA_OF_USE_CODE = AREA_CODE" +
                          " WHERE SOURCE_CRS_CODE = ?" +
                            " AND TARGET_CRS_CODE = ?" +
                          " ORDER BY ABS(CO.DEPRECATED), COORD_OP_ACCURACY ASC NULLS LAST, " +
                            " (AREA_EAST_BOUND_LON - AREA_WEST_BOUND_LON + CASE WHEN AREA_EAST_BOUND_LON < AREA_WEST_BOUND_LON THEN 360 ELSE 0 END)" +
                          " * (AREA_NORTH_BOUND_LAT - AREA_SOUTH_BOUND_LAT)" +
                          " * COS(RADIANS(AREA_NORTH_BOUND_LAT + AREA_SOUTH_BOUND_LAT)/2) DESC";
                } else {
                    key = "ConversionFromCRS";
                    sql = "SELECT PROJECTION_CONV_CODE" +
                          " FROM [Coordinate Reference System]" +
                          " WHERE SOURCE_GEOGCRS_CODE = ?" +
                            " AND COORD_REF_SYS_CODE = ?";
                }
                final Integer targetKey = searchTransformations ? null : pair[1];
                final ResultSet result = executeQuery(key, sql, pair);
                try {
                    while (result.next()) {
                        set.addAuthorityCode(getString(label, result, 1), targetKey);
                    }
                } finally {
                    result.close();
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
        set.resolve(1);
        return set;
    }

    /**
     * Returns a finder which can be used for looking up unidentified objects.
     * The finder tries to fetch a fully {@linkplain AbstractIdentifiedObject identified object} from an incomplete one,
     * for example from an object without "{@code ID[…]}" or "{@code AUTHORITY[…]}" element in <cite>Well Known Text</cite>.
     *
     * @return A finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder can not be created.
     */
    @Override
    public IdentifiedObjectFinder newIdentifiedObjectFinder() throws FactoryException {
        return new Finder();
    }

    /**
     * An implementation of {@link IdentifiedObjectFinder} which scans over a smaller set of authority codes.
     */
    private final class Finder extends IdentifiedObjectFinder {
        /**
         * Creates a new finder.
         */
        Finder() {
            super(owner);
        }

        /**
         * Searches for the given object with warnings for deprecations temporarily disabled.
         */
        @Override
        public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
            final boolean old = quiet;
            quiet = true;
            try {
                return super.find(object);
            } finally {
                quiet = old;
            }
        }

        /**
         * Returns a set of authority codes that <strong>may</strong> identify the same object than the specified one.
         * This implementation tries to get a smaller set than what {@link EPSGDataAccess#getAuthorityCodes()} would produce.
         * Deprecated objects must be last in iteration order.
         */
        @Override
        protected Set<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
            String select = "COORD_REF_SYS_CODE";
            String from   = "Coordinate Reference System";
            final String where;
            final Set<Number> codes;
            boolean isFloat = false;
            if (object instanceof Ellipsoid) {
                select  = "ELLIPSOID_CODE";
                from    = "Ellipsoid";
                where   = "SEMI_MAJOR_AXIS";
                codes   = Collections.<Number>singleton(((Ellipsoid) object).getSemiMajorAxis());
                isFloat = true;
            } else {
                final IdentifiedObject dependency;
                if (object instanceof GeneralDerivedCRS) {
                    dependency = ((GeneralDerivedCRS) object).getBaseCRS();
                    where      = "SOURCE_GEOGCRS_CODE";
                } else if (object instanceof SingleCRS) {
                    dependency = ((SingleCRS) object).getDatum();
                    where      = "DATUM_CODE";
                } else if (object instanceof GeodeticDatum) {
                    dependency = ((GeodeticDatum) object).getEllipsoid();
                    select     = "DATUM_CODE";
                    from       = "Datum";
                    where      = "ELLIPSOID_CODE";
                } else {
                    // Not a supported type. Returns all codes.
                    return super.getCodeCandidates(object);
                }
                /*
                 * Search for the dependency.  The super.find(…) method performs a check (not documented in public API)
                 * for detecting when it is invoked recursively, which is the case here. Consequently the super.find(…)
                 * behavior below is slightly different than usual: since invoked recursively, super.find(…) checks the
                 * cache of the ConcurrentAuthorityFactory wrapper. If found, the dependency will also be stored in the
                 * cache. This is desirable since this method may be invoked (indirectly) in a loop for many CRS objects
                 * sharing the same CoordinateSystem or Datum dependencies.
                 */
                final boolean previous = isIgnoringAxes();
                final Set<IdentifiedObject> find;
                try {
                    setIgnoringAxes(true);
                    find = find(dependency);
                } finally {
                    setIgnoringAxes(previous);
                }
                codes = new LinkedHashSet<Number>(Containers.hashMapCapacity(find.size()));
                for (final IdentifiedObject dep : find) {
                    Identifier id = IdentifiedObjects.getIdentifier(dep, Citations.EPSG);
                    if (id != null) try {           // Should never be null, but let be safe.
                        codes.add(Integer.parseInt(id.getCode()));
                    } catch (NumberFormatException e) {
                        Logging.recoverableException(Logging.getLogger(Loggers.CRS_FACTORY), Finder.class, "getCodeCandidates", e);
                    }
                }
                codes.remove(null);                 // Paranoiac safety.
                if (codes.isEmpty()) {
                    // Dependency not found.
                    return Collections.emptySet();
                }
            }
            /*
             * Build the SQL statement. The parameters depend on whether the search criterion is an EPSG code
             * or a numeric value.
             *
             * - If EPSG code, there is only one parameter which is the code to search.
             * - If numeric, there is 3 parameters: lower value, upper value, exact value to search.
             */
            final StringBuilder buffer = new StringBuilder(60);
            buffer.append("SELECT ").append(select).append(" FROM [").append(from).append("] WHERE ").append(where);
            if (isFloat) {
                buffer.append(">=? AND ").append(where).append("<=?");
            } else {
                buffer.append("=?");
            }
            buffer.append(getSearchDomain() == Domain.ALL_DATASET
                          ? " ORDER BY ABS(DEPRECATED), "
                          : " AND DEPRECATED=0 ORDER BY ");     // Do not put spaces around "=" - SQLTranslator searches for this exact match.
            if (isFloat) {
                buffer.append("ABS(").append(select).append("-?), ");
            }
            buffer.append(select);          // Only for making order determinist.
            /*
             * Run the SQL statement. The parameter can be any of the following types:
             *
             * - A String, which represent a foreigner key as an integer value.
             *   The search will require an exact match.
             *
             * - A floating point number, in which case the search will be performed
             *   with a tolerance threshold of 1 cm for a planet of the size of Earth.
             */
            final Set<String> result = new LinkedHashSet<String>();       // We need to preserve order in this set.
            try {
                final PreparedStatement s = connection.prepareStatement(translator.apply(buffer.toString()));
                try {
                    for (final Number code : codes) {
                        if (isFloat) {
                            final double value = code.doubleValue();
                            final double tolerance = Math.abs(value * (Formulas.LINEAR_TOLERANCE / ReferencingServices.AUTHALIC_RADIUS));
                            s.setDouble(1, value - tolerance);
                            s.setDouble(2, value + tolerance);
                            s.setDouble(3, value);
                        } else {
                            s.setInt(1, code.intValue());
                        }
                        final ResultSet r = s.executeQuery();
                        try {
                            while (r.next()) {
                                result.add(r.getString(1));
                            }
                        } finally {
                            r.close();
                        }
                    }
                } finally {
                    s.close();
                }
                result.remove(null);    // Should not have null element, but let be safe.
                /*
                 * Sort the result by taking in account the supersession table.
                 */
                if (result.size() > 1) {
                    final Object[] id = result.toArray();
                    if (sort(select, id)) {
                        result.clear();
                        for (final Object c : id) {
                            result.add((String) c);
                        }
                    }
                }
            } catch (SQLException exception) {
                throw databaseFailure(Identifier.class, String.valueOf(CollectionsExt.first(codes)), exception);
            }
            return result;
        }
    }

    /**
     * Returns {@code true} if the {@link CoordinateOperation} for the specified code is a {@link Projection}.
     * The caller must have verified that the designed operation is a {@link Conversion} before to invoke this method.
     *
     * @throws SQLException If an error occurred while querying the database.
     */
    final boolean isProjection(final Integer code) throws SQLException {
        Boolean projection = isProjection.get(code);
        if (projection == null) {
            final ResultSet result = executeQuery("isProjection",
                    "SELECT COORD_REF_SYS_CODE" +
                    " FROM [Coordinate Reference System]" +
                    " WHERE PROJECTION_CONV_CODE = ?" +
                      " AND COORD_REF_SYS_KIND LIKE 'projected%'", code);
            try {
                projection = result.next();
            } finally {
                result.close();
            }
            isProjection.put(code, projection);
        }
        return projection;
    }

    /**
     * Returns the source and target dimensions for the specified method, provided that they are the same
     * for all operations using that method. The returned array has a length of 2 and is never null,
     * but some elements in that array may be null.
     *
     * @param  method  The EPSG code of the operation method for which to get the dimensions.
     * @return The dimensions in an array of length 2.
     *
     * @see #getDimensionForCS(int)
     */
    private Integer[] getDimensionsForMethod(final Integer method) throws SQLException {
        final Integer[] dimensions = new Integer[2];
        final boolean[] differents = new boolean[2];
        int numDifferences = 0;
        boolean projections = false;
        do {
            /*
             * This loop is executed twice. On the first execution, we look for the source and
             * target CRS declared directly in the "Coordinate Operations" table. This applies
             * mostly to coordinate transformations, since those fields are typically empty in
             * the case of projected CRS.
             *
             * In the second execution, we will look for the base geographic CRS and
             * the resulting projected CRS that use the given operation method. This
             * allows us to handle the case of projected CRS (typically 2 dimensional).
             */
            final String key, sql;
            if (!projections) {
                key = "MethodDimensions";
                sql = "SELECT DISTINCT SRC.COORD_SYS_CODE," +
                                     " TGT.COORD_SYS_CODE" +
                      " FROM [Coordinate_Operation] AS CO" +
                " INNER JOIN [Coordinate Reference System] AS SRC ON SRC.COORD_REF_SYS_CODE = CO.SOURCE_CRS_CODE" +
                " INNER JOIN [Coordinate Reference System] AS TGT ON TGT.COORD_REF_SYS_CODE = CO.TARGET_CRS_CODE" +
                      " WHERE CO.DEPRECATED=0 AND COORD_OP_METHOD_CODE = ?";
                // Do not put spaces in "DEPRECATED=0" - SQLTranslator searches for this exact match.
            } else {
                key = "DerivedDimensions";
                sql = "SELECT DISTINCT SRC.COORD_SYS_CODE," +
                                     " TGT.COORD_SYS_CODE" +
                      " FROM [Coordinate Reference System] AS TGT" +
                " INNER JOIN [Coordinate Reference System] AS SRC ON TGT.SOURCE_GEOGCRS_CODE = SRC.COORD_REF_SYS_CODE" +
                " INNER JOIN [Coordinate_Operation] AS CO ON TGT.PROJECTION_CONV_CODE = CO.COORD_OP_CODE" +
                      " WHERE CO.DEPRECATED=0 AND COORD_OP_METHOD_CODE = ?";
            }
            final ResultSet result = executeQuery(key, sql, method);
            try {
                while (result.next()) {
                    for (int i=0; i<dimensions.length; i++) {
                        if (!differents[i]) {   // Not worth to test heterogenous dimensions.
                            final Integer dim = getDimensionForCS(result.getInt(i + 1));
                            if (dim != null) {
                                if (dimensions[i] == null) {
                                    dimensions[i] = dim;
                                } else if (!dim.equals(dimensions[i])) {
                                    dimensions[i] = null;
                                    differents[i] = true;
                                    if (++numDifferences == differents.length) {
                                        // All dimensions have been set to null.
                                        return dimensions;
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                result.close();
            }
        } while ((projections = !projections) == true);
        return dimensions;
    }

    /**
     * Sorts an array of codes in preference order. This method orders pairwise the codes according the information
     * provided in the supersession table. If the same object is superseded by more than one object, then the most
     * recent one is inserted first. Except for the codes moved as a result of pairwise ordering, this method tries
     * to preserve the old ordering of the supplied codes (since deprecated operations should already be last).
     * The ordering is performed in place.
     *
     * @param table The table of the objects for which to check for supersession.
     * @param codes The codes, usually as an array of {@link String}. If the array do not contains string objects,
     *              then the {@link Object#toString()} method must return the code for each element.
     * @return {@code true} if the array changed as a result of this method call.
     */
    final synchronized boolean sort(final String table, final Object[] codes) throws SQLException, FactoryException {
        int iteration = 0;
        do {
            boolean changed = false;
            for (int i=0; i<codes.length; i++) {
                final String code = codes[i].toString();
                final ResultSet result = executeQuery("Supersession", null, null,
                        "SELECT OBJECT_TABLE_NAME, SUPERSEDED_BY" +
                        " FROM [Supersession]" +
                        " WHERE OBJECT_CODE = ?" +
                        " ORDER BY SUPERSESSION_YEAR DESC", code);
                try {
                    while (result.next()) {
                        if (tableMatches(table, result.getString(1))) {
                            final String replacement = result.getString(2);
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
                } finally {
                    result.close();
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
     * @param  type  The GeoAPI interface that was to be created (e.g. {@code CoordinateReferenceSystem.class}).
     * @param  code  The unknown authority code.
     * @return An exception initialized with an error message built from the specified informations.
     */
    private NoSuchAuthorityCodeException noSuchAuthorityCode(final Class<?> type, final String code) {
        return new NoSuchAuthorityCodeException(error().getString(Errors.Keys.NoSuchAuthorityCode_3,
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
        return Errors.getResources(getLocale());
    }

    /**
     * Logs a warning about an unexpected but non-fatal exception.
     *
     * @param method    The source method.
     * @param exception The exception to log.
     */
    private static void unexpectedException(final String method, final Exception exception) {
        Logging.unexpectedException(Logging.getLogger(Loggers.CRS_FACTORY), EPSGDataAccess.class, method, exception);
    }

    /**
     * Returns {@code true} if it is safe to close this factory. This method is invoked indirectly
     * by {@link EPSGFactory} after some timeout in order to release resources.
     * This method will block the disposal if some {@link AuthorityCodes} are still in use.
     */
    final synchronized boolean canClose() {
        boolean can = true;
        if (!authorityCodes.isEmpty()) {
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
     * If this {@code EPSGDataAccess} is used by an {@link EPSGFactory}, then this method
     * will be automatically invoked after some {@linkplain EPSGFactory#getTimeout timeout}.
     *
     * @throws FactoryException if an error occurred while closing the connection.
     *
     * @see #connection
     */
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
                    // exception.addSuppressed(e) on the JDK7 branch.
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
                    // exception.addSuppressed(e) on the JDK7 branch.
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
                // e.addSuppressed(exception) on the JDK7 branch.
            }
        }
        if (exception != null) {
            throw new FactoryException(exception);
        }
    }
}
