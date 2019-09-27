package org.apache.sis.internal.sql.feature;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import org.opengis.util.GenericName;
import org.opengis.util.LocalName;

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
 * TODO: define a set of accepter property names, so any {@link PropertyName} filter refering to non pure SQL property
 * (like relations) will cause a failure.
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
        ensureNonNull("Literal value formmatter", valueFormatter);
        ensureNonNull("Property name formmatter", nameFormatter);
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
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
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
        return evaluateMandatory(filter.getExpression(), extraData) + " = NULL";
    }

    @Override
    public Object visit(PropertyIsNil filter, Object extraData) {
        return evaluateMandatory(filter.getExpression(), extraData) + " = NULL";
    }

    @Override
    public Object visit(BBOX filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Beyond filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Contains filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Crosses filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Disjoint filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(DWithin filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Equals filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Intersects filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Overlaps filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Touches filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

    @Override
    public Object visit(Within filter, Object extraData) {
        throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
    }

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
        final Object value = candidate == null ? null : candidate.getValue();
        if (value == null) return "NULL";
        else if (value instanceof CharSequence) {
            final String asStr = value.toString();
            asStr.replace("'", "''");
            return "'"+asStr+"'";
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

    protected CharSequence evaluateMandatory(final Filter candidate, Object extraData) {
        final Object exp = candidate == null ? null : candidate.accept(this, extraData);
        if (isNonEmptyText(exp)) return (CharSequence) exp;
        else throw new IllegalArgumentException("Filter evaluate to an empty text: "+candidate);
    }

    protected CharSequence evaluateMandatory(final Expression candidate, Object extraData) {
        final Object exp = candidate == null ? null : candidate.accept(this, extraData);
        if (isNonEmptyText(exp)) return (CharSequence) exp;
        else throw new IllegalArgumentException("Expression evaluate to an empty text: "+candidate);
    }

    protected static boolean isNonEmptyText(final Object toCheck) {
        return toCheck instanceof CharSequence && ((CharSequence) toCheck).length() > 0;
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
}
