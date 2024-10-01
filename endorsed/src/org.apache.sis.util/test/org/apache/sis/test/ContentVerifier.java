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

import java.util.Map;
import java.util.function.Predicate;
import org.opengis.metadata.Metadata;

// Test dependencies
import static org.junit.jupiter.api.Assumptions.abort;


/**
 * Place-holder for a GeoAPI 3.1 class. Used only for allowing the code to compile.
 * For real test execution, see the development branches on GeoAPI 4.0-SNAPSHOT.
 */
@SuppressWarnings("doclint:missing")
public class ContentVerifier {
    public void addPropertyToIgnore(Class<?> type, String property) {
        abort("This test requires GeoAPI 3.1.");
    }

    public void addPropertyToIgnore(Predicate<String> ignore) {
        abort("This test requires GeoAPI 3.1.");
    }

    public void addExpectedValue(String property, Object value) {
        abort("This test requires GeoAPI 3.1.");
    }

    @SafeVarargs
    public final void addExpectedValues(final Map.Entry<String,?>... properties) {
        abort("This test requires GeoAPI 3.1.");
    }

    public void addMetadataToVerify(Metadata actual) {
        abort("This test requires GeoAPI 3.1.");
    }

    public void assertMetadataEquals() {
        abort("This test requires GeoAPI 3.1.");
    }
}
