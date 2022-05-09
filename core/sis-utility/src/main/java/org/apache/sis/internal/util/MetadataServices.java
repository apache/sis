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

import java.text.Format;
import java.util.Locale;
import java.util.TimeZone;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import java.sql.SQLException;
import org.opengis.annotation.UML;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.OptionalDependency;
import org.apache.sis.util.CharSequences;

// Branch-dependent imports
import org.opengis.util.ControlledVocabulary;


/**
 * Provides access to services defined in the {@code "sis-metadata"} module.
 * This class searches for the {@link org.apache.sis.internal.metadata.ServicesForUtility}
 * implementation using Java reflection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.6
 * @module
 */
public class MetadataServices extends OptionalDependency {
    /**
     * A pseudo-authority name used by {@link org.apache.sis.setup.InstallationResources} for identifying
     * the embedded data resources. The actual data are provided by the metadata module.
     */
    public static final String EMBEDDED = "Embedded";

    /**
     * The services, fetched when first needed.
     */
    private static volatile MetadataServices instance;

    /**
     * For subclass only. This constructor registers this instance as a
     * {@link org.apache.sis.internal.system.SystemListener} in order to
     * force a new {@code MetadataServices} lookup if the classpath changes.
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
     * @return the singleton instance.
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
                     * In the particular case of this class, the intent is to ensure that SystemListener.add(…)
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
     * {@code true} if this thread is in the process of reading a XML document with JAXB.
     *
     * @return if XML unmarshalling is in progress in current thread.
     */
    public boolean isUnmarshalling() {
        return false;
    }

    /**
     * Returns the title of the given enumeration or code list value.
     *
     * @param  code    the code for which to get the title.
     * @param  locale  desired locale for the title.
     * @return the title.
     *
     * @see org.apache.sis.util.iso.Types#getCodeTitle(ControlledVocabulary)
     */
    public String getCodeTitle(final ControlledVocabulary code, final Locale locale) {
        /*
         * Following code reproduces the work done by org.apache.sis.util.iso.Types.getCodeList(…)
         * with less handling of special cases. It is executed only if the sis-metadata module is
         * not on the classpath, otherwise the sis-metadata implementation will be used.
         */
        final UML uml = code.getClass().getAnnotation(UML.class);
        if (uml != null) try {
            return ResourceBundle.getBundle(CodeLists.RESOURCES, locale, UML.class.getClassLoader())
                                 .getString(uml.identifier() + '.' + code.identifier());
        } catch (MissingResourceException e) {
            /*
             * Ignore. The reason for not finding the resource may because of above code not covering enough cases.
             * Usually the sis-metadata module will be present on the classpath, in which case this implementation
             * will not be used. We need just enough code for allowing sis-utility tests to pass.
             */
        }
        return CharSequences.camelCaseToSentence(code.identifier()).toString();
    }

    /**
     * Infers an identifier from the given citation, or returns {@code null} if no identifier has been found.
     * This method returns a non-null value only if the identifier is a valid Unicode identifier.
     *
     * @param  citation  the citation for which to get the identifier, or {@code null}.
     * @return a non-empty identifier without leading or trailing whitespaces, or {@code null}.
     */
    public String getUnicodeIdentifier(Citation citation) {
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
     * @param  key     a key identifying the information to return.
     * @param  locale  language to use if possible.
     * @return the information, or {@code null} if none.
     *
     * @see org.apache.sis.internal.metadata.ReferencingServices#getInformation(String, Locale)
     */
    public String getInformation(String key, Locale locale) {
        return null;
    }

    /**
     * Creates a format for {@link org.opengis.geometry.DirectPosition} instances.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     * @return a {@link org.apache.sis.geometry.CoordinateFormat}.
     */
    public Format createCoordinateFormat(final Locale locale, final TimeZone timezone) {
        throw moduleNotFound();
    }

    /**
     * Returns the data source for the SIS-wide "SpatialMetadata" database.
     *
     * @return the data source for the {@code $SIS_DATA/Databases/SpatialMetadata} or equivalent database, or {@code null} if none.
     * @throws SQLException if an error occurred while fetching the database source.
     */
    public DataSource getDataSource() throws SQLException {
        throw moduleNotFound();
    }

    /**
     * Specifies the data source to use if there is no JNDI environment or if no data source is binded
     * to {@code jdbc/SpatialMetadata}.
     *
     * @param  ds  supplier of data source to set, or {@code null} for removing previous supplier.
     *             This supplier may return {@code null}, in which case it will be ignored.
     * @throws IllegalStateException if {@link DataSource} has already be obtained before this method call.
     */
    public void setDataSource(final Supplier<DataSource> ds) {
        throw moduleNotFound();
    }
}
