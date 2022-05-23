package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.sql.TestDatabase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.time.ZoneOffset.UTC;
import static java.time.ZoneOffset.ofHours;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Check access and conversion to database datetime values.
 */
public class TemporalValueGetterTest extends TestCase {

    private static final String DB_TITLE = "Test Temporal values";
    private static final String TEST_SCHEMA = "SIS_TEST_TEMPORAL";
    private static final String DEFAULT_DRIVER = "DEFAULT";
    public static final String PG_DRIVER = "POSTGRESQL";
    private static Map<String, TestDatabase> DATABASES;

    @BeforeClass
    public static void initTestDatabases() throws Exception {
        if (DATABASES != null) throw new IllegalStateException("Test database should be null before initialization");
        DATABASES = new HashMap<>();
        DATABASES.put(DEFAULT_DRIVER, TestDatabase.create(DB_TITLE));
        DATABASES.put("HSQLDB",  TestDatabase.createOnHSQLDB(DB_TITLE, true));
        DATABASES.put("H2",      TestDatabase.createOnH2(DB_TITLE));
        try {
            final TestDatabase db = TestDatabase.createOnPostgreSQL(TEST_SCHEMA, true);
            DATABASES.put(PG_DRIVER, db);
        } catch (AssumptionViolatedException e) {
            // Ok, environment does not contain a PostGreSQL test database
            out.println("[FINE] PostgreSQL database deactivated due to assumption requirement not met: "+e.getMessage());
        }
    }

