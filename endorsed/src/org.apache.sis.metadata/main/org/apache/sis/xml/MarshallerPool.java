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
package org.apache.sis.xml;

import java.util.Map;
import java.util.Deque;
import java.util.ServiceLoader;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.sis.util.Classes;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Reflect;
import org.apache.sis.system.Configuration;
import org.apache.sis.system.DelayedExecutor;
import org.apache.sis.system.DelayedRunnable;
import org.apache.sis.xml.bind.AdapterReplacement;
import org.apache.sis.xml.bind.TypeRegistration;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.util.internal.shared.Constants;


/**
 * Creates and configures {@link Marshaller} or {@link Unmarshaller} objects for use with SIS.
 * Users fetch (un)marshallers by calls to the {@link #acquireMarshaller()} or
 * {@link #acquireUnmarshaller()} methods, and can restitute the (un)marshaller to the pool
 * after usage like below:
 *
 * {@snippet lang="java" :
 *     Marshaller marshaller = pool.acquireMarshaller();
 *     marshaller.marchall(...);
 *     pool.recycle(marshaller);
 *     }
 *
 * <h2>Configuring (un)marshallers</h2>
 * The (un)marshallers created by this class can optionally by configured with the SIS-specific
 * properties defined in the {@link XML} class, in addition to JAXB standard properties.
 *
 * <h2>Thread safety</h2>
 * The same {@code MarshallerPool} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses should make sure that any overridden methods remain safe to call
 * from multiple threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see XML
 * @see <a href="http://jaxb.java.net/guide/Performance_and_thread_safety.html">JAXB Performance and thread-safety</a>
 *
 * @since 0.3
 */
public class MarshallerPool {
    /**
     * Amount of nanoseconds to wait before to remove unused (un)marshallers.
     * This is a very rough value: actual timeout will not be shorter,
     * but may be twice longer.
     */
    @Configuration
    private static final long TIMEOUT = 15000000000L;           // 15 seconds.

    /**
     * The JAXB context to use for creating marshaller and unmarshaller.
     *
     * @see #createMarshaller()
     * @see #createUnmarshaller()
     */
    protected final JAXBContext context;

    /**
     * The JAXB implementation, or {@code null} if unknown.
     */
    private final Implementation implementation;

    /**
     * The provider of {@code AdapterReplacement} instances.
     * <strong>Every usage of this service loader must be synchronized.</strong>
     *
     * <h4>Implementation note</h4>
     * Each {@code MarshallerPool} has its own service loader instance rather than using a system-wide instance
     * because the {@link ClassLoader} used by the service loader is the <i>context class loader</i>,
     * which depends on the thread that created the pool. So two pools in two different applications could have
     * two different set of replacements.
     */
    private final ServiceLoader<AdapterReplacement> replacements;

    /**
     * The {@link PooledTemplate} to use for initializing recycled (un)marshaller.
     */
    private final PooledTemplate template;

    /**
     * The pool of marshaller. This pool is initially empty and will be filled with elements as needed.
     * Marshallers (if any) shall be fetched using the {@link Deque#poll()} method and, after use,
     * given back to the pool using the {@link Deque#push(Object)} method.
     *
     * <p>This queue must be a thread-safe implementation, since it will not be invoked in
     * synchronized block.</p>
     *
     * @see #acquireMarshaller()
     * @see #recycle(Marshaller)
     */
    private final Deque<Marshaller> marshallers;

    /**
     * The pool of unmarshaller. This pool is initially empty and will be filled with elements as needed.
     * Unmarshallers (if any) shall be fetched using the {@link Deque#poll()} method and, after use,
     * given back to the pool using the {@link Deque#push(Object)} method.
     *
     * <p>This queue must be a thread-safe implementation, since it will not be invoked in
     * synchronized block.</p>
     *
     * @see #acquireUnmarshaller()
     * @see #recycle(Unmarshaller)
     */
    private final Deque<Unmarshaller> unmarshallers;

    /**
     * {@code true} if a task has been scheduled for removing expired (un)marshallers,
     * or {@code false} if no removal task is currently scheduled.
     *
     * @see #scheduleRemoval()
     */
    private final AtomicBoolean isRemovalScheduled;

