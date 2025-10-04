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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.function.ToIntFunction;
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
import java.time.DateTimeException;
import java.time.LocalDate;
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
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
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
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.internal.DeferredCoordinateOperation;
import org.apache.sis.referencing.internal.DeprecatedCode;
import org.apache.sis.referencing.internal.Epoch;
import org.apache.sis.referencing.internal.ParameterizedTransformBuilder;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.referencing.internal.SignReversalComment;
import org.apache.sis.referencing.internal.VerticalDatumTypes;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.ServicesForMetadata;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.referencing.internal.shared.CoordinateOperations;
import org.apache.sis.referencing.internal.shared.ReferencingFactoryContainer;
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
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.iso.Types;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicDescription;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.sql.internal.shared.SQLUtilities;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.pending.jdk.JDK16;

// Specific to the main branch:
import org.opengis.referencing.ObjectFactory;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.referencing.cs.DefaultParametricCS;
import org.apache.sis.referencing.datum.AbstractDatum;
import org.apache.sis.referencing.datum.DefaultParametricDatum;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.temporal.TemporalDate;


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
     * An option added to the code of a parameter descriptor for specifying the type of parameter values.
     * This is an undocumented extension specific to Apache <abbr>SIS</abbr>
     *
     * @see #getParameterType(int, Map)
     * @see #separateOptions(String, Map)
     * @see #createParameterDescriptor(String)
     */
    private static final String PARAMETER_TYPE_OPTION = "type";

    /**
     * Possible value for {@link #PARAMETER_TYPE_OPTION}.
     */
    private static final String URI_TYPE = "URI";

    /**
     * An option added to the code of a parameter descriptor for specifying the {@code uom_code} integer value.
     * The same operation method may have different units of measurement and "sign reversal" flag depending on
     * the operation which uses it, at least in the way that the <abbr>EPSG</abbr> database is structured.
     * This is an undocumented extension specific to Apache <abbr>SIS</abbr>
     *
     * <h4>Example</h4>
     * The EPSG:8617 (<cite>Coordinate 1 of evaluation point</cite>) parameter may be used in the
     * <abbr>EPSG</abbr> database with either meters or degrees units, depending on which operation
     * uses that parameter.
     *
     * @see #getParameterUnit(int, Map)
     * @see #separateOptions(String, Map)
     * @see #createParameterDescriptor(String)
     */
    private static final String UOM_CODE_OPTION = "uom_code";

    /**
     * An option added to the code of a parameter descriptor for specifying the {@code sign_reversal} Boolean value.
     * This is an undocumented extension specific to Apache <abbr>SIS</abbr>
     *
     * @see #getSignReversal(int, Map)
     * @see #separateOptions(String, Map)
     * @see #createParameterDescriptor(String)
     */
    private static final String SIGN_REVERSAL_OPTION = "sign_reversal";

    /**
     * The namespace of EPSG names and codes. This namespace is needed by all {@code createFoo(String)} methods.
     * The {@code EPSGDataAccess} constructor relies on the {@link EPSGFactory#nameFactory} caching mechanism
     * for giving us the same {@code NameSpace} instance than the one used by previous {@code EPSGDataAccess}
     * instances, if any.
     */
    private final NameSpace namespace;

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
    private final Map<Object, CloseableReference> authorityCodes = new HashMap<>();

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
     * However, this duplication should not happen often. For example, each conventional <abbr>RS</abbr> should appear
     * in only one datum ensemble created by {@link #createDatumEnsemble(Integer, Map)}.
     *
     * <p>Keys are {@link Long} except the keys for naming systems which are {@link String}.
     * The {@code Long} values are computed by {@link #cacheKey(int, int)}.</p>
     *
     * @see #getAxisName(Integer)
     * @see #getRealizationMethod(Integer)
     * @see #createConventionalRS(Integer)
     * @see #createProperties(TableInfo, Integer, String, CharSequence, String, String, CharSequence, boolean)
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
     * code(s) of the object to search. It is legal to have two queries in progress on the same table, provided
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
     * @param  type  the type of spatial reference objects for which to get the authority codes.
     * @return the set of authority codes for spatial reference objects of the given type (may be an empty set).
     * @throws FactoryException if access to the underlying database failed.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        try {
            if (connection.isClosed()) {
                throw new FactoryException(error().getString(Errors.Keys.ConnectionClosed));
            }
            final AuthorityCodes codes = getCodeMap(Objects.requireNonNull(type), null, true);
            if (codes != null) {
                return codes.keySet();
            }
        } catch (SQLException exception) {
            throw new FactoryException(exception.getLocalizedMessage(), Exceptions.unwrap(exception));
        }
        return Set.of();
    }

    /**
     * Puts all codes in the given collection. This method is used only as a fallback when {@link EPSGCodeFinder}
     * cannot get a smaller list of authority codes by using {@code WHERE} conditions on property values.
     * This method should not be invoked for the most common objects such as <abbr>CRS</abbr> and datum.
     * This method may do nothing if getting all codes would be too expensive
     * (especially since the caller would instantiate all enumerated objects).
     *
     * @param  object  the object to search in the database.
     * @param  addTo   the collection where to add all codes.
     * @return whether the collection has changed as a result of this method call.
     * @throws SQLException if an error occurred while querying the database.
     */
    final boolean getAuthorityCodes(final IdentifiedObject object, final Collection<Integer> addTo) throws SQLException {
        final AuthorityCodes codes = getCodeMap(TableInfo.toCacheKey(object), null, false);
        return (codes != null) && codes.getAllCodes(addTo);
    }

    /**
     * Returns a map of <abbr>EPSG</abbr> authority codes as keys and object names as values.
     * The cautions documented in {@link #getAuthorityCodes(Class)} apply also to this map.
     * If the given type is unsupported or too generic, returns {@code null}.
     *
     * @param  cacheKey  object class or {@link TableInfo#toCacheKey(IdentifiedObject)} value.
     * @param  source    the table from which to get the authority codes, or {@code null} for automatic.
     * @param  publish   whether the returned authority codes will be given to a user outside this package.
     * @return the map of authority codes associated to their names, or {@code null} if unsupported.
     * @throws FactoryException if access to the underlying database failed.
     *
     * @see #getAuthorityCodes(Class)
     * @see #getDescriptionText(Class, String)
     */
    private synchronized AuthorityCodes getCodeMap(final Object cacheKey, TableInfo source, boolean publish)
            throws SQLException
    {
        CloseableReference reference = authorityCodes.get(cacheKey);
        if (reference != null) {
            AuthorityCodes existing = reference.get();
            if (existing != null) {
                reference.published |= publish;
                return existing;
            }
        }
        if (source != null) {
            assert source.isSpecificEnough() && source.type.isAssignableFrom(TableInfo.typeOfCacheKey(cacheKey)) : source;
        } else {
            final Class<?> userType = TableInfo.typeOfCacheKey(cacheKey);
            for (TableInfo candidate : TableInfo.values()) {
                if (candidate.isSpecificEnough() && candidate.type.isAssignableFrom(userType)) {
                    if (source != null) {
                        return null;        // The specified type is too generic.
                    }
                    source = candidate;
                }
            }
            if (source == null) {
                return null;                // The specified type is unsupported.
            }
        }
        AuthorityCodes codes = new AuthorityCodes(source, cacheKey, this);
        /*
         * Maybe an instance already existed but was not found above because the user specified some
         * implementation class instead of an interface class. Before to return a newly created map,
         * check again in the cached maps using the type computed by AuthorityCodes itself.
         */
        reference = authorityCodes.get(codes.cacheKey);
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
            authorityCodes.put(codes.cacheKey, reference);
        }
        if (cacheKey != codes.cacheKey) {
            authorityCodes.put(cacheKey, reference);
        }
        reference.published |= publish;
        return codes;
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
            final AuthorityCodes codes = getCodeMap(Objects.requireNonNull(type), null, false);
            if (codes != null) {
                final String text = codes.get(code);
                if (text != null) {
                    return Optional.of(new SimpleInternationalString(text));
                }
            }
        } catch (SQLException | BackingStoreException exception) {
            throw new FactoryException(exception.getLocalizedMessage(), Exceptions.unwrap(exception));
        }
        return Optional.empty();
    }

    /**
     * Formats a code with options. The returned string can be parsed by {@link #separateOptions(String, Map)}.
     * This is used for passing options when creating an object from an authority code through public <abbr>API</abbr>.
     * Formatting options with the authority code allows to differentiate variants in the {@linkplain #owner} cache.
     * This is specific to Apache <abbr>SIS</abbr>.
     *
     * @param  code     the raw authority code, without options.
     * @param  options  the options to format in order. Should be a map with deterministic entry order.
     * @return the given code with the given options appended.
     */
    private static String codeWithOptions(final String code, final Map<String, String> options) {
        if (options.isEmpty()) {
            return code;
        }
        final var s = new StringBuilder(code);
        char separator = '?';
        for (final Map.Entry<String, String> entry : options.entrySet()) {
            s.append(separator).append(entry.getKey()).append('=').append(entry.getValue());
            separator = '&';
        }
        return s.toString();
    }

    /**
     * If the given code has any options, stores them in the given map and returns the code alone.
     * The keys are strings defined by the {@code *_OPTION} constants in this class.
     * Example: {@code 8623?uom_code=9001&sign_reversal=false} (a parameter descriptor).
     *
     * @param  code     the code potentially followed by options.
     * @param  options  the map where to store the options.
     * @return the code without options.
     *
     * @see #UOM_CODE_OPTION
     * @see #SIGN_REVERSAL_OPTION
     * @see #createParameterDescriptor(String)
     */
    private static String separateOptions(final String code, final Map<String, String> options) {
        final int s = code.indexOf('?');
        if (s < 0) {
            return code;
        }
        for (String option : (String[]) CharSequences.split(code.substring(s+1), '&')) {
            final int e = option.indexOf('=');
            if (s >= 0) {
                options.put(option.substring(0, e).trim().toLowerCase(Locale.US), option.substring(e+1).trim());
            }
        }
        return code.substring(0, s);
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
     * @param  source  the table where the code should appear, or {@code null} for no search by name.
     * @param  codes   the codes or names to convert to primary keys, as an array of length 1 or 2.
     * @return the numerical identifiers (i.e. the table primary key values).
     * @throws SQLException if an error occurred while querying the database.
     * @throws FactoryDataException if code is a name and two distinct numerical codes match the name.
     * @throws NoSuchAuthorityCodeException if code is a name and no numerical code match the name.
     */
    private int[] toPrimaryKeys(final TableInfo source, final String... codes) throws SQLException, FactoryException {
        final int[] primaryKeys = new int[codes.length];
        for (int i=0; i<codes.length; i++) {
            String code = codes[i];
            if (source != null && !isPrimaryKey(code)) {
                /*
                 * The given string is not a numerical code. Search the value in the database.
                 * We search first in the table of the query. If the name is not found there,
                 * then we will search in the aliases table as a fallback.
                 */
                final var result = new ArrayList<Integer>();
                final String pattern = toLikePattern(code);
                findCodesFromName(source, source.type, pattern, code, result);
                if (result.isEmpty()) {
                    // Search in aliases only if no match was found in primary names.
                    findCodesFromAlias(source, pattern, code, result);
                }
                Integer resolved = null;
                for (Integer value : result) {
                    if (resolved == null) {
                        resolved = value;
                    } else if (!resolved.equals(value)) {
                        /*
                         * Cannot use `ensureSingleton(…)` because we really need the exception type to be
                         * `NoSuchAuthorityCodeException`, as there are callers expecting that specific type
                         * in their `catch` statements. It can be understood as "no unambiguous identifier".
                         */
                        throw new NoSuchAuthorityCodeException(
                                error().getString(Errors.Keys.DuplicatedIdentifier_1, code),
                                Constants.EPSG, code);
                    }
                }
                if (resolved != null) {
                    primaryKeys[i] = resolved;
                    continue;
                }
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
                        Constants.EPSG, code).initCause(e);
            }
        }
        return primaryKeys;
    }

    /**
     * Returns the given object name as a pattern which can be used in a {@code LIKE} clause.
     * This method does not change the character case for avoiding the need to use {@code LOWER}
     * in the <abbr>SQL</abbr> statement (because it may prevent the use of the database index).
     */
    final String toLikePattern(final String name) {
        return SQLUtilities.toLikePattern(name, false, translator.wildcardEscape);
    }

    /**
     * Finds the authority codes for the given name.
     *
     * @param  source    information about the table where the code should appear.
     * @param  cacheKey  object class or {@link TableInfo#toCacheKey(IdentifiedObject)} value.
     * @param  pattern   the name to search as a pattern that can be used with {@code LIKE}.
     * @param  name      the original name. This is a temporary workaround for a Derby bug (see {@code filterFalsePositive(…)}).
     * @param  addTo     the collection where to add the codes that have been found.
     * @throws SQLException if an error occurred while querying the database.
     */
    final void findCodesFromName(final TableInfo source, final Object cacheKey, final String pattern, final String name,
                                 final Collection<Integer> addTo) throws SQLException
    {
        AuthorityCodes codes = getCodeMap(cacheKey, source, false);
        if (codes != null) {
            codes.findCodesFromName(pattern, name, addTo);
        }
    }

    /**
     * Finds the authority codes for the given alias.
     *
     * @param  source   information about the table where the code should appear.
     * @param  pattern  the name to search as a pattern that can be used with {@code LIKE}.
     * @param  name     the original name. This is a temporary workaround for a Derby bug (see {@code filterFalsePositive(…)}).
     * @param  addTo    the collection where to add the codes that have been found.
     * @throws SQLException if an error occurred while querying the database.
     */
    final void findCodesFromAlias(final TableInfo source, final String pattern, final String name, final Collection<Integer> addTo)
            throws SQLException
    {
        final PreparedStatement stmt = prepareStatement(
                "AliasKey",
                "SELECT OBJECT_CODE, ALIAS"
                        + " FROM \"Alias\""
                        + " WHERE OBJECT_TABLE_NAME=? AND ALIAS LIKE ?");
        stmt.setString(1, translator.toActualTableName(source.table));
        stmt.setString(2, pattern);
        try (ResultSet result = stmt.executeQuery()) {
            while (result.next()) {
                if (SQLUtilities.filterFalsePositive(name, result.getString(2))) {
                    addTo.add(getOptionalInteger(result, 1));
                }
            }
        }
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
     * @param  source  information about the table where the code should appear.
     * @param  sql     the <abbr>SQL</abbr> statement to use for creating the {@link PreparedStatement} object.
     *                 Will be used only if no prepared statement was already created for the given code.
     * @param  codes   the codes of the object to create, as an array of length 1 or 2.
     * @return the result of the query.
     * @throws SQLException if an error occurred while querying the database.
     */
    private ResultSet executeSingletonQuery(final TableInfo source, final String sql, final String... codes)
            throws SQLException, FactoryException
    {
        assert Thread.holdsLock(this);
        assert source.validate(sql) : source;
        final int[] keys = toPrimaryKeys(source, codes);
        currentSingletonQuery = new QueryID(source.table, keys, currentSingletonQuery);
        if (currentSingletonQuery.isAlreadyInProgress()) {
            throw new FactoryDataException(resources().getString(
                    Resources.Keys.RecursiveCreateCallForCode_2,
                    source.type.getSimpleName(),
                    (codes.length == 1) ? codes[0] : Arrays.toString(codes)));
        }
        return executeQueryForCodes(source.table, sql, keys);
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
     * @throws DateTimeException if the date cannot be parsed.
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
        return Epoch.fromYear(getOptionalDouble(result, columnIndex), 0);
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
     * The given code is a value of the {@code scopes} list
     * after a call to {@link #getUsages getUsages(…)}.
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
     * Gets the codes of extents and scopes of the object identified by the given code in the given table.
     * The {@code actualTable} argument must be the result of {@code translator.toActualTableName(table)}.
     * The {@code extents} and {@code scopes} collections should be initially empty and will be filled by
     * this method. The same number of codes will be added in both of them.
     *
     * @param  actualTable  actual name of the table of the object for which to get the usages.
     * @param  code         <abbr>EPSG</abbr> code of the object for which to get the usages.
     * @param  extents      where to store extent codes, or {@code null} for ignoring extents.
     * @param  scopes       where to store usage codes, or {@code null} for ignoring scopes.
     */
    private void getUsages(final String actualTable,
                           final int code,
                           final Collection<String>  extents,
                           final Collection<Integer> scopes) throws SQLException, FactoryDataException
    {
        try (ResultSet result = executeMetadataQuery("Usage",
                "SELECT EXTENT_CODE, SCOPE_CODE FROM \"Usage\""
                        + " WHERE OBJECT_TABLE_NAME=? AND OBJECT_CODE=?",
                actualTable, code))
        {
            while (result.next()) {
                if (extents != null) extents.add(getString (code, result, 1));
                if (scopes  != null) scopes .add(getInteger(code, result, 2));
            }
        }
    }

    /**
     * Logs a warning saying that the given code is deprecated and returns the code of the proposed replacement.
     *
     * @param  source  information about the table where the deprecated object is found.
     * @param  code    the deprecated code.
     * @return the proposed replacement (may be the "(none)" text). Never empty.
     */
    private String getReplacement(final TableInfo source, final int code, final Locale locale) throws SQLException {
        String reason = null;
        String replacedBy;
search: try (ResultSet result = executeMetadataQuery("Deprecation",
                "SELECT DEPRECATION_REASON, REPLACED_BY"
                        + " FROM \"Deprecation\""
                        + " WHERE OBJECT_TABLE_NAME=?"
                        + " AND OBJECT_CODE=?",
                translator.toActualTableName(source.table), code))
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
                    "create".concat(source.type.getSimpleName()),
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
     * Returns the identifier for the {@link IdentifiedObject} to construct.
     *
     * @param  source       information about the table on which a query has been executed.
     * @param  code         the <abbr>EPSG</abbr> code of the object to construct.
     * @param  identifier   the code to assign to the identifier, usually {@code code.toString()}.
     * @param  description  a description associated with the identifier. May be {@code null}.
     * @param  deprecated   {@code true} if the object to create is deprecated.
     * @return an identifier for the object to create.
     */
    private ReferenceIdentifier createIdentifier(final TableInfo source, final int code, final String identifier,
            final InternationalString description, final Locale locale, final boolean deprecated)
            throws SQLException, FactoryException
    {
        final Citation authority = owner.getAuthority();
        final String version = Types.toString(authority.getEdition(), locale);
        if (deprecated) {
            final String replacedBy = getReplacement(source, code, locale);
            return new DeprecatedCode(
                    authority,
                    Constants.EPSG,
                    identifier,
                    version,
                    description,
                    Character.isDigit(replacedBy.charAt(0)) ? replacedBy : null,
                    Vocabulary.formatInternational(Vocabulary.Keys.SupersededBy_1, replacedBy));
        } else {
            return new ImmutableIdentifier(
                    authority,
                    Constants.EPSG,
                    identifier,
                    version,
                    description);
        }
    }

    /**
     * Returns the identifier, name and aliases for the {@link IdentifiedObject} to construct.
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
     * @param  source      information about the table on which a query has been executed.
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
    private Map<String,Object> createProperties(final TableInfo    source,
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
        final String actualTable = translator.toActualTableName(source.table);
        /*
         * Get all domains for the object identified by the given code.
         * The table used nere is new in version 10 of EPSG database.
         * We have to create the extents outside the `while` loop for
         * the same reason as above for `extent`.
         */
        DefaultObjectDomain[] domains = null;
        if (translator.isUsageTableFound()) {
            final var extents = new ArrayList<String>();
            final var scopes  = new ArrayList<Integer>();
            getUsages(actualTable, code, extents, scopes);
            if (!extents.isEmpty()) {
                domains = new DefaultObjectDomain[extents.size()];
                for (int i=0; i<domains.length; i++) {
                    domains[i] = new DefaultObjectDomain(
                            getScope(scopes.get(i)),
                            owner.createExtent(extents.get(i)));
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
                actualTable, code))
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
        properties.put(IdentifiedObject.IDENTIFIERS_KEY, createIdentifier(
                source, code, code.toString(), scopedName.toInternationalString(), locale, deprecated));
        if (!aliases.isEmpty()) {
            properties.put(IdentifiedObject.ALIAS_KEY, aliases.toArray(GenericName[]::new));
        }
        if (deprecated) {
            properties.put(AbstractIdentifiedObject.DEPRECATED_KEY, Boolean.TRUE);
        }
        properties.put(IdentifiedObject.REMARKS_KEY, remarks);
        properties.put(AbstractIdentifiedObject.LOCALE_KEY, locale);
        properties.put(ReferencingFactoryContainer.MT_FACTORY, owner.mtFactory);
        if (domains != null) {
            properties.put(AbstractIdentifiedObject.DOMAINS_KEY, domains);
        }
        if (scope != null && !scope.equals(UNKNOWN_SCOPE)) {    // Should be always NULL since EPSG version 10.
            properties.put(Datum.SCOPE_KEY, scope);
        }
        if (extent != null) {                                   // Should be always NULL since EPSG version 10.
            properties.put(Datum.DOMAIN_OF_VALIDITY_KEY, extent);
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
        TableInfo found = null;
        try {
            final int key = isPrimaryKey ? toPrimaryKeys(null, code)[0] : 0;
            for (final TableInfo source : TableInfo.values()) {
                if (!(source.isSpecificEnough() && IdentifiedObject.class.isAssignableFrom(source.type))) {
                    continue;
                }
                final String column = isPrimaryKey ? source.codeColumn : source.nameColumn;
                query.setLength(queryStart);
                query.append(source.codeColumn);
                if (!isPrimaryKey) {
                    query.append(", ").append(column);      // Only for filterFalsePositive(…).
                }
                query.append(" FROM ").append(source.fromClause)
                     .append(" WHERE ").append(column).append(isPrimaryKey ? " = ?" : " LIKE ?");
                try (PreparedStatement stmt = connection.prepareStatement(translator.apply(query.toString()))) {
                    /*
                     * Check if at least one record is found for the code or the name.
                     * Ensure that there is not two values for the same code or name.
                     */
                    if (isPrimaryKey) {
                        stmt.setInt(1, key);
                    } else {
                        stmt.setString(1, toLikePattern(code));
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
                        if (found != null) {
                            throw new FactoryDataException(error().getString(Errors.Keys.DuplicatedIdentifier_1, code));
                        }
                        found = source;
                    }
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(IdentifiedObject.class, code, exception);
        }
        /*
         * If a record has been found in one table, then delegates to the appropriate method.
         */
        if (found != null) {
            switch (found) {
                case CRS:            return createCoordinateReferenceSystem(code);
                case CS:             return createCoordinateSystem         (code);
                case AXIS:           return createCoordinateSystemAxis     (code);
                case DATUM:          return createDatum                    (code);
                case ELLIPSOID:      return createEllipsoid                (code);
                case PRIME_MERIDIAN: return createPrimeMeridian            (code);
                case OPERATION:      return createCoordinateOperation      (code);
                case METHOD:         return createOperationMethod          (code);
                case PARAMETER:      return createParameterDescriptor      (code);
                case UNIT:           break; // Cannot cast Unit to IdentifiedObject
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
     * Invokes the {@code create(…)} method of the given constructor in a block which ensures that recursive method
     * calls will be redirected to this factory. This is needed only when the constructor may indirectly invoke the
     * {@link org.apache.sis.referencing.CRS#getAuthorityFactory(String)} method.
     */
    private <F extends Factory, R extends IdentifiedObject> R create(
            final FactoryCall<F, R> constructor,
            final F factory,
            final Map<String, Object> properties) throws FactoryException
    {
        final ThreadLocal<CRSAuthorityFactory> caller = ParameterizedTransformBuilder.CREATOR;
        final CRSAuthorityFactory old = caller.get();
        caller.set(owner);
        try {
            return constructor.create(factory, properties);
        } finally {
            if (old != null) {
                caller.set(old);
            } else {
                caller.remove();
            }
        }
    }

    /**
     * Returns the geodetic factory to use by casting the given one if possible.
     */
    private static GeodeticObjectFactory extended(final ObjectFactory factory) {
        return (factory instanceof GeodeticObjectFactory)
                ? (GeodeticObjectFactory) factory
                : GeodeticObjectFactory.provider();
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
                TableInfo.CRS,
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
                 * The following switch statement should have a case for all "CRS Kind" values enumerated
                 * in the `Prepare.sql` file, except that the values in this Java code are in lower cases.
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
                        final DefaultDatumEnsemble<GeodeticDatum> ensemble = DatumOrEnsemble.asEnsemble(datumOrEnsemble).orElse(null);
                        final GeodeticDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        if (cs instanceof CartesianCS) {
                            final var c = (CartesianCS) cs;
                            constructor = (factory, metadata) ->
                                    (ensemble != null) ? extended(factory).createGeodeticCRS(metadata, datum, ensemble, c)
                                                       : factory.createGeocentricCRS(metadata, datum, c);
                        } else if (cs instanceof SphericalCS) {
                            final var c = (SphericalCS) cs;
                            constructor = (factory, metadata) ->
                                    (ensemble != null) ? extended(factory).createGeodeticCRS(metadata, datum, ensemble, c)
                                                       : factory.createGeocentricCRS(metadata, datum, c);
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
                        final DefaultDatumEnsemble<GeodeticDatum> ensemble = DatumOrEnsemble.asEnsemble(datumOrEnsemble).orElse(null);
                        final GeodeticDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) ->
                                (ensemble != null) ? extended(factory).createGeographicCRS(metadata, datum, ensemble, cs)
                                                   : factory.createGeographicCRS(metadata, datum, cs);
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
                                 * For a ProjectedCRS, the baseCRS is usually geodetic. However, geocentric CRS
                                 * is also allowed, but not yet supported in the code below. We could also have
                                 * a ProjectedCRS derived from another ProjectedCRS.
                                 */
                                if (baseCRS instanceof GeographicCRS) {
                                    return factory.createProjectedCRS(metadata, (GeographicCRS) baseCRS, fromBase, cs);
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
                        final DefaultDatumEnsemble<VerticalDatum> ensemble = DatumOrEnsemble.asEnsemble(datumOrEnsemble).orElse(null);
                        final VerticalDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) ->
                                (ensemble != null) ? extended(factory).createVerticalCRS(metadata, datum, ensemble, cs)
                                                   : factory.createVerticalCRS(metadata, datum, cs);
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
                        final DefaultDatumEnsemble<TemporalDatum> ensemble = DatumOrEnsemble.asEnsemble(datumOrEnsemble).orElse(null);
                        final TemporalDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) ->
                                (ensemble != null) ? extended(factory).createTemporalCRS(metadata, datum, ensemble, cs)
                                                   : factory.createTemporalCRS(metadata, datum, cs);
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
                        final DefaultDatumEnsemble<EngineeringDatum> ensemble = DatumOrEnsemble.asEnsemble(datumOrEnsemble).orElse(null);
                        final EngineeringDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) ->
                                (ensemble != null) ? extended(factory).createEngineeringCRS(metadata, datum, ensemble, cs)
                                                   : factory.createEngineeringCRS(metadata, datum, cs);
                        break;
                    }
                    /* ──────────────────────────────────────────────────────────────────────
                     *   PARAMETRIC CRS
                     * ────────────────────────────────────────────────────────────────────── */
                    case "parametric": {
                        final String csCode    = getString(code, result, 8);
                        final String datumCode = getString(code, result, 9);
                        final DefaultParametricCS cs = owner.createParametricCS(csCode);    // Do not inline the `getString(…)` calls.
                        final DefaultParametricDatum datumOrEnsemble = owner.createParametricDatum(datumCode);
                        final DefaultDatumEnsemble<DefaultParametricDatum> ensemble = null; // TODO: not yet implemented.
                        final DefaultParametricDatum datum = (ensemble == null) ? datumOrEnsemble : null;
                        constructor = (factory, metadata) -> extended(factory).createParametricCRS(metadata, datum, ensemble, cs);
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
                        TableInfo.CRS, epsg, name, null, area, scope, remarks, deprecated);
                final CoordinateReferenceSystem crs = create(constructor, owner.crsFactory, properties);
                returnValue = ensureSingleton(crs, returnValue, code);
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(CoordinateReferenceSystem.class, code, exception);
        } catch (ClassCastException exception) {
            throw new FactoryDataException(exception.getLocalizedMessage(), exception);
        } finally {
            currentSingletonQuery = previousSingletonQuery;
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
                TableInfo.DATUM,
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
                 * The following switch statement should have a case for all "Datum Kind" values enumerated
                 * in the `Prepare.sql` file, except that the values in this Java code are in lower cases.
                 */
                switch (type.toLowerCase(Locale.US)) {
                    case "dynamic geodetic":
                    case "geodetic": {
                        final String ellipsoidCode   = getString(code, result, 12);
                        final String meridianCode    = getString(code, result, 13);
                        final Ellipsoid ellipsoid    = owner.createEllipsoid(ellipsoidCode);  // Do not inline the `getString(…)` calls.
                        final PrimeMeridian meridian = owner.createPrimeMeridian(meridianCode);
                        constructor = (factory, metadata) ->
                                (dynamic != null) ? extended(factory).createGeodeticDatum(metadata, ellipsoid, meridian, dynamic)
                                                  : factory.createGeodeticDatum(metadata, ellipsoid, meridian);
                        break;
                    }
                    case "vertical": {
                        final VerticalDatumType method = getRealizationMethod(getOptionalInteger(result, 14));
                        constructor = (factory, metadata) ->
                                (dynamic != null) ? extended(factory).createVerticalDatum(metadata, method, dynamic)
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
                        constructor = (factory, metadata) -> factory.createTemporalDatum(metadata, TemporalDate.toDate(originDate));
                        break;
                    }
                    /*
                     * Straightforward cases.
                     */
                    case "engineering": constructor = DatumFactory::createEngineeringDatum; break;
                    case "parametric":  constructor = (factory, metadata) -> extended(factory).createParametricDatum(metadata);  break;
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
                        TableInfo.DATUM, epsg, name, null, area, scope, remarks, deprecated);
                properties.put(AbstractDatum.ANCHOR_DEFINITION_KEY, anchor);
                properties.put(AbstractDatum.ANCHOR_EPOCH_KEY,      epoch);
                properties.put(AbstractDatum.PUBLICATION_DATE_KEY,  publish);
                properties.put(AbstractDatum.CONVENTIONAL_RS_KEY,   conventionalRS);
                properties.values().removeIf(Objects::isNull);
                final Datum datum = constructor.create(owner.datumFactory, properties);
                returnValue = ensureSingleton(datum, returnValue, code);
                if (result.isClosed()) break;   // See createProperties(…) for explanation.
            }
        } catch (SQLException exception) {
            throw databaseFailure(Datum.class, code, exception);
        } catch (DateTimeException exception) {
            throw new FactoryDataException(exception.getLocalizedMessage(), exception);
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
                "Datum Ensemble",
                "SELECT ENSEMBLE_ACCURACY"
                        + " FROM \"Datum Ensemble\""
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
                "Datum Ensemble Member",
                "SELECT DATUM_CODE"
                        + " FROM \"Datum Ensemble Member\""
                        + " WHERE DATUM_ENSEMBLE_CODE = ?"
                        + " ORDER BY DATUM_SEQUENCE", code);
        return (factory, metadata) -> extended(factory).createDatumEnsemble(metadata, members, accuracy);
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
        C create(GeodeticAuthorityFactory factory, String code) throws SQLException, FactoryException;
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
                    TableInfo.CONVENTIONAL_RS.table,
                    "SELECT"+ /* column 1 */ " CONVENTIONAL_RS_CODE,"
                            + /* column 2 */ " CONVENTIONAL_RS_NAME,"
                            + /* column 3 */ " REMARKS,"
                            + /* column 4 */ " DEPRECATED"
                            + " FROM \"Conventional RS\""
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
                            TableInfo.CONVENTIONAL_RS, epsg, name, null, null, null, remarks, deprecated);
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
                TableInfo.ELLIPSOID,
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
                final String  uom_code          = getString   (code, result, 6);
                final String  remarks           = getOptionalString (result, 7);
                final boolean deprecated        = getOptionalBoolean(result, 8);
                final Unit<Length> unit         = owner.createUnit(uom_code).asType(Length.class);
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
                        TableInfo.ELLIPSOID, epsg, name, null, null, null, remarks, deprecated);
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
                TableInfo.PRIME_MERIDIAN,
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
                final String  uom_code   = getString   (code, result, 4);
                final String  remarks    = getOptionalString (result, 5);
                final boolean deprecated = getOptionalBoolean(result, 6);
                final Unit<Angle> unit = owner.createUnit(uom_code).asType(Angle.class);
                /*
                 * Map of properties should be populated only after we extracted all
                 * information needed from the `ResultSet`, because it may be closed.
                 */
                final PrimeMeridian primeMeridian = owner.datumFactory.createPrimeMeridian(
                        createProperties(TableInfo.PRIME_MERIDIAN, epsg, name, null, null, null, remarks, deprecated),
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
                TableInfo.EXTENT,
                "SELECT"+ /* column  1 */ " EXTENT_CODE,"
                        + /* column  2 */ " EXTENT_NAME,"
                        + /* column  3 */ " EXTENT_DESCRIPTION,"
                        + /* column  4 */ " BBOX_SOUTH_BOUND_LAT,"
                        + /* column  5 */ " BBOX_NORTH_BOUND_LAT,"
                        + /* column  6 */ " BBOX_WEST_BOUND_LON,"
                        + /* column  7 */ " BBOX_EAST_BOUND_LON,"
                        + /* column  8 */ " VERTICAL_EXTENT_MIN,"
                        + /* column  9 */ " VERTICAL_EXTENT_MAX,"
                        + /* column 10 */ " VERTICAL_EXTENT_CRS_CODE,"
                        + /* column 11 */ " TEMPORAL_EXTENT_BEGIN,"
                        + /* column 12 */ " TEMPORAL_EXTENT_END,"
                        + /* column 13 */ " DEPRECATED"
                        + " FROM \"Extent\""
                        + " WHERE EXTENT_CODE = ?", code))
        {
            while (result.next()) {
                Integer  epsg        = getOptionalInteger (result,  1);
                String   name        = getOptionalString  (result,  2);
                String   description = getOptionalString  (result,  3);
                double   ymin        = getOptionalDouble  (result,  4);
                double   ymax        = getOptionalDouble  (result,  5);
                double   xmin        = getOptionalDouble  (result,  6);
                double   xmax        = getOptionalDouble  (result,  7);
                double   zmin        = getOptionalDouble  (result,  8);
                double   zmax        = getOptionalDouble  (result,  9);
                Temporal tmin        = getOptionalTemporal(result, 11, "createExtent");
                Temporal tmax        = getOptionalTemporal(result, 12, "createExtent");
                boolean  deprecated  = getOptionalBoolean (result, 13);
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
                    final Integer crs = getOptionalInteger(result, 10);
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
                if (epsg != null && name != null) {     // Should never be null, but we don't need to be strict.
                    var identifier = createIdentifier(TableInfo.EXTENT, epsg, name, null, getLocale(), deprecated);
                    extent.getGeographicElements().add(new DefaultGeographicDescription(identifier));
                }
                if (!extent.isEmpty()) {
                    returnValue = ensureSingleton(extent, returnValue, code);
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure(Extent.class, code, exception);
        } catch (DateTimeException exception) {
            throw new FactoryDataException(exception.getLocalizedMessage(), exception);
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
                TableInfo.CS,
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
                        TableInfo.CS, epsg, name, null, null, null, remarks, deprecated);
                /*
                 * The following switch statement should have a case for all "CS Kind" values enumerated
                 * in the `Prepare.sql` file, except that the values in this Java code are in lower cases.
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
                            case 2: cs = extended(csFactory).createSphericalCS(properties, axes[0], axes[1]); break;
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
                            case 1: cs = extended(csFactory).createParametricCS(properties, axes[0]); break;
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
                TableInfo.AXIS,
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
                final String  uom_code     = getString (code, result, 5);
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
                        createProperties(TableInfo.AXIS, epsg, an.name, an.description, null, null, an.remarks, false),
                        abbreviation, direction, owner.createUnit(uom_code));
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
     * Returned as the legacy {@link VerticalDatumType} because
     * {@code RealizationMethod} did not existed in GeoAPI 3.0.
     *
     * @param  code  code of the realization method, or {@code null} if none.
     * @return realization method, or {@code GEOIDAL} if the given code was null.
     */
    private VerticalDatumType getRealizationMethod(final Integer code) throws FactoryException, SQLException {
        assert Thread.holdsLock(this);
        if (code == null) {
            return VerticalDatumType.GEOIDAL;
        }
        final Long cacheKey = cacheKey(2, code);
        var returnValue = (VerticalDatumType) localCache.get(cacheKey);
        if (returnValue == null && code != null) {
            try (ResultSet result = executeQueryForCodes(
                    "Datum Realization Method",
                    "SELECT REALIZATION_METHOD_NAME"
                            + " FROM \"Datum Realization Method\""
                            + " WHERE REALIZATION_METHOD_CODE = ?", code))
            {
                while (result.next()) {
                    final String name = getString(code, result, 1);
                    returnValue = ensureSingleton(VerticalDatumTypes.fromMethod(name), returnValue, code);
                }
            }
            if (returnValue == null) {
                throw noSuchAuthorityCode(VerticalDatumType.class, String.valueOf(code));
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
                TableInfo.UNIT,
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
     * Determines the type of values in a parameter. If the parameter has at least one non-null value in the
     * "Parameter File Name" column, then the value type is assumed to be <abbr>URI</abbr> stored as string.
     * Otherwise, the type is assumed floating point number.
     *
     * @param  parameter  the <abbr>EPSG</abbr> code of the parameter descriptor.
     * @param  options    the options where to store the {@value #PARAMETER_TYPE_OPTION} value.
     */
    private void getParameterType(final int parameter, final Map<String, String> options) throws SQLException {
        try (ResultSet result = executeQueryForCodes(
                "Parameter Type",
                "SELECT PARAM_VALUE_FILE_REF"
                        + " FROM \"Coordinate_Operation Parameter Value\""
                        + " WHERE PARAM_VALUE_FILE_REF IS NOT NULL"
                        + " AND (PARAMETER_CODE = ?)", parameter))
        {
            while (result.next()) {
                String element = getOptionalString(result, 1);
                if (element != null && !element.isBlank()) {
                    options.put(PARAMETER_TYPE_OPTION, URI_TYPE);
                    return;
                }
            }
        }
    }

    /**
     * Determines the most frequently used units of measurement of a parameter.
     * We can have many different units for the same parameter, but usually all units have the same dimension.
     * For example, we may have meters, kilometers, and feet. In such case, the declared unit will be the most
     * frequently used unit. However, some parameters accept units of different dimensions. For example, the
     * "Coordinate 1 of evaluation point" (EPSG:8617) parameter may be in meters or in degrees. In such case,
     * the {@code dimension} argument is necessary for considering only compatible units when searching for
     * the most frequently used unit.
     *
     * @param  parameter  the <abbr>EPSG</abbr> code of the parameter descriptor.
     * @param  options    the options where to store the {@value #UOM_CODE_OPTION} value.
     * @param  dimension  if non-null, consider only the units compatible with {@code dimension}.
     */
    private void getParameterUnit(final int parameter, final Map<String, String> options, final Unit<?> dimension)
            throws SQLException, FactoryException
    {
        try (ResultSet result = executeQueryForCodes(
                "Parameter Unit",
                "SELECT UOM_CODE"
                        + " FROM \"Coordinate_Operation Parameter Value\""
                        + " WHERE (PARAMETER_CODE = ?)"
                        + " GROUP BY UOM_CODE"
                        + " ORDER BY COUNT(UOM_CODE) DESC", parameter))
        {
            while (result.next()) {
                final String uom_code = getOptionalString(result, 1);
                if (uom_code != null) {
                    if (dimension != null) {
                        final Unit<?> candidate = owner.createUnit(uom_code);
                        if (!candidate.isCompatible(dimension)) {
                            continue;
                        }
                    }
                    options.put(UOM_CODE_OPTION, uom_code);
                    return;
                }
            }
        }
    }

    /**
     * Determines if the inverse operation can be performed by reversing the parameter sign.
     * The <abbr>EPSG</abbr> dataset uses "Yes" or "No" value while <abbr>SIS</abbr> scripts
     * use Boolean type. This method accepts the two forms. If a string is not recognized as
     * a Boolean value, then this method throws a {@link SQLException} because a wrong value
     * would let {@code EPSGDataAccess} finishes its work without apparent problem but would
     * cause failures later when Apache <abbr>SIS</abbr> tries to infer an inverse operation.
     * An exception thrown at a later time is much more difficult to relate to the root cause
     * than if we throw the exception here.
     *
     * @param  parameter  the <abbr>EPSG</abbr> code of the parameter descriptor.
     * @param  options    the options where to store the {@value #SIGN_REVERSAL_OPTION} value.
     */
    private void getSignReversal(final int parameter, final Map<String, String> options) throws SQLException {
        try (ResultSet result = executeQueryForCodes(
                "Sign Reversal",
                "SELECT DISTINCT PARAM_SIGN_REVERSAL"
                        + " FROM \"Coordinate_Operation Parameter Usage\""
                        + " WHERE (PARAMETER_CODE = ?)", parameter))
        {
            Boolean reversibility = null;
            while (result.next()) {
                Boolean value;
                if (translator.useBoolean()) {
                    value = result.getBoolean(1);
                    if (result.wasNull()) return;
                } else {
                    // May throw SQLException - see above comment.
                    value = SQLUtilities.parseBoolean(result.getString(1));
                    if (value == null) return;
                }
                if (reversibility == null) reversibility = value;
                else if (!reversibility.equals(value)) return;
            }
            if (reversibility != null) {
                options.put(SIGN_REVERSAL_OPTION, reversibility.toString());
            }
        }
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
        final var options = new HashMap<String, String>(4);
        final String base = separateOptions(code, options);
        /*
         * If the code has options (e.g., "8623?uom_code=9001&sign_reversal=false"),
         * get the descriptor with default options, then amends that descriptor with
         * the given options. We take this approach for leveraging the `owner` cache.
         */
        if (!options.isEmpty()) try {
            final ParameterDescriptor<?> generic = owner.createParameterDescriptor(base);
            final var metadata = new HashMap<String, Object>(IdentifiedObjects.getProperties(generic));
            final var returnValue = createParameterDescriptor(Integer.valueOf(base), metadata, options);
            return generic.equals(returnValue) ? generic : returnValue;     // Share the existing instance.
        } catch (SQLException exception) {
            throw databaseFailure(OperationMethod.class, code, exception);
        }
        /*
         * Case of parameters without options. This case is indirectly executed even when
         * options were present, in order to get the base parameter to be used as template.
         */
        ParameterDescriptor<?> returnValue = null;
        final QueryID previousSingletonQuery = currentSingletonQuery;
        try (ResultSet result = executeSingletonQuery(
                TableInfo.PARAMETER,
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
                getParameterUnit(epsg, options, null);
                getParameterType(epsg, options);
                getSignReversal (epsg, options);
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties(
                        TableInfo.PARAMETER, epsg, name, description, null, null, null, deprecated);
                returnValue = createParameterDescriptor(epsg, properties, options);
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
     * Creates a parameter descriptor from the given properties.
     * The given maps shall be modifiable, as this method will modify them.
     *
     * @param  code      value allocated by EPSG.
     * @param  metadata  the properties fetched from the database or from another descriptor.
     * @param  options   the value type, unit of measurement and sign reversal.
     * @return the parameter descriptor for the given code, properties and options.
     */
    private ParameterDescriptor<?> createParameterDescriptor(final Integer code,
            final Map<String, Object> metadata, final Map<String, String> options)
            throws SQLException, FactoryException
    {
        final Class<?> type;
        NumberRange<?> valueDomain = null;
        if (EPSG_CODE_PARAMETERS.contains(code)) {
            // If the parameter is an EPSG code, the type is integer and there is no unit.
            type = Integer.class;
        } else {
            if (URI_TYPE.equalsIgnoreCase(options.remove(PARAMETER_TYPE_OPTION))) {
                type = String.class;
            } else {
                type = Double.class;
            }
            final String uom_code = options.remove(UOM_CODE_OPTION);
            if (uom_code != null) {
                valueDomain = MeasurementRange.create(
                        Double.NEGATIVE_INFINITY, false,
                        Double.POSITIVE_INFINITY, false,
                        owner.createUnit(uom_code));
            }
        }
        /*
         * Determine if the inverse operation can be performed by reversing the parameter sign.
         * The `SignReversalComment` is used as a sentinel value in other Apache SIS packages,
         * so this information needs to be accurate. For that reason, if we fail to parse the
         * Boolean value, it is better to throw an exception now rather than later.
         */
        metadata.put(IdentifiedObject.REMARKS_KEY, SignReversalComment.of(
                SQLUtilities.parseBoolean(options.remove(SIGN_REVERSAL_OPTION))));
        return new DefaultParameterDescriptor<>(metadata, 1, 1, type, valueDomain, null, null);
    }

    /**
     * Temporary storage for parameter values before they are set in the parameter group.
     *
     * <h2>Purpose</h2>
     * The {@link ParameterDescriptor} and {@link ParameterValue} instances are created together,
     * because the parameter descriptors for the same operation method may vary slightly depending
     * on which coordinate operation use that method. But we cannot set the {@link ParameterValue}s
     * before all descriptors have been created and put in a {@link ParameterValueGroup}.
     * Hence, we temporarily store the values in this object until the group is created.
     */
    private static final class Parameter {
        /**
         * The descriptor of the parameter.
         */
        final ParameterDescriptor<?> descriptor;

        /**
         * Value of the {@code PARAM_VALUE_FILE_REF} column, or {@code null} if none.
         * It may be a file in the {@code "$SIS_DATA/DatumChanges"} directory.
         * Should be relative and <em>not</em> encoded for valid <abbr>URI</abbr> syntax.
         * The encoding will be applied by invoking the {@link URI} multi-argument constructor.
         */
        final String reference;

        /**
         * Value of the {@code PARAMETER_VALUE} column.
         * Ignored if {@code PARAM_VALUE_FILE_REF} is non-null.
         */
        final double value;

        /**
         * Value of the {@code UOM_CODE} column, or {@code null} if none.
         */
        final Unit<?> unit;

        /**
         * Creates a new value for the given descriptor.
         * Callers shall set at least one of the non-final fields after construction.
         */
        Parameter(final ParameterDescriptor<?> descriptor, final String reference, final double value, final Unit<?> unit) {
            this.descriptor = descriptor;
            this.reference  = reference;
            this.value      = value;
            this.unit       = unit;
        }

        /**
         * Returns an operation method with the same metadata than the given method,
         * but with the descriptors of the parameters in the given list.
         * Those parameter descriptors should be the same as the parameters of the given method.
         * But sometime, the parameters differ in units of measurement or in sign reversal flag.
         *
         * @param  generic  a generic operation method for no operation in particular.
         * @return the given operation method potentially fitted to a particular operation.
         */
        static OperationMethod recreateIfChanged(OperationMethod generic, final List<Parameter> values) {
            final List<GeneralParameterDescriptor> existing = generic.getParameters().descriptors();
            final var descriptors = new ParameterDescriptor<?>[values.size()];
            boolean changed = false;
            for (int i=0; i<descriptors.length; i++) {
                ParameterDescriptor<?> descriptor = values.get(i).descriptor;
                final GeneralParameterDescriptor other = existing.get(i);
                if (descriptor.equals(other)) {
                    descriptor = (ParameterDescriptor<?>) other;    // Share existing instances.
                } else {
                    changed = true;
                }
                descriptors[i] = descriptor;
            }
            if (changed) {
                // Rebuild the operation with slightly different parameter descriptors.
                generic = new DefaultOperationMethod(
                        IdentifiedObjects.getProperties(generic),
                        new DefaultParameterDescriptorGroup(
                                IdentifiedObjects.getProperties(generic.getParameters()),
                                1, 1, descriptors));
            }
            return generic;
        }

        /**
         * Sets the value in the given group of parameters.
         *
         * @param  target  where to set the value.
         */
        final void setValue(final ParameterValueGroup target) throws URISyntaxException {
            final ParameterValue<?> param = target.parameter(name());
            if (reference != null) {
                param.setValue(new URI(null, reference, null));
                return;
            }
            if (unit != null) {
                if (!Units.UNITY.equals(unit) || param.getUnit() != null) {
                    param.setValue(value, unit);
                    return;
                }
            }
            param.setValue(value);
        }

        /**
         * Returns the parameter name.
         */
        final String name() {
            return descriptor.getName().getCode();
        }

        /**
         * Returns a string representation for debugging purposes.
         */
        @Override
        public String toString() {
            final var s = new StringBuilder(name()).append(" = ");
            if (reference != null) {
                s.append(reference);
            } else {
                s.append(value);
                if (unit != null) {
                    s.append(' ').append(unit);
                }
            }
            return s.toString();
        }
    }

    /**
     * Creates the parameters descriptors and their values for all parameters of the given operation.
     * The descriptors are created in same time than their values because some descriptor metadata,
     * such as units of measurement and aliases, may differ for different operations.
     *
     * @param  operation  the EPSG code for the operation (conversion or transformation).
     * @param  method     the EPSG code for the method used by the specified operation.
     * @return the parameter values with their descriptors.
     * @throws SQLException if a SQL statement failed.
     */
    private List<Parameter> createParameterValues(final Integer operation, final int method)
            throws FactoryException, SQLException
    {
        final var parameters = new ArrayList<Parameter>();
        try (ResultSet result = executeQueryForCodes(
                "Coordinate_Operation Parameter Value",
                "SELECT"+ /* column 1 */ " CV.PARAMETER_CODE,"
                        + /* column 2 */ " CV.PARAMETER_VALUE,"
                        + /* column 3 */ " CV.PARAM_VALUE_FILE_REF,"
                        + /* column 4 */ " CV.UOM_CODE,"
                        + /* column 5 */ " CU.PARAM_SIGN_REVERSAL"
                        + " FROM "       + "\"Coordinate_Operation Parameter Value\"" + " AS CV"
                        + " INNER JOIN " + "\"Coordinate_Operation Parameter Usage\"" + " AS CU"
                        +   " ON (CV.PARAMETER_CODE"  + " = CU.PARAMETER_CODE)"
                        +  " AND (CV.COORD_OP_METHOD_CODE = CU.COORD_OP_METHOD_CODE)"
                        + " WHERE CV.COORD_OP_METHOD_CODE = ?"
                        +   " AND CV.COORD_OP_CODE = ?"
                        + " ORDER BY CU.SORT_ORDER", method, operation))
        {
            while (result.next()) {
                final Integer descriptor    = getInteger(operation, result, 1);
                final double  value         = getOptionalDouble(result, 2);
                final String  reference     = Double.isNaN(value) ? getString(operation, result, 3) : null;
                final String  uom_code      = getOptionalString(result, 4);
                final Unit<?> unit          = (uom_code != null) ? owner.createUnit(uom_code) : null;
                final Boolean reversibility = getOptionalBoolean(result, 5);
                final var     options       = new LinkedHashMap<String, String>(8);    // The cache needs stable order.
                if (reference != null) {
                    options.put(PARAMETER_TYPE_OPTION, URI_TYPE);
                }
                getParameterUnit(descriptor, options, unit);
                if (reversibility != null) {
                    options.put(SIGN_REVERSAL_OPTION, reversibility.toString());
                }
                final String codeWithOptions = codeWithOptions(descriptor.toString(), options);
                parameters.add(new Parameter(owner.createParameterDescriptor(codeWithOptions), reference, value, unit));
            }
        }
        return parameters;
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
                TableInfo.METHOD,
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
                 * Note: we do not store the formula at this time because the text is
                 * very verbose and rarely used.
                 */
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<String,Object> properties = createProperties(
                        TableInfo.METHOD, epsg, name, null, null, null, remarks, deprecated);

                final Object identifier = properties.remove(IdentifiedObject.IDENTIFIERS_KEY);
                final var params = new DefaultParameterDescriptorGroup(properties, 1, 1, descriptors);
                properties.putAll(IdentifiedObjects.getProperties(params));     // For sharing existing instances.
                properties.put(IdentifiedObject.IDENTIFIERS_KEY, identifier);
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
                TableInfo.OPERATION,
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
                    final OperationMethod generic = owner.createOperationMethod(methodCode.toString());
                    final List<Parameter> values = createParameterValues(epsg, methodCode);
                    operationMethod = Parameter.recreateIfChanged(generic, values);
                    parameterValues = operationMethod.getParameters().createValue();
                    for (final Parameter element : values) try {
                        element.setValue(parameterValues);
                    } catch (RuntimeException | URISyntaxException exception) {
                        String message = error().getString(Errors.Keys.CanNotSetParameterValue_1, element.name());
                        throw new FactoryDataException(message, exception);
                    }
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
                    constructor = (factory, metadata) -> {
                        if (factory instanceof DefaultCoordinateOperationFactory) {
                            return ((DefaultCoordinateOperationFactory) factory)
                                    .createConcatenatedOperation(metadata, sourceCRS, targetCRS, operations);
                        } else {
                            return factory.createConcatenatedOperation(metadata, operations);
                        }
                    };
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
                        TableInfo.OPERATION, epsg, name, null, area, scope, remarks, deprecated);
                properties.put(CoordinateOperations.OPERATION_TYPE_KEY, operationType);
                properties.put(CoordinateOperations.PARAMETERS_KEY, parameterValues);
                properties.put(CoordinateOperation .OPERATION_VERSION_KEY, version);
                properties.put(CoordinateOperation .COORDINATE_OPERATION_ACCURACY_KEY,
                               PositionalAccuracyConstant.transformation(accuracy));
                CoordinateOperation operation = create(constructor, owner.copFactory, properties);
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
            final int[] pair = toPrimaryKeys(null, sourceCRS, targetCRS);
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
                            + " FROM \"Coordinate_Operation\""
                            + " WHERE DEPRECATED=FALSE"  // Do not put spaces around "=" - SQLTranslator searches for this exact match.
                            + " AND SOURCE_CRS_CODE = ?"
                            + " AND TARGET_CRS_CODE = ?"
                            + " ORDER BY COORD_OP_ACCURACY ASC NULLS LAST";
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
             * Alter the ordering using the information supplied in the extents
             * and supersession tables.
             */
            final List<String> codes = Arrays.asList(set.getAuthorityCodes());
            sort(TableInfo.OPERATION, codes, Integer::parseInt).ifPresent((sorted) -> {
                set.setAuthorityCodes(sorted.mapToObj(Integer::toString).toArray(String[]::new));
            });
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
     * <h4>Lifetime</h4>
     * The finder returned by this method depends on this {@code EPSGDataAccess} instance.
     * The finder should not be used after this factory has been {@linkplain #close() closed}
     * or given back to the {@link EPSGFactory}.
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
     * Sorts a collection of codes in preference order.
     * This method orders pairwise the codes according the information provided in the supersession table.
     * If the same object is superseded by more than one object, then the most recent one is inserted first.
     * Except for the codes moved as a result of pairwise ordering, this method tries to preserve the old
     * ordering of the supplied codes (since deprecated operations should already be last).
     *
     * @param  source  the table of the objects for which to check for supersession.
     * @param  codes   the codes to sort. This collection will not be modified by this method.
     * @param  parser  the method to invoke for converting a {@code codes} element to an integer.
     * @return codes of sorted elements, or empty if this method did not change the codes order.
     */
    final synchronized <C extends Comparable<?>> Optional<IntStream> sort(final TableInfo source, final Collection<C> codes, final ToIntFunction<C> parser)
            throws SQLException, FactoryException
    {
        final int size = codes.size();
        if (size > 1) try {
            final var elements = new ObjectPertinence[size];
            final var extents = new ArrayList<String>();
            final String actualTable = translator.toActualTableName(source.table);
            int count = 0;
            for (final C code : codes) {
                final int key;
                try {
                    key = parser.applyAsInt(code);
                } catch (NumberFormatException e) {
                    unexpectedException("sort", e);
                    continue;
                }
                if (translator.isUsageTableFound()) {
                    getUsages(actualTable, key, extents, null);
                } else {
                    /*
                     * For compatibility with EPSG database before version 10.
                     * We may delete this block in a future Apache SIS version.
                     * Note: if this block is deleted, consider deleting also
                     * the finally block and the `TableInfo.areaOfUse` flag.
                     */
                    if (source.areaOfUse) {
                        try (ResultSet result = executeQueryForCodes(
                                "Area",     // Table from EPSG version 9. Does not exist anymore in version 10.
                                "SELECT AREA_OF_USE_CODE FROM \"" + source.table + "\" WHERE " + source.codeColumn + "=?",
                                key))
                        {
                            while (result.next()) {
                                extents.add(getString(code, result, 1));
                            }
                        }
                    }
                }
                final ObjectPertinence element = new ObjectPertinence(key, extents, owner);
                extents.clear();
                try (ResultSet result = executeMetadataQuery(
                        "Supersession",
                        "SELECT SUPERSEDED_BY FROM \"Supersession\""
                                + " WHERE OBJECT_TABLE_NAME=? AND OBJECT_CODE=?"
                                + " ORDER BY SUPERSESSION_YEAR DESC",
                        actualTable, key))
                {
                    while (result.next()) {
                        final int replacement = result.getInt(1);
                        if (!result.wasNull()) {
                            element.replacedBy.add(replacement);
                        }
                    }
                }
                elements[count++] = element;
            }
            if (ObjectPertinence.sort(elements)) {
                return Optional.of(Arrays.stream(elements).mapToInt((p) -> p.code));
            }
        } finally {
            /*
             * Remove from the cache because the table name may change.
             * Note: this is for compatibility with EPSG before version 10.
             * This block may be deleted in a future Apache SIS version.
             */
            PreparedStatement stmt = statements.remove("Area");
            if (stmt != null) {
                stmt.close();
            }
        }
        return Optional.empty();
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
        SQLException error = null;
        if (!authorityCodes.isEmpty()) {
            System.gc();                // For cleaning as much weak references as we can before we check them.
            final Iterator<CloseableReference> it = authorityCodes.values().iterator();
            while (it.hasNext()) {
                final CloseableReference reference = it.next();
                if (!reference.published) {
                    it.remove();
                    try {
                        reference.close();
                    } catch (SQLException e) {
                        if (error == null) error = e;
                        else error.addSuppressed(e);
                    }
                    reference.clear();
                } else if (JDK16.refersTo(reference, null)) {
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
        if (error != null) {
            unexpectedException("canClose", error);
        }
        return can;
    }

    /**
     * Closes the <abbr>JDBC</abbr> connection used by this factory.
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
