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

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.internal.cql.CQLLexer;
import org.apache.sis.internal.cql.CQLParser;
import static org.apache.sis.internal.cql.CQLParser.*;
import org.apache.sis.internal.cql.CQLParser.CoordinateContext;
import org.apache.sis.internal.cql.CQLParser.CoordinateSerieContext;
import org.apache.sis.internal.cql.CQLParser.CoordinateSeriesContext;
import org.apache.sis.internal.cql.CQLParser.ExpressionContext;
import org.apache.sis.internal.cql.CQLParser.ExpressionFctParamContext;
import org.apache.sis.internal.cql.CQLParser.ExpressionGeometryContext;
import org.apache.sis.internal.cql.CQLParser.ExpressionNumContext;
import org.apache.sis.internal.cql.CQLParser.ExpressionTermContext;
import org.apache.sis.internal.cql.CQLParser.ExpressionUnaryContext;
import org.apache.sis.internal.cql.CQLParser.FilterContext;
import org.apache.sis.internal.cql.CQLParser.FilterGeometryContext;
import org.apache.sis.internal.cql.CQLParser.FilterOrExpressionContext;
import org.apache.sis.internal.cql.CQLParser.FilterTermContext;
import org.apache.sis.internal.util.StandardDateFormat;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Or;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
public final class CQL {

    private static final GeometryFactory GF = new GeometryFactory();

    private CQL() {
    }

