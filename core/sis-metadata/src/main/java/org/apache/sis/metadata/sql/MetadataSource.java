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

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.lang.reflect.Method;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.opengis.annotation.UML;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
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
public class MetadataSource {
    /**
     * The column name used for the identifiers. We do not quote this identifier;
     * we will let the database uses its own convention.
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
    private final WeakValueHashMap<CacheKey,Object> cache;

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
        if (ms == null) try {
            final DataSource dataSource = Initializer.getDataSource();
            if (dataSource == null) {
                throw new MetadataStoreException(Initializer.unspecified(null));
            }
            synchronized (MetadataSource.class) {
                ms = instance;
                if (ms == null) {
                    instance = ms = new MetadataSource(MetadataStandard.ISO_19115, dataSource, "metadata");
                }
            }
        } catch (MetadataStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new MetadataStoreException(Errors.format(Errors.Keys.CanNotConnectTo_1, Initializer.JNDI), e);
        }
        return ms;
    }

    /**
     * Creates a new metadata source.
     *
     * @param  standard    the metadata standard to implement.
     * @param  dataSource  the source for getting a connection to the database.
     * @param  schema      the schema were metadata are expected to be found, or {@code null} if none.
     * @throws SQLException if the connection to the given database can not be established.
     */
    public MetadataSource(final MetadataStandard standard, final DataSource dataSource, final String schema)
            throws SQLException
    {
        ArgumentChecks.ensureNonNull("standard",   standard);
        ArgumentChecks.ensureNonNull("dataSource", dataSource);
        this.standard = standard;
        this.schema   = schema;
        this.tables   = new HashMap<>();
        statements    = new ResultPool(dataSource, this);
        cache         = new WeakValueHashMap<>(CacheKey.class);
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
        cache      = new WeakValueHashMap<>(CacheKey.class);
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
     * Temporary place-holder for a method to be developed later.
     */
    public <T> T lookup(final Class<T> type, String identifier) throws MetadataStoreException {
        if (type == Format.class) {
            final DefaultCitation spec = new DefaultCitation();
            /*
             * TODO: move the following hard-coded values in a database
             * after we ported the org.apache.sis.metadata.sql package.
             */
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
        return null;
    }
}
