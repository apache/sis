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
package org.apache.sis.test;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Callable;
import java.io.PrintWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.Format;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.internal.util.X364;

import static org.junit.Assert.*;


/**
 * Miscellaneous utility methods for test cases.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final strictfp class TestUtilities extends Static {
    /**
     * Width of the separator to print to {@link TestCase#out}, in number of characters.
     */
    private static final int SEPARATOR_WIDTH = 80;

    /**
     * Maximal time that {@code waitFoo()} methods can wait, in milliseconds.
     *
     * @see #waitForBlockedState(Thread)
     * @see #waitForGarbageCollection(Callable)
     */
    private static final int MAXIMAL_WAIT_TIME = 1000;

    /**
     * Date parser and formatter using the {@code "yyyy-MM-dd HH:mm:ss"} pattern
     * and UTC time zone.
     */
    private static final DateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.setLenient(false);
    };

    /**
     * The {@link TreeTableFormat} to use for unlocalized string representations.
     * Created when first needed.
     */
    private static Format tableFormat;

    /**
     * The thread group for every threads created for testing purpose.
     */
    public static final ThreadGroup THREADS = new ThreadGroup("SIS-Tests");

    /**
     * Do not allow instantiation of this class.
     */
    private TestUtilities() {
    }

    /**
     * Prints and clear the current content of {@link TestCase#out}, regardless of whether
     * {@link TestCase#verbose} is {@code true} or {@code false}. This method should rarely
     * be needed.
     *
     * @since 0.4
     */
    public static void forceFlushOutput() {
        TestCase.flushOutput();
    }

    /**
     * If verbose output are enabled, prints the given title to {@link TestCase#out} in a box.
     * This method is invoked for writing a clear visual separator between the verbose output
     * of different test cases. This method does nothing if verbose output is not enabled,
     * because only the output of failed tests should be printed in such case.
     *
     * @param title The title to write.
     */
    public static void printSeparator(final String title) {
        if (TestCase.VERBOSE) {
            final PrintWriter out = TestCase.out;
            final boolean isAnsiSupported = X364.isAnsiSupported();
            if (isAnsiSupported) {
                out.print(X364.FOREGROUND_CYAN.sequence());
            }
            out.print('╒');
            for (int i=0; i<SEPARATOR_WIDTH-2; i++) {
                out.print('═');
            }
            out.println('╕');
            out.print("│ ");
            out.print(title);
            for (int i=title.codePointCount(0, title.length()); i<SEPARATOR_WIDTH-3; i++) {
                out.print(' ');
            }
            out.println('│');
            out.print('└');
            for (int i=0; i<SEPARATOR_WIDTH-2; i++) {
                out.print('─');
            }
            out.println('┘');
            if (isAnsiSupported) {
                out.print(X364.FOREGROUND_DEFAULT.sequence());
            }
        }
    }

    /**
     * Returns a new random number generator with a random seed.
     * If the test succeed, nothing else will happen. But if the test fails, then the seed value will
     * be logged to the {@link TestCase#out} stream in order to allow the developer to reproduce the
     * test failure.
     *
     * <p>This method shall be invoked only in the body of a test method - the random number generator
     * is not valid anymore after the test finished.</p>
     *
     * <p>This method doesn't need to be used in every cases. For example test cases using
     * {@link Random#nextGaussian()} should create their own random numbers generator with
     * the {@link Random#Random(long)} constructor instead
     * (see {@link org.apache.sis.math.StatisticsTest} for more explanation).
     * Or test cases that are mostly insensitive to the exact sequence of numbers
     * can use the {@link Random#Random()} constructor instead.</p>
     *
     * <p>This method is rather for testing relatively complex code which are likely to behave
     * differently depending on the exact sequence of numbers. We want to use random sequence
     * of numbers in order to test the code in a wider range of scenarios. However in case of
     * test failure, we need to know the <cite>seed</cite> which has been used in order to allow
     * the developer to reproduce the test with the exact same sequence of numbers.
     * Using this method, the seed can be retrieved in the messages sent to the output stream.</p>
     *
     * @return A new random number generator initialized with a random seed.
     */
    public static Random createRandomNumberGenerator() {
        long seed;
        do seed = StrictMath.round(StrictMath.random() * (1L << 48));
        while (seed == 0); // 0 is a sentinal value for "no generator".
        TestCase.randomSeed = seed;
        return new Random(seed);
    }

    /**
     * Returns a new random number generator with the given seed. This method is used only for debugging a test failure.
     * The seed given in argument is the value printed by the test runner. This argument shall be removed after the test
     * has been fixed.
     *
     * <p>The work flow is as below:</p>
     * <ul>
     *   <li>Uses {@link #createRandomNumberGenerator()} (without argument} in tests.</li>
     *   <li>If a test fail, find the seed value printed by the test runner, then insert that value in argument
     *       to {@code createRandomNumberGenerator(…)}.</li>
     *   <li>Debug the test.</li>
     *   <li>Once the test has been fixed, remove the argument from the {@code createRandomNumberGenerator()} call.</li>
     * </ul>
     *
     * @param  seed The random generator seed.
     * @return A new random number generator initialized with the given seed.
     *
     * @since 0.5
     */
    @Debug
    public static Random createRandomNumberGenerator(final long seed) {
        TestCase.randomSeed = seed;
        return new Random(seed);
    }

    /**
     * Parses the date for the given string using the {@code "yyyy-MM-dd HH:mm:ss"} pattern in UTC timezone.
     *
     * @param  date The date as a {@link String}.
     * @return The date as a {@link Date}.
     */
    public static Date date(final String date) {
        ArgumentChecks.ensureNonNull("date", date);
        try {
            synchronized (dateFormat) {
                return dateFormat.parse(date);
            }
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Formats the given date using the {@code "yyyy-MM-dd HH:mm:ss"} pattern in UTC timezone.
     *
     * @param  date The date to format.
     * @return The date as a {@link String}.
     */
    public static String format(final Date date) {
        ArgumentChecks.ensureNonNull("date", date);
        synchronized (dateFormat) {
            return dateFormat.format(date);
        }
    }

    /**
     * Formats the given value using the given formatter, and parses the text back to its value.
     * If the parsed value is not equal to the original one, an {@link AssertionError} is thrown.
     *
     * @param  formatter The formatter to use for formatting and parsing.
     * @param  value The value to format.
     * @return The formatted value.
     */
    public static String formatAndParse(final Format formatter, final Object value) {
        final String text = formatter.format(value);
        final Object parsed;
        try {
            parsed = formatter.parseObject(text);
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
        assertEquals("Parsed text not equal to the original value", value, parsed);
        return text;
    }

    /**
     * Returns a unlocalized string representation of {@code NAME} and {@code VALUE} columns of the given tree table.
     * Dates and times, if any, will be formatted using the {@code "yyyy-MM-dd HH:mm:ss"} pattern in UTC timezone.
     * This method is used mostly as a convenient way to verify the content of an ISO 19115 metadata object.
     *
     * @param  table The table for which to get a string representation.
     * @return A unlocalized string representation of the given tree table.
     */
    public static String formatNameAndValue(final TreeTable table) {
        synchronized (TestUtilities.class) {
            if (tableFormat == null) {
                final TreeTableFormat f = new TreeTableFormat(null, null);
                f.setColumns(TableColumn.NAME, TableColumn.VALUE);
                tableFormat = f;
            }
            return tableFormat.format(table);
        }
    }

    /**
     * Returns the tree structure of the given string representation, without the localized text.
     * For example given the following string:
     *
     * {@preformat text
     *   Citation
     *     ├─Title…………………………………………………… Some title
     *     └─Cited responsible party
     *         └─Individual name……………… Some person of contact
     * }
     *
     * this method returns an array containing the following elements:
     *
     * {@preformat text
     *   "",
     *   "  ├─",
     *   "  └─",
     *   "      └─"
     * }
     *
     * This method is used for comparing two trees having string representation in different locales.
     * In such case, we can not compare the actual text content. The best we can do is to compare
     * the tree structure.
     *
     * @param  tree The string representation of a tree.
     * @return The structure of the given tree, without text.
     */
    public static CharSequence[] toTreeStructure(final CharSequence tree) {
        final CharSequence[] lines = CharSequences.split(tree, '\n');
        for (int i=0; i<lines.length; i++) {
            final CharSequence line = lines[i];
            final int length = line.length();
            for (int j=0; j<length;) {
                final int c = Character.codePointAt(line, j);
                if (Character.isLetterOrDigit(c)) {
                    lines[i] = line.subSequence(0, j);
                    break;
                }
                j += Character.charCount(c);
            }
        }
        return lines;
    }

    /**
     * Returns the single element from the given array. If the given array is null or
     * does not contains exactly one element, then an {@link AssertionError} is thrown.
     *
     * @param  <E> The type of array elements.
     * @param  array The array from which to get the singleton.
     * @return The singleton element from the array.
     */
    public static <E> E getSingleton(final E[] array) {
        assertNotNull("Null array.", array);
        assertEquals("Not a singleton array.", 1, array.length);
        return array[0];
    }

    /**
     * Returns the single element from the given collection. If the given collection is null
     * or does not contains exactly one element, then an {@link AssertionError} is thrown.
     *
     * @param  <E> The type of collection elements.
     * @param  collection The collection from which to get the singleton.
     * @return The singleton element from the collection.
     */
    public static <E> E getSingleton(final Iterable<? extends E> collection) {
        assertNotNull("Null collection.", collection);
        final Iterator<? extends E> it = collection.iterator();
        assertTrue("The collection is empty.", it.hasNext());
        final E element = it.next();
        assertFalse("The collection has more than one element.", it.hasNext());
        return element;
    }

    /**
     * Returns a copy of the given array with the last ordinate values dropped for each coordinates.
     *
     * @param  coordinates The source coordinates from which to drop the last ordinate values.
     * @param  sourceDim   Number of dimensions of each point in the {@code coordinates} array.
     * @param  targetDim   Number of dimensions to retain.
     * @return Copy of the given {@code coordinates} array with only the {@code targetDim} first dimension for each point.
     *
     * @since 0.7
     */
    public static double[] dropLastDimensions(final double[] coordinates, final int sourceDim, final int targetDim) {
        assertEquals("Unexpected array length.", 0, coordinates.length % sourceDim);
        final int numPts = coordinates.length / sourceDim;
        final double[] reduced = new double[numPts * targetDim];
        for (int i=0; i<numPts; i++) {
            System.arraycopy(coordinates, i*sourceDim, reduced, i*targetDim, targetDim);
        }
        return reduced;
    }

    /**
     * If the given failure is not null, re-thrown it as an {@link Error} or
     * {@link RuntimeException}. Otherwise do nothing.
     *
     * @param failure The exception to re-thrown if non-null.
     */
    public static void rethrownIfNotNull(final Throwable failure) {
        if (failure != null) {
            if (failure instanceof Error) {
                throw (Error) failure;
            }
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            throw new UndeclaredThrowableException(failure);
        }
    }

    /**
     * Waits up to one second for the given thread to reach the
     * {@linkplain java.lang.Thread.State#BLOCKED blocked} or the
     * {@linkplain java.lang.Thread.State#WAITING waiting} state.
     *
     * @param  thread The thread to wait for blocked or waiting state.
     * @throws IllegalThreadStateException If the thread has terminated its execution,
     *         or has not reached the waiting or blocked state before the timeout.
     * @throws InterruptedException If this thread has been interrupted while waiting.
     */
    public static void waitForBlockedState(final Thread thread) throws IllegalThreadStateException, InterruptedException {
        int retry = MAXIMAL_WAIT_TIME / 5; // 5 shall be the same number than in the call to Thread.sleep.
        do {
            Thread.sleep(5);
            switch (thread.getState()) {
                case WAITING:
                case BLOCKED: return;
                case TERMINATED: throw new IllegalThreadStateException("The thread has completed execution.");
            }
        } while (--retry != 0);
        throw new IllegalThreadStateException("The thread is not in a blocked or waiting state.");
    }

    /**
     * Waits up to one second for the garbage collector to do its work. This method can be invoked
     * only if {@link TestConfiguration#allowGarbageCollectorDependentTests()} returns {@code true}.
     *
     * <p>Note that this method does not throw any exception if the given condition has not been
     * reached before the timeout. Instead, it is the caller responsibility to test the return
     * value. This method is designed that way because the caller can usually produce a more
     * accurate error message about which value has not been garbage collected as expected.</p>
     *
     * @param  stopCondition A condition which return {@code true} if this method can stop waiting,
     *         or {@code false} if it needs to ask again for garbage collection.
     * @return {@code true} if the given condition has been meet, or {@code false} if we waited up
     *         to the timeout without meeting the given condition.
     * @throws InterruptedException If this thread has been interrupted while waiting.
     */
    public static boolean waitForGarbageCollection(final Callable<Boolean> stopCondition) throws InterruptedException {
        assertTrue("GC-dependent tests not allowed in this run.", TestConfiguration.allowGarbageCollectorDependentTests());
        int retry = MAXIMAL_WAIT_TIME / 50; // 50 shall be the same number than in the call to Thread.sleep.
        boolean stop;
        do {
            if (--retry == 0) {
                return false;
            }
            Thread.sleep(50);
            System.gc();
            try {
                stop = stopCondition.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        } while (!stop);
        return true;
    }
}
