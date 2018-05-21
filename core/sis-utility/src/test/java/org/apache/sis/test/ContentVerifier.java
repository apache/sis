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

import static org.junit.Assume.assumeTrue;

import org.opengis.metadata.Metadata;


/**
 * Place-holder for a GeoAPI 3.1 class. Used only for allowing the code to compile.
 * For real test execution, see the development branches on GeoAPI 4.0-SNAPSHOT.
 */
public class ContentVerifier {
    public void addPropertyToIgnore(Class<?> type, String property) {
        assumeTrue("This test requires GeoAPI 3.1.", false);
    }

    public void addMetadataToVerify(Metadata actual) {
        assumeTrue("This test requires GeoAPI 3.1.", false);
    }

    public void assertMetadataEquals(final String path, final Object value, final Object... others) {
        assumeTrue("This test requires GeoAPI 3.1.", false);
    }
}
