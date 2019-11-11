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
package org.apache.sis.gui.referencing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableListBase;
import javafx.scene.control.TableColumn;
import javafx.concurrent.Task;
import javafx.util.Callback;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.internal.util.StandardDateFormat;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.util.Constants;


/**
 * A list of authority codes (usually for CRS) which fetch code values in a background thread
 * and descriptions only when needed.
 *
 * @todo {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess} internally uses a {@link java.util.Map}
 *       from codes to descriptions. We could open an access to this map for a little bit more efficiency.
 *       It will be necessary if we want to use {@link AuthorityCodes} for other kinds of objects than CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class AuthorityCodes extends ObservableListBase<Code>
        implements Callback<TableColumn.CellDataFeatures<Code,String>, ObservableValue<String>>
{
    /**
     * Delay in nanoseconds before to refresh the list with new content.
     * Data will be transferred from background threads to JavaFX threads every time this delay is elapsed.
     * Delay value is a compromise between fast user experience and giving enough time for allowing a few
     * large data transfers instead than many small data transfers.
     */
    private static final long REFRESH_DELAY = StandardDateFormat.NANOS_PER_SECOND / 10;

    /**
     * The type of object for which we want authority codes. Fixed to {@link CoordinateReferenceSystem} for now,
     * but could be made configurable in a future version. Making this field configurable would require resolving
     * the "todo" documented in class javadoc.
     */
    private static final Class<? extends IdentifiedObject> type = CoordinateReferenceSystem.class;

    /**
     * The authority codes obtained from the factory. The list elements are provided by a background thread.
     * Elements are initially {@link String} instances and can be replaced later by {@link Code} instances.
     */
    private Object[] codes;

    /**
     * The preferred locale of CRS descriptions.
     */
    final Locale locale;

    /**
     * The task where to send request for CRS descriptions, or {@code null} if an error occurred.
     * In later case, no more background tasks will be scheduled.
     */
    private Loader loader;

    /**
     * Creates a new deferred list and starts a background process for loading CRS codes.
     */
    AuthorityCodes(final CRSAuthorityFactory factory, final Locale locale) {
        this.locale = locale;
        codes  = new Object[0];
        loader = new Loader(factory);
        BackgroundThreads.execute(loader);
    }

    /**
     * Returns the number of elements in this list. This method initially returns only the number of
     * cached elements. This number may increase progressively as the background loading progresses.
     */
    @Override
    public int size() {
        return codes.length;
    }

    /**
     * Returns the authority code at given index, eventually with its name.
     */
    @Override
    public Code get(final int index) {
        final Object value = codes[index];
        if (value instanceof Code) {
            return (Code) value;
        }
        // Wraps the String only when first needed.
        final Code c = new Code((String) value);
        codes[index] = c;
        return c;
    }

    /**
     * Adds a single code. This method should never be invoked except of an error occurred
     * while loading codes, in which case we add a single pseudo-code with error message.
     */
    @Override
    public boolean add(final Code code) {
        final int i = codes.length;
        codes = Arrays.copyOf(codes, i + 1);
        codes[i] = code;
        beginChange();
        nextAdd(i, i+1);
        endChange();
        return true;
    }

    /**
     * Invoked when the name or description of an authority code is requested.
     * If the name is not available, then this method sends to the background thread a
     * request for fetching that name and update this property when name become known.
     */
    @Override
    public ObservableValue<String> call(final TableColumn.CellDataFeatures<Code,String> cell) {
        return getName(cell.getValue()).getReadOnlyProperty();
    }

    /**
     * Returns the name (or description) for the given code.
     * If the name is not available, then this method sends to the background thread a
     * request for fetching that name and update this property when name become known.
     */
    final ReadOnlyStringWrapper getName(final Code code) {
        final ReadOnlyStringWrapper p = code.name();
        final String name = p.getValue();
        if (name == null && loader != null) {
            loader.requestName(code);
        }
        return p;
    }

    /**
     * Adds new codes in this list and/or updates existing codes with CRS names.
     * This method is invoked after the background thread has loaded new codes,
     * and/or after that thread has fetched names (descriptions) of some codes.
     * We combine those two tasks in a single method in order to send a single event.
     *
     * @param newCodes  new codes as {@link String} instances, or {@code null} if none.
     * @param updated   {@link Code} instances to update with new names, or {@code null} if none.
     */
    private void update(final Object[] newCodes, final Map<Code,String> updated) {
        final int s = codes.length;
        int n = s;
        if (newCodes != null) {
            codes = Arrays.copyOf(codes, n += newCodes.length);
            System.arraycopy(newCodes, 0, codes, s, newCodes.length);
        }
        beginChange();
        if (updated != null) {
            for (int i=0; i<s; i++) {                           // Update names first for having increasing indices.
                final Object value = codes[i];
                final String name = updated.remove(value);
                if (name != null) {
                    ((Code) value).name().set(name);            // The name needs to be set in JavaFX thread.
                    nextUpdate(i);
                }
            }
        }
        nextAdd(s, n);
        endChange();
    }

    /**
     * Loads a {@link AuthorityCodes} codes in background thread. This background thread may send tasks
     * to be executed in JavaFX thread before the final result. The final result contains only the codes
     * that have not been processed by above-cited tasks or the codes for which names need to be updated
     * (see {@link #call()} for more information).
     */
    private final class Loader extends Task<Object> {
        /**
         * The factory to use for creating coordinate reference systems,
         * or {@code null} if not yet determined.
         */
        private CRSAuthorityFactory factory;

        /**
         * The items for which {@link Code#name} has been requested.
         * Completing those items have priority over completing {@link AuthorityCodes} because
         * those completion requests should happen only for cells that are currently visible.
         * This list is read and written by two different threads; usages must be synchronized.
         */
        private final List<Code> toDescribe;

        /**
         * {@code true} for loading authority codes in addition of processing {@link #toDescribe},
         * or {@code false} if codes are already loaded. In later case this task will only process
         * the {@link #toDescribe} list.
         */
        private final boolean loadCodes;

        /**
         * Creates a new loader using the given factory. If the given factory is null, then the
         * {@linkplain CRS#getAuthorityFactory(String) Apache SIS default factory} will be used.
         */
        Loader(final CRSAuthorityFactory factory) {
            this.factory = factory;
            toDescribe   = new ArrayList<>();
            loadCodes    = true;
        }

        /**
         * Invoked after a background thread finished its task. Prepares a new background thread
         * for loading names (descriptions) for authority codes listed in {@link #toDescribe}.
         */
        private Loader(final Loader previous) {
            factory    = previous.factory;
            toDescribe = previous.toDescribe;
            loadCodes  = false;
        }

        /**
         * Sends to this background thread a request for fetching the name (description) of given code.
         * The {@link AuthorityCodes} list will receive an update event after the name has been fetched.
         * This method is invoked from JavaFX thread.
         */
        final void requestName(final Code code) {
            synchronized (toDescribe) {
                toDescribe.add(code);
            }
            if (!isRunning()) {                         // Include "scheduled" state.
                BackgroundThreads.execute(this);
            }
        }

        /**
         * Fetches the names of all objects in the {@link #toDescribe} array and clears that array.
         * The names are returned as a map with {@link Code} as keys and names (descriptions) as values.
         * This method is invoked from background thread and returned value will be consumed in JavaFX thread.
         */
        private Map<Code,String> processNameRequests() throws FactoryException {
            final Code[] snapshot;
            synchronized (toDescribe) {
                final int size = toDescribe.size();
                if (size == 0) return null;
                snapshot = toDescribe.toArray(new Code[size]);
                toDescribe.clear();
            }
            final Map<Code,String> updated = new IdentityHashMap<>(snapshot.length);
            for (final Code code : snapshot) {
                // Do not update code in this thread; it will be updated in JavaFX thread.
                updated.put(code, factory.getDescriptionText(code.code).toString(locale));
            }
            return updated;
        }

        /**
         * Invoked in background thread for reading authority codes. Intermediate results are sent
         * to the JavaFX thread every {@value #REFRESH_DELAY} nanoseconds. Requests for code names
         * are also handled in priority since they are typically for visible cells.
         *
         * @return one of the followings:
         *   <ul>
         *     <li>A {@code List<String>} which contains the remaining codes that need to be
         *         sent to {@link AuthorityCodes} list.</li>
         *     <li>A {@code Map<Code,String>} which contains the codes for which the names
         *         or descriptions have been updated.</li>
         *   </ul>
         *
         * @throws Exception if an error occurred while fetching the codes or the names/descriptions.
         */
        @Override
        protected Object call() throws Exception {
            long lastTime = System.nanoTime();
            List<String> codes = Collections.emptyList();
            try {
                if (factory == null) {
                    factory = CRS.getAuthorityFactory(Constants.EPSG);
                }
                if (loadCodes) {
                    codes = new ArrayList<>(100);
                    final Iterator<String> it = factory.getAuthorityCodes(type).iterator();
                    while (it.hasNext()) {
                        codes.add(it.next());
                        if (System.nanoTime() - lastTime > REFRESH_DELAY) {
                            final Object[] newCodes = codes.toArray();                // Snapshot of current content.
                            codes.clear();
                            final Map<Code,String> updated = processNameRequests();   // Must be outside lambda expression.
                            Platform.runLater(() -> update(newCodes, updated));
                            lastTime = System.nanoTime();
                        }
                    }
                }
                /*
                 * At this point we loaded all authority codes. If there is some remaining codes,
                 * returns them immediately for allowing the user interface to be updated quickly.
                 * If there is no more codes to return, wait a little bit for giving a chance to
                 * the `toDescribe` list to be populated with more requests, then process them.
                 */
                if (codes.isEmpty()) {
                    Thread.sleep(REFRESH_DELAY / StandardDateFormat.NANOS_PER_MILLISECOND);
                    return processNameRequests();
                }
            } catch (BackingStoreException e) {
                throw e.unwrapOrRethrow(Exception.class);
            }
            return codes;
        }

        /**
         * Invoked after the background thread finished to load authority codes.
         * This method adds the remaining codes to {@link AuthorityCodes} list,
         * then prepare another background tasks for loading descriptions.
         */
        @Override
        @SuppressWarnings("unchecked")
        protected void succeeded() {
            super.succeeded();
            Object[] newCodes = null;
            Map<Code,String> updated = null;
            final Object result = getValue();
            if (result instanceof List<?>){
                final List<?> codes = (List<?>) result;
                if (!codes.isEmpty()) {
                    newCodes = codes.toArray();
                }
            } else {
                updated = (Map<Code,String>) result;
            }
            update(newCodes, updated);
            /*
             * Prepare the next task for loading description. If new description requests were posted
             * between the end of `call()` execution and the start of this `succeeded()` execution,
             * starts the new task immediately.
             */
            loader = new Loader(this);
            final boolean isEmpty;
            synchronized (toDescribe) {
                isEmpty = toDescribe.isEmpty();
            }
            if (!isEmpty) {
                BackgroundThreads.execute(loader);
            }
        }

        /**
         * Invoked if an error occurred while loading the codes. A pseudo-code is added with error message
         * and no more background tasks will be scheduled.
         */
        @Override
        protected void failed() {
            super.failed();
            loader = null;
            final Throwable e = getException();
            final Code code = new Code(Vocabulary.getResources(locale).getString(Vocabulary.Keys.Errors));
            String message = Exceptions.getLocalizedMessage(e, locale);
            if (message == null) {
                message = e.toString();
            }
            code.name().set(message);
            add(code);
        }
    }
}
