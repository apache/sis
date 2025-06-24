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
package org.apache.sis.storage.netcdf.base;

import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.system.Modules;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.storage.netcdf.internal.Resources;


/**
 * Base class of netCDF dimension, variable or axis or grid.
 * All those objects share in common a {@link #getName()} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class NamedElement {
    /**
     * For subclasses constructors.
     */
    protected NamedElement() {
    }

    /**
     * Returns the dimension, variable or attribute name.
     * Note that dimensions in HDF5 files may be unnamed.
     *
     * @return the name of this element, or {@code null} if unnamed.
     */
    public abstract String getName();

    /**
     * If this element is member of a group, returns the name of that group.
     * Otherwise returns {@code null}. The default implementation always returns {@code null}.
     *
     * @return name of the group which contains this element, or {@code null}.
     */
    public String getGroupName() {
        return null;
    }

    /**
     * Creates a name for a {@code NamedElement} made of other components.
     * Current implementation returns a separated list of component names.
     *
     * @param  components  the component of the named object.
     * @param  count       number of valid elements in the {@code components} array.
     * @param  delimiter   the separator between component names.
     * @return a name for an object composed of the given components.
     */
    protected static String listNames(final NamedElement[] components, final int count, final String delimiter) {
        final StringJoiner joiner = new StringJoiner(delimiter);
        for (int i=0; i<count; i++) {
            joiner.add(components[i].getName());
        }
        return joiner.toString();
    }

    /**
     * Returns {@code true} if the given names are considered equals for the purpose of netCDF decoder.
     * Two names are considered similar if they are equal ignoring case and characters that are not valid
     * for an Unicode identifier.
     *
     * @param  s1  the first characters sequence to compare, or {@code null}.
     * @param  s2  the second characters sequence to compare, or {@code null}.
     * @return whether the two characters sequences are considered similar names.
     */
    protected static boolean similar(final CharSequence s1, final CharSequence s2) {
        return CharSequences.equalsFiltered(s1, s2, Characters.Filter.UNICODE_IDENTIFIER, true);
    }

    /**
     * Reports a warning to the specified listeners.
     *
     * @param  listeners  the listeners where to report the warning.
     * @param  caller     the caller class to report, preferably a public class.
     * @param  method     the caller method to report, preferable a public method.
     * @param  exception  the exception that occurred, or {@code null} if none.
     * @param  resources  the resources bundle for {@code key} and {@code arguments}, or {@code null} for {@link Resources}.
     * @param  key        one of the {@code resources} constants (by default, a {@link Resources.Keys} constant).
     * @param  arguments  values to be formatted in the {@link java.text.MessageFormat} pattern.
     */
    static void warning(final StoreListeners listeners, final Class<?> caller, final String method,
            final Exception exception, IndexedResourceBundle resources, final short key, final Object... arguments)
    {
        if (resources == null) {
            resources = Resources.forLocale(listeners.getLocale());
        }
        final LogRecord record = resources.createLogRecord(Level.WARNING, key, arguments);
        record.setLoggerName(Modules.NETCDF);
        record.setSourceClassName(caller.getCanonicalName());
        record.setSourceMethodName(method);
        if (exception != null) {
            record.setThrown(exception);
        }
        listeners.warning(record, StoreUtilities.removeStackTraceInLogs());
    }

    /**
     * Returns a string representation of this element. Current implementation returns only the element class and name.
     *
     * @return string representation of this element for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.bracket(getClass().getSimpleName(), getName());
    }

    /*
     * Do not override `equals(Object)` and `hashCode()`. Some subclasses are
     * used in `HashSet` and the identity comparison is well suited for them.
     * For example, `Variable` is used as keys in `GridMapping.forVariable(…)`.
     */
}
