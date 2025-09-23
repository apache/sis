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
package org.apache.sis.test.mock;

import java.util.Set;
import java.util.Collection;
import java.util.Locale;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.lan.LocaleAdapter;
import org.apache.sis.metadata.simple.SimpleMetadata;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Map;
import java.util.Collections;
import java.nio.charset.Charset;


/**
 * A dummy implementation of {@link org.opengis.metadata.Metadata} with minimal XML (un)marshalling capability.
 * Used for testing marshalling of legacy ISO 19139:2007 attributes.
 * Contrarily to {@link org.apache.sis.metadata.iso.DefaultMetadata}, this mock does not set automatically
 * the {@link org.apache.sis.xml.XML#LOCALE} attribute according the {@code <mdb:defaultLocale>} element.
 * So this mock makes easier to test localization aspects without the interference of automatic mechanism.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlRootElement(name = "MD_Metadata", namespace = Namespaces.MDB)
public final class MetadataMock extends SimpleMetadata {
    /**
     * The language used for documenting metadata.
     */
    @XmlElement(namespace = LegacyNamespaces.GMD)
    @XmlJavaTypeAdapter(LocaleAdapter.class)
    private Locale language;

    /**
     * Creates an initially empty metadata.
     * This constructor is required by JAXB.
     */
    public MetadataMock() {
    }

    /**
     * Creates an initially empty metadata with the given language.
     * Callers are free to assign new value to the {@link #language} field directly.
     *
     * @param  language  the initial {@link #language} value (can be {@code null}).
     */
    public MetadataMock(final Locale language) {
        this.language = language;
    }

    /**
     * Returns {@link #language} in a singleton map or an empty map.
     *
     * @return {@link #language}
     */
    @Override
    public Map<Locale,Charset> getLocalesAndCharsets() {
        return (language != null) ? Collections.singletonMap(language, null) : Collections.emptyMap();
    }

    /**
     * Returns {@link #language} in a singleton set or an empty set.
     *
     * @return {@link #language}
     */
    @Override
    @Deprecated
    public Collection<Locale> getLanguages() {
        return (language != null) ? Set.of(language) : Set.of();
    }

    /**
     * Returns {@link #language}.
     *
     * @return {@link #language}
     */
    @Override
    @Deprecated
    public Locale getLanguage() {
        return language;
    }
}