    private static Object compileExpression(String cql) {
        try {
            //lexer splits input into tokens
            final CodePointCharStream input = CharStreams.fromString(cql);
            final TokenStream tokens = new CommonTokenStream(new CQLLexer(input));

            //parser generates abstract syntax tree
            final CQLParser parser = new CQLParser(tokens);
            final ExpressionContext ctx = parser.expression();
            return ctx;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }

    private static Object compileFilter(String cql) {
        try {
            //lexer splits input into tokens
            final CodePointCharStream input = CharStreams.fromString(cql);
            final TokenStream tokens = new CommonTokenStream(new CQLLexer(input));

            //parser generates abstract syntax tree
            final CQLParser parser = new CQLParser(tokens);
            final FilterContext retfilter = parser.filter();

            return retfilter;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }

    private static Object compileFilterOrExpression(String cql) {
        try {
            //lexer splits input into tokens
            final CodePointCharStream input = CharStreams.fromString(cql);
            final TokenStream tokens = new CommonTokenStream(new CQLLexer(input));

            //parser generates abstract syntax tree
            final CQLParser parser = new CQLParser(tokens);
            final FilterOrExpressionContext retfilter = parser.filterOrExpression();

            return retfilter;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }

    public static ParseTree compile(String cql) {
        final Object obj = compileFilterOrExpression(cql);

        ParseTree tree = null;
        if (obj instanceof ParseTree) {
            tree = (ParseTree) obj;
        }

        return tree;
    }

    public static Expression parseExpression(String cql) throws CQLException {
        return parseExpression(cql, null);
    }

    public static Expression parseExpression(String cql, FilterFactory2 factory) throws CQLException {
        final Object obj = compileExpression(cql);

        ParseTree tree = null;
        Expression result = null;
        if (obj instanceof ExpressionContext) {
            tree = (ParseTree) obj;
            if (factory == null) {
                factory = new DefaultFilterFactory();
            }
            result = convertExpression(tree, factory);
        }

        return result;
    }

    public static Filter parseFilter(String cql) throws CQLException {
        return parseFilter(cql, null);
    }

    public static Filter parseFilter(String cql, FilterFactory2 factory) throws CQLException {
        cql = cql.trim();

        //bypass parsing for inclusive filter
        if (cql.isEmpty() || "*".equals(cql)) {
            return Filter.INCLUDE;
        }

        final Object obj = compileFilter(cql);

        ParseTree tree = null;
        Filter result = null;
        if (obj instanceof FilterContext) {
            tree = (FilterContext) obj;
            if (factory == null) {
                factory = new DefaultFilterFactory();
            }
            result = convertFilter(tree, factory);
        }

        return result;
    }

    public static String write(Filter filter) {
        if (filter == null) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        filter.accept(FilterToCQLVisitor.INSTANCE, sb);
        return sb.toString();
    }

    public static String write(Expression exp) {
        if (exp == null) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        exp.accept(FilterToCQLVisitor.INSTANCE, sb);
        return sb.toString();
    }

    /**
     * Create a TreeNode for the given tree. method is recursive.
     */
    private static DefaultMutableTreeNode explore(ParseTree tree) {
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree);

        final int nb = tree.getChildCount();
        for (int i = 0; i < nb; i++) {
            final DefaultMutableTreeNode n = explore((ParseTree) tree.getChild(i));
            node.add(n);
        }
        return node;
    }

    /**
     * Convert the given tree in an Expression.
     */
    private static Expression convertExpression(ParseTree tree, FilterFactory2 ff) throws CQLException {

        if (tree instanceof ExpressionContext) {
            //: expression MULT expression
            //| expression UNARY expression
            //| expressionTerm
            if (tree.getChildCount() == 3) {
                final String operand = tree.getChild(1).getText();
                final Expression left = convertExpression((ParseTree) tree.getChild(0), ff);
                final Expression right = convertExpression((ParseTree) tree.getChild(2), ff);
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

                //handle as a function
                final List<ExpressionContext> params = prm.expression();
                final List<Expression> exps = new ArrayList<Expression>();
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
            final int type = exp.getSymbol().getType();
            if (PROPERTY_NAME == type) {
                //strip start and end "
                final String text = tree.getText();
                return ff.property(text.substring(1, text.length() - 1));
            } else if (NAME == type) {
                return ff.property(tree.getText());
            } else if (INT == type) {
                return ff.literal(Integer.valueOf(tree.getText()));
            } else if (FLOAT == type) {
                return ff.literal(Double.valueOf(tree.getText()));
            } else if (DATE == type) {
                TemporalAccessor ta = StandardDateFormat.FORMAT.parse(tree.getText());
                return ff.literal(ta);
            } else if (DURATION_P == type || DURATION_T == type) {
                // TODO
                //return ff.literal(TemporalUtilities.getTimeInMillis(tree.getText()));
            } else if (TEXT == type) {
                //strip start and end '
                String text = tree.getText();
                text = text.replaceAll("\\\\'", "'");
                return ff.literal(text.substring(1, text.length() - 1));
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
            final int type = ((TerminalNode) exp.getChild(0)).getSymbol().getType();

            if (POINT == type) {
                final ParseTree st = (ParseTree) tree.getChild(1);
                final CoordinateSequence cs;
                if (isEmptyToken(st)) {
                    cs = GF.getCoordinateSequenceFactory().create(new Coordinate[0]);
                } else {
                    cs = parseSequence(st);
                }
                final Geometry geom = GF.createPoint(cs);
                return ff.literal(geom);
            } else if (LINESTRING == type) {
                final ParseTree st = (ParseTree) tree.getChild(1);
                final CoordinateSequence cs;
                if (isEmptyToken(st)) {
                    cs = GF.getCoordinateSequenceFactory().create(new Coordinate[0]);
                } else {
                    cs = parseSequence(st);
                }
                final Geometry geom = GF.createLineString(cs);
                return ff.literal(geom);
            } else if (POLYGON == type) {
                final ParseTree st = (ParseTree) tree.getChild(1);
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
            } else if (MPOINT == type) {
                final ParseTree st = (ParseTree) tree.getChild(1);
                final CoordinateSequence cs;
                if (isEmptyToken(st)) {
                    cs = GF.getCoordinateSequenceFactory().create(new Coordinate[0]);
                } else {
                    cs = parseSequence(st);
                }
                final Geometry geom = GF.createMultiPoint(cs);
                return ff.literal(geom);
            } else if (MLINESTRING == type) {
                final ParseTree st = (ParseTree) tree.getChild(1);
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
            } else if (MPOLYGON == type) {
                final ParseTree st = (ParseTree) tree.getChild(1);
                final Geometry geom;
                if (isEmptyToken(st)) {
                    geom = GF.createMultiPolygon(new Polygon[0]);
                } else {
                    final List<CoordinateSeriesContext> eles = exp.coordinateSeries();
                    final int n = eles.size();
                    final Polygon[] polygons = new Polygon[n];
                    for (int i = 0; i < n; i++) {
                        final CoordinateSeriesContext polyTree = eles.get(i);
                        final List<CoordinateSerieContext> subs = polyTree.coordinateSerie();
                        final LinearRing contour = GF.createLinearRing(parseSequence(subs.get(0)));
                        final int hn = subs.size();
                        final LinearRing[] holes = new LinearRing[hn - 1];
                        for (int j = 1; j < hn; j++) {
                            holes[j - 1] = GF.createLinearRing(parseSequence(subs.get(j)));
                        }
                        final Polygon poly = GF.createPolygon(contour, holes);
                        polygons[i] = poly;
                    }
                    geom = GF.createMultiPolygon(polygons);
                }
                return ff.literal(geom);
            } else if (GEOMETRYCOLLECTION == type) {
                final ParseTree st = (ParseTree) tree.getChild(1);
                final Geometry geom;
                if (isEmptyToken(st)) {
                    geom = GF.createGeometryCollection(new Geometry[0]);
                } else {
                    final List<ExpressionGeometryContext> eles = exp.expressionGeometry();
                    final int n = eles.size();
                    final Geometry[] subs = new Geometry[n];
                    for (int i = 0; i < n; i++) {
                        final ParseTree subTree = eles.get(i);
                        final Geometry sub = (Geometry) convertExpression(subTree, ff).evaluate(null);
                        subs[i] = sub;
                    }
                    geom = GF.createGeometryCollection(subs);
                }
                return ff.literal(geom);
            } else if (ENVELOPE == type) {
                final ParseTree st = (ParseTree) tree.getChild(1);
                final Geometry geom;
                if (isEmptyToken(st)) {
                    geom = GF.createPolygon(GF.createLinearRing(new Coordinate[0]), new LinearRing[0]);
                } else {
                    final List<ExpressionUnaryContext> unaries = exp.expressionUnary();
                    final double west = unaryAsNumber(unaries.get(0)).doubleValue();
                    final double east = unaryAsNumber(unaries.get(1)).doubleValue();
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

            return convertExpression(tree.getChild(0), ff);
        }

        throw new CQLException("Unreconized expression : type=" + tree.getText());
    }

    private static boolean isEmptyToken(ParseTree tree) {
        return tree instanceof TerminalNode && ((TerminalNode) tree).getSymbol().getType() == EMPTY;
    }

    private static final Number unaryAsNumber(ExpressionUnaryContext tree) {
        //: UNARY? expressionNum ;
        final ExpressionUnaryContext exp = (ExpressionUnaryContext) tree;
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

    /**
     * Convert the given tree in a Filter.
     */
    private static Filter convertFilter(ParseTree tree, FilterFactory2 ff) throws CQLException {

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
                final List<Filter> subs = new ArrayList<Filter>();
                for (FilterContext f : exp.filter()) {
                    final Filter sub = convertFilter(f, ff);
                    if (sub instanceof And) {
                        subs.addAll(((And) sub).getChildren());
                    } else {
                        subs.add(sub);
                    }
                }
                return ff.and(subs);
            } else if (!exp.OR().isEmpty()) {
                //| filter (OR filter)+
                final List<Filter> subs = new ArrayList<Filter>();
                for (FilterContext f : exp.filter()) {
                    final Filter sub = convertFilter(f, ff);
                    if (sub instanceof Or) {
                        subs.addAll(((Or) sub).getChildren());
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
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);

                if ("=".equals(text)) {
                    return ff.equals(left, right);
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
                } else if ("<=".equals(text)) {
                    return ff.lessOrEqual(left, right);
                }
            } else if (exp.IN() != null) {
                // expression NOT? IN LPAREN (expressionFctParam )?  RPAREN
                final Expression val = convertExpression(exps.get(0), ff);
                final ExpressionFctParamContext prm = exp.expressionFctParam();
                final List<ExpressionContext> params = prm.expression();
                final List<Expression> subexps = new ArrayList<Expression>();
                for (int i = 0, n = params.size(); i < n; i++) {
                    subexps.add(convertExpression(params.get(i), ff));
                }

                final int size = subexps.size();
                final Filter selection;
                if (size == 0) {
                    selection = Filter.EXCLUDE;
                } else if (size == 1) {
                    selection = ff.equals(val, subexps.get(0));
                } else {
                    final List<Filter> filters = new ArrayList<Filter>();
                    for (Expression e : subexps) {
                        filters.add(ff.equals(val, e));
                    }
                    selection = ff.or(filters);
                }

                if (exp.NOT() != null) {
                    return ff.not(selection);
                } else {
                    return selection;
                }
            } else if (exp.BETWEEN() != null) {
                // expression BETWEEN expression AND expression
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                final Expression exp3 = convertExpression(exps.get(2), ff);
                return ff.between(exp1, exp2, exp3);

            } else if (exp.LIKE() != null) {
                // expression NOT? LIKE expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                if (exp.NOT() != null) {
                    return ff.not(ff.like(left, right.evaluate(null, String.class), "%", "_", "\\", true));
                } else {
                    return ff.like(left, right.evaluate(null, String.class), "%", "_", "\\", true);
                }

            } else if (exp.ILIKE() != null) {
                // expression NOT? LIKE expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                if (exp.NOT() != null) {
                    return ff.not(ff.like(left, right.evaluate(null, String.class), "%", "_", "\\", false));
                } else {
                    return ff.like(left, right.evaluate(null, String.class), "%", "_", "\\", false);
                }

            } else if (exp.IS() != null) {
                // expression IS NOT? NULL
                final Expression exp1 = convertExpression(exps.get(0), ff);
                if (exp.NOT() != null) {
                    return ff.not(ff.isNull(exp1));
                } else {
                    return ff.isNull(exp1);
                }

            } else if (exp.AFTER() != null) {
                // expression AFTER expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.after(left, right);

            } else if (exp.ANYINTERACTS() != null) {
                // expression ANYINTERACTS expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.anyInteracts(left, right);

            } else if (exp.BEFORE() != null) {
                // expression BEFORE expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.before(left, right);

            } else if (exp.BEGINS() != null) {
                // expression BEGINS expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.begins(left, right);

            } else if (exp.BEGUNBY() != null) {
                // expression BEGUNBY expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.begunBy(left, right);

            } else if (exp.DURING() != null) {
                // expression DURING expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.during(left, right);

            } else if (exp.ENDEDBY() != null) {
                // expression ENDEDBY expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.endedBy(left, right);

            } else if (exp.ENDS() != null) {
                // expression ENDS expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.ends(left, right);

            } else if (exp.MEETS() != null) {
                // expression MEETS expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.meets(left, right);

            } else if (exp.METBY() != null) {
                // expression METBY expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.metBy(left, right);

            } else if (exp.OVERLAPPEDBY() != null) {
                // expression OVERLAPPEDBY expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.overlappedBy(left, right);

            } else if (exp.TCONTAINS() != null) {
                // expression TCONTAINS expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.tcontains(left, right);

            } else if (exp.TEQUALS() != null) {
                // expression TEQUALS expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.tequals(left, right);

            } else if (exp.TOVERLAPS() != null) {
                // expression TOVERLAPS expression
                final Expression left = convertExpression(exps.get(0), ff);
                final Expression right = convertExpression(exps.get(1), ff);
                return ff.toverlaps(left, right);

            } else if (exp.filterGeometry() != null) {
                //expression filterGeometry
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
                final Expression prop = convertExpression(exps.get(0), ff);
                final double v1 = unaryAsNumber(exp.expressionUnary(0)).doubleValue();
                final double v2 = unaryAsNumber(exp.expressionUnary(1)).doubleValue();
                final double v3 = unaryAsNumber(exp.expressionUnary(2)).doubleValue();
                final double v4 = unaryAsNumber(exp.expressionUnary(3)).doubleValue();
                String crs = null;
                if (exp.TEXT() != null) {
                    crs = convertExpression(exp.TEXT(), ff).evaluate(null, String.class);
                }
                return ff.bbox(prop, v1, v2, v3, v4, crs);
            } else if (exp.BEYOND() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                final double distance = convertExpression(exps.get(2), ff).evaluate(null, Double.class);
                final Expression unitExp = convertExpression(exps.get(3), ff);
                final String unit = (unitExp instanceof PropertyName)
                        ? ((PropertyName) unitExp).getPropertyName()
                        : unitExp.evaluate(null, String.class);
                return ff.beyond(exp1, exp2, distance, unit);
            } else if (exp.CONTAINS() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                return ff.contains(exp1, exp2);
            } else if (exp.CROSSES() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                return ff.crosses(exp1, exp2);
            } else if (exp.DISJOINT() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                return ff.disjoint(exp1, exp2);
            } else if (exp.DWITHIN() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                final double distance = convertExpression(exps.get(2), ff).evaluate(null, Double.class);
                final Expression unitExp = convertExpression(exps.get(3), ff);
                final String unit = (unitExp instanceof PropertyName)
                        ? ((PropertyName) unitExp).getPropertyName()
                        : unitExp.evaluate(null, String.class);
                return ff.dwithin(exp1, exp2, distance, unit);
            } else if (exp.EQUALS() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                return ff.equal(exp1, exp2);
            } else if (exp.INTERSECTS() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                return ff.intersects(exp1, exp2);
            } else if (exp.OVERLAPS() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                return ff.overlaps(exp1, exp2);
            } else if (exp.TOUCHES() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                return ff.touches(exp1, exp2);
            } else if (exp.WITHIN() != null) {
                final Expression exp1 = convertExpression(exps.get(0), ff);
                final Expression exp2 = convertExpression(exps.get(1), ff);
                return ff.within(exp1, exp2);
            }

        }

        throw new CQLException("Unreconized filter : type=" + tree.getText());

    }

}
