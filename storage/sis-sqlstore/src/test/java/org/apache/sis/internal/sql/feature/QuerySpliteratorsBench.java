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

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.util.ArgumentChecks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import static org.junit.Assert.assertEquals;


/**
 * Measure the performance of the stream returned by {@link QueryFeatureSet#features(boolean)}.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@Fork(value = 2, jvmArgs = {"-server", "-Xmx2G"} )
@Warmup(iterations = 2, time = 4, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 4, timeUnit = TimeUnit.SECONDS)
public class QuerySpliteratorsBench {
    /**
     * Provide {@link QueryFeatureSet} instances to be tested.
     * Various configurations are used (number of rows, parallelism, <i>etc.</i>).
     *
     * @see QuerySpliteratorsBench#test(DatabaseInput)
     */
    @State(Scope.Benchmark)
    public static class DatabaseInput {
        /**
         * Number of rows to insert in the test database.
         */
        @Param({"1000", "100000"})
        int numRows;

        /**
         * Argument to be given in the call to {@link QueryFeatureSet#features(boolean)}.
         */
        @Param({"true", "false"})
        boolean parallel;

        /**
         * Value to assign to {@link QueryFeatureSet#allowBatchLoading}.
         */
        @Param({"true", "false"})
        private boolean prefetch;

        /**
         * Value to assign to {@link QueryFeatureSet#fetchSize}.
         */
        @Param({"10", "100", "1000"})
        private int fetchSize;

        /**
         * Value to assign to {@link QueryFeatureSet#fetchRatio}.
         */
        @Param({"0.5", "1", "2"})
        private float fetchRatio;

        /**
         * Encapsulate a data source for an in-memory database.
         */
        private TestDatabase db;

        /**
         * The feature set to benchmark.
         */
        QueryFeatureSet fs;

        /**
         * Creates a new set of {@link QueryFeatureSet} provider.
         */
        public DatabaseInput() {
        }

        /**
         * Prepares a database populated with arbitrary rows, then creates a {@link QueryFeatureSet}
         * for querying that database. The {@code QueryFeatureSet} configuration will vary between
         * different calls to this method, depending on values injected by JMH in the annotated fields.
         *
         * @throws SQLException if an error occurred while preparing the database.
         */
        @Setup(Level.Trial)
        public void setup() throws SQLException {
            ArgumentChecks.ensureStrictlyPositive("Number of rows", numRows);

            db = TestDatabase.create("spliterators");
            try (Connection c = db.source.getConnection()) {
                c.createStatement().execute(
                        "CREATE TABLE TEST (str CHARACTER VARYING(20), myInt INTEGER, myDouble DOUBLE)"
                );
                final PreparedStatement st = c.prepareStatement("INSERT INTO TEST values (?, ?, ?)");

                final Random rand = new Random();
                int rows = 1;
                final byte[] txt = new byte[20];
                do {
                    for (int i = 0; i < 500 ; i++, rows++) {
                        rand.nextBytes(txt);
                        st.setString(1, new String(txt, StandardCharsets.US_ASCII));
                        st.setInt(2, rand.nextInt());
                        st.setDouble(3, rand.nextDouble());
                        st.addBatch();
                    }
                    st.executeBatch();
                    st.clearBatch();
                } while (rows < numRows);

                fs = new QueryFeatureSet("SELECT * FROM TEST", db.source, c);
                fs.allowBatchLoading = prefetch;
                fs.fetchSize  = fetchSize;
                fs.fetchRatio = fetchRatio;
            }
        }

        /**
         * Destroy the in-memory database.
         *
         * @throws SQLException in an error occurred.
         */
        @TearDown
        public void dropDatabase() throws SQLException {
            db.close();
        }
    }

    /**
     * Invoked by JMH for executing the operation that we want to benchmark.
     *
     * @param  input  encapsulate the {@link QueryFeatureSet} instance to benchmark.
     * @throws SQLException if an error occurred during the database query.
     */
    @Benchmark
    public void test(DatabaseInput input) throws SQLException {
        final int sum = input.fs.features(input.parallel).mapToInt(f -> 1).sum();
        assertEquals(input.numRows, sum);
    }

    /**
     * Launch the benchmark.
     *
     * @param  args  JMH command line argument.
     * @throws IOException if JMH can not proceed.
     */
    public static void main(final String[] args) throws IOException {
        org.openjdk.jmh.Main.main(args);
    }
}
