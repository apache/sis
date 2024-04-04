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
package org.apache.sis.referencing;

import java.util.Locale;
import java.io.Serializable;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.util.AbstractInternationalString;


/**
 * Name of an identified object in specified locale.
 * This class is serializable if the identified object is serializable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
final class DisplayName extends AbstractInternationalString implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8061637252049031838L;

    /**
     * The object for which to provide a display name.
     */
    @SuppressWarnings("serial")             // Most Apache SIS implementations are serializable.
    private final IdentifiedObject object;

    /**
     * Creates a new display name.
     *
     * @param object  the object for which to provide a display name.
     */
    DisplayName(final IdentifiedObject object) {
        this.object = object;
    }

    /**
     * Returns the display name in the given locale.
     *
     * @param  locale  the desired locale for the string to be returned.
     * @return the string in the given locale if available, or in an
     *         implementation-dependent fallback locale otherwise.
     */
    @Override
    public String toString(final Locale locale) {
        return IdentifiedObjects.getDisplayName(object, locale);
    }

    /**
     * Returns a hash code value for this international string.
     */
    @Override
    public int hashCode() {
        return object.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Compares this international string with the given object for equality.
     *
     * @param  obj  the object to compare with this international string.
     * @return whether the two object are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof DisplayName) && object.equals(((DisplayName) obj).object);
    }
}
