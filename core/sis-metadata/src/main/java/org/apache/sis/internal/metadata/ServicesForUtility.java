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
package org.apache.sis.internal.metadata;

import java.text.Format;
import java.util.Locale;
import java.util.TimeZone;
import javax.sql.DataSource;
import org.opengis.util.CodeList;
import org.apache.sis.internal.util.MetadataServices;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;



/**
 * Implements the metadata services needed by the {@code "sis-utility"} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.6
 * @module
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
                    Logging.recoverableException(Logging.getLogger(Loggers.SYSTEM),
                            MetadataServices.class, "getInformation", e);
                } catch (Exception e) {
                    // Leave the message alone if it contains at least 2 words.
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
     *
     * @since 0.8
     */
    @Override
    public Format createCoordinateFormat(final Locale locale, final TimeZone timezone) {
        return ReferencingServices.getInstance().createCoordinateFormat(locale, timezone);
    }
}
