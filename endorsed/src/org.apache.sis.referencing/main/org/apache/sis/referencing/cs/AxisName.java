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
package org.apache.sis.referencing.cs;

import java.util.Locale;
import java.util.regex.Pattern;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Pattern of a {@link CoordinateSystemAxis} together with the resource to use for localized axis name.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class AxisName {
    /**
     * Regular expressions for matching axis names.
     */
    private static final AxisName[] KEYWORDS = {
        new AxisName(".*\\blatitudes?\\b.*",  Vocabulary.Keys.Latitude),
        new AxisName(".*\\blongitudes?\\b.*", Vocabulary.Keys.Longitude),
        new AxisName(".*\\bheights?\\b.*",    Vocabulary.Keys.Height),
        new AxisName(".*\\btimes?\\b.*",      Vocabulary.Keys.Time)
    };

    /**
     * The pattern to compare with an axis name.
     */
    private final Pattern pattern;

    /**
     * Key of localized word for the axis.
     */
    private final short word;

    /**
     * Creates a new pattern for axis name.
     */
    private AxisName(final String regex, final short word) {
        pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        this.word = word;
    }

    /**
     * Returns a short (if possible) localized name for the given axis. This method replaces
     * names such as "Geodetic latitude" or "Geocentric latitude" by a simple "Latitude" word.
     * This method can be used for example in column or row headers when the context is known
     * and the space is rare.
     *
     * @param  axis    the axis for which to get a short label.
     * @param  locale  desired locale for the label, or {@code null} for the default.
     * @return a relatively short axis label, in the desired locale if possible.
     */
    static String find(final CoordinateSystemAxis axis, final Locale locale) {
        final String name = IdentifiedObjects.getName(axis, null);
        if (name != null) {
            for (final AxisName keyword : KEYWORDS) {
                if (keyword.pattern.matcher(name).matches()) {
                    return Vocabulary.forLocale(locale).getString(keyword.word);
                }
            }
        }
        return IdentifiedObjects.getDisplayName(axis, locale);
    }
}
