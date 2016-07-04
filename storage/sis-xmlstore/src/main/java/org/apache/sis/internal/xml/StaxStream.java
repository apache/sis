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

import java.util.Locale;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.util.Localized;
import org.apache.sis.util.resources.Errors;


/**
 * Common base class for {@code StaxStreamReader} and {@code StaxStreamWriter}.
 * {@code StaxStream} subclasses are not used directly (they are Apache SIS internal mechanic);
 * they are rather used as helper classes for {@link org.apache.sis.storage.DataStore} implementations.
 * Those {@code DataStore}s will typically manage {@code StaxStreamReader} and {@code StaxStreamWriter}
 * instances on which they delegate their read and write operations.
 *
 * <div class="section">Multi-threading</div>
 * This class and subclasses are not tread-safe. Synchronization shall be done by the {@code DataStore}
 * that contains the {@code StaxStream} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
abstract class StaxStream implements AutoCloseable, Localized {
    /**
     * The locale to use for error messages, or {@code null} for the default locale.
     */
    private Locale locale;

    /**
     * For sub-classes constructors.
     */
    StaxStream() {
    }

    /**
     * Sets the locale to use for formatting error messages.
     * A null value means to use the default locale.
     *
     * @param locale the locale to use for formatting error messages (can be null).
     */
    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns the locale to use for formatting error messages.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the error resources in the current locale.
     */
    protected final Errors errors() {
        return Errors.getResources(getLocale());
    }

    /**
     * Closes the input or output stream and releases any resources used by this XML reader or writer.
     * This reader or writer can not be used anymore after this method has been invoked.
     *
     * @throws IOException if an error occurred while closing the input or output stream.
     * @throws XMLStreamException if an error occurred while releasing XML reader/writer resources.
     */
    @Override
    public abstract void close() throws IOException, XMLStreamException;
}
