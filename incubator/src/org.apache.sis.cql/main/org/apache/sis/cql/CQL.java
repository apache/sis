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
package org.apache.sis.cql;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.ArrayList;
import java.time.temporal.TemporalAccessor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.ValueReference;
import org.opengis.filter.Literal;
import org.opengis.filter.SortOrder;
import org.opengis.filter.SortProperty;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Quantities;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.cql.internal.AntlrCQL;
import org.apache.sis.cql.internal.CQLParser.CoordinateContext;
import org.apache.sis.cql.internal.CQLParser.CoordinateSerieContext;
import org.apache.sis.cql.internal.CQLParser.CoordinateSeriesContext;
import org.apache.sis.cql.internal.CQLParser.ExpressionContext;
import org.apache.sis.cql.internal.CQLParser.ExpressionFctParamContext;
import org.apache.sis.cql.internal.CQLParser.ExpressionGeometryContext;
import org.apache.sis.cql.internal.CQLParser.ExpressionNumContext;
import org.apache.sis.cql.internal.CQLParser.ExpressionTermContext;
import org.apache.sis.cql.internal.CQLParser.ExpressionUnaryContext;
import org.apache.sis.cql.internal.CQLParser.FilterContext;
import org.apache.sis.cql.internal.CQLParser.FilterGeometryContext;
import org.apache.sis.cql.internal.CQLParser.FilterTermContext;
import org.apache.sis.temporal.LenientDateFormat;
import static org.apache.sis.cql.internal.CQLParser.*;


