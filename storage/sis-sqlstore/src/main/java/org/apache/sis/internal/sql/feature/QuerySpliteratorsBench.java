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
/*
    @Test
    public void benchmark() throws Exception {
        System.out.println("COMMON POOL: "+ ForkJoinPool.getCommonPoolParallelism());
        final DatabaseInput db = new DatabaseInput();
        db.numRows = 100000;
        db.parallel = true;

        long start = System.nanoTime();
        db.setup();
        System.out.println("Insertion time: "+((System.nanoTime()-start)/1e6)+" ms");

        // warmup
        for (int i = 0 ;  i < 5 ; i++) {
            test(db);
            test(db);
        }

        // go
        long prefetch = 0, noprefetch = 0;
        for (int i = 0 ; i < 100 ; i++) {
            start = System.nanoTime();
            test(db);
            prefetch += System.nanoTime()-start;

            start = System.nanoTime();
            test(db);
            noprefetch += System.nanoTime()-start;
        }

        System.out.println(String.format(
                "Performances:%nP: %d%nI: %d",
                (long) (prefetch / 1e7), (long) (noprefetch / 1e8)
        ));
    }
*/
    public static void main(String... args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
