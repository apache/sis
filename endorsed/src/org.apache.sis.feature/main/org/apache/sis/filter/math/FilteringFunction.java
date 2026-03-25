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
package org.apache.sis.filter.math;

import org.opengis.util.CodeList;


/**
 * A function viewed as a code list.
 * This is used only when {@link Predicate} is used as a filter.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FilteringFunction extends CodeList<FilteringFunction> {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -3980988422378881835L;

    /**
     * Creates a new filtering function.
     *
     * @param  name  name of the function.
     */
    private FilteringFunction(final String name) {
        super(name);
    }

    /**
     * Returns the list of {@code FilteringFunction}s.
     *
     * @return the list of codes declared in the current JVM.
     */
    @Override
    public FilteringFunction[] family() {
        return values(FilteringFunction.class);
    }

    /**
     * Returns the function name that matches the given string, or a new one if none match it.
     *
     * @param  code  the name of the code to fetch or to create.
     * @return a code matching the given name, or {@code null}.
     */
    public static FilteringFunction valueOf(final String code) {
        return valueOf(FilteringFunction.class, code, FilteringFunction::new).get();
    }
}
