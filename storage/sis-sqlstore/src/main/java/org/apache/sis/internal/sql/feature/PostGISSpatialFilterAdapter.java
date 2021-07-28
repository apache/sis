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
package org.apache.sis.internal.sql.feature;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.filter.sqlmm.SQLMM;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.InvalidFilterValueException;
import org.opengis.filter.Literal;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.ValueReference;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;

/**
 * Modify operands of a spatial operator to make their CRS match each other.
 * This is required for PostGIS that expect user to provide geometries in the same CRS.
 * It also force {@link AxesConvention#DISPLAY_ORIENTED display orientation} on resulting CRS / geometries.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class PostGISSpatialFilterAdapter implements UnaryOperator<Filter<Feature>> {

    static final Logger LOGGER = Logging.getLogger(Modules.SQL);

    /**
     * Allow to detect third-party filters that use SQLMM function names instead of filter names.
     */
    private static final Map<String, SpatialOperatorName> SQLMM_TO_FILTER;

    static {
        final Map<String, SpatialOperatorName> tmp = new HashMap<>();
        tmp.put(toKey(SQLMM.ST_Contains), SpatialOperatorName.CONTAINS);
        tmp.put(toKey(SQLMM.ST_Crosses), SpatialOperatorName.CROSSES);
        tmp.put(toKey(SQLMM.ST_Disjoint), SpatialOperatorName.DISJOINT);
        tmp.put(toKey(SQLMM.ST_Equals), SpatialOperatorName.EQUALS);
        tmp.put(toKey(SQLMM.ST_Intersects), SpatialOperatorName.INTERSECTS);
        tmp.put(toKey(SQLMM.ST_Overlaps), SpatialOperatorName.OVERLAPS);
        tmp.put(toKey(SQLMM.ST_Touches), SpatialOperatorName.TOUCHES);
        tmp.put(toKey(SQLMM.ST_Within), SpatialOperatorName.WITHIN);
        SQLMM_TO_FILTER = Collections.unmodifiableMap(tmp);
    }

    private static String toKey(SQLMM value) {
        return value.name().toLowerCase(Locale.ENGLISH);
    }

    private final Function<ValueReference<Feature, ?>, Optional<CoordinateReferenceSystem>> fetchCrs;

    PostGISSpatialFilterAdapter(Function<ValueReference<Feature, ?>, Optional<CoordinateReferenceSystem>> fetchCrs) {
        this.fetchCrs = fetchCrs;
    }

    @Override
    public Filter<Feature> apply(Filter<Feature> base) {
        if (base instanceof BinarySpatialOperator) return adaptBinarySpatialOp((BinarySpatialOperator<Feature>) base);
        return getSpatialType(base)
                .map(type -> {
                    final List<Expression<? super Feature, ?>> operands = base.getExpressions();
                    if (operands.size() != 2) return base; // TODO: manage single operand -> Ex: bbox filter matching any geometric column
                    return adaptBinarySpatialOp(type, operands.get(0), operands.get(1), base);
                })
                .orElse(base);
    }

    private Optional<SpatialOperatorName> getSpatialType(Filter<Feature> base) {
        if (base == null) return Optional.empty();
        final CodeList<?> baseType = base.getOperatorType();
        SpatialOperatorName son;
        if (baseType instanceof SpatialOperatorName) return Optional.of((SpatialOperatorName) baseType);
        else if (baseType == null) return Optional.empty();

        final String filterTypeIdent = baseType.identifier() == null ? baseType.name() : baseType.identifier();
        son = SQLMM_TO_FILTER.get(filterTypeIdent.toLowerCase(Locale.ENGLISH));
        if (son == null) {
            try {
                SpatialOperatorName.valueOf(filterTypeIdent);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e, () -> "No match in spatial ops for "+filterTypeIdent);
                son = null;
            }
        }

        return Optional.ofNullable(son);
    }

    private Filter<Feature> adaptBinarySpatialOp(BinarySpatialOperator<Feature> operator) {
        return adaptBinarySpatialOp(operator.getOperatorType(), operator.getOperand1(), operator.getOperand2(), operator);
    }

    /**
     * Verify if input filter or any of its operand need to be adapted. If it's the case, we return a new filter
     * instance that contains wanted changes.
     *
     * @param opType Information from input binary spatial operator: its {@link Filter#getOperatorType() operation name}
     * @param left The {@link BinarySpatialOperator#getOperand1() first operand} of the filter
     * @param right The {@link BinarySpatialOperator#getOperand2() second operand} of the filter
     * @param source The filter object other parameters have been extracted from, for reference. Can be useful to get
     *               other informations that are not part of {@link BinarySpatialOperator} interface. It is also needed,
     *               so we can return it directly if no change is necessary.
     */
    private Filter<Feature> adaptBinarySpatialOp(SpatialOperatorName opType, final Expression<? super Feature, ?> left, final Expression<? super Feature, ?> right, Filter<Feature> source) {
        final Object constVal;
        boolean leftIsConstant = false;
        if (left instanceof Literal) {
            constVal = ((Literal<?, ?>) left).getValue();
            leftIsConstant = true;
        } else if (right instanceof Literal) {
            constVal = ((Literal<?, ?>) right).getValue();
        } else constVal = null;

        if (constVal == null) return source;
        final CoordinateReferenceSystem constValCrs = getCrs(constVal).orElse(null);
        if (constValCrs == null) return source; // assume same CRS as other expression.

        Expression<? super Feature, ?> other = leftIsConstant ? right : left;
        final CoordinateReferenceSystem otherCrs = getTargetCrs(other).orElse(null);
        if (otherCrs == null) {
            /* TODO: we could mitigate this problem by forcing a ST_Transform operation upon the non literal operand.
             * However, I am a little afraid to do so, because it requires to be able to safely define a PostGIS SRID.
             * Moreover, it would potentially prevent PostGIS to use its index, hurting performance a lot.
             */
            final String newLine = System.lineSeparator();
            LOGGER.warning(
                    "Context: Spatial filter upon a PostGIS table"+newLine+
                    "Issue: We cannot guarantee the coherence between the two geometric operands CRS"+ newLine+
                    "Result: You might get a wrong result for your geometric comparison filter" + newLine +
                    "Solution: To fix this, you should:"+newLine+
                    "- Verify SRID constraints on your geometric columns"+newLine+
                    "- In case the data source is an SQL view, you should cast statically any geometric column like:" +
                            " `SELECT geom_column::GEOMETRY(<TYPE>, <SRID>)`."+newLine+
                    "- Otherwise, manually wrap one (or both) of the operand with a ST_Transform expression");
        }

        // PostGIS CRS are all forced with east axis first.
        final CoordinateReferenceSystem normalizedTargetCrs = forceEastFirst(otherCrs == null ? constValCrs : otherCrs);

        final Object adaptedVal = findTransform(constValCrs, normalizedTargetCrs)
                .map(transform -> transform(constVal, transform))
                .orElse(constVal);

        if (adaptedVal != constVal) {
            return new BinarySpatialOpProxy(source, opType, other, new Literal<Feature, Object>() {

                @Override
                public Object getValue() {
                    return adaptedVal;
                }

                @Override
                public <N> Expression<Feature, N> toValueType(Class<N> aClass) {
                    throw new UnsupportedOperationException("Not supported yet");
                }

            });
        }

        return source;
    }

    private Optional<CoordinateOperation> findTransform(CoordinateReferenceSystem from, CoordinateReferenceSystem to) {
        if (from == null || to == null || from == to || Utilities.equalsIgnoreMetadata(from, to)) return Optional.empty();
        try {
            final CoordinateOperation op = CRS.findOperation(from, to, null);
            final MathTransform transform = op.getMathTransform();
            // If the transform is a no-op, we avoid mutating filter for nothing.
            if (transform == null || transform.isIdentity()) return Optional.empty();
            else return Optional.of(op);
        } catch (FactoryException e) {
            throw new BackingStoreException("Cannot prepare CRS conversion for spatial filter", e);
        }
    }

    private Object transform(Object constVal, CoordinateOperation operation) {
        try {
            if (constVal instanceof Envelope) return Envelopes.transform(operation, (Envelope) constVal);
            else if (constVal instanceof GeometryWrapper)
                return ((GeometryWrapper<?>) constVal).transform(operation, false);
            else {
                final GeometryWrapper<?> wrapper = Geometries.wrap(constVal)
                        .orElseThrow(() -> new IllegalArgumentException("Expression value is unsupported: " + constVal.getClass()))
                        .transform(operation, false);
                return wrapper.implementation();
            }
        } catch (TransformException | FactoryException e) {
            throw new BackingStoreException("Cannot transform expression value of a spatial filter", e);
        }
    }

    private CoordinateReferenceSystem forceEastFirst(CoordinateReferenceSystem crs) {
        return AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.DISPLAY_ORIENTED);
    }

    private Optional<CoordinateReferenceSystem> getCrs(Object constVal) {
        if (constVal instanceof Envelope) {
            return Optional.ofNullable(((Envelope) constVal).getCoordinateReferenceSystem());
        } else if (constVal instanceof org.opengis.geometry.Geometry) {
            return Optional.ofNullable(((org.opengis.geometry.Geometry) constVal).getCoordinateReferenceSystem());
        } else return Geometries.wrap(constVal)
                .map(org.opengis.geometry.Geometry::getCoordinateReferenceSystem);
    }

    private Optional<CoordinateReferenceSystem> getTargetCrs(Expression<? super Feature, ?> other) {
        if (other instanceof ValueReference<?, ?>) {
            return fetchCrs.apply((ValueReference<Feature, ?>) other);
        } else {
            final String fnName = other.getFunctionName().tip().toString().toLowerCase(Locale.ENGLISH);
            if (SQLMM.ST_Transform.name().toLowerCase(Locale.ENGLISH).equals(fnName)) {
                for (Expression<?, ?> arg : other.getParameters()) {
                    try {
                        // DO NOT apply toValueType. I want to avoid it in case it is "smart enough" to extract a CRS
                        // from the geometry to transform
                        final Object value = arg.apply(null);
                        if (value instanceof CoordinateReferenceSystem) return Optional.of((CoordinateReferenceSystem) value);
                    } catch (Exception e) {
                        LOGGER.log(Level.FINEST, "Cannot evaluate expression without input", e);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static final class BinarySpatialOpProxy implements BinarySpatialOperator<Feature> {
        final Filter<Feature> source;

        final SpatialOperatorName op;
        final Expression<? super Feature, ?> left;
        final Expression<? super Feature, ?> right;

        private BinarySpatialOpProxy(Filter<Feature> source, SpatialOperatorName op, Expression<? super Feature, ?> left, Expression<? super Feature, ?> right) {
            this.source = source;
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        public SpatialOperatorName getOperatorType() {
            return op;
        }

        @Override
        public Expression<? super Feature, ?> getOperand1() {
            return left;
        }

        @Override
        public Expression<? super Feature, ?> getOperand2() {
            return right;
        }

        @Override
        public List<Expression<? super Feature, ?>> getExpressions() {
            return Arrays.asList(left, right);
        }

        @Override
        public boolean test(Feature feature) throws InvalidFilterValueException {
            LOGGER.fine("A filter meant to be optimized as a PostGIS query has been executed by Java");
            return source.test(feature);
        }
    }
}
