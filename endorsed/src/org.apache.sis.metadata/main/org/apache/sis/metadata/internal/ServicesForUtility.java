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
package org.apache.sis.metadata.internal;

import java.text.Format;
import java.util.Locale;
import java.util.TimeZone;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.function.Supplier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.MetadataServices;
import org.apache.sis.system.SystemListener;
import org.apache.sis.metadata.sql.internal.shared.Initializer;
import org.apache.sis.metadata.internal.shared.Identifiers;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import org.apache.sis.xml.bind.Context;

// Specific to the main branch:
import org.opengis.util.CodeList;


/**
 * Implements the metadata services needed by the {@code org.apache.sis.util} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ServicesForUtility extends MetadataServices {
    /**
     * Creates a new instance. This constructor is invoked by reflection only.
     */
    public ServicesForUtility() {
    }

    /**
     * {@code true} if this thread is in the process of reading a XML document with JAXB.
     *
     * @return if XML unmarshalling is in progress in current thread.
     */
    @Override
    public boolean isUnmarshalling() {
        final Context context = Context.current();
        return (context != null) && !Context.isFlagSet(context, Context.MARSHALLING);
    }

    /**
     * Returns the title of the given enumeration or code list value.
     *
     * @param  code    the code for which to get the title.
     * @param  locale  desired locale for the title.
     * @return the title.
     *
     * @see org.apache.sis.util.iso.Types#getCodeTitle(CodeList)
     */
    @Override
    public String getCodeTitle(final CodeList<?> code, final Locale locale) {
        return Types.getCodeTitle(code).toString(locale);
    }

    /**
     * Infers an identifier from the given citation, or returns {@code null} if no identifier has been found.
     * This method returns a non-null value only if the identifier is a valid Unicode identifier.
     *
     * @param  citation  the citation for which to get the identifier, or {@code null}.
     * @return a non-empty identifier without leading or trailing whitespaces, or {@code null}.
     */
    @Override
    public String getUnicodeIdentifier(final Citation citation) {
        return Identifiers.getIdentifier(citation, true);
    }

    /**
     * Returns information about the Apache SIS configuration.
     * See super-class for a list of keys.
     *
     * @param  key     a key identifying the information to return.
     * @param  locale  language to use if possible.
     * @return the information, or {@code null} if none.
     */
    @Override
    public String getInformation(final String key, final Locale locale) {
        switch (key) {
            case "DataSource": {
                Object server = null, database = null;
                try {
                    final DataSource ds = Initializer.getDataSource();
                    if (ds != null) {
                        final Class<?> type = ds.getClass();
                        database = type.getMethod("getDatabaseName", (Class[]) null).invoke(ds, (Object[]) null);
                        server   = type.getMethod("getServerName", (Class[]) null).invoke(ds, (Object[]) null);
                    }
                } catch (NoSuchMethodException e) {
                    Logging.recoverableException(SystemListener.LOGGER, MetadataServices.class, "getInformation", e);
                } catch (Exception e) {
                    // Leave the message unchanged if it contains at least 2 words.
                    String message = Exceptions.getLocalizedMessage(e, locale);
                    if (message == null || message.indexOf(' ') < 0) {
                        message = Classes.getShortClassName(e) + ": " + message;
                    }
                    return message;
                }
                if (database != null) {
                    if (server != null) {
                        database = "//" + server + '/' + database;
                    }
                    return database.toString();
                }
                return null;
            }
            // More cases may be added in future SIS versions.
        }
        return ReferencingServices.getInstance().getInformation(key, locale);
    }

    /**
     * Creates a format for {@link org.opengis.geometry.DirectPosition} instances.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     * @return a {@link org.apache.sis.geometry.CoordinateFormat}.
     */
    @Override
    public Format createCoordinateFormat(final Locale locale, final TimeZone timezone) {
        return ReferencingServices.getInstance().createCoordinateFormat(locale, timezone);
    }

    /**
     * Returns the data source for the SIS-wide "SpatialMetadata" database.
     */
    @Override
    public DataSource getDataSource() throws SQLException {
        try {
            return Initializer.getDataSource();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(Errors.format(Errors.Keys.CanNotConnectTo_1, Initializer.DATABASE), e);
        }
    }

    /**
     * Specifies the data source to use if there is no JNDI environment or if no data source is binded
     * to {@code jdbc/SpatialMetadata}.
     */
    @Override
    public void setDataSource(final Supplier<DataSource> ds) {
        if (!Initializer.setDefault(ds)) {
            throw new IllegalStateException(Resources.format(Resources.Keys.ConnectionAlreadyInitialized_1, Initializer.DATABASE));
        }
    }
}
