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
package org.apache.sis.internal.jaxb;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.system.DefaultFactories;


/**
 * Declares the classes of objects to be marshalled using a default {@code MarshallerPool}.
 * This class is not strictly necessary for marshalling a SIS object using JAXB, but makes
 * the job easier by allowing {@code MarshallerPool} to configure the JAXB context automatically.
 * To allow such automatic configuration, modules must declare instances of this interface in the
 * following file:
 *
 * {@preformat text
 *     META-INF/services/org.org.apache.sis.internal.jaxb.TypeRegistration
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see org.apache.sis.xml.MarshallerPool
 *
 * @since 0.3
 * @module
 */
public abstract class TypeRegistration {
    /**
     * Undocumented (for now) marshaller property for specifying conversions to apply on root objects
     * before marshalling. Conversions are applied by the {@link #toImplementation(Object)} method.
     *
     * @see #addDefaultRootAdapters(Map)
     */
    public static final String ROOT_ADAPTERS = "org.apache.sis.xml.rootAdapters";

    /**
     * The JAXB context, or {@code null} if not yet created or if the classpath changed.
     *
     * @see #getSharedContext()
     */
    private static Reference<JAXBContext> context;

    /**
     * The {@link TypeRegistration} instances found on the classpath for which the
     * {@link #toImplementation(Object)} method has been overridden.
     *
     * @see #addDefaultRootAdapters(Map)
     */
    private static TypeRegistration[] converters;

    /**
     * Forces reloading of JAXB context and converters if the classpath changes.
     */
    static {
        SystemListener.add(new SystemListener(Modules.UTILITIES) {
            @Override protected void classpathChanged() {
                synchronized (TypeRegistration.class) {
                    context    = null;
                    converters = null;
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
     * then the {@link #canMarshalInterfaces()} method should be overridden.
     *
     * @param  addTo  the collection in which to add new types.
     */
    protected abstract void getTypes(final Collection<Class<?>> addTo);

    /**
     * Returns {@code true} if the module can also marshal arbitrary implementation of some interfaces.
     * If this method returns {@code true}, then the {@link #toImplementation(Object)} method shall be
     * overridden.
     *
     * @return whether the module can also marshal arbitrary implementation of some interfaces.
     *
     * @since 0.8
     */
    protected boolean canMarshalInterfaces() {
        return false;
    }

    /**
     * If the given value needs to be converted before marshalling, apply the conversion now.
     * Otherwise returns {@code null} if the value class is not recognized, or {@code value}
     * if the class is recognized but the value does not need to be changed.
     *
     * <p>Subclasses that override this method will typically perform an {@code instanceof} check, then
     * invoke one of the {@code castOrCopy(…)} static methods defined in various Apache SIS classes.</p>
     *
     * <p>This method is invoked only if {@link #canMarshalInterfaces()} returns {@code true}.</p>
     *
     * @param  value  the value to convert before marshalling.
     * @return the value to marshall; or {@code null} if this method does not recognize the value class.
     * @throws JAXBException if an error occurred while converting the given object.
     *
     * @since 0.8
     */
    public Object toImplementation(final Object value) throws JAXBException {
        return null;
    }

    /**
     * Scans the classpath for root classes to put in JAXB context and for converters to those classes.
     * Those lists are determined dynamically from the SIS modules found on the classpath.
     * The list of root classes is created only if the {@code getTypes} argument is {@code true}.
     *
     * @param  getTypes  whether to get the root classes to put in JAXB context (may cause class loading).
     * @return if {@code getTypes} was {@code true}, the root classes to be bound in {@code JAXBContext}.
     */
    private static Class<?>[] load(final boolean getTypes) {
        /*
         * Implementation note: do not keep the ServiceLoader in static field because:
         *
         * 1) It would cache more TypeRegistration instances than needed for this method call.
         * 2) The ClassLoader between different invocations may be different in an OSGi context.
         */
        final ArrayList<Class<?>> types = new ArrayList<>();
        final ArrayList<TypeRegistration> toImpl = (converters == null) ? new ArrayList<>() : null;
        if (toImpl != null || getTypes) {
            for (final TypeRegistration t : DefaultFactories.createServiceLoader(TypeRegistration.class)) {
                if (getTypes) {
                    t.getTypes(types);
                }
                if (toImpl != null && t.canMarshalInterfaces()) {
                    toImpl.add(t);
                }
            }
            if (toImpl != null) {
                converters = toImpl.toArray(new TypeRegistration[toImpl.size()]);
            }
        }
        return types.toArray(new Class<?>[types.size()]);
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
     * <p>This method store a direct reference to the internal {@code TypeRegistration[]} array in the given map.
     * <strong>That array shall not be modified.</strong> This method is currently for Apache SIS internal usage only,
     * because the {@code TypeRegistration} class is not part of public API. However if we add this functionality in a
     * future SIS release (probably as an interface rather than exposing {@code TypeRegistration} itself), then we may
     * consider removing this method.</p>
     *
     * @param  properties  the properties to complete.
     * @return the given properties with the {@link #ROOT_ADAPTERS} entry added.
     *
     * @since 0.8
     */
    public static Map<String,?> addDefaultRootAdapters(final Map<String,?> properties) {
        if (properties != null && properties.containsKey(ROOT_ADAPTERS)) {
            return properties;
        }
        TypeRegistration[] c;
        synchronized (TypeRegistration.class) {
            c = converters;
            if (c == null) {
                load(false);
                c = converters;
            }
        }
        if (properties == null) {
            return Collections.singletonMap(ROOT_ADAPTERS, c);
        }
        final Map<String,Object> copy = new HashMap<>(properties);
        copy.put(ROOT_ADAPTERS, c);
        return copy;
    }
}
