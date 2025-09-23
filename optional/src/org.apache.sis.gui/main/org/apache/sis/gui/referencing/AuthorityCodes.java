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
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableListBase;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.concurrent.Task;
import javafx.util.Callback;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.gui.internal.BackgroundThreads;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;


/**
 * A list of authority codes (usually for CRS) which fetch code values in a background thread
 * and CRS names only when needed.
 *
 * @todo {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess} internally uses a {@link java.util.Map}
 *       from codes to descriptions. We could open an access to this map for a little bit more efficiency.
 *       It will be necessary if we want to use {@link AuthorityCodes} for other kinds of objects than CRS
 *       (see {@link #type} field).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class AuthorityCodes extends ObservableListBase<Code>
        implements Callback<TableColumn.CellDataFeatures<Code,String>, ObservableValue<String>>
{
    /**
     * Delay in nanoseconds before to refresh the list with new content.
     * Data will be transferred from background threads to JavaFX threads every time this delay is elapsed.
     * The delay value is a compromise between fast user experience and giving enough time for doing a few
     * large data transfers instead of many small data transfers.
     */
    private static final long REFRESH_DELAY = Constants.NANOS_PER_SECOND / 10;

    /**
     * The table view which use this list, or {@code null} if we don't need this information anymore.
     * See {@link #describedCount} for an explanation about its purpose.
     */
    TableView<Code> owner;

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
    private final List<Object> codes;

    /**
     * Count of the number of {@linkplain #codes} for which we completed the {@link Code#name} information.
     * This is used for notifying the {@linkplain #owner} when we do not expect more information to be loaded.
     * This notification is only indicative and may not be fully accurate. Its effect should be only visual
     * (removing the hour glass icon).
     */
    private int describedCount;

    /**
     * The preferred locale of CRS descriptions.
     */
    final Locale locale;

    /**
     * The factory to use for creating coordinate reference systems,
     * or {@code null} if not yet determined.
     *
     * @see #getFactory()
     */
    private CRSAuthorityFactory factory;

    /**
     * The task where to send requests for CRS descriptions (never {@code null}).
     * The task is not necessarily running; it may have been created and not yet scheduled,
     * in which case the task is waiting in {@link Task.State#READY} state for work to arrive.
     */
    private Loader loader;

    /**
     * {@code true} if an error occurred. This is used for reporting only one error
     * for avoiding to flood the logger.
     *
     * @see #errorOccurred(Throwable)
     */
    private volatile boolean hasError;

    /**
     * Creates a new deferred list and starts a background process for loading CRS codes.
     * If the given factory is {@code null}, then a
     * {@linkplain org.apache.sis.referencing.CRS#getAuthorityFactory(String) default factory}
     * capable to handle at least some EPSG codes will be used.
     *
     * @param  factory  the authority factory, or {@code null} for default factory.
     * @param  locale   the preferred locale of CRS descriptions.
     */
    AuthorityCodes(final CRSAuthorityFactory factory, final Locale locale) {
        this.locale  = locale;
        this.factory = factory;
        this.codes   = new ArrayList<>();
        this.loader  = new Loader();
        loader.start();
    }

    /**
     * Returns the authority factory. This method may be invoked from any thread.
     * The factory is not fetched at construction time for giving {@link Loader}
     * a chance to fetch it in a background thread.
     */
    final synchronized CRSAuthorityFactory getFactory() throws FactoryException {
        if (factory == null) {
            factory = Utils.getDefaultFactory();
        }
        return factory;
    }

    /**
     * Returns the number of elements in this list. This method initially returns only the number of
     * cached elements. This number may increase progressively as the background loading progresses.
     */
    @Override
    public int size() {
        return codes.size();
    }

    /**
     * Returns the authority code at given index, eventually with its name.
     */
    @Override
    public Code get(final int index) {
        final Object value = codes.get(index);
        if (value instanceof Code) {
            return (Code) value;
        }
        // Wraps the String only when first needed.
        final Code c = new Code((String) value);
        codes.set(index, c);
        return c;
    }

    /**
     * Adds a single code. This method should never be invoked except if an error occurred
     * while loading codes, in which case we add a single pseudo-code with error message.
     */
    @Override
    public boolean add(final Code code) {
        final int i = codes.size();
        codes.add(code);
        beginChange();
        nextAdd(i, i+1);
        endChange();
        return true;
    }

    /**
     * Invoked when the name or description of an authority code is requested.
     * If the name is not available, then this method sends to the background thread a
     * request for fetching that name and update cell property when name become known.
     */
    @Override
    public ObservableValue<String> call(final TableColumn.CellDataFeatures<Code,String> cell) {
        return getName(cell.getValue()).getReadOnlyProperty();
    }

    /**
     * Returns the name (or description) for the given code.
     * If the name is not available, then this method sends to the background thread a request
     * for fetching that name and will update the returned property when the name become known.
     */
    final ReadOnlyStringWrapper getName(final Code code) {
        final ReadOnlyStringWrapper p = code.name();
        final String name = p.getValue();
        if (name == null) {
            loader.requestName(code);
        }
        return p;
    }

    /**
     * Adds new codes in this list and/or updates existing codes with CRS names.
     * This method is invoked after the background thread has loaded new codes,
     * and/or after that thread has fetched names (descriptions) of some codes.
     * We combine those two tasks in a single method in order to send a single event.
     * This method must be invoked in JavaFX thread.
     */
    private void update(final PartialResult result) {
        assert Platform.isFxApplicationThread();
        final int s = codes.size();
        if (result.codes != null) {
            codes.addAll(Arrays.asList(result.codes));
        }
        beginChange();
        nextAdd(s, codes.size());
        if (result.names != null) {
            final ListIterator<Object> it = codes.listIterator();
            while (it.hasNext()) {
                final Object value = it.next();
                final String name = result.names.remove(value);
                if (name != null) {
                    final int i = it.previousIndex();
                    if (name.isEmpty()) {
                        it.remove();                        // Remove code that we cannot resolve.
                        nextRemove(i, (Code) value);        // ClassCastException should never happen here.
                    } else {
                        ((Code) value).name().set(name);    // ClassCastException should never happen here.
                        describedCount++;
                        nextUpdate(i);
                    }
                }
            }
        }
        endChange();
        if (describedCount >= codes.size()) {
            removeHourglass();
        }
    }

    /**
     * Removes the hourglass icon which was shown in the table during initial data loading phase.
     * Removing this icon restores the JavaFX default behavior, which is to show "no data" when the
     * list is empty. We want this default behavior when we think that there is no more data to load.
     * This is especially important when the user applies a filter which produces an empty result.
     * Since the effect is only visual, its okay if the criterion for invoking this method is approximate.
     */
    private void removeHourglass() {
        if (owner != null) {
            owner.setPlaceholder(null);
            owner = null;
        }
    }

    /**
     * The result of fetching authority codes and/or fetching CRS names in a background thread.
     */
    private static final class PartialResult {
        /**
         * New CRS authority codes, or {@code null} if none.
         */
        final Object[] codes;

        /**
         * Names for some CRS codes as a modifiable map, or {@code null} if none.
         * Empty values mean that the code should be removed (because it has an error).
         */
        final Map<Code,String> names;

        /**
         * Creates a new partial result.
         */
        PartialResult(final Object[] codes, final Map<Code,String> names) {
            this.codes = codes;
            this.names = names;
        }
    }

    /**
     * Loads CRS authority codes in background thread. The background thread may send tasks to be executed
     * in JavaFX thread before the final result. The final result returned by {@link #getValue()} contains
     * only codes that have not been fetched by previous {@code Loader} task executions, or the codes for
     * which names need to be updated (see {@link #call()} for more information).
     */
    private final class Loader extends Task<PartialResult> {
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
         * Wether this task has been scheduled for execution or is already executing.
         * This flag shall be read and updated in JavaFX thread only. We cannot rely
         * on {@link #isRunning()} because that method does not return {@code true}
         * immediately after {@link BackgroundThreads#execute(Runnable)} invocation.
         *
         * @see #start()
         */
        private boolean isRunning;

        /**
         * Creates a new loader.
         */
        Loader() {
            toDescribe = new ArrayList<>();
            loadCodes  = true;
        }

        /**
         * Invoked after a background thread finished its task. Prepares a new background thread
         * for loading names (descriptions) for authority codes listed in {@link #toDescribe}.
         */
        private Loader(final Loader previous) {
            toDescribe = previous.toDescribe;
            loadCodes  = false;
        }

        /**
         * Schedule for execution in a background thread.
         * This method shall be invoked in JavaFX thread.
         */
        final void start() {
            isRunning = true;
            BackgroundThreads.execute(this);
        }

        /**
         * Sends to this background thread a request for fetching the name (description) of given code.
         * The {@link AuthorityCodes} list will receive an update event after the name has been fetched.
         * This method must be invoked from JavaFX thread.
         *
         * @param  code  the CRS authority code for which to fetch the name in background thread.
         */
        final void requestName(final Code code) {
            assert Platform.isFxApplicationThread();
            synchronized (toDescribe) {
                toDescribe.add(code);
            }
            /*
             * This task may be created and ready but not yet started. It happens if `scheduleNewLoader()`
             * found no code to process in the `toDescribe` list at the time that method has been invoked.
             */
            if (!isRunning) {
                start();
            }
        }

        /**
         * Fetches the names of all objects in the {@link #toDescribe} array and clears that array.
         * The names are returned as a map with {@link Code} as keys and names (or descriptions) as values.
         * This method is invoked from a background thread and the returned value will be consumed in JavaFX thread.
         * Some entries in the returned map be empty strings if the corresponding code should be removed.
         *
         * @param  factory  value of {@link #getFactory()}.
         * @return the names of CRS authority codes submitted to {@link #requestName(Code)}, or {@code null} if none.
         */
        private Map<Code,String> processNameRequests(final CRSAuthorityFactory factory) {
            final Code[] snapshot;
            synchronized (toDescribe) {
                final int size = toDescribe.size();
                if (size == 0) return null;
                snapshot = toDescribe.toArray(new Code[size]);
                toDescribe.clear();
            }
            final Map<Code,String> updated = new IdentityHashMap<>(snapshot.length);
            for (final Code code : snapshot) {
                String text;
                try {
                    var i18n = factory.getDescriptionText(code.code);
                    text = Strings.trimOrNull(Types.toString(i18n, locale));
                    if (text == null) {
                        text = Vocabulary.forLocale(locale).getString(Vocabulary.Keys.Unnamed);
                    }
                } catch (FactoryException e) {
                    errorOccurred(e);
                    text = "";              // Tells `AuthorityCodes.update(PartialResult)` to remove this code.
                }
                updated.put(code, text);    // Do not update code in this thread; it will be updated in JavaFX thread.
            }
            return updated;
        }

        /**
         * Invoked in background thread for reading authority codes. Intermediate results are sent
         * to the JavaFX thread every {@value #REFRESH_DELAY} nanoseconds. Requests for code names
         * are also handled in priority since they are typically required for visible cells.
         *
         * @return one or both of the followings:
         *   <ul>
         *     <li>An array of {@code String}s which contains the remaining codes that need
         *         to be sent to {@link AuthorityCodes} list.</li>
         *     <li>A {@code Map<Code,String>} which contains the codes for which the names
         *         or descriptions have been updated.</li>
         *   </ul>
         *
         * @throws Exception if an error occurred while fetching the codes or the names/descriptions.
         */
        @Override
        protected PartialResult call() throws Exception {
            long lastTime = System.nanoTime();
            List<String> codes = List.of();
            final CRSAuthorityFactory factory = getFactory();
            try {
                if (loadCodes) {
                    codes = new ArrayList<>(100);
                    final Iterator<String> it = factory.getAuthorityCodes(type).iterator();
                    while (it.hasNext()) {
                        codes.add(it.next());
                        if (System.nanoTime() - lastTime > REFRESH_DELAY) {
                            final PartialResult p = new PartialResult(codes.toArray(), processNameRequests(factory));
                            codes.clear();
                            Platform.runLater(() -> update(p));
                            lastTime = System.nanoTime();
                        }
                    }
                }
                /*
                 * At this point we loaded all authority codes. If there is some remaining codes,
                 * returns them immediately for allowing the user interface to be updated quickly.
                 * If there are no more codes to return, wait a little bit for giving a chance to
                 * the `toDescribe` list to be populated with more requests, then process them.
                 */
                if (codes.isEmpty()) {
                    Thread.sleep(REFRESH_DELAY / Constants.NANOS_PER_MILLISECOND);
                    return new PartialResult(null, processNameRequests(factory));
                }
            } catch (BackingStoreException e) {
                throw e.unwrapOrRethrow(Exception.class);
            }
            return new PartialResult(codes.toArray(), null);
        }

        /**
         * Invoked after the background thread finished to load authority codes.
         * This method adds the remaining codes to {@link AuthorityCodes} list,
         * then prepare another background tasks for loading descriptions.
         */
        @Override
        @SuppressWarnings("unchecked")
        protected void succeeded() {
            update(getValue());
            prepareNewLoader();
        }

        /**
         * Invoked if an error occurred while loading the codes. A pseudo-code is added with error message.
         * A background task is still scheduled for allowing {@link AuthorityCodes} to get descriptions of
         * codes obtained so far.
         */
        @Override
        protected void failed() {
            final Throwable e = getException();
            errorOccurred(e);
            if (loadCodes) {
                final Code code = new Code(Vocabulary.forLocale(locale).getString(Vocabulary.Keys.Errors));
                String message = Exceptions.getLocalizedMessage(e, locale);
                if (message == null) {
                    message = Classes.getShortClassName(e);
                }
                code.name().set(message);
                add(code);
            }
            removeHourglass();
            prepareNewLoader();
        }

        /**
         * Prepares the next task for loading descriptions. If new description requests were posted
         * between the end of {@link #call()} execution and the start of the {@link #succeeded()} or
         * {@link #failed()} execution, starts the new task immediately.
         */
        private void prepareNewLoader() {
            assert Platform.isFxApplicationThread();
            isRunning = false;
            loader = new Loader(this);
            final boolean isEmpty;
            synchronized (toDescribe) {
                isEmpty = toDescribe.isEmpty();
            }
            if (!isEmpty) {
                loader.start();
            }
        }
    }

    /**
     * Invoked when an error occurred. This method may be invoked from any thread.
     * Current implementation logs the first error.
     */
    private void errorOccurred(final Throwable e) {
        if (!hasError) {
            hasError = true;    // Not a big problem if we have race condition; error will just be logged twice.
            Logging.unexpectedException(LOGGER, AuthorityCodes.class, "get", e);
        }
    }
}
