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

import java.util.Locale;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.opengis.util.InternationalString;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.ObjectConverter;


/**
 * Handles conversions from arbitrary objects to {@link String}. This converter is
 * suitable to any object for which the {@link #toString()} method is sufficient.
 *
 * @param <S> The source type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@Immutable
class ObjectToString<S> extends InjectiveConverter<S,String> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1460382215827540172L;

    /**
     * The shared instance for international strings.
     */
    static final ObjectToString<InternationalString> I18N = new ObjectToString<>(InternationalString.class, StringConverter.InternationalString.INSTANCE);

    /**
     * The shared instance for numbers. {@link ConverterRegistry} needs only the {@link Number}
     * type. Other subtypes are created only by {@code StringConverter.Foo.inverse()} methods.
     */
    static final ObjectToString<Number> NUMBER = new ObjectToString<>(Number.class, StringConverter.Number.INSTANCE);

    /**
     * The shared instance for booleans.
     */
    static final ObjectToString<Boolean> BOOLEAN = new ObjectToString<>(Boolean.class, StringConverter.Boolean.INSTANCE);

    /**
     * The shared instance for URI.
     * This converter does not encode the string, i.e. the conversion is performed with
     * the {@link URI#toString()} method rather than {@link URI#toASCIIString()}. We do
     * that in order to avoid too many transformations if we convert back and forward to
     * {@code String}.
     */
    static final ObjectToString<File> FILE = new ObjectToString<>(File.class, StringConverter.File.INSTANCE);

    /**
     * The shared instance for URI.
     * This converter does not encode the string, i.e. the conversion is performed with
     * the {@link URI#toString()} method rather than {@link URI#toASCIIString()}. We do
     * that in order to avoid too many transformations if we convert back and forward to
     * {@code String}.
     */
    static final ObjectToString<URI> URI = new ObjectToString<>(URI.class, StringConverter.URI.INSTANCE);

    /**
     * The shared instance for URL.
     * This converter does not encode the string. We do that in order to avoid too many
     * transformations if we convert back and forward to {@code String}.
     */
    static final ObjectToString<URL> URL = new ObjectToString<>(URL.class, StringConverter.URL.INSTANCE);

    /**
     * The shared instance for locales.
     */
    static final ObjectToString<Locale> LOCALE = new ObjectToString<>(Locale.class, StringConverter.Locale.INSTANCE);

    /**
     * The shared instance for character sets.
     */
    static final ObjectToString<java.nio.charset.Charset> CHARSET = new Charset();

    /**
     * The source class.
     */
    private final Class<S> sourceClass;

    /**
     * The inverse converter specified at construction time.
     */
    private final ObjectConverter<String, S> inverse;

    /**
     * Creates a new converter from the given type of objects to {@code String} instances.
     */
    ObjectToString(final Class<S> sourceClass, final ObjectConverter<String, S> inverse) {
        this.sourceClass = sourceClass;
        this.inverse = inverse;
    }

    /**
     * Returns the source class given at construction time.
     */
    @Override
    public final Class<S> getSourceClass() {
        return sourceClass;
    }

    /**
     * Returns the destination type (same for all instances of this class).
     */
    @Override
    public final Class<String> getTargetClass() {
        return String.class;
    }

    /**
     * Converts the given number to a string.
     */
    @Override
    public String convert(final S source) {
        return (source != null) ? source.toString() : null;
    }

    /**
     * Returns the inverse given at construction time.
     */
    @Override
    public final ObjectConverter<String, S> inverse() {
        return inverse;
    }

    /**
     * Returns the singleton instance on deserialization.
     */
    final Object readResolve() throws ObjectStreamException {
        if (sourceClass == InternationalString.class) return I18N;
        if (sourceClass == Number .class) return NUMBER;
        if (sourceClass == Boolean.class) return BOOLEAN;
        if (sourceClass == File   .class) return FILE;
        if (sourceClass == URI    .class) return URI;
        if (sourceClass == URL    .class) return URL;
        if (sourceClass == Locale .class) return LOCALE;
        if (sourceClass == java.nio.charset.Charset.class) return CHARSET;
        return this;
    }


    /**
     * Specialized instance for {@link java.nio.charset.Charset}.
     * This class invokes {@java.nio.charset.Charset#name()} instead than {@code toString()}.
     */
    private static final class Charset extends ObjectToString<java.nio.charset.Charset> {
        private static final long serialVersionUID = 1313285261794842777L;

        Charset() {
            super(java.nio.charset.Charset.class, StringConverter.Charset.INSTANCE);
        }

        @Override
        public String convert(final java.nio.charset.Charset source) {
            return (source != null) ? source.name() : null;
        }
    }
}
