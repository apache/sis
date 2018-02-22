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
import java.util.Iterator;
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
import org.apache.sis.internal.storage.FileSystemProvider;
import org.apache.sis.internal.storage.FileSystemResource;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.util.ArgumentChecks;


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
     * Creates a new folder store from the given file, path or URI.
     * Contrarily to the {@link Store} parent class, the {@code format} is mandatory for writable stores.
     */
    WritableStore(final DataStoreProvider provider, final StorageConnector connector, final String format)
            throws DataStoreException, IOException
    {
        super(provider, connector, format);
    }

    /**
     * Create a new file for the given resource.
     * This implementation uses the provider specified by the format name given at creation time.
     */
    @Override
    public synchronized Resource add(final Resource resource) throws DataStoreException {
        ArgumentChecks.ensureNonNull("resource", resource);
        if (!(resource instanceof FeatureSet)) {
            throw new DataStoreException(message(Resources.Keys.CanNotStoreResourceType_2, new Object[] {
                FolderStoreProvider.NAME, StoreUtilities.getInterface(resource.getClass())
            }));
        }
        /*
         * Infer a filename from the resource identifier, if one can be found.
         * A suffix is added to the filename if available (some formats may have no suffix at all).
         */
        String filename = StoreUtilities.getIdentifier(resource.getMetadata());
        if (filename == null) {
            throw new DataStoreException(message(Resources.Keys.MissingResourceIdentifier_1, StoreUtilities.getLabel(resource)));
        }
        if (componentProvider instanceof FileSystemProvider) {
            final Iterator<String> suffixes = ((FileSystemProvider) componentProvider).getSuffix().iterator();
            if (suffixes.hasNext()) {
                filename += '.' + suffixes.next();
            }
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
                 * Delete the file that the store may have created.
                 *
                 * TODO: we should set a flag for blocking next attempts to add a resources, since they are likely
                 *       to fail as well. Maybe we should not delete any files since we are not sure to delete the
                 *       right ones. For example store.getComponentPaths() may return a path outside the directory
                 *       managed by this folder store.
                 */
                final DataStoreException ex = new DataStoreException(Resources.format(
                        Resources.Keys.NotAWritableFeatureSet_1, store.getDisplayName()));
                store.close();
                try {
                    if (store instanceof Store) {
                        deleteRecursively(((Store) store).location, true);
                    } else if (store instanceof FileSystemResource) {
                        for (Path c : ((FileSystemResource) store).getResourcePaths()) {
                            Files.delete(c);
                        }
                    }
                } catch (IOException e) {
                    ex.addSuppressed(e);
                }
                children.remove(path, store);
                throw ex;
            }
            store.close();
        }
        throw new DataStoreException(Resources.format(Resources.Keys.ResourceAlreadyExists_1, path));
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
            } else if (resource instanceof FileSystemResource) {
                final Path[] componentPaths = ((FileSystemResource) resource).getResourcePaths().clone();
                for (final Path root : componentPaths) {
                    if (Files.isSameFile(root.getParent(), location)) {
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
            throw new DataStoreException(Resources.format(Resources.Keys.CanNotRemoveResource_2,
                        getDisplayName(), ((DataStore) resource).getDisplayName()), e);
        }
        throw new DataStoreException(Resources.format(Resources.Keys.NoSuchResourceInAggregate_2,
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
             * The later can happen if the path given to {@code deleteRecursively(…)} is a file rather than a directory.
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
