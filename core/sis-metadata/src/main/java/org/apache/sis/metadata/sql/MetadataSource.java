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
package org.apache.sis.metadata.sql;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import org.opengis.annotation.UML;
import org.opengis.util.CodeList;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Types;


/**
 * A connection to a metadata database in read-only mode. The database must have a schema of the given name
 * ({@code "metadata"} in the example below). Existing entries can be obtained as in the example below:
 *
 * {@preformat java
 *   DataSource     source     = ... // This is database-specific.
 *   MetadataSource source     = new MetadataSource(MetadataStandard.ISO_19115, source, "metadata");
 *   Telephone      telephone  = source.lookup(Telephone.class, id);
 * }
 *
 * where {@code id} is the primary key value for the desired record in the {@code CI_Telephone} table.
 *
 * <div class="section">Concurrency</div>
 * {@code MetadataSource} is thread-safe but is not concurrent. If concurrency is desired,
 * multiple instances of {@code MetadataSource} can be created for the same {@link DataSource}.
 * The {@link #MetadataSource(MetadataSource)} convenience constructor can be used for this purpose.
 *
 * @author  Touraïvane (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class MetadataSource implements AutoCloseable {
    /**
     * The column name used for the identifiers. We do not quote this identifier;
     * we will let the database uses its own lower-case / upper-case convention.
     */
    static final String ID_COLUMN = "ID";

    /**
     * The metadata standard to be used for constructing the database schema.
     */
    protected final MetadataStandard standard;

    /**
     * The catalog, set to {@code null} for now. This is defined as a constant in order to make easier
     * to spot the places where catalog would be used, if we want to use it in a future version.
     */
    static final String CATALOG = null;

    /**
     * The schema where metadata are stored, or {@code null} if none.
     */
    final String schema;

    /**
     * The tables which have been queried or created up to date.
     * Keys are table names and values are the columns defined for that table.
     */
    private final Map<String, Set<String>> tables;

    /**
     * The prepared statements created in previous calls to {@link #getValue(Class, Method, String)} method.
     * Those statements are encapsulated into {@link MetadataResult} objects.
     * This object is also the lock on which every SQL query must be guarded.
     * We use this object because SQL queries will typically involve usage of this map.
     */
    private final ResultPool statements;

    /**
     * The previously created objects.
     * Used in order to share existing instances for the same interface and primary key.
     */
    private final WeakValueHashMap<CacheKey,Object> pool;

    /**
     * The last converter used.
     */
    private transient volatile ObjectConverter<?,?> lastConverter;

    /**
     * The class loader to use for proxy creation.
     */
    private final ClassLoader loader;

    /**
     * The default instance, created when first needed and cleared when the classpath change.
     */
    private static volatile MetadataSource instance;
    static {
        SystemListener.add(new SystemListener(Modules.METADATA) {
            @Override protected void classpathChanged() {
                instance = null;
            }
        });
    }

    /**
     * Returns the metadata source connected to the {@code "jdbc/SpatialMetadata"} database.
     * In a default Apache SIS installation, this metadata source contains pre-defined records
     * for some commonly used {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation
     * citations} and {@linkplain org.apache.sis.metadata.iso.distribution.DefaultFormat formats}
     * among others.
     *
     * @return source of pre-defined metadata records from the {@code "jdbc/SpatialMetadata"} database.
     * @throws MetadataStoreException if this method can not connect to the database.
     */
    public static MetadataSource getDefault() throws MetadataStoreException {
        MetadataSource ms = instance;
        if (ms == null) {
            final DataSource dataSource;
            try {
                dataSource = Initializer.getDataSource();
            } catch (Exception e) {
                throw new MetadataStoreException(Errors.format(Errors.Keys.CanNotConnectTo_1, Initializer.JNDI), e);
            }
            if (dataSource == null) {
                throw new MetadataStoreException(Initializer.unspecified(null));
            }
            synchronized (MetadataSource.class) {
                ms = instance;
                if (ms == null) {
                    instance = ms = new MetadataSource(MetadataStandard.ISO_19115, dataSource, "metadata");
                }
            }
        }
        return ms;
    }

    /**
     * Creates a new metadata source.
     *
     * @param  standard    the metadata standard to implement.
     * @param  dataSource  the source for getting a connection to the database.
     * @param  schema      the schema were metadata are expected to be found, or {@code null} if none.
     */
    public MetadataSource(final MetadataStandard standard, final DataSource dataSource, final String schema) {
        ArgumentChecks.ensureNonNull("standard",   standard);
        ArgumentChecks.ensureNonNull("dataSource", dataSource);
        this.standard = standard;
        this.schema   = schema;
        this.tables   = new HashMap<>();
        statements    = new ResultPool(dataSource, this);
        pool          = new WeakValueHashMap<>(CacheKey.class);
        loader        = getClass().getClassLoader();
    }

    /**
     * Creates a new metadata source with the same configuration than the given source.
     * The two sources will share the same data source but will use their own {@linkplain Connection connection}.
     * This constructor is useful when concurrency is desired.
     *
     * @param  source  the source from which to copy the configuration.
     */
    public MetadataSource(final MetadataSource source) {
        ArgumentChecks.ensureNonNull("source", source);
        standard   = source.standard;
        schema     = source.schema;
        loader     = source.loader;
        tables     = new HashMap<>();
        statements = new ResultPool(source.statements);
        pool       = new WeakValueHashMap<>(CacheKey.class);
    }

    /**
     * If the given value is a collection, returns the first element in that collection
     * or {@code null} if empty.
     *
     * @param  value  the value to inspect (can be {@code null}).
     * @return the given value, or its first element if the value is a collection,
     *         or {@code null} if the given value is null or an empty collection.
     */
    private static Object extractFromCollection(Object value) {
        while (value instanceof Iterable<?>) {
            final Iterator<?> it = ((Iterable<?>) value).iterator();
            if (!it.hasNext()) {
                return null;
            }
            if (value == (value = it.next())) break;
        }
        return value;
    }

    /**
     * Returns the table name for the specified class.
     * This is usually the ISO 19115 name.
     */
    private static String getTableName(final Class<?> type) {
        final UML annotation = type.getAnnotation(UML.class);
        if (annotation == null) {
            return type.getSimpleName();
        }
        final String name = annotation.identifier();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * Returns the column name for the specified method.
     */
    private static String getColumnName(final Method method) {
        final UML annotation = method.getAnnotation(UML.class);
        if (annotation == null) {
            return method.getName();
        }
        final String name = annotation.identifier();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * Returns a view of the given metadata as a map. This method returns always a map using UML identifier
     * and containing all entries including the null ones because the {@code MetadataSource} implementation
     * assumes so.
     *
     * @param  metadata  the metadata object to view as a map.
     * @return a map view over the metadata object.
     * @throws ClassCastException if the metadata object does not implement a metadata interface
     *         of the expected package.
     */
    final Map<String,Object> asMap(final Object metadata) throws ClassCastException {
        return standard.asValueMap(metadata, KeyNamePolicy.UML_IDENTIFIER, ValueExistencePolicy.ALL);
    }

    /**
     * If the given metadata is a proxy generated by this {@code MetadataSource}, returns the
     * identifier of that proxy. Such metadata do not need to be inserted again in the database.
     *
     * @param  metadata  the metadata to test.
     * @return the identifier (primary key), or {@code null} if the given metadata is not a proxy.
     */
    final String proxy(final Object metadata) {
        return (metadata instanceof MetadataProxy) ? ((MetadataProxy) metadata).identifier(this) : null;
    }

    /**
     * Searches for the given metadata in the database. If such metadata is found, then its
     * identifier (primary key) is returned. Otherwise this method returns {@code null}.
     *
     * @param  metadata  the metadata to search for.
     * @return the identifier of the given metadata, or {@code null} if none.
     * @throws MetadataStoreException if the metadata object does not implement a metadata interface
     *         of the expected package, or if an error occurred while searching in the database.
     */
    public String search(final Object metadata) throws MetadataStoreException {
        ArgumentChecks.ensureNonNull("metadata", metadata);
        String identifier = proxy(metadata);
        if (identifier == null) {
            /*
             * Code lists do not need to be stored in the database. Some code list tables may
             * be present in the database in order to ensure foreigner key constraints, but
             * those tables are not used in any way by the org.apache.sis.metadata.sql package.
             */
            if (metadata instanceof CodeList<?>) {
                identifier = ((CodeList<?>) metadata).name();
            } else {
                final String table;
                final Map<String,Object> asMap;
                try {
                    table = getTableName(standard.getInterface(metadata.getClass()));
                    asMap = asMap(metadata);
                } catch (ClassCastException e) {
                    throw new MetadataStoreException(Errors.format(
                            Errors.Keys.IllegalArgumentClass_2, "metadata", metadata.getClass()));
                }
                synchronized (statements) {
                    try (Statement stmt = statements.connection().createStatement()) {
                        identifier = search(table, null, asMap, stmt, statements.helper());
                    } catch (SQLException e) {
                        throw new MetadataStoreException(e);
                    }
                }
            }
        }
        return identifier;
    }

    /**
     * Searches for the given metadata in the database. If such metadata is found, then its
     * identifier (primary key) is returned. Otherwise this method returns {@code null}.
     *
     * @param  table     the table where to search.
     * @param  columns   the table columns as given by {@link #getExistingColumns(String)}, or {@code null}.
     * @param  metadata  a map view of the metadata to search for.
     * @param  stmt      the statement to use for executing the query.
     * @param  helper    an helper class for creating the SQL query.
     * @return the identifier of the given metadata, or {@code null} if none.
     * @throws SQLException if an error occurred while searching in the database.
     */
    final String search(final String table, Set<String> columns, final Map<String,Object> metadata,
            final Statement stmt, final SQLBuilder helper) throws SQLException
    {
        assert Thread.holdsLock(statements);
        helper.clear();
        for (final Map.Entry<String,Object> entry : metadata.entrySet()) {
            /*
             * Gets the value and the column where this value is stored. If the value is non-null,
             * then the column must exist otherwise the metadata will be considered as not found.
             */
            Object value = extractFromCollection(entry.getValue());
            final String column = entry.getKey();
            if (columns == null) {
                columns = getExistingColumns(table);
            }
            if (!columns.contains(column)) {
                if (value != null) {
                    return null;            // The column was mandatory for the searched metadata.
                } else {
                    continue;               // Do not include a non-existent column in the SQL query.
                }
            }
            /*
             * Tests if the value is another metadata, in which case we will invoke this method recursively.
             * Note that if a metadata dependency is not found, we can stop the whole process immediately.
             */
            if (value != null) {
                if (value instanceof CodeList<?>) {
                    value = ((CodeList<?>) value).name();
                } else {
                    String dependency = proxy(value);
                    if (dependency != null) {
                        value = dependency;
                    } else {
                        final Class<?> type = value.getClass();
                        if (standard.isMetadata(type)) {
                            dependency = search(getTableName(standard.getInterface(type)),
                                    null, asMap(value), stmt, new SQLBuilder(helper));
                            if (dependency == null) {
                                return null;                    // Dependency not found.
                            }
                            value = dependency;
                        }
                    }
                }
            }
            /*
             * Builds the SQL statement with the resolved value.
             */
            if (helper.isEmpty()) {
                helper.append("SELECT ").append(ID_COLUMN).append(" FROM ")
                        .appendIdentifier(schema, table).append(" WHERE ");
            } else {
                helper.append(" AND ");
            }
            helper.appendIdentifier(column).appendCondition(value);
        }
        /*
         * The SQL statement is ready, with metadata dependency (if any) resolved. We can now execute it.
         * If more than one record is found, the identifier of the first one will be selected add a warning
         * will be logged.
         */
        String identifier = null;
        try (ResultSet rs = stmt.executeQuery(helper.toString())) {
            while (rs.next()) {
                final String candidate = rs.getString(1);
                if (candidate != null) {
                    if (identifier == null) {
                        identifier = candidate;
                    } else if (!identifier.equals(candidate)) {
                        warning(MetadataSource.class, "search", resources().getLogRecord(
                                Level.WARNING, Errors.Keys.DuplicatedElement_1, candidate));
                        break;
                    }
                }
            }
        }
        return identifier;
    }

    /**
     * Returns the set of all columns in a table, or an empty set if none (never {@code null}).
     * Because each table should have at least the {@value #ID_COLUMN} column, an empty set of
     * columns will be understood as meaning that the table does not exist.
     *
     * <p>This method returns a direct reference to the cached set. The returned set shall be
     * modified in-place if new columns are added in the database table.</p>
     *
     * @param  table  the name of the table for which to get the columns.
     * @return the set of columns, or an empty set if the table has not yet been created.
     * @throws SQLException if an error occurred while querying the database.
     */
    final Set<String> getExistingColumns(final String table) throws SQLException {
        assert Thread.holdsLock(statements);
        Set<String> columns = tables.get(table);
        if (columns == null) {
            columns = new HashSet<>();
            /*
             * Note: a null schema in the DatabaseMetadata.getColumns(…) call means "do not take schema in account";
             * it does not mean "no schema" (the later is specified by an empty string). This match better what we
             * want because if we do not specify a schema in a SELECT statement, then the actual schema used depends
             * on the search path specified in the database environment variables.
             */
            try (ResultSet rs = statements.connection().getMetaData().getColumns(CATALOG, schema, table, null)) {
                while (rs.next()) {
                    if (!columns.add(rs.getString("COLUMN_NAME"))) {
                        // Paranoiac check, but should never happen.
                        throw new SQLNonTransientException(table);
                    }
                }
            }
            tables.put(table, columns);
        }
        return columns;
    }

    /**
     * Returns an implementation of the specified metadata interface filled with the data referenced
     * by the specified identifier. Alternatively, this method can also returns a {@link CodeList} element.
     *
     * @param  <T>         the parameterized type of the {@code type} argument.
     * @param  type        the interface to implement (e.g. {@link org.opengis.metadata.citation.Citation}),
     *                     or the {@link CodeList} value.
     * @param  identifier  the identifier of the record for the metadata entity to be created.
     *                     This is usually the primary key of the record to search for.
     * @return an implementation of the required interface, or the code list element.
     * @throws MetadataStoreException if a SQL query failed.
     */
    public <T> T lookup(final Class<T> type, String identifier) throws MetadataStoreException {
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonNull("identifier", identifier);
        /*
         * TODO: temporary hack until we ported the following information to the database.
         */
        if (type == Format.class) {
            final DefaultCitation spec = new DefaultCitation();
            String title = null;
            switch (identifier) {
                case "GeoTIFF": title = "GeoTIFF Coverage Encoding Profile"; break;
                case "NetCDF":  title = "NetCDF Classic and 64-bit Offset Format"; break;
                case "PNG":     title = "PNG (Portable Network Graphics) Specification"; break;
                case "CSV":     title = "Common Format and MIME Type for Comma-Separated Values (CSV) Files"; break;
                case "CSV/MF":  title = "OGC Moving Features Encoding Extension: Simple Comma-Separated Values (CSV)";
                                identifier  = "CSV";      // "CSV/MF" is not yet a documented format.
                                break;
            }
            spec.setTitle(Types.toInternationalString(title));
            spec.setAlternateTitles(Collections.singleton(Types.toInternationalString(identifier)));
            final DefaultFormat format = new DefaultFormat();
            format.setFormatSpecificationCitation(spec);
            return (T) format;
        }
        /*
         * IMPLEMENTATION NOTE: This method must not invoke any method which may access 'statements'.
         * It is not allowed to acquire the lock on 'statements' neither.
         */
        Object value;
        if (CodeList.class.isAssignableFrom(type)) {
            value = getCodeList(type, identifier);
        } else {
            final CacheKey key = new CacheKey(type, identifier);
            synchronized (pool) {
                value = pool.get(key);
                if (value == null) {
                    value = Proxy.newProxyInstance(loader,
                            new Class<?>[] {type, MetadataProxy.class}, new Dispatcher(identifier, this));
                    pool.put(key, value);
                }
            }
        }
        return type.cast(value);
    }

    /**
     * Returns an attribute from a table.
     *
     * @param  type      the interface class. This is mapped to the table name in the database.
     * @param  method    the method invoked. This is mapped to the column name in the database.
     * @param  toSearch  contains the identifier and preferred index of the record to search.
     * @return the value of the requested attribute.
     * @throws SQLException if the SQL query failed.
     * @throws MetadataStoreException if a value can not be converted to the expected type.
     */
    final Object getValue(final Class<?> type, final Method method, final Dispatcher toSearch)
            throws SQLException, MetadataStoreException
    {
        final Class<?> returnType     = method.getReturnType();
        final boolean  wantCollection = Collection.class.isAssignableFrom(returnType);
        final Class<?> elementType    = wantCollection ? Classes.boundOfParameterizedProperty(method) : returnType;
        final boolean  isMetadata     = standard.isMetadata(elementType);
        final String   tableName      = getTableName(type);
        final String   columnName     = getColumnName(method);
        final boolean  isArray;
        Object value;
        synchronized (statements) {
            final Connection connection = statements.connection();
            final boolean columnExists;
            try (ResultSet rs = connection.getMetaData().getColumns(CATALOG, schema, tableName, columnName)) {
                columnExists = rs.next();
            }
            if (!columnExists) {
                value   = null;
                isArray = false;
            } else {
                /*
                 * Prepares the statement and executes the SQL query in this synchronized block.
                 * Note that the usage of 'result' must stay inside this synchronized block
                 * because we can not assume that JDBC connections are thread-safe.
                 */
                MetadataResult result = statements.take(type, toSearch.preferredIndex);
                if (result == null) {
                    final SQLBuilder helper = statements.helper();
                    final String query = helper.clear().append("SELECT * FROM ")
                            .appendIdentifier(schema, tableName).append(" WHERE ")
                            .append(ID_COLUMN).append("=?").toString();
                    result = new MetadataResult(type, connection.prepareStatement(query), statements.listeners);
                }
                value = result.getValue(toSearch.identifier, columnName);
                isArray = (value instanceof java.sql.Array);
                if (isArray) {
                    final java.sql.Array array = (java.sql.Array) value;
                    value = array.getArray();
                    array.free();
                }
                toSearch.preferredIndex = statements.recycle(result, toSearch.preferredIndex);
            }
        }
        /*
         * If the value is an array and the return type is anything except an array of primitive type, ensure
         * that the value is converted in an array of type Object[]. In this process, resolve foreigner keys.
         */
        if (isArray && (wantCollection || !elementType.isPrimitive())) {
            final Object[] values = new Object[Array.getLength(value)];
            for (int i=0; i<values.length; i++) {
                Object element = Array.get(value, i);
                if (element != null) {
                    if (isMetadata) {
                        element = lookup(elementType, element.toString());
                    } else try {
                        element = convert(elementType, element);
                    } catch (UnconvertibleObjectException e) {
                        throw new MetadataStoreException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                                columnName + '[' + i + ']', elementType, element.getClass()), e);
                    }
                }
                values[i] = element;
            }
            value = values;             // Now a Java array.
            if (wantCollection) {
                value = specialize(UnmodifiableArrayList.wrap(values), returnType, elementType);
            }
        }
        /*
         * Now converts the value to its final type, including conversion of null
         * value to empty collections if the return value should be a collection.
         */
        if (value == null) {
            if (wantCollection) {
                if (Set.class.isAssignableFrom(returnType)) {
                    if (SortedSet.class.isAssignableFrom(returnType)) {
                        return Collections.emptySortedSet();
                    } else {
                        return Collections.EMPTY_SET;
                    }
                } else {
                    return Collections.EMPTY_LIST;
                }
            }
        } else {
            if (isMetadata) {
                value = lookup(elementType, value.toString());
            } else try {
                value = convert(elementType, value);
            } catch (UnconvertibleObjectException e) {
                throw new MetadataStoreException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                        columnName, elementType, value.getClass()), e);
            }
            if (wantCollection) {
                if (Set.class.isAssignableFrom(returnType)) {
                    return Collections.singleton(value);
                } else {
                    return Collections.singletonList(value);
                }
            }
        }
        return value;
    }

    /**
     * Converts the specified non-metadata value into an object of the expected type.
     * The expected value is an instance of a class outside the metadata package, for
     * example {@link String}, {@link InternationalString}, {@link URI}, <i>etc.</i>
     *
     * @throws UnconvertibleObjectException if the value can not be converter.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private Object convert(final Class<?> targetType, Object value) throws UnconvertibleObjectException {
        final Class<?> sourceType = value.getClass();
        if (!targetType.isAssignableFrom(sourceType)) {
            ObjectConverter converter = lastConverter;
            if (converter == null || !converter.getSourceClass().isAssignableFrom(sourceType) ||
                                     !targetType.isAssignableFrom(converter.getTargetClass()))
            {
                lastConverter = converter = ObjectConverters.find(sourceType, targetType);
            }
            value = converter.apply(value);
        }
        return value;
    }

    /**
     * Returns the code of the given type and name. This method is defined for avoiding the warning message
     * when the actual class is unknown (it must have been checked dynamically by the caller however).
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static CodeList<?> getCodeList(final Class<?> type, final String name) {
        return Types.forCodeName((Class) type, name, true);
    }

    /**
     * Copies the given collection into the best {@code Set} implementation if possible,
     * or returns the given collection unchanged otherwise.
     *
     * @param  collection   the collection to copy.
     * @param  returnType   the desired collection type.
     * @param  elementType  the type of elements in the collection.
     * @return the collection of a specialized type if relevant.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static <E> Collection<?> specialize(Collection<?> collection, final Class<?> returnType, final Class<E> elementType) {
        if (!returnType.isAssignableFrom(Set.class)) {
            return collection;
        }
        final Set<E> enumeration;
        if (CodeList.class.isAssignableFrom(elementType)) {
            enumeration = new CodeListSet<>((Class) elementType);
        } else if (Enum.class.isAssignableFrom(elementType)) {
            enumeration = EnumSet.noneOf((Class) elementType);
        } else {
            /*
             * If 'returnType' is Collection.class, do not copy into a Set since a List
             * is probably good enough. Copy only if a Set is explicitely requested.
             */
            if (Set.class.isAssignableFrom(returnType)) {
                if (SortedSet.class.isAssignableFrom(returnType)) {
                    if (collection.isEmpty()) {
                        collection = Collections.emptySortedSet();
                    } else {
                        collection = Collections.unmodifiableSortedSet(new TreeSet<>(collection));
                    }
                } else {
                    switch (collection.size()) {
                        case 0:  collection = Collections.emptySet(); break;
                        case 1:  collection = Collections.singleton(CollectionsExt.first(collection)); break;
                        default: collection = Collections.unmodifiableSet(new LinkedHashSet<>(collection)); break;
                    }
                }
            }
            return collection;
        }
        for (final Object e : collection) {
            enumeration.add(elementType.cast(e));
        }
        return Collections.unmodifiableSet(enumeration);
    }

    /**
     * Returns the resources for warnings and error messages.
     */
    private static Errors resources() {
        return Errors.getResources((Locale) null);
    }

    /**
     * Reports a warning.
     *
     * @param source  the class to report as the warning emitter.
     * @param method  the method to report as the warning emitter.
     * @param record  the warning to report.
     */
    private void warning(final Class<?> source, final String method, final LogRecord record) {
        record.setSourceClassName(source.getCanonicalName());
        record.setSourceMethodName(method);
        record.setLoggerName(Loggers.SQL);
        statements.listeners.warning(record);
    }

    /**
     * Closes the database connection used by this object.
     *
     * @throws MetadataStoreException if an error occurred while closing the connection.
     */
    @Override
    public void close() throws MetadataStoreException {
        try {
            statements.close();
        } catch (SQLException e) {
            throw new MetadataStoreException(e);
        }
    }
}
