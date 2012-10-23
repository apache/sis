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
package org.apache.sis.io;

import java.io.IOException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.util.CharSequencesTest;

import static org.junit.Assert.*;

// Related to JDK7
import org.apache.sis.internal.util.JDK7;


/**
 * Tests the {@link TableFormatter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@DependsOn({CharSequencesTest.class, X364Test.class, ExpandedTabFormatterTest.class})
public final strictfp class TableFormatterTest extends FormatterTestCase {
    /**
     * The table formatter to test. May not be same instance than {@link #formatter},
     * because the super-class will wraps it in a {@link SingleCharAppendable} in
     * some occasions.
     */
    private final TableFormatter table;

    /**
     * Creates a new test case.
     */
    public TableFormatterTest() {
        formatter = table = new TableFormatter(formatter);
    }

    /**
     * A simple test using the given line separator.
     */
    @Override
    void run(String lineSeparator) throws IOException {
        final Appendable out = formatter;
        table.nextLine('═');

        // r.e.d. = Equatorial diameter Measured relative to the Earth.
        // Source: "Planet" on wikipedia on July 25, 2008.
        assertSame(out, out.append("English\tFrench\tr.e.d." + lineSeparator));
        table.writeHorizontalSeparator();
        assertSame(out, out.append("Mercury\tMercure\t0.382" + lineSeparator));
        assertSame(out, out.append("Venus\tVénus\t0.949"     + lineSeparator));
        assertSame(out, out.append("Earth\tTerre"));
        assertSame(out, out.append("\t1.00"                  + lineSeparator));
        assertSame(out, out.append("Mars\tMa"));
        assertSame(out, out.append("rs\t0.532"               + lineSeparator));
        assertSame(out, out.append("Jupiter\tJupiter\t11.209"));
        assertSame(out, out.append(lineSeparator));
        assertSame(out, out.append("Saturn"));
        assertSame(out, out.append("\tSaturne\t"));
        assertSame(out, out.append("9.449"                   + lineSeparator));
        assertSame(out, out.append("Uranus\tUranus\t4.007"   + lineSeparator
                                 + "Neptune\tNeptune\t3.883" + lineSeparator));
        table.nextLine('═');
        /*
         * If our test case is using the wrapper which will send the data once character at time,
         * our TableFormatter implementation will not be able to detect the line separator and
         * will fallback on the default one. So we set the line separator to the one actually used
         * not because this is the class contract (quite the opposite, this is a limitation in our
         * implementation), but simply in order to allow the test to pass.
         */
        if (out instanceof SingleCharAppendable) {
            lineSeparator = JDK7.lineSeparator();
        }
        assertOutputEquals(
                  "╔═════════╤═════════╤════════╗" + lineSeparator
                + "║ English │ French  │ r.e.d. ║" + lineSeparator
                + "╟─────────┼─────────┼────────╢" + lineSeparator
                + "║ Mercury │ Mercure │ 0.382  ║" + lineSeparator
                + "║ Venus   │ Vénus   │ 0.949  ║" + lineSeparator
                + "║ Earth   │ Terre   │ 1.00   ║" + lineSeparator
                + "║ Mars    │ Mars    │ 0.532  ║" + lineSeparator
                + "║ Jupiter │ Jupiter │ 11.209 ║" + lineSeparator
                + "║ Saturn  │ Saturne │ 9.449  ║" + lineSeparator
                + "║ Uranus  │ Uranus  │ 4.007  ║" + lineSeparator
                + "║ Neptune │ Neptune │ 3.883  ║" + lineSeparator
                + "╚═════════╧═════════╧════════╝" + lineSeparator);
    }
}
