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
package org.apache.sis.process;

import java.util.List;
import java.util.ServiceLoader;
import org.opengis.metadata.Identifier;
import org.opengis.util.NoSuchIdentifierException;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * Utility class for finding process descriptors.
 * Descriptors are discovered with {@link ServiceLoader}. Implementations of {@link ProcessDescriptor}
 * shall be declared as services, either in the {@code module-info.java} file of the module providing them:
 *
 * {@snippet lang="java" :
 *     provides org.apache.sis.process.ProcessDescriptor with com.myorg.MyProcessDescriptor;
 *     }
 *
 * or, for libraries on the class-path, in a {@code META-INF/services/org.apache.sis.process.ProcessDescriptor} file.
 * Descriptors are loaded at the first call of a {@code ProcessFinder} method, then cached.
 * Invoke {@link #scanForPlugins()} for discarding the cache.
 *
 * <h2>Usage example</h2>
 * {@snippet lang="java" :
 *     // Get all descriptors.
 *     List<ProcessDescriptor> descriptors = ProcessFinder.getProcessDescriptors();
 *
 *     // Get a descriptor by its name, regardless of the authority.
 *     ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor("myProcessName");
 *
 *     // Get a descriptor by its name in the namespace of a specific authority.
 *     ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor("myAuthority", "myProcessName");
 *     }
 *
 * @author Johann Sorel (Geomatys)
 * @author Quentin Boileau (Geomatys)
 */
public final class ProcessFinder {
    /**
     * All process descriptors found by the service loader,
     * or {@code null} if not yet loaded or if a rescan has been requested.
     */
    private static List<ProcessDescriptor> descriptors;

    /**
     * Do not allow instantiation of this class.
     */
    private ProcessFinder() {
    }

    /**
     * Returns all process descriptors declared as services.
     *
     * @return all available process descriptors (unmodifiable).
     */
    public static synchronized List<ProcessDescriptor> getProcessDescriptors() {
        if (descriptors == null) {
            descriptors = ServiceLoader.load(ProcessDescriptor.class).stream().map(ServiceLoader.Provider::get).toList();
        }
        return descriptors;
    }

    /**
     * Returns the process descriptor of the given name, regardless of its authority.
     * If many authorities provide a process of the given name, the first one found is returned.
     *
     * @param  name  {@linkplain Identifier#getCode() code} of the process descriptor identifier.
     * @return the process descriptor (never null).
     * @throws NoSuchIdentifierException if no descriptor exists for the given name.
     */
    public static ProcessDescriptor getProcessDescriptor(final String name) throws NoSuchIdentifierException {
        return getProcessDescriptor(null, name);
    }

    /**
     * Returns the process descriptor of the given name in the namespace of the given authority.
     * The authority is compared to the {@linkplain Identifier#getCodeSpace() code space} and to the
     * {@linkplain Identifier#getAuthority() authority} identifiers of the process descriptor identifier.
     *
     * @param  authority  authority of the process, or {@code null} or blank for searching in all authorities.
     * @param  name       {@linkplain Identifier#getCode() code} of the process descriptor identifier.
     * @return the process descriptor (never null).
     * @throws NoSuchIdentifierException if no descriptor exists for the given authority and name.
     */
    public static ProcessDescriptor getProcessDescriptor(String authority, final String name) throws NoSuchIdentifierException {
        if (authority != null && authority.isBlank()) {
            authority = null;
        }
        for (final ProcessDescriptor candidate : getProcessDescriptors()) {
            final Identifier id = candidate.getIdentifier();
            if (id != null && id.getCode().equals(name)) {
                if (authority == null || authority.equalsIgnoreCase(id.getCodeSpace())
                        || Citations.identifierMatches(id.getAuthority(), authority))
                {
                    return candidate;
                }
            }
        }
        final String code = (authority != null) ? authority + ':' + name : name;
        throw new NoSuchIdentifierException("No process for the given identifier.", code);
    }

    /**
     * Discards the cached list of process descriptors.
     * Services will be searched again at the next call of a {@code ProcessFinder} method.
     */
    public static synchronized void scanForPlugins() {
        descriptors = null;
    }
}
