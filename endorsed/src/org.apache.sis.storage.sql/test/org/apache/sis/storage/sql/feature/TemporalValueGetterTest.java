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
package org.apache.sis.storage.sql.feature;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.function.BiPredicate;
import org.apache.sis.metadata.sql.internal.shared.Dialect;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.storage.sql.TestOnAllDatabases;
import org.apache.sis.metadata.sql.TestDatabase;


/**
 * Tests accesses and conversions to database date and time values.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TemporalValueGetterTest extends TestOnAllDatabases {
    /**
     * Whether the JDBC driver supports conversions from objects to {@code java.time} API.
     */
    private boolean supportsJavaTime;

    /**
     * Whether {@code ResultSet.getTime(int)} uses an unknown time zone.
     * In such case, the fallbacks are unreliable and their tests should
     * be disabled.
     */
    private boolean unreliableFallback;

    /**
     * Creates a new test.
     */
    public TemporalValueGetterTest() {
    }

    /**
     * Runs all tests on a single database software.
     *
     * @throws Exception if the execution of an SQL query or the conversion of a value failed.
     */
    @Override
    protected void test(final TestDatabase database, final boolean noschema) throws Exception {
        supportsJavaTime = database.dialect.supportsJavaTime();
        try (Connection connection = database.source.getConnection()) {
            if (!noschema) {
                connection.setSchema(SCHEMA);
            }
            try (Statement stmt = connection.createStatement()) {
                sqlDateToLocalDate(stmt);
                sqlTimeToLocalTime(stmt);
                sqlTimestampToLocalDateTime(stmt);
                if (database.dialect != Dialect.DERBY) {    // Derby does not support TIMESTAMP WITH TIME ZONE.
                    unreliableFallback = (database.dialect != Dialect.POSTGRESQL);
                    sqlTimeWithTimeZoneToOffsetTime(stmt);
                    sqlTimestampWithTimeZoneToOffsetDateTime(stmt);
                }
            }
        }
    }

    /**
     * Returns the value getter than can be used for the test.
     * The list of available value getters depends on the database driver.
     *
     * @param  <V>       type of values to get.
     * @param  main      the preferred value getter.
     * @param  fallback  the fallback to use when the main getter is not available.
     * @return the value getters to use for the tests.
     */
    @SuppressWarnings({"unchecked","rawtypes"})     // Generic array creation.
    private <V> ValueGetter<V>[] getters(final ValueGetter<V> main, final ValueGetter<V> fallback) {
        if (supportsJavaTime) {
            if (unreliableFallback) {
                return new ValueGetter[] {main};
            } else {
                return new ValueGetter[] {main, fallback};
            }
        } else {
            return new ValueGetter[] {fallback};
        }
    }

    /**
     * Tests conversion from SQL {@code DATE} to {@link LocalDate}.
     */
    private void sqlDateToLocalDate(final Statement stmt) throws Exception {
        assertFalse(stmt.execute("CREATE TABLE TEST_DATE (\"value\" DATE)"));
        assertFalse(stmt.execute("INSERT INTO TEST_DATE (\"value\") VALUES ('2022-05-01'), ('2022-05-31'), (null)"));
        LocalDate[] expected = {
            LocalDate.of(2022, 5, 1),
            LocalDate.of(2022, 5, 31),
            null                        // Ensure no NPE occurs, i.e the converter is resilient to null inputs.
        };
        for (ValueGetter<LocalDate> getter : getters(ValueGetter.LOCAL_DATE, ValueGetter.AsLocalDate.INSTANCE)) {
            try (ResultSet r = stmt.executeQuery("SELECT \"value\" FROM TEST_DATE")) {
                assertContentEquals(expected, r, getter, null, "Date conversion");
            }
        }
    }

    /**
     * Tests conversion from SQL {@code TIMESTAMP} to {@link LocalDateTime}.
     */
    private void sqlTimestampToLocalDateTime(final Statement stmt) throws Exception {
        assertFalse(stmt.execute("CREATE TABLE TEST_TIMESTAMP_WITHOUT_TIMEZONE (\"value\" TIMESTAMP)"));
        assertFalse(stmt.execute("INSERT INTO TEST_TIMESTAMP_WITHOUT_TIMEZONE (\"value\") "
                + "VALUES ('2022-05-01 01:01:01'), ('2022-05-31 23:59:59.999'), (null)"));
        LocalDateTime[] expected = {
            LocalDateTime.of(2022, 5, 1, 1, 1, 1),
            LocalDateTime.of(2022, 5, 31, 23, 59, 59, 999_000_000),
            null                        // Ensure no NPE occurs, i.e the converter is resilient to null inputs.
        };
        for (ValueGetter<LocalDateTime> getter : getters(ValueGetter.LOCAL_DATE_TIME, ValueGetter.AsLocalDateTime.INSTANCE)) {
            try (ResultSet r = stmt.executeQuery("SELECT \"value\" FROM TEST_TIMESTAMP_WITHOUT_TIMEZONE")) {
                assertContentEquals(expected, r, getter, null, "Date and time conversion");
            }
        }
    }

    /**
     * Tests conversion from SQL {@code TIMESTAMP WITH TIME ZONE} to {@link OffsetDateTime}.
     */
    private void sqlTimestampWithTimeZoneToOffsetDateTime(final Statement stmt) throws Exception {
        assertFalse(stmt.execute("CREATE TABLE TEST_TIMESTAMP_WITH_TIMEZONE (\"value\" TIMESTAMP WITH TIME ZONE)"));
        assertFalse(stmt.execute("INSERT INTO TEST_TIMESTAMP_WITH_TIMEZONE (\"value\") "
                + "VALUES ('2022-05-01 01:01:01+01:00'), ('2022-05-31 23:59:59.999+02:00'), (null)"));
        OffsetDateTime[] expected = {
            LocalDateTime.of(2022, 5, 1, 1, 1, 1).atOffset(ZoneOffset.ofHours(1)),
            LocalDateTime.of(2022, 5, 31, 23, 59, 59, 999_000_000).atOffset(ZoneOffset.ofHours(2)),
            null
        };
        for (ValueGetter<OffsetDateTime> getter : getters(ValueGetter.OFFSET_DATE_TIME, ValueGetter.AsOffsetDateTime.INSTANCE)) {
            try (ResultSet r = stmt.executeQuery("SELECT \"value\" FROM TEST_TIMESTAMP_WITH_TIMEZONE")) {
                /*
                 * Some databases do not properly preserve insert time zone, so we can
                 * only check if values represent the same instant from the start of day.
                 */
                assertContentEquals(expected, r, getter, OffsetDateTime::isEqual,
                                    "Timestamp with time zone conversion");
            }
        }
    }

    /**
     * Tests conversion from SQL {@code TIME} to {@link LocalTime}.
     */
    private void sqlTimeToLocalTime(final Statement stmt) throws Exception {
        assertFalse(stmt.execute("CREATE TABLE TEST_TIME (\"value\" TIME)"));
        assertFalse(stmt.execute("INSERT INTO TEST_TIME (\"value\") VALUES ('08:01:01'), ('09:30:30'), (null)"));
        LocalTime[] expected = {
            LocalTime.of(8, 1, 1),
            LocalTime.of(9, 30, 30),
            null
        };
        for (ValueGetter<LocalTime> getter : getters(ValueGetter.LOCAL_TIME, ValueGetter.AsLocalTime.INSTANCE)) {
            try (ResultSet r = stmt.executeQuery("SELECT \"value\" FROM TEST_TIME")) {
                assertContentEquals(expected, r, getter, null, "Time conversion");
            }
        }
    }

    /**
     * Tests conversion from SQL {@code TIME} to {@link LocalTime}.
     */
    private void sqlTimeWithTimeZoneToOffsetTime(final Statement stmt) throws Exception {
        assertFalse(stmt.execute("CREATE TABLE TEST_TIME_WITH_TIMEZONE (\"value\" TIME WITH TIME ZONE)"));
        assertFalse(stmt.execute("INSERT INTO TEST_TIME_WITH_TIMEZONE (\"value\") "
                + "VALUES ('08:01:01+00:00'), ('09:30:30-01:00'), (null)"));
        OffsetTime[] expected = {
            LocalTime.of(8, 1, 1).atOffset(ZoneOffset.UTC),
            LocalTime.of(10, 30, 30).atOffset(ZoneOffset.UTC),
            null
        };
        for (ValueGetter<OffsetTime> getter : getters(ValueGetter.OFFSET_TIME, ValueGetter.AsOffsetTime.INSTANCE)) {
            try (ResultSet r = stmt.executeQuery("SELECT \"value\" FROM TEST_TIME_WITH_TIMEZONE")) {
                /*
                 * Some databases do not properly preserve insert time zone, so we can
                 * only check if values represent the same instant from the start of day.
                 */
                assertContentEquals(expected, r, getter, OffsetTime::isEqual, "Time conversion");
            }
        }
    }

    /**
     * Compares the new rows of the given result set against the expected values.
     *
     * @param <V>         type of values to compare.
     * @param expected    values that should be returned by the query after conversion by the getter.
     * @param actual      result of an SQL query to validate.
     * @param getter      how to map each SQL result value to Java objects. Must not be null.
     * @param comparator  how to compare values, or {@code null} for default comparison.
     * @param title       an error message for assertion failures.
     */
    private static <V> void assertContentEquals(final V[] expected, final ResultSet actual, final ValueGetter<V> getter,
                                                final BiPredicate<V,V> comparator, final String title) throws Exception
    {
        for (final V expectedValue : expected) {
            assertTrue(actual.next());
            final V actualValue = getter.getValue(null, actual, 1);
            if (comparator != null && expectedValue != null && actualValue != null) {
                if (comparator.test(expectedValue, actualValue)) {
                    continue;
                }
                // Otherwise `assertEquals(…)` will format an error message.
            }
            assertEquals(expectedValue, actualValue, title);
        }
        assertFalse(actual.next());
    }
}
