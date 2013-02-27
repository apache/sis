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
package org.apache.sis.metadata;

import java.util.Comparator;
import java.lang.reflect.Method;
import net.jcip.annotations.Immutable;

import org.opengis.annotation.UML;
import org.opengis.annotation.Obligation;


/**
 * The comparator for sorting method order. This comparator puts mandatory methods first,
 * which is necessary for reducing the risk of ambiguity in {@link PropertyTree#parse}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@Immutable
final class PropertyComparator implements Comparator<Method> {
    /**
     * The singleton instance.
     */
    static final Comparator<Method> INSTANCE = new PropertyComparator();

    /**
     * Do not allow instantiation of this class, except for the singleton.
     */
    private PropertyComparator() {
    }

    /**
     * Compares the given methods for order.
     */
    @Override
    public int compare(final Method m1, final Method m2) {
        final UML a1 = m1.getAnnotation(UML.class);
        final UML a2 = m2.getAnnotation(UML.class);
        if (a1 != null) {
            if (a2 == null) return +1;       // Sort annotated elements first.
            int c = order(a1) - order(a2);   // Mandatory elements must be first.
            if (c == 0) {
                // Fallback on alphabetical order.
                c = a1.identifier().compareToIgnoreCase(a2.identifier());
            }
            return c;
        } else if (a2 != null) {
            return -1; // Sort annotated elements first.
        }
        // Fallback on alphabetical order.
        return m1.getName().compareToIgnoreCase(m2.getName());
    }

    /**
     * Returns a higher number for obligation which should be first.
     */
    private int order(final UML uml) {
        final Obligation obligation = uml.obligation();
        if (obligation != null) {
            switch (obligation) {
                case MANDATORY:   return 1;
                case CONDITIONAL: return 2;
                case OPTIONAL:    return 3;
                case FORBIDDEN:   return 4;
            }
        }
        return 5;
    }
}
