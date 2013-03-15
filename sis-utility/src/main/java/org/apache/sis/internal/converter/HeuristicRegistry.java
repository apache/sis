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
import org.opengis.util.CodeList;
import net.jcip.annotations.ThreadSafe;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ObjectConverter;


/**
 * A {@link ConverterRegistry} which applies heuristic rules in addition of the explicitly
 * registered converters. Those heuristic rules are provided in a separated class in order
 * to keep the {@link ConverterRegistry} class "pure", and concentrate all arbitrary
 * decisions in this single class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.02)
 * @version 0.3
 * @module
 */
@ThreadSafe
public final class HeuristicRegistry extends ConverterRegistry {
    /**
     * The default system-wide instance. This register is initialized with conversions between
     * some basic Java and SIS objects, like conversions between {@link java.util.Date} and
     * {@link java.lang.Long}. Those conversions are defined for the lifetime of the JVM.
     *
     * <p>If a temporary set of converters is desired, a new instance of {@code ConverterRegistry}
     * should be created explicitly instead.</p>
     *
     * {@section Adding system-wide converters}
     * Applications can add system-wide custom providers either by explicit call to the
     * {@link #register(ObjectConverter)} method on the system converter.
     */
    public static final ConverterRegistry SYSTEM = new HeuristicRegistry();

    /**
     * Creates an initially empty set of object converters. The heuristic
     * rules apply right away, even if no converter have been registered yet.
     */
    private HeuristicRegistry() {
    }

    /**
     * Create dynamically the converters for a few special cases.
     * This method is invoked only the first time that a new pair of source and target classes is
     * requested. Then, the value returned by this method will be cached for future invocations.
     *
     * <p>Some (not all) special cases are:</p>
     * <ul>
     *   <li>If the source class is {@link CharSequence}, tries to delegate to an other
     *       converter accepting {@link String} sources.</li>
     *   <li>If the source and target types are numbers, generates a {@link NumberConverter}
     *       on the fly.</li>
     *   <li>If the target type is a code list, generate the converter on-the-fly.
     *       We do not register every code lists in advance because there is too
     *       many of them, and a generic code is available for all of them.</li>
     * </ul>
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
        if (targetClass == Date.class) {
            final ObjectConverter<Date, S> candidate = DateConverter.getInstance(sourceClass);
            if (candidate != null) {
                return (ObjectConverter<S,T>) candidate.inverse();
            }
        }
        if (targetClass == String.class) {
            final ObjectConverter<String, S> candidate = StringConverter.getInstance(sourceClass);
            if (candidate != null) {
                return (ObjectConverter<S,T>) candidate.inverse();
            }
            if (CodeList.class.isAssignableFrom(sourceClass)) {
                return findExact(targetClass, sourceClass).inverse();
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
            return (ObjectConverter<S,T>) new CharSequenceConverter<>(
                    targetClass, find(String.class, targetClass));
        }
        /*
         * From String to various kind of objects.
         */
        if (sourceClass == String.class) {
            final ObjectConverter<String, T> candidate = StringConverter.getInstance(targetClass);
            if (candidate != null) {
                return (ObjectConverter<S,T>) candidate;
            }
            if (CodeList.class.isAssignableFrom(targetClass)) {
                return (ObjectConverter<S,T>) new StringConverter.CodeList<>(
                        targetClass.asSubclass(CodeList.class));
            }
        }
        /*
         * From Number to other kinds of Number.
         */
        if (sourceClass == Number.class || isSupportedNumber(sourceClass)) {
            if (isSupportedNumber(targetClass)) {
                return (ObjectConverter<S,T>) new NumberConverter<>(
                        sourceClass.asSubclass(Number.class),
                        targetClass.asSubclass(Number.class));
            }
            if (targetClass == Comparable.class) {
                return (ObjectConverter<S,T>) new NumberConverter.Comparable<>(
                        sourceClass.asSubclass(Number.class));
            }
        }
        /*
         * From Date to various kind of objects.
         */
        if (sourceClass == Date.class) {
            final ObjectConverter<Date, T> candidate = DateConverter.getInstance(targetClass);
            if (candidate != null) {
                return (ObjectConverter<S,T>) candidate;
            }
        }
        /*
         * Various kind of paths (Path, File, URL, URI).
         */
        final ObjectConverter<S,T> p = PathConverter.getInstance(sourceClass, targetClass);
        if (p != null) {
            return p;
        }
        /*
         * From various objects to String.
         */
        if (targetClass == String.class) {
            return (ObjectConverter<S,T>) new ObjectToString<>(sourceClass, null);
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
