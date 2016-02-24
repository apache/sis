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
package org.apache.sis.internal.util;

import java.util.Locale;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.OptionalDependency;


/**
 * Provides access to services defined in the {@code "sis-metadata"} module.
 * This class searches for the {@link org.apache.sis.internal.metadata.ServicesForUtility}
 * implementation using Java reflection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public class MetadataServices extends OptionalDependency {
    /**
     * The services, fetched when first needed.
     */
    private static volatile MetadataServices instance;

    /**
     * For subclass only. This constructor registers this instance as a {@link SystemListener}
     * in order to force a new {@code MetadataServices} lookup if the classpath changes.
     */
    protected MetadataServices() {
        super(Modules.UTILITIES, "sis-metadata");
    }

    /**
     * Invoked when the classpath changed. Resets the {@link #instance} to {@code null} in order
     * to force the next call to {@link #getInstance()} to fetch a new one, which may be different.
     */
    @Override
    protected final void classpathChanged() {
        synchronized (MetadataServices.class) {
            super.classpathChanged();
            instance = null;
        }
    }

    /**
     * Returns the singleton instance.
     *
     * @return The singleton instance.
     */
    @SuppressWarnings("DoubleCheckedLocking")
    public static MetadataServices getInstance() {
        MetadataServices c = instance;
        if (c == null) {
            synchronized (MetadataServices.class) {
                c = instance;
                if (c == null) {
                    /*
                     * Double-checked locking: okay since Java 5 provided that the 'instance' field is volatile.
                     * In the particular case of this class, the intend is to ensure that SystemListener.add(…)
                     * is invoked only once.
                     */
                    c = getInstance(MetadataServices.class, Modules.UTILITIES, "sis-metadata",
                            "org.apache.sis.internal.metadata.ServicesForUtility");
                    if (c == null) {
                        c = new MetadataServices();
                    }
                    instance = c;
                }
            }
        }
        return c;
    }

    /**
     * Returns the constant defined in the {@link org.apache.sis.metadata.iso.citation.Citations} class for the
     * given name. This is used at {@link org.apache.sis.internal.simple.CitationConstant} deserialization time,
     * for which the two citations of interest are {@code "ISBN"} (International Standard Book Number) and
     * {@code "ISSN"} (International Standard Serial Number) citation.
     *
     * @param  name The name of one of the citation constants defined in the {@code Citations} class.
     * @return The requested citation, or {@code null} if the {@code sis-metadata} module is not available.
     */
    public CitationConstant getCitationConstant(final String name) {
        return null;
    }

    /**
     * Returns the build-in citation for the given primary key, or {@code null} if none.
     * The metadata module will search in a database for information like a descriptive
     * title, abbreviations, identifiers, URL to a web site, <i>etc</i>.
     *
     * @param  key The primary key of the desired citation.
     * @return The requested citation, or {@code null} if the {@code sis-metadata} module is not available.
     */
    public Citation createCitation(final String key) {
        return null;
    }

    /**
     * Returns information about the Apache SIS configuration to be reported in {@link org.apache.sis.setup.About}.
     * This method is invoked only for aspects that depends on other modules than {@code sis-utility}.
     *
     * <p>Current keys are:</p>
     * <ul>
     *   <li>{@code "EPSG"}: version of EPSG database.</li>
     *   <li>{@code "DataSource"}: URL to the data source, or error message.</li>
     * </ul>
     *
     * @param  key A key identifying the information to return.
     * @param  locale Language to use if possible.
     * @return The information, or {@code null} if none.
     *
     * @see org.apache.sis.internal.metadata.ReferencingServices#getInformation(String)
     */
    public String getInformation(String key, Locale locale) {
        return null;
    }
}
