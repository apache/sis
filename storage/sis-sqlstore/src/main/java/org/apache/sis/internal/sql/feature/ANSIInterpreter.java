package org.apache.sis.internal.sql.feature;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.opengis.filter.*;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.BinaryExpression;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.*;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.util.GenericName;
import org.opengis.util.LocalName;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.WrapResolution;
import org.apache.sis.util.iso.Names;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * Port of Geotk FilterToSQL for an ANSI compliant query builder.
 *
 * @implNote For now, we over-use parenthesis to ensure consistent operator priority. In the future, we could evolve
 * this component to provide more elegant transcription of filter groups.
 *
 * No case insensitive support of binary comparison is done.
 *
 * TODO: define a set of accepter property names (even better: link to {@link FeatureAdapter}), so any {@link PropertyName}
 * filter refering to non pure SQL property (like relations) will cause a failure.
 *
 * @author Alexis Manin (Geomatys)
 */
public class ANSIInterpreter implements FilterVisitor, ExpressionVisitor {

    private final java.util.function.Function<Literal, CharSequence> valueFormatter;

    private final java.util.function.Function<PropertyName, CharSequence> nameFormatter;

    public ANSIInterpreter() {
        this(ANSIInterpreter::format, ANSIInterpreter::format);
    }

    public ANSIInterpreter(
            java.util.function.Function<Literal, CharSequence> valueFormatter,
            java.util.function.Function<PropertyName, CharSequence> nameFormatter
    ) {
        ensureNonNull("Literal value formatter", valueFormatter);
        ensureNonNull("Property name formatter", nameFormatter);
        this.valueFormatter = valueFormatter;
        this.nameFormatter = nameFormatter;
    }

    @Override
    public CharSequence visitNullFilter(Object extraData) {
        throw new UnsupportedOperationException("Null filter is not supported.");
    }

