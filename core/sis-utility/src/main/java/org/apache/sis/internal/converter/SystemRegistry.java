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
package org.apache.sis.internal.converter;

import java.util.Date;
import java.util.ServiceLoader;
import org.opengis.util.CodeList;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.system.Modules;


/**
 * The Apache SIS system-wide {@link ConverterRegistry}.
 * This class serves two purposes:
 *
 * <ul>
 *   <li>Fetch the list of converters from the content of all
 *       {@code META-INF/services/org.apache.sis.util.converter.ObjectConverter} files found on the classpath.
 *       The intend is to allow other modules to register their own converters.</li>
 *
 *   <li>Apply heuristic rules in addition to the explicitly registered converters.
 *       Those heuristic rules are provided in a separated class in order to keep the
 *       {@link ConverterRegistry} class a little bit more "pure", and concentrate
 *       most arbitrary decisions in this single class.</li>
 * </ul>
 *
 * When using {@code SystemRegistry}, new converters may "automagically" appear as a consequence
 * of the above-cited heuristic rules. This differs from the {@link ConverterRegistry} behavior,
 * where only registered converters are used.
 *
 * <div class="section">Thread safety</div>
 * The same {@link #INSTANCE} can be safely used by many threads without synchronization on the part of the caller.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class SystemRegistry extends ConverterRegistry {
    /**
     * The default system-wide instance. This register is initialized with conversions between
     * some basic Java and SIS objects, like conversions between {@link java.util.Date} and
     * {@link java.lang.Long}. Those conversions are defined for the lifetime of the JVM.
     *
     * <p>If a temporary set of converters is desired, a new instance of {@code ConverterRegistry}
     * should be created explicitly instead.</p>
     *
     * <p>See the package javadoc for information about how applications can add
     * custom system-wide converters.</p>
     */
    public static final ConverterRegistry INSTANCE = new SystemRegistry();
    static {
        /*
         * Force reloading of META-INF/services files if the classpath changed,
         * since the set of reachable META-INF/services files may have changed.
         * If any converters were registered by explicit calls to the 'register' method,
         * then those converters are lost. This is of concern only for applications using
         * a modularization framework like OSGi. See package javadoc for more information.
         */
        SystemListener.add(new SystemListener(Modules.UTILITIES) {
            @Override protected void classpathChanged() {
                INSTANCE.clear();
            }
        });
    }

    /**
     * Creates an initially empty set of object converters. The heuristic
     * rules apply right away, even if no converter have been registered yet.
     */
    private SystemRegistry() {
    }

    /**
     * Invoked when this {@code ConverterRegistry} needs to be initialized. This method
     * is automatically invoked the first time that {@link #register(ObjectConverter)}
     * or {@link #find(Class, Class)} is invoked.
     *
     * <p>The default implementation is equivalent to the following code
     * (see the package javadoc for more information):</p>
     *
     * {@preformat java
     *     ClassLoader loader = getClass().getClassLoader();
     *     for (ObjectConverter<?,?> converter : ServiceLoader.load(ObjectConverter.class, loader)) {
     *         register(converter);
     *     }
     * }
     */
    @Override
    protected void initialize() {
        for (ObjectConverter<?,?> converter : ServiceLoader.load(ObjectConverter.class, getClass().getClassLoader())) {
            if (converter instanceof SystemConverter<?,?>) {
                converter = ((SystemConverter<?,?>) converter).unique();
            }
            register(converter);
        }
    }

    /**
     * Returns {@code true} if we should look for the inverse converter of the given target class.
     * For example if no converter is explicitely registered from {@code Float} to {@code String},
     * we can look for the converter from {@code String} to {@code Float}, then fetch its inverse.
     *
     * <p>We allow this operation only for a few types which are needed for the way SIS converters
     * are defined in this internal package.</p>
     */
    private static boolean tryInverse(final Class<?> targetClass) {
        return (targetClass == String.class) || (targetClass == Date.class);
    }

    /**
     * Create dynamically the converters for a few special cases.
     * This method is invoked only the first time that a new pair of source and target classes is
     * requested. Then, the value returned by this method will be cached for future invocations.
     *
     * <p>Some (not all) special cases are:</p>
     * <ul>
     *   <li>If the source class is {@link CharSequence}, tries to delegate to an other
     *       converter accepting {@link String} sources.</li>
     *   <li>If the source and target types are numbers, generates a {@link NumberConverter}
     *       on the fly.</li>
     *   <li>If the target type is a code list, generate the converter on-the-fly.
     *       We do not register every code lists in advance because there is too
     *       many of them, and a generic code is available for all of them.</li>
     * </ul>
     *
     * @return A newly generated converter from the specified source class to the target class,
     *         or {@code null} if none.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    protected <S,T> ObjectConverter<S,T> createConverter(final Class<S> sourceClass, final Class<T> targetClass) {
        /*
         * Most methods in this package are provided in such a way that we need to look at the
         * source class first, then at the target class. However before to perform those usual
         * checks, we need to check for the inverse conversions below. The reason is that some
         * of them are identity conversion (e.g. going from java.sql.Date to java.util.Date),
         * but we don't want to leave the creation of those identity tranforms to the parent
         * class because it doesn't know what are the inverse of those inverse conversions
         * (i.e. the direct conversions). For example if the conversion from java.sql.Date
         * to java.util.Date was created by the super class, that conversion would not contain
         * an inverse conversion from java.util.Date to java.sql.Date.
         */
        if (tryInverse(targetClass) && !tryInverse(sourceClass)) { // The ! is for preventing infinite recursivity.
            try {
                return findExact(targetClass, sourceClass).inverse();
            } catch (UnconvertibleObjectException e) {
                // Ignore. The code below may succeed.
            }
        }
        /*
         * After we checked for the above special-cases, check for the identity converter.
         * We need this check before to continue because some of the code below may create
         * a "real" converter for what was actually an identity operation.
         */
        final ObjectConverter<S,T> identity = super.createConverter(sourceClass, targetClass);
        if (identity != null) {
            return identity;
        }
        /*
         * From CharSequence to anything. Note that this check shall be done only after we have
         * determined that the conversion is not the identity conversion (i.e. the target is not
         * CharSequence or Object), otherwise this converter would apply useless toString().
         */
        if (sourceClass == CharSequence.class) {
            return (ObjectConverter<S,T>) new CharSequenceConverter<T>( // More checks in JDK7 branch.
                    targetClass, find(String.class, targetClass));
        }
        /*
         * From String to CodeList or Enum.
         */
        if (sourceClass == String.class) {
            if (CodeList.class.isAssignableFrom(targetClass)) {
                return (ObjectConverter<S,T>) new StringConverter.CodeList( // More checks in JDK7 branch.
                        targetClass.asSubclass(CodeList.class));
            }
            if (targetClass.isEnum()) {
                return (ObjectConverter<S,T>) new StringConverter.Enum( // More checks in JDK7 branch.
                        targetClass.asSubclass(Enum.class));
            }
        }
        /*
         * From Number to other kinds of Number.
         */
        if (sourceClass == Number.class || isSupportedNumber(sourceClass)) {
            if (isSupportedNumber(targetClass)) {
                return (ObjectConverter<S,T>) new NumberConverter( // More checks in JDK7 branch.
                        sourceClass.asSubclass(Number.class),
                        targetClass.asSubclass(Number.class));
            }
            if (targetClass == Comparable.class) {
                return (ObjectConverter<S,T>) new NumberConverter.Comparable( // More checks in JDK7 branch.
                        sourceClass.asSubclass(Number.class));
            }
        }
        /*
         * From various objects to String.
         */
        if (targetClass == String.class) {
            return (ObjectConverter<S,T>) new ObjectToString<S>(sourceClass, null);
        }
        return null;
    }

    /**
     * Returns {@code true} if the given type is one of the types supported
     * by {@link NumberConverter}.
     */
    private static boolean isSupportedNumber(final Class<?> type) {
        final int code = Numbers.getEnumConstant(type);
        return (code >= Numbers.BYTE && code <= Numbers.BIG_DECIMAL);
    }
}