    /**
     * Creates a new factory using the SIS default {@code JAXBContext} instance.
     * The {@code properties} map is optional. If non-null, then the keys can be {@link XML} constants or the
     * names of any other properties recognized by <em>both</em> {@code Marshaller} and {@code Unmarshaller}
     * implementations.
     *
     * <p><b>Tip:</b> if the properties for the {@code Marshaller} differ from the properties
     * for the {@code Unmarshaller}, then consider overriding the {@link #createMarshaller()}
     * or {@link #createUnmarshaller()} methods instead.</p>
     *
     * @param  properties  the properties to be given to the (un)marshaller, or {@code null} if none.
     * @throws JAXBException if the JAXB context cannot be created.
     */
    public MarshallerPool(final Map<String,?> properties) throws JAXBException {
        /*
         * We currently add the default root adapters only when using the JAXB context provided by Apache SIS.
         * We presume that if the user specified his own JAXBContext, then he does not expect us to change the
         * classes that he wants to marshal.
         */
        this(TypeRegistration.getSharedContext(), TypeRegistration.getPrivateInfo(properties));
    }

    /**
     * Creates a new factory using the given JAXB context.
     * The {@code properties} map is optional. If non-null, then the keys can be {@link XML} constants or the
     * names of any other properties recognized by <em>both</em> {@code Marshaller} and {@code Unmarshaller}
     * implementations.
     *
     * <p><b>Tip:</b> if the properties for the {@code Marshaller} differ from the properties
     * for the {@code Unmarshaller}, then consider overriding the {@link #createMarshaller()}
     * or {@link #createUnmarshaller()} methods instead.</p>
     *
     * @param  context     the JAXB context.
     * @param  properties  the properties to be given to the (un)marshaller, or {@code null} if none.
     * @throws JAXBException if the marshaller pool cannot be created.
     */
    @SuppressWarnings("this-escape")
    public MarshallerPool(final JAXBContext context, final Map<String,?> properties) throws JAXBException {
        this.context       = Objects.requireNonNull(context);
        replacements       = loader();
        implementation     = Implementation.detect(context);
        template           = new PooledTemplate(this, properties, implementation);
        marshallers        = new ConcurrentLinkedDeque<>();
        unmarshallers      = new ConcurrentLinkedDeque<>();
        isRemovalScheduled = new AtomicBoolean();
    }

    /** Temporary method to be removed after Apache SIS requires Java 24. */
    private static ServiceLoader<AdapterReplacement> loader() {
        try {
            return ServiceLoader.load(AdapterReplacement.class, Reflect.getContextClassLoader());
        } catch (SecurityException e) {
            Reflect.log(AdapterReplacement.class, "<init>", e);
            return ServiceLoader.load(AdapterReplacement.class);
        }
    }

    /**
     * Marks the given marshaller or unmarshaller available for further reuse.
     * This method:
     *
     * <ul>
     *   <li>{@link Pooled#reset(Pooled) Resets} the (un)marshaller to its initial state.</li>
     *   <li>{@linkplain Deque#push(Object) Pushes} the (un)marshaller in the given queue.</li>
     *   <li>Registers a delayed task for disposing expired (un)marshallers after the timeout.</li>
     * </ul>
     */
    private <T> void recycle(final Deque<T> queue, final T marshaller) {
        try {
            ((Pooled) marshaller).reset(template);
        } catch (JAXBException exception) {
            /*
             * Not expected to happen because we are supposed
             * to reset the properties to their initial values.
             */
            Logging.unexpectedException(Context.LOGGER, MarshallerPool.class, "recycle", exception);
            return;
        }
        queue.push(marshaller);
        scheduleRemoval();
    }

    /**
     * Schedule a new task for removing expired (un)marshallers if no such task is currently
     * registered. If a task is already registered, then this method does nothing. Note that
     * this task will actually wait for a longer time than the {@link #TIMEOUT} value before
     * to execute, in order to increase the chances to process many (un)marshallers at once.
     */
    private void scheduleRemoval() {
        if (isRemovalScheduled.compareAndSet(false, true)) {
            DelayedExecutor.schedule(new DelayedRunnable(System.nanoTime() + 2*TIMEOUT) {
                @Override public void run() {
                    removeExpired();
                }
            });
        }
    }

