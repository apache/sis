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
package org.apache.sis.util.collection;

import org.opengis.util.InternationalString;
import org.apache.sis.util.type.SimpleInternationalString;


/**
 * A trivial implementation of {@link TableColumn} for {@link String} values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class StringColumn implements TableColumn<String> {
    /**
     * The column header.
     */
    private final InternationalString header;

    /**
     * Creates a new column for the given header.
     */
    StringColumn(final String header) {
        this.header = new SimpleInternationalString(header);
    }

    /**
     * Returns the type of column values, which is {@link String}.
     */
    @Override
    public Class<String> getElementType() {
        return String.class;
    }

    /**
     * Returns the column header.
     */
    @Override
    public InternationalString getHeader() {
        return header;
    }

    /**
     * Returns a string representation o this column for debugging purpose.
     */
    @Override
    public String toString() {
        return header.toString();
    }
}
