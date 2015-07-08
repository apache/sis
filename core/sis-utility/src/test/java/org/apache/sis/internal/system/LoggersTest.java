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
package org.apache.sis.internal.system;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Verifies some constants declared in {@link Loggers} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class LoggersTest extends TestCase {
    /**
     * Verifies some logger names.
     */
    @Test
    public void verifyLoggerNames() {
        assertTrue(Loggers.ISO_19115,            Loggers.ISO_19115           .startsWith(Modules.METADATA    + '.'));
        assertTrue(Loggers.CRS_FACTORY,          Loggers.CRS_FACTORY         .startsWith(Modules.REFERENCING + '.'));
        assertTrue(Loggers.COORDINATE_OPERATION, Loggers.COORDINATE_OPERATION.startsWith(Modules.REFERENCING + '.'));
        assertTrue(Loggers.LOCALIZATION,         Loggers.LOCALIZATION        .startsWith(Modules.UTILITIES   + '.'));
    }
}
