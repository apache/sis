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
import org.opengis.annotation.UML;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Modules;


/**
 * Information about an Apache SIS metadata standard implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class StandardImplementation extends MetadataStandard {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 855786625369724248L;

    /**
     * The root packages for metadata implementations, or {@code null} if none.
     * If non-null, then this string must ends with a trailing {@code "."}.
     */
    private final String implementationPackage;

    /**
     * The prefixes that implementation classes may have.
     * The most common prefixes should be first, since the prefixes will be tried in that order.
     */
    private final String[] prefix;

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
     *
     * <div class="note"><b>Implementation note:</b>
     * In the particular case of {@code Class} keys, {@code IdentityHashMap} and {@code HashMap} have identical
     * behavior since {@code Class} is final and does not override the {@code equals(Object)} and {@code hashCode()}
     * methods. The {@code IdentityHashMap} Javadoc claims that it is faster than the regular {@code HashMap}.
     * But maybe the most interesting property is that it allocates less objects since {@code IdentityHashMap}
     * implementation doesn't need the chain of objects created by {@code HashMap}.</div>
     */
    private final transient Map<Class<?>,Class<?>> implementations; // written by reflection on deserialization.

    /**
     * Creates a new instance working on implementation of interfaces defined in the
     * specified package. This constructor is used only for the pre-defined constants.
     *
     * @param citation              The title of the standard.
     * @param interfacePackage      The root package for metadata interfaces, with a trailing {@code '.'}.
     * @param implementationPackage The root package for metadata implementations. with a trailing {@code '.'}.
     * @param prefix                The prefix of implementation class. This array is not cloned.
     * @param acronyms              An array of (full text, acronyms) pairs. This array is not cloned.
     * @param dependencies          The dependencies to other metadata standards, or {@code null} if none.
     */
    StandardImplementation(final String citation, final String interfacePackage, final String implementationPackage,
            final String[] prefix, final String[] acronyms, final MetadataStandard[] dependencies)
    {
        super(citation, interfacePackage, dependencies);
        this.implementationPackage = implementationPackage;
        this.prefix                = prefix;
        this.acronyms              = acronyms;
        this.implementations       = new IdentityHashMap<Class<?>,Class<?>>();
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
     * @param  <T>  The compile-time {@code type}.
     * @param  type The interface, typically from the {@code org.opengis.metadata} package.
     * @return The implementation class, or {@code null} if none.
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
                     * Try to insert a prefix in front of the class name, until a match is found.
                     */
                    final int prefixPosition = buffer.lastIndexOf(".") + 1;
                    int length = 0;
                    for (final String p : prefix) {
                        classname = buffer.replace(prefixPosition, prefixPosition + length, p).toString();
                        try {
                            candidate = Class.forName(classname);
                        } catch (ClassNotFoundException e) {
                            Logging.recoverableException(Logging.getLogger(Modules.METADATA),
                                    MetadataStandard.class, "getImplementation", e);
                            length = p.length();
                            continue;
                        }
                        if (candidate.isAnnotationPresent(Deprecated.class)) {
                            // Skip deprecated implementations.
                            candidate = candidate.getSuperclass();
                            if (!type.isAssignableFrom(candidate) || candidate.isAnnotationPresent(Deprecated.class)) {
                                length = p.length();
                                continue;
                            }
                        }
                        implementations.put(type, candidate);
                        return candidate.asSubclass(type);
                    }
                    implementations.put(type, Void.TYPE); // Marker for "class not found".
                }
            }
        }
        return null;
    }

    /**
     * Invoked on deserialization. Returns one of the pre-existing constants if possible.
     */
    Object readResolve() {
        if (ISO_19111.citation.equals(citation)) return ISO_19111;
        if (ISO_19115.citation.equals(citation)) return ISO_19115;
        /*
         * Following should not occurs, unless we are deserializing an instance created by a
         * newer version of the Apache SIS library. The newer version could contains constants
         * not yet declared in this older SIS version, so we have to use this instance.
         */
        setMapForField(StandardImplementation.class, "implementations");
        return this;
    }
}
