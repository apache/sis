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

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.sis.internal.metadata.sql.Initializer;

import org.apache.derby.jdbc.EmbeddedDataSource;

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

import static org.apache.sis.util.ArgumentChecks.ensureStrictlyPositive;

/**
 *
 * @author Alexis Manin (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
@Fork(value = 2, jvmArgs = {"-server", "-Xmx2g"} )
@Warmup(iterations = 2, time = 4, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 4, timeUnit = TimeUnit.SECONDS)
public class QuerySpliteratorsBench {

    @State(Scope.Benchmark)
    public static class DatabaseInput {

        @Param({"1000", "100000"})
        int numRows;

        @Param({"true", "false"})
        boolean parallel;

        @Param({"true", "false"})
        boolean prefetch;

        @Param({"10", "100", "1000"})
        int fetchSize;

        @Param({"0.5", "1", "2"})
        float fetchRatio;

        EmbeddedDataSource db;
        QueryFeatureSet fs;

        public DatabaseInput() {}

        @Setup(Level.Trial)
        public void setup() throws SQLException {
            ensureStrictlyPositive("Number of rows", numRows);

            db = new EmbeddedDataSource();
            db.setDatabaseName("memory:spliterators");
            db.setDataSourceName("Apache SIS test database");
            db.setCreateDatabase("create");

            try (Connection c = db.getConnection()) {
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

                fs = new QueryFeatureSet("SELECT * FROM TEST", db, c);
                fs.allowBatchLoading = prefetch;
                fs.fetchSize = fetchSize;
                fs.fetchRatio = fetchRatio;
            }
        }

        @TearDown
        public void dropDatabase() throws SQLException {
            db.setCreateDatabase("no");
            db.setConnectionAttributes("drop=true");
            try {
                db.getConnection().close();
            } catch (SQLException e) {                          // This is the expected exception.
                if (!Initializer.isSuccessfulShutdown(e)) {
                    throw e;
                }
            }
        }
    }

    @Benchmark
    public void test(DatabaseInput input) throws SQLException {
        final int sum = input.fs.features(input.parallel).mapToInt(f -> 1).sum();
        if (sum != input.numRows) throw new AssertionError("..." + sum + "..." + "WTF ?!");
    }

    public static void main(String... args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
