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
package org.apache.sis.referencing.factory;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.apache.sis.internal.jdk7.JDK7;
import org.apache.sis.internal.jdk8.JDK8;


/**
 * A lazy set of {@code IdentifiedObject} instances created from their authority codes only when first needed.
 * This set delegates {@link IdentifiedObject} creation to the most appropriate {@code createFoo(String)} method
 * of the {@link AuthorityFactory} given at construction time.
 *
 * <p>Elements can be added to this collection with calls to {@link #addAuthorityCode(String)} for deferred
 * {@linkplain #createObject(String) object creation}, or to {@link #add(IdentifiedObject)} for objects
 * that are already instantiated. This collection can not contain two {@code IdentifiedObject} instances
 * having the same identifier. However the identifiers used by this class can be controlled by overriding
 * {@link #getAuthorityCode(IdentifiedObject)}.</p>
 *
 * <p>Iterations over elements in this collection preserve insertion order.</p>
 *
 * <div class="section">Purpose</div>
 * {@code IdentifiedObjectSet} can be used as the set returned by implementations of the
 * {@link GeodeticAuthorityFactory#createFromCoordinateReferenceSystemCodes(String, String)} method.
 * Deferred creation can have great performance impact since some set may contain as much as 40 entries
 * (e.g. transformations from <cite>"ED50"</cite> (EPSG:4230) to <cite>"WGS 84"</cite> (EPSG:4326))
 * while some users only want to look for the first entry.
 *
 * <div class="section">Exception handling</div>
 * If the underlying factory failed to creates an object because of an unsupported operation method
 * ({@link NoSuchIdentifierException}), the exception is logged at {@link Level#WARNING} and the iteration continue.
 * If the operation creation failed for any other kind of reason ({@link FactoryException}), then the exception is
 * re-thrown as an unchecked {@link BackingStoreException}. This default behavior can be changed by overriding
 * the {@link #isRecoverableFailure(FactoryException)} method.
 *
 * <div class="section">Thread safety</div>
 * This class is thread-safe is the underlying {@linkplain #factory} is also thread-safe.
 * However, implementors are encouraged to wrap in {@linkplain java.util.Collections#unmodifiableSet unmodifiable set}
 * if they intend to cache {@code IdentifiedObjectSet} instances.
 *
 * @param <T> The type of objects to be included in this set.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class IdentifiedObjectSet<T extends IdentifiedObject> extends AbstractSet<T> implements CheckedContainer<T> {
    /**
     * The map of object codes (keys), and the actual identified objects (values) when they have been created.
     * Each entry has a null value until the corresponding object is created.
     *
     * <p><b>Note:</b> using {@code ConcurrentHahMap} would be more efficient.
     * But the later does not support null values and does not preserve insertion order.</p>
     */
    final Map<String,T> objects = new LinkedHashMap<String,T>();

    /**
     * The {@link #objects} keys, created for iteration purpose when first needed and cleared when the map is modified.
     * We need to use such array as a snapshot of the map state at the time the iterator was created because the map
     * may be modified during iteration or by concurrent threads.
     */
    private transient String[] codes;

    /**
     * The factory to use for creating {@code IdentifiedObject}s when first needed.
     * This is the authority factory given at construction time.
     */
    protected final AuthorityFactory factory;

    /**
     * A proxy to the most specific {@linkplain #factory} method to invoke
     * for the {@linkplain #type} of objects included in this set.
     */
    private final AuthorityFactoryProxy<? super T> proxy;

    /**
     * The type of objects included in this set.
     *
     * @see #getElementType()
     */
    private final Class<T> type;

    /**
     * Creates an initially empty set. The set can be populated after construction by calls
     * to {@link #addAuthorityCode(String)} for deferred {@code IdentifiedObject} creation,
     * or to {@link #add(IdentifiedObject)} for already instantiated objects.
     *
     * @param factory The factory to use for deferred {@code IdentifiedObject} instances creation.
     * @param type The type of objects included in this set.
     */
    public IdentifiedObjectSet(final AuthorityFactory factory, final Class<T> type) {
        ArgumentChecks.ensureNonNull("factory", factory);
        ArgumentChecks.ensureNonNull("type", type);
        proxy = AuthorityFactoryProxy.getInstance(type);
        this.factory = factory;
        this.type = type;
    }

    /**
     * Returns the type of {@code IdentifiedObject} included in this set.
     *
     * @return The type of {@code IdentifiedObject} included in this set.
     */
    @Override
    public Class<T> getElementType() {
        return type;
    }

    /**
     * Removes all of the elements from this collection.
     */
    @Override
    public void clear() {
        synchronized (objects) {
            codes = null;
            objects.clear();
        }
    }

    /**
     * Returns the number of objects available in this set. Note that this number may decrease
     * during the iteration process if the creation of some {@code IdentifiedObject}s failed.
     *
     * @return The number of objects available in this set.
     */
    @Override
    public int size() {
        synchronized (objects) {
            return objects.size();
        }
    }

    /**
     * Returns the authority codes of all {@code IdentifiedObject}s contained in this collection, in insertion order.
     * This method does not trig the {@linkplain #createObject(String) creation} of any object.
     *
     * @return The authority codes in iteration order.
     */
    public String[] getAuthorityCodes() {
        return codes().clone();
    }

    /**
     * Returns the {@code codes} array, creating it if needed. This method does not clone the array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final String[] codes() {
        synchronized (objects) {
            if (codes == null) {
                final Set<String> keys = objects.keySet();
                codes = keys.toArray(new String[keys.size()]);
            }
            return codes;
        }
    }

    /**
     * Sets the content of this collection to the object identified by the given codes.
     * For any code in the given sequence, this method will preserve the corresponding {@code IdentifiedObject}
     * instance if it was already created. Otherwise objects will be {@linkplain #createObject(String) created}
     * only when first needed.
     *
     * <div class="note"><b>Purpose:</b>
     * this method is typically used together with {@link #getAuthorityCodes()} for altering the iteration order
     * on the basis of authority codes. If the specified {@code codes} sequence contains the same elements than
     * the ones in the array returned by {@link #getAuthorityCodes()} but in a different order, then this method
     * just sets the new ordering.</div>
     *
     * @param codes The authority codes of identified objects to store in this set.
     *
     * @see #addAuthorityCode(String)
     */
    public void setAuthorityCodes(final String... codes) {
        synchronized (objects) {
            this.codes = null;
            final Map<String,T> copy = new HashMap<String,T>(objects);
            objects.clear();
            for (final String code : codes) {
                objects.put(code, copy.get(code));
            }
        }
    }

    /**
     * Ensures that this collection contains an object for the specified authority code.
     * If this collection does not contain any element for the given code, then this method
     * will instantiate an {@code IdentifiedObject} for the given code only when first needed.
     * Otherwise this collection is unchanged.
     *
     * @param  code The code authority code of the {@code IdentifiedObject} to include in this set.
     */
    public void addAuthorityCode(final String code) {
        synchronized (objects) {
            if (JDK8.putIfAbsent(objects, code, null) == null) {
                codes = null;
            }
        }
    }

    /**
     * Ensures that this collection contains the specified object. This collection does not allow
     * multiple objects for the same {@linkplain #getAuthorityCode(IdentifiedObject) authority code}.
     * If this collection already contains an object using the same authority code than the given object,
     * then the old object is replaced by the new one regardless of whether the objects themselves are equal or not.
     *
     * @param object The object to add to the set.
     * @return {@code true} if this set changed as a result of this call.
     *
     * @see #getAuthorityCode(IdentifiedObject)
     */
    @Override
    public boolean add(final T object) {
        final String code = getAuthorityCode(object);
        final T previous;
        synchronized (objects) {
            codes = null;
            previous = objects.put(code, object);
        }
        return !Objects.equals(previous, object);
    }

    /**
     * Returns the identified object for the specified value, creating it if needed.
     *
     * @throws BackingStoreException if the object creation failed.
     *
     * @see #createObject(String)
     */
    final T get(final String code) throws BackingStoreException {
        T object;
        boolean success;
        synchronized (objects) {
            object = objects.get(code);
            success = (object != null || !objects.containsKey(code));
        }
        /*
         * If we need to create the object, it should be done outside synchronized block.
         * There is a risk that the same object is created twice in concurrent threads.
         * If this happen, we will discard the duplicated value.
         */
        if (!success) {
            try {
                object = createObject(code);
                success = true;                         // Shall be set only after above line succeed.
            } catch (FactoryException exception) {
                if (!isRecoverableFailure(exception)) {
                    throw new BackingStoreException(exception);
                }
                final LogRecord record = Messages.getResources(null).getLogRecord(Level.WARNING,
                        Messages.Keys.CanNotInstantiateForIdentifier_3, type, code, getCause(exception));
                record.setLoggerName(Loggers.CRS_FACTORY);
                Logging.log(IdentifiedObjectSet.class, "createObject", record);
            }
            synchronized (objects) {
                if (success) {
                    /*
                     * The check for 'containsKey' is a paranoiac check in case the element has been removed
                     * in another thread while we were creating the object. This is likely to be unnecessary
                     * in the vast majority of cases where the set of codes is never modified after this set
                     * has been published. However, if someone decided to do such concurrent modifications,
                     * not checking for concurrent removal could be a subtle and hard-to-find bug, so we are
                     * better to be safe. Note that if a concurrent removal happened, we still return the non-null
                     * object but we do not put it in this IdentifiedObjectSet. This behavior is as if this method
                     * has been invoked before the concurrent removal happened.
                     */
                    if (objects.containsKey(code)) {
                        final T c = JDK8.putIfAbsent(objects, code, object);
                        if (c != null) {
                            object = c;                     // The object has been created concurrently.
                        } else {
                            codes = null;
                        }
                    }
                } else if (JDK8.remove(objects, code, null)) {    // Do not remove if a concurrent thread succeeded.
                    codes = null;
                }
            }
        }
        return object;
    }

    /**
     * Returns {@code true} if this collection contains the specified {@code IdentifiedObject}.
     *
     * @param  object The {@code IdentifiedObject} to test for presence in this set.
     * @return {@code true} if the given object is presents in this set.
     */
    @Override
    public boolean contains(final Object object) {
        return (object != null) && object.equals(get(getAuthorityCode(type.cast(object))));
    }

    /**
     * Removes the object for the given code.
     *
     * @param code The code of the object to remove.
     */
    final void removeAuthorityCode(final String code) {
        synchronized (objects) {
            objects.remove(code);
            codes = null;
        }
    }

    /**
     * Removes the specified {@code IdentifiedObject} from this collection, if it is present.
     *
     * @param  object The {@code IdentifiedObject} to remove from this set.
     * @return {@code true} if this set changed as a result of this call.
     */
    @Override
    public boolean remove(final Object object) {
        if (object != null) {
            final String code = getAuthorityCode(type.cast(object));
            final T current = get(code);
            if (object.equals(current)) {
                synchronized (objects) {
                    objects.remove(code);
                    codes = null;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Removes from this collection all of its elements that are contained in the specified collection.
     *
     * @param  collection The {@code IdentifiedObject}s to remove from this set.
     * @return {@code true} if this set changed as a result of this call.
     */
    @Override
    public boolean removeAll(final Collection<?> collection) {
        boolean modified = false;
        for (final Object object : collection) {
            modified |= remove(object);
        }
        return modified;
    }

    /**
     * Returns an iterator over the objects in this set. If the iteration encounter any kind of
     * {@link FactoryException} other than {@link NoSuchIdentifierException}, then the exception
     * will be re-thrown as an unchecked {@link BackingStoreException}.
     *
     * <p>This iterator is <strong>not</strong> thread safe – iteration should be done in a single thread.
     * However the iterator is robust to concurrent changes in {@code IdentifiedObjectSet} during iteration.</p>
     *
     * @return An iterator over all {@code IdentifiedObject} instances in this set, in insertion order.
     * @throws BackingStoreException if an error occurred while creating the iterator.
     */
    @Override
    public Iterator<T> iterator() throws BackingStoreException {
        return new Iterator<T>() {
            /**
             * The keys from the underlying map, as a snapshot taken at the time the iterator has been created.
             * We need to take a snapshot because the underlying {@link IdentifiedObjectSet#objects} map may be
             * modified concurrently in other threads.
             */
            private final String[] keys = codes();

            /**
             * Index of the next element to obtain by a call to {@link IdentifiedObjectSet#get(String)}.
             */
            private int index;

            /**
             * The next element to return, or {@code null} if not yet determined.
             */
            private T next;

            /**
             * {@code true} if {@link #remove()} can remove the element identified by {@code keys[index - 1]}.
             */
            private boolean canRemove;

            /**
             * Returns {@code true} if there is more elements.
             *
             * @throws BackingStoreException if the underlying factory failed to creates the {@code IdentifiedObject}.
             */
            @Override
            public boolean hasNext() throws BackingStoreException {
                while (next == null) {
                    if (index >= keys.length) {
                        return false;
                    }
                    next = get(keys[index++]);
                }
                return true;
            }

            /**
             * Returns the next element.
             *
             * @throws NoSuchElementException if there is no more {@code IdentifiedObject} to iterate.
             */
            @Override
            public T next() throws NoSuchElementException {
                if (canRemove = hasNext()) {
                    final T e = next;
                    next = null;
                    return e;
                }
                throw new NoSuchElementException();
            }

            /**
             * Removes the previous element from the underlying set.
             */
            @Override
            public void remove() {
                if (!canRemove) {
                    throw new IllegalStateException();
                }
                removeAuthorityCode(keys[index - 1]);
                canRemove = false;
            }
        };
    }

    /**
     * Ensures that the <var>n</var> first objects in this set are created. This method can be invoked for
     * making sure that the underlying {@linkplain #factory} is really capable to create at least one object.
     * {@link FactoryException} (except the ones accepted as {@linkplain #isRecoverableFailure recoverable failures})
     * are thrown as if they were never wrapped into {@link BackingStoreException}.
     *
     * @param n The number of object to resolve. If this number is equals or greater than {@link #size()}, then
     *          this method ensures that all {@code IdentifiedObject} instances in this collection are created.
     * @throws FactoryException if an {@linkplain #createObject(String) object creation} failed.
     */
    public void resolve(int n) throws FactoryException {
        if (n > 0) try {
            for (final Iterator<T> it=iterator(); it.hasNext(); it.next()) {
                if (--n == 0) {
                    break;
                }
            }
        } catch (BackingStoreException exception) {
            throw exception.unwrapOrRethrow(FactoryException.class);
        }
    }

    /**
     * Returns the identifier for the specified object.
     * The default implementation takes the first of the following identifier which is found:
     *
     * <ol>
     *   <li>An identifier allocated by the authority given by
     *       <code>{@linkplain #factory}.{@linkplain GeodeticAuthorityFactory#getAuthority() getAuthority()}</code>.</li>
     *   <li>The first {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getIdentifiers() object identifier},
     *       regardless its authority.</li>
     *   <li>The first {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName() object name},
     *       regardless its authority.</li>
     * </ol>
     *
     * Subclasses may override this method if they want to use a different identifiers.
     *
     * @param  object The object for which to get the authority code.
     * @return The authority code of the given identified object.
     */
    protected String getAuthorityCode(final T object) {
        final Identifier id = IdentifiedObjects.getIdentifier(object, factory.getAuthority());
        return (id != null) ? id.getCode() : IdentifiedObjects.getIdentifierOrName(object);
    }

    /**
     * Creates an object for the specified authority code.
     * This method is invoked during the iteration process if an object was not already created.
     *
     * @param  code The code for which to create the identified object.
     * @return The identified object created from the given code.
     * @throws FactoryException If the object creation failed.
     */
    protected T createObject(final String code) throws FactoryException {
        return type.cast(proxy.createFromAPI(factory, code));
    }

    /**
     * Returns {@code true} if the specified exception should be handled as a recoverable failure.
     * This method is invoked during the iteration process if the factory failed to create some objects.
     * If this method returns {@code true} for the given exception, then the exception will be logged
     * at {@link Level#WARNING}. If this method returns {@code false}, then the exception will be re-thrown
     * as a {@link BackingStoreException}.
     *
     * <p>The default implementation applies the following rules:</p>
     * <ul>
     *   <li>If {@link NoSuchAuthorityCodeException}, returns {@code false} since failure to find a code declared
     *       in the collection would be an inconsistency. Note that this exception is a subtype of
     *       {@code NoSuchIdentifierException}, so it must be tested before the last case below.</li>
     *   <li>If {@link NoSuchIdentifierException}, returns {@code true} since this exception is caused by an attempt to
     *       {@linkplain org.opengis.referencing.operation.MathTransformFactory#createParameterizedTransform
     *       create a parameterized transform} for an unimplemented operation.</li>
     *   <li>If {@link MissingFactoryResourceException}, returns {@code true}.</li>
     *   <li>Otherwise returns {@code false}.</li>
     * </ul>
     *
     * @param  exception The exception that occurred while creating an object.
     * @return {@code true} if the given exception should be considered recoverable,
     *         or {@code false} if it should be considered fatal.
     */
    protected boolean isRecoverableFailure(final FactoryException exception) {
        if (exception instanceof NoSuchIdentifierException) {
            return !(exception instanceof NoSuchAuthorityCodeException);
        }
        return (exception instanceof MissingFactoryResourceException);
    }

    /**
     * Returns the message to format below the logging for giving the cause of an error.
     */
    private static String getCause(Throwable cause) {
        final String lineSeparator = JDK7.lineSeparator();
        final StringBuilder trace = new StringBuilder(180);
        while (cause != null) {
            trace.append(lineSeparator).append("  • ").append(Classes.getShortClassName(cause));
            final String message = cause.getLocalizedMessage();
            if (message != null) {
                trace.append(": ").append(message);
            }
            cause = cause.getCause();
        }
        return trace.toString();
    }
}
