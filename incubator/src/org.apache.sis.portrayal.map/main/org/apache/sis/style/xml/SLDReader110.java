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

import javax.xml.stream.XMLStreamReader;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory;


/**
 * Reader of Styled Layer Descriptor 1.1 documents.
 * The symbology elements are read by a {@link SEReader110}.
 * This reader is lenient: it also accepts the encoding of version 1.0,
 * as documented in {@link AbstractStaxReader}.
 * This reader is used by the {@link StyleReader} facade, including when the version
 * of the document cannot be determined (for example a document containing only a filter).
 *
 * @author  Johann Sorel (Geomatys)
 */
final class SLDReader110 extends SLDReader {
    /**
     * The reader of the symbology elements of Symbology Encoding 1.1.
     */
    private final SEReader110 seReader;

    /**
     * Creates a new reader using the given StAX cursor.
     *
     * @param  reader         the StAX cursor to read from.
     * @param  filterFactory  the factory to use for creating filters and expressions.
     */
    SLDReader110(final XMLStreamReader reader, final FilterFactory<Feature,Object,Object> filterFactory) {
        super(reader, filterFactory);
        seReader = new SEReader110(this);
    }

    /**
     * Returns the reader of the symbology elements of Symbology Encoding 1.1.
     */
    @Override
    SEReader110 seReader() {
        return seReader;
    }
}