    @AfterClass
    public static void disposeTestDatabases() throws Exception {
        if (DATABASES != null) {
            List<Exception> closeErrors = DATABASES.values().stream()
                    .map(db -> {
                        try {
                            db.close();
                            return null;
                        } catch (Exception e) {
                            return e;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!closeErrors.isEmpty()) {
                final Exception base = closeErrors.get(0);
                for (int i = 1 ; i < closeErrors.size() ; i++) {
                    base.addSuppressed(closeErrors.get(i));
                }
                throw base;
            }
        }
    }

    @Test
    public void sqlDateToLocalDate() {
        testOnEachDatabase((driver, db) -> {
            execute(driver, db,
                    "CREATE TABLE TEST_DATE (\"value\" DATE)",
                    "INSERT INTO TEST_DATE (\"value\") VALUES ('2022-05-01'), ('2022-05-31'), (null)"
            );

            ValueGetter<LocalDate> getter = ValueGetter.AsDate.INSTANCE;
            execute(driver, db, "SELECT \"value\" FROM TEST_DATE", r -> {
                r.next();
                assertEquals(LocalDate.of(2022, 5, 1), getter.getValue(null, r, 1));
                r.next();
                assertEquals(LocalDate.of(2022, 5, 31), getter.getValue(null, r, 1));
                r.next();
                // Ensure no NPE occurs, i.e the converter is resilient to null inputs
                assertNull(getter.getValue(null, r, 1));
                assertFalse("Defense against test change: only 3 values should be present in the test table", r.next());
            });
        });
    }

    @Test
    public void sqlTimestampToLocalDateTime() {
        testOnEachDatabase((driver, db) -> {
            execute(driver, db,
                    "CREATE TABLE TEST_TIMESTAMP_WITHOUT_TIMEZONE (\"value\" TIMESTAMP)",
                    "INSERT INTO TEST_TIMESTAMP_WITHOUT_TIMEZONE (\"value\") VALUES ('2022-05-01 01:01:01'), ('2022-05-31 23:59:59.999'), (null)"
            );

            ValueGetter<LocalDateTime> getter = ValueGetter.AsLocalDateTime.INSTANCE;
            execute(driver, db, "SELECT \"value\" FROM TEST_TIMESTAMP_WITHOUT_TIMEZONE", r -> {
                r.next();
                assertEquals(LocalDateTime.of(2022, 5, 1, 1, 1, 1), getter.getValue(null, r, 1));
                r.next();
                assertEquals(LocalDateTime.of(2022, 5, 31, 23, 59, 59, 999_000_000), getter.getValue(null, r, 1));
                r.next();
                // Ensure no NPE occurs, i.e the converter is resilient to null inputs
                assertNull(getter.getValue(null, r, 1));
                assertFalse("Defense against test change: only 3 values should be present in the test table", r.next());
            });
        });
    }

    @Test
    public void sqlTimestampWithTimeZoneToInstant() {
        testOnEachDatabase((driver, db) -> {
            Assume.assumeFalse("Derby does not support TIMESTAMP WITH TIME ZONE data type", DEFAULT_DRIVER.equals(driver));
            execute(driver, db,
                    "CREATE TABLE TEST_TIMESTAMP_WITH_TIMEZONE (\"value\" TIMESTAMP WITH TIME ZONE)",
                    "INSERT INTO TEST_TIMESTAMP_WITH_TIMEZONE (\"value\") VALUES ('2022-05-01 01:01:01+01:00'), ('2022-05-31 23:59:59.999+02:00'), (null)"
            );

            ValueGetter<Instant> getter = ValueGetter.AsInstant.INSTANCE;
            execute(driver, db, "SELECT \"value\" FROM TEST_TIMESTAMP_WITH_TIMEZONE", r -> {
                Instant[] expectedValues = {
                        LocalDateTime.of(2022, 5, 1, 1, 1, 1).atOffset(ofHours(1)).toInstant(),
                        LocalDateTime.of(2022, 5, 31, 23, 59, 59, 999_000_000).atOffset(ofHours(2)).toInstant(),
                        null
                };
                assertContentEquals(expectedValues, r, 1, getter, "Timestamp with time zone conversion");
            });
        });
    }

    /**
     * Note: It does not test sub-second precision, although PostGreSQL, HSQLDB and H2 support it.
     * The conversion from {@link java.sql.Timestamp} makes it difficult to get back the information.
     * See {@link ValueGetter.AsLocalTime} todo section for details.
     */
    @Test
    public void sqlTimeToLocalTime() {
        testOnEachDatabase(((driver, db) -> {
            execute(driver, db,
                    "CREATE TABLE TEST_TIME (\"value\" TIME)",
                    "INSERT INTO TEST_TIME (\"value\") VALUES ('08:01:01'), ('09:30:30'), (null)");
            ValueGetter<LocalTime> getter = ValueGetter.AsLocalTime.INSTANCE;
            execute(driver, db, "SELECT \"value\" FROM TEST_TIME", r -> {
                LocalTime[] expectedValues = {
                        LocalTime.of(8, 1, 1),
                        LocalTime.of(9, 30, 30),
                        null
                };
                assertContentEquals(expectedValues, r, 1, getter, "Time conversion");
            });
        }));
    }

    @Test
    public void sqlTimeWithTimeZoneToOffsetTime() {
        testOnEachDatabase(((driver, db) -> {
            Assume.assumeFalse("Derby does not support TIMESTAMP WITH TIME ZONE data type", DEFAULT_DRIVER.equals(driver));
            execute(driver, db,
                    "CREATE TABLE TEST_TIME_WITH_TIMEZONE (\"value\" TIME WITH TIME ZONE)",
                    "INSERT INTO TEST_TIME_WITH_TIMEZONE (\"value\") VALUES ('08:01:01+00:00'), ('09:30:30-01:00'), (null)");
            ValueGetter<OffsetTime> getter = ValueGetter.AsOffsetTime.INSTANCE;
            execute(driver, db, "SELECT \"value\" FROM TEST_TIME_WITH_TIMEZONE", r -> {
                OffsetTime[] expectedValues = {
                        LocalTime.of(8, 1, 1).atOffset(UTC),
                        LocalTime.of(10, 30, 30).atOffset(UTC),
                        null
                };

                // Databases don't properly preserve insert time zone, so we can only check if values represent the same
                // instant from the start of day.
                assertContentEquals(expectedValues, r, 1, getter, OffsetTime::isEqual, "Time conversion");
            });
        }));
    }

    private <V> void assertContentEquals(V[] expected, ResultSet actual, int columnIndex, ValueGetter<V> valueConverter, String title) throws Exception {
        assertContentEquals(expected, actual, columnIndex, valueConverter, Objects::equals, title);
    }

    /**
     * Equivalent of {@link Assert#assertArrayEquals(String, Object[], Object[])}, except "actual values" are retrieved
     * by parsing an {@link ResultSet SQL query result}.
     *
     * @param expected Values that should be returned by the query+converter combo.
     * @param actual Result of an SQL query to validate.
     * @param columnIndex Column to read values from in given result set.
     * @param valueConverter How to map each SQL result value to JAVA API. Must not be null.
     * @param comparisonFunction How to compare an expected element with. Must not be null.
     * @param title An error message title for assertion failures.
     * @param <V> Type of values to compare.
     */
    private <V> void assertContentEquals(V[] expected, ResultSet actual, int columnIndex, ValueGetter<V> valueConverter, BiPredicate<V, V> comparisonFunction, String title) throws Exception {
        final List<V> values = new ArrayList<>(expected.length);
        while (values.size() < expected.length && actual.next()) {
            values.add(valueConverter.getValue(null, actual, columnIndex));
        }

        assertEquals(title + ": Not enough values in query result", expected.length, values.size());
        assertFalse(title + ": Too many values in query result", actual.next());

        for (int i = 0 ; i < expected.length ; i++) {
            final V expectedValue = expected[i];
            final V actualValue = values.get(i);

            if (expectedValue == actualValue) continue;

            if (expectedValue == null || actualValue == null || !comparisonFunction.test(expectedValue, actualValue)) {
                throw new AssertionError(String.format("%s: error at index %d -> Expected: %s, but was: %s", title, i, expectedValue, actualValue));
            }
        }
    }

    private void testOnEachDatabase(DatabaseTester tester) {
        DATABASES.forEach((driver, db) -> {
            try {
                tester.test(driver, db);
            } catch (AssumptionViolatedException e) {
                // OK, user has deactivated test upon some conditions
                out.println("[FINE] Error ignored because of failed assumption. Driver="+driver+", reason="+e.getMessage());
            } catch (AssertionError e) {
                throw new AssertionError("Test failed for driver: "+driver, e);
            } catch (Exception e) {
                throw new RuntimeException("Error while testing driver: "+driver, e);
            }
        });
    }

    @FunctionalInterface
    private interface DatabaseTester {
        void test(String driver, TestDatabase db) throws Exception;
    }

    @FunctionalInterface
    private interface QueryAction {
        void accept(ResultSet queryResult) throws Exception;
    }

    private static void execute(String driver, TestDatabase db, String firstQuery, String... moreQueries) throws SQLException {
        try (Connection c = db.source.getConnection()) {
            if (PG_DRIVER.equals(driver)) c.setSchema(TEST_SCHEMA);
            try (Statement s = c.createStatement()) {
                s.execute(firstQuery);
                for (String q : moreQueries) s.execute(q);
            }
        }
    }

    private static void execute(String driver, TestDatabase db, String sqlQuery, QueryAction action) throws Exception {
        try (Connection c = db.source.getConnection()) {
            if (PG_DRIVER.equals(driver)) c.setSchema(TEST_SCHEMA);
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(sqlQuery)
            ) {
                action.accept(r);
            }
        }
    }
}
