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
package org.apache.sis.util.internal;

import java.util.Locale;
import java.text.Format;
import java.text.NumberFormat;
import java.text.MessageFormat;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * A message format which adjust automatically the number of fraction digits needed for formatting numbers.
 * Callers need to invoke {@link #configure(Object[])} before to invoke any {@code format(…)} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})               // Not intended to be serialized.
public final class AutoMessageFormat extends MessageFormat {
    /**
     * Formats that may need to be configured before to write a value, or {@code null} if none.
     * May contain null elements. This information is used for adjusting the number of fraction digits.
     */
    private transient NumberFormat[] formatsToConfigure;

    /**
     * Whether the {@link #formatsToConfigure} needs to be updated.
     */
    private transient boolean update;

    /**
     * Creates a new message for the default locale.
     *
     * @param pattern  the pattern for this message format.
     */
    public AutoMessageFormat(final String pattern) {
        super(pattern);
    }

    /**
     * Creates a new message for the given locale.
     *
     * @param pattern  the pattern for this message format.
     * @param locale   the locale for this message format.
     */
    public AutoMessageFormat(final String pattern, final Locale locale) {
        super(pattern, locale);
    }

    /**
     * Modifies the pattern used by this message format.
     *
     * @param pattern the new pattern for this message format
     */
    @Override
    public void applyPattern(final String pattern) {
        super.applyPattern(pattern);
        update = true;
    }

    /**
     * Configures the number of fraction digits in the formats used by this {@code MessageFormat}.
     * This method can work only for parameters declared as {@code "{#,number}"} in the message pattern.
     *
     * @param  arguments  the argument to be given to {@link #format(Object)}.
     */
    public void configure(final Object[] arguments) {
        if (update) {
            formatsToConfigure = null;
            final Format[] fc = getFormatsByArgumentIndex();
            for (int i=fc.length; --i >= 0;) {
                final Format c = fc[i];
                if (c instanceof NumberFormat) {
                    if (formatsToConfigure == null) {
                        formatsToConfigure = new NumberFormat[i+1];
                    }
                    formatsToConfigure[i] = (NumberFormat) c;
                }
            }
        }
        if (formatsToConfigure != null) {
            for (int i=Math.min(formatsToConfigure.length, arguments.length); --i >= 0;) {
                final NumberFormat f = formatsToConfigure[i];
                if (f != null) {
                    final Object value = arguments[i];
                    int p = 3;                              // Default number of fraction digits.
                    if (value instanceof Number) {
                        final double n = ((Number) value).doubleValue();
                        if (!Double.isNaN(n)) {
                            p = -1 - Numerics.toExp10(Math.getExponent(Math.ulp(n)));
                        }
                    }
                    f.setMaximumFractionDigits(Math.min(p, 16));
                }
            }
        }
    }
}
