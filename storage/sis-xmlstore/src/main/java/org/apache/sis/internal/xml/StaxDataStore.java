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
package org.apache.sis.internal.xml;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import org.apache.sis.xml.XML;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.logging.WarningListeners;


/**
 * Base class of XML data stores based on the STAX framework.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class StaxDataStore extends DataStore implements XMLReporter {
    /**
     * The locale to use for locale-sensitive data (<strong>not</strong> for logging or warning messages),
     * or {@code null} if unspecified.
     *
     * @see OptionKey#LOCALE
     */
    private final Locale locale;

    /**
     * The timezone to use when parsing or formatting dates and times without explicit timezone,
     * or {@code null} if unspecified.
     *
     * @see OptionKey#TIMEZONE
     */
    private final TimeZone timezone;

    /**
     * The STAX readers factory, created when first needed.
     *
     * @see #inputFactory()
     */
    private XMLInputFactory inputFactory;

    /**
     * The STAX writers factory, created when first needed.
     *
     * @see #outputFactory()
     */
    private XMLOutputFactory outputFactory;

    /**
     * Creates a new data store.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     */
    protected StaxDataStore(final StorageConnector connector) {
        super(connector);
        locale   = connector.getOption(OptionKey.LOCALE);
        timezone = connector.getOption(OptionKey.TIMEZONE);
    }

    /**
     * Returns the short name (abbreviation) of the format being read or written.
     * This is used for error messages.
     *
     * @return short name of format being read or written.
     */
    public abstract String getFormatName();

    /**
     * Returns the factory for STAX readers.
     */
    final synchronized XMLInputFactory inputFactory() {
        if (inputFactory == null) {
            inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLReporter(this);
        }
        return inputFactory;
    }

    /**
     * Returns the factory for STAX writers.
     */
    final synchronized XMLOutputFactory outputFactory() {
        if (outputFactory == null) {
            outputFactory = XMLOutputFactory.newInstance();
            outputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        }
        return outputFactory;
    }

    /**
     * Returns the properties that can be used to JAXB (un)marshaller.
     *
     * @param  target  the object for which we are creating (un)marshaller configuration.
     */
    final Map<String,Object> configuration(final StaxStreamIO target) {
        final Map<String,Object> properties = new HashMap<>(4);
        if (locale   != null) properties.put(XML.LOCALE,   locale);
        if (timezone != null) properties.put(XML.TIMEZONE, timezone);
        properties.put(XML.WARNING_LISTENER, target);
        return properties;
    }

    /**
     * Gives to {@link StaxStreamIO} an access to the {@link #listeners} field.
     */
    final WarningListeners<DataStore> listeners() {
        return listeners;
    }

    /**
     * Forwards STAX warnings to {@link DataStore} listeners.
     * This method is invoked by {@link javax.xml.stream.XMLStreamReader} when needed.
     *
     * @param message    the message to put in a logging record.
     * @param errorType  ignored.
     * @param info       ignored.
     * @param location   ignored.
     */
    @Override
    public void report(String message, String errorType, Object info, Location location) {
        final LogRecord record = new LogRecord(Level.WARNING, message);
        record.setSourceClassName(getClass().getCanonicalName());
        listeners.warning(record);
    }
}
