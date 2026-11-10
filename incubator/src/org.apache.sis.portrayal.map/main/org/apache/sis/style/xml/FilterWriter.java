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
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.xml.stream.XMLStreamException;
import org.opengis.geometry.Envelope;
import org.opengis.util.ScopedName;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.gml.GMLVersion;
import org.apache.sis.gml.GMLWriter;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.style.se1.function.Concatenate;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.DistanceOperator;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.Literal;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.NullOperator;
import org.opengis.filter.ResourceId;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.ValueReference;


/**
 * Base class of the writers of OGC Filter Encoding filters and expressions.
 * The geometries of spatial operators are written with {@link GMLWriter}.
 * This class contains the writing code shared by {@link FilterWriter100} and {@link FilterWriter110}.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class FilterWriter extends AbstractStaxWriter {
    /**
     * Creates a new writer sharing the cursor and the state of the given writer.
     *
     * @param  parent  the symbology writer which contains this filter writer.
     */
    protected FilterWriter(final AbstractStaxWriter parent) {
        super(parent);
    }

    /**
     * Returns the version of the GML geometries embedded in the filters.
     *
     * @return GML 2 for Filter Encoding 1.0, or GML 3.1 for Filter Encoding 1.1.
     */
    abstract GMLVersion gmlVersion();

    /**
     * Writes the given expression if it is a function defined by Symbology Encoding and if
     * that function has a specific XML encoding in the version of this writer.
     *
     * @param  <R>         type of resources on which the expression applies.
     * @param  expression  the expression to write.
     * @return whether the expression has been written.
     */
    abstract <R> boolean writeSymbologyFunction(Expression<R,?> expression) throws XMLStreamException, DataStoreException;

    // ---------------------------------------------------------------------------------------------
    //  Expressions
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the local part of the name of the given function, or {@code null} if none.
     */
    static String functionName(final Expression<?,?> expression) {
        final ScopedName name = expression.getFunctionName();
        return (name != null) ? name.tip().toString() : null;
    }

    /**
     * Returns whether the given value is a geometry or an envelope.
     */
    private static boolean isGeometry(final Object value) {
        return (value instanceof Geometry) || (value instanceof Envelope);
    }

    /**
     * Writes the given expression as a Filter Encoding element.
     *
     * @param  <R>         type of resources on which the expression applies.
     * @param  expression  the expression to write.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreException if the expression cannot be written.
     */
    final <R> void writeExpression(final Expression<R,?> expression) throws XMLStreamException, DataStoreException {
        if (expression instanceof ValueReference<R,?> ref) {
            text(FilterTags.NAMESPACE, FilterTags.PROPERTY_NAME, ref.getXPath());
            return;
        }
        if (expression instanceof Literal<R,?> literal) {
            final Object value = literal.getValue();
            start(FilterTags.NAMESPACE, FilterTags.LITERAL);
            if (isGeometry(value)) {
                writeGeometry(value);
            } else if (value != null) {
                writer.writeCharacters(format(value));
            }
            end();
            return;
        }
        if (writeSymbologyFunction(expression)) {
            return;
        }
        final String name = functionName(expression);
        if (name == null) {
            throw new DataStoreContentException("Unsupported expression: " + expression);
        }
        final String element;
        switch (name) {
            case "Add":      element = FilterTags.ADD; break;
            case "Subtract": element = FilterTags.SUB; break;
            case "Multiply": element = FilterTags.MUL; break;
            case "Divide":   element = FilterTags.DIV; break;
            default: {
                start(FilterTags.NAMESPACE, FilterTags.FUNCTION);
                attribute(FilterTags.NAME, name);
                for (final Expression<R,?> parameter : expression.getParameters()) {
                    writeExpression(parameter);
                }
                end();
                return;
            }
        }
        start(FilterTags.NAMESPACE, element);
        for (final Expression<R,?> parameter : expression.getParameters()) {
            writeExpression(parameter);
        }
        end();
    }

    /**
     * Writes the given expression as the content of the current element, which is a parameter value.
     * Parameter values are mixed contents: literals are written as texts, and the parts of a top-level
     * {@link Concatenate} function are written as a sequence of texts and expressions.
     *
     * @param  <R>         type of resources on which the expression applies.
     * @param  expression  the expression to write.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreException if the expression cannot be written.
     */
    final <R> void writeParameterContent(final Expression<R,?> expression) throws XMLStreamException, DataStoreException {
        if (expression instanceof Literal<R,?> literal && !isGeometry(literal.getValue())) {
            final String text = format(literal.getValue());
            if (text != null) {
                writer.writeCharacters(text);
            }
        } else if (expression instanceof Concatenate<?> concatenate) {
            for (final Expression<?,?> part : concatenate.getStringValues()) {
                if (part instanceof Literal<?,?> literal && !isGeometry(literal.getValue())) {
                    final String text = format(literal.getValue());
                    if (text != null) {
                        writer.writeCharacters(text);
                    }
                } else {
                    writeExpression(part);
                }
            }
        } else {
            writeExpression(expression);
        }
    }

    /**
     * Writes an element containing the given expression as a parameter value.
     * Does nothing if the given expression is null.
     *
     * @param  <R>         type of resources on which the expression applies.
     * @param  ns          namespace of the element to write.
     * @param  local       local name of the element to write.
     * @param  expression  the expression to write, or {@code null}.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreException if the expression cannot be written.
     */
    final <R> void writeParameter(final String ns, final String local, final Expression<R,?> expression)
            throws XMLStreamException, DataStoreException
    {
        if (expression != null) {
            start(ns, local);
            writeParameterContent(expression);
            end();
        }
    }

    /**
     * Writes the given geometry or envelope in GML.
     *
     * @param  value  the geometry or envelope to write.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreException if the geometry cannot be written.
     */
    final void writeGeometry(final Object value) throws XMLStreamException, DataStoreException {
        final Geometry geometry;
        if (value instanceof Geometry g) {
            geometry = g;
        } else if (value instanceof Envelope e) {
            geometry = new BBox(e);
        } else {
            throw new DataStoreContentException("Cannot write a geometry of class "
                    + (value != null ? value.getClass().getName() : "null")
                    + ". Only Apache SIS geometries and envelopes are supported.");
        }
        try (GMLWriter gml = GMLWriter.fragment(writer, gmlVersion())) {
            gml.writeGeometry(geometry);
        }
    }

    // ---------------------------------------------------------------------------------------------
    //  Filters (comparison, logical, spatial and identifier operators)
    // ---------------------------------------------------------------------------------------------

    /**
     * Writes the given filter as a {@code <ogc:Filter>} element.
     *
     * @param  <R>     type of resources on which the filter applies.
     * @param  filter  the filter to write.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreException if the filter cannot be written.
     */
    final <R> void writeFilter(final Filter<R> filter) throws XMLStreamException, DataStoreException {
        start(FilterTags.NAMESPACE, FilterTags.FILTER);
        if (isIdentifierList(filter)) {
            for (final Filter<R> operand : ((LogicalOperator<R>) filter).getOperands()) {
                writeOperator(operand);
            }
        } else {
            writeOperator(filter);
        }
        end();
    }

    /**
     * Returns whether the given filter is a disjunction of resource identifiers.
     * Such filters are written as a list of identifiers directly in the {@code <ogc:Filter>} element.
     */
    private static boolean isIdentifierList(final Filter<?> filter) {
        if (filter instanceof LogicalOperator<?> logical && logical.getOperatorType() == LogicalOperatorName.OR) {
            for (final Filter<?> operand : logical.getOperands()) {
                if (!(operand instanceof ResourceId<?>)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Writes the given filter operator.
     *
     * @param  <R>     type of resources on which the filter applies.
     * @param  filter  the filter to write.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreException if the filter cannot be written.
     */
    private <R> void writeOperator(final Filter<R> filter) throws XMLStreamException, DataStoreException {
        if (filter instanceof ResourceId<R> id) {
            empty(FilterTags.NAMESPACE, FilterTags.FEATURE_ID);
            attribute(FilterTags.FID, id.getIdentifier());
        } else if (filter instanceof LikeOperator<R> like) {
            final List<Expression<R,?>> operands = like.getExpressions();
            start(FilterTags.NAMESPACE, FilterTags.PROPERTY_IS_LIKE);
            attribute(FilterTags.WILD_CARD,   String.valueOf(like.getWildCard()));
            attribute(FilterTags.SINGLE_CHAR, String.valueOf(like.getSingleChar()));
            attribute(isVersion11() ? FilterTags.ESCAPE_CHAR : FilterTags.ESCAPE, String.valueOf(like.getEscapeChar()));
            if (isVersion11() && !like.isMatchingCase()) {
                attribute(FilterTags.MATCH_CASE, Boolean.FALSE);
            }
            writeExpressions(operands);
            end();
        } else if (filter instanceof BetweenComparisonOperator<R> between) {
            start(FilterTags.NAMESPACE, FilterTags.PROPERTY_IS_BETWEEN);
            writeExpression(between.getExpression());
            start(FilterTags.NAMESPACE, FilterTags.LOWER_BOUNDARY);
            writeExpression(between.getLowerBoundary());
            end();
            start(FilterTags.NAMESPACE, FilterTags.UPPER_BOUNDARY);
            writeExpression(between.getUpperBoundary());
            end();
            end();
        } else if (filter instanceof NullOperator<R> isNull) {
            start(FilterTags.NAMESPACE, FilterTags.PROPERTY_IS_NULL);
            writeExpressions(isNull.getExpressions());
            end();
        } else if (filter instanceof BinaryComparisonOperator<R> comparison) {
            start(FilterTags.NAMESPACE, comparisonName(comparison.getOperatorType()));
            if (isVersion11() && !comparison.isMatchingCase()) {
                attribute(FilterTags.MATCH_CASE, Boolean.FALSE);
            }
            writeExpressions(comparison.getExpressions());
            end();
        } else if (filter instanceof LogicalOperator<R> logical) {
            final LogicalOperatorName type = logical.getOperatorType();
            final String element;
            if (type == LogicalOperatorName.AND) {
                element = FilterTags.AND;
            } else if (type == LogicalOperatorName.OR) {
                element = FilterTags.OR;
            } else if (type == LogicalOperatorName.NOT) {
                element = FilterTags.NOT;
            } else {
                throw unsupported(filter);
            }
            start(FilterTags.NAMESPACE, element);
            for (final Filter<R> operand : logical.getOperands()) {
                writeOperator(operand);
            }
            end();
        } else if (filter instanceof DistanceOperator<R> distance) {
            final DistanceOperatorName type = distance.getOperatorType();
            final String element;
            if (type == DistanceOperatorName.WITHIN) {
                element = FilterTags.DWITHIN;
            } else if (type == DistanceOperatorName.BEYOND) {
                element = FilterTags.BEYOND;
            } else {
                throw unsupported(filter);
            }
            final List<Expression<R,?>> operands = distance.getExpressions();
            start(FilterTags.NAMESPACE, element);
            writeSpatialOperand(operands.get(0));
            writeSpatialOperand(operands.get(1));
            final Quantity<Length> value = distance.getDistance();
            start(FilterTags.NAMESPACE, FilterTags.DISTANCE);
            attribute(FilterTags.UNITS, value.getUnit());
            writer.writeCharacters(format(value.getValue().doubleValue()));
            end();
            end();
        } else if (filter instanceof BinarySpatialOperator<R> spatial) {
            final SpatialOperatorName type = spatial.getOperatorType();
            final List<Expression<R,?>> operands = spatial.getExpressions();
            start(FilterTags.NAMESPACE, spatialName(type, filter));
            if (type == SpatialOperatorName.BBOX) {
                final Expression<R,?> property = operands.get(0);
                if (!(property instanceof ValueReference<R,?> ref && FilterReader.DEFAULT_GEOMETRY.equals(ref.getXPath()))
                        || !isVersion11())
                {
                    writeExpression(property);
                }
                writeSpatialOperand(operands.get(1));
            } else {
                for (final Expression<R,?> operand : operands) {
                    writeSpatialOperand(operand);
                }
            }
            end();
        } else {
            throw unsupported(filter);
        }
    }

    /**
     * Writes all the given expressions.
     */
    private <R> void writeExpressions(final List<Expression<R,?>> expressions) throws XMLStreamException, DataStoreException {
        for (final Expression<R,?> expression : expressions) {
            writeExpression(expression);
        }
    }

    /**
     * Writes an operand of a spatial operator. Literal geometries and envelopes are written in GML,
     * without {@code <ogc:Literal>} wrapper.
     */
    private <R> void writeSpatialOperand(final Expression<R,?> operand) throws XMLStreamException, DataStoreException {
        if (operand instanceof Literal<R,?> literal && isGeometry(literal.getValue())) {
            writeGeometry(literal.getValue());
        } else {
            writeExpression(operand);
        }
    }

    /**
     * Returns the element name for the given comparison operator.
     */
    private static String comparisonName(final ComparisonOperatorName type) throws DataStoreContentException {
        if (type == ComparisonOperatorName.PROPERTY_IS_EQUAL_TO)                 return FilterTags.PROPERTY_IS_EQUAL_TO;
        if (type == ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO)             return FilterTags.PROPERTY_IS_NOT_EQUAL_TO;
        if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN)                return FilterTags.PROPERTY_IS_LESS_THAN;
        if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN)             return FilterTags.PROPERTY_IS_GREATER_THAN;
        if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO)    return FilterTags.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO;
        if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO) return FilterTags.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO;
        throw new DataStoreContentException("Unsupported comparison operator: " + type);
    }

    /**
     * Returns the element name for the given spatial operator.
     */
    private static String spatialName(final SpatialOperatorName type, final Filter<?> filter) throws DataStoreContentException {
        if (type == SpatialOperatorName.BBOX)       return FilterTags.BBOX;
        if (type == SpatialOperatorName.EQUALS)     return FilterTags.EQUALS;
        if (type == SpatialOperatorName.DISJOINT)   return FilterTags.DISJOINT;
        if (type == SpatialOperatorName.TOUCHES)    return FilterTags.TOUCHES;
        if (type == SpatialOperatorName.WITHIN)     return FilterTags.WITHIN;
        if (type == SpatialOperatorName.OVERLAPS)   return FilterTags.OVERLAPS;
        if (type == SpatialOperatorName.CROSSES)    return FilterTags.CROSSES;
        if (type == SpatialOperatorName.INTERSECTS) return FilterTags.INTERSECTS;
        if (type == SpatialOperatorName.CONTAINS)   return FilterTags.CONTAINS;
        throw unsupported(filter);
    }

    /**
     * Returns the exception to throw for a filter which cannot be written.
     */
    private static DataStoreContentException unsupported(final Filter<?> filter) {
        return new DataStoreContentException("Unsupported filter operator: " + filter.getOperatorType());
    }
}