/**
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class CQL {

    private static final GeometryFactory GF = org.apache.sis.geometry.wrapper.jts.Factory.INSTANCE.factory(false);

    private CQL() {
    }

    public static Expression<Feature,?> parseExpression(String cql) throws CQLException {
        return parseExpression(cql, null);
    }

    public static Expression<Feature,?> parseExpression(String cql, FilterFactory<Feature,?,?> factory) throws CQLException {
        final Object obj = AntlrCQL.compileExpression(cql);
        Expression<Feature,?> result = null;
        if (obj instanceof ExpressionContext) {
            ParseTree tree = (ParseTree) obj;
            if (factory == null) {
                factory = DefaultFilterFactory.forFeatures();
            }
            result = convertExpression(tree, factory);
        }
        return result;
    }

    public static Filter<Feature> parseFilter(String cql) throws CQLException {
        return parseFilter(cql, null);
    }

    public static Filter<Feature> parseFilter(String cql, FilterFactory<Feature,Object,Object> factory) throws CQLException {
        cql = cql.trim();

        // Bypass parsing for inclusive filter.
        if (cql.isEmpty() || "*".equals(cql)) {
            return Filter.include();
        }
        final Object obj = AntlrCQL.compileFilter(cql);
        Filter<Feature> result = null;
        if (obj instanceof FilterContext) {
            ParseTree tree = (ParseTree) obj;
            if (factory == null) {
                factory = DefaultFilterFactory.forFeatures();
            }
            result = convertFilter(tree, factory);
        }
        return result;
    }

    public static Query parseQuery(String cql) throws CQLException {
        return parseQuery(cql, null);
    }

    public static Query parseQuery(String cql, FilterFactory<Feature,Object,Object> factory) throws CQLException {
        final Object obj = AntlrCQL.compileQuery(cql);
        Query result = null;
        if (obj instanceof QueryContext) {
            ParseTree tree = (ParseTree) obj;
            if (factory == null) {
                factory = DefaultFilterFactory.forFeatures();
            }
            result = convertQuery(tree, factory);
        }
        return result;
    }

    public static String write(final Filter<Feature> filter) {
        final StringBuilder sb = new StringBuilder();
        FilterToCQLVisitor.INSTANCE.visit(filter, sb);
        return sb.toString();
    }

    public static String write(final Expression<Feature,?> exp) {
        final StringBuilder sb = new StringBuilder();
        FilterToCQLVisitor.INSTANCE.visit(exp, sb);
        return sb.toString();
    }

    public static String write(final Query query) {
        final StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        if (query.projections.isEmpty()) {
            sb.append('*');
        } else {
            for (int i = 0, n = query.projections.size(); i < n; i++) {
                Query.Projection p = query.projections.get(i);
                if (i != 0) sb.append(", ");
                sb.append(write(p.expression));
                if (p.alias != null && !p.alias.isEmpty()) {
                    sb.append(" AS '");
                    sb.append(p.alias);
                    sb.append('\'');
                }
            }
        }
        if (query.filter != null) {
            sb.append(" WHERE ");
            sb.append(write(query.filter));
        }

        if (!query.sortby.isEmpty()) {
            sb.append(" ORDER BY ");
            for (int i = 0, n = query.sortby.size(); i < n; i++) {
                SortProperty p = query.sortby.get(i);
                if (i != 0) sb.append(", ");
                sb.append(write(p.getValueReference()));
                switch (p.getSortOrder()) {
                    case ASCENDING : sb.append(" ASC"); break;
                    case DESCENDING : sb.append(" DESC"); break;
                }
            }
        }

        if (query.offset != null) {
            sb.append(" OFFSET ");
            sb.append(query.offset);
        }
        if (query.limit != null) {
            sb.append(" LIMIT ");
            sb.append(query.limit);
        }

        return sb.toString();
    }

    /**
     * Convert the given tree in an Expression.
     */
    private static Expression<Feature,?> convertExpression(ParseTree tree, FilterFactory<Feature,?,?> ff) throws CQLException {
        if (tree instanceof ExpressionContext) {
            //: expression MULT expression
            //| expression UNARY expression
            //| expressionTerm
            if (tree.getChildCount() == 3) {
                final String operand = tree.getChild(1).getText();
                // TODO: unsafe cast.
                final Expression<Feature, ? extends Number> left  = (Expression<Feature, ? extends Number>) convertExpression(tree.getChild(0), ff);
                final Expression<Feature, ? extends Number> right = (Expression<Feature, ? extends Number>) convertExpression(tree.getChild(2), ff);
                if ("*".equals(operand)) {
                    return ff.multiply(left, right);
                } else if ("/".equals(operand)) {
                    return ff.divide(left, right);
                } else if ("+".equals(operand)) {
                    return ff.add(left, right);
                } else if ("-".equals(operand)) {
                    return ff.subtract(left, right);
                }
            } else {
                return convertExpression(tree.getChild(0), ff);
            }
        } //        else if(tree instanceof ExpressionStringContext){
        //            //strip start and end '
        //            final String text = tree.getText();
        //            return ff.literal(text.substring(1, text.length()-1));
        //        }
        else if (tree instanceof ExpressionTermContext) {
            //: expressionString
            //| expressionUnary
            //| PROPERTY_NAME
            //| DATE
            //| DURATION_P
            //| DURATION_T
            //| NAME (LPAREN expressionFctParam? RPAREN)?
            //| expressionGeometry
            //| LPAREN expression RPAREN

            //: TEXT
            //| expressionUnary
            //| PROPERTY_NAME
            //| DATE
            //| DURATION_P
            //| DURATION_T
            //| expressionGeometry
            final ExpressionTermContext exp = (ExpressionTermContext) tree;
            if (exp.getChildCount() == 1) {
                return convertExpression(tree.getChild(0), ff);
            }
            // LPAREN expression RPAREN
            if (exp.expression() != null) {
                return convertExpression(exp.expression(), ff);
            }
            // NAME (LPAREN expressionFctParam? RPAREN)?
            if (exp.NAME() != null) {
                final String name = exp.NAME().getText();
                final ExpressionFctParamContext prm = exp.expressionFctParam();
                if (prm == null) {
                    //handle as property name
                    return ff.property(name);
                }
                // Handle as a function.
                final List<ExpressionContext> params = prm.expression();
                final List<Expression<Feature,?>> exps = new ArrayList<>();
                for (int i = 0, n = params.size(); i < n; i++) {
                    exps.add(convertExpression(params.get(i), ff));
                }
                return ff.function(name, exps.toArray(new Expression[exps.size()]));
            }
        } else if (tree instanceof ExpressionUnaryContext) {
            //: UNARY? expressionNum ;
            final ExpressionUnaryContext exp = (ExpressionUnaryContext) tree;
            return ff.literal(unaryAsNumber(exp));

        } else if (tree instanceof ExpressionNumContext) {
            //: INT | FLOAT ;
            return convertExpression(tree.getChild(0), ff);
        } else if (tree instanceof TerminalNode) {
            final TerminalNode exp = (TerminalNode) tree;
            switch (exp.getSymbol().getType()) {
                case PROPERTY_NAME: {
                    // strip start and end "
                    final String text = tree.getText();
                    return ff.property(text.substring(1, text.length() - 1));
                }
                case NAME:  return ff.property(tree.getText());
                case INT:   return ff.literal(Integer.valueOf(tree.getText()));
                case FLOAT: return ff.literal(Double.valueOf(tree.getText()));
                case DATE, DATETIME: {
                    TemporalAccessor ta = LenientDateFormat.parseBest(tree.getText());
                    return ff.literal(ta);
                    // TODO: return ff.literal(TemporalObjects.getTimeInMillis(tree.getText()));
                }
                case TEXT: {
                    // strip start and end '
                    String text = tree.getText();
                    text = text.replaceAll("\\\\'", "'");
                    return ff.literal(text.substring(1, text.length() - 1));
                }
                case DURATION_P :
                    return ff.literal(Period.parse(tree.getText()));
                case DURATION_T :
                    return ff.literal(Duration.parse(tree.getText()));
            }
        } else if (tree instanceof ExpressionGeometryContext) {
            //: POINT ( EMPTY | coordinateSerie )
            //| LINESTRING ( EMPTY | coordinateSerie )
            //| POLYGON ( EMPTY | coordinateSeries )
            //| MPOINT ( EMPTY | coordinateSerie )
            //| MLINESTRING  ( EMPTY | coordinateSeries )
            //| MPOLYGON ( EMPTY | LPAREN coordinateSeries (COMMA coordinateSeries)* RPAREN )
            //| GEOMETRYCOLLECTION ( EMPTY | (LPAREN expressionGeometry (COMMA expressionGeometry)* RPAREN) )
            //| ENVELOPE ( EMPTY | (LPAREN expressionUnary COMMA expressionUnary COMMA expressionUnary COMMA expressionUnary RPAREN) )
            final ExpressionGeometryContext exp = (ExpressionGeometryContext) tree;
            switch (((TerminalNode) exp.getChild(0)).getSymbol().getType()) {
                case POINT: {
                    final ParseTree st = tree.getChild(1);
                    final CoordinateSequence cs;
                    if (isEmptyToken(st)) {
                        cs = GF.getCoordinateSequenceFactory().create(new Coordinate[0]);
                    } else {
                        cs = parseSequence(st);
                    }
                    final Geometry geom = GF.createPoint(cs);
                    return ff.literal(geom);
                }
                case LINESTRING: {
                    final ParseTree st = tree.getChild(1);
                    final CoordinateSequence cs;
                    if (isEmptyToken(st)) {
                        cs = GF.getCoordinateSequenceFactory().create(new Coordinate[0]);
                    } else {
                        cs = parseSequence(st);
                    }
                    final Geometry geom = GF.createLineString(cs);
                    return ff.literal(geom);
                }
                case POLYGON: {
                    final ParseTree st = tree.getChild(1);
                    final Geometry geom;
                    if (isEmptyToken(st)) {
                        geom = GF.createPolygon(GF.createLinearRing(new Coordinate[0]), new LinearRing[0]);
                    } else {
                        final CoordinateSeriesContext series = (CoordinateSeriesContext) st;
                        final List<CoordinateSerieContext> subs = series.coordinateSerie();
                        final LinearRing contour = GF.createLinearRing(parseSequence(subs.get(0)));
                        final int n = subs.size();
                        final LinearRing[] holes = new LinearRing[n - 1];
                        for (int i = 1; i < n; i++) {
                            holes[i - 1] = GF.createLinearRing(parseSequence(subs.get(i)));
                        }
                        geom = GF.createPolygon(contour, holes);
                    }
                    return ff.literal(geom);
                }
                case MPOINT: {
                    final ParseTree st = tree.getChild(1);
                    final CoordinateSequence cs;
                    if (isEmptyToken(st)) {
                        cs = GF.getCoordinateSequenceFactory().create(new Coordinate[0]);
                    } else {
                        cs = parseSequence(st);
                    }
                    final Geometry geom = GF.createMultiPoint(cs);
                    return ff.literal(geom);
                }
                case MLINESTRING: {
                    final ParseTree st = tree.getChild(1);
                    final Geometry geom;
                    if (isEmptyToken(st)) {
                        geom = GF.createMultiLineString(new LineString[0]);
                    } else {
                        final CoordinateSeriesContext series = (CoordinateSeriesContext) st;
                        final List<CoordinateSerieContext> subs = series.coordinateSerie();
                        final int n = subs.size();
                        final LineString[] strings = new LineString[n];
                        for (int i = 0; i < n; i++) {
                            strings[i] = GF.createLineString(parseSequence(subs.get(i)));
                        }
                        geom = GF.createMultiLineString(strings);
                    }
                    return ff.literal(geom);
                }
                case MPOLYGON: {
                    final ParseTree st = tree.getChild(1);
                    final Geometry geom;
                    if (isEmptyToken(st)) {
                        geom = GF.createMultiPolygon(new Polygon[0]);
                    } else {
                        final List<CoordinateSeriesContext> eles = exp.coordinateSeries();
                        final int n = eles.size();
                        final Polygon[] polygons = new Polygon[n];
                        for (int i=0; i<n; i++) {
                            final CoordinateSeriesContext polyTree = eles.get(i);
                            final List<CoordinateSerieContext> subs = polyTree.coordinateSerie();
                            final LinearRing contour = GF.createLinearRing(parseSequence(subs.get(0)));
                            final int hn = subs.size();
                            final LinearRing[] holes = new LinearRing[hn - 1];
                            for (int j=1; j<hn; j++) {
                                holes[j-1] = GF.createLinearRing(parseSequence(subs.get(j)));
                            }
                            final Polygon poly = GF.createPolygon(contour, holes);
                            polygons[i] = poly;
                        }
                        geom = GF.createMultiPolygon(polygons);
                    }
                    return ff.literal(geom);
                }
                case GEOMETRYCOLLECTION: {
                    final ParseTree st = tree.getChild(1);
                    final Geometry geom;
                    if (isEmptyToken(st)) {
                        geom = GF.createGeometryCollection(new Geometry[0]);
                    } else {
                        final List<ExpressionGeometryContext> eles = exp.expressionGeometry();
                        final int n = eles.size();
                        final Geometry[] subs = new Geometry[n];
                        for (int i=0; i<n; i++) {
                            final ParseTree subTree = eles.get(i);
                            final Geometry sub = (Geometry) convertExpression(subTree, ff).apply(null);
                            subs[i] = sub;
                        }
                        geom = GF.createGeometryCollection(subs);
                    }
                    return ff.literal(geom);
                }
                case ENVELOPE: {
                    final ParseTree st = tree.getChild(1);
                    final Geometry geom;
                    if (isEmptyToken(st)) {
                        geom = GF.createPolygon(GF.createLinearRing(new Coordinate[0]), new LinearRing[0]);
                    } else {
                        final List<ExpressionUnaryContext> unaries = exp.expressionUnary();
                        final double west  = unaryAsNumber(unaries.get(0)).doubleValue();
                        final double east  = unaryAsNumber(unaries.get(1)).doubleValue();
                        final double north = unaryAsNumber(unaries.get(2)).doubleValue();
                        final double south = unaryAsNumber(unaries.get(3)).doubleValue();
                        final LinearRing contour = GF.createLinearRing(new Coordinate[]{
                            new Coordinate(west, north),
                            new Coordinate(east, north),
                            new Coordinate(east, south),
                            new Coordinate(west, south),
                            new Coordinate(west, north)
                        });
                        geom = GF.createPolygon(contour, new LinearRing[0]);
                    }
                    return ff.literal(geom);
                }
            }
            return convertExpression(tree.getChild(0), ff);
        }
        throw new CQLException("Unreconized expression : type=" + tree.getText());
    }

    private static boolean isEmptyToken(ParseTree tree) {
        return tree instanceof TerminalNode && ((TerminalNode) tree).getSymbol().getType() == EMPTY;
    }

    private static Number unaryAsNumber(ExpressionUnaryContext exp) {
        //: UNARY? expressionNum ;
        final boolean negate = (exp.UNARY() != null && exp.UNARY().getSymbol().getText().equals("-"));
        final ExpressionNumContext num = exp.expressionNum();
        if (num.INT() != null) {
            int val = Integer.valueOf(num.INT().getText());
            return negate ? -val : val;
        } else {
            double val = Double.valueOf(num.FLOAT().getText());
            return negate ? -val : val;
        }
    }

    private static CoordinateSequence parseSequence(ParseTree tree) {
        final CoordinateSerieContext exp = (CoordinateSerieContext) tree;
        final List<CoordinateContext> lst = exp.coordinate();
        final int size = lst.size();
        final Coordinate[] coords = new Coordinate[size];
        for (int i = 0; i < size; i++) {
            final CoordinateContext cc = lst.get(i);
            coords[i] = new Coordinate(
                    unaryAsNumber(cc.expressionUnary(0)).doubleValue(),
                    unaryAsNumber(cc.expressionUnary(1)).doubleValue());
        }
        return GF.getCoordinateSequenceFactory().create(coords);
    }

    private static Unit<Length> parseLengthUnit(final Expression<Feature,?> unitExp) {
        Object value = unitExp.apply(null);
        if (value != null) {
            return Units.ensureLinear(Units.valueOf(value.toString()));
        }
        if (unitExp instanceof ValueReference<?,?>) {
            value = ((ValueReference<?,?>) unitExp).getXPath();
        }
        throw new IllegalArgumentException("Unit `" + value + "` is not a literal.");
    }

    /**
     * Convert the given tree in a Filter.
     */
    private static Filter<Feature> convertFilter(ParseTree tree, FilterFactory<Feature,Object,Object> ff) throws CQLException {
        if (tree instanceof FilterContext) {
            //: filter (AND filter)+
            //| filter (OR filter)+
            //| LPAREN filter RPAREN
            //| NOT filterTerm
            //| filterTerm

            final FilterContext exp = (FilterContext) tree;

            //| filterTerm
            if (exp.getChildCount() == 1) {
                return convertFilter(tree.getChild(0), ff);
            } else if (exp.NOT() != null) {
                //| NOT (filterTerm | ( LPAREN filter RPAREN ))
                if (exp.filterTerm() != null) {
                    return ff.not(convertFilter(exp.filterTerm(), ff));
                } else {
                    return ff.not(convertFilter(exp.filter(0), ff));
                }

            } else if (!exp.AND().isEmpty()) {
                //: filter (AND filter)+
                final List<Filter<Feature>> subs = new ArrayList<>();
                for (FilterContext f : exp.filter()) {
                    final Filter<Feature> sub = convertFilter(f, ff);
                    if (sub.getOperatorType() == LogicalOperatorName.AND) {
                        subs.addAll(((LogicalOperator<Feature>) sub).getOperands());
                    } else {
                        subs.add(sub);
                    }
                }
                return ff.and(subs);
            } else if (!exp.OR().isEmpty()) {
                //| filter (OR filter)+
                final List<Filter<Feature>> subs = new ArrayList<>();
                for (FilterContext f : exp.filter()) {
                    final Filter<Feature> sub = convertFilter(f, ff);
                    if (sub.getOperatorType() == LogicalOperatorName.OR) {
                        subs.addAll(((LogicalOperator<Feature>) sub).getOperands());
                    } else {
                        subs.add(sub);
                    }
                }
                return ff.or(subs);
            } else if (exp.LPAREN() != null) {
                //| LPAREN filter RPAREN
                return convertFilter(exp.filter(0), ff);
            }
        } else if (tree instanceof FilterTermContext) {
            //: expression
            //    (
            //              COMPARE  expression
            //            | NOT? IN LPAREN (expressionFctParam )?  RPAREN
            //            | BETWEEN expression AND expression
            //            | NOT? LIKE expression
            //            | NOT? ILIKE expression
            //            | IS NOT? NULL
            //            | AFTER expression
            //            | ANYINTERACTS expression
            //            | BEFORE expression
            //            | BEGINS expression
            //            | BEGUNBY expression
            //            | DURING expression
            //            | ENDEDBY expression
            //            | ENDS expression
            //            | MEETS expression
            //            | METBY expression
            //            | OVERLAPPEDBY expression
            //            | TCONTAINS expression
            //            | TEQUALS expression
            //            | TOVERLAPS expression
            //    )
            //| filterGeometry

            final FilterTermContext exp = (FilterTermContext) tree;
            final List<ExpressionContext> exps = exp.expression();
            if (exp.COMPARE() != null) {
                // expression COMPARE expression
                final String text = exp.COMPARE().getText();
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                if ("=".equals(text)) {
                    return ff.equal(left, right);
                } else if ("<>".equals(text)) {
                    return ff.notEqual(left, right);
                } else if (">".equals(text)) {
                    return ff.greater(left, right);
                } else if ("<".equals(text)) {
                    return ff.less(left, right);
                } else if (">=".equals(text)) {
                    return ff.greaterOrEqual(left, right);
                } else if ("<=".equals(text)) {
                    return ff.lessOrEqual(left, right);
                }
            } else if (exp.IN() != null) {
                // expression NOT? IN LPAREN (expressionFctParam )?  RPAREN
                final Expression<Feature,?> val = convertExpression(exps.get(0), ff);
                final ExpressionFctParamContext prm = exp.expressionFctParam();
                final List<ExpressionContext> params = prm.expression();
                final List<Expression<Feature,?>> subexps = new ArrayList<>();
                for (int i = 0, n = params.size(); i < n; i++) {
                    subexps.add(convertExpression(params.get(i), ff));
                }
                final int size = subexps.size();
                final Filter<Feature> selection;
                switch (size) {
                    case 0: {
                        selection = Filter.exclude();
                        break;
                    }
                    case 1: {
                        selection = ff.equal(val, subexps.get(0));
                        break;
                    }
                    default: {
                        final List<Filter<Feature>> filters = new ArrayList<>();
                        for (Expression<Feature,?> e : subexps) {
                            filters.add(ff.equal(val, e));
                        }   selection = ff.or(filters);
                        break;
                    }
                }
                if (exp.NOT() != null) {
                    return ff.not(selection);
                } else {
                    return selection;
                }
            } else if (exp.BETWEEN() != null) {
                // expression BETWEEN expression AND expression
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                final Expression<Feature,?> exp3 = convertExpression(exps.get(2), ff);
                return ff.between(exp1, exp2, exp3);
            } else if (exp.LIKE() != null) {
                // expression NOT? LIKE expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                if (exp.NOT() != null) {
                    return ff.not(ff.like(left, right.apply(null).toString(), '%', '_', '\\', true));
                } else {
                    return ff.like(left, right.apply(null).toString(), '%', '_', '\\', true);
                }
            } else if (exp.ILIKE() != null) {
                // expression NOT? LIKE expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                if (exp.NOT() != null) {
                    return ff.not(ff.like(left, right.apply(null).toString(), '%', '_', '\\', false));
                } else {
                    return ff.like(left, right.apply(null).toString(), '%', '_', '\\', false);
                }
            } else if (exp.IS() != null) {
                // expression IS NOT? NULL
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                if (exp.NOT() != null) {
                    return ff.not(ff.isNull(exp1));
                } else {
                    return ff.isNull(exp1);
                }
            } else if (exp.AFTER() != null) {
                // expression AFTER expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.after(left, right);
            } else if (exp.ANYINTERACTS() != null) {
                // expression ANYINTERACTS expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.anyInteracts(left, right);
            } else if (exp.BEFORE() != null) {
                // expression BEFORE expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.before(left, right);
            } else if (exp.BEGINS() != null) {
                // expression BEGINS expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.begins(left, right);
            } else if (exp.BEGUNBY() != null) {
                // expression BEGUNBY expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.begunBy(left, right);
            } else if (exp.DURING() != null) {
                // expression DURING expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.during(left, right);
            } else if (exp.ENDEDBY() != null) {
                // expression ENDEDBY expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.endedBy(left, right);
            } else if (exp.ENDS() != null) {
                // expression ENDS expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.ends(left, right);
            } else if (exp.MEETS() != null) {
                // expression MEETS expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.meets(left, right);
            } else if (exp.METBY() != null) {
                // expression METBY expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.metBy(left, right);
            } else if (exp.OVERLAPPEDBY() != null) {
                // expression OVERLAPPEDBY expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.overlappedBy(left, right);
            } else if (exp.TCONTAINS() != null) {
                // expression TCONTAINS expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.tcontains(left, right);
            } else if (exp.TEQUALS() != null) {
                // expression TEQUALS expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.tequals(left, right);
            } else if (exp.TOVERLAPS() != null) {
                // expression TOVERLAPS expression
                final Expression<Feature,?> left  = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(exps.get(1), ff);
                return ff.toverlaps(left, right);
            } else if (exp.filterGeometry() != null) {
                // expression filterGeometry
                return convertFilter(exp.filterGeometry(), ff);
            }
        } else if (tree instanceof FilterGeometryContext) {
            //: BBOX LPAREN (PROPERTY_NAME|NAME) COMMA expressionUnary COMMA expressionUnary COMMA expressionUnary COMMA expressionUnary (COMMA TEXT)? RPAREN
            //| BEYOND LPAREN expression COMMA expression COMMA expression COMMA expression RPAREN
            //| CONTAINS LPAREN expression COMMA expression RPAREN
            //| CROSSES LPAREN expression COMMA expression RPAREN
            //| DISJOINT LPAREN expression COMMA expression RPAREN
            //| DWITHIN LPAREN expression COMMA expression COMMA expression COMMA expression RPAREN
            //| EQUALS LPAREN expression COMMA expression RPAREN
            //| INTERSECTS LPAREN expression COMMA expression RPAREN
            //| OVERLAPS LPAREN expression COMMA expression RPAREN
            //| TOUCHES LPAREN expression COMMA expression RPAREN
            //| WITHIN LPAREN expression COMMA expression RPAREN

            final FilterGeometryContext exp = (FilterGeometryContext) tree;
            final List<ExpressionContext> exps = exp.expression();
            if (exp.BBOX() != null) {
                final Expression<Feature,?> prop = convertExpression(exps.get(0), ff);
                final double v1 = unaryAsNumber(exp.expressionUnary(0)).doubleValue();
                final double v2 = unaryAsNumber(exp.expressionUnary(1)).doubleValue();
                final double v3 = unaryAsNumber(exp.expressionUnary(2)).doubleValue();
                final double v4 = unaryAsNumber(exp.expressionUnary(3)).doubleValue();
                GeneralEnvelope env;
                if (exp.TEXT() != null) try {
                    String crs = convertExpression(exp.TEXT(), ff).apply(null).toString();
                    env = new GeneralEnvelope(CRS.forCode(crs));
                } catch (FactoryException e) {
                    throw new CQLException("Cannot parse CRS code.", e);
                } else {
                    env = new GeneralEnvelope(2);
                }
                env.setRange(0, v1, v3);
                env.setRange(1, v2, v4);
                return ff.bbox(prop, env);
            } else if (exp.BEYOND() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                final double distance = ((Number) convertExpression(exps.get(2), ff).apply(null)).doubleValue();
                final Unit<Length> unit = parseLengthUnit(convertExpression(exps.get(3), ff));
                return ff.beyond(exp1, exp2, Quantities.create(distance, unit));
            } else if (exp.CONTAINS() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                return ff.contains(exp1, exp2);
            } else if (exp.CROSSES() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                return ff.crosses(exp1, exp2);
            } else if (exp.DISJOINT() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                return ff.disjoint(exp1, exp2);
            } else if (exp.DWITHIN() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                final double distance = ((Number) convertExpression(exps.get(2), ff).apply(null)).doubleValue();
                final Unit<Length> unit = parseLengthUnit(convertExpression(exps.get(3), ff));
                return ff.within(exp1, exp2, Quantities.create(distance, unit));
            } else if (exp.EQUALS() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                return ff.equals(exp1, exp2);
            } else if (exp.INTERSECTS() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                return ff.intersects(exp1, exp2);
            } else if (exp.OVERLAPS() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                return ff.overlaps(exp1, exp2);
            } else if (exp.TOUCHES() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                return ff.touches(exp1, exp2);
            } else if (exp.WITHIN() != null) {
                final Expression<Feature,?> exp1 = convertExpression(exps.get(0), ff);
                final Expression<Feature,?> exp2 = convertExpression(exps.get(1), ff);
                return ff.within(exp1, exp2);
            }
        }
        throw new CQLException("Unreconized filter : type=" + tree.getText());
    }

    /**
     * Convert the given tree in a Query.
     */
    private static Query convertQuery(ParseTree tree, FilterFactory<Feature,Object,Object> ff) throws CQLException {
        if (tree instanceof QueryContext) {
            final QueryContext context = (QueryContext) tree;
            final List<ProjectionContext> projections = context.projection();
            final WhereContext where = context.where();
            final OffsetContext offset = context.offset();
            final LimitContext limit = context.limit();
            final OrderbyContext orderby = context.orderby();

            final Query query = new Query();
            if (context.MULT() == null) {
                for (ProjectionContext pc : projections) {
                    final Expression<Feature,?> exp = convertExpression(pc.expression(), ff);
                    if (pc.AS() != null) {
                        final Expression<Feature,?> alias = convertExpression(pc.TEXT(), ff);
                        query.projections.add(new Query.Projection(exp, String.valueOf(( (Literal) alias).getValue())));
                    } else {
                        query.projections.add(new Query.Projection(exp, null));
                    }
                }
            }
            if (where != null) {
                query.filter = (Filter<Feature>) convertFilter(where.filter(), ff);
            }
            if (offset != null) {
                query.offset = Integer.valueOf(offset.INT().getText());
            }
            if (limit != null) {
                query.limit = Integer.valueOf(limit.INT().getText());
            }
            if (orderby != null) {
                for (SortpropContext spc : orderby.sortprop()) {
                    final Expression<Feature,?> exp = convertExpression(spc.expression(), ff);
                    if (exp instanceof ValueReference) {
                        query.sortby.add(ff.sort((ValueReference<Feature,?>) exp,
                                spc.DESC() != null ? SortOrder.DESCENDING : SortOrder.ASCENDING));
                    } else {
                        throw new CQLException("Sort by may be used with property names only");
                    }
                }
            }
            return query;
        }
        return null;
    }
}
