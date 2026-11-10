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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.gml.GMLVersion;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.function.ChangeCase;
import org.apache.sis.style.se1.function.Concatenate;
import org.apache.sis.style.se1.function.FormatDate;
import org.apache.sis.style.se1.function.FormatNumber;
import org.apache.sis.style.se1.function.Interpolate;
import org.apache.sis.style.se1.function.Recode;
import org.apache.sis.style.se1.function.StringLength;
import org.apache.sis.style.se1.function.StringPosition;
import org.apache.sis.style.se1.function.Substring;
import org.apache.sis.style.se1.function.Trim;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;


/**
 * Writer of OGC Filter Encoding 1.1 filters and expressions, with geometries encoded in GML 3.1.
 * This writer also writes the functions defined by Symbology Encoding 1.1 with their specific
 * elements in the SE namespace, since those functions can be used everywhere an expression is expected.
 * This writer is used by {@link SEWriter110}.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class FilterWriter110 extends FilterWriter {
    /**
     * Creates a new writer sharing the cursor and the state of the given writer.
     *
     * @param  parent  the symbology writer which contains this filter writer.
     */
    FilterWriter110(final AbstractStaxWriter parent) {
        super(parent);
    }

    /**
     * Returns the version written by this writer, which is FE 1.1.0 as used by SLD 1.1.0 and SE 1.1.0.
     */
    @Override
    StyleVersion getVersion() {
        return StyleVersion.V1_1_0;
    }

    /**
     * Returns GML 3.1, which is the GML version used by Filter Encoding 1.1.
     */
    @Override
    GMLVersion gmlVersion() {
        return GMLVersion.V3_1;
    }

    /**
     * Returns the keyword used in Symbology Encoding for the given enumeration value.
     * For example {@code FRONT_TO_BACK} is mapped to {@code "frontToBack"}.
     */
    private static String keyword(final Enum<?> value) {
        final var buffer = new StringBuilder();
        boolean upper = false;
        for (final char c : value.name().toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                buffer.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the text of the given literal, or {@code null} if absent.
     * A warning is emitted if the expression is not a literal.
     *
     * @param  expression  the expression to format.
     * @param  attribute   name of the attribute where the text will be written, for the warning message.
     */
    private String literal(final Optional<? extends Expression<?,?>> expression, final String attribute) {
        if (expression.isPresent()) {
            if (expression.get() instanceof Literal<?,?> literal) {
                return format(literal.getValue());
            }
            warning("Only literal values can be written in the \"" + attribute + "\" attribute.");
        }
        return null;
    }

    /**
     * Writes the functions defined by Symbology Encoding with their specific XML elements.
     * Other functions are left to the caller, which writes them as {@code <ogc:Function>}.
     */
    @Override
    <R> boolean writeSymbologyFunction(final Expression<R,?> expression) throws XMLStreamException, DataStoreException {
        final String ns = SETags.NAMESPACE;
        if (expression instanceof Categorize<R,?> f) {
            start(ns, SETags.CATEGORIZE);
            attribute(SETags.FALLBACK_VALUE, literal(f.getFallbackValue(), SETags.FALLBACK_VALUE));
            attribute(SETags.THRESHOLDS_BELONG_TO, keyword(f.getThresholdsBelongTo()));
            writeParameter(ns, SETags.LOOKUP_VALUE, f.getLookupValue());
            final List<? extends Expression<R,?>> values = f.getValues();
            final List<Expression<R,?>> thresholds = f.getThresholds();
            for (int i=0; i<values.size(); i++) {
                if (i != 0) {
                    writeParameter(ns, SETags.THRESHOLD, thresholds.get(i-1));
                }
                writeParameter(ns, SETags.VALUE, values.get(i));
            }
        } else if (expression instanceof Interpolate<?> f) {
            start(ns, SETags.INTERPOLATE);
            attribute(SETags.FALLBACK_VALUE, literal(f.getFallbackValue(), SETags.FALLBACK_VALUE));
            attribute(SETags.MODE, keyword(f.getMode()));
            if (f.getMethod().isPresent()) {
                attribute(SETags.METHOD, keyword(f.getMethod().get()));
            }
            writeParameter(ns, SETags.LOOKUP_VALUE, f.getLookupValue());
            for (final Interpolate.InterpolationPoint<?> point : f.getInterpolationPoints()) {
                start(ns, SETags.INTERPOLATION_POINT);
                writeParameter(ns, SETags.DATA,  point.getData());
                writeParameter(ns, SETags.VALUE, point.getValue());
                end();
            }
        } else if (expression instanceof Recode<?> f) {
            start(ns, SETags.RECODE);
            attribute(SETags.FALLBACK_VALUE, literal(f.getFallbackValue(), SETags.FALLBACK_VALUE));
            writeParameter(ns, SETags.LOOKUP_VALUE, f.getLookupValue());
            for (final Recode.MapItem<?> item : f.getMapItems()) {
                start(ns, SETags.MAP_ITEM);
                writeParameter(ns, SETags.DATA,  item.getData());
                writeParameter(ns, SETags.VALUE, item.getValue());
                end();
            }
        } else if (expression instanceof FormatNumber<?> f) {
            start(ns, SETags.FORMAT_NUMBER);
            attribute(SETags.DECIMAL_POINT,      literal(f.getDecimalPoint(),      SETags.DECIMAL_POINT));
            attribute(SETags.GROUPING_SEPARATOR, literal(f.getGroupingSeparator(), SETags.GROUPING_SEPARATOR));
            writeParameter(ns, SETags.NUMERIC_VALUE,    f.getNumericValue());
            writeParameter(ns, SETags.PATTERN,          f.getPattern());
            writeParameter(ns, SETags.NEGATIVE_PATTERN, f.getNegativePattern().orElse(null));
        } else if (expression instanceof FormatDate<?> f) {
            start(ns, SETags.FORMAT_DATE);
            writeParameter(ns, SETags.DATE_VALUE, f.getDateValue());
            writeParameter(ns, SETags.PATTERN,    f.getPattern().orElse(null));
        } else if (expression instanceof Substring<?> f) {
            start(ns, SETags.SUBSTRING);
            writeParameter(ns, SETags.STRING_VALUE, f.getStringValue());
            writeParameter(ns, SETags.POSITION,     f.getPosition().orElse(null));
            writeParameter(ns, SETags.LENGTH,       f.getLength().orElse(null));
        } else if (expression instanceof Concatenate<?> f) {
            start(ns, SETags.CONCATENATE);
            for (final Expression<?,?> value : f.getStringValues()) {
                writeParameter(ns, SETags.STRING_VALUE, value);
            }
        } else if (expression instanceof ChangeCase<?> f) {
            start(ns, SETags.CHANGE_CASE);
            attribute(SETags.DIRECTION, keyword(f.getDirection()));
            writeParameter(ns, SETags.STRING_VALUE, f.getStringValue());
        } else if (expression instanceof Trim<?> f) {
            start(ns, SETags.TRIM);
            attribute(SETags.STRIP_OFF_POSITION, keyword(f.getStripOffPosition()));
            attribute(SETags.STRIP_OFF_CHAR, literal(f.getStripOffChar(), SETags.STRIP_OFF_CHAR));
            writeParameter(ns, SETags.STRING_VALUE, f.getStringValue());
        } else if (expression instanceof StringPosition<?> f) {
            start(ns, SETags.STRING_POSITION);
            attribute(SETags.SEARCH_DIRECTION, keyword(f.getSearchDirection()));
            writeParameter(ns, SETags.LOOKUP_STRING, f.getLookupString());
            writeParameter(ns, SETags.STRING_VALUE,  f.getStringValue());
        } else if (expression instanceof StringLength<?> f) {
            start(ns, SETags.STRING_LENGTH);
            writeParameter(ns, SETags.STRING_VALUE, f.getStringValue());
        } else {
            return false;
        }
        end();
        return true;
    }
}
