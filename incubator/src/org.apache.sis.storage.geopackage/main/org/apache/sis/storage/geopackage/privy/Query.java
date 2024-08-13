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
package org.apache.sis.storage.geopackage.privy;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.util.ArraysExt;

/**
 * SQL Query object.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Query {

    private final String query;
    private final Integer[] parameters;

    public Query(String stmt) {

        final StringBuilder sb = new StringBuilder();

        int before = 0;
        final List<Integer> params = new ArrayList<>();
        for (int i = stmt.indexOf('?', before); i >= 0; i = stmt.indexOf('?', before)) {
            final String part = stmt.substring(before, i);
            final int start = part.indexOf('[');
            final int end = part.indexOf(']');
            if (start < 0 || end < 0) {
                throw new IllegalArgumentException("Invalid query " + stmt);
            }
            final String type = part.substring(start + 1, end);
            try {
                final Field f = Types.class.getField(type);
                params.add(f.getInt(null));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Type unknowed :  " + type + ", Invalid query " + stmt);
            }

            sb.append(part.substring(0, start));
            sb.append(part.substring(end + 1));
            sb.append('?');

            before = i + 1;
        }

        sb.append(stmt.substring(before));
        this.parameters = params.toArray(new Integer[params.size()]);
        this.query = sb.toString();
    }

    public Query(String query, Integer[] parameters) {
        this.query = query;
        this.parameters = parameters;
    }

    public String query() {
        return query;
    }

    /**
     * Replace prepared statement variable by given String.
     * the returned Query has one less parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @return
     */
    public Query defineParam(int index, String value) {
        final StringBuilder sb = new StringBuilder();

        int before = 0;
        for (int i = query.indexOf('?', before),k=0; i >= 0; i = query.indexOf('?', before),k++) {
            final String part = query.substring(before, i);
            sb.append(part);
            if (k == index) {
                sb.append(value);
            } else {
                sb.append('?');
            }
            before = i + 1;
        }

        sb.append(query.substring(before));
        return new Query(sb.toString(), ArraysExt.remove(parameters, index, 1));
    }

    public int getNbParameters() {
        return parameters.length;
    }

    public boolean execute(Connection cnx, Object... params) throws SQLException {
        final int nb = getNbParameters();
        if (nb != params.length) {
            throw new SQLException("Was expecting "+nb+" parameters for query but only received "+params.length);
        }

        String query = this.query;
        for (Object param : params) {
            int idx = query.indexOf('?');
            if (param instanceof String) {
                param = "\"" + param + "\"";
            }
            query = query.substring(0, idx) + param + query.substring(idx+1);
        }

        return execute(query, cnx);
    }

    public static boolean execute(final String sqlQuery, final Connection cnx) throws SQLException {
        try (final Statement statement = cnx.createStatement()) {
            return statement.execute(sqlQuery);
        }
    }

    /**
     * Create and fill prepared statement.
     * The original query statement may be modified if some parameters are null.
     *
     * SQlite hack : Decimal NaN values are converted to matching SQL String 'NaN',
     * Problem is know but unlikely to be fixed :
     * https://www.sqlite.org/floatingpoint.html
     * https://www.anycodings.com/1questions/2537259/store-nan-values-in-sqlite-database
     * https://github.com/JuliaDatabases/SQLite.jl/issues/213
     *
     *
     * example : SELECT * FROM house WHERE user=[INTEGER]?
     * will be replaced by : SELECT * FROM house WHERE user IS NULL , if parameter is null.
     */
    public PreparedStatement createPreparedStatement(Connection cnx, Object... params) throws SQLException {

        final int nb = getNbParameters();
        if (nb != params.length) {
            throw new SQLException("Was expecting "+nb+" parameters for query but only received "+params.length);
        }

        boolean hasNullValue = false;
        for (int i = 0; i < params.length; i++) {
            Object obj = params[i];
            hasNullValue = (obj == null);
            if (obj == null) {
                hasNullValue = true;
            } else if (obj instanceof Double) {
                if (Double.isNaN( (Double) obj)) params[i] = "NaN";
            } else if (obj instanceof Float) {
                if (Float.isNaN( (Float) obj)) params[i] = "NaN";
            }
        }

        String query = this.query;

        //adapt query for null values
        if (hasNullValue) {
            final List<Object> noNullParams = new ArrayList<Object>();
            final StringBuilder sb = new StringBuilder();
            int before = 0;

            //check if we are in a where condition
            int clauseIndex = query.indexOf("WHERE");
            if (clauseIndex < 0) clauseIndex = Integer.MAX_VALUE;

            for (int i=query.indexOf('?',before),k=0; i>=0; i=query.indexOf('?',before),k++) {
                final Object param = params[k];
                final String part = query.substring(before, i);

                nullReplace:
                if (param == null) {
                    if (i > clauseIndex) {
                        //check if we have a '=' before and we are in a WHERE condition
                        for (int t=part.length()-1;t>=0;t--) {
                            final char c = part.charAt(t);
                            if (c == '=') {
                                sb.append(part.substring(0,t));
                                sb.append(" IS NULL ");
                                break nullReplace;
                            } else if (c != ' ') {
                                break;
                            }
                        }
                    }

                    noNullParams.add(param);
                    sb.append(part);
                    sb.append('?');
                } else {
                    noNullParams.add(param);
                    sb.append(part);
                    sb.append('?');
                }
                before = i+1;
            }

            //add remaining
            params = noNullParams.toArray();
            sb.append(query.substring(before));
            query = sb.toString();
        }

        final PreparedStatement stmt;
        try {
            stmt = cnx.prepareStatement(query);
        } catch (SQLException ex) {
            throw new SQLException("Failed to create statement for query : "+ query, ex);
        }
        fill(stmt, params);
        return stmt;
    }

    /**
     * Caution : if some arguments may be null consider using 'create' method to
     * automaticaly refactor the query.
     */
    public void fillPreparedStatement(final PreparedStatement stmt, final Object... params) throws SQLException {
        final int nb = getNbParameters();

        if (nb != params.length) {
            throw new SQLException("Failed to fill statement for query : " + query + "\nWas expecting " + nb + " parameters for query but only received " + params.length);
        }
        if (nb == 0) {
            //nothing to fill
            return;
        }
        fill(stmt, params);
    }

    private void fill(final PreparedStatement stmt, final Object ... params) throws SQLException {
        try {
            for (int i=0;i<params.length;i++) {
                final Object param = params[i];
                if (param != null) {
                    stmt.setObject(i+1, params[i], parameters[i]);
                } else {
                    stmt.setNull(i+1, parameters[i]);
                }
            }
        } catch (SQLException ex) {
            throw new SQLException("Failed to fill statement for query : " + query, ex);
        }
    }

    @Override
    public String toString() {
        return query;
    }

}
