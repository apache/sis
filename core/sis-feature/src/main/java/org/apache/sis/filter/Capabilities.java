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
package org.apache.sis.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.opengis.filter.Filter;

/**
 * Filter capabilities describing classes.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
abstract class Capabilities {

    private Capabilities() {
    }

    static final class FilterCapabilities implements org.opengis.filter.capability.FilterCapabilities {

        private final String version;
        private final org.opengis.filter.capability.IdCapabilities id;
        private final org.opengis.filter.capability.SpatialCapabilities spatial;
        private final org.opengis.filter.capability.ScalarCapabilities scalar;
        private final org.opengis.filter.capability.TemporalCapabilities temporal;

        FilterCapabilities(final String version,
                final org.opengis.filter.capability.IdCapabilities id,
                final org.opengis.filter.capability.SpatialCapabilities spatial,
                final org.opengis.filter.capability.ScalarCapabilities scalar,
                final org.opengis.filter.capability.TemporalCapabilities temporal) {
            this.version = version;
            this.id = id;
            this.spatial = spatial;
            this.scalar = scalar;
            this.temporal = temporal;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public org.opengis.filter.capability.ScalarCapabilities getScalarCapabilities() {
            return scalar;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public org.opengis.filter.capability.SpatialCapabilities getSpatialCapabilities() {
            return spatial;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public org.opengis.filter.capability.TemporalCapabilities getTemporalCapabilities() {
            return temporal;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public org.opengis.filter.capability.IdCapabilities getIdCapabilities() {
            return id;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public String getVersion() {
            return version;
        }

        public boolean supports(final Filter filter) {
            throw new UnsupportedOperationException();
        }
    }

    static final class IdCapabilities implements org.opengis.filter.capability.IdCapabilities {

        private final boolean eid;
        private final boolean fid;

        public IdCapabilities(final boolean eid, final boolean fid) {
            this.eid = eid;
            this.fid = fid;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public boolean hasEID() {
            return eid;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public boolean hasFID() {
            return fid;
        }
    }

    static final class ScalarCapabilities implements org.opengis.filter.capability.ScalarCapabilities {

        private final boolean logical;
        private final org.opengis.filter.capability.ComparisonOperators comparisons;
        private final org.opengis.filter.capability.ArithmeticOperators arithmetics;

        public ScalarCapabilities(final boolean logical,
                final org.opengis.filter.capability.ComparisonOperators comparisons,
                final org.opengis.filter.capability.ArithmeticOperators arithmetics) {
            this.logical = logical;
            this.comparisons = comparisons;
            this.arithmetics = arithmetics;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public boolean hasLogicalOperators() {
            return logical;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public org.opengis.filter.capability.ComparisonOperators getComparisonOperators() {
            return comparisons;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public org.opengis.filter.capability.ArithmeticOperators getArithmeticOperators() {
            return arithmetics;
        }
    }

    static final class SpatialCapabilities implements org.opengis.filter.capability.SpatialCapabilities {

        private final List<org.opengis.filter.capability.GeometryOperand> operands;
        private final org.opengis.filter.capability.SpatialOperators operators;

        public SpatialCapabilities(final org.opengis.filter.capability.GeometryOperand[] operands, final org.opengis.filter.capability.SpatialOperators operators) {
            ensureNonNull("operands", operands);
            ensureNonNull("spatial operators", operators);
            if (operands.length == 0) {
                throw new IllegalArgumentException("Operands must not be empty");
            }

            this.operands = UnmodifiableArrayList.wrap(operands);
            this.operators = operators;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public Collection<org.opengis.filter.capability.GeometryOperand> getGeometryOperands() {
            return operands;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public org.opengis.filter.capability.SpatialOperators getSpatialOperators() {
            return operators;
        }
    }

    static final class TemporalCapabilities implements org.opengis.filter.capability.TemporalCapabilities {

        private final Collection<org.opengis.filter.capability.TemporalOperand> operands;
        private final org.opengis.filter.capability.TemporalOperators operators;

        TemporalCapabilities(final org.opengis.filter.capability.TemporalOperand[] operands,
                final org.opengis.filter.capability.TemporalOperators operators) {
            ensureNonNull("operands", operands);
            ensureNonNull("temporal operators", operators);
            if (operands == null) {
                throw new IllegalArgumentException("Operands must not be null");
            }

            this.operands = UnmodifiableArrayList.wrap(operands);
            this.operators = operators;
        }

        @Override
        public Collection<org.opengis.filter.capability.TemporalOperand> getTemporalOperands() {
            return operands;
        }

        @Override
        public org.opengis.filter.capability.TemporalOperators getTemporalOperators() {
            return operators;
        }
    }

    static final class FunctionName implements org.opengis.filter.capability.FunctionName {

        private final String name;
        private final List<String> argNames;
        private final int size;

        FunctionName(final String name, final List<String> argNames, final int size) {
            this.name = name;
            this.argNames = argNames;
            this.size = size;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public int getArgumentCount() {
            return size;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public List<String> getArgumentNames() {
            return argNames;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public String getName() {
            return name;
        }
    }

    static final class Functions implements org.opengis.filter.capability.Functions {

        private final Map<String, org.opengis.filter.capability.FunctionName> functions = new HashMap<>();

        public Functions(final org.opengis.filter.capability.FunctionName[] functions) {
            if (functions == null) {
                throw new IllegalArgumentException("Functions must not be null");
            }
            for (org.opengis.filter.capability.FunctionName fn : functions) {
                this.functions.put(fn.getName(), fn);
            }
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public Collection<org.opengis.filter.capability.FunctionName> getFunctionNames() {
            return functions.values();
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public org.opengis.filter.capability.FunctionName getFunctionName(final String name) {
            return functions.get(name);
        }
    }

    private abstract static class Operators<T extends org.opengis.filter.capability.Operator> {

        private final Map<String, T> operators = new HashMap<>();

        Operators(final T[] operators) {
            if (operators == null || operators.length == 0) {
                throw new IllegalArgumentException("Functions must not be null or empty");
            }
            for (T op : operators) {
                this.operators.put(op.getName(), op);
            }
        }

        public Collection<T> getOperators() {
            return Collections.unmodifiableCollection(operators.values());
        }

        public T getOperator(final String name) {
            return operators.get(name);
        }
    }

    static final class ArithmeticOperators implements org.opengis.filter.capability.ArithmeticOperators {

        private final boolean simple;
        private final org.opengis.filter.capability.Functions functions;

        public ArithmeticOperators(final boolean simple, final org.opengis.filter.capability.Functions functions) {
            ensureNonNull("functions", functions);
            this.simple = simple;
            this.functions = functions;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public boolean hasSimpleArithmetic() {
            return simple;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public org.opengis.filter.capability.Functions getFunctions() {
            return functions;
        }
    }

    static final class ComparisonOperators extends Operators<org.opengis.filter.capability.Operator> implements org.opengis.filter.capability.ComparisonOperators {

        ComparisonOperators(final org.opengis.filter.capability.Operator[] operators) {
            super(operators);
        }
    }

    static final class SpatialOperators extends Operators<org.opengis.filter.capability.SpatialOperator> implements org.opengis.filter.capability.SpatialOperators {

        public SpatialOperators(final org.opengis.filter.capability.SpatialOperator[] operators) {
            super(operators);
        }
    }

    static final class TemporalOperators extends Operators<org.opengis.filter.capability.TemporalOperator> implements org.opengis.filter.capability.TemporalOperators {

        public TemporalOperators(final TemporalOperator[] operators) {
            super(operators);
        }
    }

    static class Operator implements org.opengis.filter.capability.Operator {

        private final String name;

        public Operator(final String name) {
            ensureNonNull("operator name", name);
            this.name = name;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public String getName() {
            return name;
        }
    }

    static final class SpatialOperator extends Operator implements org.opengis.filter.capability.SpatialOperator {

        private final List<org.opengis.filter.capability.GeometryOperand> operands;

        public SpatialOperator(final String name, final org.opengis.filter.capability.GeometryOperand[] operands) {
            super(name);

            if (operands == null || operands.length == 0) {
                throw new IllegalArgumentException("Operands list can not be null or empty");
            }

            //use a threadsafe optimized immutable list
            this.operands = UnmodifiableArrayList.wrap(operands.clone());
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public Collection<org.opengis.filter.capability.GeometryOperand> getGeometryOperands() {
            return operands;
        }
    }

    static final class TemporalOperator extends Operator implements org.opengis.filter.capability.TemporalOperator {

        private final List<org.opengis.filter.capability.TemporalOperand> operands;

        public TemporalOperator(final String name, final org.opengis.filter.capability.TemporalOperand[] operands) {
            super(name);

            if (operands == null || operands.length == 0) {
                throw new IllegalArgumentException("Operands list can not be null or empty");
            }

            //use a threadsafe optimized immutable list
            this.operands = UnmodifiableArrayList.wrap(operands.clone());
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public Collection<org.opengis.filter.capability.TemporalOperand> getTemporalOperands() {
            return operands;
        }
    }

}
