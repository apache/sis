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
package org.apache.sis.internal.cql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;


/**
 * ANTLR CQL parser methods.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class AntlrCQL {

    private AntlrCQL() {
    }

    public static ParseTree compile(String cql) {
        final Object obj = compileFilterOrExpression(cql);
        ParseTree tree = null;
        if (obj instanceof ParseTree) {
            tree = (ParseTree) obj;
        }
        return tree;
    }

    public static Object compileExpression(String cql) {
        try {
            // Lexer splits input into tokens.
            final CodePointCharStream input = CharStreams.fromString(cql);
            final TokenStream tokens = new CommonTokenStream(new CQLLexer(input));

            // Parser generates abstract syntax tree.
            final CQLParser parser = new CQLParser(tokens);
            final CQLParser.ExpressionContext ctx = parser.expression();
            return ctx;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }

    public static Object compileFilter(String cql) {
        try {
            // Lexer splits input into tokens.
            final CodePointCharStream input = CharStreams.fromString(cql);
            final TokenStream tokens = new CommonTokenStream(new CQLLexer(input));

            // Parser generates abstract syntax tree.
            final CQLParser parser = new CQLParser(tokens);
            final CQLParser.FilterContext retfilter = parser.filter();

            return retfilter;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }

    public static Object compileFilterOrExpression(String cql) {
        try {
            // Lexer splits input into tokens.
            final CodePointCharStream input = CharStreams.fromString(cql);
            final TokenStream tokens = new CommonTokenStream(new CQLLexer(input));

            // Parser generates abstract syntax tree.
            final CQLParser parser = new CQLParser(tokens);
            final CQLParser.FilterOrExpressionContext retfilter = parser.filterOrExpression();

            return retfilter;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }

    public static Object compileQuery(String cql) {
        try {
            // Lexer splits input into tokens.
            final CodePointCharStream input = CharStreams.fromString(cql);
            final TokenStream tokens = new CommonTokenStream(new CQLLexer(input));

            // Parser generates abstract syntax tree.
            final CQLParser parser = new CQLParser(tokens);
            final CQLParser.QueryContext ctx = parser.query();
            return ctx;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }
}
