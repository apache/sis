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
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.Format;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.util.Debug;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.internal.shared.X364;
import static org.apache.sis.util.internal.shared.Constants.UTC;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;

// Specific to the main branch:
import org.opengis.referencing.ReferenceSystem;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.operation.CoordinateOperation;


/**
 * Miscellaneous utility methods for test cases.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TestUtilities {
    /**
     * Width of the separator to print to {@link TestCase#out}, in number of characters.
     */
    private static final int SEPARATOR_WIDTH = 80;

    /**
     * Date parser and formatter using the {@code "yyyy-MM-dd HH:mm:ss"} pattern
     * and UTC time zone.
     */
    private static final DateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
        dateFormat.setTimeZone(TimeZone.getTimeZone(UTC));
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
     * The seed for the random number generator created by {@link #createRandomNumberGenerator()}, or null if none.
     * This information is used for printing the seed in case of test failure, in order to allow the developer to
     * reproduce the failure.
     */
    static final ThreadLocal<Long> randomSeed = new ThreadLocal<>();

    /**
     * Do not allow instantiation of this class.
     */
    private TestUtilities() {
    }

    /**
     * Prints and clear the current content of {@link TestCase#out}, regardless of whether
     * {@link TestCase#VERBOSE} is {@code true} or {@code false}. This method should rarely
     * be needed.
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
     * @param  title  the title to write.
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
     * <p>This method doesn't need to be used in every cases. For example, test cases using
     * {@link Random#nextGaussian()} should create their own random numbers generator with
     * the {@link Random#Random(long)} constructor instead
     * (see {@link org.apache.sis.math.StatisticsTest} for more explanation).
     * Or test cases that are mostly insensitive to the exact sequence of numbers
     * can use the {@link Random#Random()} constructor instead.</p>
     *
     * <p>This method is rather for testing relatively complex code which are likely to behave
     * differently depending on the exact sequence of numbers. We want to use random sequence
     * of numbers in order to test the code in a wider range of scenarios. However, in case of
     * test failure, we need to know the <i>seed</i> which has been used in order to allow
     * the developer to reproduce the test with the exact same sequence of numbers.
     * Using this method, the seed can be retrieved in the messages sent to the output stream.</p>
     *
     * @return a new random number generator initialized with a random seed.
     */
    public static Random createRandomNumberGenerator() {
        final long seed = StrictMath.round(StrictMath.random() * (1L << 48));
        randomSeed.set(seed);
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
     * @param  seed  the random generator seed.
     * @return a new random number generator initialized with the given seed.
     */
    @Debug
    public static Random createRandomNumberGenerator(final long seed) {
        randomSeed.set(seed);
        return new Random(seed);
    }

    /**
     * Parses the date for the given string using the {@code "yyyy-MM-dd HH:mm:ss"} pattern in UTC timezone.
     *
     * @param  date  the date as a {@link String}.
     * @return the date as a {@link Date}.
     *
     * @todo Remove in favor of {@link java.time.Instant#parse}.
     */
    public static Date date(final String date) {
        ArgumentChecks.ensureNonNull("date", date);
        final Date t;
        try {
            synchronized (dateFormat) {
                t = dateFormat.parse(date);
            }
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
        /*
         * The milliseconds are not part of the pattern used by this method because they are rarely specified.
         * If a test needs to specify milliseconds, add them manually here. Note that this naive hack requires
         * all milliseconds digits to be provided, e.g. ".900" - not ".9".
         */
        final int s = date.lastIndexOf('.');
        if (s >= 0) {
            final int ms = Integer.parseInt(date.substring(s + 1));
            t.setTime(t.getTime() + ms);
        }
        return t;
    }

    /**
     * Formats the given value using the given formatter, and parses the text back to its value.
     * If the parsed value is not equal to the original one, an {@link AssertionError} is thrown.
     *
     * @param  formatter  the formatter to use for formatting and parsing.
     * @param  value      the value to format.
     * @return the formatted value.
     */
    public static String formatAndParse(final Format formatter, final Object value) {
        final String text = formatter.format(value);
        final Object parsed;
        try {
            parsed = formatter.parseObject(text);
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
        assertEquals(value, parsed, "Parsed text not equal to the original value");
        return text;
    }

    /**
     * Returns a unlocalized string representation of {@code NAME}, {@code VALUE} and {@code REMARKS} columns
     * of the given tree table. They are the columns included in default string representation of metadata.
     * Dates and times, if any, will be formatted using the {@code "yyyy-MM-dd HH:mm:ss"} pattern in UTC timezone.
     * This method is used mostly as a convenient way to verify the content of an ISO 19115 metadata object.
     *
     * @param  table  the table for which to get a string representation.
     * @return a unlocalized string representation of the given tree table.
     */
    public static String formatMetadata(final TreeTable table) {
        synchronized (TestUtilities.class) {
            if (tableFormat == null) {
                final TreeTableFormat f = new TreeTableFormat(null, null);
                f.setColumns(TableColumn.NAME, TableColumn.VALUE, TableColumn.REMARKS);
                tableFormat = f;
            }
            return tableFormat.format(table);
        }
    }

    /**
     * Returns the tree structure of the given string representation, without the localized text.
     * For example, given the following string:
     *
     * <pre class="text">
     *   Citation
     *     ├─Title…………………………………………………… Some title
     *     └─Cited responsible party
     *         └─Individual name……………… Some person of contact</pre>
     *
     * this method returns an array containing the following elements:
     *
     * <pre class="text">
     *   "",
     *   "  ├─",
     *   "  └─",
     *   "      └─"</pre>
     *
     * This method is used for comparing two trees having string representation in different locales.
     * In such case, we cannot compare the actual text content. The best we can do is to compare
     * the tree structure.
     *
     * @param  tree  the string representation of a tree.
     * @return the structure of the given tree, without text.
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
     * @param  <E>    the type of array elements.
     * @param  array  the array from which to get the singleton.
     * @return the singleton element from the array.
     */
    public static <E> E getSingleton(final E[] array) {
        assertNotNull(array, "Null array.");
        assertEquals(1, array.length, "Not a singleton array.");
        return array[0];
    }

    /**
     * Returns the single element from the given collection. If the given collection is null
     * or does not contains exactly one element, then an {@link AssertionError} is thrown.
     *
     * @param  <E>         the type of collection elements.
     * @param  collection  the collection from which to get the singleton.
     * @return the singleton element from the collection.
     */
    public static <E> E getSingleton(final Iterable<? extends E> collection) {
        assertNotNull(collection, "Null collection.");
        final Iterator<? extends E> it = collection.iterator();
        assertTrue(it.hasNext(), "The collection is empty.");
        final E element = it.next();
        assertFalse(it.hasNext(), "The collection has more than one element.");
        return element;
    }

    /**
     * Returns the scope of the given object. Exactly one scope shall exist.
     *
     * @param  object  the object for which to get the scope.
     * @return the single scope of the given object.
     */
    public static String getScope(final IdentifiedObject object) {
        final InternationalString scope;
        if (object instanceof ReferenceSystem) {
            scope = ((ReferenceSystem) object).getScope();
        } else if (object instanceof Datum) {
            scope = ((Datum) object).getScope();
        } else if (object instanceof CoordinateOperation) {
            scope = ((CoordinateOperation) object).getScope();
        } else {
            scope = null;
        }
        assertNotNull(scope, "Missing scope.");
        return scope.toString();
    }

    /**
     * Returns the domain of validity of the given object. Exactly one domain shall exist,
     * and that domain shall be a geographic bounding box.
     *
     * @param  object  the object for which to get the domain of validity.
     * @return the single domain of validity of the given object.
     */
    public static GeographicBoundingBox getDomainOfValidity(final IdentifiedObject object) {
        final Extent extent;
        if (object instanceof ReferenceSystem) {
            extent = ((ReferenceSystem) object).getDomainOfValidity();
        } else if (object instanceof Datum) {
            extent = ((Datum) object).getDomainOfValidity();
        } else if (object instanceof CoordinateOperation) {
            extent = ((CoordinateOperation) object).getDomainOfValidity();
        } else {
            extent = null;
        }
        assertNotNull(extent, "Missing extent.");
        return assertInstanceOf(GeographicBoundingBox.class, getSingleton(extent.getGeographicElements()));
    }

    /**
     * Returns a copy of the given array with the last coordinate values dropped for each coordinates.
     *
     * @param  coordinates  the source coordinates from which to drop the last coordinate values.
     * @param  sourceDim    number of dimensions of each point in the {@code coordinates} array.
     * @param  targetDim    number of dimensions to retain.
     * @return copy of the given {@code coordinates} array with only the {@code targetDim} first dimension for each point.
     */
    public static double[] dropLastDimensions(final double[] coordinates, final int sourceDim, final int targetDim) {
        assertEquals(0, coordinates.length % sourceDim, "Unexpected array length.");
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
     * @param  failure  the exception to re-thrown if non-null.
     */
    public static void rethrownIfNotNull(final Throwable failure) {
        if (failure != null) {
            if (failure instanceof Error e) throw e;
            if (failure instanceof RuntimeException e) throw e;
            throw new UndeclaredThrowableException(failure);
        }
    }

    /**
     * Copies the full content of the given input stream in a temporary file and returns the channel for that file.
     * The file is opened with {@link StandardOpenOption#DELETE_ON_CLOSE}, together with read and write options.
     *
     * @param  data    the data to copy in the temporary file.
     * @param  suffix  suffix (dot included) to append to the temporary file name, or {@code null} if none.
     * @return a channel opened on a copy of the content of the given test resource.
     * @throws IOException if an error occurred while copying the data.
     */
    public static SeekableByteChannel createTemporaryFile(final InputStream data, final String suffix) throws IOException {
        final SeekableByteChannel channel;
        try (ReadableByteChannel in = Channels.newChannel(data)) {
            final Path file = Files.createTempFile("SIS", suffix);
            channel = Files.newByteChannel(file, StandardOpenOption.DELETE_ON_CLOSE,
                                StandardOpenOption.READ, StandardOpenOption.WRITE);
            final ByteBuffer buffer = ByteBuffer.allocate(4000);
            while (in.read(buffer) >= 0) {
                buffer.flip();
                channel.write(buffer);
                buffer.clear();
            }
        }
        return channel.position(0);
    }
}
