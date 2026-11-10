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

import javax.xml.stream.XMLStreamException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.style.se1.ColorMap;
import org.apache.sis.style.se1.Description;
import org.apache.sis.style.se1.OverlapBehavior;


/**
 * Writer of the symbology elements of Symbology Encoding 1.1.
 * In this version, the symbology elements are in the SE namespace.
 * The filters and expressions, including the functions defined by Symbology Encoding,
 * are written by a {@link FilterWriter110}.
 * This writer is used by {@link SLDWriter110}.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class SEWriter110 extends SEWriter {
    /**
     * The writer of Filter Encoding 1.1 filters and expressions.
     */
    private final FilterWriter110 filterWriter;

    /**
     * Creates a new writer sharing the cursor and the state of the given writer.
     *
     * @param  parent  the style writer which contains this symbology writer.
     */
    SEWriter110(final AbstractStaxWriter parent) {
        super(parent);
        filterWriter = new FilterWriter110(this);
    }

    /**
     * Returns the writer of Filter Encoding 1.1 filters and expressions.
     */
    @Override
    FilterWriter110 filterWriter() {
        return filterWriter;
    }

    /**
     * Returns the version written by this writer, which is SE 1.1.0.
     */
    @Override
    StyleVersion getVersion() {
        return StyleVersion.V1_1_0;
    }

    /**
     * Returns the SE namespace, which contains the symbology elements in version 1.1.
     */
    @Override
    String ns() {
        return SETags.NAMESPACE;
    }

    /**
     * Returns {@code "SvgParameter"}.
     */
    @Override
    String parameterElement() {
        return SETags.SVG_PARAMETER;
    }

    /**
     * Writes the description as a {@code <se:Description>} element.
     */
    @Override
    void writeDescription(final Description<?> description, final boolean allowedIn10) throws XMLStreamException {
        if (description != null) {
            final String title   = asText(description.getTitle());
            final String summary = asText(description.getAbstract());
            if (title != null || summary != null) {
                start(SETags.NAMESPACE, SETags.DESCRIPTION);
                text(SETags.NAMESPACE, SETags.TITLE,    title);
                text(SETags.NAMESPACE, SETags.ABSTRACT, summary);
                end();
            }
        }
    }

    /**
     * Writes the color map with its {@code <se:Categorize>} or {@code <se:Interpolate>} function.
     */
    @Override
    <R> void writeColorMap(final ColorMap<R> colorMap) throws XMLStreamException, DataStoreException {
        start(SETags.NAMESPACE, SETags.COLOR_MAP);
        if (colorMap.getFunction().isPresent()) {
            filterWriter.writeExpression(colorMap.getFunction().get());
        }
        end();
    }

    /**
     * Writes the overlap behavior as a text, for example {@code <se:OverlapBehavior>AVERAGE</se:OverlapBehavior>}.
     */
    @Override
    void writeOverlapBehavior(final OverlapBehavior behavior) throws XMLStreamException {
        text(SETags.NAMESPACE, SETags.OVERLAP_BEHAVIOR, behavior.name());
    }
}
