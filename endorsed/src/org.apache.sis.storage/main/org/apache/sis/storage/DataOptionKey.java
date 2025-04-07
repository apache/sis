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

import java.io.ObjectStreamException;
import java.nio.file.Path;
import static java.util.logging.Logger.getLogger;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Modules;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.feature.FoliationRepresentation;
import org.apache.sis.storage.modifier.CoverageModifier;


/**
 * Keys in a map of options for configuring the way data are read or written to a storage.
 * {@code DataOptionKey} extends {@link OptionKey} with options about features, coverages
 * or other kinds of structure that are specific to some data formats.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <T>  the type of option values.
 *
 * @since 1.0
 */
public final class DataOptionKey<T> extends OptionKey<T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8927757348322016043L;

    /**
     * Path to an auxiliary file containing metadata encoded in an ISO 19115-3 XML document.
     * The given path, if not absolute, is relative to the path of the main storage file.
     * If the file exists, it is parsed and its content is merged or appended after the
     * metadata read by the storage. If the file does not exist, then it is ignored.
     *
     * <h4>Wildcard</h4>
     * It the {@code '*'} character is present in the path, then it is replaced by the name of the
     * main file without its extension. For example if the main file is {@code "city-center.tiff"},
     * then {@code "*.xml"} will become {@code "city-center.xml"}.
     *
     * @since 1.5
     */
    public static final OptionKey<Path> METADATA_PATH =
            new DataOptionKey<>("METADATA_PATH", Path.class);

    /**
     * Whether to assemble trajectory fragments (distinct CSV lines) into a single {@code Feature} instance
     * forming a foliation. This is ignored if the file does not seem to contain moving features.
     *
     * @since 1.0
     */
    public static final OptionKey<FoliationRepresentation> FOLIATION_REPRESENTATION =
            new DataOptionKey<>("FOLIATION_REPRESENTATION", FoliationRepresentation.class);

    /**
     * The listeners to declare as the parent of the data store listeners.
     * This option can be used when the {@link DataStore} to open is itself
     * a child of an {@link Aggregate}.
     *
     * @since 1.3
     */
    public static final OptionKey<StoreListeners> PARENT_LISTENERS =
            new DataOptionKey<>("PARENT_LISTENERS", StoreListeners.class);

    /**
     * Callback methods invoked for modifying some aspects of the grid coverages created by resources.
     *
     * @since 1.5
     */
    public static final OptionKey<CoverageModifier> COVERAGE_MODIFIER =
            new DataOptionKey<>("COVERAGE_MODIFIER", CoverageModifier.class);

    /**
     * Creates a new key of the given name.
     */
    private DataOptionKey(final String name, final Class<T> type) {
        super(name, type);
    }

    /**
     * Resolves this option key on deserialization. This method is invoked only
     * for instance of the exact {@code DataOptionKey} class, not subclasses.
     *
     * @return the unique {@code DataOptionKey} instance.
     * @throws ObjectStreamException required by specification but should never be thrown.
     */
    private Object readResolve() throws ObjectStreamException {
        try {
            return DataOptionKey.class.getField(getName()).get(null);
        } catch (ReflectiveOperationException e) {
            /*
             * This may happen if we are deserializing a stream produced by a more recent SIS library
             * than the one running in this JVM.
             */
            Logging.recoverableException(getLogger(Modules.STORAGE), DataOptionKey.class, "readResolve", e);
            return this;
        }
    }
}
