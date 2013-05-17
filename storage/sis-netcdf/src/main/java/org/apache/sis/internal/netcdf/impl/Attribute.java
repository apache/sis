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
package org.apache.sis.internal.netcdf.impl;

import org.apache.sis.util.Debug;
import org.apache.sis.util.Utilities;
import org.apache.sis.internal.jdk8.Function;


/**
 * Attribute found in a NetCDF file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class Attribute {
    /**
     * The function for obtaining the name of an attribute.
     */
    static final Function<Attribute,String> NAME_FUNCTION = new Function<Attribute,String>() {
        @Override public String apply(final Attribute value) {
            return value.name;
        }
    };

    /**
     * The attribute name.
     */
    final String name;

    /**
     * The value, either as a {@link String} or as an array of primitive type.
     */
    final Object value;

    /**
     * Creates a new attribute of the given name and value.
     */
    Attribute(final String name, final Object value) {
        this.name  = name;
        this.value = value;
    }

    /**
     * A string representation of this dimension for debugging purpose only.
     */
    @Debug
    @Override
    public String toString() {
        return name + " = " + Utilities.deepToString(value);
    }
}
