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
package org.apache.sis.storage.geopackage;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.sql.Connection;
import javax.sql.DataSource;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.IncompatibleResourceException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.sql.DataAccess;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.metadata.sql.internal.shared.ScriptRunner;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Workaround;


/**
 * A data store backed by a Geopackage file. The Geopackage specification mandates the use of SQLite
 * as the database engine, but this {@code GpkgStore} class actually accepts any {@link DataSource}
 * connecting to a database having the same tables as the ones specified by the Geopackage standard.
 * If the input is a {@link Path}, {@link java.io.File}, {@link java.net.URL} or {@link java.net.URI}
 * instead of a {@code DataSource}, then a SQLite data source is automatically created for the file.
 * If the file does not exist, it will be created if the {@linkplain StorageConnector#getOption options}
 * contain a {@linkplain StandardOpenOption#CREATE create} or {@linkplain StandardOpenOption#CREATE_NEW
 * create new} open option.
 *
 * <h2>PRAGMA statements</h2>
 * SQLite can be configured with <a href="https://www.sqlite.org/pragma.html">PRAGMA statements</a>.
 * Some PRAGMA statements to note are:
 *
 * <ul>
 *   <li>{@code SYNCHRONOUS=off;JOURNAL_MODE=off;} for single database creation in a single access context.</li>
 *   <li>{@code SECURE_DELETE=off;} for faster delete operation on slow hard drives.</li>
 * </ul>
 *
 * Note that {@code LOCKING_MODE=exclusive;} may not work with connection polls.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 *
 * @see <a href="https://www.geopackage.org/spec140/index.html">OGC® GeoPackage Encoding Standard version 1.4.0</a>
 */
public class GpkgStore extends SQLStore implements WritableAggregate {
    /**
     * Type of data which for which {@code GpkgStore} will delegate the handling to the {@code SQLStore} parent.
     * A data type is a values of the {@value Content#DATA_TYPE} column of the {@value Content#TABLE_NAME} table.
     */
    private static final String[] DELEGATED_DATA_TYPES = {"features", "attributes"};

    /**
     * Path to the SQLite file, or {@code null} if the store was opened with a {@link DataSource}.
     * This is for information purpose only.
     *
     * @see #getFileSet()
     */
    private final Path path;

    /**
     * The database URL. This is the value provided by {@link DatabaseMetadata#getURL()}
     * and is fetched only if {@link #path} is null. This is for information purpose only.
     *
     * @see #getOpenParameters()
     */
    private String location;

    /**
     * Whether the database needs to be created. This is {@code true} only if the storage is a file and
     * that file does not exist. This flag is set to {@code false} after the database has been created.
     */
    private boolean create;

    /**
     * The PRAGMA statements, or an empty map if none.
     * This is for information purpose only.
     *
     * @see #getOpenParameters()
     */
    private final Map<String,String> pragmas;

    /**
     * Plugins for handling resources other than the ones handled automatically by {@code SQLStore}.
     * The resources handled automatically are {@link org.apache.sis.storage.FeatureSet}.
     */
    private final ServiceLoader<ContentHandler> contentHandlers;

    /**
     * Components having a custom handler.
     */
    private final List<Content> userContents;

    /**
     * Opens a Geopackage file or database. The storage specified in the {@code connector} argument should
     * be a {@link DataSource} for an existing database, or an object convertible to a {@link Path}.
     * If the file does not exist, it will be created if the {@linkplain StorageConnector#getOption options}
     * contain a {@linkplain StandardOpenOption#CREATE create} or {@linkplain StandardOpenOption#CREATE_NEW
     * create new} open option.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (JDBC data source, <i>etc</i>).
     * @throws DataStoreException if Geopackage tables initialization failed
     */
    public GpkgStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        this(provider, new Initializer(connector));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private GpkgStore(final DataStoreProvider provider, final Initializer init) throws DataStoreException {
        super(provider, init.connector);
        path    = init.path;
        create  = init.create;
        pragmas = init.pragmas;
        contentHandlers = ServiceLoader.load(ContentHandler.class);
        userContents = new ArrayList<>();
    }

    /**
     * Returns the path to the main file and its auxiliary files, or an empty value if unknown.
     *
     * @return the Geopackage file and auxiliary files, or an empty value if unknown.
     */
    @Override
    public Optional<FileSet> getFileSet() {
        if (path == null) {
            return Optional.empty();
        }
        final var paths = new ArrayList<Path>(3);
        paths.add(path);
        final String filename = path.getFileName().toString();
        for (String suffix : new String[] {Initializer.WAL_SUFFIX, Initializer.SHM_SUFFIX}) {
            Path aux = path.resolveSibling(filename.concat(suffix));
            if (Files.exists(aux)) paths.add(aux);
        }
        return Optional.of(new FileSet(paths));
    }

