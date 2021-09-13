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
package org.apache.sis.filter;

import java.util.List;
import java.util.Collections;
import java.io.Serializable;
import java.io.ObjectStreamException;


/**
 * Placeholder for GeoAPI 3.1 interfaces (not yet released).
 * Shall not be visible in public API, as it will be deleted after next GeoAPI release.
 */
final class FilterLiteral implements Filter<Object>, Serializable {
    @SuppressWarnings("rawtypes")
    public static final Filter INCLUDE = new FilterLiteral(true);

    @SuppressWarnings("rawtypes")
    public static final Filter EXCLUDE = new FilterLiteral(false);

    private final boolean value;

    private FilterLiteral(final boolean value) {
        this.value = value;
    }

    @Override
    public Enum<?> getOperatorType() {
        return value ? FilterName.INCLUDE : FilterName.EXCLUDE;
    }

    @Override
    public List<Expression<? super Object, ?>> getExpressions() {
        return Collections.emptyList();
    }

    @Override
    public boolean test(Object object) {
        return value;
    }

    @Override
    public String toString() {
        return "Filter." + (value ? "INCLUDE" : "EXCLUDE");
    }

    private Object readResolve() throws ObjectStreamException {
        return value ? INCLUDE : EXCLUDE;
    }
}
