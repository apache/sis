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
package org.apache.sis.internal.referencing;

import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Utility methods for referencing WKT formatting.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class WKTUtilities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private WKTUtilities() {
    }

    /**
     * Appends the name of the given object to the formatter.
     *
     * @param object    The object from which to get the name.
     * @param formatter The formatter where to append the name.
     * @param type      The key of colors to apply if syntax colors are enabled.
     */
    public static void appendName(final IdentifiedObject object, final Formatter formatter, final ElementKind type) {
        String name = IdentifiedObjects.getName(object, formatter.getNameAuthority());
        if (name == null) {
            name = IdentifiedObjects.getName(object, null);
            if (name == null) {
                name = Vocabulary.getResources(formatter.getLocale()).getString(Vocabulary.Keys.Unnamed);
            }
        }
        formatter.append(name, (type != null) ? type : ElementKind.NAME);
    }

    /**
     * Appends a {@linkplain ParameterValue parameter} in a {@code PARAMETER[…]} element.
     * If the supplied parameter is actually a {@linkplain ParameterValueGroup parameter group},
     * all contained parameters will be flattened in a single list.
     *
     * @param parameter The parameter to append to the WKT, or {@code null} if none.
     * @param formatter The formatter where to append the parameter.
     */
    public static void append(GeneralParameterValue parameter, final Formatter formatter) {
        if (parameter instanceof ParameterValueGroup) {
            boolean first = true;
            for (final GeneralParameterValue param : ((ParameterValueGroup) parameter).values()) {
                if (first) {
                    formatter.newLine();
                    first = false;
                }
                append(param, formatter);
            }
        }
        if (parameter instanceof ParameterValue<?>) {
            if (!(parameter instanceof FormattableObject)) {
                parameter = new DefaultParameterValue<>((ParameterValue<?>) parameter);
            }
            formatter.append((FormattableObject) parameter);
            formatter.newLine();
        }
    }
}
