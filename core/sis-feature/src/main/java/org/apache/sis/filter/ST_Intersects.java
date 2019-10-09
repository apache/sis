package org.apache.sis.filter;

import java.util.function.Predicate;

import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.Intersects;
import org.opengis.geometry.Geometry;

import org.apache.sis.internal.feature.GeometryWrapper;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import static org.apache.sis.util.ArgumentChecks.ensureNonEmpty;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * TODO: check CRS
 * TODO: refine once Geometry API is stable.
 */
public class ST_Intersects implements Intersects {

    public static final String NAME = "ST_Intersect";

    final Expression left;
    final Expression right;

    private final Predicate intersects;

    public ST_Intersects(Expression[] parameters) {
        ensureNonEmpty("Parameters", parameters);
        if (parameters.length != 2) throw new IllegalArgumentException("2 parameters are expected for intersection, but "+parameters.length+" are provided");

        left = parameters[0];
        right = parameters[1];
        ensureNonNull("Left operand", left);
        ensureNonNull("Right operand", right);
        if (left instanceof Literal && right instanceof Literal) {
            intersects = constantOp((Literal) left, (Literal) right);
        } else if (left instanceof Literal) {
            intersects = intersect(right, (Literal) left);
        } else if (right instanceof Literal) {
            intersects = intersect(left, (Literal) right);
        } else intersects = this::nonOptimizedIntersects;
    }

    private boolean nonOptimizedIntersects(Object candidate) {
        final Object leftEval = left.evaluate(candidate);
        final Object rightEval = right.evaluate(candidate);
        if (leftEval == null || rightEval == null) return false;
        return toJTS(leftEval).intersects(toJTS(rightEval));
    }

    private static org.locationtech.jts.geom.Geometry toJTS(Object value) {
        if (value instanceof GeometryWrapper) value = ((GeometryWrapper) value).geometry;
        if (value instanceof org.locationtech.jts.geom.Geometry) return (org.locationtech.jts.geom.Geometry) value;
        throw new UnsupportedOperationException("Unsupported geometry type: "+value.getClass().getCanonicalName());
    }

    private Predicate constantOp(Literal left, Literal right) {
        final boolean result = left.getValue() != null
                && right.getValue() != null
                && toJTS(left.getValue()).intersects(toJTS(right.getValue()));
        return it -> result;
    }

    private static Predicate intersect(Expression left, Literal right) {
        Object value = right.getValue();
        ensureNonNull("Literal value", value);
        // TODO: make more consistent strategy once Geometry API is stable.
        if (value instanceof GeometryWrapper) value = ((GeometryWrapper) value).geometry;
        if (value instanceof org.locationtech.jts.geom.Geometry) {
            final PreparedGeometry pg = new PreparedGeometryFactory().create((org.locationtech.jts.geom.Geometry) value);
            return it -> {
                Object val = left.evaluate(it);
                if (val == null) return false;
                return pg.intersects(toJTS(val));
            };
        } else if (value instanceof Geometry) {
            final Geometry geom = (Geometry) value;
            return it -> {
                final Geometry newVal = left.evaluate(it, Geometry.class);
                if (newVal == null) {
                    final Object testVal = left.evaluate(it);
                    if (testVal == null) return false;
                    throw new UnsupportedOperationException("Unsupported geometry type: "+testVal.getClass().getCanonicalName());
                }
                return geom.intersects(newVal);
            };
        } else throw new UnsupportedOperationException("Unsupported geometry type: "+value.getClass().getCanonicalName());
    }

    @Override
    public Expression getExpression1() {
        return left;
    }

    @Override
    public Expression getExpression2() {
        return right;
    }

    @Override
    public boolean evaluate(Object object) {
        return intersects.test(object);
    }

    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }
}
