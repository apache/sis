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

import java.io.PrintStream;

// Test dependencies
import org.opentest4j.TestAbortedException;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


/**
 * A test watcher which will report more details if a test execution fails.
 * This watcher also reports who logged a warning, for helping to identify
 * where to add {@link LoggingWatcher} extension.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class FailureDetailsReporter implements BeforeEachCallback, AfterEachCallback {
    /**
     * Creates a new reporter.
     */
    public FailureDetailsReporter() {
    }

    /**
     * Clears the {@link TestCase#out} buffer and prepares the reporter to receive log records.
     *
     * @param  description  description of the test to run.
     */
    @Override
    public final void beforeEach(final ExtensionContext description) {
        if (!TestCase.VERBOSE) {
            TestCase.out.clearBuffer();
        }
        LogRecordCollector.INSTANCE.setCurrentTest(description);
    }

    /**
     * If verbose mode is enabled, or if the test failed, prints {@link TestCase#out} to the console.
     * Otherwise, silently discards the output.
     *
     * @param  description  description of the test which has been run.
     */
    @Override
    public final void afterEach(final ExtensionContext description) {
        boolean flush = TestCase.VERBOSE;
        LogRecordCollector.INSTANCE.setCurrentTest(null);
        Throwable ex = description.getExecutionException().orElse(null);
        if (ex != null && !(ex instanceof TestAbortedException)) {
            description.getTestMethod().ifPresent((method) -> {
                final Long seed = TestUtilities.randomSeed.get();
                if (seed != null) {
                    @SuppressWarnings("UseOfSystemOutOrSystemErr")
                    final PrintStream out = System.err;           // Same as logging console handler.
                    out.print("Random number generator for ");
                    out.print(description.getTestClass().map(Class::getCanonicalName).orElse("<?>"));
                    out.print('.');
                    out.print(method.getName());
                    out.print("() was created with seed ");
                    out.print(seed);
                    out.println('.');
                }
            });
            flush = true;
        }
        TestUtilities.randomSeed.remove();
        if (flush) {
            TestCase.out.flushUnconditionally();
        }
    }
}