    /**
     * Invoked from the task scheduled by {@link #scheduleRemoval()} for removing expired
     * (un)marshallers. If some (un)marshallers remain after execution of this task, then
     * this method will reschedule a new task for checking again later.
     */
    final void removeExpired() {
        isRemovalScheduled.set(false);
        final long now = System.nanoTime();
        if (!removeExpired(marshallers, now) |                      // Really |, not ||
            !removeExpired(unmarshallers, now))
        {
            scheduleRemoval();
        }
    }

    /**
     * Removes expired (un)marshallers from the given queue.
     *
     * @param  <T>    either {@code Marshaller} or {@code Unmarshaller} type.
     * @param  queue  the queue from which to remove expired (un)marshallers.
     * @param  now    current value of {@link System#nanoTime()}.
     * @return {@code true} if the queue became empty as a result of this method call.
     */
    private static <T> boolean removeExpired(final Deque<T> queue, final long now) {
        T next;
        while ((next = queue.peekLast()) != null) {
            /*
             * The above line fetched the oldest (un)marshaller without removing it.
             * If the timeout is not yet elapsed, do not remove that (un)marshaller.
             * Since marshallers are enqueued in chronological order, the next ones
             * should be yet more recent, so it is not worth to continue the search.
             */
            if (now - ((Pooled) next).resetTime < TIMEOUT) {
                return false;
            }
            /*
             * Now remove the (un)marshaller and check again the timeout. This second
             * check is because the oldest (un)marshaller may have changed concurrently
             * (depending on the queue implementation, which depends on the target JDK).
             * If such case, restore the (un)marshaller on the queue.
             */
            next = queue.pollLast();
            if (now - ((Pooled) next).resetTime < TIMEOUT) {
                queue.addLast(next);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a JAXB marshaller from the pool. If there is no marshaller currently available
     * in the pool, then this method will {@linkplain #createMarshaller() create} a new one.
     *
     * <p>This method shall be used as below:</p>
     *
     * {@snippet lang="java" :
     *     Marshaller marshaller = pool.acquireMarshaller();
     *     marshaller.marchall(...);
     *     pool.recycle(marshaller);
     *     }
     *
     * Note that {@link #recycle(Marshaller)} shall not be invoked in case of exception,
     * since the marshaller may be in an invalid state.
     *
     * @return a marshaller configured for formatting OGC/ISO XML.
     * @throws JAXBException if an error occurred while creating and configuring a marshaller.
     */
    public Marshaller acquireMarshaller() throws JAXBException {
        Marshaller marshaller = marshallers.poll();
        if (marshaller == null) {
            marshaller = new PooledMarshaller(createMarshaller(), template);
        }
        return marshaller;
    }

    /**
     * Returns a JAXB unmarshaller from the pool. If there is no unmarshaller currently available
     * in the pool, then this method will {@linkplain #createUnmarshaller() create} a new one.
     *
     * <p>This method shall be used as below:</p>
     *
     * {@snippet lang="java" :
     *     Unmarshaller unmarshaller = pool.acquireUnmarshaller();
     *     Unmarshaller.unmarchall(...);
     *     pool.recycle(unmarshaller);
     *     }
     *
     * Note that {@link #recycle(Unmarshaller)} shall not be invoked in case of exception,
     * since the unmarshaller may be in an invalid state.
     *
     * @return a unmarshaller configured for parsing OGC/ISO XML.
     * @throws JAXBException if an error occurred while creating and configuring the unmarshaller.
     */
    public Unmarshaller acquireUnmarshaller() throws JAXBException {
        Unmarshaller unmarshaller = unmarshallers.poll();
        if (unmarshaller == null) {
            unmarshaller = new PooledUnmarshaller(createUnmarshaller(), template);
        }
        return unmarshaller;
    }

    /**
     * Acquires a unmarshaller and set the properties to the given value, if non-null.
     */
    final Unmarshaller acquireUnmarshaller(final Map<String,?> properties) throws JAXBException {
        final Unmarshaller unmarshaller = acquireUnmarshaller();
        if (properties != null) {
            for (final Map.Entry<String,?> entry : properties.entrySet()) {
                unmarshaller.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return unmarshaller;
    }

    /**
     * Declares a marshaller as available for reuse.
     * The caller should not use anymore the given marshaller after this method call,
     * since the marshaller may be re-used by another thread at any time after recycle.
     *
     * <h4>Cautions</h4>
     * <ul>
     *   <li>Do not invoke this method if the marshaller threw an exception, since the
     *       marshaller may be in an invalid state. In particular, this method should not
     *       be invoked in a {@code finally} block.</li>
     *   <li>Do not invoke this method twice for the same marshaller, unless the marshaller
     *       has been obtained by a new call to {@link #acquireMarshaller()}.
     *       In case of doubt, it is better to not recycle the marshaller at all.</li>
     * </ul>
     *
     * Note that this method does not close any output stream.
     * Closing the marshaller stream is caller's or JAXB responsibility.
     *
     * @param  marshaller  the marshaller to return to the pool.
     */
    public void recycle(final Marshaller marshaller) {
        recycle(marshallers, marshaller);
    }

    /**
     * Declares a unmarshaller as available for reuse.
     * The caller should not use anymore the given unmarshaller after this method call,
     * since the unmarshaller may be re-used by another thread at any time after recycle.
     *
     * <h4>Cautions</h4>
     * <ul>
     *   <li>Do not invoke this method if the unmarshaller threw an exception, since the
     *       unmarshaller may be in an invalid state. In particular, this method should not
     *       be invoked in a {@code finally} block.</li>
     *   <li>Do not invoke this method twice for the same unmarshaller, unless the unmarshaller
     *       has been obtained by a new call to {@link #acquireUnmarshaller()}.
     *       In case of doubt, it is better to not recycle the unmarshaller at all.</li>
     * </ul>
     *
     * Note that this method does not close any input stream.
     * Closing the unmarshaller stream is caller's or JAXB responsibility.
     *
     * @param  unmarshaller  the unmarshaller to return to the pool.
     */
    public void recycle(final Unmarshaller unmarshaller) {
        recycle(unmarshallers, unmarshaller);
    }

    /**
     * Creates an configures a new JAXB marshaller.
     * This method is invoked only when no existing marshaller is available in the pool.
     * Subclasses can override this method if they need to change the marshaller configuration.
     *
     * @return a new marshaller configured for formatting OGC/ISO XML.
     * @throws JAXBException if an error occurred while creating and configuring the marshaller.
     *
     * @see #context
     * @see #acquireMarshaller()
     */
    protected Marshaller createMarshaller() throws JAXBException {
        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        /*
         * Note: we do not set the Marshaller.JAXB_ENCODING property because specification
         * said that the default value is "UTF-8", which is what we want.
         */
        String key;
        if ((key = implementation.indentKey) != null) {
            marshaller.setProperty(key, CharSequences.spaces(Constants.DEFAULT_INDENTATION));
        }
        synchronized (replacements) {
            for (final AdapterReplacement adapter : replacements) {
                adapter.register(marshaller);
            }
        }
        return marshaller;
    }

    /**
     * Creates an configures a new JAXB unmarshaller.
     * This method is invoked only when no existing unmarshaller is available in the pool.
     * Subclasses can override this method if they need to change the unmarshaller configuration.
     *
     * @return a new unmarshaller configured for parsing OGC/ISO XML.
     * @throws JAXBException if an error occurred while creating and configuring the unmarshaller.
     *
     * @see #context
     * @see #acquireUnmarshaller()
     */
    protected Unmarshaller createUnmarshaller() throws JAXBException {
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        synchronized (replacements) {
            for (final AdapterReplacement adapter : replacements) {
                adapter.register(unmarshaller);
            }
        }
        return unmarshaller;
    }

    /**
     * {@return a string representation of this pool for debugging purposes}.
     * The string representation is unspecified and may change in any future
     * Apache SIS version.
     *
     * @since 1.5
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder(Classes.getShortClassName(this)).append('[');
        final Context c = Context.current();
        boolean s = (c != null && c.getPool() == this);
        if (s) buffer.append("in use");
        s = appendSize(buffer,   marshallers,   "marshallers", s);
            appendSize(buffer, unmarshallers, "unmarshallers", s);
        return buffer.append(']').toString();
    }

    /**
     * Appends the size of the marshaller or unmarshaller pool to the given buffer.
     * This is a helper method for {@link #toString()} only.
     */
    private static boolean appendSize(final StringBuilder buffer, final Deque<?> pool, final String label, boolean s) {
        int n = pool.size();
        if (n == 0) return s;
        if (s) buffer.append(", ");
        buffer.append(n).append(' ').append(label);
        return true;
    }
}
