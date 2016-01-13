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
package org.apache.sis.util;

import java.util.Locale;


/**
 * Interface of classes for which each instance is configured for a particular locale.
 * Those classes are often parsers or formatters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public interface Localized {
    /**
     * Returns the locale of the implemented service. Some implementations may return
     * {@code null} if no locale is explicitly defined. The meaning of null locale is
     * implementation-dependent, but typical interpretations are:
     *
     * <ul>
     *   <li>A synonymous of the {@linkplain Locale#getDefault() system default locale}.</li>
     *   <li>A synonymous of {@link Locale#ROOT} for an "unlocalized" service. For example the
     *       service may format numbers using {@link Double#toString(double)} instead than
     *       {@link java.text.NumberFormat}.</li>
     * </ul>
     *
     * Implementations are encouraged to return a non-null value in every cases.
     * Nevertheless client codes should be prepared to receive null values.
     *
     * @return The locale, or {@code null} if not explicitly defined.
     */
    Locale getLocale();
}
