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

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.LogRecord;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListener;
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
abstract class StaxStream implements AutoCloseable, WarningListener<Object> {
    /**
     * The data store for which this reader or writer has been created.
     */
    final StaxDataStore owner;

    /**
     * The underlying stream to close when this {@code StaxStream} reader or writer is closed,
     * or {@code null} if none.
     */
    private Closeable stream;

    /**
     * For sub-classes constructors.
     *
     * @param owner  the data store for which this reader or writer is created.
     */
    StaxStream(final StaxDataStore owner) {
        ArgumentChecks.ensureNonNull("owner", owner);
        this.owner = owner;
    }

    /**
     * Notifies this {@code StaxStream} that the given stream will need to be closed by the {@link #close()} method.
     * This method can be invoked at most once. This method does nothing if the given object does not implement the
     * {@link Closeable} interface.
     *
     * @param stream the stream to be closed when {@link #close()} will be invoked.
     */
    protected final void initCloseable(final Object stream) {
        if (this.stream != null) {
            throw new IllegalStateException();
        }
        if (stream instanceof Closeable) {
            this.stream = (Closeable) stream;
        }
    }

    /**
     * Closes the input or output stream and releases any resources used by this XML reader or writer.
     * This reader or writer can not be used anymore after this method has been invoked.
     *
     * @throws IOException if an error occurred while closing the input or output stream.
     * @throws XMLStreamException if an error occurred while releasing XML reader/writer resources.
     */
    @Override
    public void close() throws IOException, XMLStreamException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    /**
     * Returns the error resources in the current locale.
     */
    protected final Errors errors() {
        return Errors.getResources(owner.getLocale());
    }

    /**
     * Reports a warning represented by the given log record.
     *
     * @param source  ignored (typically a JAXB object being unmarshalled). Can be {@code null}.
     * @param record  the warning as a log record.
     */
    @Override
    public final void warningOccured(final Object source, final LogRecord warning) {
        owner.listeners().warning(warning);
    }

    /**
     * Returns the type of objects that emit warnings of interest for this listener.
     * Fixed to {@code Object.class} as required by {@link org.apache.sis.xml.XML#WARNING_LISTENER} documentation.
     */
    @Override
    public final Class<Object> getSourceClass() {
        return Object.class;
    }
}
