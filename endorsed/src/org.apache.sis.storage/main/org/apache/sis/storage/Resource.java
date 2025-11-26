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
package org.apache.sis.storage;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.util.Collection;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;


/**
 * Provides access to geospatial data in a {@code DataStore}. The ISO 19115 specification defines resource as
 * an <q>identifiable asset or means that fulfills a requirement</q>. For example, a resource can be a
 * coverage of Sea Surface Temperature, or a coverage of water salinity, or the set of all buoys in a harbor,
 * or an aggregation of all the above. A resource is not necessarily digital; it can be a paper document or an
 * organization, in which case only metadata are provided. If the resource is digital, then {@code Resource}s
 * should be instances of sub-types like {@link Aggregate} or {@link FeatureSet}.
 *
 * <p>{@code DataStore}s are themselves closeable resources.
 * If the data store contains resources for many feature types or coverages, then the data store will be an
 * instance of {@link Aggregate}. The {@linkplain Aggregate#components() components} of an aggregate can be
 * themselves other aggregates, thus forming a tree.</p>
 *
 * <h2>Relationship with ISO 19115</h2>
 * This type is closely related to the {@code DS_Resource} type defined by ISO 19115.
 * The Apache SIS type differs from the ISO type by being more closely related to data extraction,
 * as can be seen from the checked {@link DataStoreException} thrown by most methods.
 * Convenience methods for frequently requested information – for example {@link DataSet#getEnvelope()} – were added.
 * The sub-types performing the actual data extraction – for example {@link FeatureSet} – are specific to Apache SIS.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @see Aggregate#components()
 *
 * @since 0.8
 */
