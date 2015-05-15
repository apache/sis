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

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;


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
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.xml.MarshallerPool
 */
public abstract class TypeRegistration {
    /**
     * The JAXB context, or {@code null} if not yet created or if the classpath changed.
     */
    private static Reference<JAXBContext> context;
    static {
        SystemListener.add(new SystemListener(Modules.UTILITIES) {
            @Override protected void classpathChanged() {
                synchronized (TypeRegistration.class) {
                    context = null;
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
     * Adds to the given collection every types that should be given to
     * the initial JAXB context.
     *
     * @param addTo The collection in which to add new types.
     */
    public abstract void getTypes(final Collection<Class<?>> addTo);

    /**
     * Returns the root classes of SIS objects to be marshalled by default.
     * Those classes can be given as the last argument to the {@code MarshallerPool}
     * constructors, in order to bound a default set of classes with {@code JAXBContext}.
     *
     * <p>The list of classes is determined dynamically from the SIS modules found on
     * the classpath.</p>
     *
     * @return The default set of classes to be bound to the {@code JAXBContext}.
     */
    private static Class<?>[] defaultClassesToBeBound() {
        /*
         * Implementation note: do not keep the ServiceLoader in static field because:
         *
         * 1) It would cache the TypeRegistration instances, which are not needed after this method call.
         * 2) The ClassLoader between different invocations may be different in an OSGi context.
         */
        final ArrayList<Class<?>> types = new ArrayList<Class<?>>();
        for (final TypeRegistration t : ServiceLoader.load(TypeRegistration.class)) {
            t.getTypes(types);
        }
        return types.toArray(new Class<?>[types.size()]);
    }

    /**
     * Returns the shared {@code JAXBContext} for the set of {@link #defaultClassesToBeBound()}.
     * Note that the {@code JAXBContext} class is thread safe, but the {@code Marshaller},
     * {@code Unmarshaller}, and {@code Validator} classes are not thread safe.
     *
     * @return The shared JAXB context.
     * @throws JAXBException If an error occurred while creating the JAXB context.
     */
    public static synchronized JAXBContext getSharedContext() throws JAXBException {
        final Reference<JAXBContext> c = context;
        if (c != null) {
            final JAXBContext instance = c.get();
            if (instance != null) {
                return instance;
            }
        }
        final JAXBContext instance = JAXBContext.newInstance(defaultClassesToBeBound());
        context = new WeakReference<JAXBContext>(instance);
        return instance;
    }
}
