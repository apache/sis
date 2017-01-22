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

import java.util.Locale;
import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.Metadata;
import org.apache.sis.internal.jaxb.gmd.LocaleAdapter;
import org.apache.sis.internal.simple.SimpleMetadata;
import org.apache.sis.xml.Namespaces;


/**
 * A dummy implementation of {@link Metadata} with minimal XML (un)marshalling capability.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
@XmlRootElement(name = "MD_Metadata", namespace = Namespaces.GMD)
public final strictfp class MetadataMock extends SimpleMetadata {
    /**
     * The language used for documenting metadata.
     */
    @XmlElement(namespace = Namespaces.GMD)
    @XmlJavaTypeAdapter(LocaleAdapter.class)
    public Locale language;

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
     * Returns {@link #language} in a singleton set or an empty set.
     *
     * @return {@link #language}
     */
    @Override
    public Collection<Locale> getLanguages() {
        return (language != null) ? Collections.singleton(language) : Collections.emptySet();
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
