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
package org.apache.sis.internal.storage.folder;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import org.opengis.util.GenericName;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.internal.storage.StoreUtilities;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.storage.ReadOnlyStorageException;


/**
 * Writable version of the store which rely on given datastore provider to create new types.
 *
 * Note 1: this implementation is experimental.
 * Note 2: it has not been tested since we do not have writable feature sets yet.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class WritableStore extends Store implements WritableAggregate {
    /**
     * {@code false} if this data store is capable to write {@link FeatureSet}.
     * We currently have no easy way to determine that from the provider only,
     * so this flag may be set after the first attempt to add a new resource.
     */
    private boolean isReadOnly;

    /**
     * Creates a new folder store from the given file, path or URI.
     * Contrarily to the {@link Store} parent class, the {@code format} is mandatory for writable stores.
     * This is not verified by this constructor; it should be verified by {@link StoreProvider} instead.
     */
    WritableStore(DataStoreProvider provider, StorageConnector connector, Path path, DataStoreProvider format)
            throws DataStoreException, IOException
    {
        super(provider, connector, path, format);
    }

    /**
     * Create a new file for the given resource.
     * This implementation uses the provider specified at creation time.
     */
    @Override
    public synchronized Resource add(final Resource resource) throws DataStoreException {
        ArgumentChecks.ensureNonNull("resource", resource);
        if (!(resource instanceof FeatureSet)) {
            throw new DataStoreException(message(Resources.Keys.CanNotStoreResourceType_2, new Object[] {
                StoreProvider.NAME, StoreUtilities.getInterface(resource.getClass())
            }));
        }
        /*
         * If we determined in a previous method invocation that the given provider can not write feature set,
         * we are better to fail now instead of polluting the directory with files that we can not use.
         */
        if (isReadOnly) {
            throw new ReadOnlyStorageException(messages().getString(Resources.Keys.StoreIsReadOnly));
        }
        /*
         * Infer a filename from the resource identifier, if one can be found.
         * A suffix is added to the filename if available (some formats may have no suffix at all).
         *
         * TODO: find a more specific metadata property for this informtion.
         */
        final GenericName identifier = resource.getIdentifier().orElse(null);
        if (identifier == null) {
            throw new DataStoreException(message(Resources.Keys.MissingResourceIdentifier_1, StoreUtilities.getLabel(resource)));
        }
        String filename = identifier.toString();
        final String[] suffixes = StoreUtilities.getFileSuffixes(componentProvider.getClass());
        if (suffixes.length != 0) {
            filename += '.' + suffixes[0];
        }
        /*
         * Create new store/resource for write access, provided that no store already exist for the path.
         * We use the CREATE_NEW option in order to intentionally fail if the resource already exists.
         */
        final Path path = location.resolve(filename);
        if (!children.containsKey(path)) {
            final StorageConnector connector = new StorageConnector(path);
            connector.setOption(OptionKey.LOCALE,   locale);
            connector.setOption(OptionKey.TIMEZONE, timezone);
            connector.setOption(OptionKey.ENCODING, encoding);
            connector.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
            });
            final DataStore store = componentProvider.open(connector);
            if (children.putIfAbsent(path, store) == null) {
                /*
                 * Check if we can write data. It should be the case since we specified StandardOpenOption.WRITE.
                 * Note that if 'componentProvider' can create only read-only store, then open(connector) should
                 * have failed with NoSuchFileException before we reach this point.
                 */
                // TODO: handle transactional case.
                if (store instanceof WritableFeatureSet) {
                    StoreUtilities.copy((FeatureSet) resource, (WritableFeatureSet) store);
                    components = null;      // Clear cache. TODO: we should do something more efficient.
                    return store;
                }
                /*
                 * If the data store is not a WritableFeatureSet, current implementation can not use it.
                 * Files created by this failed attempt may remain; instead of trying to delete them with
                 * uncertain consequences, we set a flag for avoiding to pollute further the directory.
                 */
                isReadOnly = true;
                children.remove(path, store);
                final String name = store.getDisplayName();
                store.close();
                throw new DataStoreException(message(Resources.Keys.NotAWritableFeatureSet_1, name));
            }
            store.close();
        }
        throw new DataStoreException(message(Resources.Keys.ResourceAlreadyExists_1, path));
    }

    /**
     * Removes a {@code Resource} from this store. The resource must be a part of this {@code Aggregate}.
     * For a folder store, this means that the resource must be a direct children of the directory managed
     * by this store.
     *
     * This operation is destructive: the {@link Resource} and it's related files will be deleted.
     */
    @Override
    public synchronized void remove(final Resource resource) throws DataStoreException {
        if (resource instanceof DataStore) try {
            if (resource instanceof Store) {
                final Path path = ((Store) resource).location;
                if (Files.isSameFile(path.getParent(), location)) {
                    ((Store) resource).close();
                    deleteRecursively(path, true);
                    children.remove(path);
                    return;
                }
            } else if (resource instanceof ResourceOnFileSystem) {
                final Path[] componentPaths = ((ResourceOnFileSystem) resource).getComponentFiles().clone();
                for (Path root : componentPaths) {
                    root = root.getParent();
                    if (Files.isSameFile(root, location)) {
                        /*
                         * If we enter in this block, we have determined that at least one file is located in the
                         * directory managed by this store - NOT in a subdirectory since they could be managed by
                         * different folder stores. We assume that this root file is the "main" file. Other files
                         * could be in subdirectories, but we need to verify - we do not delete files outside.
                         */
                        for (final Path path : componentPaths) {
                            if (path.startsWith(root)) {
                                Files.delete(path);
                            }
                        }
                        children.values().removeIf((e) -> e == resource);
                        components = null;      // Clear cache. TODO: we should do something more efficient.
                        return;
                    }
                }
            }
        } catch (IOException e) {
            throw new DataStoreException(messages().getString(Resources.Keys.CanNotRemoveResource_2,
                        getDisplayName(), ((DataStore) resource).getDisplayName()), e);
        }
        throw new DataStoreException(messages().getString(Resources.Keys.NoSuchResourceInAggregate_2,
                    getDisplayName(), StoreUtilities.getLabel(resource)));
    }

    /**
     * Deletes all files and sub-directories in the specified directory.
     * This method does nothing if the given {@code root} is a file rather than a directory.
     * The root directory is left in place (after being emptied) if {@code deleteRoot} is {@code false}.
     *
     * @param  root        the directory to delete with all sub-directories.
     * @param  deleteRoot  {@code true} for deleting also {@code root}, or {@code false} for leaving it empty.
     */
    static void deleteRecursively(final Path root, final boolean deleteRoot) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            /**
             * Count of the number of time we entered in a directory.
             * We use this count for detecting when a file is the {@code root} argument.
             */
            private int depth;

            /**
             * Invoked for a directory before entries in the directory are visited.
             */
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                depth++;
                return FileVisitResult.CONTINUE;
            }

            /**
             * Invoked for a file in a directory. Deletes the file provided that it is not the {@code root} argument.
             * The latter can happen if the path given to {@code deleteRecursively(…)} is a file rather than a directory.
             */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (depth > 0) Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            /**
             * Invoked for a directory after entries in the directory have been visited.
             * This method delete the directory unless it is the root and {@code deleteRoot} is {@code false}.
             */
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
                if (ex != null) throw ex;
                if (--depth > 0 || deleteRoot) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