public interface Resource {
    /**
     * Returns the resource persistent identifier.
     * This identifier can be used to uniquely identify a resource in the containing {@link DataStore}.
     * For this identifier to be reliable the following conditions must hold:
     *
     * <ul>
     *   <li>It shall be unique in the {@link DataStore} which contains it, if there is one.</li>
     *   <li>It's value shall not change after closing and reopening the {@link DataStore} on the same data.</li>
     *   <li>It should be consistent with the <code>{@linkplain #getMetadata()}/identificationInfo/citation/identifier</code> value.</li>
     * </ul>
     *
     * If any of above conditions is not met, then this identifier should be absent.
     * This case may happen when a resource is an intermediate result of an ongoing process
     * or is a temporary resource generated on-the-fly, for example a sensor event.
     *
     * @return a persistent identifier unique within the data store, or absent if this resource has no such identifier.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     *
     * @see DataStore#getIdentifier()
     * @see DataStore#findResource(String)
     *
     * @since 1.0
     */
    default Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.empty();
    }

    /**
     * Returns information about this resource.
     * If this resource is an {@link Aggregate}, then the metadata may enumerate characteristics
     * (spatiotemporal extents, feature types, range dimensions, <i>etc.</i>) of all
     * {@linkplain Aggregate#components() components} in the aggregate, or summarize them (for example by omitting
     * {@linkplain org.apache.sis.metadata.iso.extent.DefaultExtent extents} that are fully included in larger extents).
     * If this resource is a {@link DataSet}, then the metadata shall contain only the information that apply to that
     * particular dataset, optionally with a reference to the parent metadata (see below).
     *
     * <p>Some relationships between metadata and resources are:</p>
     * <ul class="verbose">
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getParentMetadata() parentMetadata} /
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getTitle() title}:<br>
     *       a human-readable caption for {@link DataStore#getMetadata()} (if not redundant with this metadata).</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     *       {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getCitation() citation} /
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getTitle() title}:<br>
     *       a human-readable designation for this resource.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     *       {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getAssociatedResources() associatedResource} /
     *       {@link org.apache.sis.metadata.iso.identification.DefaultAssociatedResource#getName() name} /
     *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation#getTitle() title}:<br>
     *       a human-readable designation for parent, children or other related resources.
     *       May be omitted if too expensive to compute.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getMetadataScopes() metadataScope} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadataScope#getResourceScope() resourceScope}:<br>
     *       {@link org.opengis.metadata.maintenance.ScopeCode#DATASET} if the resource is a {@link DataSet}, or
     *       {@link org.opengis.metadata.maintenance.ScopeCode#SERVICE} if the resource is a web service, or
     *       {@link org.opengis.metadata.maintenance.ScopeCode#SERIES} or
     *       {@code ScopeCode.INITIATIVE}
     *       if the resource is an {@link Aggregate} other than a transfer aggregate.</li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getContentInfo() contentInfo} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription#getFeatureTypeInfo() featureType} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultFeatureTypeInfo#getFeatureTypeName() featureTypeName}:<br>
     *       names of feature types included in this resource. Example: “bridge”, “road”, “river”. <i>etc.</i></li>
     *   <li>{@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getContentInfo() contentInfo} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultCoverageDescription#getAttributeGroups() attributeGroup} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultAttributeGroup#getAttributes() attribute} /
     *       {@link org.apache.sis.metadata.iso.content.DefaultRangeDimension#getSequenceIdentifier() sequenceIdentifier}:<br>
     *       sample dimension names (or band numbers in simpler cases) of coverages or rasters included in this resource.</li>
     * </ul>
     *
     * <h4>Metadata edition</h4>
     * This method often returns an {@linkplain org.apache.sis.metadata.ModifiableMetadata.State#FINAL unmodifiable}
     * metadata, for making possible to return efficiently a cached instance.
     * If the caller wants to modify some metadata elements, it may be necessary to perform a
     * {@linkplain org.apache.sis.metadata.iso.DefaultMetadata#deepCopy(Metadata) deep copy} first.
     *
     * @return information about this resource. Should not be {@code null}, but this is not strictly forbidden.
     * @throws DataStoreException if an error occurred while reading the metadata.
     *
     * @see DataStore#getMetadata()
     * @see org.apache.sis.metadata.iso.DefaultMetadata#deepCopy(Metadata)
     */
    Metadata getMetadata() throws DataStoreException;

    /**
     * Gets the paths to the files potentially used by this resource.
     * They are the files that would need to be linked, copied, moved or deleted if this resource was
     * to be transferred efficiently to another location or file system (e.g. a <abbr>ZIP</abbr> file).
     * Such set of files usually exists for {@link DataStore} instances, but not for their
     * {@linkplain DataStore#findResource(String) components} because it is often impractical
     * to copy a single resource component without copying the full data set that contains it.
     *
     * <p>There are some exceptions to above scenario. A {@link DataStore} may have no declared set of files
     * if the store is generated by an in-memory calculation, or if the store uses a connection to a database.
     * Conversely, a component (child resource) may have a set of files if the parent {@code DataStore} stores
     * each component separately.</p>
     *
     * @return files used by this resource.
     * @throws DataStoreException if an error occurred while preparing the set of files.
     *
     * @since 1.5
     */
    default Optional<FileSet> getFileSet() throws DataStoreException {
        return Optional.empty();
    }

    /**
     * Paths to the files potentially used by the enclosing resource.
     * They are typically the files given to the {@link DataStore} constructor,
     * sometime with auxiliary files (metadata, index, <i>etc.</i>).
     * {@code FileSet} allows efficient copies of data set from one location to another.
     *
     * <p>Note that some {@code FileSet} items of a given resource may be shared by the {@code FileSet}
     * of another resource. Therefore, there is no guarantee that modifying or deleting a file will not
     * impact other resources.</p>
     *
     * @author  Johann Sorel (Geomatys)
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.5
     * @since   1.5
     */
    public static class FileSet {
        /**
         * The files to be returned by {@link #getPaths()}.
         */
        private final Collection<Path> paths;

        /**
         * Creates a new instance with the given path.
         * This is a convenience constructor for the common case where the data store uses exactly one file.
         *
         * @param  paths  the single file to be returned by {@link #getPaths()}.
         */
        public FileSet(final Path path) {
            this.paths = List.of(path);
        }

        /**
         * Creates a new instance with the given paths.
         *
         * @param  paths  the files to be returned by {@link #getPaths()}.
         */
        public FileSet(final Path[] paths) {
            this.paths = List.of(paths);
        }

        /**
         * Creates a new instance with the given paths.
         * The content of the given collection is copied.
         *
         * @param  paths  the files to be returned by {@link #getPaths()}.
         */
        public FileSet(final Collection<Path> paths) {
            this.paths = List.copyOf(paths);
        }

        /**
         * Returns the paths to the files potentially used by the enclosing resource.
         * The first file in the returned collection should be the main file given to the {@link DataStore}
         * constructor or opened by other kinds of resource. All other files, if any, may be auxiliary files
         * such as metadata or index. The files are not necessarily in the same directory,
         * and the same files may be used by more than one resource.
         *
         * <p>The returned paths should be regular files instead of directories, unless the enclosing
         * resource is designed for using the content of the whole directory. The collection should not
         * contain non-existent files.</p>
         *
         * @return paths to the files potentially used by the enclosing resource.
         */
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        public Collection<Path> getPaths() {
            return paths;
        }

        /**
         * Returns the directory which contains all files, or {@code null} if none.
         * This is either the directory of the main file, or a parent directory if
         * an auxiliary file is found there. The latter case happens when the same
         * file (e.g. a global {@code metadata.xml}) is shared by many resources.
         */
        private Path getBaseDirectory() {
            Path directory = null;
            for (Path path : getPaths()) {
                path = path.getParent();
                if (directory == null || directory.startsWith(path)) {
                    directory = path;
                }
            }
            return directory;
        }

        /**
         * Copies all resource files to the given directory. The copied files are usually the files returned
         * by {@link #getPaths()}, but details may very depending on the {@link DataStore} implementation.
         * It is recommended to invoke this method only when the {@link DataStore} is closed.
         *
         * <p><b>Limitations:</b></p>
         * <ul>
         *   <li>This method does not overwrite existing files or sub-directories.
         *       If a destination already exists, an exception should be thrown.</li>
         *   <li>This copy operation is not atomic. If an exception is thrown,
         *       some files may be only partially copied or not created at all.</li>
         * </ul>
         *
         * This method can be used for exporting the resource to a <abbr>ZIP</abbr> file if the given
         * destination directory is associated to the file system of the {@code jdk.zipfs} module.
         *
         * <h4>Default implementation</h4>
         * The default implementation performs a {@linkplain Files#copy(Path, Path, CopyOption...) copy operation}
         * for each item returned by {@link #getPath()}. If those source files are in different directories,
         * the default implementation reproduces the directory structure which is below the common parent of
         * all source files.
         *
         * @param  destDir  the directory where to copy the resource files.
         * @return the copied main file. May be in a sub-directory of {@code destDir}.
         * @throws FileAlreadyExistsException if a destination file or directory already exists.
         * @throws IOException if another error occurred while copying the files.
         *
         * @see Files#copy(Path, Path, CopyOption...)
         */
        public Path copy(final Path destDir) throws IOException {
            final var subdirs = new HashSet<Path>();
            final Path base = getBaseDirectory();
            Path main = null;
            for (Path source : getPaths()) {
                Path target = source;
                if (base != null) {
                    target = base.relativize(target);
                    mkdirs(destDir, target, subdirs);
                }
                target = Files.copy(source, destDir.resolve(target));
                if (main == null) main = target;
            }
            return main;
        }

        /**
         * Creates the parent directories if they are needed.
         * We do not use {@code Files.mkdirs(…)} because we want this method to fail if a directory exists.
         */
        private static void mkdirs(final Path destDir, Path file, final Set<Path> done) throws IOException {
            file = file.getParent();
            if (file != null && done.add(file)) {
                mkdirs(destDir, file, done);   // Create parents first.
                Files.createDirectory(destDir.resolve(file));
            }
        }

        /**
         * Deletes the files used by the enclosing resource.
         * The {@link DataStore} that contains the resource should be closed before to invoke this method.
         * This is not an atomic operation. If an exception is thrown, some files may still remain.
         *
         * <h4>Default implementation</h4>
         * The default implementation {@linkplain Files#delete(Path) deletes} the files returned by
         * {@link #getPaths()} that are in the same directory or a sub-directory of the main file.
         * Files in parent directory are not deleted, because they are often files shared by many
         * resources (e.g. a global {@code metadata.xml} file).
         *
         * @throws IOException if another error occurred while deleting a file.
         *
         * @see Files#delete(Path)
         */
        public void delete() throws IOException {
            Path base = null;
            boolean first = true;
            for (Path path : getPaths()) {
                if (first) {
                    first = false;
                    base = path.getParent();    // May still null.
                } else if (base != null && !path.startsWith(base)) {
                    continue;
                }
                Files.delete(path);
            }
        }
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs in this resource or in children.
     * The resource will call the {@link StoreListener#eventOccured(StoreEvent)} method when new events matching
     * the {@code eventType} occur. An event may be a change in resource content or structure, or a warning that
     * occurred during a read or write operation.
     *
     * <p>Registering a listener for a given {@code eventType} also register the listener for all event sub-types.
     * The same listener can be registered many times, but its {@link StoreListener#eventOccured(StoreEvent)}
     * method will be invoked only once per event. This filtering applies even if the listener is registered
     * on different resources in the same tree, for example a parent and its children.</p>
     *
     * <p>If this resource may produce events of the given type, then the given listener is kept by strong reference;
     * it will not be garbage collected unless {@linkplain #removeListener(Class, StoreListener) explicitly removed}
     * or unless this {@code Resource} is itself garbage collected. However if the given type of events can never
     * happen with this resource, then this method is not required to keep a reference to the given listener.</p>
     *
     * <h4>Warning events</h4>
     * If {@code eventType} is assignable from <code>{@linkplain org.apache.sis.storage.event.WarningEvent}.class</code>,
     * then registering that listener turns off logging of warning messages for this resource.
     * This side-effect is applied on the assumption that the registered listener will handle
     * warnings in its own way, for example by showing warnings in a widget.
     *
     * <h4>Default implementation</h4>
     * The default implementation does nothing.
     * This is okay for resources that do not emit any event.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to notify about events.
     * @param  eventType  type of {@link StoreEvent}s to listen (cannot be {@code null}).
     */
    default <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
    }

    /**
     * Unregisters a listener previously added to this resource for the given type of events.
     * The {@code eventType} must be the exact same class as the one given to the {@code addListener(…)} method;
     * this method does not remove listeners registered for subclasses and does not remove listeners registered in
     * parent resources.
     *
     * <p>If the same listener has been registered many times for the same even type, then this method removes only
     * the most recent registration. In other words if {@code addListener(type, ls)} has been invoked twice, then
     * {@code removeListener(type, ls)} needs to be invoked twice in order to remove all instances of that listener.
     * If the given listener is not found, then this method does nothing (no exception is thrown).</p>
     *
     * <h4>Warning events</h4>
     * If {@code eventType} is <code>{@linkplain org.apache.sis.storage.event.WarningEvent}.class</code>
     * and if, after this method invocation, there are no remaining listeners for warning events,
     * then this {@code Resource} will send future warnings to the loggers.
     *
     * <h4>Default implementation</h4>
     * The default implementation does nothing.
     * This is okay for resources that do not emit any event.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to stop notifying about events.
     * @param  eventType  type of {@link StoreEvent}s which were listened (cannot be {@code null}).
     */
    default <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
    }
}
