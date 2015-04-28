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
package org.apache.sis.io;

import java.util.Locale;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import org.opengis.util.GenericName;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Used by {@link CompoundFormat} for formatting the name of objects of type {@link IdentifiedObject}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
final class IdentifiedObjectFormat extends Format {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -832517434580782189L;

    /**
     * The locale to use.
     */
    private final Locale locale;

    /**
     * Creates an instance for the given locale.
     */
    IdentifiedObjectFormat(final Locale locale) {
        this.locale = locale;
    }

    /**
     * Formats the given object.
     */
    @Override
    public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
        final ReferenceIdentifier identifier = ((IdentifiedObject) obj).getName();
        if (identifier == null) {
            return toAppendTo.append(Vocabulary.getResources(locale).getString(Vocabulary.Keys.Unnamed));
        }
        if (identifier instanceof GenericName) {
            // The toString() behavior is specified by the GenericName javadoc.
            return toAppendTo.append(((GenericName) identifier).toInternationalString().toString(locale));
        }
        final String code = identifier.getCode();
        String cs = identifier.getCodeSpace();
        if (cs == null || cs.isEmpty()) {
            cs = Citations.getIdentifier(identifier.getAuthority(), true);
        }
        if (cs != null) {
            toAppendTo.append(cs).append(DefaultNameSpace.DEFAULT_SEPARATOR);
        }
        return toAppendTo.append(code);
    }

    /**
     * Can not parse object only from their name.
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        throw new UnsupportedOperationException();
    }
}