    /**
     * Returns the parameters used to open this Geopackage data store.
     * The parameters are described by {@link GpkgStoreProvider#getOpenParameters()}.
     *
     * @return parameters used for opening this data store.
     */
    @Override
    public synchronized Optional<ParameterValueGroup> getOpenParameters() {
        if (provider == null) {
            return Optional.empty();
        }
        final ParameterValueGroup pg = provider.getOpenParameters().createValue();
        URI uri = null;
        if (path != null) {
            uri = path.toUri();
        } else if (location != null) try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            listeners.warning(e);
        }
        if (uri != null) {
            pg.parameter(GpkgStoreProvider.LOCATION).setValue(uri);
        }
        if (!pragmas.isEmpty()) {
            final var b = new StringBuilder();
            for (Map.Entry<String,String> entry : pragmas.entrySet()) {
                if (b.length() != 0) b.append(';');
                b.append(entry.getKey()).append('=').append(entry.getValue());
            }
            pg.parameter(GpkgStoreProvider.PRAGMAS).setValue(b.toString());
        }
        return Optional.of(pg);
    }

    /**
     * Returns whether the given content should be handled by the {@link SQLStore} parent class.
     */
    private static boolean isDelegatedToParentClass(final Content content) {
        return ArraysExt.containsIgnoreCase(DELEGATED_DATA_TYPES, content.dataType());
    }

    /**
     * If this store is creating a new Geopackage file, creates the tables.
     * This method is invoked <em>before</em> {@link #readResourceDefinitions(DataAccess)}.</p>
     *
     * @param  cnx  a connection to the database.
     * @throws DataStoreException if an error occurred during the initialization.
     */
    @Override
    protected void initialize(final Connection cnx) throws DataStoreException {
        assert Thread.holdsLock(this) && userContents.isEmpty();
        if (create) try {
            cnx.setAutoCommit(false);
            try (ScriptRunner runner = new ScriptRunner(cnx, null, 100)) {
                runner.run("PRAGMA main.application_id = " + GpkgStoreProvider.APPLICATION_ID + ';'
                         + "PRAGMA main.user_version = " + GpkgStoreProvider.VERSION + ';');
                runner.run("Core.sql", GpkgStore.class.getResourceAsStream("Core.sql"));
            }
            cnx.commit();
            cnx.setAutoCommit(true);
            create = false;
        } catch (Exception ex) {
            throw cannotExecute("Cannot create the Geopackage tables.", ex);
        }
    }

    /**
     * Returns the subset of the Geopackage content table which is handled by default.
     * This method reads the {@value #TABLE_NAME} table, then filters its elements for
     * retaining only the contents supported by the {@link SQLStore} parent class.
     * Other contents are either delegated to a {@link ContentHandler} if one claims
     * to support that content, or otherwise discarded with a warning.
     *
     * <p>{@code SQLStore} will invoke this method when first needed after construction or after
     * calls to {@link #refresh()}. This method should not be invoked in other circumstances.</p>
     *
     * @param  dao  low-level access (such as <abbr>SQL</abbr> connection) to the database.
     * @return tables or views to include in the store.
     * @throws DataStoreException if an error occurred while fetching the resource definitions.
     */
    @Override
    protected Content[] readResourceDefinitions(final DataAccess dao) throws DataStoreException {
        assert Thread.holdsLock(this) && userContents.isEmpty();
        final List<Content> contents = Content.readFromTable(dao, listeners);
        for (final Iterator<Content> it = contents.iterator(); it.hasNext();) {
            final Content content = it.next();
            if (!isDelegatedToParentClass(content)) {
                it.remove();
                if (content.findHandler(contentHandlers)) {
                    userContents.add(content);
                } else {
                    listeners.warning("Unsupported content: " + content);
                }
            }
        }
        return contents.toArray(Content[]::new);
    }

    /**
     * Returns the resources in this Geopackage store. The collection of resources is
     * constructed when first requested and cached for future invocations of this method.
     * The cache can be cleared by a call to {@link #refresh()}.
     * The components are returned in no particular order.
     *
     * @return children resources that are components of this Geopackage store.
     * @throws DataStoreException if an error occurred while fetching the components.
     */
    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        final Collection<? extends Resource> components = super.components();
        if (userContents.isEmpty()) {
            return components;
        }
        final var copy = new ArrayList<Resource>(components);
        try (DataAccess dao = newDataAccess(false)) {
            for (final Content content : userContents) {
                copy.add(content.resource(dao));
            }
        } catch (Exception e) {
            throw cannotExecute(null, e);
        }
        return copy;
    }

    /**
     * Searches for a resource identified by the given identifier.
     *
     * @param  identifier  identifier of the resource to fetch. Must be non-null.
     * @return resource associated to the given identifier (never {@code null}).
     * @throws IllegalNameException if no resource is found for the given identifier, or if more than one resource is found.
     * @throws DataStoreException if another kind of error occurred while searching resources.
     */
    @Override
    public synchronized Resource findResource(final String identifier) throws DataStoreException {
        /*
         * Find a match in the following priority order:
         *
         *    - Value of the "table_name" column, exact match.
         *    - Value of the "identifier" column, exact match.
         *    - Value of the "table_name" column, ignoring case.
         *    - Value of the "identifier" column, ignoring case.
         */
        if (!userContents.isEmpty()) {
            for (int i=0; i<4; i++) {
                for (Content content : userContents) {
                    final String cid = (i & 1) == 0 ? content.getName().tip().toString() : content.identifier().orElse(null);
                    if ((i & 2) == 0 ? identifier.equals(cid) : identifier.equalsIgnoreCase(cid)) {
                        return content.resource(this);
                    }
                }
            }
        }
        return super.findResource(identifier);
    }

    /**
     * Adds a new resource in this Geopackage. If the resource is a {@link org.apache.sis.storage.FeatureSet},
     * then a new table will be created with the {@linkplain Resource#getIdentifier() resource identifier} as
     * its name.
     *
     * @param  resource  the resource to write in the Geopackage.
     * @return the <i>effectively added</i> resource.
     * @throws DataStoreException if the given resource cannot be stored in this {@code Aggregate}.
     *
     * @todo The implementation of this method is not completed.
     */
    @Override
    public synchronized Resource add(Resource resource) throws DataStoreException {
        ArgumentChecks.ensureNonNull("resource", resource);
        final Resource result = new ContentWriter<Resource>(false) {
            /** Invoked by {@code ContentWriter} for inserting the resource. */
            @Override Content updateResource(final DataAccess dao) throws Exception {
                Content content = null;
                for (ContentHandler handler : contentHandlers) {
                    content = handler.addIfSupported(dao, resource);
                    if (content != null) break;
                }
                return content;
            }

            /** Invoked on success for getting the effective resource. */
            @Override Resource result(final Content content, final DataAccess dao) throws Exception{
                userContents.add(content);
                return content.resource(dao);
            }
        }.execute(this);
        if (result != null) {
            return result;
        }
        /*
         * TODO: delegate to parent class. If the `FeatureSet` has a geometry column, then the `Content.dataType`
         * shall be "features". Otherwise (a `FeatureSet` without geometry), the data type shall be "attributes".
         * SQLStore needs to detect by itself if "geometry_column" is updated automatically (e.g. by PostGIS) or
         * if we need to add a row outselves. Geometry type name shall be in upper case.
         *
         * Note: Geopackage requires that each feature table has exactly one geometry column,
         * while SQLStore accepts any number of geometry columns (including zero).
         */
        throw new IncompatibleResourceException("Unsupported resource type").addAspect("class");
    }

    /**
     * Removes a resource from this Geopackage.
     * The given resource should be one of the instances returned by {@link #components()}.
     *
     * @param  resource  child resource to remove from this {@code Aggregate}.
     * @throws DataStoreException if the given resource could not be removed.
     *
     * @todo The implementation of this method is not completed.
     */
    @Override
    public synchronized void remove(final Resource resource) throws DataStoreException {
        ArgumentChecks.ensureNonNull("resource", resource);
        final Boolean result = new ContentWriter<Boolean>(false) {
            /** The user content to update and to remove if a row is found. */
            private final Iterator<Content> it = userContents.iterator();

            /** Invoked by {@code ContentWriter} for removing the resource. */
            @Override Content updateResource(final DataAccess dao) throws Exception {
                while (it.hasNext()) {
                    final Content content = it.next();
                    final ContentHandler handler = content.handler(resource);
                    if (handler != null && handler.remove(dao, content, resource)) {
                        return content;
                    }
                }
                return null;
            }

            /** Invoked on success for getting the final status. */
            @Override Boolean result(final Content content, final DataAccess dao) throws Exception{
                it.remove();
                return Boolean.TRUE;
            }
        }.execute(this);
        if (result == null) {
            // TODO: delegate to parent class.
            throw new DataStoreException("Unexpected resource");
        }
    }

    /**
     * Clears the cache so that next operations will reload the content table.
     * This method can be invoked when the database content has been modified
     * by a process other than the methods in this class.
     */
    @Override
    public synchronized void refresh() {
        super.refresh();
        userContents.forEach(Content::clear);
        userContents.clear();
    }

    /**
     * Returns the listeners of this data store.
     * This is an accessor for the {@link Content} constructor.
     */
    static StoreListeners listeners(final SQLStore store) {
        return (store instanceof GpkgStore) ? ((GpkgStore) store).listeners : null;
    }

    /**
     * Returns the exception to throw for the given cause.
     * The cause is typically a {@link java.sql.SQLException}, but not necessarily.
     * This method provides a central point where we may specialize the type of the
     * returned exception depending on the type of the cause.
     *
     * @param  message  the message, or {@code null} for the message of the cause.
     * @param  cause    the cause of the error. Cannot be null.
     * @return the data store exception to throw. May be {@code cause} itself.
     */
    static DataStoreException cannotExecute(String message, final Exception cause) {
        final Exception unwrap = Exceptions.unwrap(cause);
        if (unwrap instanceof DataStoreException) {
            return (DataStoreException) unwrap;
        } else {
            if (message == null) {
                message = cause.getMessage();
            }
            return new DataStoreException(message, unwrap);
        }
    }
}
