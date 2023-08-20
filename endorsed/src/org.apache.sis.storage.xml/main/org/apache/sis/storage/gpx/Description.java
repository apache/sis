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
package org.apache.sis.storage.gpx;

import java.util.Locale;
import java.util.ResourceBundle;
import org.apache.sis.util.ResourceInternationalString;


/**
 * A localized description.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
final class Description extends ResourceInternationalString {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7091720253401192418L;

    /**
     * The name of the resource bundle, as a fully qualified class name.
     * This value is given at construction time and cannot be {@code null}.
     * It most be a resource in the same module than this class.
     */
    private final String resources;

    /**
     * Creates a new international string from the specified key.
     *
     * @param resources  the name of the resource bundle, as a fully qualified class name.
     * @param key        the key for the resource to fetch.
     */
    Description(final String resources, final String key) {
        super(key);
        this.resources = resources;
    }

    /**
     * Returns the resource bundle for the given locale.
     */
    @Override
    protected ResourceBundle getBundle(final Locale locale) {
        return ResourceBundle.getBundle(resources, locale, Description.class.getClassLoader());
    }
}
