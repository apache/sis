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
package org.apache.sis.storage;

import java.util.logging.Logger;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.WarningEvent;

// Test dependencies
import org.apache.sis.test.TestCaseWithLogs;


/**
 * Base class for {@link DataStore} tests which may emit warnings.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public abstract class DataStoreTestCase extends TestCaseWithLogs implements StoreListener<WarningEvent> {
    /**
     * Creates a new test case which will listen to logs emitted by the given logger.
     *
     * @param  logger  the logger to listen to.
     */
    protected DataStoreTestCase(Logger logger) {
        super(logger);
    }

    /**
     * Creates a new test case which will listen to logs emitted by the logger of the given name.
     *
     * @param logger  name of the logger to listen.
     */
    protected DataStoreTestCase(String logger) {
        super(logger);
    }

    /**
     * Performs an unconditional redirection of the warnings emitted by the given store to the JUnit extension.
     * The {@link #eventOccured(WarningEvent)} method will be invoked for each warning.
     *
     * @param  store  the data store to listen to.
     */
    protected final void listen(DataStore store) {
        store.addListener(WarningEvent.class, this);
    }

    /**
     * Forwards the warning emitted by the data store to the JUnit extension that we use for checking warnings.
     * This is defined as an ordinary method rather than a lambda function for making easy to set debugger break point.
     *
     * @param  event  the warning emitted by the data store.
     */
    @Override
    public final void eventOccured(WarningEvent event) {
        loggings.accept(event.getDescription());
    }
}
