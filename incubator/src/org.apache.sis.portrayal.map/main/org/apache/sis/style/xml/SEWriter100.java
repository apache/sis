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

import java.awt.Color;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.style.se1.ColorMap;
import org.apache.sis.style.se1.Description;
import org.apache.sis.style.se1.OverlapBehavior;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.function.Interpolate;
import org.apache.sis.style.se1.function.Recode;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;


/**
 * Writer of the symbology elements of Styled Layer Descriptor 1.0.
 * In this version, the symbology elements are in the SLD namespace.
 * The filters and expressions are written by a {@link FilterWriter100}.
 * Elements which exist only in version 1.1 are not written, and a warning is emitted.
 * This writer is used by {@link SLDWriter100}.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class SEWriter100 extends SEWriter {
    /**
     * The writer of Filter Encoding 1.0 filters and expressions.
     */
    private final FilterWriter100 filterWriter;

    /**
     * Creates a new writer sharing the cursor and the state of the given writer.
     *
     * @param  parent  the style writer which contains this symbology writer.
     */
    SEWriter100(final AbstractStaxWriter parent) {
        super(parent);
        filterWriter = new FilterWriter100(this);
    }

    /**
     * Returns the writer of Filter Encoding 1.0 filters and expressions.
     */
    @Override
    FilterWriter100 filterWriter() {
        return filterWriter;
    }

    /**
     * Returns the version written by this writer, which is SLD 1.0.0.
     */
    @Override
    StyleVersion getVersion() {
        return StyleVersion.V1_0_0;
    }

    /**
     * Returns the SLD namespace, which contains the symbology elements in version 1.0.
     */
    @Override
    String ns() {
        return SLDTags.NAMESPACE;
    }

    /**
     * Returns {@code "CssParameter"}.
     */
    @Override
    String parameterElement() {
        return SLDTags.CSS_PARAMETER;
    }

    /**
     * Writes the description as {@code <Title>} and {@code <Abstract>} elements, if allowed.
     */
    @Override
    void writeDescription(final Description<?> description, final boolean allowedIn10) throws XMLStreamException {
        if (description != null) {
            final String title   = asText(description.getTitle());
            final String summary = asText(description.getAbstract());
            if (title != null || summary != null) {
                if (allowedIn10) {
                    text(SLDTags.NAMESPACE, SETags.TITLE,    title);
                    text(SLDTags.NAMESPACE, SETags.ABSTRACT, summary);
                } else {
                    unsupported(SETags.DESCRIPTION);
                }
            }
        }
    }

    /**
     * Writes the color map as a list of {@code <ColorMapEntry>} elements.
     * An interpolation is written as a list of entries. Categories and recoding are written
     * with the {@code type="intervals"} or {@code type="values"} attribute, which is a common
     * extension of SLD 1.0.
     */
    @Override
    <R> void writeColorMap(final ColorMap<R> colorMap) throws XMLStreamException {
        Expression<R,?> function = colorMap.getFunction().orElse(null);
        String type = null;
        if (function instanceof Categorize) {
            type = SLDTags.INTERVALS;
        } else if (function instanceof Recode) {
            type = SLDTags.VALUES;
        } else if (function != null && !(function instanceof Interpolate)) {
            warning("Unsupported color map function: " + FilterWriter.functionName(function) + ", ignored.");
            function = null;
        }
        start(SLDTags.NAMESPACE, SETags.COLOR_MAP);
        if (type != null) {
            attribute(SLDTags.TYPE, type);
            warning("The color map type \"" + type + "\" is an extension of SLD 1.0.");
        }
        if (function instanceof Interpolate<?> f) {
            for (final Interpolate.InterpolationPoint<?> point : f.getInterpolationPoints()) {
                writeColorMapEntry(literalValue(point.getData()), point.getValue());
            }
        } else if (function instanceof Categorize<R,?> f) {
            final List<? extends Expression<R,?>> values = f.getValues();
            final List<Expression<R,?>> thresholds = f.getThresholds();
            final int n = thresholds.size();
            for (int i=0; i<n; i++) {
                writeColorMapEntry(literalValue(thresholds.get(i)), values.get(i));
            }
            final Expression<R,?> last = values.get(n);
            if (!(literalValue(last) instanceof Color c && c.getAlpha() == 0)) {
                writeColorMapEntry(Double.POSITIVE_INFINITY, last);
            }
        } else if (function instanceof Recode<?> f) {
            for (final Recode.MapItem<?> item : f.getMapItems()) {
                writeColorMapEntry(literalValue(item.getData()), item.getValue());
            }
        }
        end();
    }

    /**
     * Returns the value of the given expression if it is a literal, or {@code null} otherwise.
     */
    private static Object literalValue(final Expression<?,?> expression) {
        return (expression instanceof Literal<?,?> literal) ? literal.getValue() : null;
    }

    /**
     * Writes a {@code <ColorMapEntry>} element.
     *
     * @param  quantity  the value in the raster data.
     * @param  color     the color associated to the quantity.
     */
    private void writeColorMapEntry(final Object quantity, final Expression<?,?> color) throws XMLStreamException {
        Object value = literalValue(color);
        if (value instanceof CharSequence text) try {
            value = AbstractStaxReader.parseColor(text.toString().trim());
        } catch (NumberFormatException e) {
            value = null;
        }
        double q = Double.NaN;
        if (quantity instanceof Number n) {
            q = n.doubleValue();
        } else if (quantity instanceof CharSequence text) try {
            q = Double.parseDouble(text.toString().trim());
        } catch (NumberFormatException e) {
            // Leave the quantity to NaN.
        }
        if (!(value instanceof Color c) || Double.isNaN(q)) {
            warning("Only colors and quantities given by literals can be written in a <" + SETags.COLOR_MAP + ">.");
            return;
        }
        empty(SLDTags.NAMESPACE, SLDTags.COLOR_MAP_ENTRY);
        attribute(SLDTags.COLOR, format(new Color(c.getRGB() & 0xFFFFFF)));
        final int alpha = c.getAlpha();
        if (alpha != 0xFF) {
            attribute(SLDTags.OPACITY, format(alpha / 255d));
        }
        attribute(SLDTags.QUANTITY, format(q));
    }

    /**
     * Writes the overlap behavior as an empty child element, for example {@code <AVERAGE/>}.
     */
    @Override
    void writeOverlapBehavior(final OverlapBehavior behavior) throws XMLStreamException {
        start(SLDTags.NAMESPACE, SETags.OVERLAP_BEHAVIOR);
        empty(SLDTags.NAMESPACE, behavior.name());
        end();
    }
}
