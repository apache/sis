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

import java.util.Locale;
import java.util.TimeZone;
import org.apache.sis.internal.jaxb.Context;
import org.junit.After;

import static org.junit.Assert.*;


/**
 * Base class of XML (un)marshalling tests. SIS (un)marshalling process can be partially controlled
 * by a {@link Context}, which defines (among other) the locale and timezone. Some tests will need
 * to fix the context to a particular locale and timezone before to execute the test.
 *
 * <p>The {@link #context} field can be initialized by subclasses either explicitely or by invoking
 * a {@code createContext(…)} convenience method. The {@link #clearContext()} method will be invoked
 * after each test for clearing the SIS internal {@link ThreadLocal} which was holding that context.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract strictfp class XMLTestCase extends TestCase {
    /**
     * The context containing locale, timezone, GML version and other information.
     * The context is initially {@code null} and can be created either explicitely,
     * or by invoking the {@link #createContext(boolean, Locale, String)} convenience method.
     *
     * @see #createContext(boolean, Locale, String)
     * @see #clearContext()
     */
    protected Context context;

    /**
     * Creates a new test case.
     */
    protected XMLTestCase() {
    }

    /**
     * Initializes the {@link #context} to the given locale and timezone.
     *
     * @param marshal  {@code true} for setting the {@link Context#MARSHALLING} flag.
     * @param locale   The locale, or {@code null} for the default.
     * @param timezone The timezone, or {@code null} for the default.
     *
     * @see #clearContext()
     */
    protected final void createContext(final boolean marshal, final Locale locale, final String timezone) {
        context = new Context(marshal ? Context.MARSHALLING : 0, locale,
                (timezone != null) ? TimeZone.getTimeZone(timezone) : null, null, null, null, null, null);
    }

    /**
     * Resets {@link #context} to {@code null} and clears the {@link ThreadLocal} which was holding that context.
     * This method is automatically invoked by JUnit after each test, but can also be invoked explicitely before
     * to create a new context. It is safe to invoke this method more than once.
     */
    @After
    public final void clearContext() {
        assertSame("Unexpected context. Is this method invoked from the right thread?", context, Context.current());
        if (context != null) {
            context.finish();
            context = null;
        }
    }
}