    @Override
    public Object visit(ExcludeFilter filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(IncludeFilter filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public CharSequence visit(And filter, Object extraData) {
        return join(filter, " AND ", extraData);
    }

    @Override
    public Object visit(Id filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Not filter, Object extraData) {
        final CharSequence innerFilter = evaluateMandatory(filter.getFilter(), extraData);
        return "NOT (" + innerFilter + ")";
    }

    @Override
    public Object visit(Or filter, Object extraData) {
        return join(filter, " OR ", extraData);
    }

    @Override
    public Object visit(PropertyIsBetween filter, Object extraData) {
        final CharSequence propertyExp = evaluateMandatory(filter.getExpression(), extraData);
        final CharSequence lowerExp = evaluateMandatory(filter.getLowerBoundary(), extraData);
        final CharSequence upperExp = evaluateMandatory(filter.getUpperBoundary(), extraData);

        return new StringBuilder(propertyExp)
                .append(" BETWEEN ")
                .append(lowerExp)
                .append(" AND ")
                .append(upperExp);
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        return joinMatchCase(filter, " = ", extraData);
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        return joinMatchCase(filter, " <> ", extraData);
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        return joinMatchCase(filter, " > ", extraData);
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        return joinMatchCase(filter, " >= ", extraData);
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        return joinMatchCase(filter, " < ", extraData);
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        return joinMatchCase(filter, " <= ", extraData);
    }

    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        // TODO: PostgreSQL extension : ilike
        ensureMatchCase(filter::isMatchingCase);
        // TODO: port Geotk
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(PropertyIsNull filter, Object extraData) {
        return evaluateMandatory(filter.getExpression(), extraData) + " IS NULL";
    }

    @Override
    public Object visit(PropertyIsNil filter, Object extraData) {
        return evaluateMandatory(filter.getExpression(), extraData) + " IS NULL";
    }

    /*
     * SPATIAL FILTERS
     */

    @Override
    public Object visit(BBOX filter, Object extraData) {
        // TODO: This is a wrong interpretation, but sqlmm has no equivalent of filter encoding bbox, so we'll
        // fallback on a standard intersection. However, PostGIS, H2, etc. have their own versions of such filters.
        if (filter.getExpression1() == null || filter.getExpression2() == null)
            throw new UnsupportedOperationException("Not supported yet : bbox over all geometric properties");
        return function("ST_Intersects", filter, extraData);
    }

    @Override
    public Object visit(Beyond filter, Object extraData) {
        // TODO: ISO SQL specifies that unit of distance could be specified. However, PostGIS documentation does not
        // talk about it. For now, we'll fallback on Java implementation until we're sure how to perform native
        // operation properly.
        throw new UnsupportedOperationException("Not yet: unit management ambiguous");
    }

    @Override
    public Object visit(Contains filter, Object extraData) {
        return function("ST_Contains", filter, extraData);
    }

    @Override
    public Object visit(Crosses filter, Object extraData) {
        return function("ST_Crosses", filter, extraData);
    }

    @Override
    public Object visit(Disjoint filter, Object extraData) {
        return function("ST_Disjoint", filter, extraData);
    }

    @Override
    public Object visit(DWithin filter, Object extraData) {
        // TODO: as for beyond filter above, unit determination is a bit complicated.
        throw new UnsupportedOperationException("Not yet: unit management to handle properly");
    }

    @Override
    public Object visit(Equals filter, Object extraData) {
        return function("ST_Equals", filter, extraData);
    }

    @Override
    public Object visit(Intersects filter, Object extraData) {
        return function("ST_Intersects", filter, extraData);
    }

    @Override
    public Object visit(Overlaps filter, Object extraData) {
        return function("ST_Overlaps", filter, extraData);
    }

    @Override
    public Object visit(Touches filter, Object extraData) {
        return function("ST_Touches", filter, extraData);
    }

    @Override
    public Object visit(Within filter, Object extraData) {
        return function("ST_Within", filter, extraData);
    }

    /*
     * TEMPORAL OPERATORS
     */

    @Override
    public Object visit(After filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(AnyInteracts filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Before filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Begins filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(BegunBy filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(During filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(EndedBy filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Ends filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Meets filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(MetBy filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(OverlappedBy filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(TContains filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(TEquals filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(TOverlaps filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    /*
     * Expression visitor
     */

    @Override
    public Object visit(NilExpression expression, Object extraData) {
        return "NULL";
    }

    @Override
    public Object visit(Add expression, Object extraData) {
        return join(expression, " + ", extraData);
    }

    @Override
    public Object visit(Divide expression, Object extraData) {
        return join(expression, " / ", extraData);
    }

    @Override
    public Object visit(Function expression, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 01/10/2019
    }

    @Override
    public Object visit(Literal expression, Object extraData) {
        return valueFormatter.apply(expression);
    }

    @Override
    public Object visit(Multiply expression, Object extraData) {
        return join(expression, " * ", extraData);
    }


    @Override
    public Object visit(PropertyName expression, Object extraData) {
        return nameFormatter.apply(expression);
    }

    @Override
    public Object visit(Subtract expression, Object extraData) {
        return join(expression, " - ", extraData);
    }

    /*
     * UTILITIES
     */

    protected static CharSequence format(Literal candidate) {
        Object value = candidate == null ? null : candidate.getValue();
        if (value == null) return "NULL";
        else if (value instanceof CharSequence) {
            final String asStr = value.toString();
            asStr.replace("'", "''");
            return "'"+asStr+"'";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        // geometric special cases
        if (value instanceof GeographicBoundingBox) {
            value = new GeneralEnvelope((GeographicBoundingBox) value);
        }
        if (value instanceof Envelope) {
            value = asGeometry((Envelope) value);
        }
        if (value instanceof Geometry) {
            return format((Geometry) value);
        }

        throw new UnsupportedOperationException("Not supported yet: Literal value of type "+value.getClass());
    }

    /**
     * Beware ! This implementation is a naïve one, expecting given property name to match exactly SQL database names.
     * In the future, it would be appreciable to be able to configure a mapper between feature and SQL names.
     * @param candidate The property name to parse.
     * @return The SQL representation of the given name.
     */
    protected static CharSequence format(PropertyName candidate) {
        final GenericName pName = Names.parseGenericName(null, ":", candidate.getPropertyName());
        return pName.getParsedNames().stream()
                .map(LocalName::toString)
                .collect(Collectors.joining("\".\"", "\"", "\""));
    }

    protected CharSequence join(final BinaryLogicOperator filter, String separator, Object extraData) {
        final List<Filter> subFilters = filter.getChildren();
        if (subFilters == null || subFilters.isEmpty()) return "";
        return subFilters.stream()
                .map(sub -> sub.accept(this, extraData))
                .filter(ANSIInterpreter::isNonEmptyText)
                .map( result -> (CharSequence) result)
                .collect(Collectors.joining(separator, "(", ")"));
    }

    protected CharSequence joinMatchCase(BinaryComparisonOperator filter, String operator, Object extraData) {
        ensureMatchCase(filter);
        return join(filter, operator, extraData);
    }

    protected CharSequence join(BinaryComparisonOperator candidate, String operator, Object extraData) {
        return join(candidate::getExpression1, candidate::getExpression2, operator, extraData);
    }

    protected CharSequence join(BinaryExpression candidate, String operator, Object extraData) {
        return join(candidate::getExpression1, candidate::getExpression2, operator, extraData);
    }


    protected CharSequence join(BinarySpatialOperator candidate, String operator, Object extraData) {
        return join(candidate::getExpression1, candidate::getExpression2, operator, extraData);
    }

    protected CharSequence join(
            Supplier<Expression> leftOperand,
            Supplier<Expression> rightOperand,
            String operator, Object extraData
    ) {
        return "("
                + evaluateMandatory(leftOperand.get(), extraData)
                + operator
                + evaluateMandatory(rightOperand.get(), extraData)
                + ")";
    }

    protected CharSequence function(Object extraData, final String fnName, Supplier<Expression>... parameters) {
        return Arrays.stream(parameters)
                .map(Supplier::get)
                .map(exp -> evaluateMandatory(exp, extraData))
                .collect(Collectors.joining(", ", fnName+'(', ")"));
    }

    private CharSequence function(String fnName, BinarySpatialOperator filter, Object extraData) {
        return function(extraData, fnName, filter::getExpression1, filter::getExpression2);
    }

    protected CharSequence evaluateMandatory(final Filter candidate, Object extraData) {
        final Object exp = candidate == null ? null : candidate.accept(this, extraData);
        return asNonEmptyText(exp)
                .orElseThrow(() -> new IllegalArgumentException("Filter evaluate to an empty text: "+candidate));
    }

    protected CharSequence evaluateMandatory(final Expression candidate, Object extraData) {
        final Object exp = candidate == null ? null : candidate.accept(this, extraData);
        return asNonEmptyText(exp)
                .orElseThrow(() -> new IllegalArgumentException("Expression evaluate to an empty text: "+candidate));
    }

    protected static Optional<CharSequence> asNonEmptyText(final Object toCheck) {
        if (toCheck instanceof CharSequence) {
            final CharSequence asCS = (CharSequence) toCheck;
            if (asCS.length() > 0) return Optional.of(asCS);
        }

        return Optional.empty();
    }

    protected static boolean isNonEmptyText(final Object toCheck) {
        return asNonEmptyText(toCheck).isPresent();
    }

    private static void ensureMatchCase(BinaryComparisonOperator filter) {
        ensureMatchCase(filter::isMatchingCase);
    }
    private static void ensureMatchCase(BooleanSupplier filter) {
        if (!filter.getAsBoolean())
            throw new UnsupportedOperationException("case insensitive match is not defined by ANSI SQL");
    }

    protected static CharSequence append(CharSequence toAdd, Object extraData) {
        if (extraData instanceof StringBuilder) return ((StringBuilder) extraData).append(toAdd);
        return toAdd;
    }

    protected static Geometry asGeometry(final Envelope source) {
        final double[] lower = source.getLowerCorner().getCoordinate();
        final double[] upper = source.getUpperCorner().getCoordinate();
        for (int i = 0 ; i < lower.length ; i++) {
            if (Double.isNaN(lower[i]) || Double.isNaN(upper[i])) {
                throw new IllegalArgumentException("Cannot use envelope containing NaN for filter");
            }
            lower[i] = clampInfinity(lower[i]);
            upper[i] = clampInfinity(upper[i]);
        }
        final GeneralEnvelope env = new GeneralEnvelope(lower, upper);
        env.setCoordinateReferenceSystem(source.getCoordinateReferenceSystem());
        return Geometries.toGeometry(env, WrapResolution.SPLIT)
                .orElseThrow(() -> new UnsupportedOperationException("No geometry implementation available"));
    }

    protected static CharSequence format(final Geometry source) {
        // TODO: find a better approximation of desired "flatness"
        final Envelope env = source.getEnvelope();
        final double flatness = 0.05 * IntStream.range(0, env.getDimension())
                .mapToDouble(env::getSpan)
                .average()
                .orElseThrow(() -> new IllegalArgumentException("Given geometry envelope dimension is 0"));
        return new StringBuilder("ST_GeomFromText('")
                .append(Geometries.formatWKT(source, flatness))
                .append("')");
    }

    protected static double clampInfinity(final double candidate) {
        if (candidate == Double.NEGATIVE_INFINITY) {
            return -Double.MAX_VALUE;
        } else if (candidate == Double.POSITIVE_INFINITY) {
            return Double.MAX_VALUE;
        }

        return candidate;
    }
}
