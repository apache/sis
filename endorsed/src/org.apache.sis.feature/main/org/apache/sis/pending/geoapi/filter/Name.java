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
package org.apache.sis.pending.geoapi.filter;

import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.apache.sis.filter.internal.shared.FunctionNames;
import org.apache.sis.util.iso.Names;


/**
 * Placeholder for GeoAPI 3.1 interfaces (not yet released).
 * Shall not be visible in public API, as it will be deleted after next GeoAPI release.
 */
@SuppressWarnings("doclint:missing")
public final class Name {
    static final LocalName STANDARD = Names.createLocalName(null, null, "fes");

    static final LocalName EXTENSION = Names.createLocalName(null, null, "extension");

    public static final ScopedName LITERAL = Names.createScopedName(STANDARD, null, FunctionNames.Literal);

    public static final ScopedName VALUE_REFERENCE = Names.createScopedName(STANDARD, null, FunctionNames.ValueReference);

    private Name() {
    }
}
