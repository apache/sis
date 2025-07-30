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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;
import java.util.Locale;
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
import java.time.Year;
import java.time.temporal.Temporal;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.format.MeasurementParseException;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.util.Factory;
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
import org.apache.sis.referencing.DefaultObjectDomain;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;
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
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Version;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.iso.Types;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
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
     * without unit (i.e., associated to {@link Units#UNITY} in the database).
     */
    private static final Set<Integer> EPSG_CODE_PARAMETERS = Set.of(
        1048,   // The EPSG code for the CRS that should be used to interpolate gridded data.
        1062);  // The EPSG code for the coordinate transformation that may be used to avoid iteration in geocentric translation by grid interpolation methods.

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
     * @param  code  <abbr>EPSG</abbr> code of a coordinate system to potentially replace.
     * @return <abbr>EPSG</abbr> code of the replacement, or {@code code} if there is no replacement to apply.
     *
     * @see #replaceDeprecatedCS
     */
    @Workaround(library = "EPSG:6401-6420", version = "8.9")        // Deprecated in 2002 but still present in 2016.
    static int replaceDeprecatedCS(final int code) {
        if (code == 6402 || (code >= 6405 && code <= 6412)) return 6422;    // Ellipsoidal 2D CS in degrees.
        if (code == 6401 || (code >= 6413 && code <= 6420)) return 6423;    // Ellipsoidal 3D CS in degrees and metres.
        return code;
    }

    /**
     * String sometime used in the <abbr>EPSG</abbr> database for unknown scope.
     * If Apache <abbr>SIS</abbr>, this is replaced by {@code null}.
     */
    private static final String UNKNOWN_SCOPE = "?";

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
     * Cache for axis names, conventional reference systems, realization methods or naming systems.
     * We do not use the shared cache provided by {@code ConcurrentAuthorityFactory} for the following reasons:
     *
     * <ol>
     *   <li>{@link InternationalString} for scopes are read from a table specific to the <abbr>EPSG</abbr> database.</li>
     *   <li>{@link RealizationMethod} codes are read from a table specific to the <abbr>EPSG</abbr> database.</li>
     *   <li>{@link AxisName} objects are specific to the <abbr>EPSG</abbr> authority factory.</li>
     *   <li>Conventional Reference Systems (<abbr>RS</abbr>) do not have a specific GeoAPI type
     *       that we could use in the shared cache. Conventional <abbr>RS</abbr> are represented
     *       in ISO 19111 by instances of the generic {@link IdentifiedObject} type.</li>
     *   <li></li>
     *   <li>{@link NameSpace} objects are read from a table specific to the <abbr>EPSG</abbr> database.</li>
     * </ol>
     *
     * Since we are not using the shared cache, there is a possibility that many objects are created for the same code.
     * However, this duplication should not happen often. For example, each conventional <abbr>RS</abbr> should appears
     * in only one datum ensemble created by {@link #createDatumEnsemble(Integer, Map)}.
     *
     * <p>Keys are {@link Long} except the keys for naming systems which are {@link String}.</p>
     *
     * @see #getAxisName(Integer)
     * @see #getRealizationMethod(Integer)
     * @see #createConventionalRS(Integer)
     * @see #createProperties(String, Integer, String, CharSequence, String, String, CharSequence, boolean)
     */
    private final Map<Object, Object> localCache = new HashMap<>();

    /**
     * Returns a key for use in {@link #localCache}. The {@code type} argument can have any value,
     * provided that all types of object stored in {@link #localCache} use a distinct {@code type}.
     * Usually, all invocations of this method should use its own unique {@code type} value.
     *
     * @param  type  an arbitrary value that identify the type of object.
     * @param  code  <abbr>EPSG</code> code.
     * @return key to use in {@link #localCache}.
     */
    private static Long cacheKey(final int type, final int code) {
        return (((long) type) << Integer.SIZE) | Integer.toUnsignedLong(code);
    }

    /**
     * The properties to be given the objects to construct.
     * Reused every time {@code createProperties(…)} is invoked.
     */
    private final Map<String, Object> properties = new HashMap<>();

    /**
     * A safety guard for preventing never-ending loops in recursive calls to some {@code createFoo(String)} methods.
     * Recursion may theoretically happen during the creation of the following objects:
     *
     * <ul>
     *   <li>projected <abbr>CRS</abbr> if the database contains cycles (it would be an error in the database).</li>
     *   <li>Compound <abbr>CRS</abbr> if the database contains cycles (it would be an error in the database).</li>
     *   <li>Coordinate operations if the database contains cycles (it would be an error in the database).</li>
     *   <li>Extent created by a <abbr>CRS</abbr> as the extent may itself reference a vertical <abbr>CRS</abbr>.</li>
     * </ul>
     *
     * The database distributed by <abbr>EPSG</abbr> avoids the above-cited cycles, for example by setting
     * the <abbr>CRS</abbr> code to {@code null} instead of putting a value that would create a cycle.
     * Apache <abbr>SIS</abbr> nevertheless check by safety, since the database can be user-provided.
     *
     * <p>This is a linked queue. The value of the field is an identification of the most recent query under execution.
     * Older elements of the list are traversed by {@link QueryID#previous}.</p>
     */
    private QueryID currentSingletonQuery;

    /**
     * Identification of a query under execution. The query is identified by a table name and the <abbr>EPSG</abbr>
     * code(s) of the object to search. It is legal to have have two queries in progress on the same table, provided
     * that the <abbr>EPSG</abbr> codes are different. For example, if a projected <abbr>CRS</abbr> is read from the
     * "Coordinate Reference System" table, that query will require another query on the same table but for the base
     * geographic <abbr>CRS</abbr>. Therefore, a limited form of recursive queries need to be accepted, but we want
     * to make sure that the application does not fall in an infinite recursive loop by requesting the same object.
     *
     * <p>This class behaves as a linked queue. The length of this queue should be small,
     * so it is probably not worth to use an hash map for {@link #isAlreadyInProgress()}.</p>
     */
    private static final class QueryID {
        /** The table being queried. */
        private final String table;

        /** The codes of the requested object as an array of 1 or 2 elements. */
        private final int[] codes;

        /** The previous (older) query in the queue, or {@code null} if none. */
        final QueryID previous;

        /** Creates a new query identification. */
        QueryID(final String table, final int[] codes, final QueryID previous) {
            this.table    = table;
            this.codes    = codes;
            this.previous = previous;
        }

        /** Returns whether the same query is already in progress. */
        final boolean isAlreadyInProgress() {
            for (QueryID p = previous; p != null; p = p.previous) {
                if (table.equals(p.table) && Arrays.equals(codes, p.codes)) {
                    return true;
                }
            }
            return false;
        }

        /** Returns a string representation for debugging purposes. */
        @Override public String toString() {
            return table + ": " + Arrays.toString(codes);
        }
    }

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
     * @see #replaceDeprecatedCS(int)
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
        final var c = new DefaultCitation(Citations.EPSG);
        try {
            /*
             * Get the most recent version number from the history table. We get the date in local timezone
             * instead of UTC because the date is for information purpose only, and the local timezone is
             * more likely to be shown nicely (without artificial hours) to the user.
             */
            final String query = translator.apply("SELECT VERSION_NUMBER, VERSION_DATE FROM \"Version History\""
                                                + " ORDER BY VERSION_DATE DESC, VERSION_HISTORY_CODE DESC");
            String version = null;
            try (Statement stmt = connection.createStatement();
                 ResultSet result = stmt.executeQuery(query))
            {
                while (result.next()) {
                    version = getOptionalString(result, 1);
                    final Date date = result.getDate(2);        // Local timezone.
                    if (version != null && date != null) {      // Paranoiac check.
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
             */
            final DatabaseMetaData metadata  = connection.getMetaData();
            final InternationalString description = Resources.formatInternational(
                    Resources.Keys.GeodeticDataBase_4,
                    Constants.EPSG,
                    version,
                    metadata.getDatabaseProductName(),
                    Version.valueOf(metadata.getDatabaseMajorVersion(),
                                    metadata.getDatabaseMinorVersion()));
            final var r = new DefaultOnlineResource();
            try {
                r.setLinkage(new URI(SQLUtilities.getSimplifiedURL(metadata)));
            } catch (URISyntaxException exception) {
                // May happen if there is spaces in the URI.
                Logging.recoverableException(LOGGER, EPSGDataAccess.class, "getAuthority", exception);
            }
            r.setFunction(OnLineFunction.valueOf(ServicesForMetadata.CONNECTION));
            r.setDescription(description);
            c.getOnlineResources().add(r);
        } catch (SQLException exception) {
            unexpectedException("getAuthority", exception);
        }
        c.transitionTo(DefaultCitation.State.FINAL);
        return c;
    }

    /**
     * Returns the set of authority codes of the given type.
     * This returned set may keep a connection to the <abbr>EPSG</abbr> database.
     * Therefore, the set can execute efficiently idioms like the following one:
     *
     * {@snippet lang="java" :
     *     getAuthorityCodes(type).containsAll(others);
     *     }
     *
     * The returned set should not be referenced for a long time, as it may prevent this factory
     * to release <abbr>JDBC</abbr> resources. If the set of codes is needed for a long time,
     * their values should be copied in another collection object.
     *
     * <h4>Handling of deprecated objects</h4>
     * The collection returned by this method gives an enumeration of <abbr>EPSG</abbr> codes for valid objects only.
     * The <abbr>EPSG</abbr> codes of deprecated objects are not included in iterations, computation
     * of {@code Set.size()} value, {@code Set.toString()} result, <i>etc.</i> with one exception:
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
            throw new FactoryException(exception.getLocalizedMessage(), Exceptions.unwrap(exception));
        }
    }

    /**
     * Returns a map of <abbr>EPSG</abbr> authority codes as keys and object names as values.
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
        Map<String, String> result = Map.of();
        for (final TableInfo table : TableInfo.EPSG) {
            /*
             * We test `isAssignableFrom` in the two ways for catching the following use cases:
             *
             *  - `table.type.isAssignableFrom(type)`
             *    is for the case where a table is for CoordinateReferenceSystem while the user type is some subtype
             *    like GeographicCRS. The GeographicCRS need to be queried into the CoordinateReferenceSystem table.
             *    An additional filter will be applied inside the AuthorityCodes class implementation.
             *
             *  - `type.isAssignableFrom(table.type)`
             *    is for the case where the user type is IdentifiedObject or Object, in which case we basically want
             *    to iterate through every tables.
             */
            if (table.type.isAssignableFrom(type) || type.isAssignableFrom(table.type)) {
                /*
                 * Maybe an instance already existed but was not found above because the user specified some
                 * implementation class instead of an interface class. Before to return a newly created map,
                 * check again in the cached maps using the type computed by AuthorityCodes itself.
                 */
                var codes = new AuthorityCodes(connection, table, type, this);
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
        } catch (SQLException | BackingStoreException exception) {
            throw new FactoryException(exception.getLocalizedMessage(), Exceptions.unwrap(exception));
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
     */
    private static boolean isPrimaryKey(final String code) {
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
     * Converts <abbr>EPSG</abbr> codes or <abbr>EPSG</abbr> names to the numerical identifiers (the primary keys).
     * This method can be seen as the converse of above {@link #getDescriptionText(Class, String)} method.
     *
     * @param  table       the table where the code should appears, or {@code null} if {@code codeColumn} is null.
     * @param  codeColumn  the column name for the codes, or {@code null} if none.
     * @param  nameColumn  the column name for the names, or {@code null} if none.
     * @param  codes       the codes or names to convert to primary keys, as an array of length 1 or 2.
     * @return the numerical identifiers (i.e. the table primary key values).
     * @throws SQLException if an error occurred while querying the database.
     * @throws FactoryDataException if code is a name and two distinct numerical codes match the name.
     * @throws NoSuchAuthorityCodeException if code is a name and no numerical code match the name.
     */
    private int[] toPrimaryKeys(final String table, final String codeColumn, final String nameColumn, final String... codes)
            throws SQLException, FactoryException
    {
        final int[] primaryKeys = new int[codes.length];
next:   for (int i=0; i<codes.length; i++) {
            String code = codes[i];
            if (codeColumn != null && nameColumn != null && !isPrimaryKey(code)) {
                /*
                 * The given string is not a numerical code. Search the value in the database.
                 * We search first in the table of the query. If the name is not found there,
                 * then we will search in the aliases table as a fallback.
                 */
                final String pattern = SQLUtilities.toLikePattern(code, false);
                boolean searchInTableOfQuery = true;
                Integer resolved = null;
                do {    // Executed exactly 1 or 2 times.
                    PreparedStatement stmt;
                    if (searchInTableOfQuery) {
                        /*
                         * The SQL query for searching in the queried table is a little bit more complicated
                         * than the query for searching in the alias table. The existing prepared statement
                         * can be reused only if it was created for the current table.
                         */
                        final String KEY = "PrimaryKey";
                        if (table.equals(lastTableForName)) {
                            stmt = statements.get(KEY);
                        } else {
                            stmt = statements.remove(KEY);
                            if (stmt != null) {
                                stmt.close();
                                stmt = null;
                            }
                        }
                        if (stmt == null) {
                            stmt = connection.prepareStatement(translator.apply(
                                    "SELECT " + codeColumn + ", " + nameColumn
                                            + " FROM \"" + table + '"'
                                            + " WHERE " + nameColumn + " LIKE ?"));
                            statements.put(KEY, stmt);
                            lastTableForName = table;
                        }
                        stmt.setString(1, pattern);
                    } else {
                        /*
                         * If the object name is not found in the queries table,
                         * search in the table of aliases.
                         */
                        stmt = prepareStatement("AliasKey",
                                "SELECT OBJECT_CODE, ALIAS"
                                        + " FROM \"Alias\""
                                        + " WHERE OBJECT_TABLE_NAME=? AND ALIAS LIKE ?");
                        stmt.setString(1, translator.toActualTableName(table));
                        stmt.setString(2, pattern);
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
                        continue next;
                    }
                } while ((searchInTableOfQuery = !searchInTableOfQuery) == false);
            }
            /*
             * At this point, `code` should be the primary key. It may still be a non-numerical string
             * if the user gave a name instead of a code, and the above code did not found a match in
             * the name column or in the alias table.
             */
            try {
                primaryKeys[i] = Integer.parseInt(code);
            } catch (NumberFormatException e) {
                throw (NoSuchAuthorityCodeException) new NoSuchAuthorityCodeException(
                        error().getString(Errors.Keys.IllegalIdentifierForCodespace_2, Constants.EPSG, code),
                        Constants.EPSG,
                        code).initCause(e);
            }
        }
        return primaryKeys;
    }

    /**
     * Creates and executes a statement for the given codes with a protection against infinite loops.
     * The first code value is assigned to parameter #1, the second code value (if any) is assigned to parameter #2,
     * <i>etc</i>. If a given code is not a {@linkplain #isPrimaryKey primary key}, then this method assumes that the
     * code is the object name or an alias and will search for its primary key value.
     *
     * <p>This method is invoked for queries that <em>may</em> cause the same table to be queried again.
     * For example, creating a projected <abbr>CRS</abbr> from the "Coordinate Reference System" table
     * may imply creating a base geographic <abbr>CRS</abbr> from the same table, reusing the statement.
     * If there is two enclosed queries on the same table, the older {@link ResultSet} will be closed.
     * Therefore:</p>
     *
     * <ul>
     *   <li>The caller shall be looking for at most one object, typically by a call to {@link #ensureSingleton}.</li>
     *   <li>It the caller performs a paranoiac check of the rows after the first accepted row, it should check whether
     *       such check is possible with a call to {@link ResultSet#isClosed()}.</li>
     *   <li>All callers shall restore the previous value of {@link #currentSingletonQuery}
     *       in their {@code finally} block.</li>
     * </ul>
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
    private ResultSet executeSingletonQuery(final String    table,
                                            final String    codeColumn,
                                            final String    nameColumn,
                                            final String    sql,
                                            final String... codes)
            throws SQLException, FactoryException
    {
        assert Thread.holdsLock(this);
        assert sql.contains('"' + table + '"') : table;
        assert (codeColumn == null) || sql.contains(codeColumn) || table.equals("Extent") : codeColumn;
        assert (nameColumn == null) || sql.contains(nameColumn) || table.equals("Extent") : nameColumn;
        final int[] keys = toPrimaryKeys(table, codeColumn, nameColumn, codes);
        currentSingletonQuery = new QueryID(table, keys, currentSingletonQuery);
        if (currentSingletonQuery.isAlreadyInProgress()) {
            throw new FactoryDataException(resources().getString(
                    Resources.Keys.RecursiveCreateCallForCode_2,
                    TableInfo.getObjectClassName(table).orElse(table),
                    (codes.length == 1) ? codes[0] : Arrays.toString(codes)));
        }
        return executeQueryForCodes(table, sql, keys);
    }

    /**
     * Creates and executes a statement for the given codes.
     * The first code value is assigned to parameter #1, the second code value (if any) is assigned to parameter #2,
     * <i>etc</i>. If this method is invoked directly, then it should be in context where the caller will not invoke
     * another {@code create(…)} method of this factory.
     *
     * @param  table  a key uniquely identifying the caller.
     * @param  sql    the SQL statement to use for creating the {@link PreparedStatement} object.
     *                Will be used only if no prepared statement was already created for the specified key.
     * @param  codes  the codes of the object to create, as an array of length 1 or 2.
     * @return the result of the query.
     * @throws SQLException if an error occurred while querying the database.
     */
    private ResultSet executeQueryForCodes(final String table, final String sql, final int... codes) throws SQLException {
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
     * Returns the cached statement or creates a new one for the given table.
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
     * Gets the local date or year from the specified {@link ResultSet}, or {@code null} if none.
     * This method parses the date itself from the character string because the column type is sometime declared
     * as {@code VARCHAR} instead of {@code DATE} in the <abbr>SQL</abbr> scripts distributed by <abbr>EPSG</abbr>.
     * The Apache <abbr>SIS</abbr> installer replaces {@code VARCHAR} by {@code DATE}, but we have no guarantee
     * that we are reading an <abbr>EPSG</abbr> database created by our installer. Furthermore, an older version
     * of <abbr>EPSG</abbr> installer was using {@code SMALLINT} instead of {@code DATE},
     * because scripts before <abbr>EPSG</abbr> 9.0 were reporting only the epoch year.
     *
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @param  caller       the caller, used for reporting a warning in case of parsing error.
     * @return the temporal at the specified column, or {@code null}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private static Temporal getOptionalTemporal(final ResultSet result, final int columnIndex, final String caller)
            throws SQLException
    {
        try {
            return LenientDateFormat.parseBest(getOptionalString(result, columnIndex));
        } catch (NumberFormatException exception) {
            unexpectedException(caller, exception);          // Not a fatal error.
        }
        return null;
    }

    /**
     * Gets the epoch from the specified {@link ResultSet}, or {@code null} if none.
     * The column type should be a floating point number.
     *
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @return the epoch at the specified column, or {@code null}.
     * @throws SQLException if an error occurred while querying the database.
     */
    private static Temporal getOptionalEpoch(final ResultSet result, final int columnIndex) throws SQLException {
        final double epoch = getOptionalDouble(result, columnIndex);
        if (Double.isNaN(epoch)) {
            return null;
        }
        final var year = Year.of((int) epoch);
        final long day = Math.round((epoch - year.getValue()) * year.length());
        if (day == 0) {
            return year;
        }
        return year.atMonth(year.atDay(Math.toIntExact(day)).getMonth());
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
     * The EPSG database stores Boolean values as integers instead of using the <abbr>SQL</abbr> type.
     *
     * @param  result       the result set to fetch value from.
     * @param  columnIndex  the column index (1-based).
     * @return the Boolean at the specified column, or {@code null}.
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
        return error().getString(Errors.Keys.NullValueInTable_3, table, column, code);
    }

    /**
     * Same as {@link #getString(Comparable, ResultSet, int)}, but blames another column if the value is null.
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
     * Returns the scope from the given authority code.
     *
     * @param  code  the <abbr>EPSG</code> code.
     * @return the scope, or {@code null} if none.
     */
    private InternationalString getScope(final Integer code) throws SQLException, FactoryDataException {
        final Long cacheKey = cacheKey(1, code);
        var scope = (InternationalString) localCache.get(cacheKey);
        if (scope == null) {
            try (ResultSet result = executeQueryForCodes("Scope", "SELECT SCOPE FROM \"Scope\" WHERE SCOPE_CODE=?", code)) {
                while (result.next()) {
                    String s = result.getString(1);
                    if (!UNKNOWN_SCOPE.equals(s)) {
                        scope = ensureSingleton(Types.toInternationalString(s), scope, code);
                    }
                }
            }
            localCache.put(cacheKey, scope);
        }
        return scope;
    }

    /**
     * Logs a warning saying that the given code is deprecated and returns the code of the proposed replacement.
     *
     * @param  table   the table of the deprecated code.
     * @param  code    the deprecated code.
     * @return the proposed replacement (may be the "(none)" text). Never empty.
     */
    private String getSupersession(final String table, final Integer code, final Locale locale) throws SQLException {
        String reason = null;
        String replacedBy;
search: try (ResultSet result = executeMetadataQuery("Deprecation",
                "SELECT DEPRECATION_REASON, REPLACED_BY"
                        + " FROM \"Deprecation\""
                        + " WHERE OBJECT_TABLE_NAME=?"
                        + " AND OBJECT_CODE=?",
                translator.toActualTableName(table), code))
        {
            while (result.next()) {
                reason    = getOptionalString (result, 1);
                Integer r = getOptionalInteger(result, 2);
                if (r != null) {
                    replacedBy = r.toString();
                    break search;                   // Prefer the first record providing a replacement.
                }
            }
            replacedBy = '(' + Vocabulary.forLocale(locale).getString(Vocabulary.Keys.None).toLowerCase(locale) + ')';
        }
        /*
         * Try to infer the method name from the table name. For example, if the deprecated code was found in
         * the "Coordinate Reference System" table, then we declare `createCoordinateReferenceSystem(String)`
         * as the source of the log message.
         */
        if (!quiet) {
            Logging.completeAndLog(LOGGER,
                    EPSGDataAccess.class,
                    "create".concat(TableInfo.getObjectClassName(table).orElse("")),
                    Resources.forLocale(locale).createLogRecord(
                            Level.WARNING,
                            Resources.Keys.DeprecatedCode_3,
                            Constants.EPSG + Constants.DEFAULT_SEPARATOR + code,
                            replacedBy,
                            reason));
        }
        return replacedBy;
    }

    /**
     * Returns the name and aliases for the {@link IdentifiedObject} to construct.
     *
     * <h4>Possible recursive calls</h4>
     * Invoking this method may cause a recursive call to {@link #createCoordinateReferenceSystem(String)}
     * because this method may create a vertical extent, which may contain a {@link VerticalCRS} property.
     * A recursive <abbr>CRS</abbr> creation may cause some {@link ResultSet}s to be closed when the cached
     * {@link PreparedStatement}s are reused. Callers should use a loop like below:
     *
     * {@snippet lang="java" :
     *     while (result.next()) {   // Expect a singleton, but loop as a safety.
     *         // Get values of columns, then invoke `createProperties(…)` last.
     *         returnValue = ensureSingleton(currentValue, returnValue, code);
     *         if (result.isClosed()) break;
     *     }
     *     }
     *
     * @param  table       the table on which a query has been executed.
     * @param  code        the EPSG code of the object to construct.
     * @param  name        the name for the {@link IdentifiedObject} to construct.
     * @param  description a description associated with the name, or {@code null} if none.
     * @param  extentCode  extent code, or {@code null} if none. This is a legacy of <abbr>EPSG</abbr> version 9.
     * @param  scope       the scope, or {@code null} if none. This is a legacy of <abbr>EPSG</abbr> version 9.
     * @param  remarks     remarks as a {@link String} or {@link InternationalString}, or {@code null} if none.
     * @param  deprecated  {@code true} if the object to create is deprecated.
     * @return the name together with a set of properties.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<String,Object> createProperties(final String       table,
                                                final Integer      code,
                                                      String       name,        // May be replaced by an alias.
                                                final CharSequence description,
                                                final String       extentCode,  // Legacy from EPSG version 9.
                                                final String       scope,       // Legacy from EPSG version 9.
                                                final CharSequence remarks,
                                                final boolean      deprecated)
            throws SQLException, FactoryException
    {
        /*
         * Request for extent may cause a recursive call to `createCoordinateReferenceSystem(…)`,
         * se we need to fetch and store the extent before to populate the `properties` map.
         */
        final Extent extent = (extentCode == null) ? null : createExtent(extentCode);
        /*
         * Get all domains for the object identified by the given code.
         * The table used nere is new in version 10 of EPSG database.
         * We have to create the extents outside the `while` loop for
         * the same reason as above for `extent`.
         */
        ObjectDomain[] domains = null;
        if (translator.isUsageTableFound()) {
            final var extents = new ArrayList<String>();
            final var scopes  = new ArrayList<InternationalString>();
            try (ResultSet result = executeMetadataQuery("Usage",
                    "SELECT EXTENT_CODE, SCOPE_CODE FROM \"Usage\""
                            + " WHERE OBJECT_TABLE_NAME=? AND OBJECT_CODE=?",
                    translator.toActualTableName(table), code))
            {
                while (result.next()) {
                    extents.add(getString(code, result, 1));
                    scopes .add(getScope(getInteger(code, result, 2)));
                }
            }
            if (!extents.isEmpty()) {
                domains = new ObjectDomain[extents.size()];
                for (int i=0; i<domains.length; i++) {
                    domains[i] = new DefaultObjectDomain(scopes.get(i), owner.createExtent(extents.get(i)));
                }
            }
        }
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
                "SELECT NAMING_SYSTEM_NAME, ALIAS"
                        + " FROM \"Alias\" INNER JOIN \"Naming System\""
                        + " ON \"Alias\".NAMING_SYSTEM_CODE = \"Naming System\".NAMING_SYSTEM_CODE"
                        + " WHERE OBJECT_TABLE_NAME=? AND OBJECT_CODE=?",
                translator.toActualTableName(table), code))
        {
            while (result.next()) {
                final String naming = getOptionalString(result, 1);
                final String alias  = getString(code,   result, 2);
                NameSpace ns = null;
                if (naming != null) {
                    ns = (NameSpace) localCache.get(naming);
                    if (ns == null) {
                        ns = owner.nameFactory.createNameSpace(owner.nameFactory.createLocalName(null, naming), null);
                        localCache.put(naming, ns);
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
         * Creates the primary name as an object which is both a name (like aliases) and an identifier.
         * The identifier type is necessary because ISO 19111 defines the object names as identifiers.
         * We put all information that we have, such as version and object description, in that name.
         */
        final Locale      locale     = getLocale();
        final Citation    authority  = owner.getAuthority();
        final String      version    = Types.toString(authority.getEdition(), locale);
        final GenericName scopedName = owner.nameFactory.createGenericName(namespace, Constants.EPSG, name);
        properties.clear();
        properties.put("name",                          scopedName);
        properties.put(NamedIdentifier.CODE_KEY,        name);
        properties.put(NamedIdentifier.VERSION_KEY,     version);
        properties.put(NamedIdentifier.AUTHORITY_KEY,   authority);
        properties.put(NamedIdentifier.DESCRIPTION_KEY, description);
        properties.put(AbstractIdentifiedObject.LOCALE_KEY, locale);
        final var nameAsIdentifier = new NamedIdentifier(properties);
        /*
         * At this point, we can fill the map of object properties.
         * We store the deprecation flag in the object identifier.
         */
        properties.clear();
        properties.put(IdentifiedObject.NAME_KEY, nameAsIdentifier);
        if (!aliases.isEmpty()) {
            properties.put(IdentifiedObject.ALIAS_KEY, aliases.toArray(GenericName[]::new));
        }
        final ImmutableIdentifier identifier;
        if (deprecated) {
            properties.put(AbstractIdentifiedObject.DEPRECATED_KEY, Boolean.TRUE);
            final String replacedBy = getSupersession(table, code, locale);
            identifier = new DeprecatedCode(
                    authority,
                    Constants.EPSG,
                    code.toString(),
                    version,
                    Character.isDigit(replacedBy.charAt(0)) ? replacedBy : null,
                    Vocabulary.formatInternational(Vocabulary.Keys.SupersededBy_1, replacedBy));
        } else {
            identifier = new ImmutableIdentifier(
                    authority,
                    Constants.EPSG,
                    code.toString(),
                    version,
                    scopedName.toInternationalString());
        }
        properties.put(IdentifiedObject.IDENTIFIERS_KEY, identifier);
        properties.put(IdentifiedObject.REMARKS_KEY, remarks);
        properties.put(AbstractIdentifiedObject.LOCALE_KEY, locale);
        properties.put(ReferencingFactoryContainer.MT_FACTORY, owner.mtFactory);
        if (domains != null) {
            properties.put(IdentifiedObject.DOMAINS_KEY, domains);
        }
        if (scope != null && !scope.equals(UNKNOWN_SCOPE)) {    // Should be always NULL since EPSG version 10.
            properties.put(ObjectDomain.SCOPE_KEY, scope);
        }
        if (extent != null) {                                   // Should be always NULL since EPSG version 10.
            properties.put(ObjectDomain.DOMAIN_OF_VALIDITY_KEY, extent);
        }
        return properties;
    }

    /**
     * Returns an arbitrary object from a code. The default implementation delegates to more specific methods,
     * for example {@link #createCoordinateReferenceSystem(String)}, {@link #createDatum(String)}, <i>etc.</i>
     * until a successful one is found.
     *
     * <p><strong>Note that this method may be ambiguous</strong>
     * because the same <abbr>EPSG</abbr> code can be used for different kinds of objects.
     * This method throws an exception on a <em>best-effort</em> basis if it detects an ambiguity.
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
        final var query = new StringBuilder("SELECT ");
        final int queryStart = query.length();
        int found = -1;
        try {
            final int key = isPrimaryKey ? toPrimaryKeys(null, null, null, code)[0] : 0;
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
                        stmt.setInt(1, key);
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
     * The {@code CRSFactory.createFoo(…)} or {@code DatumFactory.createFoo(…)} method to invoke.
     * This is a convenience used by some {@link EPSGDataAccess} {@code createFoo(String)} methods when
     * the factory method to invoke has been decided but the properties map has not yet been populated.
     *
     * @see Proxy
     */
    @FunctionalInterface
    private interface FactoryCall<F extends Factory, R extends IdentifiedObject> {
        /**
         * Creates a <abbr>CRS</abbr> or datum.
         *
         * @param  factory     the factory to use for creating the object.
         * @param  properties  the properties to give to the object.
         * @return the object created from the given properties.
         * @throws FactoryException if the factory cannot create the object.
         */
        R create(F factory, Map<String, Object> properties) throws FactoryException;
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
    public synchronized CoordinateReferenceSystem createCoordinateReferenceSystem(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        CoordinateReferenceSystem returnValue = null;
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Coordinate Reference System",
                "COORD_REF_SYS_CODE",
                "COORD_REF_SYS_NAME",
                "SELECT"+ /* column  1 */ " COORD_REF_SYS_CODE,"
                        + /* column  2 */ " COORD_REF_SYS_NAME,"
                        + /* column  3 */ " AREA_OF_USE_CODE,"      // Deprecated since EPSG version 10 (always NULL)
                        + /* column  4 */ " CRS_SCOPE,"             // Deprecated since EPSG version 10 (always NULL)
                        + /* column  5 */ " REMARKS,"
                        + /* column  6 */ " DEPRECATED,"
                        + /* column  7 */ " COORD_REF_SYS_KIND,"
                        + /* column  8 */ " COORD_SYS_CODE,"        // Null for CompoundCRS
                        + /* column  9 */ " DATUM_CODE,"            // Null for ProjectedCRS
                        + /* column 10 */ " BASE_CRS_CODE,"         // For ProjectedCRS
                        + /* column 11 */ " PROJECTION_CONV_CODE,"  // For ProjectedCRS
                        + /* column 12 */ " CMPD_HORIZCRS_CODE,"    // For CompoundCRS only
                        + /* column 13 */ " CMPD_VERTCRS_CODE"      // For CompoundCRS only
                        + " FROM \"Coordinate Reference System\""
                        + " WHERE COORD_REF_SYS_CODE = ?", code))
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
                 * Do not invoke `createProperties` now, even if we have all required information,
                 * because the `properties` map will be overwritten by calls to `createDatum` and
                 * similar methods. Instead, remember the constructor to invoke later.
                 */
                final FactoryCall<CRSFactory, CoordinateReferenceSystem> constructor;
                /*
                 * The following switch statement should have a case for all "epsg_crs_kind" values enumerated
                 * in the "EPSG_Prepare.sql" file, except that the values in this Java code are in lower cases.
                 */
                switch (type.toLowerCase(Locale.US)) {
                    /* ──────────────────────────────────────────────────────────────────────
                     *   GEOCENTRIC CRS
                     *
                     *   NOTE: all values must be extracted from the `ResultSet`
                     *         before to invoke any `owner.createFoo(…)` method.
                     * ────────────────────────────────────────────────────────────────────── */
                    case "geocentric": {
                        final String csCode    = getString(code, result, 8);
                        final String datumCode = getString(code, result, 9);
                        final CoordinateSystem cs = owner.createCoordinateSystem(csCode);  // Do not inline the `getString(…)` calls.
                        final GeodeticDatum datumOrEnsemble = owner.createGeodeticDatum(datumCode);
                        final DatumEnsemble<GeodeticDatum> ensemble = wasDatumEnsemble(datumOrEnsemble, GeodeticDatum.class);
                        final GeodeticDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        if (cs instanceof CartesianCS) {
                            final var c = (CartesianCS) cs;
                            constructor = (factory, metadata) -> factory.createGeodeticCRS(metadata, datum, ensemble, c);
                        } else if (cs instanceof SphericalCS) {
                            final var c = (SphericalCS) cs;
                            constructor = (factory, metadata) -> factory.createGeodeticCRS(metadata, datum, ensemble, c);
                        } else {
                            throw new FactoryDataException(error().getString(
                                    Errors.Keys.IllegalCoordinateSystem_1, cs.getName()));
                        }
                        break;
                    }
                    /* ──────────────────────────────────────────────────────────────────────
                     *   GEOGRAPHIC CRS
                     * ────────────────────────────────────────────────────────────────────── */
                    case "geographic 2d":
                    case "geographic 3d": {
                        Integer csCode    = getInteger(code,  result, 8);
                        String  datumCode = getOptionalString(result, 9);
                        final GeodeticDatum datumOrEnsemble;
                        if (datumCode == null) {
                            String baseCode = getString(code, result, 10, 9);
                            datumOrEnsemble = owner.createGeographicCRS(baseCode).getDatum();
                        } else {
                            datumOrEnsemble = owner.createGeodeticDatum(datumCode);
                        }
                        if (replaceDeprecatedCS) {
                            csCode = replaceDeprecatedCS(csCode);
                        }
                        final EllipsoidalCS cs = owner.createEllipsoidalCS(csCode.toString());
                        final DatumEnsemble<GeodeticDatum> ensemble = wasDatumEnsemble(datumOrEnsemble, GeodeticDatum.class);
                        final GeodeticDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) -> factory.createGeographicCRS(metadata, datum, ensemble, cs);
                        break;
                    }
                    /* ──────────────────────────────────────────────────────────────────────
                     *   PROJECTED CRS
                     *
                     *   NOTE: This method may invoke itself for creating the base CRS,
                     *         in which case the `ResultSet` will be closed. Therefore,
                     *         all values must be extracted from the `ResultSet` before
                     *         to invoke any `owner.createFoo(…)` method.
                     * ────────────────────────────────────────────────────────────────────── */
                    case "projected": {
                        final String csCode   = getString(code, result,  8);
                        final String baseCode = getString(code, result, 10);
                        final String opCode   = getString(code, result, 11);
                        final Conversion fromBase;
                        try {
                            fromBase = (Conversion) owner.createCoordinateOperation(opCode);
                        } catch (ClassCastException e) {
                            // Should never happen in a well-formed EPSG database.
                            // If happen anyway, the ClassCastException cause will give more hints than just the message.
                            throw (NoSuchAuthorityCodeException) noSuchAuthorityCode(Conversion.class, opCode).initCause(e);
                        }
                        final CoordinateReferenceSystem baseCRS;
                        if (deprecated) {
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
                                baseCRS = createCoordinateReferenceSystem(baseCode);         // Do not cache that CRS.
                            } finally {
                                replaceDeprecatedCS = false;
                                quiet = old;
                            }
                        } else {
                            baseCRS = owner.createCoordinateReferenceSystem(baseCode);      // Use the cache.
                        }
                        final CartesianCS cs = owner.createCartesianCS(csCode);
                        constructor = (factory, metadata) -> {
                            /*
                             * The crsFactory method calls will indirectly create a parameterized MathTransform.
                             * Their constructor will try to verify the parameter validity. But some deprecated
                             * CRS had invalid parameter values (they were deprecated precisely for that reason).
                             * If and only if we are creating a deprecated CRS, temporarily suspend the parameter
                             * checks.
                             */
                            final boolean old = !deprecated || Semaphores.queryAndSet(Semaphores.SUSPEND_PARAMETER_CHECK);
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
                                if (baseCRS instanceof GeodeticCRS) {
                                    return factory.createProjectedCRS(metadata, (GeodeticCRS) baseCRS, fromBase, cs);
                                } else {
                                    return factory.createDerivedCRS(metadata, baseCRS, fromBase, cs);
                                }
                            } finally {
                                Semaphores.clearIfFalse(Semaphores.SUSPEND_PARAMETER_CHECK, old);
                            }
                        };
                        break;
                    }
                    /* ──────────────────────────────────────────────────────────────────────
                     *   VERTICAL CRS
                     * ────────────────────────────────────────────────────────────────────── */
                    case "vertical": {
                        final String csCode    = getString(code, result, 8);
                        final String datumCode = getString(code, result, 9);
                        final VerticalCS cs = owner.createVerticalCS(csCode);   // Do not inline the `getString(…)` calls.
                        final VerticalDatum datumOrEnsemble = owner.createVerticalDatum(datumCode);
                        final DatumEnsemble<VerticalDatum> ensemble = wasDatumEnsemble(datumOrEnsemble, VerticalDatum.class);
                        final VerticalDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) -> factory.createVerticalCRS(metadata, datum, ensemble, cs);
                        break;
                    }
                    /* ──────────────────────────────────────────────────────────────────────
                     *   TEMPORAL CRS
                     *
                     *   NOTE : As of version 12, the EPSG database does not define temporal CRS.
                     *          This block is a SIS─specific extension.
                     * ────────────────────────────────────────────────────────────────────── */
                    case "time":
                    case "temporal": {
                        final String csCode    = getString(code, result, 8);
                        final String datumCode = getString(code, result, 9);
                        final TimeCS cs = owner.createTimeCS(csCode);    // Do not inline the `getString(…)` calls.
                        final TemporalDatum datumOrEnsemble = owner.createTemporalDatum(datumCode);
                        final DatumEnsemble<TemporalDatum> ensemble = wasDatumEnsemble(datumOrEnsemble, TemporalDatum.class);
                        final TemporalDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) -> factory.createTemporalCRS(metadata, datum, ensemble, cs);
                        break;
                    }
                    /* ──────────────────────────────────────────────────────────────────────
                     *   ENGINEERING CRS
                     * ────────────────────────────────────────────────────────────────────── */
                    case "engineering": {
                        final String csCode    = getString(code, result, 8);
                        final String datumCode = getString(code, result, 9);
                        final CoordinateSystem cs = owner.createCoordinateSystem(csCode);    // Do not inline the `getString(…)` calls.
                        final EngineeringDatum datumOrEnsemble = owner.createEngineeringDatum(datumCode);
                        final DatumEnsemble<EngineeringDatum> ensemble = wasDatumEnsemble(datumOrEnsemble, EngineeringDatum.class);
                        final EngineeringDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) -> factory.createEngineeringCRS(metadata, datum, ensemble, cs);
                        break;
                    }
                    /* ──────────────────────────────────────────────────────────────────────
                     *   PARAMETRIC CRS
                     * ────────────────────────────────────────────────────────────────────── */
                    case "parametric": {
                        final String csCode    = getString(code, result, 8);
                        final String datumCode = getString(code, result, 9);
                        final ParametricCS cs = owner.createParametricCS(csCode);    // Do not inline the `getString(…)` calls.
                        final ParametricDatum datumOrEnsemble = owner.createParametricDatum(datumCode);
                        final DatumEnsemble<ParametricDatum> ensemble = wasDatumEnsemble(datumOrEnsemble, ParametricDatum.class);
                        final ParametricDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) -> factory.createParametricCRS(metadata, datum, ensemble, cs);
                        break;
                    }
                    /* ──────────────────────────────────────────────────────────────────────
                     *   COMPOUND CRS
                     *
                     *   NOTE: This method invokes itself recursively.
                     *         Consequently, we cannot use `result` anymore.
                     * ────────────────────────────────────────────────────────────────────── */
                    case "compound": {
                        final String code1 = getString(code, result, 12);
                        final String code2 = getString(code, result, 13);
                        final CoordinateReferenceSystem crs1 = owner.createCoordinateReferenceSystem(code1);
                        final CoordinateReferenceSystem crs2 = owner.createCoordinateReferenceSystem(code2);
                        constructor = (factory, metadata) -> factory.createCompoundCRS(metadata, crs1, crs2);
                        break;
                    }
                    /* ──────────────────────────────────────────────────────────────────────
                     *   UNKNOWN CRS
                     * ────────────────────────────────────────────────────────────────────── */
                    default: {
                        throw new FactoryDataException(error().getString(Errors.Keys.UnknownType_1, type));
                    }
                }
                /*
                 * Map of properties should be populated only after we extracted all
                 * information needed from the `ResultSet`, because it may be closed.
                 */
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties(
                        "Coordinate Reference System", epsg, name, null, area, scope, remarks, deprecated);
                final CoordinateReferenceSystem crs = constructor.create(owner.crsFactory, properties);
                returnValue = ensureSingleton(crs, returnValue, code);
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateReferenceSystem.class, code, exception);
        } catch (ClassCastException exception) {
            throw new FactoryDataException(error().getString(exception.getLocalizedMessage()), exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
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
     * We cannot resolve the type with a private field which would be set by {@link #createDatumEnsemble(String)}
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
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Datum",
                "DATUM_CODE",
                "DATUM_NAME",
                "SELECT"+ /* column  1 */ " DATUM_CODE,"
                        + /* column  2 */ " DATUM_NAME,"
                        + /* column  3 */ " DATUM_TYPE,"
                        + /* column  4 */ " ORIGIN_DESCRIPTION,"
                        + /* column  5 */ " ANCHOR_EPOCH,"
                        + /* column  6 */ " FRAME_REFERENCE_EPOCH,"     // NULL for static datum, non-null if dynamic.
                        + /* column  7 */ " PUBLICATION_DATE,"          // Was REALIZATION_EPOCH in EPSG version 9.
                        + /* column  8 */ " AREA_OF_USE_CODE,"          // Deprecated since EPSG version 10 (always NULL)
                        + /* column  9 */ " DATUM_SCOPE,"               // Deprecated since EPSG version 10 (always NULL)
                        + /* column 10 */ " REMARKS,"
                        + /* column 11 */ " DEPRECATED,"
                        + /* column 12 */ " ELLIPSOID_CODE,"            // Only for geodetic type
                        + /* column 13 */ " PRIME_MERIDIAN_CODE,"       // Only for geodetic type
                        + /* column 14 */ " REALIZATION_METHOD_CODE,"   // Only for vertical type
                        + /* column 15 */ " CONVENTIONAL_RS_CODE"       // Only for members of an ensemble
                        + " FROM \"Datum\""
                        + " WHERE DATUM_CODE = ?", code))
        {
            while (result.next()) {
                final Integer  epsg       = getInteger   (code, result,  1);
                final String   name       = getString    (code, result,  2);
                final String   type       = getString    (code, result,  3);
                final String   anchor     = getOptionalString  (result,  4);
                final Temporal epoch      = getOptionalEpoch   (result,  5);
                final Temporal dynamic    = getOptionalEpoch   (result,  6);
                final Temporal publish    = getOptionalTemporal(result,  7, "createDatum");
                final String   area       = getOptionalString  (result,  8);
                final String   scope      = getOptionalString  (result,  9);
                final String   remarks    = getOptionalString  (result, 10);
                final boolean  deprecated = getOptionalBoolean (result, 11);
                final Integer  convRSCode = getOptionalInteger (result, 15);
                /*
                 * Do not invoke `createProperties` now, even if we have all required information,
                 * because the `properties` map will be overwritten by calls to `createEllipsoid`
                 * and similar methods. Instead, remember the constructor to invoke later.
                 */
                final FactoryCall<DatumFactory, ? extends Datum> constructor;
                /*
                 * The following switch statement should have a case for all "epsg_datum_kind" values enumerated
                 * in the "EPSG_Prepare.sql" file, except that the values in this Java code are in lower cases.
                 */
                switch (type.toLowerCase(Locale.US)) {
                    case "dynamic geodetic":
                    case "geodetic": {
                        final String ellipsoidCode   = getString(code, result, 12);
                        final String meridianCode    = getString(code, result, 13);
                        final Ellipsoid ellipsoid    = owner.createEllipsoid(ellipsoidCode);  // Do not inline the `getString(…)` calls.
                        final PrimeMeridian meridian = owner.createPrimeMeridian(meridianCode);
                        constructor = (factory, metadata) ->
                                (dynamic != null) ? factory.createGeodeticDatum(metadata, ellipsoid, meridian, dynamic)
                                                  : factory.createGeodeticDatum(metadata, ellipsoid, meridian);
                        break;
                    }
                    case "vertical": {
                        final RealizationMethod method = getRealizationMethod(getOptionalInteger(result, 14));
                        constructor = (factory, metadata) ->
                                (dynamic != null) ? factory.createVerticalDatum(metadata, method, dynamic)
                                                  : factory.createVerticalDatum(metadata, method);
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
                        constructor = (factory, metadata) -> factory.createTemporalDatum(metadata, originDate);
                        break;
                    }
                    /*
                     * Straightforward cases.
                     */
                    case "engineering": constructor = DatumFactory::createEngineeringDatum; break;
                    case "parametric":  constructor = DatumFactory::createParametricDatum;  break;
                    case "ensemble":    constructor = createDatumEnsemble(epsg); break;
                    default: throw new FactoryDataException(error().getString(Errors.Keys.UnknownType_1, type));
                }
                /*
                 * Map of properties should be populated only after we extracted all
                 * information needed from the `ResultSet`, because it may be closed.
                 */
                final IdentifiedObject conventionalRS = createConventionalRS(convRSCode);
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties(
                        "Datum", epsg, name, null, area, scope, remarks, deprecated);
                properties.put(Datum.ANCHOR_DEFINITION_KEY, anchor);
                properties.put(Datum.ANCHOR_EPOCH_KEY,      epoch);
                properties.put(Datum.PUBLICATION_DATE_KEY,  publish);
                properties.put(Datum.CONVENTIONAL_RS_KEY,   conventionalRS);
                properties.values().removeIf(Objects::isNull);
                final Datum datum = constructor.create(owner.datumFactory, properties);
                returnValue = ensureSingleton(datum, returnValue, code);
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(Datum.class, code, exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(Datum.class, code);
        }
        return returnValue;
    }

    /**
     * Creates an arbitrary datum ensemble from a code. The datum ensemble is returned as a lambda function
     * because the metadata need to be provided by the caller, but only after this method fetched the members.
     *
     * @param  code        value allocated by EPSG.
     * @param  properties  properties to assign to the datum ensemble.
     * @return provider of the datum ensemble for the given code.
     *
     * @see #createDatum(String)
     * @see #createDatumEnsemble(String)
     */
    private FactoryCall<DatumFactory, DefaultDatumEnsemble<?>> createDatumEnsemble(final Integer code)
            throws SQLException, FactoryException
    {
        double max = Double.NaN;
        try (ResultSet result = executeQueryForCodes(
                "DatumEnsemble",
                "SELECT ENSEMBLE_ACCURACY"
                        + " FROM \"DatumEnsemble\""
                        + " WHERE DATUM_ENSEMBLE_CODE = ?", code))
        {
            // Should have exactly one value. The loop is a paranoiac safety.
            while (result.next()) {
                final double value = getDouble(code, result, 1);
                if (Double.isNaN(max) || value > max) {
                    max = value;
                }
            }
        }
        final var accuracy = PositionalAccuracyConstant.ensemble(max);
        final List<Datum> members = createComponents(
                GeodeticAuthorityFactory::createDatum,
                "DatumEnsembleMember",
                "SELECT DATUM_CODE"
                        + " FROM \"DatumEnsembleMember\""
                        + " WHERE DATUM_ENSEMBLE_CODE = ?"
                        + " ORDER BY DATUM_SEQUENCE", code);
        return (factory, metadata) -> DefaultDatumEnsemble.castOrCopy(factory.createDatumEnsemble(metadata, members, accuracy));
    }

    /**
     * Creates the members of a geodetic object. This method gets all member codes and closes the
     * result set before to create the members, because the creation of a member causes recursive
     * invocation to some {@code create(…)} methods of this factory.
     *
     * @param  <C>          the type of component objects.
     * @param  constructor  the method to invoke for creating the components.
     * @param  table        a key uniquely identifying the caller.
     * @param  sql          the SQL statement to use for creating the {@link PreparedStatement} object.
     * @param  parent       the code of the container object.
     * @return all components for the given parent.
     */
    private <C extends IdentifiedObject> List<C> createComponents(final Proxy<C> constructor,
                                                                  final String   table,
                                                                  final String   sql,
                                                                  final Integer  parent)
            throws SQLException, FactoryException
    {
        final var codes = new ArrayList<String>();
        try (ResultSet result = executeQueryForCodes(table, sql, parent)) {
            while (result.next()) {
                codes.add(getString(parent, result, 1));
            }
        }
        final var members = new ArrayList<C>(codes.size());
        for (String code : codes) {
            members.add(constructor.create(owner, code));
        }
        return members;
    }

    /**
     * Delegates object creations to one of the {@code create(…)} methods. This is used for creating
     * the components of a geodetic object. Invoking the {@code create(…)} method of this interface
     * will often result in invocation of a public {@code create(…)} method of the enclosing class.
     *
     * @param  <C>  the type of geodetic objects to create.
     *
     * @see FactoryCall
     * @see org.apache.sis.referencing.factory.AuthorityFactoryProxy
     */
    @FunctionalInterface
    private interface Proxy<C extends IdentifiedObject> {
        /**
         * Creates a component from the given code by delegating
         * (indirectly) to a method of the enclosing class.
         *
         * @param  factory  the factory to use for creating the component.
         * @param  code     authority code of the component to create.
         * @return the component created from the given code.
         * @throws FactoryException if an error occurred while creating the component.
         */
        C create(GeodeticAuthorityFactory factory, String code) throws FactoryException;
    }

    /**
     * Creates a conventional reference system from a code.
     * All members of a datum ensemble shall have the same conventional reference system.
     *
     * @param  code  value allocated by EPSG, or {@code null} if none.
     * @return the datum for the given code, or {@code null} if not found.
     */
    private IdentifiedObject createConventionalRS(final Integer code) throws SQLException, FactoryException {
        assert Thread.holdsLock(this);
        if (code == null) {
            return null;
        }
        final Long cacheKey = cacheKey(4, code);
        var returnValue = (IdentifiedObject) localCache.get(cacheKey);
        if (returnValue == null) {
            try (ResultSet result = executeQueryForCodes(
                    "ConventionalRS",
                    "SELECT"+ /* column 1 */ " CONVENTIONAL_RS_CODE,"
                            + /* column 2 */ " CONVENTIONAL_RS_NAME,"
                            + /* column 3 */ " REMARKS,"
                            + /* column 4 */ " DEPRECATED"
                            + " FROM \"ConventionalRS\""
                            + " WHERE CONVENTIONAL_RS_CODE = ?", code))
            {
                while (result.next()) {
                    final Integer epsg       = getInteger   (code, result, 1);
                    final String  name       = getString    (code, result, 2);
                    final String  remarks    = getOptionalString  (result, 3);
                    final boolean deprecated = getOptionalBoolean (result, 4);
                    /*
                     * Map of properties should be populated only after we extracted all
                     * information needed from the `ResultSet`, because it may be closed.
                     */
                    @SuppressWarnings("LocalVariableHidesMemberVariable")
                    final Map<String,Object> properties = createProperties(
                            "ConventionalRS", epsg, name, null, null, null, remarks, deprecated);
                    returnValue = ensureSingleton(new AbstractIdentifiedObject(properties), returnValue, code);
                    if (result.isClosed()) break;   // See createProperties(…) for explanation.
                }
            }
            localCache.put(cacheKey, returnValue);
        }
        return returnValue;
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
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Ellipsoid",
                "ELLIPSOID_CODE",
                "ELLIPSOID_NAME",
                "SELECT"+ /* column 1 */ " ELLIPSOID_CODE,"
                        + /* column 2 */ " ELLIPSOID_NAME,"
                        + /* column 3 */ " SEMI_MAJOR_AXIS,"
                        + /* column 4 */ " INV_FLATTENING,"
                        + /* column 5 */ " SEMI_MINOR_AXIS,"
                        + /* column 6 */ " UOM_CODE,"
                        + /* column 7 */ " REMARKS,"
                        + /* column 8 */ " DEPRECATED"
                        + " FROM \"Ellipsoid\""
                        + " WHERE ELLIPSOID_CODE = ?", code))
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
                final boolean useSemiMinor      = Double.isNaN(inverseFlattening);
                if (useSemiMinor && Double.isNaN(semiMinorAxis)) {
                    // Both are null, which is not allowed.
                    final String column = result.getMetaData().getColumnName(3);
                    throw new FactoryDataException(error().getString(Errors.Keys.NullValueInTable_3, code, column));
                }
                /*
                 * Map of properties should be populated only after we extracted all
                 * information needed from the `ResultSet`, because it may be closed.
                 */
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties(
                        "Ellipsoid", epsg, name, null, null, null, remarks, deprecated);
                final Ellipsoid ellipsoid;
                if (useSemiMinor) {
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
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(Ellipsoid.class, code, exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
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
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Prime Meridian",
                "PRIME_MERIDIAN_CODE",
                "PRIME_MERIDIAN_NAME",
                "SELECT"+ /* column 1 */ " PRIME_MERIDIAN_CODE,"
                        + /* column 2 */ " PRIME_MERIDIAN_NAME,"
                        + /* column 3 */ " GREENWICH_LONGITUDE,"
                        + /* column 4 */ " UOM_CODE,"
                        + /* column 5 */ " REMARKS,"
                        + /* column 6 */ " DEPRECATED"
                        + " FROM \"Prime Meridian\""
                        + " WHERE PRIME_MERIDIAN_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final double  longitude  = getDouble   (code, result, 3);
                final String  unitCode   = getString   (code, result, 4);
                final String  remarks    = getOptionalString (result, 5);
                final boolean deprecated = getOptionalBoolean(result, 6);
                final Unit<Angle> unit = owner.createUnit(unitCode).asType(Angle.class);
                /*
                 * Map of properties should be populated only after we extracted all
                 * information needed from the `ResultSet`, because it may be closed.
                 */
                final PrimeMeridian primeMeridian = owner.datumFactory.createPrimeMeridian(
                        createProperties("Prime Meridian", epsg, name, null, null, null, remarks, deprecated),
                        longitude, unit);
                returnValue = ensureSingleton(primeMeridian, returnValue, code);
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(PrimeMeridian.class, code, exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
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
        DefaultExtent returnValue = null;
        final var deferred = new ArrayList<Map.Entry<DefaultVerticalExtent, Integer>>();
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Extent",
                "EXTENT_CODE",
                "EXTENT_NAME",
                "SELECT"+ /* column  1 */ " EXTENT_DESCRIPTION,"
                        + /* column  2 */ " BBOX_SOUTH_BOUND_LAT,"
                        + /* column  3 */ " BBOX_NORTH_BOUND_LAT,"
                        + /* column  4 */ " BBOX_WEST_BOUND_LON,"
                        + /* column  5 */ " BBOX_EAST_BOUND_LON,"
                        + /* column  6 */ " VERTICAL_EXTENT_MIN,"
                        + /* column  7 */ " VERTICAL_EXTENT_MAX,"
                        + /* column  8 */ " VERTICAL_EXTENT_CRS_CODE,"
                        + /* column  9 */ " TEMPORAL_EXTENT_BEGIN,"
                        + /* column 10 */ " TEMPORAL_EXTENT_END"
                        + " FROM \"Extent\""
                        + " WHERE EXTENT_CODE = ?", code))
        {
            while (result.next()) {
                final String description = getOptionalString(result, 1);
                double   ymin = getOptionalDouble  (result,  2);
                double   ymax = getOptionalDouble  (result,  3);
                double   xmin = getOptionalDouble  (result,  4);
                double   xmax = getOptionalDouble  (result,  5);
                double   zmin = getOptionalDouble  (result,  6);
                double   zmax = getOptionalDouble  (result,  7);
                Temporal tmin = getOptionalTemporal(result,  9, "createExtent");
                Temporal tmax = getOptionalTemporal(result, 10, "createExtent");
                DefaultGeographicBoundingBox bbox = null;
                if (!(Double.isNaN(ymin) && Double.isNaN(ymax) && Double.isNaN(xmin) && Double.isNaN(xmax))) {
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
                DefaultVerticalExtent vertical = null;
                if (!(Double.isNaN(zmin) && Double.isNaN(zmax))) {
                    vertical = new DefaultVerticalExtent(zmin, zmax, null);
                    final Integer crs = getOptionalInteger(result, 8);
                    if (crs != null) {
                        var c = new QueryID("Coordinate Reference System", new int[] {crs}, currentSingletonQuery);
                        if (!c.isAlreadyInProgress()) {
                            deferred.add(Map.entry(vertical, crs));
                        }
                    }
                }
                DefaultTemporalExtent temporal = null;
                if (tmin != null || tmax != null) {
                    temporal = new DefaultTemporalExtent(tmin, tmax);
                }
                var extent = new DefaultExtent(description, bbox, vertical, temporal);
                if (!extent.isEmpty()) {
                    returnValue = ensureSingleton(extent, returnValue, code);
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(Extent.class, code, exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
        }
        /*
         * Resolve CRS only after we finished the loop, because there is a risk of recursive call,
         * which would have closed the `ResultSet` for creating a new one.
         */
        for (Map.Entry<DefaultVerticalExtent, Integer> entry : deferred) {
            entry.getKey().setVerticalCRS(owner.createVerticalCRS(entry.getValue().toString()));
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(Extent.class, code);
        }
        returnValue.transitionTo(DefaultExtent.State.FINAL);
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
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Coordinate System",
                "COORD_SYS_CODE",
                "COORD_SYS_NAME",
                "SELECT"+ /* column 1 */ " COORD_SYS_CODE,"
                        + /* column 2 */ " COORD_SYS_NAME,"
                        + /* column 3 */ " COORD_SYS_TYPE,"
                        + /* column 4 */ " DIMENSION,"
                        + /* column 5 */ " REMARKS,"
                        + /* column 6 */ " DEPRECATED"
                        + " FROM \"Coordinate System\""
                        + " WHERE COORD_SYS_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final String  type       = getString   (code, result, 3);
                final int     dimension  = getInteger  (code, result, 4);
                final String  remarks    = getOptionalString (result, 5);
                final boolean deprecated = getOptionalBoolean(result, 6);
                final CoordinateSystemAxis[] axes = createComponents(
                        GeodeticAuthorityFactory::createCoordinateSystemAxis,
                        "AxisOrder",
                        "SELECT COORD_AXIS_CODE"
                                + " FROM \"Coordinate Axis\""
                                + " WHERE COORD_SYS_CODE = ?"
                                + " ORDER BY COORD_AXIS_ORDER", epsg).toArray(CoordinateSystemAxis[]::new);

                if (axes.length != dimension) {
                    throw new FactoryDataException(error().getString(
                            Errors.Keys.MismatchedDimension_2, axes.length, dimension));
                }
                /*
                 * Map of properties should be populated only after we extracted all
                 * information needed from the `ResultSet`, because it may be closed.
                 */
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties(
                        "Coordinate System", epsg, name, null, null, null, remarks, deprecated);
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
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateSystem.class, code, exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
        }
        if (returnValue == null) {
            throw noSuchAuthorityCode(CoordinateSystem.class, code);
        }
        return returnValue;
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
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Coordinate Axis",
                "COORD_AXIS_CODE",
                null,
                "SELECT"+ /* column 1 */ " COORD_AXIS_CODE,"
                        + /* column 2 */ " COORD_AXIS_NAME_CODE,"
                        + /* column 3 */ " COORD_AXIS_ORIENTATION,"
                        + /* column 4 */ " COORD_AXIS_ABBREVIATION,"
                        + /* column 5 */ " UOM_CODE"
                        + " FROM \"Coordinate Axis\""
                        + " WHERE COORD_AXIS_CODE = ?", code))
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
                /*
                 * Map of properties should be populated only after we extracted all
                 * information needed from the `ResultSet`, because it may be closed.
                 */
                final CoordinateSystemAxis axis = owner.csFactory.createCoordinateSystemAxis(
                        createProperties("Coordinate Axis", epsg, an.name, an.description, null, null, an.remarks, false),
                        abbreviation, direction, owner.createUnit(unit));
                returnValue = ensureSingleton(axis, returnValue, code);
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateSystemAxis.class, code, exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
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
        final Long cacheKey = cacheKey(3, code);
        var returnValue = (AxisName) localCache.get(cacheKey);
        if (returnValue == null) {
            try (ResultSet result = executeQueryForCodes(
                    "Coordinate Axis Name",
                    "SELECT"+ /* column 1 */ " COORD_AXIS_NAME,"
                            + /* column 2 */ " DESCRIPTION,"
                            + /* column 3 */ " REMARKS"
                            + " FROM \"Coordinate Axis Name\""
                            + " WHERE COORD_AXIS_NAME_CODE = ?", code))
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
            localCache.put(cacheKey, returnValue);
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
        if (code == null) {
            return null;
        }
        final Long cacheKey = cacheKey(2, code);
        var returnValue = (RealizationMethod) localCache.get(cacheKey);
        if (returnValue == null && code != null) {
            try (ResultSet result = executeQueryForCodes(
                    "DatumRealizationMethod",
                    "SELECT REALIZATION_METHOD_NAME"
                            + " FROM \"DatumRealizationMethod\""
                            + " WHERE REALIZATION_METHOD_CODE = ?", code))
            {
                while (result.next()) {
                    final String name = getString(code, result, 1);
                    returnValue = ensureSingleton(VerticalDatumTypes.fromMethod(name), returnValue, code);
                }
            }
            if (returnValue == null) {
                throw noSuchAuthorityCode(RealizationMethod.class, String.valueOf(code));
            }
            localCache.put(cacheKey, returnValue);
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
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Unit of Measure",
                "UOM_CODE",
                "UNIT_OF_MEAS_NAME",
                "SELECT"+ /* column 1 */ " UOM_CODE,"
                        + /* column 2 */ " FACTOR_B,"
                        + /* column 3 */ " FACTOR_C,"
                        + /* column 4 */ " TARGET_UOM_CODE,"
                        + /* column 5 */ " UNIT_OF_MEAS_NAME"
                        + " FROM \"Unit of Measure\""
                        + " WHERE UOM_CODE = ?", code))
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
        } finally {
            currentSingletonQuery = previousSingletonQuery;
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
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Coordinate_Operation Parameter",
                "PARAMETER_CODE",
                "PARAMETER_NAME",
                "SELECT"+ /* column 1 */ " PARAMETER_CODE,"
                        + /* column 2 */ " PARAMETER_NAME,"
                        + /* column 3 */ " DESCRIPTION,"
                        + /* column 4 */ " DEPRECATED"
                        + " FROM \"Coordinate_Operation Parameter\""
                        + " WHERE PARAMETER_CODE = ?", code))
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
                if (epsg != null && EPSG_CODE_PARAMETERS.contains(epsg)) {
                    type  = Integer.class;
                    units = Set.of();
                } else {
                    /*
                     * If the parameter appears to have at least one non-null value in the "Parameter File Name" column,
                     * then the type is assumed to be URI as a string. Otherwise, the type is a floating point number.
                     */
                    type = Double.class;
                    try (ResultSet r = executeQueryForCodes(
                            "ParameterType",
                            "SELECT PARAM_VALUE_FILE_REF"
                                    + " FROM \"Coordinate_Operation Parameter Value\""
                                    + " WHERE PARAM_VALUE_FILE_REF IS NOT NULL"
                                    + " AND (PARAMETER_CODE = ?)", epsg))
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
                    try (ResultSet r = executeQueryForCodes(
                            "ParameterUnit",
                            "SELECT UOM_CODE"
                                    + " FROM \"Coordinate_Operation Parameter Value\""
                                    + " WHERE (PARAMETER_CODE = ?)"
                                    + " GROUP BY UOM_CODE"
                                    + " ORDER BY COUNT(UOM_CODE) DESC", epsg))
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
                try (ResultSet r = executeQueryForCodes(
                        "ParameterSign",
                        "SELECT DISTINCT PARAM_SIGN_REVERSAL"
                                + " FROM \"Coordinate_Operation Parameter Usage\""
                                + " WHERE (PARAMETER_CODE = ?)", epsg))
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
                                                                   Double.POSITIVE_INFINITY, false,
                                                                   CollectionsExt.first(units)); break;
                }
                /*
                 * Map of properties should be populated only after we extracted all
                 * information needed from the `ResultSet`, because it may be closed.
                 */
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties(
                        "Coordinate_Operation Parameter", epsg, name, null, null, null, isReversible, deprecated);
                properties.put(Identifier.DESCRIPTION_KEY, description);
                final var descriptor = new DefaultParameterDescriptor<>(properties, 1, 1, type, valueDomain, null, null);
                returnValue = ensureSingleton(descriptor, returnValue, code);
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(OperationMethod.class, code, exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
        }
        if (returnValue == null) {
             throw noSuchAuthorityCode(OperationMethod.class, code);
        }
        return returnValue;
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
        try (ResultSet result = executeQueryForCodes(
                "Coordinate_Operation Parameter Value",
                "SELECT"+ /* column 1 */ " CP.PARAMETER_NAME,"
                        + /* column 2 */ " CV.PARAMETER_VALUE,"
                        + /* column 3 */ " CV.PARAM_VALUE_FILE_REF,"
                        + /* column 4 */ " CV.UOM_CODE"
                        + " FROM ("      + "\"Coordinate_Operation Parameter Value\"" + " AS CV"
                        + " INNER JOIN " + "\"Coordinate_Operation Parameter\""       + " AS CP" + " ON (CV.PARAMETER_CODE = CP.PARAMETER_CODE))"
                        + " INNER JOIN " + "\"Coordinate_Operation Parameter Usage\"" + " AS CU" + " ON (CP.PARAMETER_CODE = CU.PARAMETER_CODE)"
                        +  " AND (CV.COORD_OP_METHOD_CODE = CU.COORD_OP_METHOD_CODE)"
                        + " WHERE CV.COORD_OP_METHOD_CODE = ?"
                        +   " AND CV.COORD_OP_CODE = ?"
                        + " ORDER BY CU.SORT_ORDER", method, operation))
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
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Coordinate_Operation Method",
                "COORD_OP_METHOD_CODE",
                "COORD_OP_METHOD_NAME",
                "SELECT"+ /* column 1 */ " COORD_OP_METHOD_CODE,"
                        + /* column 2 */ " COORD_OP_METHOD_NAME,"
                        + /* column 3 */ " REMARKS,"
                        + /* column 4 */ " DEPRECATED"
                        + " FROM \"Coordinate_Operation Method\""
                        + " WHERE COORD_OP_METHOD_CODE = ?", code))
        {
            while (result.next()) {
                final Integer epsg       = getInteger  (code, result, 1);
                final String  name       = getString   (code, result, 2);
                final String  remarks    = getOptionalString (result, 3);
                final boolean deprecated = getOptionalBoolean(result, 4);
                final ParameterDescriptor<?>[] descriptors = createComponents(
                                GeodeticAuthorityFactory::createParameterDescriptor,
                                "Coordinate_Operation Parameter Usage",
                                "SELECT PARAMETER_CODE"
                                        + " FROM \"Coordinate_Operation Parameter Usage\""
                                        + " WHERE COORD_OP_METHOD_CODE = ?"
                                        + " ORDER BY SORT_ORDER", epsg)
                        .toArray(ParameterDescriptor[]::new);
                /*
                 * Map of properties should be populated only after we extracted all
                 * information needed from the `ResultSet`, because it may be closed.
                 */
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties(
                        "Coordinate_Operation Method", epsg, name, null, null, null, remarks, deprecated);
                /*
                 * Note: we do not store the formula at this time, because the text is very verbose and rarely used.
                 */
                final var params = new DefaultParameterDescriptorGroup(properties, 1, 1, descriptors);
                final var method = new DefaultOperationMethod(properties, params);
                returnValue = ensureSingleton(method, returnValue, code);
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(OperationMethod.class, code, exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
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
    public synchronized CoordinateOperation createCoordinateOperation(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ArgumentChecks.ensureNonNull("code", code);
        CoordinateOperation returnValue = null;
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                "Coordinate_Operation",
                "COORD_OP_CODE",
                "COORD_OP_NAME",
                "SELECT"+ /* column  1 */ " COORD_OP_CODE,"
                        + /* column  2 */ " COORD_OP_NAME,"
                        + /* column  3 */ " COORD_OP_TYPE,"
                        + /* column  4 */ " SOURCE_CRS_CODE,"
                        + /* column  5 */ " TARGET_CRS_CODE,"
                        + /* column  6 */ " COORD_OP_METHOD_CODE,"
                        + /* column  7 */ " COORD_TFM_VERSION,"
                        + /* column  8 */ " COORD_OP_ACCURACY,"
                        + /* column  9 */ " AREA_OF_USE_CODE,"     // Deprecated since EPSG version 10 (always NULL)
                        + /* column 10 */ " COORD_OP_SCOPE,"       // Deprecated since EPSG version 10 (always NULL)
                        + /* column 11 */ " REMARKS,"
                        + /* column 12 */ " DEPRECATED"
                        + " FROM \"Coordinate_Operation\""
                        + " WHERE COORD_OP_CODE = ?", code))
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
                final CoordinateReferenceSystem sourceCRS = (sourceCode == null) ? null : owner.createCoordinateReferenceSystem(sourceCode);
                final CoordinateReferenceSystem targetCRS = (targetCode == null) ? null : owner.createCoordinateReferenceSystem(targetCode);
                /*
                 * Get the operation method. This is mandatory for conversions and transformations
                 * (it was checked by getInteger(code, result, …) above in this method) but optional
                 * for concatenated operations. Fetching parameter values is part of this block.
                 */
                final OperationMethod     operationMethod;
                final ParameterValueGroup parameterValues;
                final boolean isDeferred = Semaphores.query(Semaphores.METADATA_ONLY);
                if (methodCode != null && !isDeferred) {
                    operationMethod = owner.createOperationMethod(methodCode.toString());
                    parameterValues = operationMethod.getParameters().createValue();
                    fillParameterValues(methodCode, epsg, parameterValues);
                } else {
                    operationMethod = null;
                    parameterValues = null;
                }
                Class<? extends SingleOperation> operationType = null;
                final FactoryCall<CoordinateOperationFactory, CoordinateOperation> constructor;
                /*
                 * Creates the operation. Conversions should be the only operations allowed to have
                 * null source and target CRS. In such case, the operation is a defining conversion
                 * (usually to be used later as part of a ProjectedCRS creation).
                 */
                if (isDeferred) {
                    constructor = (factory, metadata) -> new DeferredCoordinateOperation(metadata, sourceCRS, targetCRS, owner);
                } else if (isConversion && (sourceCRS == null || targetCRS == null)) {
                    constructor = (factory, metadata) -> factory.createDefiningConversion(metadata, operationMethod, parameterValues);
                } else if (isConcatenated) {
                    /*
                     * Concatenated operation: the current `ResulSet` may be closed, because
                     * we are going to invoke this method recursively in the following lines.
                     */
                    final CoordinateOperation[] operations = createComponents(
                            GeodeticAuthorityFactory::createCoordinateOperation,
                            "Coordinate_Operation Path",
                            "SELECT SINGLE_OPERATION_CODE"
                                    + " FROM \"Coordinate_Operation Path\""
                                    + " WHERE (CONCAT_OPERATION_CODE = ?)"
                                    + " ORDER BY OP_PATH_STEP", epsg).toArray(CoordinateOperation[]::new);
                    constructor = (factory, metadata) -> factory.createConcatenatedOperation(metadata, operations);
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
                    final var builder = new ParameterizedTransformBuilder(owner.mtFactory, null);
                    builder.setParameters(parameterValues, true);
                    builder.setSourceAxes(sourceCRS);
                    builder.setTargetAxes(targetCRS);
                    final MathTransform mt = builder.create();
                    if (isTransformation) {
                        operationType = Transformation.class;
                    } else if (isConversion) {
                        operationType = Conversion.class;
                    } else {
                        operationType = SingleOperation.class;
                    }
                    final OperationMethod provider = builder.getMethod().orElse(null);
                    if (provider instanceof DefaultOperationMethod) {                 // SIS-specific
                        final Class<?> s = ((DefaultOperationMethod) provider).getOperationType();
                        if (s != null && operationType.isAssignableFrom(s)) {
                            operationType = s.asSubclass(SingleOperation.class);
                        }
                    }
                    constructor = (factory, metadata) -> {
                        // Following restriction will be removed in a future SIS version if the method is added to GeoAPI.
                        if (factory instanceof DefaultCoordinateOperationFactory) {
                            return ((DefaultCoordinateOperationFactory) factory)
                                    .createSingleOperation(metadata, sourceCRS, targetCRS, null, operationMethod, mt);
                        }
                        throw new UnsupportedOperationException(error().getString(
                                Errors.Keys.UnsupportedImplementation_1, factory.getClass()));
                    };
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
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties(
                        "Coordinate_Operation", epsg, name, null, area, scope, remarks, deprecated);
                properties.put(CoordinateOperations.OPERATION_TYPE_KEY, operationType);
                properties.put(CoordinateOperations.PARAMETERS_KEY, parameterValues);
                properties.put(CoordinateOperation .OPERATION_VERSION_KEY, version);
                properties.put(CoordinateOperation .COORDINATE_OPERATION_ACCURACY_KEY,
                               PositionalAccuracyConstant.transformation(accuracy));
                CoordinateOperation operation = constructor.create(owner.copFactory, properties);
                returnValue = ensureSingleton(operation, returnValue, code);
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateOperation.class, code, exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
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
                    sql = "SELECT COORD_OP_CODE"
                            + " FROM \"Coordinate_Operation\" AS CO"
                            + " JOIN \"Area\" ON AREA_OF_USE_CODE = AREA_CODE"
                            + " WHERE CO.DEPRECATED=0"   // Do not put spaces around "=" - SQLTranslator searches for this exact match.
                            + " AND SOURCE_CRS_CODE = ?"
                            + " AND TARGET_CRS_CODE = ?"
                            + " ORDER BY COORD_OP_ACCURACY ASC NULLS LAST, "
                            + " (AREA_EAST_BOUND_LON - AREA_WEST_BOUND_LON + CASE WHEN AREA_EAST_BOUND_LON < AREA_WEST_BOUND_LON THEN 360 ELSE 0 END)"
                            + " * (AREA_NORTH_BOUND_LAT - AREA_SOUTH_BOUND_LAT)"
                            + " * COS(RADIANS(AREA_NORTH_BOUND_LAT + AREA_SOUTH_BOUND_LAT)/2) DESC";
                } else {
                    key = "ConversionFromCRS";
                    sql = "SELECT PROJECTION_CONV_CODE"
                            + " FROM \"Coordinate Reference System\""
                            + " WHERE BASE_CRS_CODE = ?"
                            + " AND COORD_REF_SYS_CODE = ?";
                }
                final Integer targetKey = searchTransformations ? null : pair[1];
                try (ResultSet result = executeQueryForCodes(key, sql, pair)) {
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
            throw new FactoryException(exception.getLocalizedMessage(), Exceptions.unwrap(exception));
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
                try (ResultSet result = executeMetadataQuery(
                        "Supersession",
                        "SELECT SUPERSEDED_BY FROM \"Supersession\""
                                + " WHERE OBJECT_TABLE_NAME=? AND OBJECT_CODE=?"
                                + " ORDER BY SUPERSESSION_YEAR DESC",
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
        return new FactoryException(error().getString(Errors.Keys.DatabaseError_2, type, code), Exceptions.unwrap(cause));
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
            throw new FactoryException(Exceptions.unwrap(exception));
        }
    }
}
