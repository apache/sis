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
package org.apache.sis.metadata;

import java.util.Map;
import java.util.IdentityHashMap;
import java.util.logging.Logger;
import java.io.ObjectStreamException;
import org.opengis.annotation.UML;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Modules;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.annotation.Classifier;
import org.opengis.annotation.Stereotype;


/**
 * Information about an Apache SIS metadata standard implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class StandardImplementation extends MetadataStandard {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 855786625369724248L;

    /**
     * The logger for metadata.
     */
    static final Logger LOGGER = Logger.getLogger(Modules.METADATA);

    /**
     * The root packages for metadata implementations, or {@code null} if none.
     * If non-null, then this string must ends with a trailing {@code "."}.
     */
    private final String implementationPackage;

    /**
     * The acronyms that implementation classes may have, or {@code null} if none. If non-null,
     * then this array shall contain (<var>full text</var>, <var>acronym</var>) pairs. The full
     * text shall appear to the end of the class name, otherwise it is not replaced. This is
     * necessary in order to avoid the replacement of {@code "DefaultCoordinateSystemAxis"} by
     * {@code "DefaultCSAxis"}.
     */
    private final String[] acronyms;

    /**
     * Implementations for a given interface, computed when first needed then cached.
     * Consider this field as final. It is not final only for {@link #readResolve()} purpose.
     *
     * <h4>Implementation note</h4>
     * In the particular case of {@code Class} keys, {@code IdentityHashMap} and {@code HashMap} have identical
     * behavior since {@code Class} is final and does not override the {@code equals(Object)} and {@code hashCode()}
     * methods. The {@code IdentityHashMap} Javadoc claims that it is faster than the regular {@code HashMap}.
     * But maybe the most interesting property is that it allocates less objects since {@code IdentityHashMap}
     * implementation doesn't need the chain of objects created by {@code HashMap}.
     */
    private transient Map<Class<?>,Class<?>> implementations;

    /**
     * Creates a new instance working on implementation of interfaces defined in the specified package.
     * This constructor is used only for the predefined constants.
     *
     * @param citation               the title of the standard.
     * @param interfacePackage       the root package for metadata interfaces, with a trailing {@code '.'}.
     * @param implementationPackage  the root package for metadata implementations. with a trailing {@code '.'}.
     * @param acronyms               an array of (full text, acronyms) pairs. This array is not cloned.
     * @param dependencies           the dependencies to other metadata standards, or {@code null} if none.
     */
    StandardImplementation(final String citation, final String interfacePackage, final String implementationPackage,
            final String[] acronyms, final MetadataStandard... dependencies)
    {
        super(citation, interfacePackage, dependencies);
        this.implementationPackage = implementationPackage;
        this.acronyms              = acronyms;
        this.implementations       = new IdentityHashMap<>();
    }

    /**
     * Returns {@code true} if the given type is conceptually abstract.
     * The given type is usually an interface, so here "abstract" cannot be in the Java sense.
     * If this method cannot find information about whether the given type is abstract,
     * then this method conservatively returns {@code false}.
     */
    private static boolean isAbstract(final Class<?> type) {
        final Classifier c = type.getAnnotation(Classifier.class);
        return (c != null) && c.value() == Stereotype.ABSTRACT;
    }

    /**
     * Accepts Apache SIS implementation classes as "pseudo-interfaces" if they are annotated with {@link UML}.
     * We use this feature for example in the transition from ISO 19115:2003 to ISO 19115:2014, when new API is
     * defined in Apache SIS but not yet available in GeoAPI interfaces.
     */
    @Override
    boolean isPendingAPI(final Class<?> type) {
        return type.getName().startsWith(implementationPackage) && type.isAnnotationPresent(UML.class);
    }

    /**
     * Returns the implementation class for the given interface, or {@code null} if none.
     * This class uses heuristic rules based on naming conventions.
     *
     * @param  <T>   the compile-time {@code type}.
     * @param  type  the interface, typically from the {@code org.opengis.metadata} package.
     * @return the implementation class, or {@code null} if none.
     */
    @Override
    public <T> Class<? extends T> getImplementation(final Class<T> type) {
        /*
         * We require the type to be an interface in order to exclude
         * CodeLists, Enums and Exceptions.
         */
        if (type != null && type.isInterface()) {
            String classname = type.getName();
            if (isSupported(classname)) {
                synchronized (implementations) {
                    Class<?> candidate = implementations.get(type);
                    if (candidate != null) {
                        return (candidate != Void.TYPE) ? candidate.asSubclass(type) : null;
                    }
                    /*
                     * Prepares a buffer with a copy of the class name in which the interface
                     * package has been replaced by the implementation package, and some text
                     * have been replaced by their acronym (if any).
                     */
                    final StringBuilder buffer = new StringBuilder(implementationPackage)
                            .append(classname, interfacePackage.length(), classname.length());
                    if (acronyms != null) {
                        for (int i=0; i<acronyms.length; i+=2) {
                            final String acronym = acronyms[i];
                            if (CharSequences.endsWith(buffer, acronym, false)) {
                                buffer.setLength(buffer.length() - acronym.length());
                                buffer.append(acronyms[i+1]);
                                break;
                            }
                        }
                    }
                    /*
                     * Try to instantiate the implementation class.
                     */
                    final int prefixPosition = buffer.lastIndexOf(".") + 1;
                    buffer.insert(prefixPosition, isAbstract(type) ? "Abstract" : "Default");
                    classname = buffer.toString();
                    try {
                        candidate = Class.forName(classname);
                        implementations.put(type, candidate);
                        return candidate.asSubclass(type);
                    } catch (ClassNotFoundException e) {
                        Logging.recoverableException(LOGGER, MetadataStandard.class, "getImplementation", e);
                    }
                    implementations.put(type, Void.TYPE);                       // Marker for "class not found".
                }
            }
        }
        return null;
    }

    /**
     * Invoked on deserialization. Returns one of the preexisting constants if possible.
     */
    Object readResolve() throws ObjectStreamException {
        for (final MetadataStandard standard : MetadataStandard.INSTANCES) {
            if (standard.citation.equals(citation)) return standard;
        }
        /*
         * Following should not occur, unless we are deserializing an instance created by a
         * newer version of the Apache SIS library. The newer version could contain constants
         * not yet declared in this older SIS version, so we have to use this instance.
         */
        implementations = new IdentityHashMap<>();
        return this;
    }
}
