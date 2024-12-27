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
package org.apache.sis.xml.bind;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;
import java.util.Optional;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.function.Supplier;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.system.Modules;
import org.apache.sis.system.Reflect;
import org.apache.sis.system.SystemListener;
import org.apache.sis.system.DelayedExecutor;
import org.apache.sis.system.DelayedRunnable;


/**
 * Declares the classes of objects to be marshalled using a default {@code MarshallerPool}.
 * This class is not strictly necessary for marshalling a SIS object using JAXB, but makes
 * the job easier by allowing {@code MarshallerPool} to configure the JAXB context automatically.
 * To allow such automatic configuration, modules must declare instances of this interface
 * as a service in their {@code module-info.java} files.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.xml.MarshallerPool
 */
public abstract class TypeRegistration {
    /**
     * Undocumented (for now) marshaller property for specifying conversions to apply on root objects
     * before marshalling. Conversions are applied by {@link UnaryOperator} instances.
     *
     * @see #getPrivateInfo(Map)
     */
    public static final String ROOT_ADAPTERS = "org.apache.sis.xml.rootAdapters";

    /**
     * The JAXB context, or {@code null} if not yet created or if the module path changed.
     *
     * @see #getSharedContext()
     */
    private static Reference<JAXBContext> context;

    /**
     * Converters to apply before to marshal an object, or an empty array if none.
     * This is {@code null} if not yet initialized or if module path changed.
     *
     * @see #getPrivateInfo(Map)
     */
    private static UnaryOperator<Object>[] converters;

    /**
     * The registrations, cached only a few seconds. We do not need to keep them a long time
     * because {@link TypeRegistration} is used only for initializing some classes or fields.
     *
     * @see #services()
     */
    private static ServiceLoader<TypeRegistration> services;

    /**
     * Forces reloading of JAXB context and converters if the module path changes.
     */
    static {
        SystemListener.add(new SystemListener(Modules.UTILITIES) {
            @Override protected void classpathChanged() {
                synchronized (TypeRegistration.class) {
                    context    = null;
                    converters = null;
                    services   = null;
                }
            }
        });
    }

    /**
     * For subclasses constructors.
     */
    protected TypeRegistration() {
    }

    /**
     * Adds to the given collection every types that should be given to the initial JAXB context.
     * The types added by this method include only implementation classes having JAXB annotations.
     * If the module can also marshal arbitrary implementations of some interfaces (e.g. GeoAPI),
     * then the {@link #beforeMarshal()} method should be overridden.
     *
     * @param  addTo  the collection in which to add new types.
     */
    protected abstract void getTypes(final Collection<Class<?>> addTo);

    /**
     * If some objects need to be converted before marshalling, the converter for performing those conversions.
     * The converter {@code apply(Object)} method may return {@code null} if the value class is not recognized,
     * or {@code value} if the class is recognized but the value does not need to be changed.
     * Implementations will typically perform an {@code instanceof} check, then invoke one
     * of the {@code castOrCopy(…)} static methods defined in various Apache SIS classes.
     *
     * @return converter for the value to marshal, or {@code null} if there are no values to convert.
     */
    protected UnaryOperator<Object> beforeMarshal() {
        return null;
    }

    /**
     * Opens a stream on the {@code "RenameOnImport.lst"} or {@code "RenameOnExport.lst"} file if present.
     * Those files are typically located in the same directory as this {@code TypeRegistration} subclass.
     *
     * <h4>Design note</h4>
     * If a modularized project, the call to {@link Class#getResourceAsStream(String)} must be done in the
     * module which contains the resource. It is not possible to only request the filename and open the
     * resources in this metadata module.
     *
     * @param  export    {@code true} for {@code "RenameOnExport.lst"}, {@code false} for {@code "RenameOnImport.lst"}.
     * @param  filename  the filename as documented in the {@code export} argument, for convenience.
     * @return stream opened on the {@code "RenameOnImport.lst"} or the {@code "RenameOnExport.lst"} file.
     */
    protected Optional<InputStream> getRenameDefinitionsFile(boolean export, String filename) {
        return Optional.empty();
    }

