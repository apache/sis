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

import java.util.ArrayList;
import java.util.List;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.xml.stream.XMLStreamException;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.gml.GMLReader;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStoreContentException;
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
import org.apache.sis.xml.Namespaces;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Literal;
import org.opengis.filter.MatchAction;


/**
 * Base class of the readers of OGC Filter Encoding filters and expressions, including the functions
 * defined by Symbology Encoding 1.1 (which are expressions in the SE namespace).
 * The geometries of spatial operators are read with {@link GMLReader}.
 * This class contains the reading code shared by {@link FilterReader100} and {@link FilterReader110}.
 * That code is lenient: it accepts the encodings of Filter Encoding 1.0 and 1.1 in the same document.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class FilterReader extends AbstractStaxReader {
    /**
     * Name of the default geometry property, used when a {@code <BBOX>} operator does not specify a property.
     * This is the value of {@code AttributeConvention.GEOMETRY}.
     */
    static final String DEFAULT_GEOMETRY = "sis:geometry";

    /**
     * Creates a new reader sharing the cursor, the factories and the warnings of the given reader.
     *
     * @param  parent  the symbology reader which contains this filter reader.
     */
    protected FilterReader(final AbstractStaxReader parent) {
        super(parent);
    }

    /**
     * Returns whether the given namespace is a GML namespace.
     *
     * @param  ns  the namespace to test, or {@code null}.
     * @return whether the given namespace is the GML 2, 3.0, 3.1 or 3.2 namespace.
     */
    static boolean isGML(final String ns) {
        return FilterTags.GML_NAMESPACE.equals(ns) || Namespaces.GML.equals(ns);
    }

    /**
     * Reads the GML geometry at the current cursor position. The cursor shall be on the {@link #START_ELEMENT}
     * event of the geometry. After this method returns, the cursor is on the matching {@link #END_ELEMENT}.
     *
     * @return the geometry. May be an envelope (a {@link org.apache.sis.geometries.BBox}).
     */
    @SuppressWarnings("resource")       // The cursor is owned by this reader and shall not be closed by the GML reader.
    final Geometry readGeometry() throws XMLStreamException, DataStoreException {
        return new GMLReader(reader).readGeometry();
    }

    // ---------------------------------------------------------------------------------------------
    //  Expressions (Filter Encoding and Symbology Encoding)
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the expression at the current cursor position. The cursor shall be on the {@link #START_ELEMENT}
     * event of the expression. After this method returns, the cursor is on the matching {@link #END_ELEMENT}.
     *
     * @param  <R>  type of resources on which the expression applies.
     * @param  f    the factories to use.
     * @return the expression, or {@code null} if the element is not a supported expression or is empty.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    final <R> Expression<R,?> readExpression(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final FilterFactory<R,Object,Object> ff = f.filter();
        final String name = reader.getLocalName();
        switch (name) {
            case FilterTags.PROPERTY_NAME: {
                final String xpath = readText();
                return xpath.isEmpty() ? null : ff.property(xpath);
            }
            case FilterTags.LITERAL: {
                return readLiteral(f);
            }
            case FilterTags.FUNCTION: {
                final String fn = attribute(FilterTags.NAME);
                final List<Expression<R,?>> parameters = readExpressions(f);
                if (fn == null) {
                    throw new DataStoreContentException("Missing name of <" + name + ">.");
                }
                try {
                    return ff.function(fn, parameters.toArray(new Expression[parameters.size()]));
                } catch (IllegalArgumentException e) {
                    throw new DataStoreContentException("Unsupported function \"" + fn + "\".", e);
                }
            }
            case FilterTags.ADD:
            case FilterTags.SUB:
            case FilterTags.MUL:
            case FilterTags.DIV: {
                final List<Expression<R,?>> operands = readExpressions(f);
                requireCount(name, operands, 2);
                final Expression<R,Double> a = asNumber(f, operands.get(0));
                final Expression<R,Double> b = asNumber(f, operands.get(1));
                switch (name) {
                    case FilterTags.ADD: return ff.add(a, b);
                    case FilterTags.SUB: return ff.subtract(a, b);
                    case FilterTags.MUL: return ff.multiply(a, b);
                    default:             return ff.divide(a, b);
                }
            }
            default: {
                return readSymbologyFunction(f, name);
            }
        }
    }

    /**
     * Reads all child elements of the current element as expressions.
     * After this method returns, the cursor is on the {@link #END_ELEMENT} event of the current element.
     *
     * @param  <R>  type of resources on which the expressions apply.
     * @param  f    the factories to use.
     * @return the expressions, without null elements.
     */
    final <R> List<Expression<R,?>> readExpressions(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final var expressions = new ArrayList<Expression<R,?>>();
        while (nextChild()) {
            final Expression<R,?> e = readExpression(f);
            if (e != null) {
                expressions.add(e);
            }
        }
        return expressions;
    }

    /**
     * Reads a {@code <ogc:Literal>} element. The literal may contain a GML geometry.
     */
    private <R> Expression<R,?> readLiteral(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final var buffer = new StringBuilder();
        Object value = null;
        while (true) {
            switch (reader.next()) {
                case CHARACTERS:
                case CDATA:
                case SPACE: {
                    buffer.append(reader.getText());
                    break;
                }
                case START_ELEMENT: {
                    if (value == null && isGML(reader.getNamespaceURI())) {
                        value = readGeometry();
                    } else {
                        unexpected(FilterTags.LITERAL);
                    }
                    break;
                }
                case END_ELEMENT: {
                    return f.filter().literal((value != null) ? value : buffer.toString().trim());
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Reads a parameter value, which is a mixed content of texts and expressions.
     * This is the content of elements such as {@code <SvgParameter>}, {@code <Label>} or {@code <Size>}.
     * The cursor shall be on the {@link #START_ELEMENT} event of the parameter. After this method returns,
     * the cursor is on the matching {@link #END_ELEMENT} event.
     *
     * <p>If the content is a single text, it is returned as a trimmed literal. If the content is a single
     * expression, it is returned as-is. If the content is a mix of texts and expressions, they are combined
     * in a {@link Concatenate} function. Texts made only of white spaces are ignored.</p>
     *
     * @param  <R>  type of resources on which the expression applies.
     * @param  f    the factories to use.
     * @return the expression, or {@code null} if the parameter is empty.
     */
    @SuppressWarnings("unchecked")
    final <R> Expression<R,?> readParameterValue(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final var parts  = new ArrayList<Object>();     // Instances of `String` or `Expression`.
        final var buffer = new StringBuilder();
        while (true) {
            switch (reader.next()) {
                case CHARACTERS:
                case CDATA:
                case SPACE: {
                    buffer.append(reader.getText());
                    break;
                }
                case START_ELEMENT: {
                    flush(buffer, parts);
                    final Expression<R,?> e = readExpression(f);
                    if (e != null) parts.add(e);
                    break;
                }
                case END_ELEMENT: {
                    flush(buffer, parts);
                    final int n = parts.size();
                    switch (n) {
                        case 0: return null;
                        case 1: {
                            final Object part = parts.get(0);
                            return (part instanceof String s) ? f.filter().literal(s.trim()) : (Expression<R,?>) part;
                        }
                    }
                    final var expressions = new ArrayList<Expression<R,?>>(n);
                    for (int i=0; i<n; i++) {
                        final Object part = parts.get(i);
                        if (part instanceof String s) {
                            if (i == 0)   s = s.stripLeading();
                            if (i == n-1) s = s.stripTrailing();
                            expressions.add(f.filter().literal(s));
                        } else {
                            expressions.add((Expression<R,?>) part);
                        }
                    }
                    return new Concatenate<>(expressions);
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Moves the text in the given buffer to the given list of parts, unless the text is blank.
     */
    private static void flush(final StringBuilder buffer, final List<Object> parts) {
        final String text = buffer.toString();
        if (!text.isBlank()) {
            parts.add(text);
        }
        buffer.setLength(0);
    }

    /**
     * Reads a parameter value which is expected to contain a single expression.
     * Same as {@link #readParameterValue(Factories)}, but throws an exception if the value is empty.
     */
    private <R> Expression<R,?> readRequiredValue(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final String name = reader.getLocalName();
        final Expression<R,?> e = readParameterValue(f);
        if (e == null) {
            throw new DataStoreContentException("Empty <" + name + "> element.");
        }
        return e;
    }

    /**
     * Throws an exception if the given list does not have the expected number of elements.
     */
    private static void requireCount(final String name, final List<?> list, final int expected) throws DataStoreContentException {
        if (list.size() != expected) {
            throw new DataStoreContentException("Expected " + expected + " operands in <" + name + "> but found " + list.size() + '.');
        }
    }

    /**
     * Returns the given expression converted to a number if it is a literal parseable as a number,
     * or the given expression unchanged otherwise. This is used for data which are usually numbers
     * but may also be strings, such as thresholds and the keys of {@link Recode}.
     */
    private static <R> Expression<R,?> numberIfPossible(final Factories<R> f, final Expression<R,?> e) {
        if (e instanceof Literal<R,?> literal && literal.getValue() instanceof CharSequence text) {
            try {
                return f.filter().literal(Double.valueOf(text.toString().trim()));
            } catch (NumberFormatException ex) {
                // Not a number, keep the string.
            }
        }
        return e;
    }

    // ---------------------------------------------------------------------------------------------
    //  Filters
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the {@code <ogc:Filter>} element at the current cursor position. The cursor shall be on the
     * {@link #START_ELEMENT} event of the filter. After this method returns, the cursor is on the matching
     * {@link #END_ELEMENT} event.
     *
     * @param  <R>  type of resources on which the filter applies.
     * @param  f    the factories to use.
     * @return the filter. An empty filter element is interpreted as {@link Filter#include()}.
     */
    final <R> Filter<R> readFilter(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final var filters = new ArrayList<Filter<R>>();
        while (nextChild()) {
            filters.add(readOperator(f));
        }
        switch (filters.size()) {
            case 0: {
                warning("Empty <Filter> element, interpreted as accepting all resources.");
                return Filter.include();
            }
            case 1:  return filters.get(0);
            default: return f.filter().or(filters);     // List of identifiers.
        }
    }

    /**
     * Reads the filter operator at the current cursor position. The cursor shall be on the {@link #START_ELEMENT}
     * event of the operator. After this method returns, the cursor is on the matching {@link #END_ELEMENT} event.
     *
     * @param  <R>  type of resources on which the filter applies.
     * @param  f    the factories to use.
     * @return the filter, never null.
     * @throws DataStoreContentException if the operator is not supported.
     */
    final <R> Filter<R> readOperator(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final FilterFactory<R,Object,Object> ff = f.filter();
        final String name = reader.getLocalName();
        switch (name) {
            case FilterTags.FEATURE_ID: {
                final String id = attribute(FilterTags.FID);
                skipElement();
                return ff.resourceId(requireIdentifier(name, id));
            }
            case FilterTags.GML_OBJECT_ID: {
                String id = reader.getAttributeValue(FilterTags.GML_NAMESPACE, FilterTags.ID);
                if (id == null) id = reader.getAttributeValue(Namespaces.GML, FilterTags.ID);
                if (id == null) id = attribute(FilterTags.ID);
                skipElement();
                return ff.resourceId(requireIdentifier(name, id));
            }
            case FilterTags.PROPERTY_IS_EQUAL_TO:
            case FilterTags.PROPERTY_IS_NOT_EQUAL_TO:
            case FilterTags.PROPERTY_IS_LESS_THAN:
            case FilterTags.PROPERTY_IS_GREATER_THAN:
            case FilterTags.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO:
            case FilterTags.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO: {
                final boolean matchCase = !"false".equalsIgnoreCase(attribute(FilterTags.MATCH_CASE));
                final List<Expression<R,?>> operands = readExpressions(f);
                requireCount(name, operands, 2);
                final Expression<R,?> a = operands.get(0);
                final Expression<R,?> b = operands.get(1);
                final MatchAction any = MatchAction.ANY;
                switch (name) {
                    case FilterTags.PROPERTY_IS_EQUAL_TO:     return ff.equal         (a, b, matchCase, any);
                    case FilterTags.PROPERTY_IS_NOT_EQUAL_TO: return ff.notEqual      (a, b, matchCase, any);
                    case FilterTags.PROPERTY_IS_LESS_THAN:    return ff.less          (a, b, matchCase, any);
                    case FilterTags.PROPERTY_IS_GREATER_THAN: return ff.greater       (a, b, matchCase, any);
                    case FilterTags.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO: return ff.lessOrEqual(a, b, matchCase, any);
                    default:                                  return ff.greaterOrEqual(a, b, matchCase, any);
                }
            }
            case FilterTags.PROPERTY_IS_LIKE: {
                final char wildCard   = character(FilterTags.WILD_CARD,   '%');
                final char singleChar = character(FilterTags.SINGLE_CHAR, '_');
                final char escape     = character(FilterTags.ESCAPE_CHAR, character(FilterTags.ESCAPE, '\\'));
                final boolean matchCase = !"false".equalsIgnoreCase(attribute(FilterTags.MATCH_CASE));
                final List<Expression<R,?>> operands = readExpressions(f);
                requireCount(name, operands, 2);
                if (!(operands.get(1) instanceof Literal<?,?> pattern) || pattern.getValue() == null) {
                    throw new DataStoreContentException("The pattern of <" + name + "> shall be a literal.");
                }
                return ff.like(operands.get(0), pattern.getValue().toString(), wildCard, singleChar, escape, matchCase);
            }
            case FilterTags.PROPERTY_IS_NULL: {
                final List<Expression<R,?>> operands = readExpressions(f);
                requireCount(name, operands, 1);
                return ff.isNull(operands.get(0));
            }
            case FilterTags.PROPERTY_IS_BETWEEN: {
                Expression<R,?> expression = null, lower = null, upper = null;
                while (nextChild()) {
                    switch (reader.getLocalName()) {
                        case FilterTags.LOWER_BOUNDARY: lower = readSingleExpression(f); break;
                        case FilterTags.UPPER_BOUNDARY: upper = readSingleExpression(f); break;
                        default: expression = readExpression(f); break;
                    }
                }
                if (expression == null || lower == null || upper == null) {
                    throw new DataStoreContentException("Incomplete <" + name + "> operator.");
                }
                return ff.between(expression, lower, upper);
            }
            case FilterTags.AND:
            case FilterTags.OR:
            case FilterTags.NOT: {
                final var operands = new ArrayList<Filter<R>>();
                while (nextChild()) {
                    operands.add(readOperator(f));
                }
                if (name.equals(FilterTags.NOT)) {
                    requireCount(name, operands, 1);
                    return ff.not(operands.get(0));
                }
                if (operands.isEmpty()) {
                    throw new DataStoreContentException("Empty <" + name + "> operator.");
                }
                if (operands.size() == 1) {
                    return operands.get(0);
                }
                return name.equals(FilterTags.AND) ? ff.and(operands) : ff.or(operands);
            }
            case FilterTags.FUNCTION: {
                // Filter Encoding 1.1 allows boolean functions as operands of logical operators.
                final Expression<R,?> function = readExpression(f);
                return ff.equal(function, ff.literal(Boolean.TRUE));
            }
            case FilterTags.BBOX: {
                Expression<R,?> geometry = null;
                Envelope bounds = null;
                while (nextChild()) {
                    if (isGML(reader.getNamespaceURI())) {
                        bounds = readGeometry().getEnvelope();
                    } else {
                        geometry = readExpression(f);
                    }
                }
                if (bounds == null) {
                    throw new DataStoreContentException("Missing envelope in <" + name + ">.");
                }
                if (geometry == null) {
                    geometry = ff.property(DEFAULT_GEOMETRY);
                }
                try {
                    return ff.bbox(geometry, bounds);
                } catch (IllegalArgumentException e) {
                    throw new DataStoreContentException("Cannot create the <" + name + "> operator: " + e.getMessage(), e);
                }
            }
            case FilterTags.EQUALS:
            case FilterTags.DISJOINT:
            case FilterTags.TOUCHES:
            case FilterTags.WITHIN:
            case FilterTags.OVERLAPS:
            case FilterTags.CROSSES:
            case FilterTags.INTERSECTS:
            case FilterTags.CONTAINS:
            case FilterTags.DWITHIN:
            case FilterTags.BEYOND: {
                final var operands = new ArrayList<Expression<R,?>>(2);
                Length distance = null;
                while (nextChild()) {
                    if (isGML(reader.getNamespaceURI())) {
                        operands.add(ff.literal(readGeometry()));
                    } else if (FilterTags.DISTANCE.equals(reader.getLocalName())) {
                        distance = readDistance();
                    } else {
                        final Expression<R,?> e = readExpression(f);
                        if (e != null) operands.add(e);
                    }
                }
                requireCount(name, operands, 2);
                final Expression<R,?> a = operands.get(0);
                final Expression<R,?> b = operands.get(1);
                try {
                    switch (name) {
                        case FilterTags.EQUALS:     return ff.equals    (a, b);
                        case FilterTags.DISJOINT:   return ff.disjoint  (a, b);
                        case FilterTags.TOUCHES:    return ff.touches   (a, b);
                        case FilterTags.WITHIN:     return ff.within    (a, b);
                        case FilterTags.OVERLAPS:   return ff.overlaps  (a, b);
                        case FilterTags.CROSSES:    return ff.crosses   (a, b);
                        case FilterTags.INTERSECTS: return ff.intersects(a, b);
                        case FilterTags.CONTAINS:   return ff.contains  (a, b);
                    }
                    if (distance == null) {
                        throw new DataStoreContentException("Missing distance in <" + name + ">.");
                    }
                    return name.equals(FilterTags.DWITHIN) ? ff.within(a, b, distance) : ff.beyond(a, b, distance);
                } catch (IllegalArgumentException e) {
                    // Include `InvalidFilterValueException`, for example if the geometry library is not supported.
                    throw new DataStoreContentException("Cannot create the <" + name + "> operator: " + e.getMessage(), e);
                }
            }
            default: {
                throw new DataStoreContentException("Unsupported filter operator <" + name + ">.");
            }
        }
    }

    /**
     * Reads the single expression contained in the element at the current cursor position.
     * This is used for elements such as {@code <ogc:LowerBoundary>}.
     */
    private <R> Expression<R,?> readSingleExpression(final Factories<R> f) throws XMLStreamException, DataStoreException {
        final String name = reader.getLocalName();
        final List<Expression<R,?>> expressions = readExpressions(f);
        requireCount(name, expressions, 1);
        return expressions.get(0);
    }

    /**
     * Returns the given identifier if non-null, or throws an exception otherwise.
     */
    private static String requireIdentifier(final String name, final String id) throws DataStoreContentException {
        if (id == null || id.isBlank()) {
            throw new DataStoreContentException("Missing identifier in <" + name + ">.");
        }
        return id;
    }

    /**
     * Returns the single character of the given attribute, or the given default value if the attribute is absent.
     */
    private char character(final String name, final char defaultValue) {
        final String value = reader.getAttributeValue(null, name);
        return (value != null && !value.isEmpty()) ? value.charAt(0) : defaultValue;
    }

    /**
     * Reads the {@code <ogc:Distance>} element at the current cursor position.
     * The unit of measurement is given by the {@code units} attribute. Default is metre.
     */
    private Length readDistance() throws XMLStreamException, DataStoreContentException {
        final String units = attribute(FilterTags.UNITS);
        final double value = parseDouble(readText(), FilterTags.DISTANCE);
        Unit<Length> unit = Units.METRE;
        if (units != null) try {
            unit = Units.valueOf(units).asType(Length.class);
        } catch (RuntimeException e) {
            warning("Unsupported unit of distance \"" + units + "\", metres assumed.");
        }
        return Quantities.create(value, unit);
    }

    // ---------------------------------------------------------------------------------------------
    //  Functions of Symbology Encoding
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the Symbology Encoding function at the current cursor position. The cursor shall be on the
     * {@link #START_ELEMENT} event of the function. After this method returns, the cursor is on the
     * matching {@link #END_ELEMENT} event.
     *
     * @param  <R>   type of resources on which the function applies.
     * @param  f     the factories to use.
     * @param  name  local name of the function element.
     * @return the function, or {@code null} if the element is not a known function.
     */
    private <R> Expression<R,?> readSymbologyFunction(final Factories<R> f, final String name)
            throws XMLStreamException, DataStoreException
    {
        final FilterFactory<R,Object,Object> ff = f.filter();
        final String fallbackText = attribute(SETags.FALLBACK_VALUE);
        final Expression<R,?> fallback = (fallbackText != null) ? ff.literal(fallbackText) : null;
        try {
            switch (name) {
                case SETags.CATEGORIZE: {
                    String belongTo = attribute(SETags.THRESHOLDS_BELONG_TO);
                    if (belongTo == null) belongTo = attribute("thresholdsBelongTo");      // Common misspelling.
                    final var mode       = keyword(belongTo, Categorize.ThresholdsBelongTo.class);
                    final var values     = new ArrayList<Expression<R,?>>();
                    final var thresholds = new ArrayList<Expression<R,?>>();
                    Expression<R,?> lookup = null;
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.LOOKUP_VALUE: lookup = lookup(readParameterValue(f)); break;
                            case SETags.VALUE:        values.add(readRequiredValue(f)); break;
                            case SETags.THRESHOLD:    thresholds.add(numberIfPossible(f, readRequiredValue(f))); break;
                            default: unexpected(name); break;
                        }
                    }
                    return new Categorize<R,Object>(lookup, values, thresholds, mode, fallback);
                }
                case SETags.INTERPOLATE: {
                    final var mode   = keyword(attribute(SETags.MODE),   Interpolate.Mode.class);
                    final var method = keyword(attribute(SETags.METHOD), Interpolate.Method.class);
                    final var points = new ArrayList<Interpolate.InterpolationPoint<R>>();
                    Expression<R,?> lookup = null;
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.LOOKUP_VALUE: lookup = lookup(readParameterValue(f)); break;
                            case SETags.INTERPOLATION_POINT: {
                                Expression<R,?> data = null, value = null;
                                while (nextChild()) {
                                    switch (reader.getLocalName()) {
                                        case SETags.DATA:  data  = readRequiredValue(f); break;
                                        case SETags.VALUE: value = readRequiredValue(f); break;
                                        default: unexpected(SETags.INTERPOLATION_POINT); break;
                                    }
                                }
                                points.add(new Interpolate.InterpolationPoint<>(asNumber(f, data), value));
                                break;
                            }
                            default: unexpected(name); break;
                        }
                    }
                    return new Interpolate<>(lookup, points, mode, method, fallback);
                }
                case SETags.RECODE: {
                    final var items = new ArrayList<Recode.MapItem<R>>();
                    Expression<R,?> lookup = null;
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.LOOKUP_VALUE: lookup = lookup(readParameterValue(f)); break;
                            case SETags.MAP_ITEM: {
                                Expression<R,?> data = null, value = null;
                                while (nextChild()) {
                                    switch (reader.getLocalName()) {
                                        case SETags.DATA:  data  = numberIfPossible(f, readRequiredValue(f)); break;
                                        case SETags.VALUE: value = readRequiredValue(f); break;
                                        default: unexpected(SETags.MAP_ITEM); break;
                                    }
                                }
                                items.add(new Recode.MapItem<>(data, value));
                                break;
                            }
                            default: unexpected(name); break;
                        }
                    }
                    return new Recode<>(lookup, items, fallback);
                }
                case SETags.FORMAT_NUMBER: {
                    final Expression<R,?> decimalPoint = literal(ff, attribute(SETags.DECIMAL_POINT));
                    final Expression<R,?> grouping     = literal(ff, attribute(SETags.GROUPING_SEPARATOR));
                    Expression<R,?> value = null, pattern = null, negative = null;
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.NUMERIC_VALUE:    value    = readParameterValue(f); break;
                            case SETags.PATTERN:          pattern  = readParameterValue(f); break;
                            case SETags.NEGATIVE_PATTERN: negative = readParameterValue(f); break;
                            default: unexpected(name); break;
                        }
                    }
                    return new FormatNumber<>(value, pattern, negative, decimalPoint, grouping);
                }
                case SETags.FORMAT_DATE: {
                    Expression<R,?> value = null, pattern = null;
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.DATE_VALUE: value   = readParameterValue(f); break;
                            case SETags.PATTERN:    pattern = readParameterValue(f); break;
                            default: unexpected(name); break;
                        }
                    }
                    return new FormatDate<>(value, pattern);
                }
                case SETags.SUBSTRING: {
                    Expression<R,?> value = null, position = null, length = null;
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.STRING_VALUE: value    = readParameterValue(f); break;
                            case SETags.POSITION:     position = readParameterValue(f); break;
                            case SETags.LENGTH:       length   = readParameterValue(f); break;
                            default: unexpected(name); break;
                        }
                    }
                    return new Substring<>(value, position, length);
                }
                case SETags.CONCATENATE: {
                    final var values = new ArrayList<Expression<R,?>>();
                    while (nextChild()) {
                        if (SETags.STRING_VALUE.equals(reader.getLocalName())) {
                            values.add(readRequiredValue(f));
                        } else {
                            unexpected(name);
                        }
                    }
                    return new Concatenate<>(values);
                }
                case SETags.CHANGE_CASE: {
                    final var direction = keyword(attribute(SETags.DIRECTION), ChangeCase.Direction.class);
                    return new ChangeCase<>(readStringValue(f, name), direction);
                }
                case SETags.TRIM: {
                    final var position = keyword(attribute(SETags.STRIP_OFF_POSITION), Trim.StripOffPosition.class);
                    final String c = reader.getAttributeValue(null, SETags.STRIP_OFF_CHAR);     // May be a space.
                    return new Trim<>(readStringValue(f, name), position, (c != null && !c.isEmpty()) ? ff.literal(c) : null);
                }
                case SETags.STRING_POSITION: {
                    final var direction = keyword(attribute(SETags.SEARCH_DIRECTION), StringPosition.SearchDirection.class);
                    Expression<R,?> lookup = null, value = null;
                    while (nextChild()) {
                        switch (reader.getLocalName()) {
                            case SETags.LOOKUP_STRING: lookup = readParameterValue(f); break;
                            case SETags.STRING_VALUE:  value  = readParameterValue(f); break;
                            default: unexpected(name); break;
                        }
                    }
                    return new StringPosition<>(lookup, value, direction);
                }
                case SETags.STRING_LENGTH: {
                    return new StringLength<>(readStringValue(f, name));
                }
                default: {
                    unexpected("expression");
                    return null;
                }
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new DataStoreContentException("Invalid <" + name + "> function: " + e.getMessage(), e);
        }
    }

    /**
     * Reads the {@code <se:StringValue>} element of a function having only that element.
     */
    private <R> Expression<R,?> readStringValue(final Factories<R> f, final String function)
            throws XMLStreamException, DataStoreException
    {
        Expression<R,?> value = null;
        while (nextChild()) {
            if (SETags.STRING_VALUE.equals(reader.getLocalName())) {
                value = readParameterValue(f);
            } else {
                unexpected(function);
            }
        }
        return value;
    }

    /**
     * Returns the given lookup value, or {@code null} if it is the {@code "Rasterdata"} keyword.
     * A null value is interpreted by the functions as the raster data.
     */
    private static <R> Expression<R,?> lookup(final Expression<R,?> value) {
        if (value instanceof Literal<?,?> literal && SETags.RASTER_DATA.equalsIgnoreCase(String.valueOf(literal.getValue()))) {
            return null;
        }
        return value;
    }

    /**
     * Returns a literal for the given text, or {@code null} if the text is null.
     */
    private static <R> Expression<R,?> literal(final FilterFactory<R,Object,Object> ff, final String text) {
        return (text != null) ? ff.literal(text) : null;
    }
}
