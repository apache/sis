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
package org.apache.sis.util.iso;

import org.opengis.util.CodeList;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters.Filter;


/**
 * The filters used by {@link Types#forCodeName(Class, String, boolean)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class CodeListFilter implements CodeList.Filter {
    /**
     * The name to compare during filtering operation.
     */
    private final String codename;

    /**
     * {@code true} if {@link CodeList#valueOf} is allowed to create new code lists.
     */
    private final boolean canCreate;

    /**
     * Creates a new filter for the specified code name.
     */
    CodeListFilter(final String codename, final boolean canCreate) {
        this.codename  = codename;
        this.canCreate = canCreate;
    }

    /**
     * Returns the name of the code to create, or {@code null} if no new code list shall be created.
     */
    @Override
    public String codename() {
        return canCreate ? codename : null;
    }

    /**
     * Returns {@code true} if the given code matches the name we are looking for.
     */
    @Override
    public boolean accept(final CodeList<?> code) {
        for (final String candidate : code.names()) {
            if (accept(candidate, codename)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given names matches the name we are looking for.
     * This is defined in a separated method in order to ensure that all code paths
     * use the same criterion.
     */
    static boolean accept(final String candidate, final String codename) {
        return CharSequences.equalsFiltered(candidate, codename, Filter.LETTERS_AND_DIGITS, true);
    }
}
