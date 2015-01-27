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

import org.junit.Before;
import org.apache.sis.test.DependsOn;


/**
 * Tests {@link LineAppender} implementation when used for wrapping lines to 80 characters,
 * chained with another {@code LineAppender}. Such chaining should not be needed since a
 * single {@code LineAppender} can handle many operations. But if such situation happens
 * anyway, we want it to work.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(WordWrapTest.class)
public final strictfp class WordWrapWithLineSeparatorTest extends WordWrapTest {
    /**
     * Creates and configure the {@link LineAppender} to test.
     */
    @Before
    @Override
    public void createLineAppender() {
        appender = new LineAppender(new LineAppender(appender, "\r", false), 10, false);
    }

    /**
     * Returns the line separator used by this test.
     */
    @Override
    String expectedLineSeparator(final String lineSeparator) {
        return "\r";
    }
}
