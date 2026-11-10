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
package org.apache.sis.style.xml;


/**
 * Reader of the symbology elements of Symbology Encoding 1.1.
 * In this version, the symbology elements are in the SE namespace.
 * The filters and expressions are read by a {@link FilterReader110}.
 * This reader is lenient: it also accepts the encoding of Styled Layer Descriptor 1.0,
 * as documented in {@link AbstractStaxReader}.
 * This reader is used by {@link SLDReader110}.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class SEReader110 extends SEReader {
    /**
     * The reader of Filter Encoding 1.1 filters and expressions.
     */
    private final FilterReader110 filterReader;

    /**
     * Creates a new reader sharing the cursor, the factories and the warnings of the given reader.
     *
     * @param  parent  the style reader which contains this symbology reader.
     */
    SEReader110(final AbstractStaxReader parent) {
        super(parent);
        filterReader = new FilterReader110(this);
    }

    /**
     * Returns the reader of Filter Encoding 1.1 filters and expressions.
     */
    @Override
    FilterReader110 filterReader() {
        return filterReader;
    }
}