    /**
     * Returns all input streams for loading the {@code "RenameOnImport.lst"} or {@code "RenameOnExport.lst"} file.
     * Each stream will be opened only when requested. The supplier returns {@code null} after the last stream has
     * been returned.
     *
     * @param  export    {@code true} for {@code "RenameOnExport.lst"}, {@code false} for {@code "RenameOnImport.lst"}.
     * @param  filename  the filename as documented in the {@code export} argument, for convenience.
     * @return all input streams.
     */
    public static synchronized Supplier<InputStream> getRenameDefinitionsFiles(final boolean export, final String filename) {
        final Iterator<TypeRegistration> services = services().iterator();
        return () -> {
            synchronized (TypeRegistration.class) {
                while (services.hasNext()) {
                    final Optional<InputStream> next = services.next().getRenameDefinitionsFile(export, filename);
                    if (next.isPresent()) return next.get();
                }
                return null;
            }
        };
    }

    /**
     * Returns the {@code TypeRegistration} instances.
     * Must be invoked in a synchronized block.
     */
    private static ServiceLoader<TypeRegistration> services() {
        ServiceLoader<TypeRegistration> s = services;
        if (s == null) {
            try {
                s = ServiceLoader.load(TypeRegistration.class, Reflect.getContextClassLoader());
            } catch (SecurityException e) {
                Reflect.log(TypeRegistration.class, "services", e);
                s = ServiceLoader.load(TypeRegistration.class);
            }
            services = s;
            DelayedExecutor.schedule(new DelayedRunnable(1, TimeUnit.MINUTES) {
                @Override public void run() {services = null;}
            });
        }
        return s;
    }

    /**
     * Scans the module path for root classes to put in JAXB context and for converters to those classes.
     * Those lists are determined dynamically from the SIS modules found on the module path.
     * This method does nothing if this class already has all required information.
     *
     * <p>The list of converters is cached (that result is stored in {@link #converters}) while the list
     * of root classes in JAXB context is not cached. So the information about whether this method needs
     * to fetch the list of root classes or not must be specified by the {@code getTypes} argument.</p>
     *
     * @param  getTypes  whether to get the root classes to put in JAXB context (may cause class loading).
     * @return if {@code getTypes} was {@code true}, the root classes to be bound in {@code JAXBContext}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<?>[] load(final boolean getTypes) {
        final ArrayList<Class<?>>              types  = new ArrayList<>();
        final ArrayList<UnaryOperator<Object>> toImpl = new ArrayList<>();
        for (final TypeRegistration t : services()) {
            if (getTypes) t.getTypes(types);
            final UnaryOperator<Object> c = t.beforeMarshal();
            if (c != null) toImpl.add(c);
        }
        converters = toImpl.toArray(UnaryOperator[]::new);
        return types.toArray(Class<?>[]::new);
    }

    /**
     * Returns the shared {@code JAXBContext} for the set of classes returned by {@link #load(boolean)}.
     * Note that the {@code JAXBContext} class is thread safe, but the {@code Marshaller},
     * {@code Unmarshaller}, and {@code Validator} classes are not thread safe.
     *
     * @return the shared JAXB context.
     * @throws JAXBException if an error occurred while creating the JAXB context.
     */
    public static synchronized JAXBContext getSharedContext() throws JAXBException {
        final Reference<JAXBContext> c = context;
        if (c != null) {
            final JAXBContext instance = c.get();
            if (instance != null) {
                return instance;
            }
        }
        final JAXBContext instance = JAXBContext.newInstance(load(true));
        context = new WeakReference<>(instance);
        return instance;
    }

    /**
     * Completes the given properties with an entry for {@link #ROOT_ADAPTERS} if not already present.
     * If a {@code ROOT_ADAPTERS} entry is already present, then the map is returned unchanged.
     *
     * <p>This method stores a reference to the internal {@code TypeRegistration[]} array in a copy of the given map.
     * <strong>That array shall not be modified.</strong> This method is currently for Apache SIS internal usage only,
     * because the {@code TypeRegistration} class is not part of public API. However if we add this functionality in a
     * future SIS release (probably as an interface rather than exposing {@code TypeRegistration} itself), then we may
     * consider removing this method.</p>
     *
     * @param  properties  the properties to complete.
     * @return the given properties with the {@link #ROOT_ADAPTERS} entry added.
     */
    public static Map<String,?> getPrivateInfo(final Map<String,?> properties) {
        if (properties != null && properties.containsKey(ROOT_ADAPTERS)) {
            return properties;
        }
        UnaryOperator<Object>[] c;
        synchronized (TypeRegistration.class) {
            c = converters;
            if (c == null) {
                load(false);
                c = converters;
            }
        }
        if (properties == null) {
            return Map.of(ROOT_ADAPTERS, c);
        }
        final Map<String,Object> copy = new HashMap<>(properties);
        copy.put(ROOT_ADAPTERS, c);
        return copy;
    }
}
